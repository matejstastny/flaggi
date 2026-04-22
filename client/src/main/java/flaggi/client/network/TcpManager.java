package flaggi.client.network;

import flaggi.client.App;
import flaggi.proto.ClientMessages.ClientCommand;
import flaggi.proto.ClientMessages.ClientCommandType;
import flaggi.proto.ClientMessages.ClientHello;
import flaggi.proto.ClientMessages.ClientInvite;
import flaggi.proto.ClientMessages.ClientInviteResponse;
import flaggi.proto.ClientMessages.ClientMessage;
import flaggi.proto.ServerMessages.ServerMessage;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.util.ProtoUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Manages TCP-based communication between the server and clients. Handles sending and receiving
 * messages, and manages the connection to the server.
 */
public class TcpManager implements Runnable {

    private final BlockingQueue<ServerMessage> queue;
    private final Runnable onDisconnect;
    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private String uuid;

    // Constructor ---------------------------------------------------------------

    public TcpManager(String address, int port, Runnable onDisconnect) {
        this.queue = new LinkedBlockingQueue<>();
        this.onDisconnect = onDisconnect;
        try {
            this.socket = new Socket(address, port);
            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();
            Logger.log(LogLevel.INF, "Connected to server at " + address + ":" + port);
        } catch (IOException e) {
            Logger.log(LogLevel.ERR, "Failed to connect to server", e);
            App.handleFatalError();
        }
    }

    // Message Listener ----------------------------------------------------------

    @Override
    public void run() {
        try {
            ServerMessage msg;
            while ((msg = ProtoUtil.receiveServerMessage(this.in)) != null) {
                queue.add(msg);
            }
        } catch (IOException e) {
            Logger.log(LogLevel.ERR, "An error occurred while receiving message.", e);
        } finally {
            close();
            if (onDisconnect != null) {
                onDisconnect.run();
            }
        }
    }

    // Public --------------------------------------------------------------------

    /** Connects to the server with the given username. Sends a greeting message to the server. */
    public void connect(String username, int udpPort) {
        Logger.log(LogLevel.DBG, "Greeting server with username: " + username);
        ClientMessage message = ClientMessage.newBuilder()
                .setClientHello(ClientHello.newBuilder()
                        .setUsername(username)
                        .setUpdPort(udpPort)
                        .build())
                .build();
        ProtoUtil.sendClientMessage(message, out);
    }

    public void close() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            Logger.log(LogLevel.WRN, "Failed to close TCP manager.", e);
        }
    }

    /**
     * Polls the queue for a message from the server. Returns null if no message is available.
     *
     * @return The next message from the server, or null if no message is available.
     */
    public ServerMessage poll() {
        return queue.poll();
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getIP() {
        if (socket != null && socket.isConnected()) {
            return socket.getInetAddress().getHostAddress();
        }
        return null;
    }

    // Message senders -----------------------------------------------------------

    public void sendCommand(ClientCommandType type) {
        if (!validUuid("Cannot send command to server")) {
            return;
        }
        Logger.log(LogLevel.DBG, "Sending command to server: " + type);
        ClientMessage message = ClientMessage.newBuilder()
                .setClientCommand(
                        ClientCommand.newBuilder().setRequestType(type).build())
                .setUuid(uuid)
                .build();
        ProtoUtil.sendClientMessage(message, out);
    }

    public void sendInvite(String otherUuid) {
        if (!validUuid("Cannot invite player")) {
            return;
        }
        ClientMessage message = ClientMessage.newBuilder()
                .setUuid(uuid)
                .setClientInvite(ClientInvite.newBuilder().setInvitee(otherUuid))
                .build();
        ProtoUtil.sendClientMessage(message, out);
    }

    public void respondToInvite(String otherUuid, boolean accepted) {
        if (!validUuid("Cannot respond to invite")) {
            return;
        }
        ClientMessage message = ClientMessage.newBuilder()
                .setUuid(uuid)
                .setClientInviteResponse(
                        ClientInviteResponse.newBuilder().setInvitee(otherUuid).setAccepted(accepted))
                .build();
        ProtoUtil.sendClientMessage(message, out);
    }

    // Private -------------------------------------------------------------------

    private boolean validUuid(String errorMessage) {
        if (uuid == null || uuid.isEmpty()) {
            Logger.log(LogLevel.WRN, "UUID not set. " + errorMessage);
            return false;
        }
        return true;
    }
}
