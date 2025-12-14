package org.skyraid.core.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.skyraid.SkyRaidPlugin;
import org.skyraid.core.data.PlayerData;
import org.skyraid.core.data.TeamData;

/**
 * Handles chat filtering for team chat
 */
public class ChatListener implements Listener {
    
    private final SkyRaidPlugin plugin;
    
    public ChatListener(SkyRaidPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        PlayerData playerData = plugin.getTeamManager().getPlayerData(event.getPlayer().getUniqueId());
        
        if (playerData == null || !playerData.isUsingTeamChat()) {
            // Global chat - no filtering
            return;
        }
        
        TeamData team = plugin.getTeamManager().getPlayerTeam(event.getPlayer());
        if (team == null) {
            playerData.setUseTeamChat(false);
            return;
        }
        
        // Team chat - filter recipients
        event.getRecipients().clear();
        
        String prefix = plugin.getConfigManager().getString("chat.team_prefix", "[TEAM]");
        String color = plugin.getConfigManager().getString("chat.team_prefix_color", "§6");
        
        String message = color + prefix + "§r " + event.getFormat().replace("%1\\$s", "").replace("%2\\$s", "").trim();
        
        // Send to all team members
        for (java.util.UUID memberUUID : team.getMembers()) {
            org.bukkit.entity.Player member = org.bukkit.Bukkit.getPlayer(memberUUID);
            if (member != null && member.isOnline()) {
                event.getRecipients().add(member);
            }
        }
        
        plugin.logDebug("commands", "Team chat message from " + event.getPlayer().getName());
    }
}

