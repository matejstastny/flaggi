/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 11/4/2024 (v2 - 2/22/2025)
 * Github link: https://github.com/kireiiiiiiii/flaggi
 */

package flaggi.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import flaggi.proto.ClientMessages.ClientCommand;
import flaggi.proto.ClientMessages.ClientMessage;
import flaggi.proto.ClientMessages.ClientStateUpdate;
import flaggi.proto.ServerMessages.IdleClientList;
import flaggi.proto.ServerMessages.ServerMessage;
import flaggi.server.client.User;
import flaggi.server.common.TcpListener;
import flaggi.server.common.UdpListener;
import flaggi.server.constants.Constants;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.common.UpdateLoop;
import flaggi.shared.common.UpdateLoop.Updatable;
import flaggi.shared.util.FileUtil;
import flaggi.shared.util.NetUtil;

public class Server implements Updatable {

	private final ExecutorService threads;
	private final TcpListener tcpListener;
	private final UdpListener udpListener;
	private final UpdateLoop updateLoop;
	private final Map<String, User> users = new ConcurrentHashMap<>();
	private final BlockingQueue<ClientMessage> tcpMessageQueue = new LinkedBlockingQueue<>();
	private final BlockingQueue<ClientStateUpdate> udpPacketQueue = new LinkedBlockingQueue<>();

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
		this.updateLoop = new UpdateLoop(Constants.UPDATE_INTERVAL_MS);
		this.updateLoop.add(this);
		this.threads = Executors.newFixedThreadPool(4);
		initializeThreads();
	}

	// Update -------------------------------------------------------------------

	@Override
	public void update() {
		processTcpMessages();
		processUdpPackets();
	}

	// Initialization -----------------------------------------------------------

	private void initializeLogger() {
		Logger.setLogFile(Constants.LOG_FILE);
		Logger.setLogLevelsToIgnore(Constants.IGNORED_LOG_LEVES);
		Logger.log(LogLevel.INFO, "Application start.");
		Logger.log(LogLevel.INFO, "Server IP: " + NetUtil.getLocalIPv4Address().getHostAddress());
		Logger.logMaxMemory("GB");
	}

	private void initializeThreads() {
		threads.execute(this.tcpListener);
		threads.execute(this.udpListener);
		threads.execute(this.updateLoop);
	}

	private void buildInitFiles() {
		Map<String, String> resources = Constants.SERVER_RESOURCES;
		for (String key : resources.keySet()) {
			try {
				FileUtil.copyResource(key, resources.get(key));
			} catch (IOException e) {
				Logger.log(LogLevel.ERROR, "Initialization failed for: " + key, e);
			}
		}
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

	private Map<String, String> getIdleClients(String uuid) {
		Map<String, String> idleClients = new HashMap<>();
		for (User user : users.values()) {
			if (!user.getUuid().equals(uuid)) {
				idleClients.put(user.getUuid(), user.getName());
			}
		}
		return idleClients;
	}

	// Network ------------------------------------------------------------------

	private void processUdpPackets() {
		ClientStateUpdate message;
		while ((message = udpPacketQueue.poll()) != null) {

		}
	}

	private void processTcpMessages() {
		ClientMessage msg;
		while ((msg = tcpMessageQueue.poll()) != null) {
			if (msg.hasClientHello() || msg.hasPing()) {
				continue;
			} else if (msg.hasClientCommand()) {
				processClientCommand(msg.getClientCommand(), msg.getUuid());
			} else {
				Logger.log(LogLevel.WARN, "Polled an unknown message type: " + msg);
			}
		}
	}

	// TcpUpdate ----------------------------------------------------------------

	private void processClientCommand(ClientCommand msg, String uuid) {
		switch (msg.getRequestType()) {
		case GET_IDLE_CLIENT_LIST:
			User user = users.get(uuid);
			if (user == null) {
				Logger.log(LogLevel.WARN, "User not found for UUID: '" + msg.getUuid() + "' Possible UUID's: " + users.keySet());
				return;
			}
			user.sendMessage(ServerMessage.newBuilder().setIdleClientList(IdleClientList.newBuilder().putAllClientList(getIdleClients(uuid))).build());
			break;
		default:
			Logger.log(LogLevel.WARN, "Unknown command type: " + msg.getRequestType());
			break;
		}
	}

}
