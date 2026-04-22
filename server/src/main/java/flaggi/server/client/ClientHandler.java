// ------------------------------------------------------------------------------
// ClientHandler.java - Handles communication with a connected client
// ------------------------------------------------------------------------------
// Author: Matej Stastny
// Date: 02-23-2025 (MM-DD-YYYY)
// License: MIT
// Link: https://github.com/matejstastny/flaggi
// ------------------------------------------------------------------------------

package flaggi.server.client;

import flaggi.proto.ClientMessages.ClientMessage;
import flaggi.proto.ServerMessages.Pong;
import flaggi.proto.ServerMessages.ServerHello;
import flaggi.proto.ServerMessages.ServerMessage;
import flaggi.server.constants.Constants;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.util.ProtoUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

public class ClientHandler implements Runnable {

  private final Socket clientSocket;
  private final BlockingQueue<ClientMessage> messageQueue;
  private final Map<String, Client> clients;
  private Client client;

  // Constructor --------------------------------------------------------------

  public ClientHandler(
      Socket clientSocket, BlockingQueue<ClientMessage> messageQueue, Map<String, Client> clients) {
    this.clientSocket = clientSocket;
    this.messageQueue = messageQueue;
    this.clients = clients;
  }

  // Update -------------------------------------------------------------------

  @Override
  public void run() {
    try (InputStream in = clientSocket.getInputStream();
        OutputStream out = clientSocket.getOutputStream()) {
      if (handleInitialMessage(in, out)) {
        while (!clientSocket.isClosed()) {
          ClientMessage message = ProtoUtil.receiveClientMessage(in);
          if (message == null) {
            break;
          }
          messageQueue.offer(message);
        }
      }
    } catch (SocketException e) {
      Logger.log(LogLevel.WRN, "SocketException occurred in ClientHandler", e);
    } catch (IOException e) {
      Logger.log(LogLevel.ERR, "An IOException occurred in ClientHandler", e);
    } finally {
      if (client != null) {
        Logger.log(LogLevel.INF, "Client disconnected: " + client.username());
        clients.remove(client.uuid());
      }
      try {
        clientSocket.close();
      } catch (IOException e) {
        Logger.log(LogLevel.WRN, "Failed to close client socket", e);
      }
    }
  }

  // Private ------------------------------------------------------------------

  private boolean handleInitialMessage(InputStream in, OutputStream out) throws IOException {
    ClientMessage message = ProtoUtil.receiveClientMessage(in);
    if (message.hasPing()) {
      Logger.log(LogLevel.DBG, "Recieved valid ping from a client");
      ServerMessage response =
          ServerMessage.newBuilder().setPong(Pong.newBuilder().setPong("pong")).build();
      ProtoUtil.sendServerMessage(out, response);
      return false;
    }
    if (!message.hasClientHello()) {
      clientSocket.close();
      Logger.log(LogLevel.WRN, "Invalid initial message from client. Closing connection");
      return false;
    }

    String username = message.getClientHello().getUsername();
    int clientUdpPort = message.getClientHello().getUpdPort();
    Logger.log(
        LogLevel.INF,
        "New client connected: "
            + username
            + " ("
            + clientSocket.getInetAddress().getHostAddress()
            + ")");

    String uuid = UUID.randomUUID().toString();
    client = new Client(uuid, username, clientSocket, out, clientUdpPort);
    clients.put(uuid, client);
    Logger.log(LogLevel.DBG, "Asigned UUID " + uuid + " to client " + username);

    ServerMessage response =
        ServerMessage.newBuilder()
            .setServerHello(
                ServerHello.newBuilder().setUuid(uuid).setUdpPort(Constants.UDP_PORT).build())
            .build();
    ProtoUtil.sendServerMessage(out, response);
    messageQueue.offer(message);

    return true;
  }
}
