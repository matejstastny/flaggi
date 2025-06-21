/*
 * Author: Matěj Šťastný aka matysta
 * Date created: 6/21/2025
 * GitHub link: https://github.com/matysta/flaggi
 */

package flaggi.server.common;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import flaggi.proto.ClientMessages.ClientStateUpdate;
import flaggi.proto.ServerMessages.ServerJoinGame;
import flaggi.proto.ServerMessages.ServerMessage;
import flaggi.server.client.Client;
import flaggi.shared.common.UpdateLoop.Updatable;

public class GameManager implements Updatable {

	private final String gameUuid;
	private Client[] clients;
	private Map<String, GameManager> activeGames;
	private BlockingQueue<ClientStateUpdate> updates;

	// Constructor --------------------------------------------------------------

	public GameManager(String gameUuid, Client[] clients, Map<String, GameManager> activeGames) {
		this.gameUuid = gameUuid;
		this.clients = clients;
		this.activeGames = activeGames;
		sendJoinGameMessages("");
	}

	// Update -------------------------------------------------------------------

	@Override
	public void update() {
		ClientStateUpdate update;
		while ((update = updates.poll()) != null) {
			// TODO Process the update
		}
	}

	// Public -------------------------------------------------------------------

	public void addUpdate(ClientStateUpdate update) {
		updates.offer(update);
	}

	// Private ------------------------------------------------------------------

	private void close() {
		if (activeGames != null && activeGames.get(this.gameUuid) == this) {
			activeGames.remove(this.gameUuid);
		}
	}

	private void sendJoinGameMessages(String mapJson) {
		for (Client client : clients) {
			client.sendMessage(ServerMessage.newBuilder().setServerJoinGame(ServerJoinGame.newBuilder().setMapJson(mapJson).build()).build());
		}
	}
}
