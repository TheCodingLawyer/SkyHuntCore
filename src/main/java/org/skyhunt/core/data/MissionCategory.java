package org.skyhunt.core.data;

/**
 * Mission categories as defined in the SkyHuntCore specification.
 */
public enum MissionCategory {
    HEADHUNTING,
    MINING,
    FARMING;

    public static MissionCategory fromString(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Mission category cannot be null");
        }
        return MissionCategory.valueOf(raw.trim().toUpperCase());
    }
}




