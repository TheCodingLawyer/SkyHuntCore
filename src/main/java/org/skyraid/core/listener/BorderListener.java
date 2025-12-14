package org.skyraid.core.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.skyraid.SkyRaidPlugin;

/**
 * Handles island border display for players
 * Shows colored borders around islands when players enter them
 */
public class BorderListener implements Listener {
    
    private final SkyRaidPlugin plugin;
    
    public BorderListener(SkyRaidPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Update border when player moves
     * Only triggers if player moves to a new block
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only check when player moves to a new block (not just looking around)
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        
        plugin.getBorderManager().sendIslandBorder(event.getPlayer());
    }
    
    /**
     * Update border when player joins
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Delay slightly to ensure world is fully loaded
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getBorderManager().sendIslandBorder(event.getPlayer());
        }, 10L); // 0.5 second delay
    }
    
    /**
     * Update border when player teleports
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // Delay slightly to ensure teleport completes
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getBorderManager().sendIslandBorder(event.getPlayer());
        }, 2L); // 0.1 second delay
    }
}



