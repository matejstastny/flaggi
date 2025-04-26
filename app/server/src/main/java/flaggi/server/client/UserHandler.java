/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 2/23/2025
 * Github link: https://github.com/kireiiiiiiii/flaggi
 */

package flaggi.server.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

import flaggi.proto.ClientMessages.ClientMessage;
import flaggi.proto.ServerMessages.Pong;
import flaggi.proto.ServerMessages.ServerHello;
import flaggi.proto.ServerMessages.ServerMessage;
import flaggi.server.constants.Constants;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.util.ProtoUtil;

public class UserHandler implements Runnable {

	private final Socket clientSocket;
	private final BlockingQueue<ClientMessage> messageQueue;
	private final Map<String, User> users;
	private User user;

	// Constructor --------------------------------------------------------------

	public UserHandler(Socket clientSocket, BlockingQueue<ClientMessage> messageQueue, Map<String, User> users) {
		this.clientSocket = clientSocket;
		this.messageQueue = messageQueue;
		this.users = users;
	}

	// Update -------------------------------------------------------------------

	@Override
	public void run() {
		try (InputStream in = clientSocket.getInputStream(); OutputStream out = clientSocket.getOutputStream()) {
			if (handleInitialMessage(in, out)) {
				while (!clientSocket.isClosed()) {
					ClientMessage message = receiveMessage(in);
					messageQueue.offer(message);
				}
			}
		} catch (SocketException e) {
		} catch (IOException e) {
			Logger.log(LogLevel.ERROR, "An IOException occurred in ClientHandler", e);
		} finally {
			if (user != null) {
				Logger.log(LogLevel.INFO, "Client disconnected: " + user.getName());
				users.remove(user.getUuid());
			}
		}
	}

	// Private ------------------------------------------------------------------

	private boolean handleInitialMessage(InputStream in, OutputStream out) throws IOException {
		ClientMessage message = receiveMessage(in);
		if (message.hasPing()) {
			Logger.log(LogLevel.DEBUG, "Recieved valid ping from a client");
			ServerMessage response = ServerMessage.newBuilder().setPong(Pong.newBuilder().setPong("pong")).build();
			sendMessage(out, response);
			return false;
		}
		if (!message.hasClientHello()) {
			clientSocket.close();
			Logger.log(LogLevel.WARN, "Invalid initial message from client. Closing connection");
			return false;
		}

		String username = message.getClientHello().getUsername();
		Logger.log(LogLevel.INFO, "New client connected: " + username + " (" + clientSocket.getInetAddress().getHostAddress() + ")");

		String uuid = UUID.randomUUID().toString();
		user = new User(uuid, username, clientSocket, out);
		users.put(uuid, user);

		ServerMessage response = ServerMessage.newBuilder().setServerHello(ServerHello.newBuilder().setUuid(uuid).setUdpPort(Constants.UDP_PORT).build()).build();
		sendMessage(out, response);
		messageQueue.offer(message);

		return true;
	}

	private ClientMessage receiveMessage(InputStream in) throws IOException {
		byte[] sizeBytes = new byte[4];
		in.read(sizeBytes);
		int messageSize = ProtoUtil.byteArrayToInt(sizeBytes);
		byte[] messageBytes = new byte[messageSize];
		in.read(messageBytes);
		ClientMessage msg = ClientMessage.parseFrom(messageBytes);
		Logger.log(LogLevel.TCP, "Received message from client: \n\n" + msg);
		return msg;
	}

	private void sendMessage(OutputStream out, ServerMessage message) throws IOException {
		byte[] messageBytes = message.toByteArray();
		byte[] sizeBytes = ProtoUtil.intToByteArray(messageBytes.length);
		out.write(sizeBytes);
		out.write(messageBytes);
		Logger.log(LogLevel.TCP, "Sent message to client: \n\n" + message);
	}
}
