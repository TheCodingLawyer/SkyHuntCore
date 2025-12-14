package org.skyhunt.core.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.skyhunt.core.service.HeadService;
import org.skyhunt.core.database.DatabaseManager;

/**
 * Handles selling heads via right-click interactions.
 */
public class HeadSellListener implements Listener {

    private final HeadService heads;
    private final DatabaseManager database;

    public HeadSellListener(HeadService heads, DatabaseManager database) {
        this.heads = heads;
        this.database = database;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (!heads.isHead(item)) return;

        String mob = heads.getMobType(item);
        if (!database.isHeadUnlocked(event.getPlayer().getUniqueId(), mob)) {
            String msg = event.getPlayer().getServer().getPluginManager().getPlugin("SkyHuntCore")
                .getConfig().getString("messages.head-locked", "&cYou haven't unlocked {mob} heads yet!")
                .replace("{mob}", mob);
            event.getPlayer().sendMessage(msg.replace("&", "§"));
            return;
        }

        if (event.getPlayer().isSneaking()) {
            heads.sellAll(event.getPlayer(), mob);
        } else {
            heads.sellOne(event.getPlayer(), item);
        }
    }
}




