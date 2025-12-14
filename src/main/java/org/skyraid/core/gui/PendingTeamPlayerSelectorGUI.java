package org.skyraid.core.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.skyraid.SkyRaidPlugin;
import org.skyraid.core.data.PendingTeam;
import org.skyraid.core.data.PlayerData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Paginated player selection GUI for inviting players to pending teams (pre-island)
 */
public class PendingTeamPlayerSelectorGUI {
    
    private final SkyRaidPlugin plugin;
    private final Player viewer;
    private final PendingTeam pendingTeam;
    private final List<Player> availablePlayers;
    private final int page;
    private final Inventory inventory;
    
    private static final int PLAYERS_PER_PAGE = 28; // 4 rows of 7
    
    public PendingTeamPlayerSelectorGUI(SkyRaidPlugin plugin, Player viewer, PendingTeam pendingTeam, int page) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.pendingTeam = pendingTeam;
        this.page = page;
        
        // Get all online players who are NOT in this pending team
        this.availablePlayers = Bukkit.getOnlinePlayers().stream()
            .filter(p -> !pendingTeam.getMembers().contains(p))
            .filter(p -> !p.equals(viewer))
            .collect(Collectors.toList());
        
        this.inventory = Bukkit.createInventory(null, 54, "§7§lInvite Player: §ePage " + (page + 1));
        
        buildGUI();
    }
    
    private void buildGUI() {
        // Calculate pagination
        int startIndex = page * PLAYERS_PER_PAGE;
        int endIndex = Math.min(startIndex + PLAYERS_PER_PAGE, availablePlayers.size());
        
        // Add player heads (slots 10-16, 19-25, 28-34, 37-43)
        int[] slots = new int[28];
        int slotIndex = 0;
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                slots[slotIndex++] = (row * 9) + col;
            }
        }
        
        for (int i = startIndex; i < endIndex; i++) {
            Player target = availablePlayers.get(i);
            int slot = slots[i - startIndex];
            inventory.setItem(slot, createPlayerHead(target));
        }
        
        // Navigation buttons
        if (page > 0) {
            inventory.setItem(45, createPreviousPageButton());
        }
        
        if (endIndex < availablePlayers.size()) {
            inventory.setItem(53, createNextPageButton());
        }
        
        // Back button
        inventory.setItem(49, createBackButton());
        
        // Border
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.setDisplayName("§r");
            border.setItemMeta(borderMeta);
        }
        
        for (int i = 0; i < 9; i++) {
            if (inventory.getItem(i) == null) inventory.setItem(i, border);
            if (inventory.getItem(45 + i) == null) inventory.setItem(45 + i, border);
        }
    }
    
    private ItemStack createPlayerHead(Player target) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(target);
            
            // Check if player already has a pending invite or is in a team
            PlayerData targetData = plugin.getTeamManager().getPlayerData(target.getUniqueId());
            boolean hasTeam = (targetData != null && targetData.getCurrentTeamId() != null);
            boolean hasPendingInvite = (targetData != null && targetData.getPendingInvite() != null);
            
            if (hasTeam) {
                meta.setDisplayName("§7§l" + target.getName());
                meta.setLore(Arrays.asList(
                    "§7",
                    "§c✘ Already in a team",
                    "§7",
                    "§cCannot invite"
                ));
            } else if (hasPendingInvite) {
                meta.setDisplayName("§e§l" + target.getName());
                meta.setLore(Arrays.asList(
                    "§7",
                    "§e⏳ Pending invite",
                    "§7",
                    "§eWaiting for response..."
                ));
            } else {
                meta.setDisplayName("§a§l" + target.getName());
                meta.setLore(Arrays.asList(
                    "§7",
                    "§a✔ Available",
                    "§7",
                    "§eClick to invite!"
                ));
            }
            
            head.setItemMeta(meta);
        }
        return head;
    }
    
    private ItemStack createPreviousPageButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e§l← Previous Page");
            meta.setLore(Arrays.asList("§7Go to page " + page));
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createNextPageButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e§lNext Page →");
            meta.setLore(Arrays.asList("§7Go to page " + (page + 2)));
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c§lBack");
            meta.setLore(Arrays.asList("§7Return to team setup"));
            item.setItemMeta(meta);
        }
        return item;
    }
    
    public void open() {
        viewer.openInventory(inventory);
        viewer.playSound(viewer.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }
    
    public Inventory getInventory() {
        return inventory;
    }
    
    public Player getViewer() {
        return viewer;
    }
    
    public PendingTeam getPendingTeam() {
        return pendingTeam;
    }
    
    public int getPage() {
        return page;
    }
    
    public List<Player> getAvailablePlayers() {
        return availablePlayers;
    }
}

