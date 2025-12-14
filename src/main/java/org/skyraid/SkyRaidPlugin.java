package org.skyraid;

import org.bukkit.plugin.java.JavaPlugin;
import org.skyraid.core.manager.*;
import org.skyraid.core.database.DatabaseManager;
import org.skyraid.core.command.CommandRegistry;
import org.skyraid.core.listener.*;
import org.skyraid.core.util.ConfigManager;
import org.skyraid.core.util.DebugManager;
import org.skyraid.core.util.ColorStripper;
import org.bukkit.World;

/**
 * SkyHuntCore - Premium PvP Skyblock Plugin
 * 
 * Main plugin class implementing a comprehensive raiding and forcefield system
 * for Minecraft skyblock servers.
 */
public class SkyRaidPlugin extends JavaPlugin {
    
    private static SkyRaidPlugin instance;
    private ConfigManager configManager;
    private DebugManager debugManager;
    private DatabaseManager databaseManager;
    private IslandManager islandManager;
    private TeamManager teamManager;
    private ForcefieldManager forcefieldManager;
    private BorderManager borderManager;
    private org.skyraid.core.listener.ChatConfirmationListener chatConfirmationListener;
    private org.skyraid.core.gui.SetupWizardGUI setupWizard;
    private org.skyraid.core.util.WorldValidator worldValidator;
    private org.skyraid.core.util.SchematicLoader schematicLoader;
    
    @Override
    public void onEnable() {
        instance = this;
        
        try {
            // Initialize core systems
            this.configManager = new ConfigManager(this);
            this.debugManager = new DebugManager(this);
            this.databaseManager = new DatabaseManager(this);
            this.worldValidator = new org.skyraid.core.util.WorldValidator(this);
            
            logInfo("Initializing managers...");
            
            // Ensure skyblock world exists and is created if missing
            logInfo("Checking for skyblock world...");
            World skyblockWorld = worldValidator.getOrCreateSkyblockWorld();
            
            if (skyblockWorld == null) {
                logError("Failed to create/load skyblock world! Plugin cannot continue.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            logInfo("Skyblock world ready: " + skyblockWorld.getName());
            
            // Initialize schematic loader
            this.schematicLoader = new org.skyraid.core.util.SchematicLoader(this);
            
            // Initialize managers
            this.islandManager = new IslandManager(this);
            this.teamManager = new TeamManager(this);
            this.forcefieldManager = new ForcefieldManager(this);
            this.borderManager = new BorderManager(this);
            
            // CRITICAL: Load all teams and players from database
            logInfo("Loading existing data from database...");
            this.teamManager.loadAllFromDatabase();
            
            // Initialize listeners that need to be accessible
            this.chatConfirmationListener = new org.skyraid.core.listener.ChatConfirmationListener(this);
            
            // Initialize GUI systems
            this.setupWizard = new org.skyraid.core.gui.SetupWizardGUI(this);
            
            // Register event listeners
            logInfo("Registering event listeners...");
            registerListeners();
            
            // Register commands
            logInfo("Registering commands...");
            new org.skyraid.core.command.CommandRegistry(this);
            
            logInfo("SkyHuntCore v" + getDescription().getVersion() + " enabled successfully!");
            logInfo("Running on Paper 1.21.1 - Ready for raiding!");
            
        } catch (Exception e) {
            logError("Failed to enable SkyHuntCore plugin!");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        try {
            if (databaseManager != null) {
                databaseManager.shutdown();
            }
            if (teamManager != null) {
                teamManager.saveAllTeams();
            }
            logInfo("SkyHuntCore disabled.");
        } catch (Exception e) {
            logError("Error during plugin shutdown!");
            e.printStackTrace();
        }
    }
    
    /**
     * Registers all event listeners
     * Following MC_PROJECT_RULES.md - ALL listeners MUST be registered here!
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new org.skyraid.core.listener.GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new org.skyraid.core.listener.IslandListener(this), this);
        getServer().getPluginManager().registerEvents(new org.skyraid.core.listener.ForcefieldListener(this), this);
        getServer().getPluginManager().registerEvents(new org.skyraid.core.listener.ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new org.skyraid.core.listener.PvPListener(this), this);
        getServer().getPluginManager().registerEvents(new org.skyraid.core.listener.FirstJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new org.skyraid.core.listener.BorderListener(this), this);
        getServer().getPluginManager().registerEvents(new org.skyraid.core.listener.RespawnListener(this), this);
        getServer().getPluginManager().registerEvents(chatConfirmationListener, this);
        logInfo("All event listeners registered successfully.");
    }
    
    // Static access methods
    public static SkyRaidPlugin getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public DebugManager getDebugManager() {
        return debugManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public IslandManager getIslandManager() {
        return islandManager;
    }
    
    public TeamManager getTeamManager() {
        return teamManager;
    }
    
    public ForcefieldManager getForcefieldManager() {
        return forcefieldManager;
    }
    
    public BorderManager getBorderManager() {
        return borderManager;
    }
    
    public org.skyraid.core.listener.ChatConfirmationListener getChatConfirmationListener() {
        return chatConfirmationListener;
    }
    
    public org.skyraid.core.util.WorldValidator getWorldValidator() {
        return worldValidator;
    }
    
    public org.skyraid.core.util.SchematicLoader getSchematicLoader() {
        return schematicLoader;
    }
    
    // Utility logging methods
    public void logInfo(String message) {
        getLogger().info(ColorStripper.strip(message));
    }
    
    public void logWarning(String message) {
        getLogger().warning(ColorStripper.strip(message));
    }
    
    public void logError(String message) {
        getLogger().severe(ColorStripper.strip(message));
    }
    
    public void logDebug(String category, String message) {
        debugManager.debug(category, message);
    }
}
