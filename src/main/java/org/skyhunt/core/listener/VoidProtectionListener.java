package org.skyhunt.core.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.skyhunt.core.data.IslandData;
import org.skyhunt.core.service.IslandService;

/**
 * Simple void protection: teleport players back to their island home if they fall.
 */
public class VoidProtectionListener implements Listener {

    private final IslandService islands;

    public VoidProtectionListener(IslandService islands) {
        this.islands = islands;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        if (event.getTo().getY() < 0) {
            IslandData data = islands.getIsland(event.getPlayer());
            islands.teleportHome(event.getPlayer(), data);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        IslandData data = islands.getIsland(event.getPlayer());
        var home = islands.getHomeLocation(data);
        if (home != null) {
            event.setRespawnLocation(home);
        }
    }
}




