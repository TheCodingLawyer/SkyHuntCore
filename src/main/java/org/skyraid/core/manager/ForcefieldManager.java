package org.skyraid.core.manager;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.Component;
import org.skyraid.SkyRaidPlugin;
import org.skyraid.core.data.TeamData;

import java.util.*;

/**
 * Manages forcefield mechanics and items
 */
public class ForcefieldManager {
    
    private final SkyRaidPlugin plugin;
    private final Map<Integer, Long> durationMap = new HashMap<>();
    
    public ForcefieldManager(SkyRaidPlugin plugin) {
        this.plugin = plugin;
        loadDurations();
    }
    
    /**
     * Loads forcefield durations from config
     */
    private void loadDurations() {
        durationMap.clear();
        if (plugin.getConfigManager().getConfig().contains("forcefield.durations")) {
            var durationSection = plugin.getConfigManager().getConfig().getConfigurationSection("forcefield.durations");
            if (durationSection != null) {
                for (String key : durationSection.getKeys(false)) {
                    long hours = durationSection.getLong(key);
                    durationMap.put((int) hours, hours * 60 * 60 * 1000);  // Convert to milliseconds
                }
            }
        }
    }
    
    /**
     * Creates an enchanted forcefield firework star with specified duration
     */
    public ItemStack createForcefieldItem(long durationHours) {
        ItemStack item = new ItemStack(Material.FIREWORK_STAR);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Set display name
            meta.displayName(Component.text("§d§l✦ FORCEFIELD ✦ §r§7(" + durationHours + "h)", NamedTextColor.LIGHT_PURPLE));
            
            // Add lore
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(""));
            lore.add(Component.text("§7Right-click to activate a forcefield"));
            lore.add(Component.text("§7around your island."));
            lore.add(Component.text(""));
            lore.add(Component.text("§eDuration: §f" + durationHours + " hours"));
            lore.add(Component.text(""));
            lore.add(Component.text("§6Protection:"));
            lore.add(Component.text("§7• Prevents enemy entry"));
            lore.add(Component.text("§7• Blocks PvP from outsiders"));
            lore.add(Component.text("§7• Prevents block breaking"));
            lore.add(Component.text(""));
            lore.add(Component.text("§cRestriction:"));
            lore.add(Component.text("§7• You cannot raid others"));
            lore.add(Component.text("§7  while forcefield is active"));
            lore.add(Component.text(""));
            lore.add(Component.text("§d§lRIGHT-CLICK TO ACTIVATE"));
            meta.lore(lore);
            
            // Add enchantment glow
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            
            // Store duration in NBT data
            meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "forcefield_hours"),
                PersistentDataType.LONG,
                durationHours
            );
            
            // Mark as forcefield item
            meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "forcefield_item"),
                PersistentDataType.STRING,
                "true"
            );
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Checks if an item is a forcefield item
     */
    public boolean isForcefieldItem(ItemStack item) {
        if (item == null || item.getType() != Material.FIREWORK_STAR) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        String marker = meta.getPersistentDataContainer().get(
            new org.bukkit.NamespacedKey(plugin, "forcefield_item"),
            PersistentDataType.STRING
        );
        
        return "true".equals(marker);
    }
    
    /**
     * Gets the duration of a forcefield item
     */
    public long getForcefieldItemDuration(ItemStack item) {
        if (!isForcefieldItem(item)) {
            return 0;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }
        
        Long hours = meta.getPersistentDataContainer().get(
            new org.bukkit.NamespacedKey(plugin, "forcefield_hours"),
            PersistentDataType.LONG
        );
        
        return hours != null ? hours : 0;
    }
    
    /**
     * Gives a player a forcefield star
     * Used by admin command and optional island creation
     */
    public void giveForcefieldStar(Player player, long durationHours) {
        // Give the forcefield star (no dupe check - players can have multiple)
        ItemStack star = createForcefieldItem(durationHours);
        player.getInventory().addItem(star);
        player.sendMessage("§d§l✦ §r§aYou received a forcefield star! §7(" + durationHours + "h)");
        player.sendMessage("§7Right-click to activate protection around your island.");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
    }
    
    /**
     * Activates a forcefield for a team
     */
    public boolean activateForcefield(TeamData team, long durationHours) {
        if (durationHours <= 0) {
            return false;
        }
        
        long endTime = System.currentTimeMillis() + (durationHours * 60 * 60 * 1000);
        team.setForcefieldEndTime(endTime);
        plugin.getDatabaseManager().saveTeam(team);
        
        plugin.logDebug("forcefield", String.format(
            "Forcefield activated for team %s: %d hours", team.getTeamName(), durationHours
        ));
        
        return true;
    }
    
    /**
     * Deactivates a forcefield for a team
     */
    public void deactivateForcefield(TeamData team) {
        team.setForcefieldEndTime(0);
        plugin.getDatabaseManager().saveTeam(team);
        
        plugin.logDebug("forcefield", "Forcefield deactivated for team: " + team.getTeamName());
    }
    
    /**
     * Checks if a player with a forcefield can enter a location
     */
    public boolean canEnterLocation(Player player, org.bukkit.Location location) {
        TeamData playerTeam = plugin.getTeamManager().getPlayerTeam(player);
        
        // If player has no team, they can enter anywhere
        if (playerTeam == null) {
            return true;
        }
        
        // If player's team has forcefield, they cannot enter other islands
        if (playerTeam.hasForcefield()) {
            TeamData targetTeam = plugin.getIslandManager().findTeamAtLocation(location);
            
            // Cannot enter other teams' islands
            if (targetTeam != null && !targetTeam.getTeamId().equals(playerTeam.getTeamId())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Checks if a player can break blocks in a location
     */
    public boolean canBreakBlocks(Player player, org.bukkit.Location location) {
        TeamData playerTeam = plugin.getTeamManager().getPlayerTeam(player);
        TeamData targetTeam = plugin.getIslandManager().findTeamAtLocation(location);
        
        // Can always break own team's blocks
        if (playerTeam != null && targetTeam != null && playerTeam.getTeamId().equals(targetTeam.getTeamId())) {
            return true;
        }
        
        // If TARGET island has forcefield, cannot break their blocks
        if (targetTeam != null && targetTeam.hasForcefield()) {
            player.sendMessage("§c§l⚠ This island is protected by a forcefield!");
            return false;
        }
        
        // If player has forcefield, cannot break other teams' blocks (mutual restriction)
        if (playerTeam != null && playerTeam.hasForcefield() && targetTeam != null) {
            player.sendMessage("§c§l⚠ You cannot raid while your forcefield is active!");
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks if a player can PvP in a location
     */
    public boolean canPvP(Player attacker, org.bukkit.Location location) {
        TeamData attackerTeam = plugin.getTeamManager().getPlayerTeam(attacker);
        TeamData targetTeam = plugin.getIslandManager().findTeamAtLocation(location);
        
        // Can always PvP on own team's island
        if (attackerTeam != null && targetTeam != null && attackerTeam.getTeamId().equals(targetTeam.getTeamId())) {
            return true;
        }
        
        // If TARGET island has forcefield, cannot PvP there
        if (targetTeam != null && targetTeam.hasForcefield()) {
            attacker.sendMessage("§c§l⚠ This island is protected by a forcefield!");
            return false;
        }
        
        // If attacker has forcefield, cannot PvP on other islands (mutual restriction)
        if (attackerTeam != null && attackerTeam.hasForcefield() && targetTeam != null) {
            attacker.sendMessage("§c§l⚠ You cannot raid while your forcefield is active!");
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks if a player can open containers in a location
     */
    public boolean canOpenContainers(Player player, org.bukkit.Location location) {
        TeamData playerTeam = plugin.getTeamManager().getPlayerTeam(player);
        TeamData targetTeam = plugin.getIslandManager().findTeamAtLocation(location);
        
        // Can always open own team's containers
        if (playerTeam != null && targetTeam != null && playerTeam.getTeamId().equals(targetTeam.getTeamId())) {
            return true;
        }
        
        // If TARGET island has forcefield, cannot open their containers
        if (targetTeam != null && targetTeam.hasForcefield()) {
            player.sendMessage("§c§l⚠ This island is protected by a forcefield!");
            return false;
        }
        
        // If player has forcefield, cannot open other teams' containers (mutual restriction)
        if (playerTeam != null && playerTeam.hasForcefield() && targetTeam != null) {
            player.sendMessage("§c§l⚠ You cannot raid while your forcefield is active!");
            return false;
        }
        
        return true;
    }
    
    /**
     * Gives a forcefield item to a player
     */
    public void giveForcefieldItem(Player player, long durationHours) {
        ItemStack item = createForcefieldItem(durationHours);
        player.getInventory().addItem(item);
        plugin.logDebug("forcefield", "Forcefield item given to " + player.getName());
    }
}
