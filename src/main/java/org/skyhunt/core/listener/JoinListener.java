package org.skyhunt.core.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.skyhunt.core.service.IslandService;
import org.skyhunt.core.data.IslandData;

/**
 * Ensures island data exists when player joins.
 */
public class JoinListener implements Listener {

    private final IslandService islands;

    public JoinListener(IslandService islands) {
        this.islands = islands;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        IslandData data = islands.getIsland(event.getPlayer());
        data.setLastLogin(System.currentTimeMillis());
        islands.save(data);
    }
}

