package org.skyraid.core.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.skyraid.SkyRaidPlugin;
import org.skyraid.core.data.TeamData;

import java.util.Arrays;

/**
 * Confirmation GUI for ending forcefield early
 */
public class ForcefieldConfirmGUI {
    
    private final SkyRaidPlugin plugin;
    private final Player player;
    private final TeamData team;
    private final Inventory inventory;
    
    public ForcefieldConfirmGUI(SkyRaidPlugin plugin, Player player, TeamData team) {
        this.plugin = plugin;
        this.player = player;
        this.team = team;
        this.inventory = Bukkit.createInventory(null, 27, "§7§lEnd Forcefield: §cConfirm?");
        
        buildGUI();
    }
    
    private void buildGUI() {
        // Fill with glass panes
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }
        
        // Warning info
        inventory.setItem(13, createWarningItem());
        
        // Confirm button (green)
        inventory.setItem(11, createConfirmItem());
        
        // Cancel button (red)
        inventory.setItem(15, createCancelItem());
    }
    
    private ItemStack createWarningItem() {
        ItemStack item = new ItemStack(Material.FIREWORK_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§d§lEnd Forcefield?");
        
        long hours = team.getRemainingForcefieldHours();
        meta.setLore(Arrays.asList(
            "§7",
            "§cWarning: §7This will end your",
            "§7active forcefield immediately!",
            "§7",
            "§eTime Remaining: §f" + hours + " hours",
            "§7",
            "§cThis cannot be undone!"
        ));
        
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createConfirmItem() {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§a§l✔ CONFIRM");
        meta.setLore(Arrays.asList(
            "§7",
            "§7Click to end forcefield"
        ));
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createCancelItem() {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c§l✘ CANCEL");
        meta.setLore(Arrays.asList(
            "§7",
            "§7Click to go back"
        ));
        item.setItemMeta(meta);
        return item;
    }
    
    public void open() {
        player.openInventory(inventory);
    }
    
    public Inventory getInventory() {
        return inventory;
    }
}



