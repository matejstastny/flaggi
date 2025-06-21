/*
 * Author: Matěj Šťastný aka matysta
 * Date created: 4/26/2025
 * GitHub link: https://github.com/matysta/flaggi
 */

package flaggi.client.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import flaggi.proto.ClientMessages.ClientMessage;
import flaggi.proto.ClientMessages.Ping;
import flaggi.proto.ServerMessages.ServerMessage;
import flaggi.shared.util.ProtoUtil;

public class Global {

	// Private constructor to prevent instantiation
	private Global() {
		throw new UnsupportedOperationException("Global is a static method library and cannot be instantiated");
	}

	// Net -----------------------------------------------------------------------

	/**
	 * Checks if the server is a Flaggi server by sending a ping message and
	 * expecting a pong response.
	 */
	public static boolean isFlaggiServer(String address, int port) {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(address, port), 1000);
			socket.setSoTimeout(2000);

			InputStream in = socket.getInputStream();
			OutputStream out = socket.getOutputStream();
			ClientMessage ping = ClientMessage.newBuilder().setPing(Ping.newBuilder().setPing("ping")).build();

			byte[] bytes = ping.toByteArray();
			byte[] size = ProtoUtil.intToByteArray(bytes.length);
			out.write(size);
			out.write(bytes);

			size = new byte[4];
			in.read(size);
			int messageSize = ProtoUtil.byteArrayToInt(size);
			byte[] messageBytes = new byte[messageSize];
			in.read(messageBytes);
			return ServerMessage.parseFrom(messageBytes).hasPong();
		} catch (IOException e) {
			return false;
		}
	}
}
