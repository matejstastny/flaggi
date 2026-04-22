package flaggi.client.network;

import flaggi.client.App;
import flaggi.client.constants.Constants;
import flaggi.proto.ClientMessages.ClientStateUpdate;
import flaggi.proto.ServerMessages.ServerStateUpdate;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class UdpManager implements Runnable {

  private int port;
  private InetAddress address;
  private DatagramSocket socket;
  private volatile ServerStateUpdate latestUpdate;

  // Constructor ---------------------------------------------------------------

  public UdpManager() {
    try {
      this.port = -1;
      this.address = null;
      socket = new DatagramSocket();
      InetAddress addr = socket.getLocalAddress();
      if (addr == null || addr.isAnyLocalAddress()) {
        addr = InetAddress.getLoopbackAddress();
      }
      Logger.log(
          LogLevel.DBG,
          "Datagram socket created on " + addr.getHostAddress() + ":" + socket.getLocalPort());
      socket.setSoTimeout(Constants.SERVER_TIMEOUT_MS);
    } catch (SocketException e) {
      Logger.log(LogLevel.ERR, "Failed to initialize Datagram Socket", e);
      App.handleFatalError();
    }
  }

  // Listener ------------------------------------------------------------------

  @Override
  public void run() {
    Logger.log(LogLevel.DBG, "Listening for UDP at port " + socket.getLocalPort());
    byte[] buffer = new byte[1024];
    while (!Thread.currentThread().isInterrupted()) {
      try {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        ServerStateUpdate update =
            ServerStateUpdate.parseFrom(
                new ByteArrayInputStream(packet.getData(), 0, packet.getLength()));
        Logger.log(LogLevel.UDP, "Received server update:\n" + update.toString());
        this.latestUpdate = update;
      } catch (SocketTimeoutException e) {
      } catch (SocketException e) {
        break;
      } catch (IOException e) {
        if (Thread.currentThread().isInterrupted()) {
          Logger.log(LogLevel.INF, "UDP listener thread interrupted, shutting down.");
          break;
        }
        Logger.log(LogLevel.WRN, "Error while receiving UDP packet: " + e.getMessage(), e);
      } catch (Exception e) {
        Logger.log(LogLevel.ERR, "Unexpected error in UDP listener: " + e.getMessage(), e);
      }
    }
    close();
  }

  // Public --------------------------------------------------------------------

  public void close() {
    if (socket != null || !socket.isClosed()) {
      socket.close();
      Logger.log(LogLevel.INF, "UDP socket closed");
    }
  }

  public void setAdress(String address, int port) {
    try {
      this.address = InetAddress.getByName(address);
    } catch (UnknownHostException e) {
      Logger.log(LogLevel.ERR, "Invalid inet address passed to Udp Manager", e);
      App.handleFatalError();
    }
    this.port = port;
  }

  public ServerStateUpdate getLatestUpdate() {
    return this.latestUpdate;
  }

  public void send(ClientStateUpdate message) {
    if (message == null) {
      throw new IllegalArgumentException("Message cannot be null");
    } else if (this.port == 0 || this.address == null) {
      Logger.log(LogLevel.WRN, "Cannot send UDP message, address and/or port not set");
      return;
    }
    try {
      byte[] messageBytes = message.toByteArray();
      DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, address, port);
      socket.send(packet);
      Logger.log(LogLevel.UDP, "Sent UDP ClientUpdate to server:\n" + message);
    } catch (IOException e) {
      Logger.log(
          LogLevel.WRN,
          "IOException occurred while sending message to server: " + e.getMessage(),
          e);
    }
  }

  public int listenerPort() {
    return socket.getLocalPort();
  }
}
