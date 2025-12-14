package org.skyraid.core.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.skyraid.SkyRaidPlugin;
import org.skyraid.core.data.TeamData;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Manages island generation and island-related operations with proper skyblock island creation
 */
public class IslandManager {
    
    private final SkyRaidPlugin plugin;
    private final World world;
    private final int islandSpacing;
    private final int islandSize;
    private final int islandY;  // Y level for all islands
    private final Map<Integer, TeamData> islandIndexMap = new HashMap<>();
    private final Set<String> usedCoordinates = new HashSet<>();  // Track used island coordinates
    
    public IslandManager(SkyRaidPlugin plugin) {
        this.plugin = plugin;
        
        // Try to get world from config
        String worldName = plugin.getConfigManager().getString("world.warp_world", "").trim();
        
        // If empty, auto-detect first available world
        if (worldName.isEmpty()) {
            if (plugin.getServer().getWorlds().isEmpty()) {
                plugin.logError("No worlds available! Island generation disabled.");
                this.world = null;
            } else {
                this.world = plugin.getServer().getWorlds().get(0);
                plugin.logInfo("Auto-detected world: " + this.world.getName());
            }
        } else {
            // Try to get specified world
            this.world = plugin.getServer().getWorld(worldName);
            if (this.world == null) {
                plugin.logError("World not found: '" + worldName + "'. Available worlds: " + 
                    getAvailableWorlds());
                plugin.logError("Please set 'world.warp_world' in config.yml to a valid world name.");
            }
        }
        
        this.islandSpacing = plugin.getConfigManager().getInt("island.spacing", 1000);
        this.islandSize = plugin.getConfigManager().getInt("island.size", 100);
        this.islandY = plugin.getConfigManager().getInt("island.height", 64);  // Y level for all islands
        
        if (world != null) {
            plugin.logInfo("Island Manager initialized for world: " + world.getName());
            plugin.logInfo("Island settings: Size=" + islandSize + " Spacing=" + islandSpacing + " Y=" + islandY);
        }
    }
    
    /**
     * Gets list of available worlds for error messages
     */
    private String getAvailableWorlds() {
        if (plugin.getServer().getWorlds().isEmpty()) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        for (org.bukkit.World w : plugin.getServer().getWorlds()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("'").append(w.getName()).append("'");
        }
        return sb.toString();
    }
    
    /**
     * Create a singleplayer island (GUI-triggered)
     */
    public void createSingleplayerIsland(Player player, String teamName) {
        if (world == null) {
            player.sendMessage("§cError: World not configured!");
            return;
        }
        
        // Check if player already has a team
        if (plugin.getTeamManager().getPlayerTeam(player) != null) {
            player.sendMessage("§cYou already have an island!");
            return;
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Find position
            int[] position = findNextIslandPosition();
            int islandX = position[0];
            int islandZ = position[1];
            
            plugin.logInfo("Creating singleplayer island for " + player.getName() + " at X=" + islandX + " Z=" + islandZ);
            
            // Create team
            TeamData team = new TeamData(UUID.randomUUID(), player.getUniqueId(), teamName, islandX, islandZ);
            team.setHome(islandX, islandY + 5, islandZ, 0, 0);
            
            // Store
            String coordKey = islandX + "," + islandZ;
            usedCoordinates.add(coordKey);
            islandIndexMap.put(islandX * 31 + islandZ, team);
            
            // Generate island (will complete on main thread)
            generateSkyblockIslandAsync(team).thenRun(() -> {
                // Save to database
                plugin.getDatabaseManager().saveTeam(team);
                
                // Update player data and add to team manager cache
                org.skyraid.core.data.PlayerData playerData = new org.skyraid.core.data.PlayerData(player.getUniqueId(), player.getName());
                playerData.setCurrentTeamId(team.getTeamId());
                plugin.getDatabaseManager().savePlayer(playerData);
                
                // CRITICAL: Add player to team manager's cache
                plugin.getTeamManager().addPlayerToCache(player.getUniqueId(), playerData);
                plugin.getTeamManager().addTeamToCache(team.getTeamId(), team);
                
                plugin.logInfo("Player " + player.getName() + " added to team " + team.getTeamName() + " (ID: " + team.getTeamId() + ")");
                
                // Teleport and notify
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Location home = getSafeHomeLocation(team);
                    player.setFallDistance(0.0F);
                    player.teleport(home);
                    player.sendMessage("§a§lIsland Created Successfully!");
                    player.sendMessage("§eWelcome to your skyblock adventure!");
                    player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    
                    // Give forcefield star if configured
                    if (plugin.getConfig().getBoolean("forcefield.give_on_island_creation", false)) {
                        long duration = plugin.getConfig().getLong("forcefield.initial_star_duration", 6);
                        plugin.getForcefieldManager().giveForcefieldStar(player, duration);
                    }
                });
            }).exceptionally(throwable -> {
                throwable.printStackTrace();
                player.sendMessage("§cError creating island: " + throwable.getMessage());
                return null;
            });
        });
    }
    
    /**
     * Create a multiplayer/team island (GUI-triggered)
     */
    public void createMultiplayerIsland(Player owner, org.skyraid.core.data.PendingTeam pendingTeam) {
        if (world == null) {
            owner.sendMessage("§cError: World not configured!");
            return;
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Find position
            int[] position = findNextIslandPosition();
            int islandX = position[0];
            int islandZ = position[1];
            
            plugin.logInfo("Creating team island for " + owner.getName() + " at X=" + islandX + " Z=" + islandZ);
            
            // Create team
            TeamData team = new TeamData(UUID.randomUUID(), owner.getUniqueId(), pendingTeam.getTeamName(), islandX, islandZ);
            team.setHome(islandX, islandY + 5, islandZ, 0, 0);
            
            // Store
            String coordKey = islandX + "," + islandZ;
            usedCoordinates.add(coordKey);
            islandIndexMap.put(islandX * 31 + islandZ, team);
            
            // Generate island
            generateSkyblockIslandAsync(team).thenRun(() -> {
                // Save to database
                plugin.getDatabaseManager().saveTeam(team);
                
                // CRITICAL: Add team to cache first
                plugin.getTeamManager().addTeamToCache(team.getTeamId(), team);
                
                // Add all members to team
                for (Player member : pendingTeam.getMembers()) {
                    org.skyraid.core.data.PlayerData playerData = new org.skyraid.core.data.PlayerData(member.getUniqueId(), member.getName());
                    playerData.setCurrentTeamId(team.getTeamId());
                    plugin.getDatabaseManager().savePlayer(playerData);
                    
                    // CRITICAL: Add player to cache
                    plugin.getTeamManager().addPlayerToCache(member.getUniqueId(), playerData);
                    
                    plugin.logInfo("Player " + member.getName() + " added to team " + team.getTeamName() + " (ID: " + team.getTeamId() + ")");
                    
                    // Teleport each member
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Location home = getSafeHomeLocation(team);
                        member.setFallDistance(0.0F);
                        member.teleport(home);
                        member.sendMessage("§a§lTeam Island Created!");
                        member.sendMessage("§eWelcome to your team's adventure!");
                        member.playSound(member.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                        
                        // Give forcefield star to owner only if configured
                        if (member.equals(owner) && plugin.getConfig().getBoolean("forcefield.give_on_island_creation", false)) {
                            long duration = plugin.getConfig().getLong("forcefield.initial_star_duration", 6);
                            plugin.getForcefieldManager().giveForcefieldStar(member, duration);
                        }
                    });
                }
                
                // Clean up pending team
                plugin.getTeamManager().removePendingTeam(owner);
                
            }).exceptionally(throwable -> {
                throwable.printStackTrace();
                owner.sendMessage("§cError creating island: " + throwable.getMessage());
                return null;
            });
        });
    }
    
    /**
     * Generates a new island for a team with smart positioning (async)
     * @deprecated Use createSingleplayerIsland or createMultiplayerIsland instead
     */
    @Deprecated
    public CompletableFuture<TeamData> generateIslandAsync(UUID leaderId, String teamName, Player owner) {
        return CompletableFuture.supplyAsync(() -> {
            if (world == null) {
                plugin.logError("Cannot generate island: world is null");
                return null;
            }
            
            // Find next available island position
            int[] position = findNextIslandPosition();
            int islandX = position[0];
            int islandZ = position[1];
            
            plugin.logInfo("Generating island for team: " + teamName + " at X=" + islandX + " Z=" + islandZ);
            
            // Create team data with proper home position
            TeamData team = new TeamData(UUID.randomUUID(), leaderId, teamName, islandX, islandZ);
            team.setHome(islandX, islandY + 5, islandZ, 0, 0);
            
            // Store in maps
            String coordKey = islandX + "," + islandZ;
            usedCoordinates.add(coordKey);
            islandIndexMap.put(islandX * 31 + islandZ, team);
            
            // Generate island terrain asynchronously
            generateSkyblockIslandAsync(team).join();
            
            // Save to database
            plugin.getDatabaseManager().saveTeam(team);
            
            // Teleport player to island on main thread
            if (owner != null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Location home = new Location(world, islandX, islandY + 5, islandZ);
                    owner.teleport(home);
                    owner.sendMessage("§aIsland created! Welcome to your new island!");
                });
            }
            
            plugin.logInfo("Island generated for team: " + teamName);
            return team;
        });
    }
    
    /**
     * Synchronous version for backward compatibility
     */
    public TeamData generateIsland(UUID leaderId, String teamName) {
        try {
            return generateIslandAsync(leaderId, teamName, null).join();
        } catch (Exception e) {
            plugin.logError("Failed to generate island: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Finds next available island position using IridiumSkyblock's square spiral algorithm
     * This creates a perfect square spiral pattern starting from (0, 0)
     */
    private int[] findNextIslandPosition() {
        if (world == null) {
            plugin.logError("World is null, cannot find island position");
            return new int[]{0, 0};
        }
        
        // Calculate next island ID (starting from 1)
        int islandId = usedCoordinates.size() + 1;
        
        // Use IridiumSkyblock's square spiral algorithm
        Location center = getIslandCenterFromId(islandId);
        
        int nextX = (int) center.getX();
        int nextZ = (int) center.getZ();
        
        plugin.logInfo("Island #" + islandId + " position: X=" + nextX + " Z=" + nextZ + " spacing=" + islandSpacing);
        return new int[]{nextX, nextZ};
    }
    
    /**
     * Calculate island center using IridiumSkyblock's perfect square spiral algorithm
     * Algorithm creates a spiral: (0,0) -> (0,-d) -> (d,-d) -> (d,0) -> (d,d) -> (0,d) -> (-d,d) -> ...
     * where d = distance (islandSpacing)
     */
    private Location getIslandCenterFromId(int islandId) {
        // Island ID 1 is at origin
        if (islandId == 1) {
            return new Location(world, 0, 0, 0);
        }
        
        // In this algorithm, position 2 is where id 1 is, position 3 is where id 2 is, etc.
        int position = islandId - 1;
        
        // The radius of the last completed square
        int radius = (int) (Math.floor((Math.sqrt(position) - 1) / 2) + 1);
        int diameter = radius * 2;
        int perimeter = diameter * 4;
        
        // The position the square was last completed at
        int lastCompletePosition = (perimeter * (radius - 1)) / 2;
        
        // The current index in the perimeter where 1 is first and 0 is the last index
        int currentIndexInPerimeter = (position - lastCompletePosition) % perimeter;
        
        Location location;
        
        // Determine which side of the square we're on
        switch (currentIndexInPerimeter / diameter) {
            case 0:
                location = new Location(world, (currentIndexInPerimeter - radius), 0, -radius);
                break;
            case 1:
                location = new Location(world, radius, 0, (currentIndexInPerimeter % diameter) - radius);
                break;
            case 2:
                location = new Location(world, radius - (currentIndexInPerimeter % diameter), 0, radius);
                break;
            case 3:
                location = new Location(world, -radius, 0, radius - (currentIndexInPerimeter % diameter));
                break;
            default:
                throw new IllegalStateException("Could not find island location with ID: " + islandId);
        }
        
        // Multiply by distance to get actual coordinates
        return location.multiply(islandSpacing);
    }
    
    /**
     * Generate island asynchronously (similar to IridiumSkyblock)
     */
    private CompletableFuture<Void> generateSkyblockIslandAsync(TeamData team) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        int centerX = team.getIslandX();
        int centerZ = team.getIslandZ();
        
        plugin.logInfo("Generating island at X=" + centerX + " Z=" + centerZ + " Y=" + islandY);
        
        // Run on main thread since we're placing blocks
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                Location pasteLocation = new Location(world, centerX, islandY, centerZ);
                org.skyraid.core.util.SchematicLoader schematicLoader = plugin.getSchematicLoader();
                
                if (schematicLoader != null && schematicLoader.isLoaded()) {
                    plugin.logInfo("Pasting schematic at " + pasteLocation);
                    schematicLoader.pasteSchematic(pasteLocation);
                    plugin.logInfo("Schematic pasted successfully");
                } else {
                    plugin.logWarning("Schematic not loaded, generating fallback island");
                    generateFallbackIsland(centerX, centerZ);
                }
                future.complete(null);
            } catch (Exception e) {
                plugin.logError("Error generating island: " + e.getMessage());
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }
    
    /**
     * Synchronous version for backward compatibility
     */
    private void generateSkyblockIsland(int centerX, int centerZ) {
        plugin.logInfo("Generating island schematic at X=" + centerX + " Z=" + centerZ + " Y=" + islandY);
        
        Location pasteLocation = new Location(world, centerX, islandY, centerZ);
        org.skyraid.core.util.SchematicLoader schematicLoader = plugin.getSchematicLoader();
        
        if (schematicLoader != null && schematicLoader.isLoaded()) {
            plugin.logInfo("Pasting schematic");
            schematicLoader.pasteSchematic(pasteLocation);
            plugin.logInfo("Schematic pasted successfully");
        } else {
            plugin.logWarning("Schematic not loaded, generating fallback");
            generateFallbackIsland(centerX, centerZ);
        }
    }
    
    /**
     * Clear island area before generation (async)
     */
    private CompletableFuture<Void> clearIslandArea(TeamData team) {
        return CompletableFuture.runAsync(() -> {
            if (world == null) return;
            
            int centerX = team.getIslandX();
            int centerZ = team.getIslandZ();
            int radius = islandSize / 2;
            
            // Clear in chunks to avoid lag
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (int x = centerX - radius; x <= centerX + radius; x++) {
                    for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                        for (int y = islandY - 5; y <= islandY + 50; y++) {
                            Block block = world.getBlockAt(x, y, z);
                            if (block.getType() != Material.AIR) {
                                block.setType(Material.AIR, false);
                            }
                        }
                    }
                }
            });
        });
    }
    
    /**
     * Fallback island if schematic fails
     */
    private void generateFallbackIsland(int centerX, int centerZ) {
        int baseY = islandY;
        int size = 10;
        
        // Simple 20x20 platform
        for (int x = centerX - size; x <= centerX + size; x++) {
            for (int z = centerZ - size; z <= centerZ + size; z++) {
                world.getBlockAt(x, baseY - 1, z).setType(Material.BEDROCK);
                world.getBlockAt(x, baseY, z).setType(Material.STONE);
                world.getBlockAt(x, baseY + 1, z).setType(Material.DIRT);
                world.getBlockAt(x, baseY + 2, z).setType(Material.GRASS_BLOCK);
            }
        }
        
        // Simple tree
        world.getBlockAt(centerX, baseY + 3, centerZ).setType(Material.OAK_LOG);
        world.getBlockAt(centerX, baseY + 4, centerZ).setType(Material.OAK_LOG);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.getBlockAt(centerX + dx, baseY + 4, centerZ + dz).setType(Material.OAK_LEAVES);
            }
        }
    }
    
    /**
     * Gets an island location by team
     */
    public Location getIslandLocation(TeamData team) {
        return new Location(world, team.getIslandX(), islandY, team.getIslandZ());
    }
    
    /**
     * Gets a safe home location for a team (similar to IridiumSkyblock)
     * Finds a safe spot near the home location to prevent suffocation or falling
     */
    public Location getSafeHomeLocation(TeamData team) {
        Location home = new Location(world, team.getHomeX(), team.getHomeY(), team.getHomeZ(), team.getHomeYaw(), team.getHomePitch());
        return getSafeLocation(home, team);
    }
    
    /**
     * Find a safe location near the given location (similar to IridiumSkyblock's LocationUtils)
     * A safe location has:
     * - Solid block below
     * - Air at feet and head level
     * - Not lava or dangerous blocks
     */
    private Location getSafeLocation(Location location, TeamData team) {
        if (world == null || location == null) {
            return location;
        }
        
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        
        // First, try the exact location
        if (isSafeLocation(x, y, z)) {
            return new Location(world, x + 0.5, y, z + 0.5, location.getYaw(), location.getPitch());
        }
        
        // Search in a 5x10x5 area (5 blocks horizontally, 10 blocks vertically)
        for (int dy = 0; dy <= 10; dy++) {
            // Try going up first
            if (y + dy <= world.getMaxHeight() - 2 && isSafeLocation(x, y + dy, z)) {
                return new Location(world, x + 0.5, y + dy, z + 0.5, location.getYaw(), location.getPitch());
            }
            // Then try going down
            if (dy > 0 && y - dy >= world.getMinHeight() && isSafeLocation(x, y - dy, z)) {
                return new Location(world, x + 0.5, y - dy, z + 0.5, location.getYaw(), location.getPitch());
            }
        }
        
        // Search nearby blocks horizontally
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) continue;
                
                for (int dy = -5; dy <= 5; dy++) {
                    int checkY = y + dy;
                    if (checkY < world.getMinHeight() || checkY > world.getMaxHeight() - 2) continue;
                    
                    if (isSafeLocation(x + dx, checkY, z + dz)) {
                        return new Location(world, x + dx + 0.5, checkY, z + dz + 0.5, location.getYaw(), location.getPitch());
                    }
                }
            }
        }
        
        // If no safe location found, return original (player may fall but won't suffocate)
        return location;
    }
    
    /**
     * Check if a location is safe for teleportation
     */
    private boolean isSafeLocation(int x, int y, int z) {
        if (world == null) return false;
        
        Block blockBelow = world.getBlockAt(x, y - 1, z);
        Block blockFeet = world.getBlockAt(x, y, z);
        Block blockHead = world.getBlockAt(x, y + 1, z);
        
        // Must have solid block below (not air, water, lava)
        if (!blockBelow.getType().isSolid()) {
            return false;
        }
        
        // Feet and head must be air or passable
        if (!isPassable(blockFeet.getType()) || !isPassable(blockHead.getType())) {
            return false;
        }
        
        // Not above dangerous blocks
        if (isDangerous(blockBelow.getType())) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if a block is passable (air, grass, flowers, etc)
     */
    private boolean isPassable(Material material) {
        return material == Material.AIR || 
               material == Material.CAVE_AIR ||
               material == Material.VOID_AIR ||
               !material.isSolid();
    }
    
    /**
     * Check if a block is dangerous (lava, fire, magma, cactus)
     */
    private boolean isDangerous(Material material) {
        return material == Material.LAVA ||
               material == Material.FIRE ||
               material == Material.SOUL_FIRE ||
               material == Material.MAGMA_BLOCK ||
               material == Material.CACTUS ||
               material == Material.SWEET_BERRY_BUSH ||
               material == Material.WITHER_ROSE;
    }
    
    /**
     * Resets an island (clears all blocks)
     */
    public void resetIsland(TeamData team) {
        if (world == null) return;
        
        plugin.logInfo("Resetting island for: " + team.getTeamName());
        
        int centerX = team.getIslandX();
        int centerZ = team.getIslandZ();
        int radius = islandSize / 2;
        
        // Clear the island area
        for (int x = centerX - radius - 10; x <= centerX + radius + 10; x++) {
            for (int z = centerZ - radius - 10; z <= centerZ + radius + 10; z++) {
                for (int y = islandY - 2; y <= islandY + 50; y++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR);
                }
            }
        }
        
        // Regenerate the island
        generateSkyblockIsland(centerX, centerZ);
        
        // Reset home location
        team.setHome(centerX, islandY + 5, centerZ, 0, 0);
        plugin.getDatabaseManager().saveTeam(team);
    }
    
    /**
     * Checks if a location is within an island
     */
    public boolean isInIsland(Location location, TeamData team) {
        if (!location.getWorld().equals(world)) {
            return false;
        }
        
        int centerX = team.getIslandX();
        int centerZ = team.getIslandZ();
        int radius = islandSize / 2;
        
        int x = location.getBlockX();
        int z = location.getBlockZ();
        
        return (Math.pow(x - centerX, 2) + Math.pow(z - centerZ, 2)) <= (radius * radius);
    }
    
    /**
     * Finds which team owns an island at a location
     */
    public TeamData findTeamAtLocation(Location location) {
        if (!location.getWorld().equals(world)) {
            return null;
        }
        
        for (TeamData team : islandIndexMap.values()) {
            if (isInIsland(location, team)) {
                return team;
            }
        }
        
        return null;
    }
    
    /**
     * Deletes an island
     */
    public void deleteIsland(TeamData team) {
        if (world == null) return;
        
        plugin.logInfo("Deleting island for: " + team.getTeamName());
        
        int centerX = team.getIslandX();
        int centerZ = team.getIslandZ();
        int radius = islandSize / 2;
        
        // Clear all blocks on the island
        for (int x = centerX - radius - 10; x <= centerX + radius + 10; x++) {
            for (int z = centerZ - radius - 10; z <= centerZ + radius + 10; z++) {
                for (int y = 0; y <= 255; y++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR);
                }
            }
        }
        
        // Remove from tracking
        String coordKey = centerX + "," + centerZ;
        usedCoordinates.remove(coordKey);
        islandIndexMap.remove(centerX * 31 + centerZ);
        
        plugin.logInfo("Island deleted for team: " + team.getTeamName());
    }

    /**
     * Opens setup wizard for admin configuration
     */
    public void setupIsland(org.bukkit.entity.Player player) {
        plugin.logInfo("Opening setup wizard for: " + player.getName());
        // Setup wizard will open with world detection
    }
}
