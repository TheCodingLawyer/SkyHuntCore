package org.skyhunt.core.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.skyhunt.SkyHuntCorePlugin;

/**
 * Routes inventory clicks to the active GUI handlers.
 */
public class GUIListener implements Listener {

    private final SkyHuntCorePlugin plugin;

    public GUIListener(SkyHuntCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        if (event.getView().getTitle() == null) return;
        String title = event.getView().getTitle();
        if (title.equals(GuiUtil.color("&9&lSkyHunt Island"))) {
            event.setCancelled(true);
            plugin.getGuiManager().getIslandMainGUI().handleClick((org.bukkit.entity.Player) event.getWhoClicked(), event);
        } else if (title.equals(GuiUtil.color("&b&lMissions"))) {
            event.setCancelled(true);
            plugin.getGuiManager().getMissionsGUI().handleClick((org.bukkit.entity.Player) event.getWhoClicked(), event);
        } else if (title.equals(GuiUtil.color("&d&lHeads"))) {
            event.setCancelled(true);
            plugin.getGuiManager().getHeadsGUI().handleClick((org.bukkit.entity.Player) event.getWhoClicked(), event);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        // No state to clean yet
    }
}

