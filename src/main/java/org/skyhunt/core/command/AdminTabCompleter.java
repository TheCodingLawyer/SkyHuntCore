package org.skyhunt.core.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AdminTabCompleter implements TabCompleter {

    private static final List<String> ROOT = Arrays.asList(
        "reload","setlevel","resetmissions","unlockhead","debug","addlevel","resetlevel","givexp"
    );

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return ROOT.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2) {
            // player names
            return Bukkit.getOnlinePlayers().stream()
                .map(p -> p.getName())
                .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}




