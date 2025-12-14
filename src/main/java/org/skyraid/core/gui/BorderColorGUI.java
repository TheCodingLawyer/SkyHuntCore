package org.skyraid.core.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.skyraid.SkyRaidPlugin;
import org.skyraid.core.data.TeamData;
import org.skyraid.core.util.BorderColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Border color selection GUI
 * Allows players to change their island border color
 */
public class BorderColorGUI {
    
    private final SkyRaidPlugin plugin;
    private final Player player;
    private final TeamData team;
    private final Inventory inventory;
    
    public BorderColorGUI(SkyRaidPlugin plugin, Player player, TeamData team) {
        this.plugin = plugin;
        this.player = player;
        this.team = team;
        this.inventory = Bukkit.createInventory(null, 27, "§7§lIsland Border: §bSelect Color");
        
        buildGUI();
    }
    
    private void buildGUI() {
        // Color options
        inventory.setItem(10, createColorItem(BorderColor.BLUE, Material.BLUE_STAINED_GLASS));
        inventory.setItem(11, createColorItem(BorderColor.RED, Material.RED_STAINED_GLASS));
        inventory.setItem(12, createColorItem(BorderColor.GREEN, Material.GREEN_STAINED_GLASS));
        inventory.setItem(13, createColorItem(BorderColor.PURPLE, Material.PURPLE_STAINED_GLASS));
        inventory.setItem(14, createColorItem(BorderColor.YELLOW, Material.YELLOW_STAINED_GLASS));
        inventory.setItem(15, createColorItem(BorderColor.OFF, Material.BARRIER));
        
        // Back button
        inventory.setItem(22, createBackItem());
        
        // Fill empty slots
        fillEmptySlots();
    }
    
    private ItemStack createColorItem(BorderColor color, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        String current = team.getBorderColor().equals(color.name()) ? " §a✓" : "";
        meta.setDisplayName("§f" + color.getDisplayName() + current);
        
        List<String> lore = new ArrayList<>();
        lore.add("§7");
        
        if (team.getBorderColor().equals(color.name())) {
            lore.add("§aCurrent border color");
        } else {
            lore.add("§eClick to select");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createBackItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§a§lBack");
        meta.setLore(List.of("§7Return to island menu"));
        item.setItemMeta(meta);
        return item;
    }
    
    private void fillEmptySlots() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);
        
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }
    
    public void open() {
        player.openInventory(inventory);
    }
    
    public Inventory getInventory() {
        return inventory;
    }
}

