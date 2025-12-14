package org.skyraid.core.listener;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.skyraid.SkyRaidPlugin;
import org.skyraid.core.data.PlayerData;
import org.skyraid.core.data.TeamData;

/**
 * Handles player respawn locations
 * Ensures players respawn at island home (not void spawn)
 */
public class RespawnListener implements Listener {
    
    private final SkyRaidPlugin plugin;
    
    public RespawnListener(SkyRaidPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        // Check if player has an island
        PlayerData playerData = plugin.getTeamManager().getPlayerData(player.getUniqueId());
        
        if (playerData != null && playerData.getCurrentTeamId() != null) {
            TeamData team = plugin.getTeamManager().getTeam(playerData.getCurrentTeamId());
            
            if (team != null) {
                // Respawn at island home
                World skyblockWorld = plugin.getWorldValidator().getOrCreateSkyblockWorld();
                if (skyblockWorld != null) {
                    org.bukkit.Location homeLocation = new org.bukkit.Location(
                        skyblockWorld,
                        team.getHomeX() + 0.5,
                        team.getHomeY(),
                        team.getHomeZ() + 0.5,
                        team.getHomeYaw(),
                        team.getHomePitch()
                    );
                    
                    event.setRespawnLocation(homeLocation);
                    plugin.logDebug("respawn", "Respawning " + player.getName() + " at island home");
                    return;
                }
            }
        }
        
        // No island - respawn at server spawn (NOT skyblock world spawn)
        World spawnWorld = org.bukkit.Bukkit.getWorld("world");
        if (spawnWorld != null) {
            event.setRespawnLocation(spawnWorld.getSpawnLocation());
            plugin.logDebug("respawn", "Respawning " + player.getName() + " at server spawn");
        }
    }
}



