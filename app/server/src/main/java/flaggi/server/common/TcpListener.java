/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 2/23/2025
 * Github link: https://github.com/kireiiiiiiii/flaggi
 */

package flaggi.server.common;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import flaggi.server.client.ClientHandler;
import flaggi.proto.ClientMessages.ClientMessage;
import flaggi.server.Server;
import flaggi.server.client.Client;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;

public class TcpListener implements Runnable {

	private final int port;
	private final BlockingQueue<ClientMessage> messageQueue;
	private final Map<String, Client> clients;

	// Constructor --------------------------------------------------------------

	public TcpListener(int port, BlockingQueue<ClientMessage> messageQueue, Map<String, Client> clients) {
		this.port = port;
		this.messageQueue = messageQueue;
		this.clients = clients;
	}

	// Accesors -----------------------------------------------------------------

	public BlockingQueue<ClientMessage> getMessageQueue() {
		return messageQueue;
	}

	// Update -------------------------------------------------------------------

	@Override
	public void run() {
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			Logger.log(LogLevel.INFO, "TCP listener started on port " + port);
			while (true) {
				Socket clientSocket = serverSocket.accept();
				new Thread(new ClientHandler(clientSocket, messageQueue, clients), "Client Handler Thread").start();
			}
		} catch (Exception e) {
			Logger.log(LogLevel.ERROR, "An error occurred in the TCP listener.", e);
			Server.handleFatalError();
		}
	}
}
