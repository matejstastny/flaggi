package flaggi.server.client;

import java.util.Map;

import flaggi.proto.ServerMessages.ServerUpdate;

public class UserUpdater {

    private final Map<String, User> users;

    public UserUpdater(Map<String, User> users) {
        this.users = users;
    }

    public void update() {

    }

    private ServerUpdate getUpdateMessage()


    // get update message, filter out non-renderable stuff
}
