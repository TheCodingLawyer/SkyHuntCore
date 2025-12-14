package org.skyraid.core.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.skyraid.SkyRaidPlugin;
import org.skyraid.core.data.PlayerData;
import org.skyraid.core.data.TeamData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles chat confirmations for dangerous actions
 * (e.g., disband island, delete team)
 */
public class ChatConfirmationListener implements Listener {
    
    private final SkyRaidPlugin plugin;
    
    // Track pending confirmations: UUID -> ConfirmationType
    private final Map<UUID, ConfirmationType> pendingConfirmations = new ConcurrentHashMap<>();
    
    public enum ConfirmationType {
        DISBAND_ISLAND,
        DELETE_ISLAND,
        LEAVE_ISLAND
    }
    
    public ChatConfirmationListener(SkyRaidPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Request confirmation from a player
     */
    public void requestConfirmation(Player player, ConfirmationType type) {
        pendingConfirmations.put(player.getUniqueId(), type);
        
        player.sendMessage("§c§l⚠ CONFIRMATION REQUIRED ⚠");
        player.sendMessage("§7");
        player.sendMessage("§7Type §cCONFIRM§7 in chat to continue.");
        player.sendMessage("§7Type §aanything else§7 to cancel.");
        player.sendMessage("§7");
        player.sendMessage("§c§oThis action cannot be undone!");
    }
    
    /**
     * Cancel pending confirmation
     */
    public void cancelConfirmation(UUID playerUUID) {
        pendingConfirmations.remove(playerUUID);
    }
    
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        
        if (!pendingConfirmations.containsKey(playerUUID)) {
            return; // No pending confirmation
        }
        
        event.setCancelled(true); // Don't broadcast confirmation message
        
        ConfirmationType type = pendingConfirmations.get(playerUUID);
        String message = event.getMessage();
        
        if (message.equalsIgnoreCase("CONFIRM")) {
            // Confirmed - execute action on main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                executeConfirmedAction(player, type);
            });
        } else {
            // Cancelled
            player.sendMessage("§a§lCancelled.");
        }
        
        pendingConfirmations.remove(playerUUID);
    }
    
    /**
     * Execute the confirmed action
     */
    private void executeConfirmedAction(Player player, ConfirmationType type) {
        PlayerData playerData = plugin.getTeamManager().getPlayerData(player.getUniqueId());
        
        if (playerData == null || playerData.getCurrentTeamId() == null) {
            player.sendMessage("§cYou are not in a team!");
            return;
        }
        
        TeamData team = plugin.getTeamManager().getTeam(playerData.getCurrentTeamId());
        
        if (team == null) {
            player.sendMessage("§cTeam not found!");
            return;
        }
        
        switch (type) {
            case DISBAND_ISLAND:
                handleDisbandIsland(player, team);
                break;
            case DELETE_ISLAND:
                handleDeleteIsland(player, team);
                break;
            case LEAVE_ISLAND:
                handleLeaveIsland(player, team);
                break;
        }
    }
    
    private void handleDisbandIsland(Player player, TeamData team) {
        if (!team.getLeaderId().equals(player.getUniqueId())) {
            player.sendMessage("§cOnly the island owner can disband!");
            return;
        }
        
        if (team.getMemberCount() > 1) {
            player.sendMessage("§cYou must kick all members before disbanding!");
            return;
        }
        
        // Teleport to spawn FIRST (before island deletion)
        org.bukkit.World spawnWorld = findSafeSpawnWorld();
        if (spawnWorld != null) {
            player.teleport(spawnWorld.getSpawnLocation());
            player.sendMessage("§7Teleporting to spawn...");
        } else {
            plugin.logError("Could not find a safe spawn world for player " + player.getName());
        }
        
        // End forcefield if active
        if (team.hasForcefield()) {
            plugin.getForcefieldManager().deactivateForcefield(team);
        }
        
        // Delete island (removes blocks if configured)
        plugin.getIslandManager().deleteIsland(team);
        
        // Remove player from team
        PlayerData data = plugin.getTeamManager().getPlayerData(player.getUniqueId());
        data.setCurrentTeamId(null);
        plugin.getDatabaseManager().savePlayer(data);
        
        // Delete team from database
        plugin.getDatabaseManager().deleteTeam(team.getTeamId());
        
        player.sendMessage("§c§lIsland Disbanded!");
        player.sendMessage("§7Your island has been permanently deleted.");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.0f);
    }
    
    private void handleDeleteIsland(Player player, TeamData team) {
        // Same as disband for now
        handleDisbandIsland(player, team);
    }
    
    private void handleLeaveIsland(Player player, TeamData team) {
        if (team.getLeaderId().equals(player.getUniqueId())) {
            player.sendMessage("§cYou cannot leave as the owner! Disband the island instead.");
            return;
        }
        
        // Remove player from team
        team.removeMember(player.getUniqueId());
        plugin.getDatabaseManager().saveTeam(team);
        
        PlayerData playerData = plugin.getTeamManager().getPlayerData(player.getUniqueId());
        playerData.setCurrentTeamId(null);
        plugin.getDatabaseManager().savePlayer(playerData);
        
        player.sendMessage("§a§lLeft Island!");
        player.sendMessage("§7You have left the island team.");
        
        // Teleport to spawn
        org.bukkit.World spawnWorld = findSafeSpawnWorld();
        if (spawnWorld != null) {
            player.teleport(spawnWorld.getSpawnLocation());
        } else {
            plugin.logError("Could not find a safe spawn world for player " + player.getName());
        }
    }
    
    /**
     * Find a safe spawn world (any non-void world)
     * Prioritizes: "world" → first non-skyblock world → any world
     */
    private org.bukkit.World findSafeSpawnWorld() {
        // Try "world" first (common name)
        org.bukkit.World world = org.bukkit.Bukkit.getWorld("world");
        if (world != null && !world.getName().equalsIgnoreCase("skyblock")) {
            return world;
        }
        
        // Find first non-skyblock world
        for (org.bukkit.World w : org.bukkit.Bukkit.getWorlds()) {
            if (!w.getName().equalsIgnoreCase("skyblock") && 
                !w.getName().equalsIgnoreCase("skyblock_nether") &&
                !w.getName().equalsIgnoreCase("skyblock_the_end")) {
                return w;
            }
        }
        
        // Fallback to first world if all else fails
        if (!org.bukkit.Bukkit.getWorlds().isEmpty()) {
            return org.bukkit.Bukkit.getWorlds().get(0);
        }
        
        return null;
    }
}

