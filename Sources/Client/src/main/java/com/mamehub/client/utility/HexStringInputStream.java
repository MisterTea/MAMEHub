package com.mamehub.client.utility;

import java.io.IOException;
import java.io.InputStream;

public class HexStringInputStream extends InputStream {
  // private final static Logger logger =
  // Logger.getLogger(HexStringInputStream.class.getName());
  String s;
  int pos;

  public HexStringInputStream(String s) {
    this.s = s;
    this.pos = 0;
  }

  @Override
  public int read() throws IOException {
    if (s.length() <= pos) {
      return -1;
    }

    int retval = (readHex(s.charAt(pos)) << 4) | readHex(s.charAt(pos + 1));
    pos += 2;
    // logger.info("READING "+retval);
    return retval;
  }

  private int readHex(char hex) throws IOException {
    switch (hex) {
    case '0':
      return 0;
    case '1':
      return 1;
    case '2':
      return 2;
    case '3':
      return 3;
    case '4':
      return 4;
    case '5':
      return 5;
    case '6':
      return 6;
    case '7':
      return 7;
    case '8':
      return 8;
    case '9':
      return 9;
    case 'a':
      return 10;
    case 'A':
      return 10;
    case 'b':
      return 11;
    case 'B':
      return 11;
    case 'c':
      return 12;
    case 'C':
      return 12;
    case 'd':
      return 13;
    case 'D':
      return 13;
    case 'e':
      return 14;
    case 'E':
      return 14;
    case 'f':
      return 15;
    case 'F':
      return 15;
    default:
      throw new IOException("Out of range error reading hex string");
    }
  }
}
