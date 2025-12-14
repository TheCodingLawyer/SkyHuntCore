package org.skyraid.core.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.skyraid.SkyRaidPlugin;
import org.skyraid.core.data.TeamData;

/**
 * Handles forcefield item usage
 */
public class ForcefieldListener implements Listener {
    
    private final SkyRaidPlugin plugin;
    
    public ForcefieldListener(SkyRaidPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        ItemStack item = event.getItem();
        long durationHours = plugin.getForcefieldManager().getForcefieldItemDuration(item);
        
        if (durationHours <= 0) {
            return;
        }
        
        event.setCancelled(true);
        
        TeamData team = plugin.getTeamManager().getPlayerTeam(event.getPlayer());
        if (team == null) {
            event.getPlayer().sendMessage(plugin.getConfigManager().getString(
                "messages.errors.not_in_island",
                "§cYou are not in an island team."
            ));
            return;
        }
        
        // Activate forcefield
        plugin.getForcefieldManager().activateForcefield(team, durationHours);
        
        // Remove item from inventory
        if (event.getItem() != null && event.getItem().getAmount() > 1) {
            event.getItem().setAmount(event.getItem().getAmount() - 1);
        } else {
            event.getPlayer().getInventory().removeItem(item);
        }
        
        // Send feedback
        String message = plugin.getConfigManager().getString(
            "messages.success.forcefield_activated",
            "§aForcefield activated for §e{duration} hours§a!"
        ).replace("{duration}", String.valueOf(durationHours));
        
        event.getPlayer().sendMessage(message);
        
        plugin.logDebug("forcefield", "Forcefield activated by " + event.getPlayer().getName());
    }
}

