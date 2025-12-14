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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Game Room GUI - Pre-island team management
 * Shows team members, allows invites, and has ready button
 */
public class GameRoomGUI {
    
    private final SkyRaidPlugin plugin;
    private final Player owner;
    private final Inventory inventory;
    private PendingTeam pendingTeam;
    
    public GameRoomGUI(SkyRaidPlugin plugin, Player owner) {
        this.plugin = plugin;
        this.owner = owner;
        this.inventory = Bukkit.createInventory(null, 54, "§7§lTeam Setup: §e" + owner.getName() + "'s Team");
        
        // Get or create pending team
        this.pendingTeam = plugin.getTeamManager().getOrCreatePendingTeam(owner);
        
        setupGUI();
    }
    
    /**
     * Setup the game room GUI
     */
    private void setupGUI() {
        // Clear inventory
        inventory.clear();
        
        // Border
        ItemStack border = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.setDisplayName("§r");
            border.setItemMeta(borderMeta);
        }
        
        // Top and bottom borders
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(45 + i, border);
        }
        
        // Team info - slot 4
        ItemStack teamInfo = new ItemStack(Material.YELLOW_BANNER);
        ItemMeta infoMeta = teamInfo.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§6§lTeam Information");
            infoMeta.setLore(Arrays.asList(
                "§7",
                "§eOwner: §f" + owner.getName(),
                "§eMembers: §f" + pendingTeam.getMembers().size() + "/8",
                "§7",
                "§aInvite friends to join your team!",
                "§aWhen ready, click the §2[START]§a button"
            ));
            teamInfo.setItemMeta(infoMeta);
        }
        inventory.setItem(4, teamInfo);
        
        // Display current team members (slots 10-16, 19-25, 28-34)
        displayTeamMembers();
        
        // Invite button - slot 48
        ItemStack inviteBtn = new ItemStack(Material.LIME_DYE);
        ItemMeta inviteMeta = inviteBtn.getItemMeta();
        if (inviteMeta != null) {
            inviteMeta.setDisplayName("§a§l[INVITE PLAYER]");
            inviteMeta.setLore(Arrays.asList(
                "§7",
                "§7Click to view online players",
                "§7and send invites",
                "§7",
                "§eOr use: §f/is invite <player>"
            ));
            inviteBtn.setItemMeta(inviteMeta);
        }
        inventory.setItem(48, inviteBtn);
        
        // Ready/Start button - slot 49
        boolean canStart = pendingTeam.getMembers().size() > 0;
        ItemStack startBtn = new ItemStack(canStart ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK);
        ItemMeta startMeta = startBtn.getItemMeta();
        if (startMeta != null) {
            startMeta.setDisplayName(canStart ? "§2§l[START ADVENTURE]" : "§c§l[NOT READY]");
            if (canStart) {
                startMeta.setLore(Arrays.asList(
                    "§7",
                    "§aClick to create your island",
                    "§aand begin your adventure!",
                    "§7",
                    "§eTeam: §f" + pendingTeam.getMembers().size() + " member(s)"
                ));
            } else {
                startMeta.setLore(Arrays.asList(
                    "§7",
                    "§cYou need at least 1 member",
                    "§c(yourself) to start",
                    "§7",
                    "§eInvite friends or click start!"
                ));
            }
            startBtn.setItemMeta(startMeta);
        }
        inventory.setItem(49, startBtn);
        
        // Back button - slot 50
        ItemStack backBtn = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backBtn.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§c§l[CANCEL]");
            backMeta.setLore(Arrays.asList(
                "§7",
                "§7Cancel and go back",
                "§7to mode selection"
            ));
            backBtn.setItemMeta(backMeta);
        }
        inventory.setItem(50, backBtn);
    }
    
    /**
     * Display team members as player heads
     */
    private void displayTeamMembers() {
        List<Player> members = pendingTeam.getMembers();
        int[] memberSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        
        for (int i = 0; i < memberSlots.length && i < members.size(); i++) {
            Player member = members.get(i);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
            if (skullMeta != null) {
                skullMeta.setOwningPlayer(member);
                skullMeta.setDisplayName("§b§l" + member.getName());
                List<String> lore = new ArrayList<>();
                lore.add("§7");
                if (member.getUniqueId().equals(owner.getUniqueId())) {
                    lore.add("§e[OWNER]");
                } else {
                    lore.add("§a[MEMBER]");
                }
                lore.add("§7");
                lore.add("§7Click to remove");
                skullMeta.setLore(lore);
                head.setItemMeta(skullMeta);
            }
            inventory.setItem(memberSlots[i], head);
        }
    }
    
    /**
     * Open the GUI
     */
    public void open() {
        owner.openInventory(inventory);
        owner.playSound(owner.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.0f);
    }
    
    /**
     * Refresh the GUI
     */
    public void refresh() {
        setupGUI();
    }
    
    public Inventory getInventory() {
        return inventory;
    }
    
    public Player getOwner() {
        return owner;
    }
    
    public PendingTeam getPendingTeam() {
        return pendingTeam;
    }
}

