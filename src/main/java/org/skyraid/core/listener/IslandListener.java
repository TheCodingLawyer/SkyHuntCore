package org.skyraid.core.listener;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.skyraid.SkyRaidPlugin;
import org.skyraid.core.data.PlayerData;
import org.skyraid.core.data.TeamData;

/**
 * Handles island-related events
 * Includes void fall protection and void damage
 */
public class IslandListener implements Listener {
    
    private final SkyRaidPlugin plugin;
    
    public IslandListener(SkyRaidPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is below void threshold
        int voidThreshold = plugin.getConfig().getInt("island.void_teleport_y_threshold", 0);
        
        if (player.getLocation().getY() < voidThreshold) {
            // Check if void teleport back is enabled
            boolean teleportBack = plugin.getConfig().getBoolean("island.void_teleport_back", true);
            
            if (teleportBack) {
                handleVoidTeleport(player);
            } else {
                handleVoidDamage(player);
            }
        }
    }
    
    /**
     * Teleport player back to safety when falling into void
     */
    private void handleVoidTeleport(Player player) {
        // Check if player has an island
        PlayerData playerData = plugin.getTeamManager().getPlayerData(player.getUniqueId());
        
        if (playerData != null && playerData.getCurrentTeamId() != null) {
            TeamData team = plugin.getTeamManager().getTeam(playerData.getCurrentTeamId());
            
            if (team != null) {
                World skyblockWorld = plugin.getWorldValidator().getOrCreateSkyblockWorld();
                if (skyblockWorld == null) return;
                
                // Check config: teleport to home or island center?
                boolean toHome = plugin.getConfig().getBoolean("island.void_teleport_back_to_home", true);
                
                Location safeLocation;
                if (toHome) {
                    // Teleport to home
                    safeLocation = new Location(
                        skyblockWorld,
                        team.getHomeX() + 0.5,
                        team.getHomeY(),
                        team.getHomeZ() + 0.5,
                        team.getHomeYaw(),
                        team.getHomePitch()
                    );
                } else {
                    // Teleport to island center
                    int islandHeight = plugin.getConfig().getInt("island.height", 64);
                    safeLocation = new Location(
                        skyblockWorld,
                        team.getIslandX() + 0.5,
                        islandHeight + 5,
                        team.getIslandZ() + 0.5
                    );
                }
                
                // Reset fall distance and teleport
                player.setFallDistance(0.0F);
                player.teleport(safeLocation);
                player.sendMessage("§e§lOops!§r §7You fell into the void. Teleported back to safety!");
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                
                plugin.logDebug("island_management", "Teleported " + player.getName() + " back from void");
                return;
            }
        }
        
        // No island - teleport to server spawn
        World spawnWorld = org.bukkit.Bukkit.getWorld("world");
        if (spawnWorld != null) {
            player.setFallDistance(0.0F);
            player.teleport(spawnWorld.getSpawnLocation());
            player.sendMessage("§e§lOops!§r §7You fell into the void. Teleported to spawn!");
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        }
    }
    
    /**
     * Apply void damage (legacy mode)
     */
    private void handleVoidDamage(Player player) {
        boolean damageEnabled = plugin.getConfig().getBoolean("island.void_damage_enabled", false);
        
        if (damageEnabled) {
            plugin.logDebug("island_management", "Player in void: " + player.getName());
            double damage = plugin.getConfig().getDouble("island.void_damage_amount", 2.0);
            player.damage(damage);
        }
    }
}

