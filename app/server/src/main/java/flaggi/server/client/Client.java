/*
 * Author: Matěj Šťastný aka my-daarlin
 * Date created: 2/23/2025
 * GitHub link: https://github.com/my-daarlin/flaggi
 */

package flaggi.server.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import flaggi.proto.ServerMessages.ServerMessage;
import flaggi.server.Server;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.util.ProtoUtil;

/**
 * Client data structure class.
 */
public class Client {

	private final String uuid;
	private final String name;
	private final Socket socket;
	private final OutputStream out;

	// Constructor --------------------------------------------------------------

	public Client(String uuid, String name, Socket socket, OutputStream out) {
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
			ProtoUtil.sendServerMessage(out, message);
		} catch (IOException e) {
			Logger.log(LogLevel.ERROR, "Failed to send message to client " + uuid, e);
			Server.handleFatalError();
		}
	}
}
