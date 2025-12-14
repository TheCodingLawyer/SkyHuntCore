package org.skyhunt.core.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.skyhunt.SkyHuntCorePlugin;
import org.skyhunt.core.data.MissionCategory;

import java.util.*;

/**
 * Loads SkyHuntCore configuration into typed models.
 */
public class ConfigService {

    private final SkyHuntCorePlugin plugin;
    private final Map<Integer, LevelDefinition> levels = new HashMap<>();
    private final Map<String, Double> headPrices = new HashMap<>();

    private String prefix;
    private boolean scoreboardEnabled;
    private int scoreboardInterval;
    private String scoreboardTitle;
    private List<String> scoreboardLayout;
    private int startingLevel;
    private int maxLevel;
    private int islandSpacing;
    private int islandHeight;
    private int islandSize;
    private String islandWorldName;

    public ConfigService(SkyHuntCorePlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        prefix = cfg.getString("plugin.prefix", "&8[&6SkyHunt&8]");
        scoreboardEnabled = cfg.getBoolean("plugin.scoreboard-enabled", true);
        // Legacy keys kept for backward compatibility
        scoreboardInterval = cfg.getInt("plugin.scoreboard-update-interval", cfg.getInt("scoreboard.update-interval", 20));
        scoreboardTitle = cfg.getString("scoreboard.title", "&6&lSkyHuntCore");
        scoreboardLayout = cfg.getStringList("scoreboard.layout");
        startingLevel = cfg.getInt("island.starting-level", 1);
        maxLevel = cfg.getInt("island.max-level", 10);
        islandSpacing = cfg.getInt("island.spacing", 1000);
        islandHeight = cfg.getInt("island.height", 64);
        islandSize = cfg.getInt("island.default-size", 100);
        islandWorldName = cfg.getString("island.world-name", "");

        loadLevels(cfg);
        loadHeadPrices(cfg);
    }

    private void loadLevels(FileConfiguration cfg) {
        levels.clear();
        ConfigurationSection levelSection = cfg.getConfigurationSection("levels");
        if (levelSection == null) {
            plugin.getLogger().warning("No levels defined in config.yml");
            return;
        }

        for (String key : levelSection.getKeys(false)) {
            int level;
            try {
                level = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid level key: " + key);
                continue;
            }

            ConfigurationSection levelCfg = levelSection.getConfigurationSection(key);
            if (levelCfg == null) continue;

            Map<MissionCategory, List<MissionTask>> missionMap = new EnumMap<>(MissionCategory.class);
            ConfigurationSection missionsSec = levelCfg.getConfigurationSection("missions");
            if (missionsSec != null) {
                parseCategory(missionsSec, "headhunting", MissionCategory.HEADHUNTING, missionMap, "mob");
                parseCategory(missionsSec, "mining", MissionCategory.MINING, missionMap, "block");
                parseCategory(missionsSec, "farming", MissionCategory.FARMING, missionMap, "crop");
            }

            List<String> spawners = levelCfg.getStringList("unlocks.spawners");
            String levelUpMessage = levelCfg.getString("levelup-message", "&aYou've reached Island Level " + (level + 1) + "!");

            LevelDefinition def = new LevelDefinition(level, missionMap, spawners, levelUpMessage);
            levels.put(level, def);
        }
    }

    private void parseCategory(ConfigurationSection missionsSec, String path, MissionCategory category,
                               Map<MissionCategory, List<MissionTask>> missionMap, String keyName) {
        List<MissionTask> list = new ArrayList<>();
        List<Map<?, ?>> raw = missionsSec.getMapList(path);
        for (Map<?, ?> entry : raw) {
            Object typeObj = entry.get(keyName);
            Object amountObj = entry.get("amount");
            if (typeObj == null || amountObj == null) {
                continue;
            }
            long amount = Long.parseLong(String.valueOf(amountObj));
            String typeName = String.valueOf(typeObj).toUpperCase(Locale.ROOT);
            Object rewardObj = entry.get("reward-message");
            String rewardMessage = rewardObj == null ? "" : rewardObj.toString();
            list.add(new MissionTask(category, typeName, amount, rewardMessage));
        }
        missionMap.put(category, list);
    }

    private void loadHeadPrices(FileConfiguration cfg) {
        headPrices.clear();
        ConfigurationSection section = cfg.getConfigurationSection("head-prices");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            headPrices.put(key.toUpperCase(Locale.ROOT), section.getDouble(key, 0.0));
        }
    }

    public LevelDefinition getLevelDefinition(int level) {
        return levels.get(level);
    }

    public int getStartingLevel() {
        return startingLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int getIslandSpacing() {
        return islandSpacing;
    }

    public int getIslandHeight() {
        return islandHeight;
    }

    public int getIslandSize() {
        return islandSize;
    }

    public String getIslandWorldName() {
        return islandWorldName;
    }

    public Map<String, Double> getHeadPrices() {
        return Collections.unmodifiableMap(headPrices);
    }

    public String getPrefix() {
        return prefix;
    }

    public boolean isScoreboardEnabled() {
        return scoreboardEnabled;
    }

    public int getScoreboardInterval() {
        return scoreboardInterval;
    }

    public String getScoreboardTitle() {
        return scoreboardTitle;
    }

    public List<String> getScoreboardLayout() {
        return scoreboardLayout;
    }
}

