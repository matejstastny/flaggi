/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 11/4/2024 (v2 - 2/22/2025)
 * Github link: https://github.com/kireiiiiiiii/flaggi
 */

package flaggi.server;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import flaggi.proto.ClientMessages.ClientMessageWrapper;
import flaggi.proto.ClientMessages.ClientUpdate;
import flaggi.server.client.User;
import flaggi.server.common.TcpListener;
import flaggi.server.common.UdpListener;
import flaggi.server.common.UpdateLoop;
import flaggi.server.common.UpdateLoop.Updatable;
import flaggi.server.constants.Constants;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.util.FileUtil;

public class Server implements Updatable {

	private final ExecutorService threads;
	private final TcpListener tcpListener;
	private final UdpListener udpListener;
	private final UpdateLoop updateLoop;
	private final Map<String, User> users = new ConcurrentHashMap<>();
	private final BlockingQueue<ClientMessageWrapper> tcpMessageQueue = new LinkedBlockingQueue<>();
	private final BlockingQueue<ClientUpdate> udpPacketQueue = new LinkedBlockingQueue<>();

	// Main ---------------------------------------------------------------------

	public static void main(String[] args) {
		Server server = new Server();
		Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
	}

	public Server() {
		initializeLogger();
		buildInitFiles();
		this.tcpListener = new TcpListener(Constants.TCP_PORT, tcpMessageQueue, users);
		this.udpListener = new UdpListener(Constants.UDP_PORT, udpPacketQueue);
		this.updateLoop = new UpdateLoop(Constants.UPDATE_INTERVAL, this);
		this.threads = Executors.newFixedThreadPool(4);
		initializeThreads();
	}

	// Update -------------------------------------------------------------------

	@Override
	public void update() {
		// processTcpMessages();
		// processUdpPackets();
	}

	// Initialization -----------------------------------------------------------

	private void initializeLogger() {
		Logger.setLogFile(Constants.LOG_FILE);
		Logger.setLogLevelsToIgnore(Constants.IGNORED_LOG_LEVES);
		Logger.log(LogLevel.INFO, "Application start.");
	}

	private void initializeThreads() {
		threads.execute(this.tcpListener);
		threads.execute(this.udpListener);
		threads.execute(this.updateLoop);
	}

	private void buildInitFiles() {
		String dir = FileUtil.getJarExecDirectory() + File.separator;
		initializeFile("/licenses/LICENSE", System.getProperty("os.name").toLowerCase().contains("win") ? dir + "LICENSE.txt" : dir + "LICENSE");
		initializeFile("/docker/Dockerfile", dir + "Dockerfile");
	}

	// Static -------------------------------------------------------------------

	public static void handleFatalError() {
		Logger.log(LogLevel.ERROR, "FATAL ERROR DETECTED! SHUTTING DOWN...");
		System.exit(1);
	}

	// Private ------------------------------------------------------------------

	private void shutdown() {
		Logger.log(LogLevel.INFO, "Shutting down server...");
		threads.shutdown();
		try {
			if (!threads.awaitTermination(Constants.SERVER_SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS)) {
				threads.shutdownNow();
			}
		} catch (InterruptedException e) {
			threads.shutdownNow();
			Thread.currentThread().interrupt();
		}
		Logger.log(LogLevel.INFO, "Server shut down.");
	}

	private void initializeFile(String resourcePath, String targetPath) {
		try {
			FileUtil.copyResource(resourcePath, targetPath);
		} catch (IOException e) {
			Logger.log(LogLevel.ERROR, "Initialization failed for: " + resourcePath, e);
		}
	}

	// Network ------------------------------------------------------------------

	// private void processUdpPackets() {
	// ClientMessageWrapper message;
	// while ((message = udpPacketQueue.poll()) != null) {

	// }
	// }

	// private void processTcpMessages() {
	// ClientMessageWrapper message;
	// while ((message = tcpMessageQueue.poll()) != null) {
	// if (message.hasInitial()) {
	// handleNewUser(message);
	// } else if (message.hasRequest()) {
	// System.out.println("request");
	// }
	// }
	// }

	// // Message handeling --------------------------------------------------------

	// private void handleNewUser(WrapperMessage message) {
	// Logger.log(LogLevel.INFO, "New user connected.");
	// String uuid = UUID.randomUUID().toString();
	// User user = new User(uuid, message.getInitial().getUsername(), null, null);
	// // Update as needed
	// users.put(user.getUuid(), user);

	// InitialResponse response =
	// InitialResponse.newBuilder().setUuid(uuid).build();
	// user.sendMessage(WrapperMessage.newBuilder().setInitialResponse(response).build());
	// }

	// // Utility ------------------------------------------------------------------

	// public void sendMessageToUser(String uuid, WrapperMessage message) throws
	// Exception {
	// User user = users.get(uuid);
	// if (user != null) {
	// user.sendMessage(message);
	// }
	// }
}
