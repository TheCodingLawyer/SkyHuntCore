package org.skyraid.core.util;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.skyraid.SkyRaidPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Creates Minecraft world folders and level.dat files programmatically
 * Eliminates Multiverse dependency - plugin handles world creation directly
 */
public class WorldFileGenerator {
    
    private final SkyRaidPlugin plugin;
    private final File worldsFolder;
    
    public WorldFileGenerator(SkyRaidPlugin plugin) {
        this.plugin = plugin;
        this.worldsFolder = Bukkit.getServer().getWorldContainer();
    }
    
    /**
     * Creates a new world folder with proper structure
     * Then loads it with VoidChunkGenerator
     */
    public World createWorldFolder(String worldName) {
        plugin.logInfo("Creating world folder: " + worldName);
        
        try {
            // Let Paper/Minecraft handle world creation completely
            // This ensures proper level.dat initialization
            
            // First check if world already exists
            File worldDir = new File(worldsFolder, worldName);
            
            if (worldDir.exists()) {
                plugin.logInfo("World folder already exists: " + worldName);
                return loadWorld(worldName);
            }
            
            plugin.logInfo("World folder doesn't exist, Paper will create it");
            
            // Load the world - Paper will automatically create directory structure
            // including the properly formatted level.dat file
            return loadWorld(worldName);
            
        } catch (Exception e) {
            plugin.logError("Error creating world folder: " + worldName);
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Creates a minimal but valid level.dat file
     * Minecraft reads this to recognize the world
     * 
     * NOTE: Removed - let Paper/Minecraft handle level.dat initialization
     */
    private boolean createLevelDat(File worldDir) {
        // Removed - Paper will create this automatically
        return true;
    }
    
    /**
     * Loads a world with our custom void chunk generator
     */
    public World loadWorld(String worldName) {
        try {
            // Check if world is already loaded
            World existing = Bukkit.getWorld(worldName);
            if (existing != null) {
                plugin.logInfo("World already loaded: " + worldName);
                return existing;
            }
            
            plugin.logInfo("Loading world: " + worldName);
            
            // Create world with custom void generator
            WorldCreator wc = new WorldCreator(worldName);
            wc.generator(new VoidWorldGenerator.VoidChunkGenerator());  // Custom generator, no need for WorldType
            wc.generateStructures(false);
            
            World world = wc.createWorld();
            
            if (world != null) {
                plugin.logInfo("Successfully loaded void world: " + worldName);
                plugin.logInfo("World is ready for island generation");
                return world;
            } else {
                plugin.logError("Failed to load world: " + worldName);
                return null;
            }
            
        } catch (Exception e) {
            plugin.logError("Error loading world: " + worldName);
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Checks if a world folder exists
     */
    public boolean worldExists(String worldName) {
        File worldDir = new File(worldsFolder, worldName);
        File levelDat = new File(worldDir, "level.dat");
        return worldDir.exists() && levelDat.exists();
    }
    
    /**
     * Gets all world folders in the server
     */
    public String[] getAvailableWorlds() {
        File[] folders = worldsFolder.listFiles(File::isDirectory);
        
        if (folders == null) {
            return new String[0];
        }
        
        java.util.List<String> worlds = new java.util.ArrayList<>();
        for (File folder : folders) {
            File levelDat = new File(folder, "level.dat");
            if (levelDat.exists()) {
                worlds.add(folder.getName());
            }
        }
        
        return worlds.toArray(new String[0]);
    }
}
