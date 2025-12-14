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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Paginated player selection GUI for inviting players
 */
public class PlayerSelectorGUI {
    
    private final SkyRaidPlugin plugin;
    private final Player viewer;
    private final TeamData team;
    private final List<Player> availablePlayers;
    private final int page;
    private final Inventory inventory;
    
    private static final int PLAYERS_PER_PAGE = 28; // 4 rows of 7
    
    public PlayerSelectorGUI(SkyRaidPlugin plugin, Player viewer, TeamData team, int page) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.team = team;
        this.page = page;
        
        // Get all online players who are NOT on this team
        this.availablePlayers = Bukkit.getOnlinePlayers().stream()
            .filter(p -> !team.getMembers().contains(p.getUniqueId()))
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
        
        // Fill empty slots with glass panes
        fillEmptySlots();
    }
    
    private ItemStack createPlayerHead(Player target) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        
        meta.setOwningPlayer(target);
        meta.setDisplayName("§e" + target.getName());
        
        // Check if player is in a team
        TeamData targetTeam = plugin.getTeamManager().getPlayerTeam(target);
        
        List<String> lore = new ArrayList<>();
        lore.add("§7");
        
        if (targetTeam != null) {
            lore.add("§7Current Team: §f" + targetTeam.getTeamName());
            lore.add("§7");
            lore.add("§c⚠ Player is already in a team");
            lore.add("§7They must leave their current team first");
        } else {
            lore.add("§aAvailable to invite!");
            lore.add("§7");
            lore.add("§e§l► CLICK to invite");
            lore.add("§7");
            lore.add("§7They will receive an invite GUI");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createPreviousPageButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e§l◄ Previous Page");
        meta.setLore(Arrays.asList(
            "§7",
            "§7Page " + page + " of " + ((availablePlayers.size() / PLAYERS_PER_PAGE) + 1)
        ));
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createNextPageButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e§l► Next Page");
        meta.setLore(Arrays.asList(
            "§7",
            "§7Page " + (page + 2) + " of " + ((availablePlayers.size() / PLAYERS_PER_PAGE) + 1)
        ));
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c§l← Back");
        meta.setLore(Arrays.asList(
            "§7",
            "§7Return to island management"
        ));
        item.setItemMeta(meta);
        return item;
    }
    
    private void fillEmptySlots() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        
        for (int i = 0; i < 54; i++) {
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
    
    public int getPage() {
        return page;
    }
    
    public List<Player> getAvailablePlayers() {
        return availablePlayers;
    }
}



