/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 4/4/2025
 * Github link: https://github.com/kireiiiiiiii/flaggi
 */

package flaggi.client.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import flaggi.client.App;
import flaggi.proto.ClientMessages.ClientCommand;
import flaggi.proto.ClientMessages.ClientCommandType;
import flaggi.proto.ClientMessages.ClientHello;
import flaggi.proto.ClientMessages.ClientMessage;
import flaggi.proto.ServerMessages.ServerMessage;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.util.ProtoUtil;

/**
 * Manages TCP-based communication between the server and clients. Handles
 * sending and receiving messages, and manages the connection to the server.
 */
public class TcpManager implements Runnable {

	private final BlockingQueue<ServerMessage> queue;
	private final Runnable onDisconnect;
	private Socket socket;
	private InputStream in;
	private OutputStream out;

	// Constructor ---------------------------------------------------------------

	public TcpManager(String address, int port, Runnable onDisconnect) {
		this.queue = new LinkedBlockingQueue<>();
		this.onDisconnect = onDisconnect;
		try {
			this.socket = new Socket(address, port);
			this.in = socket.getInputStream();
			this.out = socket.getOutputStream();
			Logger.log(LogLevel.INFO, "Connected to server at " + address + ":" + port);
		} catch (IOException e) {
			Logger.log(LogLevel.ERROR, "Failed to connect to server", e);
			App.handleFatalError();
		}
	}

	// Listener ------------------------------------------------------------------

	@Override
	public void run() {
		try {
			ServerMessage msg;
			while ((msg = receiveMessage(this.in)) != null) {
				queue.add(msg);
			}
		} catch (IOException e) {
			Logger.log(LogLevel.ERROR, "An error occurred while receiving message.", e);
		} finally {
			close();
			Logger.log(LogLevel.DEBUG, "TcpManager stopped");
			if (onDisconnect != null) {
				onDisconnect.run();
			}
		}
	}

	// Public --------------------------------------------------------------------

	/**
	 * Connects to the server with the given username. Sends a greeting message to
	 * the server.
	 */
	public void connect(String username) {
		Logger.log(LogLevel.DEBUG, "Greeting server with username: " + username);
		ClientMessage message = ClientMessage.newBuilder().setClientHello(ClientHello.newBuilder().setUsername(username).build()).build();
		send(message);
	}

	public void sendCommandToServer(ClientCommandType type) {
		Logger.log(LogLevel.DEBUG, "Sending command to server: " + type);
		ClientMessage message = ClientMessage.newBuilder().setClientCommand(ClientCommand.newBuilder().setRequestType(type).build()).build();
		send(message);
	}

	public void close() {
		try {
			if (socket != null && !socket.isClosed()) {
				socket.close();
			}
		} catch (IOException e) {
			Logger.log(LogLevel.WARN, "Failed to close TCP manager.", e);
		}
	}

	/**
	 * Polls the queue for a message from the server. Returns null if no message is
	 * available.
	 *
	 * @return The next message from the server, or null if no message is available.
	 */
	public ServerMessage poll() {
		return queue.poll();
	}

	public void send(ClientMessage message) {
		try {
			byte[] messageBytes = message.toByteArray();
			byte[] sizeBytes = ProtoUtil.intToByteArray(messageBytes.length);
			out.write(sizeBytes);
			out.write(messageBytes);
			Logger.log(LogLevel.TCP, "Sent a message to server: \n\n" + message);
		} catch (IOException e) {
			Logger.log(LogLevel.WARN, "IOException occurred while sending message to server.", e);
		}
	}

	// Private -------------------------------------------------------------------

	private ServerMessage receiveMessage(InputStream in) throws IOException {
		byte[] sizeBytes = new byte[4];
		int readSize = 0;
		try {
			readSize = in.read(sizeBytes);
		} catch (SocketException e) {
			return null; // If disconnected
		}
		if (readSize == -1) { // Server closed connection
			return null;
		} else if (readSize < 4) {
			throw new IOException("Failed to read message size: only read " + readSize + " bytes.");
		}

		int messageSize = ProtoUtil.byteArrayToInt(sizeBytes);
		byte[] messageBytes = new byte[messageSize];
		int totalRead = 0;
		while (totalRead < messageSize) {
			int bytesRead = in.read(messageBytes, totalRead, messageSize - totalRead);
			if (bytesRead == -1) {
				throw new IOException("Connection closed while reading message data");
			}
			totalRead += bytesRead;
		}
		ServerMessage msg = ServerMessage.parseFrom(messageBytes);
		Logger.log(LogLevel.TCP, "Received a message from the server: \n\n" + msg);
		return msg;
	}
}
