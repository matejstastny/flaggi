// ------------------------------------------------------------------------------
// UdpListener.java - UDP listener for receiving client state updates
// ------------------------------------------------------------------------------
// Author: Matej Stastny
// Date: 02-23-2025 (MM-DD-YYYY)
// License: MIT
// Link: https://github.com/matejstastny/flaggi
// ------------------------------------------------------------------------------

package flaggi.server.common;

import flaggi.proto.ClientMessages.ClientStateUpdate;
import flaggi.proto.ServerMessages.ServerStateUpdate;
import flaggi.server.Server;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class UdpManager implements Runnable {

  private final int port;
  private final DatagramSocket socket;
  private final BlockingQueue<ClientStateUpdate> messageQueue;
  private final PacketRateLimiter rateLimiter =
      new PacketRateLimiter(TimeUnit.MILLISECONDS.toMillis(16));

  // Constructor --------------------------------------------------------------

  public UdpManager(int port, BlockingQueue<ClientStateUpdate> messageQueue) {
    DatagramSocket tempSocket = null;
    this.port = port;
    this.messageQueue = messageQueue;
    try {
      tempSocket = new DatagramSocket(port);
    } catch (SocketException e) {
      Logger.log(LogLevel.ERR, "Failed to initialize Datagram Socket", e);
      Server.handleFatalError();
    }
    this.socket = tempSocket;
  }

  @Override
  public void run() {
    Logger.log(LogLevel.INF, "UDP listener started on port " + port);
    byte[] buffer = new byte[1024];
    while (true) {
      DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
      try {
        socket.receive(packet);
      } catch (Exception e) {
        Logger.log(LogLevel.ERR, "An error occurred in UdpListener.", e);
        Server.handleFatalError();
        return;
      }
      if (rateLimiter.shouldProcessPacket(packet)) {
        processPacket(packet);
      }
    }
  }

  // Public -------------------------------------------------------------------

  public void close() {
    if (socket != null || socket.isClosed()) {
      socket.close();
      Logger.log(LogLevel.INF, "UDP socket closed");
    }
  }

  public void send(ServerStateUpdate message, InetAddress address, int port) {
    if (message == null) {
      throw new IllegalArgumentException("Message cannot be null");
    }
    try {
      byte[] messageBytes = message.toByteArray();
      DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, address, port);
      socket.send(packet);
      Logger.log(
          LogLevel.UDP,
          "Sent UDP ServerUpdate to client on " + address + ":" + port + ": \n" + message);
    } catch (IOException e) {
      Logger.log(
          LogLevel.WRN,
          "IOException occurred while sending message to server: " + e.getMessage(),
          e);
    }
  }

  // Private ------------------------------------------------------------------

  private void processPacket(DatagramPacket packet) {
    try {
      ClientStateUpdate message =
          ClientStateUpdate.parseFrom(
              new ByteArrayInputStream(packet.getData(), 0, packet.getLength()));
      if (message == null) {
        Logger.log(LogLevel.WRN, "Received null ClientStateUpdate from " + packet.getAddress());
        return;
      }
      messageQueue.offer(message);
    } catch (Exception e) {
      Logger.log(
          LogLevel.WRN,
          "Failed to process UDP packet from " + packet.getAddress() + ":" + packet.getPort(),
          e);
    }
  }

  // Limiter ------------------------------------------------------------------

  /** Limits the rate at what UDP packets from clients can be accepted. */
  private static class PacketRateLimiter {
    private final Map<InetAddress, Long> lastPacketTimes = new ConcurrentHashMap<>();
    private final long minIntervalMillis;

    public PacketRateLimiter(long minIntervalMillis) {
      this.minIntervalMillis = minIntervalMillis;
    }

    public boolean shouldProcessPacket(DatagramPacket packet) {
      InetAddress address = packet.getAddress();
      long currentTime = System.currentTimeMillis();
      long lastTime = lastPacketTimes.getOrDefault(address, 0L);

      if (currentTime - lastTime > minIntervalMillis) {
        lastPacketTimes.put(address, currentTime);
        return true;
      } else {
        return false;
      }
    }
  }
}
