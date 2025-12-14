package org.skyraid.core.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.skyraid.SkyRaidPlugin;
import org.skyraid.core.data.TeamData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Player management sub-GUI
 * Allows leaders to kick, promote, or demote team members
 */
public class PlayerManagementGUI {
    
    private final SkyRaidPlugin plugin;
    private final Player viewer;
    private final TeamData team;
    private final UUID targetPlayerUUID;
    private final Inventory inventory;
    
    public PlayerManagementGUI(SkyRaidPlugin plugin, Player viewer, TeamData team, UUID targetPlayerUUID) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.team = team;
        this.targetPlayerUUID = targetPlayerUUID;
        String targetName = Bukkit.getOfflinePlayer(targetPlayerUUID).getName();
        this.inventory = Bukkit.createInventory(null, 27, "§7§lManage: §e" + (targetName != null ? targetName : "Unknown"));
        
        buildGUI();
    }
    
    private void buildGUI() {
        // Player head in center
        inventory.setItem(4, createPlayerHeadItem());
        
        // Management options
        inventory.setItem(11, createKickItem());
        inventory.setItem(13, createPromoteDemoteItem());
        inventory.setItem(15, createBackItem());
        
        // Fill empty slots
        fillEmptySlots();
    }
    
    private ItemStack createPlayerHeadItem() {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        
        Player target = Bukkit.getPlayer(targetPlayerUUID);
        String targetName = target != null ? target.getName() : "Unknown";
        
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(targetPlayerUUID));
        
        boolean isLeader = team.getLeaderId().equals(targetPlayerUUID);
        boolean isPromoted = team.isLeader(targetPlayerUUID) && !isLeader;
        
        if (isLeader) {
            meta.setDisplayName("§6§l★ " + targetName + " §7(Owner)");
        } else if (isPromoted) {
            meta.setDisplayName("§e§l★ " + targetName + " §7(Leader)");
        } else {
            meta.setDisplayName("§f" + targetName);
        }
        
        List<String> lore = new ArrayList<>();
        lore.add("§7");
        lore.add("§7Choose an action below");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createKickItem() {
        ItemStack item = new ItemStack(Material.IRON_DOOR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c§lKick Player");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7");
        lore.add("§7Remove this player from");
        lore.add("§7your island team.");
        lore.add("§7");
        lore.add("§eClick to kick");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createPromoteDemoteItem() {
        boolean isPromoted = team.isLeader(targetPlayerUUID) && !team.getLeaderId().equals(targetPlayerUUID);
        
        ItemStack item = new ItemStack(isPromoted ? Material.ORANGE_DYE : Material.YELLOW_DYE);
        ItemMeta meta = item.getItemMeta();
        
        if (isPromoted) {
            meta.setDisplayName("§6§lDemote Player");
            List<String> lore = new ArrayList<>();
            lore.add("§7");
            lore.add("§7Remove leader permissions");
            lore.add("§7from this player.");
            lore.add("§7");
            lore.add("§eClick to demote");
            meta.setLore(lore);
        } else {
            meta.setDisplayName("§e§lPromote Player");
            List<String> lore = new ArrayList<>();
            lore.add("§7");
            lore.add("§7Grant leader permissions");
            lore.add("§7to this player.");
            lore.add("§7");
            lore.add("§eClick to promote");
            meta.setLore(lore);
        }
        
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
        viewer.openInventory(inventory);
    }
    
    public Inventory getInventory() {
        return inventory;
    }
    
    public UUID getTargetPlayerUUID() {
        return targetPlayerUUID;
    }
}

