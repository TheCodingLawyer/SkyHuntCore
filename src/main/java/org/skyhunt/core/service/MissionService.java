package org.skyhunt.core.service;

import org.bukkit.entity.Player;
import org.skyhunt.SkyHuntCorePlugin;
import org.skyhunt.core.config.LevelDefinition;
import org.skyhunt.core.config.MissionTask;
import org.skyhunt.core.data.IslandData;
import org.skyhunt.core.data.MissionCategory;
import org.skyhunt.core.data.MissionProgress;
import org.skyhunt.core.database.DatabaseManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles mission progress tracking and completion checks.
 */
public class MissionService {

    private final SkyHuntCorePlugin plugin;
    private final DatabaseManager database;
    private final IslandService islandService;
    private final Map<UUID, Map<String, MissionProgress>> cache = new ConcurrentHashMap<>();

    public MissionService(SkyHuntCorePlugin plugin, DatabaseManager database, IslandService islandService) {
        this.plugin = plugin;
        this.database = database;
        this.islandService = islandService;
    }

    public Map<String, MissionProgress> getProgressMap(UUID playerId) {
        return cache.computeIfAbsent(playerId, database::loadMissionProgress);
    }

    public void resetProgress(UUID playerId) {
        cache.remove(playerId);
        database.resetMissionProgress(playerId);
    }

    public void handleKill(Player player, String mobType) {
        IslandData island = islandService.getIsland(player);
        LevelDefinition def = islandService.getLevelDef(island);
        if (def == null) return;

        updateCategory(player, island, def, MissionCategory.HEADHUNTING, mobType.toUpperCase(Locale.ROOT), 1);
    }

    public void handleBlock(Player player, String material) {
        IslandData island = islandService.getIsland(player);
        LevelDefinition def = islandService.getLevelDef(island);
        if (def == null) return;

        String mat = material.toUpperCase(Locale.ROOT);
        updateCategory(player, island, def, MissionCategory.MINING, mat, 1);
        updateCategory(player, island, def, MissionCategory.FARMING, mat, 1);
    }

    private void updateCategory(Player player, IslandData island, LevelDefinition def, MissionCategory category, String typeName, long amount) {
        for (MissionTask task : def.getTasks(category)) {
            if (!task.getTypeName().equalsIgnoreCase(typeName)) continue;
            String missionId = task.missionId(def.getLevel());
            MissionProgress progress = getProgressMap(player.getUniqueId()).get(missionId);
            if (progress == null) {
                progress = new MissionProgress(player.getUniqueId(), category, missionId, 0, false);
            }
            if (progress.isCompleted()) {
                continue;
            }

            progress.setProgress(progress.getProgress() + amount);
            if (progress.getProgress() >= task.getRequired()) {
                progress.setCompleted(true);
                if (!task.getRewardMessage().isEmpty()) {
                    player.sendMessage(color(task.getRewardMessage()));
                }
            }

            database.saveMissionProgress(progress);
            cache.get(player.getUniqueId()).put(missionId, progress);
        }

        checkAllComplete(player, island, def);
    }

    private void checkAllComplete(Player player, IslandData island, LevelDefinition def) {
        if (def == null) return;
        if (areAllMissionsComplete(player.getUniqueId(), def)) {
            String msg = plugin.getConfig().getString("messages.all-missions-complete", "&a&lAll missions completed! Use /levelup to advance!");
            player.sendMessage(color(msg));
        }
    }

    public boolean areAllMissionsComplete(UUID playerId, LevelDefinition def) {
        Map<String, MissionProgress> map = getProgressMap(playerId);
        for (MissionCategory cat : MissionCategory.values()) {
            for (MissionTask task : def.getTasks(cat)) {
                String missionId = task.missionId(def.getLevel());
                MissionProgress prog = map.get(missionId);
                if (prog == null || !prog.isCompleted()) {
                    return false;
                }
            }
        }
        return true;
    }

    private String color(String msg) {
        return msg.replace("&", "§");
    }
}




