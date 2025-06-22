/*
 * Author: Matěj Šťastný aka matysta
 * Date created: 2/23/2025
 * GitHub link: https://github.com/matysta/flaggi
 */

package flaggi.shared.util;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;

import flaggi.proto.ClientMessages.ClientMessage;
import flaggi.proto.ServerMessages.ServerMessage;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;

public class ProtoUtil {

	private static final int MAX_MESSAGE_SIZE = 10 * 1024 * 1024; // 10 MB safety limit

	// Private constructor to prevent instantiation
	private ProtoUtil() {
		throw new UnsupportedOperationException("ProtoUtil is a utility class and cannot be instantiated.");
	}

	// Client side ---------------------------------------------------------------

	public static ServerMessage receiveServerMessage(InputStream in) throws IOException {
		DataInputStream dataIn = new DataInputStream(in);

		int messageSize;
		try {
			messageSize = dataIn.readInt(); // Reads exactly 4 bytes, big-endian
		} catch (EOFException | SocketException e) {
			return null; // Disconnected or stream closed
		}

		if (messageSize < 0 || messageSize > MAX_MESSAGE_SIZE) {
			throw new IOException("Invalid or too large message size: " + messageSize);
		}

		byte[] messageBytes = new byte[messageSize];
		dataIn.readFully(messageBytes); // Blocks until full message is read or throws if failed

		ServerMessage msg = ServerMessage.parseFrom(messageBytes);
		Logger.log(LogLevel.TCP, "Received a message from the server:\n\n" + msg);
		return msg;
	}

	public static void sendClientMessage(ClientMessage message, OutputStream out) {
		try {
			byte[] messageBytes = message.toByteArray();
			byte[] sizeBytes = intToByteArray(messageBytes.length);
			out.write(sizeBytes);
			out.write(messageBytes);
			Logger.log(LogLevel.TCP, "Sent a message to server: \n\n" + message);
		} catch (IOException e) {
			Logger.log(LogLevel.WARN, "IOException occurred while sending message to server.", e);
		}
	}

	// Server side ---------------------------------------------------------------

	public static ClientMessage receiveClientMessage(InputStream in) throws IOException {
		byte[] sizeBytes = new byte[4];
		int readSize = in.read(sizeBytes);
		if (readSize == -1) {
			return null;
		}
		if (readSize < 4) {
			throw new IOException("Failed to read message size: only read " + readSize + " bytes.");
		}

		int messageSize = ProtoUtil.byteArrayToInt(sizeBytes);
		byte[] messageBytes = new byte[messageSize];

		int totalRead = 0;
		while (totalRead < messageSize) {
			int bytesRead = in.read(messageBytes, totalRead, messageSize - totalRead);
			if (bytesRead == -1) {
				throw new IOException("Connection closed while reading message body");
			}
			totalRead += bytesRead;
		}

		ClientMessage msg = ClientMessage.parseFrom(messageBytes);
		Logger.log(LogLevel.TCP, "Received message from client: \n\n" + msg);
		return msg;
	}

	public static void sendServerMessage(OutputStream out, ServerMessage message) throws IOException {
		byte[] messageBytes = message.toByteArray();
		byte[] sizeBytes = intToByteArray(messageBytes.length);
		out.write(sizeBytes);
		out.write(messageBytes);
		Logger.log(LogLevel.TCP, "Sent message to client: \n\n" + message);
	}

	// Conversion ----------------------------------------------------------------

	public static byte[] intToByteArray(int value) {
		return new byte[] { (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value };
	}

	public static int byteArrayToInt(byte[] bytes) {
		return (bytes[0] << 24) | (bytes[1] << 16) | (bytes[2] << 8) | (bytes[3]);
	}
}
