// --------------------------------------------------------------------------------------------
// DatabaseManager.java
// --------------------------------------------------------------------------------------------
// Author: Matej Stastny
// Date: 2026-02-21 (YYYY-MM-DD)
// License: MIT
// Link: https://github.com/matejstastny/flaggi
// --------------------------------------------------------------------------------------------

package flaggi.server.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages all database operations for Flaggi.
 *
 * We use SQLite via JDBC - think of it as a single .db file on disk that we
 * talk to using SQL queries (structured text commands).
 *
 * Call DatabaseManager.initialize() once when the server starts, then use the
 * static methods anywhere in the server to save/read data.
 */
public class DatabaseManager {

    // Path to the SQLite file. Will be created automatically if it doesn't exist.
    private static final String DB_PATH = "flaggi.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    /**
     * Call this once when the server starts. Creates the database file and all
     * tables if they don't already exist.
     */
    public static void initialize() {
        try (Connection conn = connect();
                Statement stmt = conn.createStatement()) {

            // players table - one row per unique player name.
            // "INTEGER PRIMARY KEY" is SQLite's auto-incrementing ID.
            stmt.execute(
                    """
					    CREATE TABLE IF NOT EXISTS players (
					        id            INTEGER PRIMARY KEY,
					        name          TEXT    NOT NULL UNIQUE,
					        total_kills   INTEGER NOT NULL DEFAULT 0,
					        total_deaths  INTEGER NOT NULL DEFAULT 0,
					        total_wins    INTEGER NOT NULL DEFAULT 0,
					        total_losses  INTEGER NOT NULL DEFAULT 0,
					        games_played  INTEGER NOT NULL DEFAULT 0,
					        first_seen    TEXT    NOT NULL DEFAULT (datetime('now')),
					        last_seen     TEXT    NOT NULL DEFAULT (datetime('now'))
					    )
					""");

            // games table - one row per match.
            stmt.execute(
                    """
					    CREATE TABLE IF NOT EXISTS games (
					        id            INTEGER PRIMARY KEY,
					        winner_team   TEXT    NOT NULL,   -- 'red' or 'blue'
					        duration_secs INTEGER NOT NULL,
					        played_at     TEXT    NOT NULL DEFAULT (datetime('now'))
					    )
					""");

            // player_game_stats - links players to games.
            // One row per player per game, storing what they did in that match.
            // "REFERENCES" means this column must point to a real row in the other table.
            stmt.execute(
                    """
					    CREATE TABLE IF NOT EXISTS player_game_stats (
					        id        INTEGER PRIMARY KEY,
					        player_id INTEGER NOT NULL REFERENCES players(id),
					        game_id   INTEGER NOT NULL REFERENCES games(id),
					        team      TEXT    NOT NULL,   -- 'red' or 'blue'
					        kills     INTEGER NOT NULL DEFAULT 0,
					        deaths    INTEGER NOT NULL DEFAULT 0,
					        won       INTEGER NOT NULL DEFAULT 0    -- 0 = loss, 1 = win (SQLite has no boolean)
					    )
					""");

            System.out.println("[DB] Database initialized at: " + DB_PATH);

        } catch (SQLException e) {
            System.err.println("[DB] Failed to initialize database: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Writing data
    // -------------------------------------------------------------------------

    /**
     * Call this at the end of each match to save everything.
     *
     * @param winnerTeam   "red" or "blue"
     * @param durationSecs how long the game lasted in seconds
     * @param playerStats  list of what each player did this match
     */
    public static void saveGame(String winnerTeam, int durationSecs, List<PlayerMatchStats> playerStats) {
        try (Connection conn = connect()) {
            // Wrap everything in a transaction - either all of it saves, or none of it
            // does.
            // This prevents half-saved data if something goes wrong mid-way.
            conn.setAutoCommit(false);

            try {
                // 1. Insert the game row and get its auto-generated ID back.
                int gameId = insertGame(conn, winnerTeam, durationSecs);

                // 2. For each player, upsert (insert or update) their row in `players`,
                // then insert their per-game stats.
                for (PlayerMatchStats stats : playerStats) {
                    int playerId = upsertPlayer(conn, stats.playerName);
                    insertPlayerGameStats(conn, playerId, gameId, stats);
                    updatePlayerTotals(conn, playerId, stats);
                }

                conn.commit();
                System.out.println("[DB] Saved game #" + gameId + " (" + playerStats.size() + " players)");

            } catch (SQLException e) {
                conn.rollback(); // Undo everything if something failed.
                System.err.println("[DB] Failed to save game, rolled back: " + e.getMessage());
            }

        } catch (SQLException e) {
            System.err.println("[DB] Connection error while saving game: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Reading data (for the leaderboard)
    // -------------------------------------------------------------------------

    /**
     * Returns all players sorted by wins descending. This is the main leaderboard
     * query.
     */
    public static List<LeaderboardEntry> getLeaderboard() {
        List<LeaderboardEntry> entries = new ArrayList<>();
        String sql =
                """
				    SELECT
				        name,
				        total_kills,
				        total_deaths,
				        total_wins,
				        total_losses,
				        games_played,
				        -- K/D ratio: kills divided by deaths, defaulting to kills if deaths = 0
				        CASE WHEN total_deaths = 0 THEN total_kills
				             ELSE ROUND(CAST(total_kills AS REAL) / total_deaths, 2)
				        END AS kd_ratio
				    FROM players
				    ORDER BY total_wins DESC, kd_ratio DESC
				""";

        try (Connection conn = connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                entries.add(new LeaderboardEntry(
                        rs.getString("name"),
                        rs.getInt("total_kills"),
                        rs.getInt("total_deaths"),
                        rs.getInt("total_wins"),
                        rs.getInt("total_losses"),
                        rs.getInt("games_played"),
                        rs.getDouble("kd_ratio")));
            }

        } catch (SQLException e) {
            System.err.println("[DB] Failed to fetch leaderboard: " + e.getMessage());
        }

        return entries;
    }

    /**
     * Returns the last N games played, most recent first.
     */
    public static List<GameHistoryEntry> getRecentGames(int limit) {
        List<GameHistoryEntry> entries = new ArrayList<>();
        String sql =
                """
				    SELECT id, winner_team, duration_secs, played_at
				    FROM games
				    ORDER BY played_at DESC
				    LIMIT ?
				""";

        try (Connection conn = connect();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                entries.add(new GameHistoryEntry(
                        rs.getInt("id"),
                        rs.getString("winner_team"),
                        rs.getInt("duration_secs"),
                        rs.getString("played_at")));
            }

        } catch (SQLException e) {
            System.err.println("[DB] Failed to fetch recent games: " + e.getMessage());
        }

        return entries;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Opens a connection to the SQLite file. Always use in a try-with-resources
     * block.
     */
    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /** Inserts a game row and returns the auto-generated game ID. */
    private static int insertGame(Connection conn, String winnerTeam, int durationSecs) throws SQLException {
        String sql = "INSERT INTO games (winner_team, duration_secs) VALUES (?, ?)";
        // RETURN_GENERATED_KEYS tells JDBC to give us back the auto-generated ID.
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, winnerTeam);
            ps.setInt(2, durationSecs);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            keys.next();
            return keys.getInt(1);
        }
    }

    /**
     * "Upsert" = INSERT if the player doesn't exist, do nothing if they do. Then
     * fetch their ID either way.
     */
    private static int upsertPlayer(Connection conn, String name) throws SQLException {
        // INSERT OR IGNORE skips the insert if the name already exists (due to UNIQUE).
        String insert = "INSERT OR IGNORE INTO players (name) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(insert)) {
            ps.setString(1, name);
            ps.executeUpdate();
        }

        // Now fetch the ID (works whether we just inserted or it already existed).
        String select = "SELECT id FROM players WHERE name = ?";
        try (PreparedStatement ps = conn.prepareStatement(select)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt("id");
        }
    }

    /**
     * Inserts a row into player_game_stats for one player's performance in one
     * game.
     */
    private static void insertPlayerGameStats(Connection conn, int playerId, int gameId, PlayerMatchStats stats)
            throws SQLException {
        String sql =
                "INSERT INTO player_game_stats (player_id, game_id, team, kills, deaths, won) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playerId);
            ps.setInt(2, gameId);
            ps.setString(3, stats.team);
            ps.setInt(4, stats.kills);
            ps.setInt(5, stats.deaths);
            ps.setInt(6, stats.won ? 1 : 0);
            ps.executeUpdate();
        }
    }

    /** Adds this match's stats onto the player's running totals. */
    private static void updatePlayerTotals(Connection conn, int playerId, PlayerMatchStats stats) throws SQLException {
        String sql =
                """
				    UPDATE players SET
				        total_kills  = total_kills  + ?,
				        total_deaths = total_deaths + ?,
				        total_wins   = total_wins   + ?,
				        total_losses = total_losses + ?,
				        games_played = games_played + 1,
				        last_seen    = datetime('now')
				    WHERE id = ?
				""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, stats.kills);
            ps.setInt(2, stats.deaths);
            ps.setInt(3, stats.won ? 1 : 0);
            ps.setInt(4, stats.won ? 0 : 1);
            ps.setInt(5, playerId);
            ps.executeUpdate();
        }
    }

    // -------------------------------------------------------------------------
    // Data classes
    // -------------------------------------------------------------------------

    /** What you pass in when saving a game - one per player. */
    public static class PlayerMatchStats {
        public final String playerName;
        public final String team; // "red" or "blue"
        public final int kills;
        public final int deaths;
        public final boolean won;

        public PlayerMatchStats(String playerName, String team, int kills, int deaths, boolean won) {
            this.playerName = playerName;
            this.team = team;
            this.kills = kills;
            this.deaths = deaths;
            this.won = won;
        }
    }

    /** One row of the leaderboard. */
    public static class LeaderboardEntry {
        public final String name;
        public final int kills, deaths, wins, losses, gamesPlayed;
        public final double kdRatio;

        public LeaderboardEntry(
                String name, int kills, int deaths, int wins, int losses, int gamesPlayed, double kdRatio) {
            this.name = name;
            this.kills = kills;
            this.deaths = deaths;
            this.wins = wins;
            this.losses = losses;
            this.gamesPlayed = gamesPlayed;
            this.kdRatio = kdRatio;
        }
    }

    /** One row of the recent games list. */
    public static class GameHistoryEntry {
        public final int id, durationSecs;
        public final String winnerTeam, playedAt;

        public GameHistoryEntry(int id, String winnerTeam, int durationSecs, String playedAt) {
            this.id = id;
            this.winnerTeam = winnerTeam;
            this.durationSecs = durationSecs;
            this.playedAt = playedAt;
        }
    }
}
