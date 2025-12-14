package org.skyhunt.core.database;

import org.skyhunt.SkyHuntCorePlugin;
import org.skyhunt.core.data.IslandData;
import org.skyhunt.core.data.MissionCategory;
import org.skyhunt.core.data.MissionProgress;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Database layer for SkyHuntCore (single-owner islands, missions, heads).
 * Uses SQLite by default with optional MySQL support via config.
 */
public class DatabaseManager {

    private final SkyHuntCorePlugin plugin;
    private final String dbType;
    private String connectionString;

    public DatabaseManager(SkyHuntCorePlugin plugin) {
        this.plugin = plugin;
        this.dbType = plugin.getConfig().getString("database.type", "sqlite").toLowerCase();
        initializeDatabase();
        createTables();
    }

    private void initializeDatabase() {
        try {
            if ("mysql".equalsIgnoreCase(dbType)) {
                String host = plugin.getConfig().getString("database.mysql.host", "localhost");
                int port = plugin.getConfig().getInt("database.mysql.port", 3306);
                String database = plugin.getConfig().getString("database.mysql.database", "skyhunt");
                String username = plugin.getConfig().getString("database.mysql.username", "root");
                String password = plugin.getConfig().getString("database.mysql.password", "");

                this.connectionString = String.format(
                    "jdbc:mysql://%s:%d/%s?user=%s&password=%s&allowPublicKeyRetrieval=true&useSSL=false",
                    host, port, database, username, password
                );

                Class.forName("com.mysql.cj.jdbc.Driver");
                plugin.getLogger().info("Using MySQL database: " + host + ":" + port + "/" + database);
            } else {
                String defaultPath = "plugins/SkyHuntCore/data/skyhunt.db";
                String dbPath = plugin.getConfig().getString("database.path", defaultPath);
                File dbFile = new File(dbPath);
                dbFile.getParentFile().mkdirs();

                this.connectionString = "jdbc:sqlite:" + dbFile.getAbsolutePath();

                Class.forName("org.sqlite.JDBC");
                plugin.getLogger().info("Using SQLite database: " + dbPath);
            }
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("Failed to load database driver: " + e.getMessage());
        }
    }

    private void createTables() {
        try (Connection conn = getConnection()) {
            Statement stmt = conn.createStatement();

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS players (
                  uuid TEXT PRIMARY KEY,
                  island_level INT NOT NULL,
                  created_at LONG NOT NULL,
                  last_login LONG NOT NULL,
                  island_x INT NOT NULL DEFAULT 0,
                  island_z INT NOT NULL DEFAULT 0,
                  home_x INT NOT NULL DEFAULT 0,
                  home_y INT NOT NULL DEFAULT 0,
                  home_z INT NOT NULL DEFAULT 0,
                  home_yaw FLOAT NOT NULL DEFAULT 0,
                  home_pitch FLOAT NOT NULL DEFAULT 0
                )
                """);

            // Backfill columns if table existed previously
            ensureColumn(stmt, "players", "island_x", "INT NOT NULL DEFAULT 0");
            ensureColumn(stmt, "players", "island_z", "INT NOT NULL DEFAULT 0");
            ensureColumn(stmt, "players", "home_x", "INT NOT NULL DEFAULT 0");
            ensureColumn(stmt, "players", "home_y", "INT NOT NULL DEFAULT 0");
            ensureColumn(stmt, "players", "home_z", "INT NOT NULL DEFAULT 0");
            ensureColumn(stmt, "players", "home_yaw", "FLOAT NOT NULL DEFAULT 0");
            ensureColumn(stmt, "players", "home_pitch", "FLOAT NOT NULL DEFAULT 0");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mission_progress (
                  uuid TEXT NOT NULL,
                  category TEXT NOT NULL,
                  mission_id TEXT NOT NULL,
                  progress LONG NOT NULL,
                  completed BOOLEAN NOT NULL DEFAULT 0,
                  PRIMARY KEY (uuid, category, mission_id)
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS unlocked_heads (
                  uuid TEXT NOT NULL,
                  mob_type TEXT NOT NULL,
                  unlocked_at LONG NOT NULL,
                  PRIMARY KEY (uuid, mob_type)
                )
                """);

            stmt.close();
            plugin.getLogger().info("Database tables created/verified.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create database tables: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(connectionString);
    }

    // -------- Island data --------

    public IslandData loadIsland(UUID ownerId, int startingLevel) {
        try (Connection conn = getConnection()) {
            String sql = "SELECT island_level, created_at, last_login FROM players WHERE uuid = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, ownerId.toString());
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    int level = rs.getInt("island_level");
                    long createdAt = rs.getLong("created_at");
                    long lastLogin = rs.getLong("last_login");
                    IslandData data = new IslandData(ownerId, level, createdAt, lastLogin);
                    data.setIslandX(getIntSafe(rs, "island_x", 0));
                    data.setIslandZ(getIntSafe(rs, "island_z", 0));
                    data.setHomeX(getIntSafe(rs, "home_x", 0));
                    data.setHomeY(getIntSafe(rs, "home_y", 0));
                    data.setHomeZ(getIntSafe(rs, "home_z", 0));
                    data.setHomeYaw(getFloatSafe(rs, "home_yaw", 0f));
                    data.setHomePitch(getFloatSafe(rs, "home_pitch", 0f));
                    return data;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load island data for " + ownerId + ": " + e.getMessage());
        }

        long now = System.currentTimeMillis();
        IslandData island = new IslandData(ownerId, startingLevel, now, now);
        saveIsland(island);
        return island;
    }

    public void saveIsland(IslandData island) {
        try (Connection conn = getConnection()) {
            String sql = replaceInto() + " players (uuid, island_level, created_at, last_login, island_x, island_z, home_x, home_y, home_z, home_yaw, home_pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, island.getOwnerId().toString());
                pstmt.setInt(2, island.getIslandLevel());
                pstmt.setLong(3, island.getCreatedAt());
                pstmt.setLong(4, island.getLastLogin());
                pstmt.setInt(5, island.getIslandX());
                pstmt.setInt(6, island.getIslandZ());
                pstmt.setInt(7, island.getHomeX());
                pstmt.setInt(8, island.getHomeY());
                pstmt.setInt(9, island.getHomeZ());
                pstmt.setFloat(10, island.getHomeYaw());
                pstmt.setFloat(11, island.getHomePitch());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save island data for " + island.getOwnerId() + ": " + e.getMessage());
        }
    }

    // -------- Mission progress --------

    public Map<String, MissionProgress> loadMissionProgress(UUID playerId) {
        Map<String, MissionProgress> progressMap = new HashMap<>();
        try (Connection conn = getConnection()) {
            String sql = "SELECT category, mission_id, progress, completed FROM mission_progress WHERE uuid = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, playerId.toString());
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    MissionCategory category = MissionCategory.fromString(rs.getString("category"));
                    String missionId = rs.getString("mission_id");
                    long progress = rs.getLong("progress");
                    boolean completed = rs.getBoolean("completed");
                    String key = buildKey(category, missionId);
                    progressMap.put(key, new MissionProgress(playerId, category, missionId, progress, completed));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load mission progress for " + playerId + ": " + e.getMessage());
        }
        return progressMap;
    }

    public MissionProgress getOrCreateMissionProgress(UUID playerId, MissionCategory category, String missionId) {
        Map<String, MissionProgress> existing = loadMissionProgress(playerId);
        String key = buildKey(category, missionId);
        return existing.getOrDefault(key, new MissionProgress(playerId, category, missionId, 0, false));
    }

    public void saveMissionProgress(MissionProgress progress) {
        try (Connection conn = getConnection()) {
            String sql = replaceInto() + " mission_progress (uuid, category, mission_id, progress, completed) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, progress.getPlayerId().toString());
                pstmt.setString(2, progress.getCategory().name());
                pstmt.setString(3, progress.getMissionId());
                pstmt.setLong(4, progress.getProgress());
                pstmt.setBoolean(5, progress.isCompleted());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save mission progress: " + e.getMessage());
        }
    }

    public void resetMissionProgress(UUID playerId) {
        try (Connection conn = getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM mission_progress WHERE uuid = ?")) {
                pstmt.setString(1, playerId.toString());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to reset mission progress for " + playerId + ": " + e.getMessage());
        }
    }

    // -------- Head unlocks --------

    public Set<String> loadUnlockedHeads(UUID playerId) {
        Set<String> unlocked = new HashSet<>();
        try (Connection conn = getConnection()) {
            String sql = "SELECT mob_type FROM unlocked_heads WHERE uuid = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, playerId.toString());
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    unlocked.add(rs.getString("mob_type"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load unlocked heads for " + playerId + ": " + e.getMessage());
        }
        return unlocked;
    }

    public boolean isHeadUnlocked(UUID playerId, String mobType) {
        String normalized = normalizeMob(mobType);
        try (Connection conn = getConnection()) {
            String sql = "SELECT 1 FROM unlocked_heads WHERE uuid = ? AND mob_type = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, playerId.toString());
                pstmt.setString(2, normalized);
                ResultSet rs = pstmt.executeQuery();
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check unlocked head for " + playerId + ": " + e.getMessage());
            return false;
        }
    }

    public void unlockHead(UUID playerId, String mobType) {
        String normalized = normalizeMob(mobType);
        try (Connection conn = getConnection()) {
            String sql = insertIgnore() + " unlocked_heads (uuid, mob_type, unlocked_at) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, playerId.toString());
                pstmt.setString(2, normalized);
                pstmt.setLong(3, System.currentTimeMillis());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to unlock head " + mobType + " for " + playerId + ": " + e.getMessage());
        }
    }

    public void resetUnlockedHeads(UUID playerId) {
        try (Connection conn = getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM unlocked_heads WHERE uuid = ?")) {
                pstmt.setString(1, playerId.toString());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to reset unlocked heads for " + playerId + ": " + e.getMessage());
        }
    }

    private String buildKey(MissionCategory category, String missionId) {
        return category.name() + ":" + missionId;
    }

    private String normalizeMob(String mobType) {
        return mobType == null ? "" : mobType.trim().toUpperCase();
    }

    private String replaceInto() {
        return "REPLACE INTO";
    }

    private String insertIgnore() {
        return dbType.equals("mysql") ? "INSERT IGNORE" : "INSERT OR IGNORE";
    }

    private void ensureColumn(Statement stmt, String table, String column, String ddl) {
        try {
            stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + ddl);
        } catch (SQLException ignored) {
            // Column likely exists; ignore
        }
    }

    private int getIntSafe(ResultSet rs, String column, int fallback) {
        try {
            return rs.getInt(column);
        } catch (SQLException e) {
            return fallback;
        }
    }

    private float getFloatSafe(ResultSet rs, String column, float fallback) {
        try {
            return rs.getFloat(column);
        } catch (SQLException e) {
            return fallback;
        }
    }
}

