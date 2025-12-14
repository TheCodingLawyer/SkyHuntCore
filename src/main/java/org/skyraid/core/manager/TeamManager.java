package org.skyraid.core.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.skyraid.SkyRaidPlugin;
import org.skyraid.core.data.PendingTeam;
import org.skyraid.core.data.PlayerData;
import org.skyraid.core.data.TeamData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages teams and team member operations
 */
public class TeamManager {
    
    private final SkyRaidPlugin plugin;
    private final Map<UUID, TeamData> teamCache = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerData> playerCache = new ConcurrentHashMap<>();
    private final Map<UUID, PendingTeam> pendingTeams = new ConcurrentHashMap<>();  // Owner UUID -> PendingTeam
    
    public TeamManager(SkyRaidPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Creates a new team
     */
    public TeamData createTeam(Player leader, String teamName) {
        UUID leaderId = leader.getUniqueId();
        
        // Check if leader is already in a team
        if (playerCache.containsKey(leaderId)) {
            PlayerData existing = playerCache.get(leaderId);
            if (existing.isInTeam()) {
                return null;
            }
        }
        
        // Generate island and create team
        TeamData team = plugin.getIslandManager().generateIsland(leaderId, teamName);
        
        if (team != null) {
            teamCache.put(team.getTeamId(), team);
            
            // Update player data
            PlayerData playerData = new PlayerData(leaderId, leader.getName());
            playerData.setCurrentTeamId(team.getTeamId());
            playerCache.put(leaderId, playerData);
            plugin.getDatabaseManager().savePlayer(playerData);
            
            plugin.logDebug("island_management", "Team created: " + teamName + " by " + leader.getName());
        }
        
        return team;
    }
    
    /**
     * Gets a team by ID
     */
    public TeamData getTeam(UUID teamId) {
        if (teamCache.containsKey(teamId)) {
            return teamCache.get(teamId);
        }
        
        // Load from database
        TeamData team = plugin.getDatabaseManager().loadTeam(teamId);
        if (team != null) {
            teamCache.put(teamId, team);
        }
        
        return team;
    }
    
    /**
     * Gets a player's team
     */
    public TeamData getPlayerTeam(Player player) {
        return getPlayerTeam(player.getUniqueId());
    }
    
    /**
     * Gets a player's team by UUID
     */
    public TeamData getPlayerTeam(UUID playerUUID) {
        PlayerData playerData = getPlayerData(playerUUID);
        if (playerData != null && playerData.isInTeam()) {
            return getTeam(playerData.getCurrentTeamId());
        }
        return null;
    }
    
    /**
     * Gets or creates player data
     */
    public PlayerData getPlayerData(UUID playerUUID) {
        if (playerCache.containsKey(playerUUID)) {
            return playerCache.get(playerUUID);
        }
        
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) {
            return null;
        }
        
        // Load from database or create new
        PlayerData data = plugin.getDatabaseManager().loadPlayer(playerUUID);
        if (data == null) {
            data = new PlayerData(playerUUID, player.getName());
        }
        
        playerCache.put(playerUUID, data);
        return data;
    }
    
    /**
     * Adds a player to a team
     */
    public boolean addPlayerToTeam(Player player, TeamData team) {
        UUID playerUUID = player.getUniqueId();
        
        // Check if team is full
        if (team.getMemberCount() >= plugin.getConfigManager().getInt("island.max_members_per_island", 8)) {
            return false;
        }
        
        // Remove from old team if in one
        TeamData oldTeam = getPlayerTeam(playerUUID);
        if (oldTeam != null) {
            removePlayerFromTeam(playerUUID, oldTeam);
        }
        
        // Add to new team
        team.addMember(playerUUID);
        team.removeInvite(playerUUID);
        
        // Update player data
        PlayerData playerData = getPlayerData(playerUUID);
        if (playerData == null) {
            playerData = new PlayerData(playerUUID, player.getName());
        }
        playerData.setCurrentTeamId(team.getTeamId());
        playerCache.put(playerUUID, playerData);
        
        // Save to database
        plugin.getDatabaseManager().saveTeam(team);
        plugin.getDatabaseManager().savePlayer(playerData);
        
        plugin.logDebug("island_management", player.getName() + " joined team: " + team.getTeamName());
        return true;
    }
    
    /**
     * Removes a player from a team
     */
    public void removePlayerFromTeam(UUID playerUUID, TeamData team) {
        team.removeMember(playerUUID);
        
        // Update player data
        PlayerData playerData = getPlayerData(playerUUID);
        if (playerData != null) {
            playerData.setCurrentTeamId(null);
            plugin.getDatabaseManager().savePlayer(playerData);
        }
        
        // Save team
        plugin.getDatabaseManager().saveTeam(team);
        
        // If team is empty, delete it
        if (team.getMemberCount() == 0) {
            deleteTeam(team);
        }
    }
    
    /**
     * Deletes a team
     */
    public void deleteTeam(TeamData team) {
        // Remove all members from cache
        for (UUID member : team.getMembers()) {
            PlayerData playerData = playerCache.get(member);
            if (playerData != null) {
                playerData.setCurrentTeamId(null);
                plugin.getDatabaseManager().savePlayer(playerData);
            }
        }
        
        // Delete island
        plugin.getIslandManager().deleteIsland(team);
        
        // Delete from database
        plugin.getDatabaseManager().deleteTeam(team.getTeamId());
        
        // Remove from cache
        teamCache.remove(team.getTeamId());
    }
    
    /**
     * Invites a player to a team
     */
    public void invitePlayerToTeam(UUID playerUUID, TeamData team) {
        team.addInvite(playerUUID);
        plugin.getDatabaseManager().saveTeam(team);
        plugin.logDebug("island_management", "Player invited to team: " + team.getTeamName());
    }
    
    /**
     * Accepts a team invite
     */
    public boolean acceptInvite(Player player, TeamData team) {
        if (!team.hasInvite(player.getUniqueId())) {
            return false;
        }
        
        return addPlayerToTeam(player, team);
    }
    
    /**
     * Promotes a team member to leader
     */
    public void promoteMember(UUID playerUUID, TeamData team) {
        if (team.isMember(playerUUID)) {
            team.promoteToLeader(playerUUID);
            plugin.getDatabaseManager().saveTeam(team);
            plugin.logDebug("island_management", "Member promoted to leader");
        }
    }
    
    /**
     * Demotes a leader
     */
    public void demoteMember(UUID playerUUID, TeamData team) {
        team.demoteFromLeader(playerUUID);
        plugin.getDatabaseManager().saveTeam(team);
        plugin.logDebug("island_management", "Member demoted from leader");
    }
    
    /**
     * Saves all teams to database
     */
    public void saveAllTeams() {
        for (TeamData team : teamCache.values()) {
            plugin.getDatabaseManager().saveTeam(team);
        }
        plugin.logInfo("All teams saved.");
    }
    
    /**
     * Gets all teams
     */
    public Collection<TeamData> getAllTeams() {
        return teamCache.values();
    }
    
    // ========== Pending Team Management (Pre-Island Phase) ==========
    
    /**
     * Get or create a pending team for a player
     */
    public PendingTeam getOrCreatePendingTeam(Player owner) {
        return pendingTeams.computeIfAbsent(owner.getUniqueId(), uuid -> new PendingTeam(owner));
    }
    
    /**
     * Get a pending team by owner
     */
    public PendingTeam getPendingTeam(Player owner) {
        return pendingTeams.get(owner.getUniqueId());
    }
    
    /**
     * Remove a pending team
     */
    public void removePendingTeam(Player owner) {
        pendingTeams.remove(owner.getUniqueId());
    }
    
    /**
     * Clean up expired pending teams
     */
    public void cleanupExpiredPendingTeams() {
        pendingTeams.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    // ========== Cache Management Methods ==========
    
    /**
     * Add player to cache (for singleplayer island creation)
     */
    public void addPlayerToCache(UUID playerUUID, PlayerData playerData) {
        playerCache.put(playerUUID, playerData);
    }
    
    /**
     * Add team to cache (for singleplayer island creation)
     */
    public void addTeamToCache(UUID teamId, TeamData team) {
        teamCache.put(teamId, team);
    }
    
    /**
     * Set a pending invite for a player (for pending team invites)
     */
    public void setPendingInvite(UUID targetPlayer, UUID inviterPlayer) {
        PlayerData playerData = getPlayerData(targetPlayer);
        if (playerData == null) {
            // Create new player data if it doesn't exist
            Player player = Bukkit.getPlayer(targetPlayer);
            if (player != null) {
                playerData = new PlayerData(targetPlayer, player.getName());
                playerCache.put(targetPlayer, playerData);
            } else {
                return;
            }
        }
        
        playerData.setPendingInvite(inviterPlayer);
        plugin.getDatabaseManager().savePlayer(playerData);
    }
    
    /**
     * Loads all teams from database into cache on startup
     * CRITICAL: Must be called on plugin enable!
     */
    public void loadAllFromDatabase() {
        plugin.logInfo("Loading teams and players from database...");
        
        // Load all teams
        List<TeamData> teams = plugin.getDatabaseManager().loadAllTeams();
        for (TeamData team : teams) {
            teamCache.put(team.getTeamId(), team);
            plugin.logDebug("database", "Loaded team: " + team.getTeamName() + " (" + team.getMembers().size() + " members)");
        }
        
        // Load all players
        List<PlayerData> players = plugin.getDatabaseManager().loadAllPlayers();
        for (PlayerData player : players) {
            playerCache.put(player.getPlayerUUID(), player);
            plugin.logDebug("database", "Loaded player: " + player.getPlayerName());
        }
        
        plugin.logInfo("§a✓ Loaded " + teams.size() + " teams and " + players.size() + " players from database.");
    }
}

