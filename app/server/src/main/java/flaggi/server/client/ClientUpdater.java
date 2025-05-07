package flaggi.server.client;

import java.util.Map;

import flaggi.proto.ServerMessages.ServerStateUpdate;

public class ClientUpdater {

	private final Map<String, Client> clients;

	public ClientUpdater(Map<String, Client> clients) {
		this.clients = clients;
	}

	public void update() {

	}

	private ServerStateUpdate getUpdateMessage() {
		return null;
	}

	// get update message, filter out non-renderable stuff
}
