package org.skyhunt.core.service;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.skyhunt.SkyHuntCorePlugin;
import org.skyhunt.core.config.ConfigService;
import org.skyhunt.core.config.LevelDefinition;
import org.skyhunt.core.data.IslandData;
import org.skyhunt.core.database.DatabaseManager;
import org.skyhunt.core.world.IslandGenerator;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages per-owner island progression (single-owner islands).
 */
public class IslandService {

    private final SkyHuntCorePlugin plugin;
    private final DatabaseManager database;
    private final ConfigService config;
    private final IslandGenerator generator;
    private final Map<UUID, IslandData> cache = new ConcurrentHashMap<>();

    public IslandService(SkyHuntCorePlugin plugin, DatabaseManager database, ConfigService config) {
        this.plugin = plugin;
        this.database = database;
        this.config = config;
        this.generator = new IslandGenerator(plugin, config);
    }

    public IslandData getIsland(Player player) {
        return getIsland(player.getUniqueId());
    }

    public IslandData getIsland(UUID ownerId) {
        return cache.computeIfAbsent(ownerId, id -> database.loadIsland(id, config.getStartingLevel()));
    }

    public void save(IslandData island) {
        database.saveIsland(island);
        cache.put(island.getOwnerId(), island);
    }

    public LevelDefinition getLevelDef(IslandData island) {
        return config.getLevelDefinition(island.getIslandLevel());
    }

    public boolean canLevelUp(IslandData island, boolean allMissionsComplete) {
        return allMissionsComplete && island.getIslandLevel() < config.getMaxLevel();
    }

    public boolean levelUp(IslandData island) {
        if (island.getIslandLevel() >= config.getMaxLevel()) {
            return false;
        }
        island.setIslandLevel(island.getIslandLevel() + 1);
        save(island);
        return true;
    }

    public void ensureIsland(Player player) {
        IslandData data = getIsland(player);
        if (data.getIslandX() != 0 || data.getIslandZ() != 0) {
            return; // already has an island
        }

        generator.createIsland(data).thenRun(() -> {
            save(data);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                Location home = getHomeLocation(data);
                if (home != null) {
                    home.getChunk().load();
                    player.teleport(home);
                }
                player.sendMessage("§aIsland created!");
            });
        }).exceptionally(ex -> {
            plugin.getLogger().severe("Failed to create island for " + player.getName() + ": " + ex.getMessage());
            return null;
        });
    }

    public IslandGenerator getGenerator() {
        return generator;
    }

    public Location getHomeLocation(IslandData data) {
        if (generator.getWorld() == null) return null;
        return new Location(generator.getWorld(), data.getHomeX() + 0.5, data.getHomeY(), data.getHomeZ() + 0.5, data.getHomeYaw(), data.getHomePitch());
    }

    public void teleportHome(Player player, IslandData data) {
        Location home = getHomeLocation(data);
        if (home == null) {
            player.sendMessage("§cIsland world is not available.");
            return;
        }
        home.getChunk().load();
        Location safe = findSafeLocation(home);
        player.teleport(safe);
    }

    private Location findSafeLocation(Location base) {
        Location loc = base.clone();
        // Ensure a solid block under feet; if void, place a temporary block
        if (loc.getBlockY() < generator.getWorld().getMinHeight() + 1) {
            loc.setY(generator.getWorld().getMinHeight() + 2);
        }
        int baseX = loc.getBlockX();
        int baseY = loc.getBlockY();
        int baseZ = loc.getBlockZ();

        // Search a small cube around the target for a safe spot
        for (int dy = 0; dy <= 6; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    int y = baseY + dy;
                    int x = baseX + dx;
                    int z = baseZ + dz;
                    if (isSafe(x, y, z)) {
                        return new Location(generator.getWorld(), x + 0.5, y, z + 0.5, loc.getYaw(), loc.getPitch());
                    }
                }
            }
        }
        return loc;
    }

    private boolean isSafe(int x, int y, int z) {
        var w = generator.getWorld();
        var below = w.getBlockAt(x, y - 1, z).getType();
        var feet = w.getBlockAt(x, y, z).getType();
        var head = w.getBlockAt(x, y + 1, z).getType();
        return below.isSolid() && feet.isAir() && head.isAir();
    }
}

