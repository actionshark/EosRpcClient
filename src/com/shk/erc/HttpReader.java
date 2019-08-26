package com.shk.erc;

import java.io.InputStream;

import com.shk.js.log.Level;
import com.shk.js.log.Logger;

public class HttpReader {
	private static final String CONTENT_LENGTH = "Content-Length: ";
	private static final String SEPARATION = "\r\n";
	private static final String SECTION = "\r\n\r\n";

	private InputStream mInputStream;

	public HttpReader(InputStream inputStream) {
		mInputStream = inputStream;
	}

	public byte[] read() {
		byte[] buffer = new byte[1024 * 1024 * 16];
		int offset = 0;

		int contentLength = -1;
		int sectionIndex = -1;

		while (true) {
			int length = 0;
			try {
				length = mInputStream.read(buffer, offset, buffer.length - offset);
			} catch (Exception e) {
				Logger.print(Level.E, e);
				break;
			}

			offset += length;

			if (sectionIndex == -1) {
				String string = new String(buffer, 0, offset);

				if (contentLength == -1) {
					int beginIndex = string.indexOf(CONTENT_LENGTH);
					if (beginIndex != -1) {
						beginIndex += CONTENT_LENGTH.length();

						int endIndex = string.indexOf(SEPARATION, beginIndex);
						if (endIndex != -1) {
							String temp = string.substring(beginIndex, endIndex);
							try {
								contentLength = Integer.parseInt(temp);
							} catch (Exception e) {
								Logger.print(Level.E, e);
							}
						}
					}
				}

				sectionIndex = string.indexOf(SECTION);
			}

			if (sectionIndex != -1 && offset >= sectionIndex + SECTION.length() + contentLength) {
				break;
			}
		}

		byte[] bs = new byte[offset];

		for (int i = 0; i < offset; i++) {
			bs[i] = buffer[i];
		}

		return bs;
	}
}
