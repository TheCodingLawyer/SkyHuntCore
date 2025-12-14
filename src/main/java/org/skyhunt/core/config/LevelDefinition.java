package org.skyhunt.core.config;

import org.skyhunt.core.data.MissionCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Holds missions and unlocks for a specific island level.
 */
public class LevelDefinition {
    private final int level;
    private final Map<MissionCategory, List<MissionTask>> missions = new EnumMap<>(MissionCategory.class);
    private final List<String> unlockedSpawners;
    private final String levelUpMessage;

    public LevelDefinition(int level, Map<MissionCategory, List<MissionTask>> missions, List<String> unlockedSpawners, String levelUpMessage) {
        this.level = level;
        for (MissionCategory category : MissionCategory.values()) {
            this.missions.put(category, new ArrayList<>(missions.getOrDefault(category, Collections.emptyList())));
        }
        this.unlockedSpawners = unlockedSpawners == null ? Collections.emptyList() : new ArrayList<>(unlockedSpawners);
        this.levelUpMessage = levelUpMessage;
    }

    public int getLevel() {
        return level;
    }

    public List<MissionTask> getTasks(MissionCategory category) {
        return missions.getOrDefault(category, Collections.emptyList());
    }

    public Map<MissionCategory, List<MissionTask>> getAllTasks() {
        return Collections.unmodifiableMap(missions);
    }

    public List<String> getUnlockedSpawners() {
        return Collections.unmodifiableList(unlockedSpawners);
    }

    public String getLevelUpMessage() {
        return levelUpMessage;
    }
}




