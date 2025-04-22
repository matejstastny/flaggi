package flaggi.server.client;

import java.util.Map;

import flaggi.proto.ServerMessages.ServerStateUpdate;

public class UserUpdater {

	private final Map<String, User> users;

	public UserUpdater(Map<String, User> users) {
		this.users = users;
	}

	public void update() {

	}

	private ServerStateUpdate getUpdateMessage() {
		return null;
	}

	// get update message, filter out non-renderable stuff
}
