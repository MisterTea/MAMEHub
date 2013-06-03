/*
 *  Copyright (c) 2011, Tonchidot Corporation
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *      * Neither the name of the Tonchidot Corporation nor the
 *        names of its contributors may be used to endorse or promote products
 *        derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  @author Julien Cayzac http://github.com/jcayzac
 *
 */

#include <google/protobuf/io/lzma_protobuf_stream.h>
#include <google/protobuf/io/coded_stream.h>
#include <lib7z/LzmaEnc.h>
#include <lib7z/LzmaDec.h>
#include <lib7z/Sha256.h>

namespace google {
namespace protobuf {
namespace io {

struct lzma_sha256: CSha256 {
  struct digest_t {
    union {
      uint8  bytes[32];
      uint64 integer[4];
    };
    bool operator==(const digest_t& o) const {
      for (uint32 i(0); i<4; ++i)
        if (integer[i] != o.integer[i])
          return false;
      return true;
    }
    bool operator!=(const digest_t& o) const { return !(*this == o); }
  };
  void compute(digest_t& digest, const uint8_t* data, uint32 size) {
    Sha256_Update(this, data, size);
    Sha256_Final(this, digest.bytes);
  }
  lzma_sha256() {
    Sha256_Init(this);
  }
};

static inline internal::LogMessage& operator<<(internal::LogMessage& o, const lzma_sha256::digest_t& x) {
  static const char* const hexify = "0123456789ABCDEF";
  for (uint32 i(0); i<32; ++i) {
    o << (char) hexify[x.bytes[i]>>4] << (char) hexify[x.bytes[i]&0xF];
  }
  return o;
}

namespace {

// Various functions of the LZMA SDK want
// a pointer to an allocator (ISzAlloc)
struct lzma_alloc: ISzAlloc {
  static lzma_alloc* get() {
    static lzma_alloc instance;
    return &instance;
  }
 private:
  static void *_alloc(void*, size_t size) {
    return operator new(size);
  }
  static void _free(void*, void* address) {
    if (address) operator delete(address);
  }
  lzma_alloc() {
    Alloc = _alloc;
    Free  = _free;
  }
};

static const char* error_msg(int err) {
  switch(err) {
    case SZ_OK:                return "";
    case SZ_ERROR_DATA:        return " [SZ_ERROR_DATA]";
    case SZ_ERROR_MEM:         return " [SZ_ERROR_MEM]";
    case SZ_ERROR_CRC:         return " [SZ_ERROR_CRC]";
    case SZ_ERROR_UNSUPPORTED: return " [SZ_ERROR_UNSUPPORTED]";
    case SZ_ERROR_PARAM:       return " [SZ_ERROR_PARAM]";
    case SZ_ERROR_INPUT_EOF:   return " [SZ_ERROR_INPUT_EOF]";
    case SZ_ERROR_OUTPUT_EOF:  return " [SZ_ERROR_OUTPUT_EOF]";
    case SZ_ERROR_READ:        return " [SZ_ERROR_READ]";
    case SZ_ERROR_WRITE:       return " [SZ_ERROR_WRITE]";
    case SZ_ERROR_PROGRESS:    return " [SZ_ERROR_PROGRESS]";
    case SZ_ERROR_FAIL:        return " [SZ_ERROR_FAIL]";
    case SZ_ERROR_THREAD:      return " [SZ_ERROR_THREAD]";
    case SZ_ERROR_ARCHIVE:     return " [SZ_ERROR_ARCHIVE]";
    case SZ_ERROR_NO_ARCHIVE:  return " [SZ_ERROR_NO_ARCHIVE]";
    default:                   return " [Unknown error]";
  }
}

static const char* status_msg(ELzmaStatus s) {
  switch (s) {
    case LZMA_STATUS_NOT_SPECIFIED:               return "";
    case LZMA_STATUS_FINISHED_WITH_MARK:          return " [Stream was finished with end mark]";
    case LZMA_STATUS_NOT_FINISHED:                return " [Stream was not finished]";
    case LZMA_STATUS_NEEDS_MORE_INPUT:            return " [Must provide more input bytes]";
    case LZMA_STATUS_MAYBE_FINISHED_WITHOUT_MARK: return " [Stream was finished without end mark]";
    default:                                      return " [Unknown status]";
  }
}

} // anonymous namespace

/////////////// INPUT STREAM ////////////////

LzmaInputStream::LzmaInputStream(
  ZeroCopyInputStream* sub_stream,
  bool verify_data)
: mWire(sub_stream)
, mPacketSize(0)
, mCurrentIndex(0)
, mByteCount(0)
, mDataVerificationRequested(verify_data)
, mSha256(0)
, mPropsEncoded()
, mFUBAR(false)
{
  if (mDataVerificationRequested) {
    mSha256 = new lzma_sha256;
  }
}

LzmaInputStream::~LzmaInputStream() {
  delete mSha256;
}

bool LzmaInputStream::Next(const void** data, int* size) {
  GOOGLE_CHECK(data);
  GOOGLE_CHECK(size);
  // "Read" what's left in the buffer
  uint32 read_count(mPacketSize-mCurrentIndex);
  if (!read_count) {
    // Buffer was empty. Fetch new data
    if (!ReadNextBlock()) {
      GOOGLE_LOG(ERROR) << "LZMA: Failed to read a block\n";
      return false;
    }
    read_count = mPacketSize-mCurrentIndex;
  }
  
  *data = &mBuffer[mCurrentIndex];
  *size = read_count;
  
  mByteCount += read_count;
  mCurrentIndex += read_count;
  return (bool)read_count;
}

void LzmaInputStream::BackUp(int count) {
  GOOGLE_DCHECK_LT(0, count);
  const uint32 u_count(count);
  GOOGLE_DCHECK_LT(u_count, mCurrentIndex);
  GOOGLE_DCHECK_LT(u_count, mByteCount);
  mCurrentIndex -= u_count;
  mByteCount    -= u_count;
}

bool LzmaInputStream::Skip(int count) {
  GOOGLE_DCHECK_LT(0, count);
  uint32 u_count(count);
  while (u_count > mPacketSize-mCurrentIndex) {
    // discard current packet and load another
    u_count    -= mPacketSize-mCurrentIndex;
    mByteCount += mPacketSize-mCurrentIndex;
    if (!ReadNextBlock()) {
      GOOGLE_LOG(ERROR) << "LZMA: Failed to read a block\n";
      return false;
    }
  }
  mCurrentIndex += u_count;
  mByteCount    += u_count;
  return true;
}

int64 LzmaInputStream::ByteCount() const {
  return (int64) mByteCount;
}

bool LzmaInputStream::ReadNextBlock() {
  if (mDataVerificationRequested && !mSha256) return false;
  if (mFUBAR) return false;

  uint32 next_props_size;
  if (!mWire.ReadVarint32(&next_props_size)) {
    GOOGLE_LOG(ERROR) << "LZMA: Failed to read properties size\n";
    mFUBAR=true;
    mPropsEncoded.clear();
    return false;
  }
  
  if (next_props_size) {
    mPropsEncoded.resize(next_props_size);
    if (!mWire.ReadRaw(&mPropsEncoded[0], next_props_size)) {
      GOOGLE_LOG(ERROR) << "LZMA: Failed to read properties\n";
      mFUBAR=true;
      return false;
    }
  }

  uint32 compressed_size;
  if (!mWire.ReadVarint32(&compressed_size)) {
    GOOGLE_LOG(ERROR) << "LZMA: Failed to parse compressed size\n";
    return false;
  }

  if (!mWire.ReadVarint32(&mPacketSize)) {
    GOOGLE_LOG(ERROR) << "LZMA: Failed to parse packet size\n";
    return false;
  }

  mBuffer.resize(mPacketSize+compressed_size);
  if (mBuffer.size() != mPacketSize+compressed_size) {
    GOOGLE_LOG(ERROR) << "LZMA: Failed to resize buffer\n";
    return false;
  }
  
  lzma_sha256::digest_t saved_sha256;
  if (!mWire.ReadRaw(saved_sha256.bytes, 32)) {
    GOOGLE_LOG(ERROR) << "LZMA: Failed to read packet's Sha-256\n";
    return false;
  }

  if (!mWire.ReadRaw(&mBuffer[mPacketSize], compressed_size)) {
    GOOGLE_LOG(ERROR) << "LZMA: Failed to read " << compressed_size << "-byte long raw packet\n";
    return false;
  }
  
  mCurrentIndex = 0;
  
  ELzmaStatus status;
  SizeT destLen(mPacketSize), srcLen(compressed_size);
  const int ret(LzmaDecode(
    &mBuffer[0], &destLen,
    &mBuffer[mPacketSize], &srcLen,
    &mPropsEncoded[0], mPropsEncoded.size(),
    LZMA_FINISH_ANY, &status, lzma_alloc::get()
  ));
  mPacketSize = (uint32) destLen;

  if (ret || (status==LZMA_STATUS_NOT_FINISHED)) {
    GOOGLE_LOG(ERROR) << "LZMA: Failed to decompress " << compressed_size << " bytes of data" << error_msg(ret) << status_msg(status) << "\n";
    mPacketSize = 0;
    return false;
  }

  if (mDataVerificationRequested && mSha256) {
    lzma_sha256::digest_t computed_sha256;
    mSha256->compute(computed_sha256, &mBuffer[0], mPacketSize);
    if (saved_sha256 != computed_sha256) {
      GOOGLE_LOG(ERROR) << "LZMA: Corrupt data\n";
      return false;
    }
  }

  GOOGLE_LOG(INFO) << "LZMA: Read a packet\n"
  << "\t         Raw: " << compressed_size << "\n"
  << "\tDecompressed: " << mPacketSize << "\n"
  << "\t     SHA-256: " << saved_sha256 << "\n"
  ;
  return true;
}

// Raw and compressed data overlap. To conserve memory
// we don't use a PACKET_SIZE-byte long buffer for
// raw data and a (PACKET_SIZE + worst case overhead)
// -byte buffer for compressed data.
// 150% of PACKET_SIZE should fit all needs, as long
// as the raw is at the end of the buffer (so that when
// data gets overwritten, it has already been processed).
LzmaOutputStream::LzmaOutputStream(
  ZeroCopyOutputStream* sub_stream,
  uint32 max_packet_size
)
: mSubStream(sub_stream)
, mPacketSize(0)
, mByteCount(0)
, mMaxPacketSize(max_packet_size)
, mOffsetToUncompressedData(mMaxPacketSize>>1)
, mBuffer(mMaxPacketSize+mOffsetToUncompressedData)
, mSha256(new lzma_sha256)
, mEncoderHandle(LzmaEnc_Create(lzma_alloc::get()))
, mPropsEncoded()
, mPropsWritten(false)
{
  if (mEncoderHandle) {
    if (!ChangeEncodingOptions()) {
      GOOGLE_LOG(ERROR) << "LZMA: Can't initialize\n";
      LzmaEnc_Destroy(mEncoderHandle, lzma_alloc::get(), lzma_alloc::get());
      mEncoderHandle = 0;
    }
  }
}

LzmaOutputStream::~LzmaOutputStream() {
  Flush();
  delete mSha256;
  if (mEncoderHandle) {
    LzmaEnc_Destroy(mEncoderHandle, lzma_alloc::get(), lzma_alloc::get());
    mEncoderHandle = 0;
  }
}

bool LzmaOutputStream::Flush() {
  if (!mSha256 || !mEncoderHandle || !mSubStream) {
    GOOGLE_LOG(ERROR) << "LZMA: Bad state\n";
    return false;
  }
  if (!mPacketSize) return true;
  
  CodedOutputStream wire(mSubStream);
  
  if (mPropsWritten) {
    // zero-sized props. Decoder will just re-use the last ones.
    wire.WriteVarint32(0);
  }
  else {
    wire.WriteVarint32(mPropsEncoded.size());
    wire.WriteRaw(&mPropsEncoded[0], mPropsEncoded.size());
    mPropsWritten=true;
  }

  SizeT dst_length(mBuffer.size());
  if (LzmaEnc_MemEncode(mEncoderHandle,
      &mBuffer[0], &dst_length,
      &mBuffer[mOffsetToUncompressedData], (SizeT) mPacketSize,
      0, 0, lzma_alloc::get(), lzma_alloc::get()
    )) {
    GOOGLE_LOG(ERROR) << "LZMA: Failed to compress packet\n";
    return false;
  }

  wire.WriteVarint32((uint32) dst_length);
  wire.WriteVarint32(mPacketSize);
  
  lzma_sha256::digest_t computed_sha256;
  mSha256->compute(computed_sha256, &mBuffer[mOffsetToUncompressedData], mPacketSize);
  wire.WriteRaw(computed_sha256.bytes, 32);
  
  wire.WriteRaw(&mBuffer[0], (uint32) dst_length);
  GOOGLE_LOG(INFO) << "LZMA: Wrote a packet\n"
  << "\t  Raw size: " << mPacketSize << "\n"
  << "\tCompressed: " << dst_length << "\n"
  << "\t   SHA-256: " << computed_sha256 << "\n"
  << "\tProperties: " << mPropsEncoded.size() << "\n"
  ;

  mPacketSize = 0;
  return true;
}

bool LzmaOutputStream::ChangeEncodingOptions(
  int      level,
  uint32   dictSize,
  int      lc,
  int      lp,
  int      pb,
  int      algo,
  int      fb,
  int      btMode,
  int      numHashBytes,
  uint32   mc,
  unsigned writeEndMark,
  int      numThreads
) {
  CLzmaEncProps props;
  LzmaEncProps_Init(&props);
  props.level        = level;
  props.dictSize     = dictSize;
  props.lc           = lc;
  props.lp           = lp;
  props.pb           = pb;
  props.algo         = algo;
  props.fb           = fb;
  props.btMode       = btMode;
  props.numHashBytes = numHashBytes;
  props.mc           = mc;
  props.writeEndMark = writeEndMark;
  props.numThreads   = numThreads;
  
  int error = LzmaEnc_SetProps(mEncoderHandle, &props);
  SizeT size(LZMA_PROPS_SIZE);
  mPropsEncoded.resize(size);
  error |= LzmaEnc_WriteProperties(mEncoderHandle, &mPropsEncoded[0], &size);
  if (error) {
    GOOGLE_LOG(ERROR) << "LZMA: Failed to change encoding options\n";
    mPropsEncoded.clear();
    return false;
  }
  mPropsEncoded.resize(size);
  mPropsWritten = false;
  return true;
}

bool LzmaOutputStream::Next(void** data, int* size) {
  if (mPacketSize==mMaxPacketSize) {
    if (!Flush()) return false;
  }

  const uint32_t actual_size(mMaxPacketSize-mPacketSize);
  *data = &mBuffer[mPacketSize + mOffsetToUncompressedData];
  *size = (int) actual_size;
  mPacketSize += actual_size;
  mByteCount  += actual_size;
  return true;
}

void LzmaOutputStream::BackUp(int count) {
  GOOGLE_DCHECK_LT(0, count);
  const uint32_t u_count(count);
  GOOGLE_DCHECK_LT(u_count, mPacketSize);
  mPacketSize -= u_count;
  mByteCount  -= u_count;
}

int64 LzmaOutputStream::ByteCount() const {
  return (int64) mByteCount;
}

} // namespace io
} // namespace protobuf
} // namespace google

/* vim: set sw=2 ts=2 sts=2 expandtab ff=unix: */
