package org.neo.blurbattle;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.Sound;
import com.onarandombox.MultiverseCore.api.Core;
import java.util.UUID;
import com.onarandombox.MultiverseCore.api.MVWorldManager;

public class pvpmap {
    // todo tp with direction and replace message with title
    public void startBattle(Player player, UUID opponentUUID) {
        Player opponent = Bukkit.getPlayer(opponentUUID);
        Core mvcore = (Core) Bukkit.getServer().getPluginManager().getPlugin("Multiverse-Core");
        MVWorldManager worldManager = mvcore.getMVWorldManager();

        if (opponent != null && opponent.isOnline()) {

            World world = worldManager.getMVWorld("blurbattle").getCBWorld();

            // Define player locations within the "blurbattle" world
            Location playerLocation = new Location(world, 27.5, 0, 0.5, 90, 0);
            Location opponentLocation = new Location(world, -27.5, 0, 0.5, 270, 0);

            // Teleport players directly using the Multiverse-Core API
            player.teleport(playerLocation);
            opponent.teleport(opponentLocation);

            // TODO: Fully heal and restore players' hunger after teleport
            Blurbattle.getInstance().originalHealth.put(player.getUniqueId(), player.getHealth());
            Blurbattle.getInstance().originalHunger.put(player.getUniqueId(), player.getFoodLevel());

            Blurbattle.getInstance().originalHealth.put(opponent.getUniqueId(), opponent.getHealth());
            Blurbattle.getInstance().originalHunger.put(opponent.getUniqueId(), opponent.getFoodLevel());
            player.setHealth(20.0);
            player.setFoodLevel(20);
            opponent.setHealth(20.0);
            opponent.setFoodLevel(20);

            // Clear ready status and battle request
            Blurbattle.getInstance().readyPlayers.remove(player.getUniqueId());
            Blurbattle.getInstance().battleRequests.remove(player.getUniqueId());
            Blurbattle.getInstance().readyPlayers.remove(opponentUUID);
            Blurbattle.getInstance().battleRequests.remove(opponentUUID);
            Blurbattle.getInstance().bettingInventories.remove(player.getUniqueId());
            Blurbattle.getInstance().bettingInventories.remove(opponentUUID);
            player.closeInventory();
            opponent.closeInventory();
            player.playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_0, 1.0f, 1.0f);
            opponent.playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_0, 1.0f, 1.0f);
            player.sendTitle(ChatColor.BLUE + "Let the battle BEGIN!",
                    ChatColor.AQUA + "May the best player win!",
                    10, 60, 10); // Customize fade times (ticks)
            opponent.sendTitle(ChatColor.BLUE + "Let the battle BEGIN!",
                    ChatColor.AQUA + "May the best player win!",
                    10, 60, 10);
        } else {
            player.sendMessage(ChatColor.RED + "The opponent is no longer online.");
            Blurbattle.getInstance().readyPlayers.remove(player.getUniqueId()); // Remove player's ready status
            Blurbattle.getInstance().readyPlayers.remove(opponentUUID);
            Blurbattle.getInstance().battleRequests.remove(player.getUniqueId());
            Blurbattle.getInstance().battleRequests.remove(opponentUUID);
        }
    }


    public void handleLoss(Player player, Player opponent, UUID opponentId) {
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
        Blurbattle.getInstance().battleplayers.remove(player.getUniqueId());
        Blurbattle.getInstance().battleplayers.remove(opponentId);
    }

}
