package org.skyraid.core.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.skyraid.SkyRaidPlugin;
import org.skyraid.core.data.PendingTeam;
import org.skyraid.core.data.TeamData;
import org.skyraid.core.gui.GameRoomGUI;
import org.skyraid.core.gui.InviteAcceptGUI;
import org.skyraid.core.gui.IslandModeSelectionGUI;
import org.skyraid.core.gui.PendingTeamPlayerSelectorGUI;

import java.util.UUID;

/**
 * Handles all GUI interactions for the plugin
 * Follows MC_PROJECT_RULES.md - HIGHEST priority, comprehensive blocking
 */
public class GUIListener implements Listener {
    
    private final SkyRaidPlugin plugin;
    
    public GUIListener(SkyRaidPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Prevent all drag events in GUIs
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        
        // Block drags in all our GUIs
        if (title.contains("Island Setup") ||
            title.contains("Team Setup") ||
            title.contains("Team Invite") ||
            title.contains("Invite Player") ||
            title.contains("Island:") ||
            title.contains("Manage:") ||
            title.contains("Island Border") ||
            title.contains("End Forcefield")) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Handle all GUI clicks
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        Inventory clickedInv = event.getClickedInventory();
        String title = event.getView().getTitle();
        
        // Island Mode Selection GUI
        if (title.contains("Island Setup")) {
            event.setCancelled(true); // Cancel ALL clicks first
            
            if (clickedInv == null) return;
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            // Singleplayer button - slot 11
            if (event.getSlot() == 11 && clicked.getType() == Material.PLAYER_HEAD) {
                IslandModeSelectionGUI gui = new IslandModeSelectionGUI(plugin, player);
                gui.handleSingleplayerClick();
            }
            // Teams button - slot 15
            else if (event.getSlot() == 15 && clicked.getType() == Material.TOTEM_OF_UNDYING) {
                IslandModeSelectionGUI gui = new IslandModeSelectionGUI(plugin, player);
                gui.handleTeamsClick();
            }
            return;
        }
        
        // Game Room GUI (Team Setup)
        if (title.contains("Team Setup")) {
            event.setCancelled(true); // Cancel ALL clicks first
            
            if (clickedInv == null) return;
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            // Invite button - slot 48
            if (event.getSlot() == 48 && clicked.getType() == Material.LIME_DYE) {
                // Get the pending team
                PendingTeam pendingTeam = plugin.getTeamManager().getPendingTeam(player);
                if (pendingTeam != null) {
                    PendingTeamPlayerSelectorGUI selectorGUI = new PendingTeamPlayerSelectorGUI(plugin, player, pendingTeam, 0);
                    selectorGUI.open();
                } else {
                    player.sendMessage("§cError: Could not find your team setup!");
                    player.closeInventory();
                }
            }
            // Start button - slot 49
            else if (event.getSlot() == 49 && (clicked.getType() == Material.EMERALD_BLOCK || clicked.getType() == Material.REDSTONE_BLOCK)) {
                handleGameRoomStart(player);
            }
            // Cancel button - slot 50
            else if (event.getSlot() == 50 && clicked.getType() == Material.BARRIER) {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.0f);
                player.closeInventory();
                // Clean up pending team
                plugin.getTeamManager().removePendingTeam(player);
                player.sendMessage("§cCancelled team creation.");
            }
            // Remove member click (player heads)
            else if (clicked.getType() == Material.PLAYER_HEAD) {
                // TODO: Handle member removal if owner
            }
            return;
        }
        
        // Team Invite GUI
        if (title.contains("Team Invite")) {
            event.setCancelled(true); // Cancel ALL clicks first
            
            if (clickedInv == null) return;
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            // Find the GUI instance (stored in a map or recreate)
            // For now, we'll handle it generically
            
            // Accept button - slot 10
            if (event.getSlot() == 10 && clicked.getType() == Material.EMERALD) {
                handleInviteAccept(player);
            }
            // Deny button - slot 16
            else if (event.getSlot() == 16 && clicked.getType() == Material.REDSTONE) {
                handleInviteDeny(player);
            }
            return;
        }
        
        // Pending Team Player Selector GUI (Invite Player)
        if (title.contains("Invite Player")) {
            event.setCancelled(true);
            
            if (clickedInv == null) return;
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            handlePendingTeamPlayerSelectorClick(player, event.getSlot(), clicked, title);
            return;
        }
        
        // Island Management GUI
        if (title.contains("Island:")) {
            event.setCancelled(true);
            
            if (clickedInv == null) return;
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            handleIslandManagementClick(player, event.getSlot(), clicked);
            return;
        }
        
        // Player Management GUI
        if (title.contains("Manage:")) {
            event.setCancelled(true);
            
            if (clickedInv == null) return;
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            handlePlayerManagementClick(player, event.getSlot(), clicked);
            return;
        }
        
        // Border Color GUI
        if (title.contains("Island Border")) {
            event.setCancelled(true);
            
            if (clickedInv == null) return;
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            handleBorderColorClick(player, event.getSlot(), clicked);
            return;
        }
        
        // Forcefield Confirmation GUI
        if (title.contains("End Forcefield")) {
            event.setCancelled(true);
            
            if (clickedInv == null) return;
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            handleForcefieldConfirmClick(player, event.getSlot(), clicked);
            return;
        }
        
        // Player Selector GUI
        if (title.contains("Invite Player")) {
            event.setCancelled(true);
            
            if (clickedInv == null) return;
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            handlePlayerSelectorClick(player, event.getSlot(), clicked, title);
            return;
        }
    }
    
    /**
     * Handle game room start button
     */
    private void handleGameRoomStart(Player owner) {
        PendingTeam pendingTeam = plugin.getTeamManager().getPendingTeam(owner);
        
        if (pendingTeam == null || pendingTeam.getMemberCount() == 0) {
            owner.playSound(owner.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
            owner.sendMessage("§cError: No pending team found!");
            owner.closeInventory();
            return;
        }
        
        owner.playSound(owner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        owner.closeInventory();
        owner.sendMessage("§a§lStarting adventure!");
        owner.sendMessage("§eCreating your team island... Please wait.");
        
        // Create the team island
        plugin.getIslandManager().createMultiplayerIsland(owner, pendingTeam);
    }
    
    /**
     * Handle invite acceptance
     */
    private void handleInviteAccept(Player invitee) {
        // Get pending invite from player's data
        org.skyraid.core.data.PlayerData playerData = plugin.getTeamManager().getPlayerData(invitee.getUniqueId());
        if (playerData == null || playerData.getPendingInvite() == null) {
            invitee.playSound(invitee.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
            invitee.sendMessage("§cNo pending invite found!");
            invitee.closeInventory();
            return;
        }
        
        UUID inviterUUID = playerData.getPendingInvite();
        Player inviter = Bukkit.getPlayer(inviterUUID);
        
        if (inviter == null || !inviter.isOnline()) {
            invitee.playSound(invitee.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
            invitee.sendMessage("§cInviter is no longer online!");
            invitee.closeInventory();
            playerData.setPendingInvite(null);
            return;
        }
        
        PendingTeam pendingTeam = plugin.getTeamManager().getPendingTeam(inviter);
        if (pendingTeam == null) {
            invitee.playSound(invitee.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
            invitee.sendMessage("§cThat team no longer exists!");
            invitee.closeInventory();
            playerData.setPendingInvite(null);
            return;
        }
        
        // Add to pending team
        pendingTeam.addMember(invitee);
        playerData.setPendingInvite(null);
        
        invitee.playSound(invitee.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        invitee.closeInventory();
        invitee.sendMessage("§a§lYou joined the team!");
        invitee.sendMessage("§eWaiting for team owner to start...");
        
        inviter.sendMessage("§a" + invitee.getName() + " §ejoined your team!");
        inviter.playSound(inviter.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        
        // Refresh game room if inviter has it open
        if (inviter.getOpenInventory().getTitle().contains("Team Setup")) {
            GameRoomGUI gameRoom = new GameRoomGUI(plugin, inviter);
            gameRoom.refresh();
        }
    }
    
    /**
     * Handle invite denial
     */
    private void handleInviteDeny(Player invitee) {
        org.skyraid.core.data.PlayerData playerData = plugin.getTeamManager().getPlayerData(invitee.getUniqueId());
        if (playerData != null) {
            UUID inviterUUID = playerData.getPendingInvite();
            playerData.setPendingInvite(null);
            
            Player inviter = Bukkit.getPlayer(inviterUUID);
            if (inviter != null && inviter.isOnline()) {
                inviter.sendMessage("§c" + invitee.getName() + " §7declined your invite.");
            }
        }
        
        invitee.playSound(invitee.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.0f);
        invitee.closeInventory();
        invitee.sendMessage("§cYou declined the invite.");
    }
    
    /**
     * Handle Island Management GUI clicks
     */
    private void handleIslandManagementClick(Player player, int slot, ItemStack clicked) {
        org.skyraid.core.data.PlayerData playerData = plugin.getTeamManager().getPlayerData(player.getUniqueId());
        if (playerData == null || playerData.getCurrentTeamId() == null) {
            player.closeInventory();
            return;
        }
        
        org.skyraid.core.data.TeamData team = plugin.getTeamManager().getTeam(playerData.getCurrentTeamId());
        if (team == null) {
            player.closeInventory();
            return;
        }
        
        // Teleport to island (slot 19)
        if (slot == 19 && clicked.getType() == Material.ENDER_PEARL) {
            // Check if home is set (default 0,0,0 means not set)
            if (team.getHomeY() == 0) {
                player.sendMessage("§cIsland home not set!");
                player.sendMessage("§7Leaders can use §e/is sethome§7 to set it.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                return;
            }
            
            // Build home location
            String worldName = plugin.getConfigManager().getString("world.warp_world", "skyblock");
            org.bukkit.World skyblockWorld = org.bukkit.Bukkit.getWorld(worldName);
            
            if (skyblockWorld == null) {
                player.sendMessage("§cSkyblock world not found!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                return;
            }
            
            org.bukkit.Location homeLocation = new org.bukkit.Location(
                skyblockWorld,
                team.getHomeX() + 0.5,
                team.getHomeY(),
                team.getHomeZ() + 0.5,
                team.getHomeYaw(),
                team.getHomePitch()
            );
            
            // Teleport player
            player.teleport(homeLocation);
            player.sendMessage("§a§lTeleported to Island!");
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            player.closeInventory();
        }
        // Sethome (slot 20)
        else if (slot == 20 && clicked.getType() == Material.RED_BED) {
            if (!team.isLeader(player.getUniqueId())) {
                player.sendMessage("§cOnly leaders can set home!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                return;
            }
            
            // SECURITY: Verify player is on their own island
            TeamData locationTeam = plugin.getIslandManager().findTeamAtLocation(player.getLocation());
            if (locationTeam == null || !locationTeam.getTeamId().equals(team.getTeamId())) {
                player.sendMessage("§c§l⚠ You can only set home on YOUR island!");
                player.sendMessage("§7You tried to set home on someone else's island.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                player.closeInventory();
                return;
            }
            
            team.setHome(
                player.getLocation().getBlockX(),
                player.getLocation().getBlockY(),
                player.getLocation().getBlockZ(),
                player.getLocation().getYaw(),
                player.getLocation().getPitch()
            );
            plugin.getDatabaseManager().saveTeam(team);
            
            player.sendMessage("§a§lHome Set!");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            player.closeInventory();
        }
        // Invite player (slot 21)
        else if (slot == 21 && clicked.getType() == Material.WRITABLE_BOOK) {
            if (!team.isLeader(player.getUniqueId())) {
                player.sendMessage("§cOnly leaders can invite players!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                return;
            }
            
            // Check if solo mode
            if (team.getMemberCount() == 1) {
                player.sendMessage("§c§l⚠ You cannot invite players in Solo Mode!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                player.closeInventory();
                return;
            }
            
            // Open player selector GUI
            org.skyraid.core.gui.PlayerSelectorGUI selectorGUI = 
                new org.skyraid.core.gui.PlayerSelectorGUI(plugin, player, team, 0);
            selectorGUI.open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }
        // Forcefield (slot 22)
        else if (slot == 22 && (clicked.getType() == Material.LIME_STAINED_GLASS || clicked.getType() == Material.RED_STAINED_GLASS)) {
            if (team.hasForcefield()) {
                // Active - allow ending if leader
                if (!team.isLeader(player.getUniqueId())) {
                    player.sendMessage("§cOnly leaders can end forcefield!");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                    return;
                }
                
                // Open confirmation GUI
                org.skyraid.core.gui.ForcefieldConfirmGUI confirmGUI = 
                    new org.skyraid.core.gui.ForcefieldConfirmGUI(plugin, player, team);
                confirmGUI.open();
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 0.8f);
            } else {
                // Inactive - show message
                player.sendMessage("§cNo active forcefield to end!");
                player.sendMessage("§7Obtain forcefield items from crates, events, or shops.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
            }
        }
        // Border color (slot 23 for team mode, slot 24 for solo mode)
        else if ((slot == 23 || slot == 24) && clicked.getType() == Material.BEACON) {
            if (!team.isLeader(player.getUniqueId())) {
                player.sendMessage("§cOnly leaders can change border color!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                return;
            }
            
            org.skyraid.core.gui.BorderColorGUI borderGUI = new org.skyraid.core.gui.BorderColorGUI(plugin, player, team);
            borderGUI.open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }
        // Leave/Disband (slot 31)
        else if (slot == 31) {
            if (clicked.getType() == Material.BARRIER && team.getLeaderId().equals(player.getUniqueId())) {
                if (team.getMemberCount() > 1) {
                    player.sendMessage("§cYou must kick all members before disbanding!");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                } else {
                    player.closeInventory();
                    plugin.getChatConfirmationListener().requestConfirmation(player, 
                        org.skyraid.core.listener.ChatConfirmationListener.ConfirmationType.DISBAND_ISLAND);
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 0.8f);
                }
            } else if (clicked.getType() == Material.IRON_DOOR) {
                player.closeInventory();
                plugin.getChatConfirmationListener().requestConfirmation(player,
                    org.skyraid.core.listener.ChatConfirmationListener.ConfirmationType.LEAVE_ISLAND);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 0.8f);
            }
        }
        // Close button (slot 49)
        else if (slot == 49 && clicked.getType() == Material.RED_STAINED_GLASS_PANE) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }
        // Member heads (slots 10-16) - open player management
        else if (slot >= 10 && slot <= 16 && clicked.getType() == Material.PLAYER_HEAD) {
            if (!team.isLeader(player.getUniqueId())) {
                player.sendMessage("§cOnly leaders can manage members!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                return;
            }
            
            // Get UUID from skull meta
            org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) clicked.getItemMeta();
            if (meta.getOwningPlayer() != null) {
                UUID targetUUID = meta.getOwningPlayer().getUniqueId();
                if (!targetUUID.equals(player.getUniqueId())) {
                    org.skyraid.core.gui.PlayerManagementGUI mgmtGUI = 
                        new org.skyraid.core.gui.PlayerManagementGUI(plugin, player, team, targetUUID);
                    mgmtGUI.open();
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                }
            }
        }
    }
    
    /**
     * Handle Player Management GUI clicks
     */
    private void handlePlayerManagementClick(Player player, int slot, ItemStack clicked) {
        org.skyraid.core.data.PlayerData playerData = plugin.getTeamManager().getPlayerData(player.getUniqueId());
        if (playerData == null || playerData.getCurrentTeamId() == null) {
            player.closeInventory();
            return;
        }
        
        org.skyraid.core.data.TeamData team = plugin.getTeamManager().getTeam(playerData.getCurrentTeamId());
        if (team == null) {
            player.closeInventory();
            return;
        }
        
        // Get target player UUID from player head (slot 4)
        ItemStack headItem = player.getOpenInventory().getItem(4);
        if (headItem == null || headItem.getType() != Material.PLAYER_HEAD) {
            player.closeInventory();
            return;
        }
        
        org.bukkit.inventory.meta.SkullMeta headMeta = (org.bukkit.inventory.meta.SkullMeta) headItem.getItemMeta();
        if (headMeta.getOwningPlayer() == null) {
            player.closeInventory();
            return;
        }
        
        UUID targetUUID = headMeta.getOwningPlayer().getUniqueId();
        
        // Kick (slot 11)
        if (slot == 11 && clicked.getType() == Material.IRON_DOOR) {
            team.removeMember(targetUUID);
            plugin.getDatabaseManager().saveTeam(team);
            
            Player target = Bukkit.getPlayer(targetUUID);
            if (target != null && target.isOnline()) {
                target.sendMessage("§c§lYou were kicked from the island!");
                org.bukkit.World spawnWorld = org.bukkit.Bukkit.getWorld("world");
                if (spawnWorld != null) {
                    target.teleport(spawnWorld.getSpawnLocation());
                }
            }
            
            player.sendMessage("§aPlayer kicked from island!");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.8f, 1.0f);
            
            // Reopen island management GUI
            org.skyraid.core.gui.IslandMainGUI mainGUI = new org.skyraid.core.gui.IslandMainGUI(plugin, player, team);
            mainGUI.open();
        }
        // Promote/Demote (slot 13)
        else if (slot == 13) {
            boolean isPromoted = team.isLeader(targetUUID) && !team.getLeaderId().equals(targetUUID);
            
            if (isPromoted) {
                // Demote
                team.demoteFromLeader(targetUUID);
                plugin.getDatabaseManager().saveTeam(team);
                
                Player target = Bukkit.getPlayer(targetUUID);
                if (target != null && target.isOnline()) {
                    target.sendMessage("§e§lYou were demoted from leader!");
                }
                
                player.sendMessage("§aPlayer demoted!");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.8f);
            } else {
                // Promote
                team.promoteToLeader(targetUUID);
                plugin.getDatabaseManager().saveTeam(team);
                
                Player target = Bukkit.getPlayer(targetUUID);
                if (target != null && target.isOnline()) {
                    target.sendMessage("§e§lYou were promoted to leader!");
                }
                
                player.sendMessage("§aPlayer promoted!");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
            }
            
            // Reopen player management GUI
            org.skyraid.core.gui.PlayerManagementGUI mgmtGUI = 
                new org.skyraid.core.gui.PlayerManagementGUI(plugin, player, team, targetUUID);
            mgmtGUI.open();
        }
        // Back (slot 15)
        else if (slot == 15 && clicked.getType() == Material.ARROW) {
            org.skyraid.core.gui.IslandMainGUI mainGUI = new org.skyraid.core.gui.IslandMainGUI(plugin, player, team);
            mainGUI.open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }
    }
    
    /**
     * Handle Border Color GUI clicks
     */
    private void handleBorderColorClick(Player player, int slot, ItemStack clicked) {
        org.skyraid.core.data.PlayerData playerData = plugin.getTeamManager().getPlayerData(player.getUniqueId());
        if (playerData == null || playerData.getCurrentTeamId() == null) {
            player.closeInventory();
            return;
        }
        
        org.skyraid.core.data.TeamData team = plugin.getTeamManager().getTeam(playerData.getCurrentTeamId());
        if (team == null) {
            player.closeInventory();
            return;
        }
        
        // Color selections (slots 10-15)
        if (slot >= 10 && slot <= 15) {
            org.skyraid.core.util.BorderColor newColor = null;
            
            switch (slot) {
                case 10 -> newColor = org.skyraid.core.util.BorderColor.BLUE;
                case 11 -> newColor = org.skyraid.core.util.BorderColor.RED;
                case 12 -> newColor = org.skyraid.core.util.BorderColor.GREEN;
                case 13 -> newColor = org.skyraid.core.util.BorderColor.PURPLE;
                case 14 -> newColor = org.skyraid.core.util.BorderColor.YELLOW;
                case 15 -> newColor = org.skyraid.core.util.BorderColor.OFF;
            }
            
            if (newColor != null) {
                team.setBorderColor(newColor.name());
                plugin.getDatabaseManager().saveTeam(team);
                plugin.getBorderManager().refreshBordersForTeam(team);
                
                player.sendMessage("§a§lBorder color changed to " + newColor.getDisplayName() + "!");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                
                // Reopen border color GUI to show updated selection
                org.skyraid.core.gui.BorderColorGUI borderGUI = new org.skyraid.core.gui.BorderColorGUI(plugin, player, team);
                borderGUI.open();
            }
        }
        // Back button (slot 22)
        else if (slot == 22 && clicked.getType() == Material.ARROW) {
            org.skyraid.core.gui.IslandMainGUI mainGUI = new org.skyraid.core.gui.IslandMainGUI(plugin, player, team);
            mainGUI.open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }
    }
    
    /**
     * Handle Forcefield Confirmation GUI clicks
     */
    private void handleForcefieldConfirmClick(Player player, int slot, ItemStack clicked) {
        org.skyraid.core.data.PlayerData playerData = plugin.getTeamManager().getPlayerData(player.getUniqueId());
        if (playerData == null || playerData.getCurrentTeamId() == null) {
            player.closeInventory();
            return;
        }
        
        org.skyraid.core.data.TeamData team = plugin.getTeamManager().getTeam(playerData.getCurrentTeamId());
        if (team == null) {
            player.closeInventory();
            return;
        }
        
        // Confirm button (slot 11)
        if (slot == 11 && clicked.getType() == Material.LIME_STAINED_GLASS_PANE) {
            if (!team.isLeader(player.getUniqueId())) {
                player.sendMessage("§cOnly leaders can end forcefield!");
                player.closeInventory();
                return;
            }
            
            if (!team.hasForcefield()) {
                player.sendMessage("§cNo active forcefield!");
                player.closeInventory();
                return;
            }
            
            // End forcefield
            plugin.getForcefieldManager().deactivateForcefield(team);
            
            player.sendMessage("§c§lForcefield Ended!");
            player.sendMessage("§7Your forcefield has been deactivated.");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WITHER_BREAK_BLOCK, 0.8f, 1.0f);
            
            // Reopen island GUI
            org.skyraid.core.gui.IslandMainGUI mainGUI = new org.skyraid.core.gui.IslandMainGUI(plugin, player, team);
            mainGUI.open();
        }
        // Cancel button (slot 15)
        else if (slot == 15 && clicked.getType() == Material.RED_STAINED_GLASS_PANE) {
            // Go back to island GUI
            org.skyraid.core.gui.IslandMainGUI mainGUI = new org.skyraid.core.gui.IslandMainGUI(plugin, player, team);
            mainGUI.open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }
    }
    
    /**
     * Handle Player Selector GUI clicks
     */
    private void handlePlayerSelectorClick(Player player, int slot, ItemStack clicked, String title) {
        org.skyraid.core.data.PlayerData playerData = plugin.getTeamManager().getPlayerData(player.getUniqueId());
        if (playerData == null || playerData.getCurrentTeamId() == null) {
            player.closeInventory();
            return;
        }
        
        org.skyraid.core.data.TeamData team = plugin.getTeamManager().getTeam(playerData.getCurrentTeamId());
        if (team == null) {
            player.closeInventory();
            return;
        }
        
        // Extract page number from title
        int currentPage = 0;
        if (title.contains("Page ")) {
            try {
                String pageStr = title.substring(title.indexOf("Page ") + 5).trim();
                currentPage = Integer.parseInt(pageStr) - 1;
            } catch (Exception ignored) {}
        }
        
        // Previous page button (slot 45)
        if (slot == 45 && clicked.getType() == Material.ARROW) {
            org.skyraid.core.gui.PlayerSelectorGUI selectorGUI = 
                new org.skyraid.core.gui.PlayerSelectorGUI(plugin, player, team, Math.max(0, currentPage - 1));
            selectorGUI.open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }
        // Next page button (slot 53)
        else if (slot == 53 && clicked.getType() == Material.ARROW) {
            org.skyraid.core.gui.PlayerSelectorGUI selectorGUI = 
                new org.skyraid.core.gui.PlayerSelectorGUI(plugin, player, team, currentPage + 1);
            selectorGUI.open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }
        // Back button (slot 49)
        else if (slot == 49 && clicked.getType() == Material.BARRIER) {
            org.skyraid.core.gui.IslandMainGUI mainGUI = new org.skyraid.core.gui.IslandMainGUI(plugin, player, team);
            mainGUI.open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }
        // Player head click
        else if (clicked.getType() == Material.PLAYER_HEAD) {
            org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) clicked.getItemMeta();
            if (meta != null && meta.getOwningPlayer() != null) {
                Player target = Bukkit.getPlayer(meta.getOwningPlayer().getUniqueId());
                
                if (target == null) {
                    player.sendMessage("§cPlayer is no longer online!");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                    return;
                }
                
                // Check if target is already in a team
                org.skyraid.core.data.TeamData targetTeam = plugin.getTeamManager().getPlayerTeam(target);
                if (targetTeam != null) {
                    player.sendMessage("§c" + target.getName() + " is already in a team!");
                    player.sendMessage("§7They must leave their current team first.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                    return;
                }
                
                // Send invite
                plugin.getTeamManager().invitePlayerToTeam(target.getUniqueId(), team);
                
                // Open invite GUI for target
                org.skyraid.core.gui.InviteAcceptGUI inviteGUI = 
                    new org.skyraid.core.gui.InviteAcceptGUI(plugin, target, player, team.getTeamName());
                inviteGUI.open();
                
                player.sendMessage("§a§lInvite Sent!");
                player.sendMessage("§7" + target.getName() + " has been invited to your island.");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                player.closeInventory();
            }
        }
    }
    
    /**
     * Handle Pending Team Player Selector GUI clicks
     */
    private void handlePendingTeamPlayerSelectorClick(Player player, int slot, ItemStack clicked, String title) {
        PendingTeam pendingTeam = plugin.getTeamManager().getPendingTeam(player);
        if (pendingTeam == null) {
            player.sendMessage("§cCould not find your team setup!");
            player.closeInventory();
            return;
        }
        
        // Extract page number from title
        int currentPage = 0;
        if (title.contains("Page ")) {
            try {
                String pageStr = title.substring(title.indexOf("Page ") + 5).trim();
                currentPage = Integer.parseInt(pageStr) - 1;
            } catch (Exception ignored) {}
        }
        
        // Previous page button (slot 45)
        if (slot == 45 && clicked.getType() == Material.ARROW) {
            PendingTeamPlayerSelectorGUI selectorGUI = 
                new PendingTeamPlayerSelectorGUI(plugin, player, pendingTeam, Math.max(0, currentPage - 1));
            selectorGUI.open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }
        // Next page button (slot 53)
        else if (slot == 53 && clicked.getType() == Material.ARROW) {
            PendingTeamPlayerSelectorGUI selectorGUI = 
                new PendingTeamPlayerSelectorGUI(plugin, player, pendingTeam, currentPage + 1);
            selectorGUI.open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }
        // Back button (slot 49)
        else if (slot == 49 && clicked.getType() == Material.BARRIER) {
            GameRoomGUI gameRoomGUI = new GameRoomGUI(plugin, player);
            gameRoomGUI.open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }
        // Player head click
        else if (clicked.getType() == Material.PLAYER_HEAD) {
            org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) clicked.getItemMeta();
            if (meta != null && meta.getOwningPlayer() != null) {
                Player target = Bukkit.getPlayer(meta.getOwningPlayer().getUniqueId());
                
                if (target == null) {
                    player.sendMessage("§cPlayer is no longer online!");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                    return;
                }
                
                // Check if player already has a pending invite
                org.skyraid.core.data.PlayerData targetData = plugin.getTeamManager().getPlayerData(target.getUniqueId());
                if (targetData != null && targetData.getPendingInvite() != null) {
                    player.sendMessage("§c" + target.getName() + " already has a pending invite!");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                    return;
                }
                
                // Check if target is already in a team or has a pending team
                org.skyraid.core.data.TeamData targetTeam = plugin.getTeamManager().getPlayerTeam(target);
                if (targetTeam != null) {
                    player.sendMessage("§c" + target.getName() + " is already in a team!");
                    player.sendMessage("§7They must leave their current team first.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                    return;
                }
                
                // Send invite through pending team system
                // Store the inviter's UUID as the pending invite
                plugin.getTeamManager().setPendingInvite(target.getUniqueId(), player.getUniqueId());
                
                // Open invite GUI for target (showing the owner's team name)
                String teamName = player.getName() + "'s Team";
                org.skyraid.core.gui.InviteAcceptGUI inviteGUI = 
                    new org.skyraid.core.gui.InviteAcceptGUI(plugin, target, player, teamName);
                inviteGUI.open();
                
                player.sendMessage("§a§lInvite Sent!");
                player.sendMessage("§7" + target.getName() + " has been invited to your team.");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                player.closeInventory();
            }
        }
    }
}

