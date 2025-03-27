/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 2/23/2025
 * Github link: https://github.com/kireiiiiiiii/flaggi
 */

package flaggi.server.common;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.InvalidProtocolBufferException;

import flaggi.proto.ClientMessages.ClientUpdate;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;

public class UdpListener implements Runnable {

    private final int port;
    private final BlockingQueue<ClientUpdate> messageQueue;
    private final PacketRateLimiter rateLimiter = new PacketRateLimiter(TimeUnit.MILLISECONDS.toMillis(50));

    // Constructor --------------------------------------------------------------

    public UdpListener(int port, BlockingQueue<ClientUpdate> messageQueue) {
        this.port = port;
        this.messageQueue = messageQueue;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                if (rateLimiter.shouldProcessPacket(packet)) {
                    processPacket(packet);
                }
            }
        } catch (Exception e) {
            Logger.log(LogLevel.ERROR, "An error occurred in UdpListener.", e);
        }
    }

    // Private ------------------------------------------------------------------

    private void processPacket(DatagramPacket packet) {
        try {
            ClientUpdate message = deserialize(packet.getData());
            messageQueue.offer(message);
        } catch (Exception e) {
            Logger.log(LogLevel.ERROR, "Failed to process UDP packet.", e);
        }
    }

    public static ClientUpdate deserialize(byte[] data) throws InvalidProtocolBufferException {
        return ClientUpdate.parseFrom(data);
    }

    // Limiter ------------------------------------------------------------------

    /**
     * Limits the rate at what UDP packets from clients can be accepted.
     */
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
