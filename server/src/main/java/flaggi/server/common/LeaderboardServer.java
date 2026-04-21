package flaggi.server.common;

import java.util.List;

import flaggi.server.common.DatabaseManager.GameHistoryEntry;
import flaggi.server.common.DatabaseManager.LeaderboardEntry;
import io.javalin.Javalin;

/**
 * A tiny web server that serves the Flaggi leaderboard at localhost:8080.
 *
 * Start it once with LeaderboardServer.start() when the game server boots. It
 * reads from the same SQLite database as the game server.
 */
public class LeaderboardServer {

	private static final int PORT = 8080;

	public static void start() {
		Javalin app = Javalin.create(config -> {
			config.showJavalinBanner = false;
		}).start(PORT);

		// When someone visits localhost:8080/ or localhost:8080/leaderboard,
		// call our method that builds and returns the HTML page.
		// Explicit lambdas avoid Java accidentally resolving the wrong Context class.
		app.get("/", ctx -> handleLeaderboard(ctx));
		app.get("/leaderboard", ctx -> handleLeaderboard(ctx));

		System.out.println("[Leaderboard] Running at http://localhost:" + PORT);
	}

	private static void handleLeaderboard(io.javalin.http.Context ctx) {
		List<LeaderboardEntry> players = DatabaseManager.getLeaderboard();
		List<GameHistoryEntry> recentGames = DatabaseManager.getRecentGames(10);
		// result() sends a plain response; we set content-type to HTML manually.
		ctx.contentType("text/html").result(buildPage(players, recentGames));
	}

	/**
	 * Builds the full HTML page as a string. No frameworks, no build step - just
	 * straightforward HTML.
	 */
	private static String buildPage(List<LeaderboardEntry> players, List<GameHistoryEntry> recentGames) {
		StringBuilder sb = new StringBuilder();

		sb.append("""
				    <!DOCTYPE html>
				    <html lang="en">
				    <head>
				        <meta charset="UTF-8">
				        <meta name="viewport" content="width=device-width, initial-scale=1.0">
				        <meta http-equiv="refresh" content="30"> <!-- Auto-refresh every 30 seconds -->
				        <title>Flaggi Leaderboard</title>
				        <style>
				            :root {
				                --red:   #e74c3c;
				                --blue:  #3498db;
				                --bg:    #1a1a2e;
				                --card:  #16213e;
				                --text:  #eaeaea;
				                --muted: #7f8c8d;
				                --gold:  #f1c40f;
				            }

				            * { box-sizing: border-box; margin: 0; padding: 0; }

				            body {
				                background: var(--bg);
				                color: var(--text);
				                font-family: 'Segoe UI', system-ui, sans-serif;
				                padding: 2rem;
				            }

				            h1 {
				                text-align: center;
				                font-size: 2.5rem;
				                margin-bottom: 0.25rem;
				                letter-spacing: 0.05em;
				            }

				            .subtitle {
				                text-align: center;
				                color: var(--muted);
				                margin-bottom: 2rem;
				                font-size: 0.85rem;
				            }

				            h2 {
				                font-size: 1.2rem;
				                margin-bottom: 1rem;
				                color: var(--muted);
				                text-transform: uppercase;
				                letter-spacing: 0.1em;
				            }

				            .section { margin-bottom: 3rem; }

				            table {
				                width: 100%;
				                border-collapse: collapse;
				                background: var(--card);
				                border-radius: 8px;
				                overflow: hidden;
				            }

				            th {
				                padding: 0.75rem 1rem;
				                text-align: left;
				                background: #0f3460;
				                color: var(--muted);
				                font-weight: 600;
				                font-size: 0.8rem;
				                text-transform: uppercase;
				                letter-spacing: 0.08em;
				            }

				            td {
				                padding: 0.75rem 1rem;
				                border-bottom: 1px solid #0f3460;
				            }

				            tr:last-child td { border-bottom: none; }

				            tr:hover td { background: rgba(255,255,255,0.03); }

				            .rank { color: var(--muted); font-size: 0.9rem; width: 2rem; }
				            .rank-1 { color: var(--gold); font-weight: bold; font-size: 1rem; }
				            .rank-2 { color: #bdc3c7; font-weight: bold; }
				            .rank-3 { color: #cd6133; font-weight: bold; }

				            .player-name { font-weight: 600; }

				            .kd { font-family: monospace; }

				            .team-red  { color: var(--red);  font-weight: bold; }
				            .team-blue { color: var(--blue); font-weight: bold; }

				            .no-data {
				                text-align: center;
				                color: var(--muted);
				                padding: 2rem;
				                background: var(--card);
				                border-radius: 8px;
				            }
				        </style>
				    </head>
				    <body>
				        <h1>🚩 Flaggi</h1>
				        <p class="subtitle">Leaderboard &mdash; auto-refreshes every 30 seconds</p>
				""");

		// --- Leaderboard table ---
		sb.append("<div class=\"section\"><h2>Player Rankings</h2>");

		if (players.isEmpty()) {
			sb.append("<p class=\"no-data\">No games played yet. Get out there!</p>");
		} else {
			sb.append("""
					    <table>
					        <thead>
					            <tr>
					                <th>#</th>
					                <th>Player</th>
					                <th>Wins</th>
					                <th>Losses</th>
					                <th>Games</th>
					                <th>Kills</th>
					                <th>Deaths</th>
					                <th>K/D</th>
					            </tr>
					        </thead>
					        <tbody>
					""");

			for (int i = 0; i < players.size(); i++) {
				LeaderboardEntry p = players.get(i);
				int rank = i + 1;
				String rankClass = rank == 1 ? "rank rank-1" : rank == 2 ? "rank rank-2" : rank == 3 ? "rank rank-3" : "rank";
				String rankLabel = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : String.valueOf(rank);

				sb.append("<tr>").append("<td class=\"").append(rankClass).append("\">").append(rankLabel).append("</td>").append("<td class=\"player-name\">").append(escapeHtml(p.name)).append("</td>").append("<td>").append(p.wins).append("</td>").append("<td>").append(p.losses).append("</td>").append("<td>").append(p.gamesPlayed).append("</td>").append("<td>").append(p.kills).append("</td>").append("<td>").append(p.deaths).append("</td>").append("<td class=\"kd\">").append(p.kdRatio).append("</td>").append("</tr>\n");
			}

			sb.append("</tbody></table>");
		}

		sb.append("</div>");

		// --- Recent games table ---
		sb.append("<div class=\"section\"><h2>Recent Games</h2>");

		if (recentGames.isEmpty()) {
			sb.append("<p class=\"no-data\">No games recorded yet.</p>");
		} else {
			sb.append("""
					    <table>
					        <thead>
					            <tr>
					                <th>Game</th>
					                <th>Winner</th>
					                <th>Duration</th>
					                <th>Played At</th>
					            </tr>
					        </thead>
					        <tbody>
					""");

			for (GameHistoryEntry g : recentGames) {
				String teamClass = "red".equals(g.winnerTeam) ? "team-red" : "team-blue";
				String winnerLabel = g.winnerTeam.substring(0, 1).toUpperCase() + g.winnerTeam.substring(1) + " Team";
				String duration = formatDuration(g.durationSecs);

				sb.append("<tr>").append("<td>#").append(g.id).append("</td>").append("<td class=\"").append(teamClass).append("\">").append(winnerLabel).append("</td>").append("<td>").append(duration).append("</td>").append("<td>").append(g.playedAt).append("</td>").append("</tr>\n");
			}

			sb.append("</tbody></table>");
		}

		sb.append("</div></body></html>");
		return sb.toString();
	}

	/** Converts seconds to "4m 32s" format. */
	private static String formatDuration(int secs) {
		if (secs < 60)
			return secs + "s";
		return (secs / 60) + "m " + (secs % 60) + "s";
	}

	/** Prevents player names with HTML characters from breaking the page. */
	private static String escapeHtml(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
}
