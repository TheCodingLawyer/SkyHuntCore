package org.skyraid.core.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.skyraid.SkyRaidPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Beautiful setup wizard GUI for first-time admin configuration
 */
public class SetupWizardGUI implements Listener {
    
    private final SkyRaidPlugin plugin;
    private int currentPage = 0;
    private List<String> detectedWorlds = new ArrayList<>();
    
    public SetupWizardGUI(SkyRaidPlugin plugin) {
        this.plugin = plugin;
        detectWorlds();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Detects all available worlds on the server
     */
    private void detectWorlds() {
        detectedWorlds.clear();
        File worldsFolder = new File(Bukkit.getServer().getWorldContainer().getPath());
        
        if (worldsFolder.exists() && worldsFolder.isDirectory()) {
            File[] folders = worldsFolder.listFiles(File::isDirectory);
            if (folders != null) {
                for (File folder : folders) {
                    if (new File(folder, "level.dat").exists()) {
                        detectedWorlds.add(folder.getName());
                    }
                }
            }
        }
        
        if (detectedWorlds.isEmpty()) {
            detectedWorlds.add("world");  // Default fallback
        }
    }
    
    /**
     * Opens the main setup wizard page
     */
    public void openWizard(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, 
            Component.text("Setup Wizard: ", NamedTextColor.GRAY)
                .decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true)
                .append(Component.text("Configuration", NamedTextColor.AQUA)));
        
        // Title/Info
        addGlassPane(gui, 0, NamedTextColor.DARK_BLUE);
        gui.setItem(1, createButton(Material.COMPASS, "World Configuration",
            Arrays.asList("Select which world will host", "all skyblock islands")));
        addGlassPane(gui, 2, NamedTextColor.DARK_BLUE);
        addGlassPane(gui, 3, NamedTextColor.DARK_BLUE);
        addGlassPane(gui, 4, NamedTextColor.DARK_BLUE);
        gui.setItem(5, createButton(Material.PAPER, "Server Detection",
            Arrays.asList("Configure BungeeCord/Proxy", "server settings")));
        addGlassPane(gui, 6, NamedTextColor.DARK_BLUE);
        addGlassPane(gui, 7, NamedTextColor.DARK_BLUE);
        addGlassPane(gui, 8, NamedTextColor.DARK_BLUE);
        
        // Decorative separator
        for (int i = 9; i < 18; i++) {
            addGlassPane(gui, i, NamedTextColor.DARK_GRAY);
        }
        
        // Instructions
        gui.setItem(10, createButton(Material.BOOK, "Instructions",
            Arrays.asList("Click WORLD CONFIGURATION", "to select your island world")));
        gui.setItem(11, createButton(Material.ARROW, "Next Step →",
            Arrays.asList("Follow the setup steps", "in order")));
        gui.setItem(12, createButton(Material.LIME_CONCRETE, "Next →",
            Arrays.asList("Click to continue", "to world selection")));
        
        for (int i = 13; i < 18; i++) {
            addGlassPane(gui, i, NamedTextColor.DARK_GRAY);
        }
        
        // Bottom border
        for (int i = 18; i < 27; i++) {
            addGlassPane(gui, i, NamedTextColor.DARK_GRAY);
        }
        
        player.openInventory(gui);
        player.sendMessage(Component.text("SkyRaid Setup Wizard Opened", NamedTextColor.GOLD)
            .decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true));
    }
    
    /**
     * Opens world selection page
     */
    public void openWorldSelection(Player player) {
        int size = Math.min(27, ((detectedWorlds.size() / 7) + 1) * 9);
        Inventory gui = Bukkit.createInventory(null, size,
            Component.text("Setup Wizard: ", NamedTextColor.GRAY)
                .decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true)
                .append(Component.text("Select World", NamedTextColor.GREEN)));
        
        // Top border
        for (int i = 0; i < 9; i++) {
            addGlassPane(gui, i, NamedTextColor.DARK_BLUE);
        }
        gui.setItem(4, createButton(Material.COMPASS, "Select Your Warp World",
            Arrays.asList("Where all islands will exist")));
        
        // World buttons (skip border)
        int slot = 10;
        for (String world : detectedWorlds) {
            if (slot >= size - 9) break;  // Leave room for bottom border
            
            Material material = world.contains("void") || world.contains("flat") ? 
                Material.PURPLE_CONCRETE : Material.GRASS_BLOCK;
            
            gui.setItem(slot, createWorldButton(material, world));
            slot++;
            
            if ((slot - 10) % 7 == 0) slot += 2;  // Skip borders
        }
        
        // Bottom border
        for (int i = size - 9; i < size; i++) {
            addGlassPane(gui, i, NamedTextColor.DARK_BLUE);
        }
        gui.setItem(size - 5, createButton(Material.ARROW, "← Back",
            Arrays.asList("Return to main menu")));
        
        player.openInventory(gui);
    }
    
    /**
     * Creates a world selection button
     */
    private ItemStack createWorldButton(Material material, String worldName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(Component.text(worldName, NamedTextColor.YELLOW)
                .decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Click to select this world", NamedTextColor.GRAY));
            lore.add(Component.text("as your SkyRaid warp world", NamedTextColor.GRAY));
            
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Creates a button with display name and lore
     */
    private ItemStack createButton(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.YELLOW)
                .decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true));
            
            List<Component> loreCom = new ArrayList<>();
            for (String line : lore) {
                loreCom.add(Component.text(line, NamedTextColor.GRAY));
            }
            meta.lore(loreCom);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Adds a decorative glass pane to GUI
     */
    private void addGlassPane(Inventory gui, int slot, NamedTextColor color) {
        Material glassMaterial;
        
        if (color == NamedTextColor.DARK_BLUE) {
            glassMaterial = Material.BLUE_STAINED_GLASS_PANE;
        } else if (color == NamedTextColor.DARK_GRAY) {
            glassMaterial = Material.GRAY_STAINED_GLASS_PANE;
        } else if (color == NamedTextColor.GREEN) {
            glassMaterial = Material.LIME_STAINED_GLASS_PANE;
        } else {
            glassMaterial = Material.BLACK_STAINED_GLASS_PANE;
        }
        
        ItemStack pane = new ItemStack(glassMaterial);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            pane.setItemMeta(meta);
        }
        gui.setItem(slot, pane);
    }
    
    /**
     * Handles GUI clicks
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTitle().contains("SkyRaid Setup") || 
              event.getView().getTitle().contains("Select Warp"))) {
            return;
        }
        
        event.setCancelled(true);
        
        if (event.getClickedInventory() == null || 
            event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        
        String displayName = clicked.getItemMeta() != null ? 
            (clicked.getItemMeta().hasDisplayName() ? 
                clicked.getItemMeta().getDisplayName() : "") : "";
        
        // Handle world selection
        if (displayName.contains("Select Warp World")) {
            openWorldSelection(player);
        } else if (displayName.contains("Next")) {
            openWorldSelection(player);
        } else if (displayName.contains("Back")) {
            openWizard(player);
        } else if (clicked.getType() == Material.GRASS_BLOCK || 
                   clicked.getType() == Material.PURPLE_CONCRETE) {
            // World selected
            String selectedWorld = displayName;
            plugin.getConfigManager().getConfig().set("world.warp_world", selectedWorld);
            plugin.getConfigManager().saveConfig();
            
            player.closeInventory();
            player.sendMessage(Component.text("Warp world set to: ", NamedTextColor.GREEN)
                .append(Component.text(selectedWorld, NamedTextColor.YELLOW)));
            player.sendMessage(Component.text("Setup complete! Run /is new to create your first island.",
                NamedTextColor.GREEN));
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        // Cleanup
    }
}
