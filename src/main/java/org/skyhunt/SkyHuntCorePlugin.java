package org.skyhunt;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.skyhunt.core.command.AdminCommand;
import org.skyhunt.core.command.IslandCommand;
import org.skyhunt.core.command.LevelUpCommand;
import org.skyhunt.core.command.IslandTabCompleter;
import org.skyhunt.core.command.AdminTabCompleter;
import org.skyhunt.core.config.ConfigService;
import org.skyhunt.core.database.DatabaseManager;
import org.skyhunt.core.listener.BlockListener;
import org.skyhunt.core.listener.HeadSellListener;
import org.skyhunt.core.listener.JoinListener;
import org.skyhunt.core.listener.KillListener;
import org.skyhunt.core.listener.QuitListener;
import org.skyhunt.core.listener.VoidProtectionListener;
import org.skyhunt.core.gui.GUIListener;
import org.skyhunt.core.gui.GuiManager;
import org.skyhunt.core.scoreboard.ScoreboardManager;
import org.skyhunt.core.service.HeadService;
import org.skyhunt.core.service.IslandService;
import org.skyhunt.core.service.MissionService;

/**
 * SkyHuntCore main plugin entry implementing headhunting progression.
 */
public class SkyHuntCorePlugin extends JavaPlugin {

    private static SkyHuntCorePlugin instance;
    private Economy economy;

    // Services
    private ConfigService configService;
    private DatabaseManager database;
    private IslandService islandService;
    private MissionService missionService;
    private HeadService headService;
    private GuiManager guiManager;
    private ScoreboardManager scoreboardManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        if (!setupEconomy()) {
            getLogger().severe("Vault not found or no economy provider. Disabling SkyHuntCore.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.configService = new ConfigService(this);
        this.database = new DatabaseManager(this);
        this.islandService = new IslandService(this, database, configService);
        this.missionService = new MissionService(this, database, islandService);
        this.headService = new HeadService(this, configService.getHeadPrices());
        this.guiManager = new GuiManager(this, islandService, missionService, headService);
        this.scoreboardManager = new ScoreboardManager(this, configService, islandService, missionService);

        registerCommands();
        registerListeners();
        scoreboardManager.start();

        getLogger().info("SkyHuntCore enabled. Economy provider: " + economy.getName());
    }

    @Override
    public void onDisable() {
        instance = null;
        economy = null;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    private void registerCommands() {
        if (getCommand("island") != null) {
            getCommand("island").setExecutor(new IslandCommand(islandService, missionService, configService, guiManager));
            getCommand("island").setTabCompleter(new IslandTabCompleter());
        }
        if (getCommand("levelup") != null) {
            LevelUpCommand lvl = new LevelUpCommand(islandService, missionService);
            getCommand("levelup").setExecutor(lvl);
            if (getCommand("rankup") != null) {
                getCommand("rankup").setExecutor(lvl);
            }
        }
        if (getCommand("skyhunt") != null) {
            getCommand("skyhunt").setExecutor(new AdminCommand(configService, islandService, missionService, database));
            getCommand("skyhunt").setTabCompleter(new AdminTabCompleter());
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new JoinListener(islandService), this);
        getServer().getPluginManager().registerEvents(new QuitListener(scoreboardManager), this);
        getServer().getPluginManager().registerEvents(new KillListener(missionService, headService, database), this);
        getServer().getPluginManager().registerEvents(new BlockListener(missionService), this);
        getServer().getPluginManager().registerEvents(new HeadSellListener(headService, database), this);
        getServer().getPluginManager().registerEvents(new VoidProtectionListener(islandService), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
    }

    public static SkyHuntCorePlugin getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return economy;
    }

    public ConfigService getConfigService() {
        return configService;
    }

    public IslandService getIslandService() {
        return islandService;
    }

    public MissionService getMissionService() {
        return missionService;
    }

    public HeadService getHeadService() {
        return headService;
    }

    public DatabaseManager getDatabase() {
        return database;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }
}

