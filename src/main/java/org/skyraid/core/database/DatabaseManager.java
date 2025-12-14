package org.skyraid.core.database;

import org.skyraid.SkyRaidPlugin;
import org.skyraid.core.data.PlayerData;
import org.skyraid.core.data.TeamData;

import java.io.File;
import java.sql.*;
import java.util.*;

/**
 * Manages all database operations using simple JDBC connections.
 * Supports SQLite (default) and MySQL databases.
 */
public class DatabaseManager {
    
    private final SkyRaidPlugin plugin;
    private final String dbType;  // "sqlite" or "mysql"
    private String connectionString;
    
    public DatabaseManager(SkyRaidPlugin plugin) {
        this.plugin = plugin;
        this.dbType = plugin.getConfigManager().getString("database.type", "sqlite").toLowerCase();
        initializeDatabase();
        createTables();
    }
    
    /**
     * Initializes the database connection string
     */
    private void initializeDatabase() {
        try {
            if ("mysql".equalsIgnoreCase(dbType)) {
                // MySQL connection
                String host = plugin.getConfigManager().getString("database.mysql.host", "localhost");
                int port = plugin.getConfigManager().getInt("database.mysql.port", 3306);
                String database = plugin.getConfigManager().getString("database.mysql.database", "skyraid");
                String username = plugin.getConfigManager().getString("database.mysql.username", "root");
                String password = plugin.getConfigManager().getString("database.mysql.password", "");
                
                this.connectionString = String.format(
                    "jdbc:mysql://%s:%d/%s?user=%s&password=%s&allowPublicKeyRetrieval=true&useSSL=false",
                    host, port, database, username, password
                );
                
                // Ensure MySQL driver is available
                Class.forName("com.mysql.cj.jdbc.Driver");
                plugin.logInfo("Using MySQL database: " + host + ":" + port + "/" + database);
                
            } else {
                // SQLite connection (default)
                String defaultPath = "plugins/SkyHuntCore/data/skyhunt.db";
                String dbPath = plugin.getConfigManager().getString("database.path", defaultPath);

                // Reuse legacy database path if present and the new default hasn't been created yet
                if (defaultPath.equals(dbPath)) {
                    File legacyFile = new File("plugins/SkyRaidCore/data/skyraid.db");
                    if (!new File(dbPath).exists() && legacyFile.exists()) {
                        plugin.logInfo("Detected legacy SkyRaidCore database, reusing: " + legacyFile.getPath());
                        dbPath = legacyFile.getPath();
                    }
                }
                
                File dbFile = new File(dbPath);
                dbFile.getParentFile().mkdirs();
                
                this.connectionString = "jdbc:sqlite:" + dbFile.getAbsolutePath();
                
                // Load SQLite driver
                Class.forName("org.sqlite.JDBC");
                plugin.logInfo("Using SQLite database: " + dbPath);
            }
            
        } catch (ClassNotFoundException e) {
            plugin.logError("Failed to load database driver!");
            e.printStackTrace();
        }
    }
    
    /**
     * Creates necessary database tables
     */
    private void createTables() {
        try (Connection conn = getConnection()) {
            Statement stmt = conn.createStatement();
            
            // Teams table
            stmt.execute("CREATE TABLE IF NOT EXISTS teams (" +
                "team_id TEXT PRIMARY KEY," +
                "leader_id TEXT NOT NULL," +
                "team_name TEXT NOT NULL," +
                "created_at LONG NOT NULL," +
                "balance LONG NOT NULL DEFAULT 0," +
                "island_x INT NOT NULL," +
                "island_z INT NOT NULL," +
                "home_x INT NOT NULL," +
                "home_y INT NOT NULL," +
                "home_z INT NOT NULL," +
                "home_yaw FLOAT NOT NULL DEFAULT 0," +
                "home_pitch FLOAT NOT NULL DEFAULT 0," +
                "forcefield_end_time LONG NOT NULL DEFAULT 0" +
            ")");
            
            // Team members table
            stmt.execute("CREATE TABLE IF NOT EXISTS team_members (" +
                "team_id TEXT NOT NULL," +
                "player_uuid TEXT NOT NULL," +
                "joined_at LONG NOT NULL," +
                "PRIMARY KEY (team_id, player_uuid)" +
            ")");
            
            // Team leaders (promoted members)
            stmt.execute("CREATE TABLE IF NOT EXISTS team_leaders (" +
                "team_id TEXT NOT NULL," +
                "player_uuid TEXT NOT NULL," +
                "PRIMARY KEY (team_id, player_uuid)" +
            ")");
            
            // Team invites
            stmt.execute("CREATE TABLE IF NOT EXISTS team_invites (" +
                "team_id TEXT NOT NULL," +
                "player_uuid TEXT NOT NULL," +
                "invited_at LONG NOT NULL," +
                "PRIMARY KEY (team_id, player_uuid)" +
            ")");
            
            // Players table
            stmt.execute("CREATE TABLE IF NOT EXISTS players (" +
                "player_uuid TEXT PRIMARY KEY," +
                "player_name TEXT NOT NULL," +
                "current_team_id TEXT," +
                "use_team_chat BOOLEAN NOT NULL DEFAULT 0," +
                "first_join_time LONG NOT NULL," +
                "last_join_time LONG NOT NULL" +
            ")");
            
            stmt.close();
            plugin.logInfo("Database tables created/verified.");
            
        } catch (SQLException e) {
            plugin.logError("Failed to create database tables!");
            e.printStackTrace();
        }
    }
    
    /**
     * Gets a database connection
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(connectionString);
    }
    
    /**
     * Saves a team to the database
     */
    public void saveTeam(TeamData team) {
        try (Connection conn = getConnection()) {
            // Save main team data
            String teamSql = "INSERT OR REPLACE INTO teams " +
                "(team_id, leader_id, team_name, created_at, balance, island_x, island_z, " +
                "home_x, home_y, home_z, home_yaw, home_pitch, forcefield_end_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement pstmt = conn.prepareStatement(teamSql)) {
                pstmt.setString(1, team.getTeamId().toString());
                pstmt.setString(2, team.getLeaderId().toString());
                pstmt.setString(3, team.getTeamName());
                pstmt.setLong(4, team.getCreatedAt());
                pstmt.setLong(5, team.getBalance());
                pstmt.setInt(6, team.getIslandX());
                pstmt.setInt(7, team.getIslandZ());
                pstmt.setInt(8, team.getHomeX());
                pstmt.setInt(9, team.getHomeY());
                pstmt.setInt(10, team.getHomeZ());
                pstmt.setFloat(11, team.getHomeYaw());
                pstmt.setFloat(12, team.getHomePitch());
                pstmt.setLong(13, team.getForcefieldEndTime());
                pstmt.executeUpdate();
            }
            
            // Clear and save members
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM team_members WHERE team_id = ?")) {
                pstmt.setString(1, team.getTeamId().toString());
                pstmt.executeUpdate();
            }
            
            String memberSql = "INSERT INTO team_members (team_id, player_uuid, joined_at) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(memberSql)) {
                for (UUID member : team.getMembers()) {
                    pstmt.setString(1, team.getTeamId().toString());
                    pstmt.setString(2, member.toString());
                    pstmt.setLong(3, System.currentTimeMillis());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
            
            // Clear and save leaders
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM team_leaders WHERE team_id = ?")) {
                pstmt.setString(1, team.getTeamId().toString());
                pstmt.executeUpdate();
            }
            
            String leaderSql = "INSERT INTO team_leaders (team_id, player_uuid) VALUES (?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(leaderSql)) {
                for (UUID leader : team.getLeaders()) {
                    pstmt.setString(1, team.getTeamId().toString());
                    pstmt.setString(2, leader.toString());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
            
            plugin.logDebug("database", "Team saved: " + team.getTeamName());
            
        } catch (SQLException e) {
            plugin.logError("Failed to save team: " + team.getTeamName());
            e.printStackTrace();
        }
    }
    
    /**
     * Loads a team from the database
     */
    public TeamData loadTeam(UUID teamId) {
        try (Connection conn = getConnection()) {
            String sql = "SELECT * FROM teams WHERE team_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, teamId.toString());
                ResultSet rs = pstmt.executeQuery();
                
                if (!rs.next()) {
                    return null;
                }
                
                TeamData team = new TeamData(
                    UUID.fromString(rs.getString("team_id")),
                    UUID.fromString(rs.getString("leader_id")),
                    rs.getString("team_name"),
                    rs.getInt("island_x"),
                    rs.getInt("island_z")
                );
                
                team.setBalance(rs.getLong("balance"));
                team.setHome(
                    rs.getInt("home_x"),
                    rs.getInt("home_y"),
                    rs.getInt("home_z"),
                    rs.getFloat("home_yaw"),
                    rs.getFloat("home_pitch")
                );
                team.setForcefieldEndTime(rs.getLong("forcefield_end_time"));
                
                // Load members
                String memberSql = "SELECT player_uuid FROM team_members WHERE team_id = ?";
                try (PreparedStatement memberStmt = conn.prepareStatement(memberSql)) {
                    memberStmt.setString(1, teamId.toString());
                    ResultSet memberRs = memberStmt.executeQuery();
                    while (memberRs.next()) {
                        team.addMember(UUID.fromString(memberRs.getString("player_uuid")));
                    }
                }
                
                // Load promoted leaders
                String leaderSql = "SELECT player_uuid FROM team_leaders WHERE team_id = ?";
                try (PreparedStatement leaderStmt = conn.prepareStatement(leaderSql)) {
                    leaderStmt.setString(1, teamId.toString());
                    ResultSet leaderRs = leaderStmt.executeQuery();
                    while (leaderRs.next()) {
                        team.promoteToLeader(UUID.fromString(leaderRs.getString("player_uuid")));
                    }
                }
                
                return team;
            }
            
        } catch (SQLException e) {
            plugin.logError("Failed to load team: " + teamId);
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Saves a player to the database
     */
    public void savePlayer(PlayerData player) {
        try (Connection conn = getConnection()) {
            String sql = "INSERT OR REPLACE INTO players " +
                "(player_uuid, player_name, current_team_id, use_team_chat, first_join_time, last_join_time) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, player.getPlayerUUID().toString());
                pstmt.setString(2, player.getPlayerName());
                pstmt.setString(3, player.getCurrentTeamId() != null ? player.getCurrentTeamId().toString() : null);
                pstmt.setBoolean(4, player.isUsingTeamChat());
                pstmt.setLong(5, player.getFirstJoinTime());
                pstmt.setLong(6, System.currentTimeMillis());
                pstmt.executeUpdate();
            }
            
        } catch (SQLException e) {
            plugin.logError("Failed to save player: " + player.getPlayerName());
            e.printStackTrace();
        }
    }
    
    /**
     * Loads a player from the database
     */
    public PlayerData loadPlayer(UUID playerUUID) {
        try (Connection conn = getConnection()) {
            String sql = "SELECT * FROM players WHERE player_uuid = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, playerUUID.toString());
                ResultSet rs = pstmt.executeQuery();
                
                if (!rs.next()) {
                    return null;
                }
                
                PlayerData player = new PlayerData(
                    UUID.fromString(rs.getString("player_uuid")),
                    rs.getString("player_name")
                );
                
                String teamIdStr = rs.getString("current_team_id");
                if (teamIdStr != null) {
                    player.setCurrentTeamId(UUID.fromString(teamIdStr));
                }
                
                player.setUseTeamChat(rs.getBoolean("use_team_chat"));
                player.setJoinedAt(rs.getLong("first_join_time"));
                
                return player;
            }
            
        } catch (SQLException e) {
            plugin.logError("Failed to load player: " + playerUUID);
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Deletes a team and all its data
     */
    public void deleteTeam(UUID teamId) {
        try (Connection conn = getConnection()) {
            // Delete all team data
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM teams WHERE team_id = ?")) {
                pstmt.setString(1, teamId.toString());
                pstmt.executeUpdate();
            }
            
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM team_members WHERE team_id = ?")) {
                pstmt.setString(1, teamId.toString());
                pstmt.executeUpdate();
            }
            
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM team_leaders WHERE team_id = ?")) {
                pstmt.setString(1, teamId.toString());
                pstmt.executeUpdate();
            }
            
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM team_invites WHERE team_id = ?")) {
                pstmt.setString(1, teamId.toString());
                pstmt.executeUpdate();
            }
            
            plugin.logDebug("database", "Team deleted: " + teamId);
            
        } catch (SQLException e) {
            plugin.logError("Failed to delete team!");
            e.printStackTrace();
        }
    }
    
    /**
     * Loads all teams from the database
     */
    public List<TeamData> loadAllTeams() {
        List<TeamData> teams = new ArrayList<>();
        
        try (Connection conn = getConnection()) {
            String sql = "SELECT team_id FROM teams";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    UUID teamId = UUID.fromString(rs.getString("team_id"));
                    TeamData team = loadTeam(teamId);
                    if (team != null) {
                        teams.add(team);
                    }
                }
            }
            
            plugin.logInfo("Loaded " + teams.size() + " teams from database.");
            
        } catch (SQLException e) {
            plugin.logError("Failed to load teams from database!");
            e.printStackTrace();
        }
        
        return teams;
    }
    
    /**
     * Loads all players from the database
     */
    public List<PlayerData> loadAllPlayers() {
        List<PlayerData> players = new ArrayList<>();
        
        try (Connection conn = getConnection()) {
            String sql = "SELECT player_uuid FROM players";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    UUID playerUUID = UUID.fromString(rs.getString("player_uuid"));
                    PlayerData player = loadPlayer(playerUUID);
                    if (player != null) {
                        players.add(player);
                    }
                }
            }
            
            plugin.logInfo("Loaded " + players.size() + " players from database.");
            
        } catch (SQLException e) {
            plugin.logError("Failed to load players from database!");
            e.printStackTrace();
        }
        
        return players;
    }
    
    /**
     * Shuts down the database (cleanup on plugin disable)
     */
    public void shutdown() {
        plugin.logInfo("Database connections closed.");
    }
}
