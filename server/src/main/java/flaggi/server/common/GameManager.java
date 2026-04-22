// ------------------------------------------------------------------------------
// GameManager.java - Manages a single game instance with players and game state
// ------------------------------------------------------------------------------
// Author: Matej Stastny
// Date: 06-21-2025 (MM-DD-YYYY)
// License: MIT
// Link: https://github.com/matejstastny/flaggi
// ------------------------------------------------------------------------------

package flaggi.server.common;

import flaggi.proto.ClientMessages.ClientKey;
import flaggi.proto.ClientMessages.ClientStateUpdate;
import flaggi.proto.ServerMessages.GameObjectType;
import flaggi.proto.ServerMessages.PlayerAnimation;
import flaggi.proto.ServerMessages.PlayerSkin;
import flaggi.proto.ServerMessages.ServerEndGame;
import flaggi.proto.ServerMessages.ServerJoinGame;
import flaggi.proto.ServerMessages.ServerMessage;
import flaggi.proto.ServerMessages.ServerStateUpdate;
import flaggi.server.Server;
import flaggi.server.client.Client;
import flaggi.server.constants.Constants;
import flaggi.server.constants.Hitboxes;
import flaggi.shared.common.BulletGameObject;
import flaggi.shared.common.FlagGameObject;
import flaggi.shared.common.FlagGameObject.Team;
import flaggi.shared.common.GameObject;
import flaggi.shared.common.Hitbox;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.common.MapData;
import flaggi.shared.common.MapData.ObjectData;
import flaggi.shared.common.MapData.Spawnpoint;
import flaggi.shared.common.PlayerGameObject;
import flaggi.shared.common.UpdateLoop.Updatable;
import flaggi.shared.util.FileUtil;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class GameManager implements Closeable, Updatable {

	private static final int CAPTURES_TO_WIN = 3;
	private static final int FLAG_CHECK_INTERVAL = 1;

	private final Map<String, ClientStateUpdate> latestClientInputs = new ConcurrentHashMap<>();
	private final BlockingQueue<ClientStateUpdate> incoming = new LinkedBlockingQueue<>();
	private final Map<String, GameManager> activeGames;
	private final UdpManager udpManager;
	private final GameData gameData;
	private final MapData mapData;
	private final String gameUuid;
	private final Client[] clients;

	private int tick = 0;
	private boolean gameOver = false;

	// Constructor --------------------------------------------------------------

	public GameManager(String gameUuid, Client[] clients, Map<String, GameManager> activeGames, UdpManager udpManager) {
		this.clients = clients;
		this.gameUuid = gameUuid;
		this.udpManager = udpManager;
		this.activeGames = activeGames;
		this.gameData = new GameData();
		this.mapData = getRandomMap();

		initializeStaticObjects();
		initializePlayers();
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
		if (gameOver)
			return;

		tick++;

		ClientStateUpdate update;
		while ((update = incoming.poll()) != null) {
			latestClientInputs.put(update.getPlayerUuid(), update);
		}

		for (Map.Entry<String, ClientStateUpdate> entry : latestClientInputs.entrySet()) {
			processPlayerInput(entry.getKey(), entry.getValue());
		}
		latestClientInputs.clear();

		tickBullets();

		if (tick % FLAG_CHECK_INTERVAL == 0) {
			tickFlags();
		}

		sendUpdatesToClients();
	}

	// Public -------------------------------------------------------------------

	public void addUpdate(ClientStateUpdate update) {
		if (incoming == null) {
			Logger.log(LogLevel.WRN, "Incoming updates queue doesn't exist for game UUID: " + gameUuid);
			return;
		}
		if (update == null) {
			Logger.log(LogLevel.WRN, "Tried to add null update to game UUID: " + gameUuid);
			return;
		}
		incoming.offer(update);
	}

	// Private ------------------------------------------------------------------

	/**
	 * Spawn both players at opposite spawn points, assign skins by slot index.
	 */
	private void initializePlayers() {
		Spawnpoint s = mapData.getSpawnpoint();
		double[][] spawnCoords = { { s.getOneX(), s.getOneY() }, { s.getTwoX(), s.getTwoY() } };
		PlayerSkin[] skins = PlayerSkin.values();

		for (int i = 0; i < clients.length; i++) {
			Client c = clients[i];
			double spawnX = spawnCoords[Math.min(i, 1)][0];
			double spawnY = spawnCoords[Math.min(i, 1)][1];
			PlayerSkin skin = skins[i % skins.length];

			PlayerGameObject player = new PlayerGameObject(spawnX, spawnY, Hitboxes.player(), c.username(), skin);
			gameData.addPlayer(player, c.uuid());
			Logger.log(LogLevel.DBG, "Spawned player " + c.uuid() + " at [" + spawnX + "," + spawnY + "]");
		}
	}

	/**
	 * Load trees and flags from the map definition and register them in GameData.
	 */
	private void initializeStaticObjects() {
		for (ObjectData obj : mapData.getGameObjects()) {
			switch (obj.getObjectType()) {
			case TREE -> {
				GameObject tree = new GameObject(GameObjectType.TREE, obj.getX(), obj.getY(), Hitboxes.obstacle());
				gameData.addTree(tree);
			}
			case RED_FLAG -> {
				FlagGameObject flag = new FlagGameObject(Team.RED, obj.getX(), obj.getY(), Hitboxes.flag());
				gameData.addFlag(flag);
			}
			case BLUE_FLAG -> {
				FlagGameObject flag = new FlagGameObject(Team.BLUE, obj.getX(), obj.getY(), Hitboxes.flag());
				gameData.addFlag(flag);
			}
			}
		}
	}

	// Player input -------------------------------------------------------------

	/**
	 * Apply one player's latest input state to their {@link PlayerGameObject}
	 * Handles movement, animation, and shooting
	 */
	private void processPlayerInput(String playerUuid, ClientStateUpdate update) {
		PlayerGameObject player = gameData.getPlayer(playerUuid);
		if (player == null || !player.isAlive())
			return;

		List<ClientKey> keys = update.getClientKeyInput().getHeldKeysList();
		int mouseX = update.getClientMouseInput().getX();
		int mouseY = update.getClientMouseInput().getY();

		// --- Movement ---------------------------------------------------------
		double dx = 0, dy = 0;
		if (keys.contains(ClientKey.KEY_UP))
			dy -= PlayerGameObject.PLAYER_SPEED;
		if (keys.contains(ClientKey.KEY_DOWN))
			dy += PlayerGameObject.PLAYER_SPEED;
		if (keys.contains(ClientKey.KEY_LEFT))
			dx -= PlayerGameObject.PLAYER_SPEED;
		if (keys.contains(ClientKey.KEY_RIGHT))
			dx += PlayerGameObject.PLAYER_SPEED;

		if (dx != 0 && dy != 0) {
			double diag = PlayerGameObject.PLAYER_SPEED / Math.sqrt(2);
			dx = Math.signum(dx) * diag;
			dy = Math.signum(dy) * diag;
		}

		if (dx != 0 || dy != 0) {
			tryMove(player, dx, dy);
		}

		double newX = Math.max(0, Math.min(mapData.getWidth(), player.x()));
		double newY = Math.max(0, Math.min(mapData.getHeight(), player.y()));
		player.setX(newX);
		player.setY(newY);

		// --- Animation / facing -----------------------------------------------
		updateAnimation(player, dx, dy, mouseX);

		// --- Shooting ---------------------------------------------------------
		if (keys.contains(ClientKey.KEY_SHOOT) && player.canShoot()) {
			spawnBullet(playerUuid, player, mouseX, mouseY);
		}
	}

	/**
	 * Attempt to move {@code player} by (dx, dy), sliding along obstacles.
	 */
	private void tryMove(PlayerGameObject player, double dx, double dy) {
		// Try full movement first
		double newX = player.x() + dx;
		double newY = player.y() + dy;

		if (!collidesWithObstacle(newX, newY, player.collision())) {
			player.setX(newX);
			player.setY(newY);
			return;
		}

		// Try horizontal only
		if (!collidesWithObstacle(newX, player.y(), player.collision())) {
			player.setX(newX);
			return;
		}

		// Try vertical only
		if (!collidesWithObstacle(player.x(), newY, player.collision())) {
			player.setY(newY);
		}
		// else: fully blocked, no movement applied
	}

	/**
	 * Returns true if placing a player hitbox at (cx, cy) would overlap any tree.
	 */
	private boolean collidesWithObstacle(double cx, double cy, Hitbox playerHb) {
		double px1 = cx + playerHb.getX();
		double py1 = cy + playerHb.getY();
		double px2 = px1 + playerHb.getWidth();
		double py2 = py1 + playerHb.getHeight();

		for (GameObject tree : gameData.getObstacles()) {
			double tx1 = tree.worldLeft();
			double ty1 = tree.worldTop();
			double tx2 = tree.worldRight();
			double ty2 = tree.worldBottom();

			if (px1 < tx2 && px2 > tx1 && py1 < ty2 && py2 > ty1) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Update the player's animation and facing direction based on movement and
	 * mouse aim.
	 */
	private void updateAnimation(PlayerGameObject player, double dx, double dy, int mouseX) {
		boolean moving = dx != 0 || dy != 0;

		if (!moving) {
			player.setAnimation(PlayerAnimation.ANIM_IDLE);
		} else if (dx != 0 && dy != 0) {
			player.setAnimation(PlayerAnimation.ANIM_WALK_DIAGONAL);
		} else if (dy != 0) {
			player.setAnimation(dy < 0 ? PlayerAnimation.ANIM_WALK_UP : PlayerAnimation.ANIM_WALK_DOWN);
		} else {
			player.setAnimation(PlayerAnimation.ANIM_WALK_SIDE);
		}

		// Facing is determined by the mouse, not the movement direction
		// mouseX is in world space - compare against player's x
		player.setFacingLeft(mouseX < player.x());
	}

	// Bullets ------------------------------------------------------------------

	private void spawnBullet(String ownerUuid, PlayerGameObject player, int mouseX, int mouseY) {
		BulletGameObject bullet = new BulletGameObject(player.x(), player.y(), mouseX, mouseY, ownerUuid, Hitboxes.bullet());
		gameData.addBullet(bullet);
		player.recordShot();
		Logger.log(LogLevel.DBG, "Player " + ownerUuid + " fired a bullet toward [" + mouseX + "," + mouseY + "]");
	}

	/**
	 * Advance all bullets, test for hits against players and trees, remove dead
	 * bullets.
	 */
	private void tickBullets() {
		List<BulletGameObject> toRemove = new ArrayList<>();

		for (BulletGameObject bullet : gameData.getBullets()) {
			boolean alive = bullet.tick(mapData.getWidth(), mapData.getHeight());
			if (!alive) {
				toRemove.add(bullet);
				continue;
			}

			// --- Hit tree? -------------------------------------------------------
			boolean hitObstacle = false;
			for (GameObject tree : gameData.getObstacles()) {
				if (bullet.overlaps(tree)) {
					hitObstacle = true;
					break;
				}
			}
			if (hitObstacle) {
				toRemove.add(bullet);
				continue;
			}

			// --- Hit player? -----------------------------------------------------
			for (Map.Entry<String, PlayerGameObject> entry : gameData.getPlayers().entrySet()) {
				String targetUuid = entry.getKey();
				PlayerGameObject target = entry.getValue();

				// Can't shoot yourself loll
				if (targetUuid.equals(bullet.ownerUuid()))
					continue;
				if (!target.isAlive())
					continue;

				if (bullet.overlaps(target)) {
					handleBulletHit(bullet, target, targetUuid);
					toRemove.add(bullet);
					break;
				}
			}
		}

		toRemove.forEach(gameData::removeBullet);
	}

	/**
	 * Apply damage to {@code target}. If they die, handle respawn and flag drop.
	 */
	private void handleBulletHit(BulletGameObject bullet, PlayerGameObject target, String targetUuid) {
		target.damage(BulletGameObject.BULLET_DAMAGE);
		Logger.log(LogLevel.DBG, "Player " + targetUuid + " hit! HP now " + target.hp());

		if (!target.isAlive()) {
			Logger.log(LogLevel.DBG, "Player " + targetUuid + " died - respawning.");

			// Drop any carried flag at the death position
			if (target.carryingFlag()) {
				dropCarriedFlag(targetUuid, target.x(), target.y());
			}

			// Respawn after death (instant respawn; add a timer here if desired)
			target.respawn();
		}
	}

	// Flag logic ---------------------------------------------------------------

	/**
	 * Each tick: check every player against every flag to resolve: - Enemy player
	 * touches opposing flag → pickup - Player carrying a flag reaches their own
	 * base → capture & score - Friendly player touches a dropped flag → return to
	 * base
	 */
	private void tickFlags() {
		for (Map.Entry<String, PlayerGameObject> entry : gameData.getPlayers().entrySet()) {
			String playerUuid = entry.getKey();
			PlayerGameObject player = entry.getValue();
			if (!player.isAlive())
				continue;

			Team playerTeam = getPlayerTeam(playerUuid);

			for (FlagGameObject flag : gameData.getFlags()) {
				switch (flag.state()) {
				case AT_BASE, DROPPED -> resolvePickupOrReturn(playerUuid, player, playerTeam, flag);
				case CARRIED -> moveCarriedFlag(playerUuid, player, playerTeam, flag);
				}
			}
		}
	}

	/**
	 * Handle a player touching a flag that is at base or dropped.
	 */
	private void resolvePickupOrReturn(String playerUuid, PlayerGameObject player, Team playerTeam, FlagGameObject flag) {
		if (!player.overlaps(flag))
			return;

		if (flag.team() != playerTeam) {
			// Enemy flag - pick it up
			if (flag.pickUp(playerUuid)) {
				player.setCarryingFlag(true);
				Logger.log(LogLevel.DBG, "Player " + playerUuid + " picked up the " + flag.team() + " flag.");
			}
		} else if (flag.state() == FlagGameObject.FlagState.DROPPED) {
			// Friendly dropped flag - return it to base
			flag.returnToBase();
			Logger.log(LogLevel.DBG, "Player " + playerUuid + " returned the " + flag.team() + " flag to base.");
		}
	}

	/**
	 * If the player is the carrier, update flag position. If the carrier has
	 * reached their own base flag, score a capture.
	 */
	private void moveCarriedFlag(String playerUuid, PlayerGameObject player, Team playerTeam, FlagGameObject carriedFlag) {
		if (!carriedFlag.carrierUuid().equals(playerUuid))
			return;

		// Move the flag with the player
		carriedFlag.moveWithCarrier(player.x(), player.y());

		// Check if this player has reached their own team's base flag
		for (FlagGameObject baseFlag : gameData.getFlags()) {
			if (baseFlag == carriedFlag)
				continue; // same object
			if (baseFlag.team() != playerTeam)
				continue; // must be own base flag
			if (!baseFlag.isAtBase())
				continue; // base flag must still be home

			if (player.overlaps(baseFlag)) {
				// Capture!
				carriedFlag.returnToBase();
				player.setCarryingFlag(false);
				player.incrementFlagCount();
				gameData.incrementScore(playerUuid);
				Logger.log(LogLevel.DBG, "Player " + playerUuid + " CAPTURED the flag! Score: " + gameData.getScore(playerUuid));

				if (gameData.getScore(playerUuid) >= CAPTURES_TO_WIN) {
					endGame(playerUuid, player.username());
				}
				break;
			}
		}
	}

	/**
	 * Drop a flag carried by {@code carrierUuid} at the given coordinates. Called
	 * when the carrier dies.
	 */
	private void dropCarriedFlag(String carrierUuid, double x, double y) {
		for (FlagGameObject flag : gameData.getFlags()) {
			if (flag.isCarried() && carrierUuid.equals(flag.carrierUuid())) {
				flag.drop(x, y);
				Logger.log(LogLevel.DBG, "Flag dropped at [" + x + "," + y + "] by " + carrierUuid);
				return;
			}
		}
	}

	// Win ----------------------------------------------------------------------

	private void endGame(String winnerUuid, String winnerName) {
		gameOver = true;
		Logger.log(LogLevel.DBG, "Game over! Winner: " + winnerName + " (" + winnerUuid + ")");

		ServerMessage msg = ServerMessage.newBuilder().setServerEndGame(ServerEndGame.newBuilder().setWinnerUuid(winnerUuid).setWinnerName(winnerName).build()).build();

		for (Client c : clients) {
			c.sendMessage(msg);
		}
		close();
	}

	// Networking ---------------------------------------------------------------

	private void sendUpdatesToClients() {
		Map<String, ServerStateUpdate> updates = gameData.getServerUpdateData();
		DebugServer.publish(updates, latestClientInputs, tick);
		for (Map.Entry<String, ServerStateUpdate> entry : updates.entrySet()) {
			Client c = getClient(entry.getKey());
			if (c != null) {
				udpManager.send(entry.getValue(), c.address(), c.udpPort());
			}
		}
	}

	private void sendJoinGameMessages(int roomWidth, int roomHeight) {
		Logger.log(LogLevel.DBG, "Sending room size [" + roomWidth + "," + roomHeight + "] to clients for game UUID: " + gameUuid);
		for (Client client : clients) {
			client.sendMessage(ServerMessage.newBuilder().setServerJoinGame(ServerJoinGame.newBuilder().setGameUuid(gameUuid).setRoomWidth(roomWidth).setRoomHeight(roomHeight).build()).build());
		}
	}

	// Helpers ------------------------------------------------------------------

	/**
	 * Determines which team a player belongs to based on their slot index (client
	 * order). Player at index 0 → RED; player at index 1 → BLUE. For 3+ players,
	 * alternates RED / BLUE.
	 */
	private Team getPlayerTeam(String playerUuid) {
		for (int i = 0; i < clients.length; i++) {
			if (clients[i].uuid().equals(playerUuid)) {
				return i % 2 == 0 ? Team.RED : Team.BLUE;
			}
		}
		return Team.RED; // fallback
	}

	private Client getClient(String uuid) {
		for (Client c : clients) {
			if (c.uuid().equals(uuid))
				return c;
		}
		return null;
	}

	private MapData getRandomMap() {
		List<String> maps = FileUtil.listResourceFiles(Constants.MAPS_RES_DIR, "json");
		if (maps.isEmpty()) {
			Logger.log(LogLevel.ERR, "No maps found in resources directory: " + Constants.MAPS_RES_DIR);
			Server.handleFatalError();
		}
		int randomIndex = (int) (Math.random() * maps.size());
		String file = maps.get(randomIndex);
		Logger.log(LogLevel.DBG, "Selected random map: " + file + " for game UUID: " + gameUuid);

		StringBuilder contentBuilder = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream(Constants.MAPS_RES_DIR + "/" + file)))) {
			String line;
			while ((line = br.readLine()) != null) {
				contentBuilder.append(line).append(System.lineSeparator());
			}
		} catch (IOException e) {
			Logger.log(LogLevel.ERR, "Failed to read map resource for file " + file, e);
			Server.handleFatalError();
		}

		MapData loaded = null;
		try {
			loaded = MapData.fromJson(contentBuilder.toString());
		} catch (IOException e) {
			Logger.log(LogLevel.ERR, "Failed to convert map JSON to MapData " + file, e);
			Server.handleFatalError();
		}
		return loaded;
	}
}
