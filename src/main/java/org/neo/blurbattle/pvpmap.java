package org.neo.blurbattle;

import com.onarandombox.MultiverseCore.MVWorld;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import com.onarandombox.MultiverseCore.api.Core;
import java.util.UUID;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import org.bukkit.inventory.ItemStack;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;


public class pvpmap {
    Core mvcore = (Core) Bukkit.getServer().getPluginManager().getPlugin("Multiverse-Core");
    MVWorldManager worldManager = mvcore.getMVWorldManager();
    String worldName;
    public void startBattle(Player player, UUID opponentUUID) {
        Player opponent = Bukkit.getPlayer(opponentUUID);


        if (opponent != null && opponent.isOnline()) {

            World world = worldManager.getMVWorld("blurbattle").getCBWorld();



            // Define player locations within the "blurbattle" world
            Location playerLocation = new Location(world, 27.5, 0, 0.5, 90, 0);
            Location opponentLocation = new Location(world, -27.5, 0, 0.5, 270, 0);

            // Teleport players directly using the Multiverse-Core API
            player.teleport(playerLocation);
            opponent.teleport(opponentLocation);

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

        World world = worldManager.getMVWorld("world").getCBWorld();
        Location opponentStoredLocation = Blurbattle.getInstance().opoglocation.getOrDefault(opponentId, null);


        // Define player locations within the "blurbattle" world
        Location playerLocation = new Location(world, -27.5, 0, 0.5, 270, 0);
        Location opponentLocation = new Location(world, opponentStoredLocation.getX(), opponentStoredLocation.getY(), opponentStoredLocation.getZ(), opponentStoredLocation.getYaw(), opponentStoredLocation.getPitch());

        // Teleport players directly using the Multiverse-Core API
        player.teleport(playerLocation);
        opponent.teleport(opponentLocation);

        if (Blurbattle.getInstance().originalHealth.containsKey(opponent.getUniqueId()) && Blurbattle.getInstance().originalHunger.containsKey(opponent.getUniqueId())) {
            opponent.setHealth(Blurbattle.getInstance().originalHealth.get(opponentId));
            opponent.setFoodLevel(Blurbattle.getInstance().originalHunger.get(opponentId));
        }

        // Announce the winner
        opponent.sendMessage(ChatColor.GREEN + "You have won the battle!");
        player.sendMessage(ChatColor.RED + "You have lost the battle.");

        // Give the winner the betted items
        // (This part needs further implementation based on how you store betted items)
        // ... (logic to transfer items from both players' betting inventories to the winner) ...
        List<ItemStack> playerItems = Blurbattle.getInstance().playerBets.get(player);
        List<ItemStack> opponentItems = Blurbattle.getInstance().playerBets.get(opponent);

        // Check if player betted items exist, if so, give them to the opponent
        if (playerItems != null) {
            for (ItemStack item : playerItems) {
                opponent.getInventory().addItem(item);
            }
            Blurbattle.getInstance().removeItemByName(opponent, ChatColor.GREEN + "Start Battle");
            Blurbattle.getInstance().removeItemByName(opponent, ChatColor.RED + "Cancel Bet");
        }

        // Check if opponent betted items exist, if so, give them to the player
        if (opponentItems != null) {
            for (ItemStack item : opponentItems) {
                opponent.getInventory().addItem(item);
            }
            Blurbattle.getInstance().removeItemByName(opponent, ChatColor.GREEN + "Start Battle");
            Blurbattle.getInstance().removeItemByName(opponent, ChatColor.RED + "Cancel Bet");
        }

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
        // Blurbattle.getInstance().battleplayers.remove(player.getUniqueId());
        // Blurbattle.getInstance().battleplayers.remove(opponentId);
        // not yet
        resetArenaWorld(player, opponent);
    }

    public void resetArenaWorld(Player player, Player opponent) {
        try {
            // Unload the world first to avoid issues while copying
            unloadWorld();
            worldName = "blurbattle";  // Define the world name locally
            File backupFolder = new File(Blurbattle.getInstance().dataFolder, "backups");
            // Delete current world and copy the backup over it
            File worldFolder = new File(Blurbattle.getInstance().getServer().getWorldContainer(), worldName);
            deleteFolder(worldFolder);

            // Copy backup to the world folder
            File backupWorldFolder = new File(backupFolder, worldName);
            copyFolder(backupWorldFolder, worldFolder);

            // Reload the world
            loadWorld();
            Blurbattle.getInstance().battleplayers.remove(player.getUniqueId());
            Blurbattle.getInstance().battleplayers.remove(opponent.getUniqueId());
        } catch (IOException e) {
            Blurbattle.getInstance().getLogger().severe("Failed to reset the world: " + e.getMessage());
        }
    }

    // Unload the world from Multiverse-Core
    private void unloadWorld() {

        worldManager.unloadWorld(worldName);
    }

    // Load the world again after reset
    private void loadWorld() {

        worldManager.loadWorld(worldName);
    }

    // Delete a folder and its contents recursively
    private void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFolder(file);
                } else {
                    file.delete();
                }
            }
        }
        folder.delete();
    }

    // Copy a folder and its contents recursively
    private void copyFolder(File source, File destination) throws IOException {
        if (!destination.exists()) {
            destination.mkdirs();
        }

        for (File file : source.listFiles()) {
            File destFile = new File(destination, file.getName());
            if (file.isDirectory()) {
                copyFolder(file, destFile);
            } else {
                Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
    public void createWorldBackup() {
        try {
            File worldFolder = new File(Bukkit.getServer().getWorldContainer(), "blurbattle");
            File backupFolder = new File(Blurbattle.getInstance().dataFolder, "backups");
            copyFolder(worldFolder, backupFolder);
        }  catch (IOException e) {
            Blurbattle.getInstance().getLogger().severe("Failed to create world backup: " + e.getMessage());
        }
    }
}
