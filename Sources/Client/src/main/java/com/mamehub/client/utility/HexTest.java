package com.mamehub.client.utility;

import java.io.IOException;
import java.util.Random;

public class HexTest {

  /**
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    final int CHUNK_SIZE = 1024;
    final int NUM_CHUNKS = 1024;
    Random r = new Random();
    byte b[] = new byte[NUM_CHUNKS * CHUNK_SIZE];
    r.nextBytes(b);

    HexStringOutputStream hsos = null;
    HexStringInputStream hsis = null;
    try {
      hsos = new HexStringOutputStream();
      for (int a = 0; a < NUM_CHUNKS; a++) {
        hsos.write(b, a * CHUNK_SIZE, CHUNK_SIZE);
      }
      hsis = new HexStringInputStream(hsos.toString());
      byte b2[] = new byte[NUM_CHUNKS * CHUNK_SIZE];
      for (int a = 0; a < NUM_CHUNKS; a++) {
        System.out.println("ON CHUNK " + a);
        hsis.read(b2, a * CHUNK_SIZE, CHUNK_SIZE);

        for (int c = 0; c < a * CHUNK_SIZE; c++) {
          if (b[c] != b2[c]) {
            throw new RuntimeException("OOPS " + a + " " + c + ": " + b[c]
                + " " + b2[c]);
          }
        }
      }
      for (int c = 0; c < NUM_CHUNKS * CHUNK_SIZE; c++) {
        if (b[c] != b2[c]) {
          throw new RuntimeException("OOPS " + " " + c + ": " + b[c] + " "
              + b2[c]);
        }
      }
    } finally {
      hsos.close();
      hsis.close();
    }
  }

}
