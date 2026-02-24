// ------------------------------------------------------------------------------
// Server.java - Main server application class
// ------------------------------------------------------------------------------
// Author: Matej Stastny
// Date: 11-04-2024 (2.0: 02-22-2025) (MM-DD-YYYY)
// License: MIT
// Link: https://github.com/matejstastny/flaggi
// ------------------------------------------------------------------------------

package flaggi.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import flaggi.proto.ClientMessages.ClientCommand;
import flaggi.proto.ClientMessages.ClientInvite;
import flaggi.proto.ClientMessages.ClientInviteResponse;
import flaggi.proto.ClientMessages.ClientMessage;
import flaggi.proto.ClientMessages.ClientStateUpdate;
import flaggi.proto.ServerMessages.IdleClientList;
import flaggi.proto.ServerMessages.ServerInvite;
import flaggi.proto.ServerMessages.ServerMessage;
import flaggi.server.client.Client;
import flaggi.server.common.GameManager;
import flaggi.server.common.TcpListener;
import flaggi.server.common.UdpManager;
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
    private final UdpManager udpManager;
    private final UpdateLoop updateLoop;
    private final Map<String, Client> clients = new ConcurrentHashMap<>();
    private final Map<String, GameManager> activeGames = new ConcurrentHashMap<>();
    private final BlockingQueue<ClientMessage> tcpMessageQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<ClientStateUpdate> udpPacketQueue = new LinkedBlockingQueue<>();

    // Main ---------------------------------------------------------------------

    public static void main(String[] args) {
        Server server = new Server();
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
    }

    public Server() {
        initializeLogger();
        buildServerFiles();
        this.tcpListener = new TcpListener(Constants.TCP_PORT, tcpMessageQueue, clients);
        this.udpManager = new UdpManager(Constants.UDP_PORT, udpPacketQueue);
        this.updateLoop = new UpdateLoop(Constants.UPDATE_INTERVAL_MS);
        this.threads = Executors.newFixedThreadPool(4);
        this.updateLoop.add(this);
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
        Logger.log(LogLevel.INF, "Application start");
        Logger.log(LogLevel.INF, "Server IP: " + NetUtil.getLocalIPv4Address().getHostAddress());
        Logger.logMaxMemory("GB");
    }

    private void initializeThreads() {
        threads.execute(this.tcpListener);
        threads.execute(this.udpManager);
        threads.execute(this.updateLoop);
    }

    private void buildServerFiles() {
        Map<String, String> resources = Constants.SERVER_RESOURCES;
        for (String key : resources.keySet()) {
            try {
                FileUtil.copyResource(key, resources.get(key));
            } catch (IOException e) {
                Logger.log(LogLevel.ERR, "Initialization failed for: " + key, e);
            }
        }
    }

    // Static -------------------------------------------------------------------

    public static void handleFatalError() {
        Logger.log(LogLevel.ERR, "FATAL ERROR DETECTED! SHUTTING DOWN...");
        System.exit(1);
    }

    // Private ------------------------------------------------------------------

    private void shutdown() {
        Logger.log(LogLevel.INF, "Shutting down server...");
        threads.shutdown();
        try {
            if (!threads.awaitTermination(Constants.SERVER_SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS)) {
                threads.shutdownNow();
            }
        } catch (InterruptedException e) {
            threads.shutdownNow();
            Thread.currentThread().interrupt();
        }
        Logger.log(LogLevel.INF, "Server shut down.");
    }

    private Map<String, String> getIdleClients(String uuid) {
        Map<String, String> idleClients = new HashMap<>();
        for (Client client : clients.values()) {
            if (!client.uuid().equals(uuid)) {
                idleClients.put(client.uuid(), client.name());
            }
        }
        return idleClients;
    }

    // Network ------------------------------------------------------------------

    private void processUdpPackets() {
        ClientStateUpdate msg;
        while ((msg = udpPacketQueue.poll()) != null) {
            String uuid = msg.getGameUuid();
            if (activeGames.containsKey(uuid)) {
                if (activeGames.get(uuid) == null) {
                    Logger.log(LogLevel.WRN, "GameManager doesn't exist for game UUID: " + msg.getGameUuid());
                }
                Logger.log(LogLevel.UDP, "Recieved UDP client update:\n" + msg.toString());
                activeGames.get(uuid).addUpdate(msg);
            } else {
                Logger.log(LogLevel.WRN, "Received an UDP packet for an unknown game UUID: " + msg.getGameUuid());
            }
        }
    }

    private void processTcpMessages() {
        ClientMessage msg;
        while ((msg = tcpMessageQueue.poll()) != null) {
            if (msg.hasClientHello() || msg.hasPing()) {
                continue;
            } else if (msg.getUuid() == null || msg.getUuid().isEmpty() || !clients.keySet().contains(msg.getUuid())) {
                Logger.log(LogLevel.WRN, "Recieved message with invalid UUID: '" + msg.getUuid() + "'");
                continue;
            } else if (msg.hasClientCommand()) {
                processClientCommand(msg.getClientCommand(), msg.getUuid());
            } else if (msg.hasClientInvite()) {
                processClientInvite(msg.getClientInvite(), msg.getUuid());
            } else if (msg.hasClientInviteResponse()) {
                processClientInviteResponse(msg.getClientInviteResponse(), msg.getUuid());
            } else {
                Logger.log(LogLevel.WRN, "Polled an unknown message type: " + msg);
            }
        }
    }

    // TcpUpdate ----------------------------------------------------------------

    private void processClientCommand(ClientCommand msg, String uuid) {
        switch (msg.getRequestType()) {
        case GET_IDLE_CLIENT_LIST:
            Client client = clients.get(uuid);
            if (client == null) {
                Logger.log(LogLevel.WRN, "Client not found for UUID: '" + uuid + "' Possible UUID's: " + clients.keySet());
                return;
            }
            client.sendMessage(ServerMessage.newBuilder().setIdleClientList(IdleClientList.newBuilder().putAllClientList(getIdleClients(uuid))).build());
            break;
        default:
            Logger.log(LogLevel.WRN, "Unknown command type: " + msg.getRequestType());
            break;
        }
    }

    private void processClientInvite(ClientInvite invite, String uuid) {
        Client targetClient = clients.get(invite.getInvitee());
        targetClient.sendMessage(ServerMessage.newBuilder().setServerInvite(ServerInvite.newBuilder().setInviteeUuid(uuid)).build());
    }

    private void processClientInviteResponse(ClientInviteResponse response, String uuid) {
        if (response.getAccepted()) {
            Logger.log(LogLevel.DBG, "Client " + uuid + " accepted invite from " + response.getInvitee() + ". Entering game...");
            Client[] gameClients = { clients.get(response.getInvitee()), clients.get(uuid) };
            for (Client gameClient : gameClients) {
                if (gameClient == null) {
                    Logger.log(LogLevel.ERR, "One of the clients is null. Cannot create game.");
                    return;
                }
            }
            String gameUuid = UUID.randomUUID().toString();
            GameManager g = new GameManager(gameUuid, gameClients, activeGames, udpManager);
            this.activeGames.put(gameUuid, g);
            this.updateLoop.add(g);
            Logger.log(LogLevel.INF, "Game created with clients: " + gameClients[0].name() + " and " + gameClients[1].name());
            Logger.log(LogLevel.DBG, "Game UUID: " + gameUuid);
        }
    }
}
