package org.skyraid.core.util;

import org.bukkit.WorldBorder;

/**
 * Border colors for island boundaries
 */
public enum BorderColor {
    BLUE(0x3F76E4, "Blue"),      // Nice bright blue
    RED(0xFF0000, "Red"),         // Bright red
    GREEN(0x00FF00, "Green"),     // Bright green
    PURPLE(0x9B59B6, "Purple"),   // Nice purple
    YELLOW(0xFFD700, "Yellow"),   // Gold yellow
    OFF(-1, "Off");               // No border

    private final int color;
    private final String displayName;

    BorderColor(int color, String displayName) {
        this.color = color;
        this.displayName = displayName;
    }

    public int getColor() {
        return color;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Apply this color to a WorldBorder
     */
    public void applyToWorldBorder(WorldBorder border) {
        if (this == OFF) {
            // For OFF, we'll make border size huge (essentially invisible)
            border.setSize(100000000);
            border.setWarningDistance(0);
            border.setWarningTime(0);
        } else {
            // Set warning zones to create colored border effect
            border.setWarningDistance(15); // Shows color when within 15 blocks
            border.setWarningTime(0);
        }
    }

    public static BorderColor fromString(String name) {
        for (BorderColor color : values()) {
            if (color.name().equalsIgnoreCase(name)) {
                return color;
            }
        }
        return BLUE; // Default
    }
}



