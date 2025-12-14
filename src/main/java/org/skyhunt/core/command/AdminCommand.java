package org.skyhunt.core.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.skyhunt.core.service.IslandService;
import org.skyhunt.core.service.MissionService;
import org.skyhunt.core.database.DatabaseManager;
import org.skyhunt.core.config.ConfigService;
import org.skyhunt.core.data.IslandData;

/**
 * Admin command: /skyhunt <reload|setlevel|resetmissions|unlockhead|debug>
 */
public class AdminCommand implements CommandExecutor {

    private final ConfigService config;
    private final IslandService islands;
    private final MissionService missions;
    private final DatabaseManager database;

    public AdminCommand(ConfigService config, IslandService islands, MissionService missions, DatabaseManager database) {
        this.config = config;
        this.islands = islands;
        this.missions = missions;
        this.database = database;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("/skyhunt <reload|setlevel|resetmissions|unlockhead|debug|addlevel|resetlevel|givexp>");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload" -> {
                config.reload();
                sender.sendMessage("Config reloaded.");
                return true;
            }
            case "setlevel" -> {
                if (args.length < 3) {
                    sender.sendMessage("Usage: /skyhunt setlevel <player> <level>");
                    return true;
                }
                Player target = sender.getServer().getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("Player not found.");
                    return true;
                }
                int level = Integer.parseInt(args[2]);
                IslandData data = islands.getIsland(target);
                data.setIslandLevel(level);
                islands.save(data);
                missions.resetProgress(target.getUniqueId());
                sender.sendMessage("Set " + target.getName() + " level to " + level);
                return true;
            }
            case "resetmissions" -> {
                if (args.length < 2) {
                    sender.sendMessage("Usage: /skyhunt resetmissions <player>");
                    return true;
                }
                Player target = sender.getServer().getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("Player not found.");
                    return true;
                }
                missions.resetProgress(target.getUniqueId());
                sender.sendMessage("Reset missions for " + target.getName());
                return true;
            }
            case "addlevel" -> {
                if (args.length < 3) {
                    sender.sendMessage("Usage: /skyhunt addlevel <player> <amount>");
                    return true;
                }
                Player target = sender.getServer().getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("Player not found.");
                    return true;
                }
                int amount = Integer.parseInt(args[2]);
                var data = islands.getIsland(target);
                int newLevel = Math.min(config.getMaxLevel(), data.getIslandLevel() + amount);
                data.setIslandLevel(newLevel);
                islands.save(data);
                missions.resetProgress(target.getUniqueId());
                sender.sendMessage("Added " + amount + " levels to " + target.getName() + " (now " + newLevel + ")");
                return true;
            }
            case "resetlevel" -> {
                if (args.length < 2) {
                    sender.sendMessage("Usage: /skyhunt resetlevel <player>");
                    return true;
                }
                Player target = sender.getServer().getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("Player not found.");
                    return true;
                }
                var data = islands.getIsland(target);
                data.setIslandLevel(config.getStartingLevel());
                islands.save(data);
                missions.resetProgress(target.getUniqueId());
                sender.sendMessage("Reset " + target.getName() + " to level " + config.getStartingLevel());
                return true;
            }
            case "givexp" -> {
                if (args.length < 3) {
                    sender.sendMessage("Usage: /skyhunt givexp <player> <amount>");
                    return true;
                }
                Player target = sender.getServer().getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("Player not found.");
                    return true;
                }
                int xp = Integer.parseInt(args[2]);
                target.giveExp(xp);
                sender.sendMessage("Gave " + xp + " xp to " + target.getName());
                return true;
            }
            case "unlockhead" -> {
                if (args.length < 3) {
                    sender.sendMessage("Usage: /skyhunt unlockhead <player> <mob>");
                    return true;
                }
                Player target = sender.getServer().getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("Player not found.");
                    return true;
                }
                database.unlockHead(target.getUniqueId(), args[2]);
                sender.sendMessage("Unlocked head " + args[2] + " for " + target.getName());
                return true;
            }
            case "debug" -> {
                if (args.length < 2) {
                    sender.sendMessage("Usage: /skyhunt debug <player>");
                    return true;
                }
                Player target = sender.getServer().getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("Player not found.");
                    return true;
                }
                IslandData data = islands.getIsland(target);
                sender.sendMessage("Player: " + target.getName() + " Level: " + data.getIslandLevel());
                return true;
            }
            default -> {
                sender.sendMessage("/skyhunt <reload|setlevel|resetmissions|unlockhead|debug>");
                return true;
            }
        }
    }
}

