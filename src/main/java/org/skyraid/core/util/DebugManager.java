package org.skyraid.core.util;

import org.skyraid.SkyRaidPlugin;

/**
 * Manages debug logging with category-based filtering
 */
public class DebugManager {
    
    private final SkyRaidPlugin plugin;
    
    public DebugManager(SkyRaidPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Logs a debug message if the category is enabled
     */
    public void debug(String category, String message) {
        if (plugin.getConfigManager().isDebugEnabled(category)) {
            plugin.logInfo("§7[DEBUG:" + category + "]§r " + message);
        }
    }
    
    /**
     * Logs a debug message with additional data
     */
    public void debug(String category, String message, Object... data) {
        if (plugin.getConfigManager().isDebugEnabled(category)) {
            String formatted = String.format(message, data);
            plugin.logInfo("§7[DEBUG:" + category + "]§r " + formatted);
        }
    }
    
    /**
     * Logs a warning-level debug message
     */
    public void debugWarn(String category, String message) {
        if (plugin.getConfigManager().isDebugEnabled(category)) {
            plugin.logWarning("§e[DEBUG:" + category + "]§r " + message);
        }
    }
    
    /**
     * Logs an error-level debug message
     */
    public void debugError(String category, String message, Exception e) {
        if (plugin.getConfigManager().isDebugEnabled(category)) {
            plugin.logError("§c[DEBUG:" + category + "]§r " + message);
            e.printStackTrace();
        }
    }
}

