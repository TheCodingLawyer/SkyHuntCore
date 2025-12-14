package org.skyhunt.core.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.skyhunt.core.service.MissionService;

/**
 * Tracks block breaks for mining/farming missions.
 */
public class BlockListener implements Listener {

    private final MissionService missions;

    public BlockListener(MissionService missions) {
        this.missions = missions;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        missions.handleBlock(event.getPlayer(), event.getBlock().getType().name());
    }
}




