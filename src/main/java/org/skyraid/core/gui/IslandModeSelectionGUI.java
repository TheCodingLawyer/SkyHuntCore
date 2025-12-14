package org.skyraid.core.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.skyraid.SkyRaidPlugin;

import java.util.Arrays;

/**
 * Beautiful GUI for selecting island mode: Singleplayer or Teams
 * Follows MC_PROJECT_RULES.md - no Unicode emojis, clean design
 */
public class IslandModeSelectionGUI {
    
    private final SkyRaidPlugin plugin;
    private final Inventory inventory;
    private final Player player;
    
    public IslandModeSelectionGUI(SkyRaidPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(null, 27, "§7§lIsland Setup: §bSelect Mode");
        
        setupGUI();
    }
    
    /**
     * Set up the GUI with beautiful design
     */
    private void setupGUI() {
        // Border - decorative gray glass panes
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.setDisplayName("§r");
            border.setItemMeta(borderMeta);
        }
        
        // Fill border slots (top and bottom rows, sides)
        int[] borderSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26};
        for (int slot : borderSlots) {
            inventory.setItem(slot, border);
        }
        
        // Singleplayer button - slot 11
        ItemStack singleplayer = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta singleMeta = singleplayer.getItemMeta();
        if (singleMeta != null) {
            singleMeta.setDisplayName("§a§lSingleplayer Mode");
            singleMeta.setLore(Arrays.asList(
                "§7",
                "§7Create your own private island",
                "§7Play solo and build at your pace",
                "§7",
                "§e[CLICK] to select"
            ));
            singleplayer.setItemMeta(singleMeta);
        }
        inventory.setItem(11, singleplayer);
        
        // Teams button - slot 15
        ItemStack teams = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta teamsMeta = teams.getItemMeta();
        if (teamsMeta != null) {
            teamsMeta.setDisplayName("§b§lTeams Mode");
            teamsMeta.setLore(Arrays.asList(
                "§7",
                "§7Create a team island",
                "§7Invite friends and play together",
                "§7Share resources and defend together",
                "§7",
                "§e[CLICK] to select"
            ));
            teams.setItemMeta(teamsMeta);
        }
        inventory.setItem(15, teams);
        
        // Info item - slot 13
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§e§lChoose Your Adventure");
            infoMeta.setLore(Arrays.asList(
                "§7",
                "§7Select how you want to play:",
                "§a * Singleplayer: Solo island",
                "§b * Teams: Play with friends",
                "§7",
                "§cYou can always change later!"
            ));
            info.setItemMeta(infoMeta);
        }
        inventory.setItem(13, info);
    }
    
    /**
     * Open the GUI for the player
     */
    public void open() {
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
    }
    
    /**
     * Get the inventory
     */
    public Inventory getInventory() {
        return inventory;
    }
    
    /**
     * Handle click on singleplayer mode
     */
    public void handleSingleplayerClick() {
        player.closeInventory();
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        player.sendMessage("§a§lSingleplayer Mode Selected!");
        player.sendMessage("§eCreating your island... Please wait.");
        
        // Create singleplayer island
        String teamName = player.getName() + "'s Island";
        plugin.getIslandManager().createSingleplayerIsland(player, teamName);
    }
    
    /**
     * Handle click on teams mode
     */
    public void handleTeamsClick() {
        player.closeInventory();
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        player.sendMessage("§b§lTeams Mode Selected!");
        
        // Open game room GUI
        GameRoomGUI gameRoom = new GameRoomGUI(plugin, player);
        Bukkit.getScheduler().runTaskLater(plugin, gameRoom::open, 2L);
    }
}

