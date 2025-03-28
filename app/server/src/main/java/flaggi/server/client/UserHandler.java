/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 2/23/2025
 * Github link: https://github.com/kireiiiiiiii/flaggi
 */

package flaggi.server.client;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

import flaggi.proto.ClientMessages.ClientMessageWrapper;
import flaggi.proto.ServerMessages.ServerInitialMessage;
import flaggi.proto.ServerMessages.ServerMessageWrapper;
import flaggi.server.constants.Constants;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.util.ProtoUtil;

public class UserHandler implements Runnable {

	private final Socket clientSocket;
	private final BlockingQueue<ClientMessageWrapper> messageQueue;
	private final Map<String, User> users;
	private User user;

	// Constructor --------------------------------------------------------------

	public UserHandler(Socket clientSocket, BlockingQueue<ClientMessageWrapper> messageQueue, Map<String, User> users) {
		this.clientSocket = clientSocket;
		this.messageQueue = messageQueue;
		this.users = users;
	}

	// Update -------------------------------------------------------------------

	@Override
	public void run() {
		try (InputStream in = clientSocket.getInputStream(); OutputStream out = clientSocket.getOutputStream()) {

			handleInitialMessage(in, out);

			while (true) {
				ClientMessageWrapper message = receiveMessage(in);
				messageQueue.offer(message);
			}

		} catch (Exception e) {
			Logger.log(LogLevel.ERROR, "An exception occurred in ClientHandler.", e);
		} finally {
			if (user != null) {
				users.remove(user.getUuid());
			}
		}
	}

	// Private ------------------------------------------------------------------

	private void handleInitialMessage(InputStream in, OutputStream out) throws Exception {
		ClientMessageWrapper message = receiveMessage(in);
		if (!message.hasClientInitialMessage()) {
			clientSocket.close();
			return;
		}

		String uuid = UUID.randomUUID().toString();
		user = new User(uuid, message.getClientInitialMessage().getUsername(), clientSocket, out);
		users.put(uuid, user);

		ServerMessageWrapper response = ServerMessageWrapper.newBuilder().setServerInitialMessage(ServerInitialMessage.newBuilder().setUuid(uuid).setUdpPort(Constants.UDP_PORT).build()).build();
		sendMessage(out, response);
		messageQueue.offer(message);
	}

	private ClientMessageWrapper receiveMessage(InputStream in) throws Exception {
		byte[] sizeBytes = new byte[4];
		in.read(sizeBytes);
		int messageSize = ProtoUtil.byteArrayToInt(sizeBytes);
		byte[] messageBytes = new byte[messageSize];
		in.read(messageBytes);

		return ClientMessageWrapper.parseFrom(messageBytes);
	}

	private void sendMessage(OutputStream out, ServerMessageWrapper message) throws Exception {
		byte[] messageBytes = message.toByteArray();
		byte[] sizeBytes = ProtoUtil.intToByteArray(messageBytes.length);
		out.write(sizeBytes);
		out.write(messageBytes);
	}
}
