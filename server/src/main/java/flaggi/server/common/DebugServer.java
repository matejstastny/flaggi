// --------------------------------------------------------------------------------------------
// DebugServer.java - Lightweight HTTP debug state endpoint for the dev dashboard
// --------------------------------------------------------------------------------------------
// Author: Matej Stastny
// Date: 2026-02-25 (YYYY-MM-DD)
// License: MIT
// Link: https://github.com/matejstastny/flaggi
// --------------------------------------------------------------------------------------------

package flaggi.server.common;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import flaggi.proto.ClientMessages.ClientStateUpdate;
import flaggi.proto.ServerMessages.ServerGameObject;
import flaggi.proto.ServerMessages.ServerStateUpdate;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;

/**
 * Tiny built-in HTTP server that exposes current game state as JSON for the
 * Electron dev dashboard. Runs on port 54323. Zero extra dependencies - uses
 * com.sun.net.httpserver which ships with the JDK.
 *
 * Usage: DebugServer.start(); // call once on server boot
 * DebugServer.publish(updates, clientInputs); // call every game tick
 * DebugServer.stop(); // call on shutdown
 */
public class DebugServer {

	private static final int PORT = 54323;
	private static HttpServer httpServer;
	private static volatile String cachedJson = "{}";

	// Lifecycle ---------------------------------------------------------------

	public static void start() {
		try {
			httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
			httpServer.createContext("/state", DebugServer::handleState);
			httpServer.createContext("/health", DebugServer::handleHealth);
			httpServer.setExecutor(Executors.newSingleThreadExecutor());
			httpServer.start();
			Logger.log(LogLevel.INF, "Debug HTTP server listening on port " + PORT);
		} catch (IOException e) {
			Logger.log(LogLevel.WRN, "Could not start debug HTTP server: " + e.getMessage());
		}
	}

	public static void stop() {
		if (httpServer != null) {
			httpServer.stop(0);
		}
	}

	/**
	 * Call this every game tick from GameManager.sendUpdatesToClients().
	 *
	 * @param updates      map of playerUuid → ServerStateUpdate (one per player,
	 *                     "me" is that player's object)
	 * @param clientInputs map of playerUuid → latest ClientStateUpdate received
	 *                     from that client
	 * @param tick         current server tick number
	 */
	public static void publish(Map<String, ServerStateUpdate> updates, Map<String, ClientStateUpdate> clientInputs, int tick) {
		cachedJson = buildJson(updates, clientInputs, tick);
	}

	// HTTP handlers -----------------------------------------------------------

	private static void handleState(HttpExchange ex) throws IOException {
		// CORS so Electron renderer can fetch without issues
		ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
		ex.getResponseHeaders().add("Content-Type", "application/json");
		byte[] body = cachedJson.getBytes("UTF-8");
		ex.sendResponseHeaders(200, body.length);
		try (OutputStream os = ex.getResponseBody()) {
			os.write(body);
		}
	}

	private static void handleHealth(HttpExchange ex) throws IOException {
		byte[] body = "ok".getBytes();
		ex.sendResponseHeaders(200, body.length);
		try (OutputStream os = ex.getResponseBody()) {
			os.write(body);
		}
	}

	// JSON builder (hand-rolled to avoid any extra deps) ----------------------

	private static String buildJson(Map<String, ServerStateUpdate> updates, Map<String, ClientStateUpdate> clientInputs, int tick) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"tick\":").append(tick).append(",\"players\":[");

		boolean firstPlayer = true;
		for (Map.Entry<String, ServerStateUpdate> entry : updates.entrySet()) {
			if (!firstPlayer)
				sb.append(",");
			firstPlayer = false;

			String uuid = entry.getKey();
			ServerStateUpdate update = entry.getValue();
			ServerGameObject me = update.getMe();
			ClientStateUpdate input = clientInputs != null ? clientInputs.get(uuid) : null;

			sb.append("{");
			sb.append("\"uuid\":").append(jsonStr(uuid)).append(",");
			sb.append("\"username\":").append(jsonStr(me.getUsername())).append(",");
			sb.append("\"x\":").append(me.getX()).append(",");
			sb.append("\"y\":").append(me.getY()).append(",");
			sb.append("\"hp\":").append(me.getHp()).append(",");
			sb.append("\"flagCount\":").append(me.getFlagCount()).append(",");
			sb.append("\"skin\":").append(jsonStr(me.getSkin().name())).append(",");
			sb.append("\"animation\":").append(jsonStr(me.getAnimation().name())).append(",");
			sb.append("\"facingLeft\":").append(me.getFacingLeft()).append(",");
			sb.append("\"collX\":").append(me.getCollX()).append(",");
			sb.append("\"collY\":").append(me.getCollY()).append(",");
			sb.append("\"collW\":").append(me.getCollWidth()).append(",");
			sb.append("\"collH\":").append(me.getCollHeight()).append(",");

			// Client inputs
			if (input != null) {
				sb.append("\"mouse\":{\"x\":").append(input.getClientMouseInput().getX()).append(",\"y\":").append(input.getClientMouseInput().getY()).append("},");
				sb.append("\"keys\":[");
				boolean firstKey = true;
				for (var key : input.getClientKeyInput().getHeldKeysList()) {
					if (!firstKey)
						sb.append(",");
					firstKey = false;
					sb.append(jsonStr(key.name()));
				}
				sb.append("],");
			} else {
				sb.append("\"mouse\":{\"x\":0,\"y\":0},\"keys\":[],");
			}

			// Other objects visible to this player
			sb.append("\"others\":[");
			boolean firstOther = true;
			for (ServerGameObject obj : update.getOtherList()) {
				if (!firstOther)
					sb.append(",");
				firstOther = false;
				sb.append("{");
				sb.append("\"type\":").append(jsonStr(obj.getType().name())).append(",");
				sb.append("\"x\":").append(obj.getX()).append(",");
				sb.append("\"y\":").append(obj.getY()).append(",");
				sb.append("\"hp\":").append(obj.getHp()).append(",");
				sb.append("\"flagCount\":").append(obj.getFlagCount()).append(",");
				sb.append("\"username\":").append(jsonStr(obj.getUsername()));
				sb.append("}");
			}
			sb.append("]");

			sb.append("}");
		}

		sb.append("]}");
		return sb.toString();
	}

	private static String jsonStr(String s) {
		if (s == null)
			return "\"\"";
		return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}
}
