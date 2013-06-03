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

#pragma once
#include <google/protobuf/io/zero_copy_stream.h>
#include <google/protobuf/io/coded_stream.h>
#include <vector>
#include <stdint.h>

namespace google {
namespace protobuf {
namespace io {

// private class used for data verification
struct lzma_sha256;

/** @brief A ZeroCopyInputStream that reads compressed data through the LZMA sdk
  *
  * LzmaInputStream decompresses data from an underlying
  * ZeroCopyInputStream and provides the decompressed data as a
  * ZeroCopyInputStream.
  */
class LIBPROTOBUF_EXPORT LzmaInputStream : public ZeroCopyInputStream {
 public:
  /** @param sub_stream  Underlying ZeroCopyInputStream.
    * @param verify_data If set, data blocks are verified upon decompression,
    * using the block's SHA-256 digest.
    */
  explicit LzmaInputStream(
    ZeroCopyInputStream* sub_stream,
    bool verify_data=false
  );
  virtual ~LzmaInputStream();

  // implements ZeroCopyInputStream ----------------------------------
  bool Next(const void** data, int* size);
  void BackUp(int count);
  bool Skip(int count);
  int64 ByteCount() const;

 private:
  bool ReadNextBlock();

 private:
  CodedInputStream mWire;
  vector<uint8>    mBuffer;
  uint32           mPacketSize;
  uint32           mCurrentIndex;
  uint32           mByteCount;
  bool             mDataVerificationRequested;
  lzma_sha256*     mSha256;
  vector<uint8>    mPropsEncoded;
  bool             mFUBAR;

  GOOGLE_DISALLOW_EVIL_CONSTRUCTORS(LzmaInputStream);
};

/** LzmaOutputStream is an ZeroCopyOutputStream that compresses data to
  * an underlying ZeroCopyOutputStream.
  */
class LIBPROTOBUF_EXPORT LzmaOutputStream : public ZeroCopyOutputStream {
 public:
  /** @param sub_stream      Underlying ZeroCopyOutputStream.
    * @param max_packet_size Size in bytes of the input buffer
    *        (i.e. size of the uncompressed blocks). If none
    *        is provided, a good default value (1MB) is used.
    */
  explicit LzmaOutputStream(
    ZeroCopyOutputStream* sub_stream,
    uint32 max_packet_size = (1u << 20)
  );
  virtual ~LzmaOutputStream();

  /** This forces a flush of the current data down the
    * compression pipe. It is called automatically when
    * the input buffer is full as well as in the
    * destructor (to flush the last bits of remaining
    * data), but client code might want to call it
    * manually.
    *
    * @return true if data was successfully written, or
    *         false if an error occured.
    */
  bool Flush();

  /** Changes the current encoding settings.
    * This can be called anytime, since compression is
    * atomic. You could use fast settings to send
    * pre-compressed data on the wire, for instance,
    * then revert to aggressive compression settings.
    *
    * It is one case where you may want to call Flush()
    * manually (before changing the options).
    *
    * See Lzma.txt in the LZMA SDK for a detailed description
    * of the various settings.
    *
    * @return true if new options were accepted by the system,
    *         or false otherwise.
    */
  bool ChangeEncodingOptions(
    int      level=9,
    uint32   dictSize=(1u << 18),
    int      lc=0,
    int      lp=2,
    int      pb=2,
    int      algo=-1,
    int      fb=273,
    int      btMode=-1,
    int      numHashBytes=-1,
    uint32   mc=0,
    unsigned writeEndMark=0,
    int      numThreads=1
  );

  // implements ZeroCopyOutputStream ---------------------------------
  bool Next(void** data, int* size);
  void BackUp(int count);
  int64 ByteCount() const;

 private:
  ZeroCopyOutputStream* mSubStream;
  uint32                mPacketSize;
  uint32                mByteCount;
  uint32                mMaxPacketSize;
  uint32                mOffsetToUncompressedData;
  vector<uint8>         mBuffer;
  lzma_sha256*          mSha256;
  void*                 mEncoderHandle;
  vector<uint8>         mPropsEncoded;
  bool                  mPropsWritten;

  GOOGLE_DISALLOW_EVIL_CONSTRUCTORS(LzmaOutputStream);
};

} // namespace io
} // namespace protobuf
} // namespace google

/* vim: set sw=2 ts=2 sts=2 expandtab ff=unix: */
