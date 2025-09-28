// ------------------------------------------------------------------------------
// ProtoUtil.java - description TODO
// ------------------------------------------------------------------------------
// Author: Matej Stastny
// Date: 02-23-2025 (MM-DD-YYYY)
// License: MIT
// Link: https://github.com/matejstastny/flaggi
// ------------------------------------------------------------------------------

package flaggi.shared.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
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
			DataOutputStream dataOut = new DataOutputStream(out);
			byte[] messageBytes = message.toByteArray();
			dataOut.writeInt(messageBytes.length);
			dataOut.write(messageBytes);
			Logger.log(LogLevel.TCP, "Sent a message to server:\n\n" + message);
		} catch (IOException e) {
			Logger.log(LogLevel.WARN, "IOException occurred while sending message to server.", e);
		}
	}

	// Server side ---------------------------------------------------------------

	public static ClientMessage receiveClientMessage(InputStream in) throws IOException {
		DataInputStream dataIn = new DataInputStream(in);

		int messageSize;
		try {
			messageSize = dataIn.readInt();
		} catch (EOFException | SocketException e) {
			return null; // Disconnected or stream closed
		}

		if (messageSize < 0 || messageSize > MAX_MESSAGE_SIZE) {
			throw new IOException("Invalid or too large message size: " + messageSize);
		}

		byte[] messageBytes = new byte[messageSize];
		dataIn.readFully(messageBytes);

		ClientMessage msg = ClientMessage.parseFrom(messageBytes);
		Logger.log(LogLevel.TCP, "Received message from client:\n\n" + msg);
		return msg;
	}

	public static void sendServerMessage(OutputStream out, ServerMessage message) throws IOException {
		DataOutputStream dataOut = new DataOutputStream(out);
		byte[] messageBytes = message.toByteArray();
		dataOut.writeInt(messageBytes.length);
		dataOut.write(messageBytes);
		Logger.log(LogLevel.TCP, "Sent message to client:\n\n" + message);
	}
}
