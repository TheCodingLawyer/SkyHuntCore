package org.skyraid.core.listener;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.entity.Player;
import org.skyraid.SkyRaidPlugin;
import org.skyraid.core.data.TeamData;

/**
 * Handles PvP, block interaction permissions, and forcefield entry prevention
 */
public class PvPListener implements Listener {
    
    private final SkyRaidPlugin plugin;
    
    public PvPListener(SkyRaidPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Prevents players from entering forcefield-protected islands
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only check when player moves to a new block
        Location from = event.getFrom();
        Location to = event.getTo();
        
        if (to == null || (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ())) {
            return; // Just looking around, not moving
        }
        
        Player player = event.getPlayer();
        TeamData playerTeam = plugin.getTeamManager().getPlayerTeam(player);
        
        // Find which island they're trying to enter
        TeamData targetTeam = plugin.getIslandManager().findTeamAtLocation(to);
        
        if (targetTeam == null) {
            return; // Not entering any island
        }
        
        // Check if player is member of target team
        if (playerTeam != null && playerTeam.getTeamId().equals(targetTeam.getTeamId())) {
            return; // Own island, always allowed
        }
        
        // Check if target island has forcefield
        if (targetTeam.hasForcefield()) {
            event.setCancelled(true);
            
            // Push player back slightly
            Location pushBack = from.clone();
            pushBack.setYaw(to.getYaw());
            pushBack.setPitch(to.getPitch());
            player.teleport(pushBack);
            
            player.sendMessage("§c§l⚠ Forcefield Active!");
            player.sendMessage("§7This island is protected by a forcefield.");
            player.sendMessage("§7Time remaining: §e" + targetTeam.getRemainingForcefieldHours() + " hours");
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        // ForcefieldManager handles messages
        if (!plugin.getForcefieldManager().canBreakBlocks(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        // ForcefieldManager handles messages
        if (!plugin.getForcefieldManager().canBreakBlocks(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player attacker = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();
        
        // ForcefieldManager handles messages
        if (!plugin.getForcefieldManager().canPvP(attacker, victim.getLocation())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteractBlock(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }
        
        String blockName = event.getClickedBlock().getType().name();
        
        // Check if it's a protected block (container)
        if (plugin.getConfigManager().getStringList("permissions.protected_blocks")
                .contains(blockName)) {
            
            // ForcefieldManager handles messages
            if (!plugin.getForcefieldManager().canOpenContainers(event.getPlayer(), event.getClickedBlock().getLocation())) {
                event.setCancelled(true);
            }
        }
    }
}

