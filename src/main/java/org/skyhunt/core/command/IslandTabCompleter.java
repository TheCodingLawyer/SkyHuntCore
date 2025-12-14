package org.skyhunt.core.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IslandTabCompleter implements TabCompleter {

    private static final List<String> ROOT = Arrays.asList("create", "home", "missions", "info", "heads");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return ROOT.stream()
                .filter(opt -> opt.toLowerCase().startsWith(args[0].toLowerCase()))
                .toList();
        }
        return Collections.emptyList();
    }
}




