package com.mamehub.client.utility;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.tukaani.xz.FinishableOutputStream;
import org.tukaani.xz.FinishableWrapperOutputStream;
import org.tukaani.xz.LZMA2InputStream;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.LZMAInputStream;

import com.mamehub.client.utility.SevenZHeaderReader.Coder;

class Coders {
  static class LZMA2Decoder extends Coders.CoderBase {
    @Override
    InputStream decode(final InputStream in, final Coder coder, byte[] password)
        throws IOException {
      final int dictionarySizeBits = 0xff & coder.properties[0];
      if ((dictionarySizeBits & (~0x3f)) != 0) {
        throw new IOException("Unsupported LZMA2 property bits");
      }
      if (dictionarySizeBits > 40) {
        throw new IOException("Dictionary larger than 4GiB maximum size");
      }
      final int dictionarySize;
      if (dictionarySizeBits == 40) {
        dictionarySize = 0xFFFFffff;
      } else {
        dictionarySize = (2 | (dictionarySizeBits & 0x1)) << (dictionarySizeBits / 2 + 11);
      }
      return new LZMA2InputStream(in, dictionarySize);
    }

    @Override
    OutputStream encode(final OutputStream out, final byte[] password)
        throws IOException {
      LZMA2Options options = new LZMA2Options();
      options.setDictSize(LZMA2Options.DICT_SIZE_DEFAULT);
      FinishableOutputStream wrapped = new FinishableWrapperOutputStream(out);
      return new FinishOnCloseStream(options.getOutputStream(wrapped));
    }

    private static class FinishOnCloseStream extends FilterOutputStream {

      private FinishOnCloseStream(FinishableOutputStream out) {
        super(out);
      }

      @Override
      public void close() throws IOException {
        ((FinishableOutputStream) out).finish();
        super.close();
      }
    }
  }

  /**
   * The (partially) supported compression/encryption methods used in 7z
   * archives.
   */
  public enum SevenZMethod {
    /** no compression at all */
    COPY(new byte[] { (byte) 0x00 }),
    /** LZMA - only supported when reading */
    LZMA(new byte[] { (byte) 0x03, (byte) 0x01, (byte) 0x01 }),
    /** LZMA2 */
    LZMA2(new byte[] { (byte) 0x21 }) {
      @Override
      byte[] getProperties() {
        int dictSize = LZMA2Options.DICT_SIZE_DEFAULT;
        int lead = Integer.numberOfLeadingZeros(dictSize);
        int secondBit = (dictSize >>> (30 - lead)) - 2;
        return new byte[] { (byte) ((19 - lead) * 2 + secondBit) };
      }
    },
    /** Deflate */
    DEFLATE(new byte[] { (byte) 0x04, (byte) 0x01, (byte) 0x08 }),
    /** BZIP2 */
    BZIP2(new byte[] { (byte) 0x04, (byte) 0x02, (byte) 0x02 }),
    /**
     * AES encryption with a key length of 256 bit using SHA256 for hashes -
     * only supported when reading
     */
    AES256SHA256(new byte[] { (byte) 0x06, (byte) 0xf1, (byte) 0x07,
        (byte) 0x01 });

    private final byte[] id;

    private SevenZMethod(byte[] id) {
      this.id = id;
    }

    byte[] getId() {
      byte[] copy = new byte[id.length];
      System.arraycopy(id, 0, copy, 0, id.length);
      return copy;
    }

    byte[] getProperties() {
      return new byte[0];
    }

  }

  static InputStream addDecoder(final InputStream is, final Coder coder,
      final byte[] password) throws IOException {
    for (final CoderId coderId : coderTable) {
      if (Arrays.equals(coderId.method.getId(), coder.decompressionMethodId)) {
        return coderId.coder.decode(is, coder, password);
      }
    }
    throw new IOException("Unsupported compression method "
        + Arrays.toString(coder.decompressionMethodId));
  }

  static OutputStream addEncoder(final OutputStream out,
      final SevenZMethod method, final byte[] password) throws IOException {
    for (final CoderId coderId : coderTable) {
      if (coderId.method.equals(method)) {
        return coderId.coder.encode(out, password);
      }
    }
    throw new IOException("Unsupported compression method " + method);
  }

  static CoderId[] coderTable = new CoderId[] {
      new CoderId(SevenZMethod.COPY, new CopyDecoder()),
      new CoderId(SevenZMethod.LZMA, new LZMADecoder()),
      new CoderId(SevenZMethod.LZMA2, new LZMA2Decoder()),
      new CoderId(SevenZMethod.DEFLATE, new DeflateDecoder()),
      new CoderId(SevenZMethod.BZIP2, new BZIP2Decoder()),
      new CoderId(SevenZMethod.AES256SHA256, new AES256SHA256Decoder()) };

  static class CoderId {
    CoderId(SevenZMethod method, final CoderBase coder) {
      this.method = method;
      this.coder = coder;
    }

    final SevenZMethod method;
    final CoderBase coder;
  }

  static abstract class CoderBase {
    abstract InputStream decode(final InputStream in, final Coder coder,
        byte[] password) throws IOException;

    OutputStream encode(final OutputStream out, final byte[] password)
        throws IOException {
      throw new UnsupportedOperationException("method doesn't support writing");
    }
  }

  static class CopyDecoder extends CoderBase {
    @Override
    InputStream decode(final InputStream in, final Coder coder, byte[] password)
        throws IOException {
      return in;
    }

    @Override
    OutputStream encode(final OutputStream out, final byte[] password) {
      return out;
    }
  }

  static class LZMADecoder extends CoderBase {
    @Override
    InputStream decode(final InputStream in, final Coder coder, byte[] password)
        throws IOException {
      byte propsByte = coder.properties[0];
      long dictSize = coder.properties[1];
      for (int i = 1; i < 4; i++) {
        dictSize |= (coder.properties[i + 1] << (8 * i));
      }
      if (dictSize < 0) {
        System.out.println("DICT SIZE: " + dictSize);
      }
      if (dictSize > LZMAInputStream.DICT_SIZE_MAX) {
        throw new IOException("Dictionary larger than 4GiB maximum size");
      }
      return new LZMAInputStream(in, -1, propsByte, (int) dictSize);
    }
  }

  static class DeflateDecoder extends CoderBase {
    @Override
    InputStream decode(final InputStream in, final Coder coder,
        final byte[] password) throws IOException {
      return new InflaterInputStream(new DummyByteAddingInputStream(in),
          new Inflater(true));
    }

    @Override
    OutputStream encode(final OutputStream out, final byte[] password) {
      return new DeflaterOutputStream(out, new Deflater(9, true));
    }
  }

  static class BZIP2Decoder extends CoderBase {
    @Override
    InputStream decode(final InputStream in, final Coder coder,
        final byte[] password) throws IOException {
      return new BZip2CompressorInputStream(in);
    }

    @Override
    OutputStream encode(final OutputStream out, final byte[] password)
        throws IOException {
      return new BZip2CompressorOutputStream(out);
    }
  }

  static class AES256SHA256Decoder extends CoderBase {
    @Override
    InputStream decode(final InputStream in, final Coder coder,
        final byte[] passwordBytes) throws IOException {
      return new InputStream() {
        private boolean isInitialized = false;
        private CipherInputStream cipherInputStream = null;

        private CipherInputStream init() throws IOException {
          if (isInitialized) {
            return cipherInputStream;
          }
          final int byte0 = 0xff & coder.properties[0];
          final int numCyclesPower = byte0 & 0x3f;
          final int byte1 = 0xff & coder.properties[1];
          final int ivSize = ((byte0 >> 6) & 1) + (byte1 & 0x0f);
          final int saltSize = ((byte0 >> 7) & 1) + (byte1 >> 4);
          if (2 + saltSize + ivSize > coder.properties.length) {
            throw new IOException("Salt size + IV size too long");
          }
          final byte[] salt = new byte[saltSize];
          System.arraycopy(coder.properties, 2, salt, 0, saltSize);
          final byte[] iv = new byte[16];
          System.arraycopy(coder.properties, 2 + saltSize, iv, 0, ivSize);

          if (passwordBytes == null) {
            throw new IOException(
                "Cannot read encrypted files without a password");
          }
          final byte[] aesKeyBytes;
          if (numCyclesPower == 0x3f) {
            aesKeyBytes = new byte[32];
            System.arraycopy(salt, 0, aesKeyBytes, 0, saltSize);
            System.arraycopy(passwordBytes, 0, aesKeyBytes, saltSize,
                Math.min(passwordBytes.length, aesKeyBytes.length - saltSize));
          } else {
            final MessageDigest digest;
            try {
              digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
              IOException ioe = new IOException(
                  "SHA-256 is unsupported by your Java implementation");
              ioe.initCause(noSuchAlgorithmException);
              throw ioe;
              // TODO: simplify when Compress requires Java 1.6
              // throw new
              // IOException("SHA-256 is unsupported by your Java implementation",
              // noSuchAlgorithmException);
            }
            final byte[] extra = new byte[8];
            for (long j = 0; j < (1L << numCyclesPower); j++) {
              digest.update(salt);
              digest.update(passwordBytes);
              digest.update(extra);
              for (int k = 0; k < extra.length; k++) {
                ++extra[k];
                if (extra[k] != 0) {
                  break;
                }
              }
            }
            aesKeyBytes = digest.digest();
          }

          final SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");
          try {
            final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(iv));
            cipherInputStream = new CipherInputStream(in, cipher);
            isInitialized = true;
            return cipherInputStream;
          } catch (GeneralSecurityException generalSecurityException) {
            IOException ioe = new IOException(
                "Decryption error "
                    + "(do you have the JCE Unlimited Strength Jurisdiction Policy Files installed?)");
            ioe.initCause(generalSecurityException);
            throw ioe;
            // TODO: simplify when Compress requires Java 1.6
            // throw new IOException("Decryption error " +
            // "(do you have the JCE Unlimited Strength Jurisdiction Policy Files installed?)",
            // generalSecurityException);
          }
        }

        @Override
        public int read() throws IOException {
          return init().read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
          return init().read(b, off, len);
        }

        @Override
        public void close() {
        }
      };
    }
  }

  /**
   * ZLIB requires an extra dummy byte.
   *
   * @see java.util.zip.Inflater#Inflater(boolean)
   * @see org.apache.commons.compress.archivers.zip.ZipFile.BoundedInputStream
   */
  private static class DummyByteAddingInputStream extends FilterInputStream {
    private boolean addDummyByte = true;

    private DummyByteAddingInputStream(InputStream in) {
      super(in);
    }

    @Override
    public int read() throws IOException {
      int result = super.read();
      if (result == -1 && addDummyByte) {
        addDummyByte = false;
        result = 0;
      }
      return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      int result = super.read(b, off, len);
      if (result == -1 && addDummyByte) {
        addDummyByte = false;
        b[off] = 0;
        return 1;
      }
      return result;
    }
  }
}
