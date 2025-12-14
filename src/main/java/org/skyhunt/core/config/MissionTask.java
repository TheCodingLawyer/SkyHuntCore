package org.skyhunt.core.config;

import org.skyhunt.core.data.MissionCategory;

/**
 * Represents a single mission requirement for a level.
 */
public class MissionTask {
    private final MissionCategory category;
    private final String typeName; // mob/block/crop material name (uppercase)
    private final long required;
    private final String rewardMessage;

    public MissionTask(MissionCategory category, String typeName, long required, String rewardMessage) {
        this.category = category;
        this.typeName = typeName.toUpperCase();
        this.required = required;
        this.rewardMessage = rewardMessage;
    }

    public MissionCategory getCategory() {
        return category;
    }

    public String getTypeName() {
        return typeName;
    }

    public long getRequired() {
        return required;
    }

    public String getRewardMessage() {
        return rewardMessage;
    }

    public String missionId(int level) {
        return "level-" + level + "-" + category.name() + "-" + typeName;
    }
}




