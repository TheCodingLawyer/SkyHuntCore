package org.skyhunt.core.world;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.skyhunt.SkyHuntCorePlugin;
import org.skyhunt.core.config.ConfigService;
import org.skyhunt.core.data.IslandData;
import org.skyhunt.core.world.schematic.SchematicPaster;
import org.skyhunt.core.world.schematic.WorldEditSchematicPaster;
import org.skyhunt.core.world.schematic.NoopSchematicPaster;
import org.skyhunt.core.world.VoidWorldGenerator;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Generates islands using a square-spiral spacing algorithm and schematic fallback.
 */
public class IslandGenerator {

    private final SkyHuntCorePlugin plugin;
    private final ConfigService config;
    private final SchematicPaster schematic;
    private final Set<String> usedCoords = new HashSet<>();
    private final World world;
    private final int spacing;
    private final int height;
    private final int islandSize;

    public IslandGenerator(SkyHuntCorePlugin plugin, ConfigService config) {
        this.plugin = plugin;
        this.config = config;
        this.spacing = config.getIslandSpacing();
        this.height = config.getIslandHeight();
        this.islandSize = config.getIslandSize();
        this.world = resolveWorld();
        this.schematic = resolveSchematicPaster();
    }

    private SchematicPaster resolveSchematicPaster() {
        plugin.getLogger().info("═══════════════════════════════════════");
        plugin.getLogger().info("CHECKING FOR WORLDEDIT/FAWE...");
        try {
            boolean wePresent = plugin.getServer().getPluginManager().getPlugin("WorldEdit") != null;
            boolean fawePresent = plugin.getServer().getPluginManager().getPlugin("FastAsyncWorldEdit") != null;
            plugin.getLogger().info("WorldEdit present: " + wePresent);
            plugin.getLogger().info("FAWE present: " + fawePresent);
            
            if (wePresent || fawePresent) {
                plugin.getLogger().info("✓ WorldEdit API detected, initializing schematic paster...");
                SchematicPaster paster = new WorldEditSchematicPaster(plugin);
                if (paster.isLoaded()) {
                    plugin.getLogger().info("✓ Schematic loaded successfully!");
                } else {
                    plugin.getLogger().warning("✗ Schematic failed to load, will use fallback");
                }
                plugin.getLogger().info("═══════════════════════════════════════");
                return paster;
            }
        } catch (NoClassDefFoundError | Exception e) {
            plugin.getLogger().warning("✗ WorldEdit check failed: " + e.getMessage());
            e.printStackTrace();
        }
        plugin.getLogger().warning("✗ WorldEdit not found, using fallback island generation.");
        plugin.getLogger().info("═══════════════════════════════════════");
        return new NoopSchematicPaster();
    }

    private World resolveWorld() {
        String name = config.getIslandWorldName();
        if (name == null || name.trim().isEmpty()) {
            name = "skyhunt";
        }
        World w = VoidWorldGenerator.createVoidWorld(name);
        if (w == null && !Bukkit.getWorlds().isEmpty()) {
            return Bukkit.getWorlds().get(0);
        }
        return w;
    }

    public World getWorld() {
        return world;
    }

    public CompletableFuture<Void> createIsland(IslandData island) {
        return CompletableFuture.runAsync(() -> {
            int[] pos = findNextPosition();
            island.setIslandX(pos[0]);
            island.setIslandZ(pos[1]);
            island.setHomeX(pos[0]);
            island.setHomeY(height + 5);
            island.setHomeZ(pos[1]);
            island.setHomeYaw(0f);
            island.setHomePitch(0f);

            Bukkit.getScheduler().runTask(plugin, () -> pasteOrFallback(pos[0], pos[1]));
        });
    }

    private int[] findNextPosition() {
        int index = usedCoords.size() + 1;
        int[] pos = spiral(index);
        usedCoords.add(pos[0] + "," + pos[1]);
        return pos;
    }

    private int[] spiral(int id) {
        if (id == 1) return new int[]{0, 0};
        int position = id - 1;
        int radius = (int) (Math.floor((Math.sqrt(position) - 1) / 2) + 1);
        int diameter = radius * 2;
        int perimeter = diameter * 4;
        int lastCompletePosition = (perimeter * (radius - 1)) / 2;
        int currentIndex = (position - lastCompletePosition) % perimeter;

        int x, z;
        switch (currentIndex / diameter) {
            case 0 -> {
                x = currentIndex - radius;
                z = -radius;
            }
            case 1 -> {
                x = radius;
                z = (currentIndex % diameter) - radius;
            }
            case 2 -> {
                x = radius - (currentIndex % diameter);
                z = radius;
            }
            case 3 -> {
                x = -radius;
                z = radius - (currentIndex % diameter);
            }
            default -> throw new IllegalStateException("Invalid spiral index");
        }
        return new int[]{x * spacing, z * spacing};
    }

    private void pasteOrFallback(int centerX, int centerZ) {
        if (world == null) {
            plugin.getLogger().severe("No world available for island generation.");
            return;
        }
        Location pasteLocation = new Location(world, centerX, height, centerZ);
        if (schematic.isLoaded()) {
            plugin.getLogger().info("═══════════════════════════════════════");
            plugin.getLogger().info("PASTING SCHEMATIC at " + centerX + "," + height + "," + centerZ);
            plugin.getLogger().info("World: " + world.getName());
            schematic.paste(pasteLocation);
            plugin.getLogger().info("═══════════════════════════════════════");
        } else {
            plugin.getLogger().warning("═══════════════════════════════════════");
            plugin.getLogger().warning("NO SCHEMATIC LOADED - Using fallback!");
            plugin.getLogger().warning("Creating " + islandSize + "x" + islandSize + " platform");
            plugin.getLogger().warning("At: " + centerX + "," + height + "," + centerZ);
            plugin.getLogger().warning("═══════════════════════════════════════");
            generateFallback(centerX, centerZ);
        }
    }

    private void generateFallback(int centerX, int centerZ) {
        int baseY = height;
        int size = islandSize / 2;
        for (int x = centerX - size; x <= centerX + size; x++) {
            for (int z = centerZ - size; z <= centerZ + size; z++) {
                world.getBlockAt(x, baseY - 1, z).setType(Material.BEDROCK);
                world.getBlockAt(x, baseY, z).setType(Material.STONE);
                world.getBlockAt(x, baseY + 1, z).setType(Material.DIRT);
                world.getBlockAt(x, baseY + 2, z).setType(Material.GRASS_BLOCK);
            }
        }
        plugin.getLogger().info("Generated fallback island at " + centerX + "," + centerZ);
    }
}

