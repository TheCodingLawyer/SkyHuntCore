package org.skyraid.core.command.user;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.skyraid.SkyRaidPlugin;
import org.skyraid.core.data.TeamData;

import java.util.*;

/**
 * Handles user commands (/is)
 */
public class UserCommand implements CommandExecutor, TabCompleter {
    
    private final SkyRaidPlugin plugin;
    private final Map<UUID, Long> deleteConfirmationTimestamps = new HashMap<>();
    private final Map<UUID, Long> resetConfirmationTimestamps = new HashMap<>();
    
    public UserCommand(SkyRaidPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command is for players only!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Handle subcommands
        if (args.length == 0) {
            // Check if player is on their island - open GUI if yes, help if no
            org.skyraid.core.data.PlayerData playerData = plugin.getTeamManager().getPlayerData(player.getUniqueId());
            
            if (playerData != null && playerData.getCurrentTeamId() != null) {
                org.skyraid.core.data.TeamData team = plugin.getTeamManager().getTeam(playerData.getCurrentTeamId());
                
                if (team != null) {
                    // Open Island Management GUI
                    org.skyraid.core.gui.IslandMainGUI mainGUI = new org.skyraid.core.gui.IslandMainGUI(plugin, player, team);
                    mainGUI.open();
                    return true;
                }
            }
            
            // No island - show help
            sendHelp(player);
            return true;
        }
        
        String subcommand = args[0].toLowerCase();
        
        switch (subcommand) {
            case "new":
                handleNew(player, args);
                break;
            case "accept":
                handleAccept(player, args);
                break;
            case "home":
                handleHome(player);
                break;
            case "debug":
                handleDebug(player);
                break;
            case "leave":
                handleLeave(player);
                break;
            case "view":
                handleView(player, args);
                break;
            case "top":
                handleTop(player);
                break;
            case "chat":
                handleChat(player);
                break;
            case "forcefield":
                handleForcefield(player);
                break;
            case "delete":
                handleDelete(player, args);
                break;
            case "reset":
                handleReset(player, args);
                break;
            case "sethome":
                handleSetHome(player);
                break;
            case "invite":
                if (args.length < 2) {
                    player.sendMessage("Usage: /is invite <player>");
                    return true;
                }
                handleInvite(player, args[1]);
                break;
            case "kick":
                if (args.length < 2) {
                    player.sendMessage("Usage: /is kick <player>");
                    return true;
                }
                handleKick(player, args[1]);
                break;
            case "promote":
                if (args.length < 2) {
                    player.sendMessage("Usage: /is promote <player>");
                    return true;
                }
                handlePromote(player, args[1]);
                break;
            case "demote":
                if (args.length < 2) {
                    player.sendMessage("Usage: /is demote <player>");
                    return true;
                }
                handleDemote(player, args[1]);
                break;
            case "endforcefield":
                handleEndForcefield(player);
                break;
            case "help":
                sendHelp(player);
                break;
            default:
                player.sendMessage("§cUnknown command. Use §e/is help§c for help.");
                break;
        }
        
        return true;
    }
    
    private void handleNew(Player player, String[] args) {
        if (!player.hasPermission("skyraid.user.new")) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.no_permission",
                "§cYou don't have permission for this."
            ));
            return;
        }
        
        // Check if warp world is valid void world
        String warpWorldName = plugin.getConfigManager().getString("world.warp_world", "skyblock");
        org.skyraid.core.util.WorldValidator.ValidationResult validation = 
            plugin.getWorldValidator().validateWorld(warpWorldName);
        
        if (!validation.isValid) {
            if (player.hasPermission("skyraid.admin")) {
                player.sendMessage("§cError: " + validation.message);
                player.sendMessage("§eUse §f/skyhunt setup §eto auto-create the skyblock world.");
                return;
            } else {
                player.sendMessage("§cServer is not ready for island creation.");
                player.sendMessage("§eAn admin needs to configure the world first.");
                return;
            }
        }
        
        TeamData existing = plugin.getTeamManager().getPlayerTeam(player);
        if (existing != null) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.already_in_team",
                "§cYou already have an island! Use §e/is home§c to go there."
            ));
            return;
        }
        
        // Open the beautiful mode selection GUI
        org.skyraid.core.gui.IslandModeSelectionGUI modeGUI = new org.skyraid.core.gui.IslandModeSelectionGUI(plugin, player);
        modeGUI.open();
    }
    
    private void handleAccept(Player player, String[] args) {
        if (!player.hasPermission("skyraid.user.accept")) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.no_permission",
                "§cYou don't have permission."
            ));
            return;
        }
        
        // TODO: Implement invite acceptance
        player.sendMessage("§cNo pending invites.");
    }
    
    private void handleHome(Player player) {
        if (!player.hasPermission("skyraid.user.home")) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.no_permission",
                "§cYou don't have permission."
            ));
            return;
        }
        
        TeamData team = plugin.getTeamManager().getPlayerTeam(player);
        if (team == null) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.not_in_island",
                "§cYou are not in an island team."
            ));
            return;
        }
        
        // Use IridiumSkyblock-style safe location finding
        Location safeHome = plugin.getIslandManager().getSafeHomeLocation(team);
        
        if (safeHome == null) {
            player.sendMessage("§cError: Could not find a safe location to teleport to.");
            player.sendMessage("§eContact an admin if this issue persists.");
            return;
        }
        
        // Reset fall distance to prevent fall damage
        player.setFallDistance(0.0F);
        
        // Teleport the player
        player.teleport(safeHome);
        player.sendMessage(plugin.getConfigManager().getString(
            "messages.success.teleported_home",
            "§aTeleported to your island!"
        ));
    }
    
    private void handleLeave(Player player) {
        if (!player.hasPermission("skyraid.user.leave")) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.no_permission",
                "§cYou don't have permission."
            ));
            return;
        }
        
        TeamData team = plugin.getTeamManager().getPlayerTeam(player);
        if (team == null) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.not_in_island",
                "§cYou are not in an island team."
            ));
            return;
        }
        
        plugin.getTeamManager().removePlayerFromTeam(player.getUniqueId(), team);
        player.sendMessage("§aYou left the island.");
    }
    
    private void handleView(Player player, String[] args) {
        if (!player.hasPermission("skyraid.user.view")) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.no_permission",
                "§cYou don't have permission."
            ));
            return;
        }
        
        if (args.length < 2) {
            TeamData team = plugin.getTeamManager().getPlayerTeam(player);
            if (team == null) {
                player.sendMessage("§cUsage: /is view <team_name>");
                return;
            }
            displayTeamInfo(player, team);
        } else {
            String teamName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            TeamData team = null;
            for (TeamData t : plugin.getTeamManager().getAllTeams()) {
                if (t.getTeamName().equalsIgnoreCase(teamName)) {
                    team = t;
                    break;
                }
            }
            
            if (team == null) {
                player.sendMessage("§cTeam not found.");
                return;
            }
            
            displayTeamInfo(player, team);
        }
    }
    
    private void displayTeamInfo(Player player, TeamData team) {
        player.sendMessage("§6=== Island Information ===");
        player.sendMessage("§eTeam: §f" + team.getTeamName());
        player.sendMessage("§eMembers: §f" + team.getMemberCount());
        player.sendMessage("§eBalance: §f" + team.getBalance());
        player.sendMessage("§eForcefield: §f" + (team.hasForcefield() ? "Active (" + team.getRemainingForcefieldHours() + "h)" : "Inactive"));
    }
    
    private void handleTop(Player player) {
        if (!player.hasPermission("skyraid.user.top")) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.no_permission",
                "§cYou don't have permission."
            ));
            return;
        }
        
        List<TeamData> teams = new ArrayList<>(plugin.getTeamManager().getAllTeams());
        teams.sort((t1, t2) -> Long.compare(t2.getBalance(), t1.getBalance()));
        
        player.sendMessage("§6=== Top Islands by Balance ===");
        int position = 1;
        for (TeamData team : teams) {
            if (position > 10) break;
            player.sendMessage("§e#" + position + " §f" + team.getTeamName() + " - §a" + team.getBalance());
            position++;
        }
    }
    
    private void handleChat(Player player) {
        if (!player.hasPermission("skyraid.user.chat")) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.no_permission",
                "§cYou don't have permission."
            ));
            return;
        }
        
        var playerData = plugin.getTeamManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            player.sendMessage("§cError: Player data not found.");
            return;
        }
        
        playerData.toggleTeamChat();
        plugin.getDatabaseManager().savePlayer(playerData);
        
        String mode = playerData.isUsingTeamChat() ? "§aTeam Chat§r" : "§aGlobal Chat§r";
        player.sendMessage("§aChat mode: " + mode);
    }
    
    private void handleForcefield(Player player) {
        if (!player.hasPermission("skyraid.user.forcefield")) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.no_permission",
                "§cYou don't have permission."
            ));
            return;
        }
        
        TeamData team = plugin.getTeamManager().getPlayerTeam(player);
        if (team == null) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.not_in_island",
                "§cYou are not in an island team."
            ));
            return;
        }
        
        if (team.hasForcefield()) {
            player.sendMessage("§aForcefield active for: §e" + team.getRemainingForcefieldHours() + " hours§a remaining");
        } else {
            player.sendMessage("§cNo active forcefield.");
        }
    }
    
    private void handleDelete(Player player, String[] args) {
        if (!player.hasPermission("skyraid.leader.delete")) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.no_permission",
                "§cYou don't have permission."
            ));
            return;
        }
        
        TeamData team = plugin.getTeamManager().getPlayerTeam(player);
        if (team == null) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.not_in_island",
                "§cYou are not in an island team."
            ));
            return;
        }
        
        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.must_be_leader",
                "§cOnly the island leader can do this."
            ));
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        long lastConfirm = deleteConfirmationTimestamps.getOrDefault(player.getUniqueId(), 0L);
        
        if (currentTime - lastConfirm > 30000) {  // 30 seconds
            deleteConfirmationTimestamps.put(player.getUniqueId(), currentTime);
            player.sendMessage("§c§lWARNING: Type the command again to confirm deletion!");
            return;
        }
        
        deleteConfirmationTimestamps.remove(player.getUniqueId());
        plugin.getTeamManager().deleteTeam(team);
        player.sendMessage("§aIsland deleted.");
    }
    
    private void handleReset(Player player, String[] args) {
        if (!player.hasPermission("skyraid.leader.reset")) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.no_permission",
                "§cYou don't have permission."
            ));
            return;
        }
        
        TeamData team = plugin.getTeamManager().getPlayerTeam(player);
        if (team == null) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.not_in_island",
                "§cYou are not in an island team."
            ));
            return;
        }
        
        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.must_be_leader",
                "§cOnly the island leader can do this."
            ));
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        long lastConfirm = resetConfirmationTimestamps.getOrDefault(player.getUniqueId(), 0L);
        
        if (currentTime - lastConfirm > 30000) {  // 30 seconds
            resetConfirmationTimestamps.put(player.getUniqueId(), currentTime);
            player.sendMessage("§c§lWARNING: Type the command again to confirm reset!");
            return;
        }
        
        resetConfirmationTimestamps.remove(player.getUniqueId());
        plugin.getIslandManager().resetIsland(team);
        player.sendMessage("§aIsland reset.");
    }
    
    private void handleSetHome(Player player) {
        if (!player.hasPermission("skyraid.leader.sethome")) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.no_permission",
                "§cYou don't have permission."
            ));
            return;
        }
        
        TeamData team = plugin.getTeamManager().getPlayerTeam(player);
        if (team == null) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.not_in_island",
                "§cYou are not in an island team."
            ));
            return;
        }
        
        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.must_be_leader",
                "§cOnly the island leader can do this."
            ));
            return;
        }
        
        org.bukkit.Location loc = player.getLocation();
        team.setHome((int) loc.getX(), (int) loc.getY(), (int) loc.getZ(), loc.getYaw(), loc.getPitch());
        plugin.getDatabaseManager().saveTeam(team);
        player.sendMessage("§aIsland home set!");
    }
    
    private void handleInvite(Player player, String targetName) {
        if (!player.hasPermission("skyraid.leader.invite")) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.no_permission",
                "§cYou don't have permission."
            ));
            return;
        }
        
        // Check if in game room (pending team)
        org.skyraid.core.data.PendingTeam pendingTeam = plugin.getTeamManager().getPendingTeam(player);
        if (pendingTeam != null) {
            // In pre-island phase
            Player target = Bukkit.getPlayer(targetName);
            if (target == null) {
                player.sendMessage("§cPlayer not found or not online.");
                return;
            }
            
            // Set pending invite and open GUI
            org.skyraid.core.data.PlayerData targetData = plugin.getTeamManager().getPlayerData(target.getUniqueId());
            if (targetData == null) {
                targetData = new org.skyraid.core.data.PlayerData(target.getUniqueId(), target.getName());
            }
            targetData.setPendingInvite(player.getUniqueId());
            plugin.getDatabaseManager().savePlayer(targetData);
            
            // Open invite GUI for target
            org.skyraid.core.gui.InviteAcceptGUI inviteGUI = new org.skyraid.core.gui.InviteAcceptGUI(
                plugin, target, player, pendingTeam.getTeamName()
            );
            inviteGUI.open();
            
            player.sendMessage("§aInvite sent to §e" + target.getName() + "§a!");
            return;
        }
        
        // Post-island phase (existing team)
        TeamData team = plugin.getTeamManager().getPlayerTeam(player);
        if (team == null) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.not_in_island",
                "§cYou are not in an island team."
            ));
            return;
        }
        
        // Check if solo mode (1 member only)
        if (team.getMemberCount() == 1) {
            player.sendMessage("§c§l⚠ You cannot invite players in Solo Mode!");
            player.sendMessage("§7Your island was created for solo play.");
            player.sendMessage("§7Create a new island to play with teams.");
            return;
        }
        
        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.must_be_leader",
                "§cOnly the island leader can do this."
            ));
            return;
        }
        
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.player_not_found",
                "§cPlayer not found."
            ));
            return;
        }
        
        plugin.getTeamManager().invitePlayerToTeam(target.getUniqueId(), team);
        player.sendMessage("§aPlayer invited!");
        target.sendMessage("§a" + player.getName() + " invited you to join their island!");
    }
    
    private void handleKick(Player player, String targetName) {
        if (!player.hasPermission("skyraid.leader.kick")) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.no_permission",
                "§cYou don't have permission."
            ));
            return;
        }
        
        TeamData team = plugin.getTeamManager().getPlayerTeam(player);
        if (team == null) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.not_in_island",
                "§cYou are not in an island team."
            ));
            return;
        }
        
        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.must_be_leader",
                "§cOnly the island leader can do this."
            ));
            return;
        }
        
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.player_not_found",
                "§cPlayer not found."
            ));
            return;
        }
        
        plugin.getTeamManager().removePlayerFromTeam(target.getUniqueId(), team);
        player.sendMessage("§aPlayer kicked!");
        target.sendMessage("§cYou were kicked from the island.");
    }
    
    private void handlePromote(Player player, String targetName) {
        if (!player.hasPermission("skyraid.leader.promote")) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.no_permission",
                "§cYou don't have permission."
            ));
            return;
        }
        
        TeamData team = plugin.getTeamManager().getPlayerTeam(player);
        if (team == null) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.not_in_island",
                "§cYou are not in an island team."
            ));
            return;
        }
        
        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.must_be_leader",
                "§cOnly the island leader can do this."
            ));
            return;
        }
        
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.player_not_found",
                "§cPlayer not found."
            ));
            return;
        }
        
        plugin.getTeamManager().promoteMember(target.getUniqueId(), team);
        player.sendMessage("§aPlayer promoted!");
    }
    
    private void handleDemote(Player player, String targetName) {
        if (!player.hasPermission("skyraid.leader.demote")) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.no_permission",
                "§cYou don't have permission."
            ));
            return;
        }
        
        TeamData team = plugin.getTeamManager().getPlayerTeam(player);
        if (team == null) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.not_in_island",
                "§cYou are not in an island team."
            ));
            return;
        }
        
        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.must_be_leader",
                "§cOnly the island leader can do this."
            ));
            return;
        }
        
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.player_not_found",
                "§cPlayer not found."
            ));
            return;
        }
        
        plugin.getTeamManager().demoteMember(target.getUniqueId(), team);
        player.sendMessage("§aPlayer demoted!");
    }
    
    private void handleEndForcefield(Player player) {
        if (!player.hasPermission("skyraid.leader.endforcefield")) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.no_permission",
                "§cYou don't have permission."
            ));
            return;
        }
        
        TeamData team = plugin.getTeamManager().getPlayerTeam(player);
        if (team == null) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.not_in_island",
                "§cYou are not in an island team."
            ));
            return;
        }
        
        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().getString(
                "messages.errors.must_be_leader",
                "§cOnly the island leader can do this."
            ));
            return;
        }
        
        plugin.getForcefieldManager().deactivateForcefield(team);
        player.sendMessage("§aForcefield ended!");
    }
    
    private void handleSetup(Player player) {
        if (!player.hasPermission("skyraid.admin")) {
            player.sendMessage("Only admins can use /is setup");
            return;
        }
        
        plugin.getIslandManager().setupIsland(player);
        player.sendMessage("§aIsland setup complete!");
    }
    
    private void handleDebug(Player player) {
        TeamData team = plugin.getTeamManager().getPlayerTeam(player);
        if (team == null) {
            player.sendMessage("§cYou are not in a team.");
            return;
        }
        
        player.sendMessage("§6=== Island Debug Info ===");
        player.sendMessage("§eIsland Position: X=" + team.getIslandX() + " Z=" + team.getIslandZ());
        player.sendMessage("§eHome Position: X=" + team.getHomeX() + " Y=" + team.getHomeY() + " Z=" + team.getHomeZ());
        player.sendMessage("§eTeam Name: " + team.getTeamName());
        player.sendMessage("§eMembers: " + team.getMemberCount());
        
        // Teleport to island center to check if blocks exist
        org.bukkit.World world = org.bukkit.Bukkit.getWorld(plugin.getConfigManager().getString("world.warp_world", "world"));
        if (world == null && !org.bukkit.Bukkit.getServer().getWorlds().isEmpty()) {
            world = org.bukkit.Bukkit.getServer().getWorlds().get(0);
        }
        
        if (world != null) {
            org.bukkit.Location centerLoc = new org.bukkit.Location(world, team.getIslandX(), 80, team.getIslandZ());
            player.teleport(centerLoc);
            player.sendMessage("§aTeleported to island center at X=" + team.getIslandX() + " Z=" + team.getIslandZ());
            
            // Check for blocks below
            int checkY = 80;
            int blocksFound = 0;
            for (int y = 80; y >= 50; y--) {
                org.bukkit.block.Block block = world.getBlockAt(team.getIslandX(), y, team.getIslandZ());
                if (block.getType() != org.bukkit.Material.AIR) {
                    blocksFound++;
                }
            }
            
            player.sendMessage("§eBlocks found nearby: " + blocksFound);
        }
    }
    
    private void sendHelp(Player player) {
        player.sendMessage("§6=== SkyHuntCore Commands ===");
        player.sendMessage("§e/is new §7- Create a new island (singleplayer or teams)");
        player.sendMessage("§e/is home §7- Teleport to your island");
        player.sendMessage("§e/is leave §7- Leave your island");
        player.sendMessage("§e/is view [team] §7- View island info");
        player.sendMessage("§e/is top §7- View leaderboard");
        player.sendMessage("§e/is chat §7- Toggle team chat");
        player.sendMessage("§e/is forcefield §7- Check forcefield status");
        player.sendMessage("§6=== Leader Commands ===");
        player.sendMessage("§e/is sethome §7- Set island home");
        player.sendMessage("§e/is invite <player> §7- Invite a player");
        player.sendMessage("§e/is kick <player> §7- Kick a player");
        player.sendMessage("§e/is promote <player> §7- Promote a member");
        player.sendMessage("§e/is demote <player> §7- Demote a member");
        player.sendMessage("§e/is delete §7- Delete island (type twice)");
        player.sendMessage("§e/is reset §7- Reset island (type twice)");
        player.sendMessage("§e/is endforcefield §7- End forcefield early");
        player.sendMessage("§e/is help §7- Show this help message");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("new");
            completions.add("home");
            completions.add("leave");
            completions.add("view");
            completions.add("top");
            completions.add("chat");
            completions.add("forcefield");
            completions.add("sethome");
            completions.add("invite");
            completions.add("kick");
            completions.add("promote");
            completions.add("demote");
            completions.add("delete");
            completions.add("reset");
            completions.add("endforcefield");
            completions.add("setup");
            completions.add("help");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("kick") || 
                                        args[0].equalsIgnoreCase("promote") || args[0].equalsIgnoreCase("demote"))) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }
        
        return completions;
    }
}
