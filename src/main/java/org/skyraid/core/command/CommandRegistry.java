package org.skyraid.core.command;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.skyraid.SkyRaidPlugin;
import org.skyraid.core.command.admin.AdminCommand;
import org.skyraid.core.command.user.UserCommand;

/**
 * Registers all plugin commands
 */
public class CommandRegistry {
    
    private final SkyRaidPlugin plugin;
    
    public CommandRegistry(SkyRaidPlugin plugin) {
        this.plugin = plugin;
        registerCommands();
    }
    
    /**
     * Registers all commands
     */
    private void registerCommands() {
        // Admin commands
        CommandExecutor adminCommand = new AdminCommand(plugin);
        TabCompleter adminCompleter = (TabCompleter) adminCommand;
        plugin.getCommand("skyhunt").setExecutor(adminCommand);
        plugin.getCommand("skyhunt").setTabCompleter(adminCompleter);
        
        // User commands
        CommandExecutor userCommand = new UserCommand(plugin);
        TabCompleter userCompleter = (TabCompleter) userCommand;
        plugin.getCommand("is").setExecutor(userCommand);
        plugin.getCommand("is").setTabCompleter(userCompleter);
        
        plugin.logInfo("Commands registered successfully.");
    }
}

