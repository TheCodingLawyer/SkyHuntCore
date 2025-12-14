package org.skyhunt.core.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.skyhunt.core.service.IslandService;
import org.skyhunt.core.service.MissionService;
import org.skyhunt.core.config.LevelDefinition;
import org.skyhunt.core.data.IslandData;

/**
 * Handles /levelup and /rankup.
 */
public class LevelUpCommand implements CommandExecutor {

    private final IslandService islands;
    private final MissionService missions;

    public LevelUpCommand(IslandService islands, MissionService missions) {
        this.islands = islands;
        this.missions = missions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        IslandData island = islands.getIsland(player);
        LevelDefinition def = islands.getLevelDef(island);
        if (def == null) {
            player.sendMessage(color("&cNo missions configured for your level."));
            return true;
        }

        boolean allComplete = missions.areAllMissionsComplete(player.getUniqueId(), def);
        if (!islands.canLevelUp(island, allComplete)) {
            String msg = player.getServer().getPluginManager().getPlugin("SkyHuntCore")
                .getConfig().getString("messages.levelup-failed", "&cYou must complete all missions before leveling up!");
            player.sendMessage(color(msg));
            return true;
        }

        missions.resetProgress(player.getUniqueId());
        islands.levelUp(island);

        String msg = player.getServer().getPluginManager().getPlugin("SkyHuntCore")
            .getConfig().getString("messages.levelup-success", "&aIsland leveled up to Level {level}!")
            .replace("{level}", String.valueOf(island.getIslandLevel()));
        player.sendMessage(color(msg));
        return true;
    }

    private String color(String msg) {
        return msg.replace("&", "§");
    }
}




