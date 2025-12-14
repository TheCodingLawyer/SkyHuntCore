package org.skyraid.core.command.admin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.skyraid.SkyRaidPlugin;
import org.skyraid.core.data.TeamData;

import java.util.*;

/**
 * Handles admin commands (/skyhunt)
 */
public class AdminCommand implements CommandExecutor, TabCompleter {
    
    private final SkyRaidPlugin plugin;
    
    public AdminCommand(SkyRaidPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("skyraid.admin")) {
            sender.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.no_permission",
                "§cYou don't have permission to use this command."
            ));
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subcommand = args[0].toLowerCase();
        
        switch (subcommand) {
            case "delete":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /skyhunt delete <team_name>");
                    return true;
                }
                handleDelete(sender, args);
                break;
                
            case "reset":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /skyhunt reset <team_name>");
                    return true;
                }
                handleReset(sender, args);
                break;
                
            case "forcefield":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cOnly players can use this command.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /skyhunt forcefield <hours>");
                    return true;
                }
                handleForcefield((Player) sender, args);
                break;
                
            case "reload":
                plugin.getConfigManager().reloadConfig();
                sender.sendMessage("§aConfiguration reloaded!");
                break;
                
            default:
                sendHelp(sender);
        }
        
        return true;
    }
    
    private void handleDelete(CommandSender sender, String[] args) {
        String teamName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        // Find team by name
        TeamData team = null;
        for (TeamData t : plugin.getTeamManager().getAllTeams()) {
            if (t.getTeamName().equalsIgnoreCase(teamName)) {
                team = t;
                break;
            }
        }
        
        if (team == null) {
            sender.sendMessage("§cTeam not found: " + teamName);
            return;
        }
        
        plugin.getTeamManager().deleteTeam(team);
        sender.sendMessage("§aTeam deleted: " + teamName);
    }
    
    private void handleReset(CommandSender sender, String[] args) {
        String teamName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        // Find team by name
        TeamData team = null;
        for (TeamData t : plugin.getTeamManager().getAllTeams()) {
            if (t.getTeamName().equalsIgnoreCase(teamName)) {
                team = t;
                break;
            }
        }
        
        if (team == null) {
            sender.sendMessage("§cTeam not found: " + teamName);
            return;
        }
        
        plugin.getIslandManager().resetIsland(team);
        sender.sendMessage("§aIsland reset for team: " + teamName);
    }
    
    private void handleForcefield(Player player, String[] args) {
        try {
            long hours = Long.parseLong(args[1]);
            plugin.getForcefieldManager().giveForcefieldStar(player, hours);
            player.sendMessage("§d§l✦ §r§aForcefield star given!");
            player.sendMessage("§7Duration: §e" + hours + " hours");
            player.sendMessage("§7This star can be added to crates, shops, or given to players.");
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid duration. Usage: /skyhunt forcefield <hours>");
            player.sendMessage("§7Example: /skyhunt forcefield 6");
        }
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== SkyHuntCore Admin Commands ===");
        sender.sendMessage("§e/skyhunt delete <team> §7- Delete a team");
        sender.sendMessage("§e/skyhunt reset <team> §7- Reset a team's island");
        sender.sendMessage("§e/skyhunt forcefield <hours> §7- Give yourself a forcefield item");
        sender.sendMessage("§e/skyhunt reload §7- Reload configuration");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!sender.hasPermission("skyraid.admin")) {
            return completions;
        }
        
        if (args.length == 1) {
            completions.add("delete");
            completions.add("reset");
            completions.add("forcefield");
            completions.add("reload");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("reset"))) {
            for (TeamData team : plugin.getTeamManager().getAllTeams()) {
                completions.add(team.getTeamName());
            }
        }
        
        return completions;
    }
}

