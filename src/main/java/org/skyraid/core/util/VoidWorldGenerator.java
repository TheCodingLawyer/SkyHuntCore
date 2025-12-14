package org.skyraid.core.util;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.block.Biome;
import org.skyraid.SkyRaidPlugin;

import java.util.Collections;
import java.util.List;

/**
 * Creates proper void worlds using custom chunk generation
 * Based on VoidGen library: https://github.com/xtkq-is-not-available/VoidGen.git
 */
public class VoidWorldGenerator {
    
    private final SkyRaidPlugin plugin;
    
    public VoidWorldGenerator(SkyRaidPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Creates a true void world (completely empty)
     */
    public World createVoidWorld(String worldName) {
        // Check if world already exists
        World existing = Bukkit.getWorld(worldName);
        if (existing != null) {
            plugin.logInfo("Void world already exists: " + worldName);
            return existing;
        }
        
        try {
            plugin.logInfo("Creating true void world: " + worldName);
            
            // Create world with custom void chunk generator
            WorldCreator wc = new WorldCreator(worldName);
            wc.generator(new VoidChunkGenerator());  // Custom generator, no need for WorldType
            wc.generateStructures(false);
            
            World world = wc.createWorld();
            
            if (world != null) {
                plugin.logInfo("Successfully created void world: " + worldName);
                plugin.logInfo("World is completely void - no terrain generation");
                return world;
            } else {
                plugin.logError("Failed to create void world: " + worldName);
                return null;
            }
            
        } catch (Exception e) {
            plugin.logError("Error creating void world: " + worldName);
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Custom chunk generator for complete void worlds
     * All chunks are completely empty (void)
     */
    public static class VoidChunkGenerator extends ChunkGenerator {
        
        @Override
        public void generateNoise(WorldInfo worldInfo, java.util.Random random, int chunkX, int chunkZ, ChunkData chunkData) {
            // Do nothing - completely void
        }
    }
}
