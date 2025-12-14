package org.skyraid.core.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.skyraid.SkyRaidPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages configuration loading, saving, and hot-reload functionality
 */
public class ConfigManager {
    
    private final SkyRaidPlugin plugin;
    private FileConfiguration config;
    private File configFile;
    private final Map<String, Boolean> debugFlags = new HashMap<>();
    
    public ConfigManager(SkyRaidPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    /**
     * Loads the configuration from file
     */
    public void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        loadDebugFlags();
        plugin.logInfo("Configuration loaded successfully.");
    }
    
    /**
     * Reloads the configuration (hot-reload)
     */
    public void reloadConfig() {
        loadConfig();
        plugin.logInfo("Configuration reloaded.");
    }
    
    /**
     * Saves the configuration to file
     */
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.logError("Failed to save configuration!");
            e.printStackTrace();
        }
    }
    
    /**
     * Gets a string value from config
     */
    public String getString(String path, String defaultValue) {
        return config.getString(path, defaultValue);
    }
    
    /**
     * Gets an integer value from config
     */
    public int getInt(String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }
    
    /**
     * Gets a double value from config
     */
    public double getDouble(String path, double defaultValue) {
        return config.getDouble(path, defaultValue);
    }
    
    /**
     * Gets a boolean value from config
     */
    public boolean getBoolean(String path, boolean defaultValue) {
        return config.getBoolean(path, defaultValue);
    }
    
    /**
     * Gets a long value from config
     */
    public long getLong(String path, long defaultValue) {
        return config.getLong(path, defaultValue);
    }
    
    /**
     * Gets a list of strings from config
     */
    @SuppressWarnings("unchecked")
    public java.util.List<String> getStringList(String path) {
        return (java.util.List<String>) config.getList(path, new java.util.ArrayList<>());
    }
    
    /**
     * Loads debug flags from config
     */
    private void loadDebugFlags() {
        debugFlags.clear();
        if (config.contains("debug")) {
            for (String key : config.getConfigurationSection("debug").getKeys(false)) {
                debugFlags.put(key, config.getBoolean("debug." + key, false));
            }
        }
    }
    
    /**
     * Checks if a debug flag is enabled
     */
    public boolean isDebugEnabled(String category) {
        return debugFlags.getOrDefault(category, false);
    }
    
    /**
     * Sets a debug flag
     */
    public void setDebugFlag(String category, boolean enabled) {
        debugFlags.put(category, enabled);
        config.set("debug." + category, enabled);
        saveConfig();
    }
    
    /**
     * Gets the raw FileConfiguration object
     */
    public FileConfiguration getConfig() {
        return config;
    }
}

