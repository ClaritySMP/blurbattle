package org.neo.blurbattle;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;

public class pvpmap {
    // todo, from here on out, new class
    public void startBattle(Player player, UUID opponentUUID) {
        Player opponent = Bukkit.getPlayer(opponentUUID);

        if (opponent != null && opponent.isOnline()) {
            // ... (Your existing battle start logic: teleport, save health, etc.) ...

            // Clear ready status and battle request
            Blurbattle.getInstance().readyPlayers.remove(player.getUniqueId());
            Blurbattle.getInstance().readyPlayers.remove(opponentUUID);
            Blurbattle.getInstance().battleRequests.remove(opponentUUID);

        } else {
            player.sendMessage(ChatColor.RED + "The opponent is no longer online.");
            Blurbattle.getInstance().readyPlayers.remove(player.getUniqueId()); // Remove player's ready status
        }
    }

    private void handleLoss(Player player, Player opponent, UUID opponentId) {         // Teleport the player back to the normal world (e.g., "world")
        World world = Bukkit.getWorld("world"); // Replace "world" with the actual name of your normal world
        Location spawnLocation = world.getSpawnLocation();
        player.teleport(spawnLocation);

        // Restore health and hunger
        if (Blurbattle.getInstance().originalHealth.containsKey(player.getUniqueId()) && Blurbattle.getInstance().originalHunger.containsKey(player.getUniqueId())) {
            player.setHealth(Blurbattle.getInstance().originalHealth.get(player.getUniqueId()));
            player.setFoodLevel(Blurbattle.getInstance().originalHunger.get(player.getUniqueId()));
        }

        // Announce the winner
        opponent.sendMessage(ChatColor.GREEN + "You have won the battle!");
        player.sendMessage(ChatColor.RED + "You have lost the battle.");

        // Give the winner the betted items
        // (This part needs further implementation based on how you store betted items)
        // ... (logic to transfer items from both players' betting inventories to the winner) ...

        // Clear related data
        Blurbattle.getInstance().battleRequests.remove(player.getUniqueId());
        Blurbattle.getInstance().battleRequests.remove(opponentId);
        Blurbattle.getInstance().originalLocations.remove(player.getUniqueId());
        Blurbattle.getInstance().originalLocations.remove(opponentId);
        Blurbattle.getInstance().originalHealth.remove(player.getUniqueId());
        Blurbattle.getInstance().originalHealth.remove(opponentId);
        Blurbattle.getInstance().originalHunger.remove(player.getUniqueId());
        Blurbattle.getInstance().originalHunger.remove(opponentId);
        Blurbattle.getInstance().bettingInventories.remove(player.getUniqueId());
        Blurbattle.getInstance().bettingInventories.remove(opponentId);
    }

}
