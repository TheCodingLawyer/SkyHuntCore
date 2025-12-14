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
 * Beautiful accept/deny GUI for team invites
 * Green glass pane for accept, red for deny
 */
public class InviteAcceptGUI {
    
    private final SkyRaidPlugin plugin;
    private final Player invitee;
    private final Player inviter;
    private final String teamName;
    private final Inventory inventory;
    
    public InviteAcceptGUI(SkyRaidPlugin plugin, Player invitee, Player inviter, String teamName) {
        this.plugin = plugin;
        this.invitee = invitee;
        this.inviter = inviter;
        this.teamName = teamName;
        this.inventory = Bukkit.createInventory(null, 27, "§7§lTeam Invite: §e" + inviter.getName());
        
        setupGUI();
    }
    
    /**
     * Setup the beautiful accept/deny GUI
     */
    private void setupGUI() {
        // Fill left side with green glass (accept)
        ItemStack acceptGlass = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta acceptMeta = acceptGlass.getItemMeta();
        if (acceptMeta != null) {
            acceptMeta.setDisplayName("§a§lACCEPT");
            acceptGlass.setItemMeta(acceptMeta);
        }
        
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, acceptGlass);
            inventory.setItem(i + 9, acceptGlass);
            inventory.setItem(i + 18, acceptGlass);
        }
        
        // Fill right side with red glass (deny)
        ItemStack denyGlass = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta denyMeta = denyGlass.getItemMeta();
        if (denyMeta != null) {
            denyMeta.setDisplayName("§c§lDENY");
            denyGlass.setItemMeta(denyMeta);
        }
        
        for (int i = 5; i < 9; i++) {
            inventory.setItem(i, denyGlass);
            inventory.setItem(i + 9, denyGlass);
            inventory.setItem(i + 18, denyGlass);
        }
        
        // Accept button - slot 10
        ItemStack acceptBtn = new ItemStack(Material.EMERALD);
        ItemMeta acceptBtnMeta = acceptBtn.getItemMeta();
        if (acceptBtnMeta != null) {
            acceptBtnMeta.setDisplayName("§a§l[ACCEPT INVITE]");
            acceptBtnMeta.setLore(Arrays.asList(
                "§7",
                "§7Join §b" + inviter.getName() + "§7's team",
                "§7Team: §e" + teamName,
                "§7",
                "§aClick to accept!"
            ));
            acceptBtn.setItemMeta(acceptBtnMeta);
        }
        inventory.setItem(10, acceptBtn);
        
        // Deny button - slot 16
        ItemStack denyBtn = new ItemStack(Material.REDSTONE);
        ItemMeta denyBtnMeta = denyBtn.getItemMeta();
        if (denyBtnMeta != null) {
            denyBtnMeta.setDisplayName("§c§l[DENY INVITE]");
            denyBtnMeta.setLore(Arrays.asList(
                "§7",
                "§7Decline the invitation",
                "§7",
                "§cClick to deny"
            ));
            denyBtn.setItemMeta(denyBtnMeta);
        }
        inventory.setItem(16, denyBtn);
        
        // Info in center - slot 13
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§e§lTeam Invite");
            infoMeta.setLore(Arrays.asList(
                "§7",
                "§b" + inviter.getName() + " §7invited you to join:",
                "§e" + teamName,
                "§7",
                "§a[LEFT] Accept  §c[RIGHT] Deny"
            ));
            info.setItemMeta(infoMeta);
        }
        inventory.setItem(13, info);
    }
    
    /**
     * Open the GUI
     */
    public void open() {
        invitee.openInventory(inventory);
        invitee.playSound(invitee.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.0f);
    }
    
    public Inventory getInventory() {
        return inventory;
    }
    
    public Player getInvitee() {
        return invitee;
    }
    
    public Player getInviter() {
        return inviter;
    }
    
    public String getTeamName() {
        return teamName;
    }
}

