/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 2/23/2025
 * Github link: https://github.com/kireiiiiiiii
 */

package flaggi.shared.util;

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

}
