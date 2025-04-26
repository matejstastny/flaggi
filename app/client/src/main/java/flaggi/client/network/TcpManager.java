/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 4/4/2025
 * Github link: https://github.com/kireiiiiiiii/flaggi
 */

package flaggi.client.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import flaggi.client.App;
import flaggi.proto.ClientMessages.ClientCommand;
import flaggi.proto.ClientMessages.ClientCommandType;
import flaggi.proto.ClientMessages.ClientHello;
import flaggi.proto.ClientMessages.ClientMessage;
import flaggi.proto.ClientMessages.Ping;
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
	private Socket socket;
	private InputStream in;
	private OutputStream out;

	// Constructor ---------------------------------------------------------------

	public TcpManager(String address, int port) {
		this.queue = new LinkedBlockingQueue<>();
		try {
			this.socket = new Socket(address, port);
			this.in = socket.getInputStream();
			this.out = socket.getOutputStream();
			Logger.log(LogLevel.INFO, "Connected to server at " + address + ":" + port);
		} catch (IOException e) {
			Logger.log(LogLevel.ERROR, "Failed to connect to server.", e);
			App.handleFatalError();
		}
	}

	// Listener ------------------------------------------------------------------

	@Override
	public void run() {
		try {
			while (!socket.isClosed()) {
				ServerMessage message = receiveMessage(this.in);
				queue.add(message);
			}
		} catch (IOException e) {
			Logger.log(LogLevel.WARN, "An error occurred while receiving message.", e);
			App.handleFatalError();
		}
	}

	// Public --------------------------------------------------------------------

	public void connect(String username) {
		Logger.log(LogLevel.INFO, "Connecting to server");
		ClientMessage message = ClientMessage.newBuilder().setClientHello(ClientHello.newBuilder().setUsername(username).build()).build();
		send(message);
	}

	public void commandServer(ClientCommandType type) {
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

	public ServerMessage poll() {
		return queue.poll();
	}

	public void send(ClientMessage message) {
		try {
			byte[] messageBytes = message.toByteArray();
			byte[] sizeBytes = ProtoUtil.intToByteArray(messageBytes.length);
			this.out.write(sizeBytes);
			this.out.write(messageBytes);
		} catch (IOException e) {
			Logger.log(LogLevel.WARN, "IOException occurred while sending message to server.", e);
		}
	}

	public static boolean isFlaggiServer(String address, int port) {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(address, port), 1000);
			socket.setSoTimeout(2000);

			InputStream in = socket.getInputStream();
			OutputStream out = socket.getOutputStream();
			ClientMessage ping = ClientMessage.newBuilder().setPing(Ping.newBuilder().setPing("ping")).build();

			byte[] bytes = ping.toByteArray();
			byte[] size = ProtoUtil.intToByteArray(bytes.length);
			out.write(size);
			out.write(bytes);

			size = new byte[4];
			in.read(size);
			int messageSize = ProtoUtil.byteArrayToInt(size);
			byte[] messageBytes = new byte[messageSize];
			in.read(messageBytes);
			return ServerMessage.parseFrom(messageBytes).hasPong();
		} catch (IOException e) {
			return false;
		}
	}

	// Private -------------------------------------------------------------------

	private ServerMessage receiveMessage(InputStream in) throws IOException {
		byte[] sizeBytes = new byte[4];
		in.read(sizeBytes);
		int messageSize = ProtoUtil.byteArrayToInt(sizeBytes);
		byte[] messageBytes = new byte[messageSize];
		in.read(messageBytes);
		return ServerMessage.parseFrom(messageBytes);
	}
}
