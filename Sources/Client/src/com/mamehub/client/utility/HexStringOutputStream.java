package com.mamehub.client.utility;

import java.io.IOException;
import java.io.OutputStream;

public class HexStringOutputStream extends OutputStream {
	//private final static Logger logger = Logger.getLogger(HexStringOutputStream.class.getName());
	
	StringBuilder builder;
	
	public HexStringOutputStream() {
		builder = new StringBuilder();
	}

	@Override
	public void write(int arg0) throws IOException {
		//logger.info("WRITING "+arg0);
		if (arg0<0) {
			arg0 += 256;
		}
		int highBits = (arg0 >> 4) % 16;
		writeHex(highBits);
		int lowBits = arg0 % 16;
		writeHex(lowBits);
	}

	@Override
	public String toString() {
		try {
			flush();
		} catch (IOException e) {
			throw new RuntimeException("OOPS");
		}
		return builder.toString();
	}

	private void writeHex(int bits) {
		switch(bits) {
		case 0: builder.append('0'); break;
		case 1: builder.append('1'); break;
		case 2: builder.append('2'); break;
		case 3: builder.append('3'); break;
		case 4: builder.append('4'); break;
		case 5: builder.append('5'); break;
		case 6: builder.append('6'); break;
		case 7: builder.append('7'); break;
		case 8: builder.append('8'); break;
		case 9: builder.append('9'); break;
		case 10: builder.append('a'); break;
		case 11: builder.append('b'); break;
		case 12: builder.append('c'); break;
		case 13: builder.append('d'); break;
		case 14: builder.append('e'); break;
		case 15: builder.append('f'); break;
		}
	}
}
