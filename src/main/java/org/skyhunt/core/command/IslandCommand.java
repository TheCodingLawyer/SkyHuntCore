package org.skyhunt.core.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.skyhunt.core.service.IslandService;
import org.skyhunt.core.service.MissionService;
import org.skyhunt.core.config.ConfigService;
import org.skyhunt.core.config.LevelDefinition;
import org.skyhunt.core.data.IslandData;
import org.skyhunt.core.data.MissionCategory;
import org.skyhunt.core.config.MissionTask;
import org.skyhunt.core.gui.GuiManager;

/**
 * Basic /island command for create/info/missions/heads placeholders.
 */
public class IslandCommand implements CommandExecutor {

    private final IslandService islands;
    private final MissionService missions;
    private final ConfigService config;
    private final GuiManager guiManager;

    public IslandCommand(IslandService islands, MissionService missions, ConfigService config) {
        this(islands, missions, config, null);
    }

    public IslandCommand(IslandService islands, MissionService missions, ConfigService config, GuiManager guiManager) {
        this.islands = islands;
        this.missions = missions;
        this.config = config;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            if (guiManager != null) {
                guiManager.getIslandMainGUI().open(player);
                return true;
            }
            args = new String[] { "create" };
        }

        if (args[0].equalsIgnoreCase("create")) {
            IslandData data = islands.getIsland(player);
            if (data.getIslandX() == 0 && data.getIslandZ() == 0) {
                islands.ensureIsland(player);
                player.sendMessage(col("&aCreating your island..."));
            } else {
                islands.teleportHome(player, data);
                player.sendMessage(col("&aTeleported to your island. Level: &e" + data.getIslandLevel()));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("home")) {
            IslandData data = islands.getIsland(player);
            islands.teleportHome(player, data);
            return true;
        }

        if (args[0].equalsIgnoreCase("missions")) {
            if (guiManager != null) {
                guiManager.getMissionsGUI().open(player);
                return true;
            }
            // Text fallback
            IslandData data = islands.getIsland(player);
            LevelDefinition def = islands.getLevelDef(data);
            if (def == null) {
                player.sendMessage(col("&cNo missions configured for your level."));
                return true;
            }
            player.sendMessage(col("&6--- Missions for Level " + data.getIslandLevel() + " ---"));
            for (MissionCategory cat : MissionCategory.values()) {
                for (MissionTask task : def.getTasks(cat)) {
                    String id = task.missionId(def.getLevel());
                    var prog = missions.getProgressMap(player.getUniqueId()).get(id);
                    long current = prog == null ? 0 : prog.getProgress();
                    boolean done = prog != null && prog.isCompleted();
                    player.sendMessage(col("&e" + cat.name() + " &7" + task.getTypeName() + ": " + current + "/" + task.getRequired() + (done ? " &a✓" : "")));
                }
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            IslandData data = islands.getIsland(player);
            player.sendMessage(col("&6Island Level: &e" + data.getIslandLevel()));
            return true;
        }

        if (args[0].equalsIgnoreCase("heads")) {
            if (guiManager != null) {
                guiManager.getHeadsGUI().open(player);
                return true;
            }
            player.sendMessage(col("&cHeads GUI unavailable."));
            return true;
        }

        return false;
    }

    private String col(String msg) {
        return msg.replace("&", "§");
    }
}

