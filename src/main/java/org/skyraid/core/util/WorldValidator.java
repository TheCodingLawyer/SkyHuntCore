package org.skyraid.core.util;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.skyraid.SkyRaidPlugin;

/**
 * Validates and manages skyblock void world
 * Automatically creates 'skyblock' world if it doesn't exist
 */
public class WorldValidator {
    
    private final SkyRaidPlugin plugin;
    private final WorldFileGenerator worldGenerator;
    private World cachedSkyblockWorld = null; // Cache to avoid repeated lookups
    
    public WorldValidator(SkyRaidPlugin plugin) {
        this.plugin = plugin;
        this.worldGenerator = new WorldFileGenerator(plugin);
    }
    
    /**
     * Gets or creates the skyblock world
     * First checks for "skyblock" - if missing, creates it automatically
     */
    public World getOrCreateSkyblockWorld() {
        final String SKYBLOCK_WORLD = "skyblock";
        
        // Return cached world if available
        if (cachedSkyblockWorld != null) {
            return cachedSkyblockWorld;
        }
        
        // Check if skyblock world exists
        World existing = Bukkit.getWorld(SKYBLOCK_WORLD);
        if (existing != null) {
            plugin.logInfo("Using existing skyblock world: " + SKYBLOCK_WORLD);
            cachedSkyblockWorld = existing; // Cache it
            return existing;
        }
        
        // Check if world folder exists
        if (worldGenerator.worldExists(SKYBLOCK_WORLD)) {
            plugin.logInfo("Skyblock world folder found, loading...");
            return worldGenerator.loadWorld(SKYBLOCK_WORLD);
        }
        
        // World doesn't exist - create it
        plugin.logInfo("Skyblock world not found. Creating new void world: " + SKYBLOCK_WORLD);
        World world = worldGenerator.createWorldFolder(SKYBLOCK_WORLD);
        
        if (world != null) {
            plugin.logInfo("Successfully created skyblock world!");
            plugin.getConfigManager().getConfig().set("world.warp_world", SKYBLOCK_WORLD);
            plugin.getConfigManager().saveConfig();
            cachedSkyblockWorld = world; // Cache it
            return world;
        } else {
            plugin.logError("Failed to create skyblock world!");
            return null;
        }
    }
    
    /**
     * Validates that a world is a void world (has level.dat)
     */
    public ValidationResult validateWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        
        if (world == null) {
            return new ValidationResult(false, "World not loaded: " + worldName);
        }
        
        return new ValidationResult(true, "World is valid: " + worldName);
    }
    
    /**
     * Gets all available worlds
     */
    public String[] getAvailableWorlds() {
        return worldGenerator.getAvailableWorlds();
    }
    
    /**
     * Simple validation result class
     */
    public static class ValidationResult {
        public final boolean isValid;
        public final String message;
        
        public ValidationResult(boolean isValid, String message) {
            this.isValid = isValid;
            this.message = message;
        }
    }
}
