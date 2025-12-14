package org.skyraid.core.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.skyraid.SkyRaidPlugin;
import org.skyraid.core.data.PlayerData;

/**
 * Handles first-join welcome message for new players
 */
public class FirstJoinListener implements Listener {
    
    private final SkyRaidPlugin plugin;
    
    public FirstJoinListener(SkyRaidPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Load or create player data
        PlayerData playerData = plugin.getTeamManager().getPlayerData(event.getPlayer().getUniqueId());
        
        // Only show welcome on first join
        if (playerData != null && playerData.getFirstJoinTime() == playerData.getJoinedAt()) {
            sendWelcomeMessage(event.getPlayer());
        }
    }
    
    /**
     * Sends beautiful welcome message with game info
     */
    private void sendWelcomeMessage(org.bukkit.entity.Player player) {
        // Clear chat area for visibility
        for (int i = 0; i < 3; i++) {
            player.sendMessage(Component.empty());
        }
        
        // Title
        player.sendMessage(
            Component.text("╔════════════════════════════════════════╗", NamedTextColor.GOLD)
        );
        player.sendMessage(
            Component.text("║  ", NamedTextColor.GOLD)
                .append(Component.text("Welcome to ", NamedTextColor.WHITE))
                .append(Component.text("SkyRaid", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.BOLD, true))
                .append(Component.text("  ║", NamedTextColor.GOLD))
        );
        player.sendMessage(
            Component.text("╚════════════════════════════════════════╝", NamedTextColor.GOLD)
        );
        
        player.sendMessage(Component.empty());
        
        // Game description
        player.sendMessage(
            Component.text("Game Overview:", NamedTextColor.YELLOW)
                .decoration(TextDecoration.BOLD, true)
        );
        player.sendMessage(
            Component.text("  • ", NamedTextColor.GRAY)
                .append(Component.text("Survive on a small skyblock island in the void", NamedTextColor.WHITE))
        );
        player.sendMessage(
            Component.text("  • ", NamedTextColor.GRAY)
                .append(Component.text("Team up with friends (max 8 players per island)", NamedTextColor.WHITE))
        );
        player.sendMessage(
            Component.text("  • ", NamedTextColor.GRAY)
                .append(Component.text("Raid other islands for resources and treasure", NamedTextColor.WHITE))
        );
        player.sendMessage(
            Component.text("  • ", NamedTextColor.GRAY)
                .append(Component.text("Use forcefields to protect your base", NamedTextColor.WHITE))
        );
        player.sendMessage(
            Component.text("  • ", NamedTextColor.GRAY)
                .append(Component.text("Bridge 1000 blocks to reach enemy islands", NamedTextColor.WHITE))
        );
        
        player.sendMessage(Component.empty());
        
        // Quick start
        player.sendMessage(
            Component.text("Quick Start:", NamedTextColor.YELLOW)
                .decoration(TextDecoration.BOLD, true)
        );
        player.sendMessage(
            Component.text("  1. ", NamedTextColor.GRAY)
                .append(Component.text("Create your first island with: ", NamedTextColor.WHITE))
                .append(Component.text("/is new", NamedTextColor.AQUA)
                    .decoration(TextDecoration.BOLD, true))
        );
        player.sendMessage(
            Component.text("  2. ", NamedTextColor.GRAY)
                .append(Component.text("Teleport home with: ", NamedTextColor.WHITE))
                .append(Component.text("/is home", NamedTextColor.AQUA)
                    .decoration(TextDecoration.BOLD, true))
        );
        player.sendMessage(
            Component.text("  3. ", NamedTextColor.GRAY)
                .append(Component.text("Invite friends: ", NamedTextColor.WHITE))
                .append(Component.text("/is invite [player]", NamedTextColor.AQUA)
                    .decoration(TextDecoration.BOLD, true))
        );
        
        player.sendMessage(Component.empty());
        
        // Available commands
        player.sendMessage(
            Component.text("Available Commands:", NamedTextColor.YELLOW)
                .decoration(TextDecoration.BOLD, true)
        );
        player.sendMessage(
            Component.text("  General: ", NamedTextColor.GRAY)
                .append(Component.text("/is help", NamedTextColor.GREEN))
                .append(Component.text(" - Show all commands", NamedTextColor.WHITE))
        );
        player.sendMessage(
            Component.text("  Info: ", NamedTextColor.GRAY)
                .append(Component.text("/is top", NamedTextColor.GREEN))
                .append(Component.text(" - View leaderboard", NamedTextColor.WHITE))
        );
        player.sendMessage(
            Component.text("  Admin: ", NamedTextColor.GRAY)
                .append(Component.text("/skyraid help", NamedTextColor.RED))
                .append(Component.text(" - Admin commands", NamedTextColor.WHITE))
        );
        
        player.sendMessage(Component.empty());
        
        // Help reminder
        player.sendMessage(
            Component.text("Need help? Run ", NamedTextColor.WHITE)
                .append(Component.text("/is help", NamedTextColor.AQUA)
                    .decoration(TextDecoration.BOLD, true))
                .append(Component.text(" anytime to see all available commands", NamedTextColor.WHITE))
        );
        
        player.sendMessage(Component.empty());
        
        // Footer
        player.sendMessage(
            Component.text("═══════════════════════════════════════", NamedTextColor.GOLD)
        );
    }
}

