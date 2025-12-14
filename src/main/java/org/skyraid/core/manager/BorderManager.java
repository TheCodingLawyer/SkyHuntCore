package org.skyraid.core.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.skyraid.SkyRaidPlugin;
import org.skyraid.core.data.TeamData;
import org.skyraid.core.util.BorderColor;

/**
 * Manages island borders using WorldBorder API
 * Creates visual boundaries around each island
 */
public class BorderManager {
    
    private final SkyRaidPlugin plugin;
    private final int borderSize; // Visual border size (much larger than island size)
    
    public BorderManager(SkyRaidPlugin plugin) {
        this.plugin = plugin;
        // Border is VISUAL ONLY - make it large so it doesn't limit building
        // IridiumSkyblock uses NMS for non-blocking borders, we use large Bukkit borders
        this.borderSize = plugin.getConfig().getInt("island.border_size", 500);
    }
    
    /**
     * Send island border to player based on their current location
     * Called when player moves/teleports
     */
    public void sendIslandBorder(Player player) {
        World skyblockWorld = plugin.getWorldValidator().getOrCreateSkyblockWorld();
        if (skyblockWorld == null || !player.getWorld().equals(skyblockWorld)) {
            return; // Not in skyblock world
        }
        
        Location playerLoc = player.getLocation();
        
        // Find which island the player is on
        TeamData team = findIslandAtLocation(playerLoc);
        
        if (team != null) {
            // Show border for this island
            showBorderForTeam(player, team);
        } else {
            // Not on any island, hide border
            hideBorder(player);
        }
    }
    
    /**
     * Find the island at the given location
     */
    private TeamData findIslandAtLocation(Location location) {
        int spacing = plugin.getConfig().getInt("island-spacing", 1000);
        int islandSize = plugin.getConfig().getInt("island.size", 100); // Actual island protection size
        int halfSize = islandSize / 2;
        
        // Get all teams and check if location is within their boundaries
        for (TeamData team : plugin.getTeamManager().getAllTeams()) {
            int islandCenterX = team.getIslandX();
            int islandCenterZ = team.getIslandZ();
            
            int minX = islandCenterX - halfSize;
            int maxX = islandCenterX + halfSize;
            int minZ = islandCenterZ - halfSize;
            int maxZ = islandCenterZ + halfSize;
            
            if (location.getBlockX() >= minX && location.getBlockX() <= maxX &&
                location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ) {
                return team;
            }
        }
        
        return null;
    }
    
    /**
     * Show colored border for a specific team's island
     */
    private void showBorderForTeam(Player player, TeamData team) {
        BorderColor borderColor = BorderColor.fromString(team.getBorderColor());
        
        if (borderColor == BorderColor.OFF) {
            hideBorder(player);
            return;
        }
        
        // Create a "fake" world border centered on the island
        WorldBorder worldBorder = createIslandBorder(team);
        
        // Send to player
        player.setWorldBorder(worldBorder);
    }
    
    /**
     * Create a WorldBorder for an island
     */
    private WorldBorder createIslandBorder(TeamData team) {
        World skyblockWorld = plugin.getWorldValidator().getOrCreateSkyblockWorld();
        if (skyblockWorld == null) {
            return null;
        }
        
        // Create border
        WorldBorder border = Bukkit.createWorldBorder();
        
        // Set center to island center
        Location center = new Location(skyblockWorld, team.getIslandX(), 64, team.getIslandZ());
        border.setCenter(center);
        
        // Set size to border size (large to not limit building)
        border.setSize(borderSize);
        
        // Apply color settings
        BorderColor color = BorderColor.fromString(team.getBorderColor());
        color.applyToWorldBorder(border);
        
        return border;
    }
    
    /**
     * Hide border for player
     */
    private void hideBorder(Player player) {
        player.setWorldBorder(null);
    }
    
    /**
     * Refresh borders for all team members (e.g., after color change)
     */
    public void refreshBordersForTeam(TeamData team) {
        World skyblockWorld = plugin.getWorldValidator().getOrCreateSkyblockWorld();
        if (skyblockWorld == null) return;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(skyblockWorld)) {
                // Check if player is on this island
                Location loc = player.getLocation();
                TeamData playerIsland = findIslandAtLocation(loc);
                
                if (playerIsland != null && playerIsland.getTeamId().equals(team.getTeamId())) {
                    sendIslandBorder(player);
                }
            }
        }
    }
}

