package org.neo.blurbattle;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class tabcomplete implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                suggestions.add(player.getName());
            }
            suggestions.add("confirm");
            suggestions.add("cancel");
        }

        return suggestions;
    }
}