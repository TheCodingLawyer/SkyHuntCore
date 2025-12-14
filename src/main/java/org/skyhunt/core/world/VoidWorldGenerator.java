package org.skyhunt.core.world;

import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

/**
 * Simple void world generator (no terrain).
 */
public class VoidWorldGenerator extends ChunkGenerator {

    public static World createVoidWorld(String name) {
        World existing = org.bukkit.Bukkit.getWorld(name);
        if (existing != null) {
            return existing;
        }
        WorldCreator wc = new WorldCreator(name);
        wc.generator(new VoidWorldGenerator());
        wc.generateStructures(false);
        return wc.createWorld();
    }

    @Override
    public void generateNoise(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData data) {
        // leave empty for full void
    }
}

