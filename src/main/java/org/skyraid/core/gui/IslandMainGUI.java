package org.skyraid.core.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.skyraid.SkyRaidPlugin;
import org.skyraid.core.data.PlayerData;
import org.skyraid.core.data.TeamData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Main island management GUI
 * Shows island info, members, and management options
 */
public class IslandMainGUI {
    
    private final SkyRaidPlugin plugin;
    private final Player player;
    private final TeamData team;
    private final Inventory inventory;
    
    public IslandMainGUI(SkyRaidPlugin plugin, Player player, TeamData team) {
        this.plugin = plugin;
        this.player = player;
        this.team = team;
        this.inventory = Bukkit.createInventory(null, 54, "§7§lIsland: §a" + team.getTeamName());
        
        buildGUI();
    }
    
    private void buildGUI() {
        // Row 1: Island Info
        inventory.setItem(4, createIslandInfoItem());
        
        // Row 2: Team Members (slots 10-16)
        List<UUID> members = new ArrayList<>(team.getMembers());
        for (int i = 0; i < Math.min(members.size(), 7); i++) {
            UUID memberUUID = members.get(i);
            inventory.setItem(10 + i, createMemberHeadItem(memberUUID));
        }
        
        // Row 3: Management Options (organized for solo/team)
        if (team.getMemberCount() == 1) {
            // Solo mode - 4 buttons with teleport
            inventory.setItem(19, createTeleportItem());
            inventory.setItem(20, createSethomeItem());
            inventory.setItem(22, createForcefieldItem());
            inventory.setItem(24, createBorderItem());
        } else {
            // Team mode - 5 buttons including invite and teleport
            inventory.setItem(19, createTeleportItem());
            inventory.setItem(20, createSethomeItem());
            inventory.setItem(21, createInviteItem());
            inventory.setItem(22, createForcefieldItem());
            inventory.setItem(23, createBorderItem());
        }
        
        // Row 4: Leave/Disband
        if (team.getLeaderId().equals(player.getUniqueId())) {
            if (team.getMemberCount() == 1) {
                inventory.setItem(31, createDisbandItem());
            } else {
                inventory.setItem(31, createCannotDisbandItem());
            }
        } else {
            inventory.setItem(31, createLeaveItem());
        }
        
        // Row 5: Close button
        inventory.setItem(49, createCloseItem());
        
        // Fill empty slots with glass panes
        fillEmptySlots();
    }
    
    private ItemStack createIslandInfoItem() {
        ItemStack item = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b§lIsland Information");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7");
        lore.add("§eIsland Name: §f" + team.getTeamName());
        lore.add("§eMembers: §f" + team.getMemberCount());
        lore.add("§eBalance: §f$" + team.getBalance());
        lore.add("§eLocation: §f" + team.getIslandX() + ", " + team.getIslandZ());
        lore.add("§7");
        
        if (team.hasForcefield()) {
            long hours = team.getRemainingForcefieldHours();
            lore.add("§aForcefield Active");
            lore.add("§7Time Remaining: §f" + hours + " hours");
        } else {
            lore.add("§cNo Forcefield Active");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createMemberHeadItem(UUID memberUUID) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        
        Player member = Bukkit.getPlayer(memberUUID);
        String memberName = member != null ? member.getName() : "Unknown";
        
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(memberUUID));
        
        boolean isLeader = team.getLeaderId().equals(memberUUID);
        boolean isPromoted = team.isLeader(memberUUID) && !isLeader;
        
        if (isLeader) {
            meta.setDisplayName("§6§l★ " + memberName + " §7(Owner)");
        } else if (isPromoted) {
            meta.setDisplayName("§e§l★ " + memberName + " §7(Leader)");
        } else {
            meta.setDisplayName("§f" + memberName);
        }
        
        List<String> lore = new ArrayList<>();
        lore.add("§7");
        
        if (team.isLeader(player.getUniqueId()) && !memberUUID.equals(player.getUniqueId())) {
            lore.add("§eClick to manage this player");
            lore.add("§7");
            lore.add("§7• Kick");
            if (isPromoted) {
                lore.add("§7• Demote");
            } else {
                lore.add("§7• Promote");
            }
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createTeleportItem() {
        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§d§lTeleport to Island");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7");
        lore.add("§7Teleport to your island's");
        lore.add("§7home location instantly.");
        lore.add("§7");
        lore.add("§e§l► CLICK to teleport");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createSethomeItem() {
        ItemStack item = new ItemStack(Material.RED_BED);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§a§lSet Home");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7");
        lore.add("§7Set the island home location");
        lore.add("§7to your current position.");
        lore.add("§7");
        
        if (team.isLeader(player.getUniqueId())) {
            lore.add("§eClick to set home");
        } else {
            lore.add("§cYou must be a leader!");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createInviteItem() {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§a§lInvite Player");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7");
        lore.add("§7Invite another player to");
        lore.add("§7join your island team.");
        lore.add("§7");
        
        if (team.isLeader(player.getUniqueId())) {
            lore.add("§eClick to select a player!");
            lore.add("§7Or use: §f/is invite <player>");
        } else {
            lore.add("§cYou must be a leader!");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createForcefieldItem() {
        boolean active = team.hasForcefield();
        ItemStack item = new ItemStack(active ? Material.LIME_STAINED_GLASS : Material.RED_STAINED_GLASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§d§lForcefield Status");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7");
        
        if (active) {
            long hours = team.getRemainingForcefieldHours();
            lore.add("§aStatus: §fActive");
            lore.add("§aTime Remaining: §f" + hours + " hours");
            lore.add("§7");
            
            if (team.isLeader(player.getUniqueId())) {
                lore.add("§e§l► CLICK to end forcefield early");
                lore.add("§7(Confirmation required)");
            } else {
                lore.add("§cOnly leaders can end forcefield");
            }
        } else {
            lore.add("§cStatus: §fInactive");
            lore.add("§7");
            lore.add("§7Obtain forcefield items from");
            lore.add("§7crates, events, or shops!");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createBorderItem() {
        ItemStack item = new ItemStack(Material.BEACON);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b§lIsland Border");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7");
        lore.add("§eCurrent Color: §f" + team.getBorderColor());
        lore.add("§7");
        lore.add("§7The border shows the");
        lore.add("§7boundary of your island.");
        lore.add("§7");
        
        if (team.isLeader(player.getUniqueId())) {
            lore.add("§eClick to change color");
        } else {
            lore.add("§cYou must be a leader!");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createLeaveItem() {
        ItemStack item = new ItemStack(Material.IRON_DOOR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c§lLeave Island");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7");
        lore.add("§7Leave this island team.");
        lore.add("§c§lWARNING: This cannot be undone!");
        lore.add("§7");
        lore.add("§eClick to leave");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createDisbandItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§4§lDisband Island");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7");
        lore.add("§7Permanently delete this island.");
        lore.add("§c§lWARNING: This CANNOT be undone!");
        lore.add("§7");
        lore.add("§eClick to disband");
        lore.add("§7You will need to type confirmation");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createCannotDisbandItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§4§lDisband Island");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7");
        lore.add("§cCannot disband with members!");
        lore.add("§7");
        lore.add("§7Kick all members first, or");
        lore.add("§7have them leave voluntarily.");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createCloseItem() {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c§lClose");
        meta.setLore(List.of("§7Click to close this menu"));
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

