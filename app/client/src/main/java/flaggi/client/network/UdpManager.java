package flaggi.client.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import flaggi.client.App;
import flaggi.client.constants.Constants;
import flaggi.proto.ClientMessages.ClientStateUpdate;
import flaggi.proto.ServerMessages.ServerStateUpdate;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;

public class UdpManager implements Runnable {

	private final BlockingQueue<ServerStateUpdate> queue;
	private DatagramSocket socket;
	private InetAddress address;
	private int port;

	// Constructor ---------------------------------------------------------------

	public UdpManager(String address, int port) {
		queue = new LinkedBlockingQueue<>();
		try {
			this.address = InetAddress.getByName(address);
			this.port = port;
			socket = new DatagramSocket();
			socket.setSoTimeout(Constants.SERVER_TIMEOUT_MS);
		} catch (SocketException | UnknownHostException e) {
			Logger.log(LogLevel.ERROR, "Failed to initialize UDP socket", e);
			App.handleFatalError();
		}
	}

	// Listener ------------------------------------------------------------------

	@Override
	public void run() {
		byte[] buffer = new byte[1024];
		while (!Thread.currentThread().isInterrupted()) {
			try {
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				socket.receive(packet);
				ServerStateUpdate update = ServerStateUpdate.parseFrom(packet.getData());
				queue.offer(update);
			} catch (IOException e) {
				if (Thread.currentThread().isInterrupted()) {
					Logger.log(LogLevel.INFO, "UDP listener thread interrupted, shutting down.");
					break;
				}
				Logger.log(LogLevel.WARN, "Error while receiving UDP packet: " + e.getMessage(), e);
			} catch (Exception e) {
				Logger.log(LogLevel.ERROR, "Unexpected error in UDP listener: " + e.getMessage(), e);
			}
		}
		close(); // Ensure socket is closed when the thread exits
	}

	// Public --------------------------------------------------------------------

	public void close() {
		if (socket != null && !socket.isClosed()) {
			socket.close();
			Logger.log(LogLevel.INFO, "UDP socket closed.");
		}
	}

	public ServerStateUpdate poll() {
		return queue.poll();
	}

	public void send(ClientStateUpdate message) {
		if (message == null) {
			throw new IllegalArgumentException("Message cannot be null");
		}
		try {
			byte[] messageBytes = message.toByteArray();
			DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, address, port);
			socket.send(packet);
			Logger.log(LogLevel.DEBUG, "Sent message to server: " + message);
		} catch (IOException e) {
			Logger.log(LogLevel.WARN, "IOException occurred while sending message to server: " + e.getMessage(), e);
		}
	}
}
