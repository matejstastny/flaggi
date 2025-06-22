/*
 * Author: Matěj Šťastný aka matysta
 * Date created: 6/21/2025
 * GitHub link: https://github.com/matysta/flaggi
 */

package flaggi.server.common;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import flaggi.proto.ClientMessages.ClientStateUpdate;
import flaggi.proto.ServerMessages.ServerJoinGame;
import flaggi.proto.ServerMessages.ServerMessage;
import flaggi.server.Server;
import flaggi.server.client.Client;
import flaggi.server.constants.Constants;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.common.UpdateLoop.Updatable;
import flaggi.shared.util.FileUtil;

public class GameManager implements Closeable, Updatable {

	private final String gameUuid;
	private Client[] clients;
	private Map<String, GameManager> activeGames;
	private BlockingQueue<ClientStateUpdate> updates;

	// Constructor --------------------------------------------------------------

	public GameManager(String gameUuid, Client[] clients, Map<String, GameManager> activeGames) {
		this.gameUuid = gameUuid;
		this.clients = clients;
		this.activeGames = activeGames;
		sendJoinGameMessages(getRandomMapJson());
	}

	@Override
	public void close() {
		if (activeGames != null && activeGames.get(this.gameUuid) == this) {
			activeGames.remove(this.gameUuid);
		}
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

	private void sendJoinGameMessages(String mapJson) {
		for (Client client : clients) {
			client.sendMessage(ServerMessage.newBuilder().setServerJoinGame(ServerJoinGame.newBuilder().setMapJson(mapJson).build()).build());
		}
	}

	private String getRandomMapJson() {
		List<String> maps = FileUtil.retrieveJarEntries(Constants.MAPS_RES_DIR, "json");
		if (maps.isEmpty()) {
			Logger.log(LogLevel.ERROR, "No maps found in resources directory: " + Constants.MAPS_RES_DIR);
			Server.handleFatalError();
		}
		int randomIndex = (int) (Math.random() * maps.size());
		String file = maps.get(randomIndex);

		StringBuilder contentBuilder = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream(Constants.MAPS_RES_DIR + "/" + file)))) {
			String line;
			while ((line = br.readLine()) != null) {
				contentBuilder.append(line).append(System.lineSeparator());
			}
		} catch (IOException e) {
			Logger.log(LogLevel.ERROR, "Failed to read map resource for file " + file, e);
			Server.handleFatalError();
		}
		return contentBuilder.toString();
	}
}
