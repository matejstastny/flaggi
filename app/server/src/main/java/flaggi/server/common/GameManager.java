// ------------------------------------------------------------------------------
// GameManager.java - Manages a single game instance with players and game state
// ------------------------------------------------------------------------------
// Author: Matej Stastny
// Date: 06-21-2025 (MM-DD-YYYY)
// License: MIT
// Link: https://github.com/matejstastny/flaggi
// ------------------------------------------------------------------------------

package flaggi.server.common;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import flaggi.proto.ClientMessages.ClientStateUpdate;
import flaggi.proto.ServerMessages.ServerJoinGame;
import flaggi.proto.ServerMessages.ServerMessage;
import flaggi.server.Server;
import flaggi.server.client.Client;
import flaggi.server.constants.Constants;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.common.MapData;
import flaggi.shared.common.UpdateLoop.Updatable;
import flaggi.shared.util.FileUtil;

public class GameManager implements Closeable, Updatable {

	private BlockingQueue<ClientStateUpdate> incoming = new LinkedBlockingQueue<>();
	private Map<String, GameManager> activeGames;
	private final String gameUuid;
	private Client[] clients;

	// Constructor --------------------------------------------------------------

	public GameManager(String gameUuid, Client[] clients, Map<String, GameManager> activeGames) {
		this.gameUuid = gameUuid;
		this.clients = clients;
		this.activeGames = activeGames;
		MapData mapData = getRandomMap();
		sendJoinGameMessages(mapData.getWidth(), mapData.getHeight());
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
		while ((update = incoming.poll()) != null) {
			processClientUpdate(update);
		}
	}

	// Public -------------------------------------------------------------------

	public void addUpdate(ClientStateUpdate update) {
		if (incoming == null) {
			Logger.log(LogLevel.WARN, "Incoming updates queue doesn't exist for game UUID: " + gameUuid);
			return;
		}
		if (update == null) {
			Logger.log(LogLevel.WARN, "Tried to add null update to game UUID: " + gameUuid);
			return;
		}
		incoming.offer(update);
	}

	// Private ------------------------------------------------------------------

	private void processClientUpdate(ClientStateUpdate update) {
		// TODO process update
		Logger.log(LogLevel.DEBUG, "Processed update " + update);
	}

	private void sendJoinGameMessages(int roomWidth, int roomHeight) {
		Logger.log(LogLevel.DEBUG, "Sending room size [" + roomWidth + "," + roomHeight + "] to clients for game UUID: " + gameUuid);
		for (Client client : clients) {
			client.sendMessage(ServerMessage.newBuilder().setServerJoinGame(ServerJoinGame.newBuilder().setGameUuid(gameUuid).setRoomWidth(roomWidth).setRoomHeight(roomHeight).build()).build());
		}
	}

	private MapData getRandomMap() {
		List<String> maps = FileUtil.listResourceFiles(Constants.MAPS_RES_DIR, "json");
		if (maps.isEmpty()) {
			Logger.log(LogLevel.ERROR, "No maps found in resources directory: " + Constants.MAPS_RES_DIR);
			Server.handleFatalError();
		}
		int randomIndex = (int) (Math.random() * maps.size());
		String file = maps.get(randomIndex);
		Logger.log(LogLevel.DEBUG, "Selected random map: " + file + " for game UUID: " + gameUuid);

		StringBuilder contentBuilder = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream(Constants.MAPS_RES_DIR + "/" + file)))) {
			String line;
			while ((line = br.readLine()) != null) {
				contentBuilder.append(line).append(System.lineSeparator());
			}
		} catch (IOException e) {
			Logger.log(LogLevel.ERROR, "Failed to read map resource for file " + file, e);
			Server.handleFatalError(); // TODO just fail game not whole server
		}
		MapData mapData = null;
		try {
			mapData = MapData.fromJson(contentBuilder.toString());
		} catch (IOException e) {
			Logger.log(LogLevel.ERROR, "Failed to convernt map JSON to MapData " + file, e);
			Server.handleFatalError(); // TODO just fail game not whole server
		}
		return mapData;
	}
}
