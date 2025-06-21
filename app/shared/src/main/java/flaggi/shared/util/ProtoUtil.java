/*
 * Author: Matěj Šťastný aka matysta
 * Date created: 2/23/2025
 * GitHub link: https://github.com/matysta/flaggi
 */

package flaggi.shared.util;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

public class ProtoUtil {

	// Private constructor to prevent instantiation
	private ProtoUtil() {
		throw new UnsupportedOperationException("ProtoUtil is a utility class and cannot be instantiated.");
	}

	// Methods -------------------------------------------------------------------

	public static byte[] intToByteArray(int value) {
		return new byte[] { (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value };
	}

	public static int byteArrayToInt(byte[] bytes) {
		return (bytes[0] << 24) | (bytes[1] << 16) | (bytes[2] << 8) | (bytes[3]);
	}

	public static byte[] readExactly(DataInputStream in, int len) throws IOException {
		byte[] data = new byte[len];
		int totalRead = 0;
		while (totalRead < len) {
			int bytesRead = in.read(data, totalRead, len - totalRead);
			if (bytesRead == -1) {
				throw new EOFException("Stream ended before reading enough bytes");
			}
			totalRead += bytesRead;
		}
		return data;
	}

}
