package flaggi.client.common;

import flaggi.proto.ClientMessages.ClientMessage;
import flaggi.proto.ClientMessages.Ping;
import flaggi.proto.ServerMessages.ServerMessage;
import flaggi.shared.util.ProtoUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Global utility class providing static methods for various functionalities across the application.
 */
public class Global {

    // Private constructor to prevent instantiation
    private Global() {
        throw new UnsupportedOperationException("Global is a static method library and cannot be instantiated");
    }

    // Net -----------------------------------------------------------------------

    /**
     * Checks if the server is a Flaggi server by sending a ping message and expecting a pong
     * response.
     */
    public static boolean isFlaggiServer(String address, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(address, port), 1000);
            socket.setSoTimeout(2000);

            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            ClientMessage ping = ClientMessage.newBuilder()
                    .setPing(Ping.newBuilder().setPing("ping"))
                    .build();

            ProtoUtil.sendClientMessage(ping, out);
            ServerMessage response = ProtoUtil.receiveServerMessage(in);

            return response != null && response.hasPong();
        } catch (IOException e) {
            return false;
        }
    }
}
