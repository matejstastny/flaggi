/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 2/23/2025
 * Github link: https://github.com/kireiiiiiiii/flaggi
 */

package flaggi.server.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import flaggi.proto.ServerMessages.ServerMessage;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.util.ProtoUtil;

/**
 * User data structure class.
 */
public class User {

	private final String uuid;
	private final String name;
	private final Socket socket;
	private final OutputStream out;

	// Constructor --------------------------------------------------------------

	public User(String uuid, String name, Socket socket, OutputStream out) {
		this.uuid = uuid;
		this.name = name;
		this.socket = socket;
		this.out = out;
	}

	// Accesors -----------------------------------------------------------------

	public String getUuid() {
		return uuid;
	}

	public String getName() {
		return name;
	}

	public Socket getSocket() {
		return socket;
	}

	public OutputStream getOutputStream() {
		return out;
	}

	// Util ---------------------------------------------------------------------

	public void sendMessage(ServerMessage message) {
		try {
			byte[] messageBytes = message.toByteArray();
			byte[] sizeBytes = ProtoUtil.intToByteArray(messageBytes.length);
			this.out.write(sizeBytes);
			this.out.write(messageBytes);
		} catch (IOException e) {
			Logger.log(LogLevel.ERROR, "IOException occured while sending message to user (" + this.name + ", " + this.uuid + ")", e);
		}
	}
}
