package org.skyraid.core.util;

/**
 * Utility class to strip Minecraft color codes for clean console output
 */
public class ColorStripper {
    
    /**
     * Removes all Minecraft color codes (§x format) from text
     */
    public static String strip(String text) {
        if (text == null) {
            return null;
        }
        return text.replaceAll("§[0-9a-fk-or]", "");
    }
    
    /**
     * Converts Minecraft color codes to ANSI console colors (for terminal support)
     */
    public static String toAnsi(String text) {
        if (text == null) {
            return null;
        }
        
        // ANSI color codes
        String result = text
            .replace("§0", "\u001B[30m")  // Black
            .replace("§1", "\u001B[34m")  // Dark Blue
            .replace("§2", "\u001B[32m")  // Dark Green
            .replace("§3", "\u001B[36m")  // Dark Cyan
            .replace("§4", "\u001B[31m")  // Dark Red
            .replace("§5", "\u001B[35m")  // Dark Magenta
            .replace("§6", "\u001B[33m")  // Gold/Yellow
            .replace("§7", "\u001B[37m")  // Gray
            .replace("§8", "\u001B[90m")  // Dark Gray
            .replace("§9", "\u001B[94m")  // Blue
            .replace("§a", "\u001B[92m")  // Green
            .replace("§b", "\u001B[96m")  // Cyan
            .replace("§c", "\u001B[91m")  // Red
            .replace("§d", "\u001B[95m")  // Magenta
            .replace("§e", "\u001B[93m")  // Yellow
            .replace("§f", "\u001B[97m")  // White
            .replace("§l", "\u001B[1m")   // Bold
            .replace("§o", "\u001B[3m")   // Italic
            .replace("§n", "\u001B[4m")   // Underline
            .replace("§m", "\u001B[9m")   // Strikethrough
            .replace("§r", "\u001B[0m");  // Reset
        
        return result + "\u001B[0m";  // Reset at end
    }
}

