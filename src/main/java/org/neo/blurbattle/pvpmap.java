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
    String worldName = Blurbattle.getInstance().getConfig().getString("pvp-arena-world-name");
    String mainworldName = Blurbattle.getInstance().getConfig().getString("main-world-name");
    String titleText = Blurbattle.getInstance().getConfig().getString("battle-start-message.title");
    String subtitleText = Blurbattle.getInstance().getConfig().getString("battle-start-message.subtitle");
    String soundName = Blurbattle.getInstance().getConfig().getString("battle-start-message.sound");


    public void startBattle(Player player, UUID opponentUUID) {
        Player opponent = Bukkit.getPlayer(opponentUUID);


        if (opponent != null && opponent.isOnline()) {

            World world = worldManager.getMVWorld(worldName).getCBWorld();



            // Define player locations within the "blurbattle" world
            double player1X = Blurbattle.getInstance().getConfig().getDouble("player1-spawn-coords.x");
            double player1Y = Blurbattle.getInstance().getConfig().getDouble("player1-spawn-coords.y");
            double player1Z = Blurbattle.getInstance().getConfig().getDouble("player1-spawn-coords.z");

            double player2X = Blurbattle.getInstance().getConfig().getDouble("player2-spawn-coords.x");
            double player2Y = Blurbattle.getInstance().getConfig().getDouble("player2-spawn-coords.y");
            double player2Z = Blurbattle.getInstance().getConfig().getDouble("player2-spawn-coords.z");

            Location playerLocation = new Location(world, player1X, player1Y, player1Z);
            Location opponentLocation = new Location(world, player2X, player2Y, player2Z);

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
            try {
                Sound sound = Sound.valueOf(soundName);

                // Play the sound for both players
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                opponent.playSound(opponent.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                // Handle the case where the sound is not valid
                Blurbattle.getInstance().getLogger().warning("The sound '" + soundName + "' is not a valid sound.");
            }
            player.sendTitle(ChatColor.RED + titleText,
                    ChatColor.GOLD + subtitleText,
                    10, 60, 10); // Customize fade times (ticks)
            opponent.sendTitle(ChatColor.BLUE + titleText,
                    ChatColor.AQUA + subtitleText,
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

        World world = worldManager.getMVWorld(mainworldName).getCBWorld();
        Location opponentStoredLocation = Blurbattle.getInstance().opoglocation.getOrDefault(opponentId, null);


        // Define player locations within the "blurbattle" world
        Location opponentLocation = new Location(world, opponentStoredLocation.getX(), opponentStoredLocation.getY(), opponentStoredLocation.getZ(), opponentStoredLocation.getYaw(), opponentStoredLocation.getPitch());

        // Teleport players directly using the Multiverse-Core API
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
        Bukkit.getScheduler().runTaskLater(Blurbattle.getInstance(), () -> {
            try {
                resetArenaWorld(player, opponent);
            } catch (Exception e) {
                Blurbattle.getInstance().getLogger().severe("Failed to reset arena world: " + e.getMessage());
                e.printStackTrace();
            }
        }, 20L); // 20 ticks = 1 second

    }

    public void resetArenaWorld(Player player, Player opponent) {
        try {
            // Unload the world first to avoid issues while copying
            unloadWorld();
              // Define the world name locally
            File backupFolder = new File(Blurbattle.getInstance().dataFolder, "backups");
            // Delete current world and copy the backup over it
            File worldFolder = new File(Blurbattle.getInstance().getServer().getWorldContainer(), worldName);
            deleteFolder(worldFolder);

            // Copy backup to the world folder
            copyFolder(backupFolder, worldFolder);

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
    public void deleteFolder(File folder) {
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
            File worldFolder = new File(Bukkit.getServer().getWorldContainer(), worldName);
            if(worldFolder.exists()) {
                File backupFolder = new File(Blurbattle.getInstance().dataFolder, "backups");
                copyFolder(worldFolder, backupFolder);
            } else {
                Blurbattle.getInstance().getLogger().info("warning: blurbattle pvp world does not exist, cannot continue.");
                Blurbattle.getInstance().getLogger().info("to create a new world, type  '/mv create [WorldName] normal' in the console to create the arena world");
                File backupFolder = new File(Blurbattle.getInstance().dataFolder, "backups");
                deleteFolder(backupFolder);
                Blurbattle.getInstance().disablePlugin();
            }
        }  catch (IOException e) {
            Blurbattle.getInstance().getLogger().severe("Failed to create world backup: " + e.getMessage());
        }
    }
}
