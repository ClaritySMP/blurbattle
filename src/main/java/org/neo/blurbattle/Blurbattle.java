package org.neo.blurbattle;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.inventory.InventoryCloseEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

public final class Blurbattle extends JavaPlugin implements Listener {

    public final HashMap<UUID, UUID> battleRequests = new HashMap<>();
    public final HashMap<UUID, Location> originalLocations = new HashMap<>();
    public final HashMap<UUID, Location> opoglocation = new HashMap<>(); //failsafe just becus
    public final HashMap<UUID, BettingInventory> bettingInventories = new HashMap<>();
    public final HashMap<UUID, Double> originalHealth = new HashMap<>();
    public final HashMap<UUID, Integer> originalHunger = new HashMap<>();
    public final HashMap<UUID, Boolean> readyPlayers = new HashMap<>();
    public final HashMap<UUID, UUID> battleplayers = new HashMap<>();
    public final HashMap<Player, List<ItemStack>> playerBets = new HashMap<>();
    private Map<UUID, List<ItemStack>> playerInventories = new HashMap<>();
    private boolean isBattleReady = false;
    public boolean isReopeningInventory = false;
    private static Blurbattle instance;
    private pvpmap pvpMap;
    public File dataFolder = getDataFolder();
    public File backupFolder = new File(dataFolder, "backups");;
    private String worldName = "blurbattle";
    private String mainworldName = getConfig().getString("main-world-name");

    public HashMap<UUID, BettingInventory> getBettingInventories() {
        return bettingInventories;
    }
    public static Blurbattle getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Plugin instance not initialized!");
        }
        return instance;
    }




    @Override
    public void onEnable() {
        instance = this;
        getServer().getPluginManager().registerEvents(this, this);

        getCommand("blurbattle").setTabCompleter(new tabcomplete());
        saveDefaultConfig();
        String eyeAscii =
                """                                
                                 
                                 ...',;;:cccccccc:;,..
                            ..,;:cccc::::ccccclloooolc;'.
                         .',;:::;;;;:loodxk0kkxxkxxdocccc;;'..
                       .,;;;,,;:coxldKNWWWMMMMWNNWWNNKkdolcccc:,.
                    .',;;,',;lxo:...dXWMMMMMMMMNkloOXNNNX0koc:coo;.
                 ..,;:;,,,:ldl'   .kWMMMWXXNWMMMMXd..':d0XWWN0d:;lkd,
               ..,;;,,'':loc.     lKMMMNl. .c0KNWNK:  ..';lx00X0l,cxo,.
             ..''....'cooc.       c0NMMX;   .l0XWN0;       ,ddx00occl:.
           ..'..  .':odc.         .x0KKKkolcld000xc.       .cxxxkkdl:,..
         ..''..   ;dxolc;'         .lxx000kkxx00kc.      .;looolllol:'..
        ..'..    .':lloolc:,..       'lxkkkkk0kd,   ..':clc:::;,,;:;,'..
        ......   ....',;;;:ccc::;;,''',:loddol:,,;:clllolc:;;,'........
            .     ....'''',,,;;:cccccclllloooollllccc:c:::;,'..
                    .......'',,,,,,,,;;::::ccccc::::;;;,,''...
                      ...............''',,,;;;,,''''''......
                           ............................""";
                        

        getLogger().info(ChatColor.GREEN + eyeAscii);
        getLogger().info(ChatColor.GREEN + "Blurbattle plugin has been enabled!");
        getLogger().info(dataFolder.toString());
        this.pvpMap = new pvpmap();


        if (!dataFolder.exists()) {
            dataFolder.mkdir();
        }


        // Check if the backups directory exists
        if (!backupFolder.exists()) {
            if (backupFolder.mkdir()) {
                getLogger().info("Created backups directory.");
                getLogger().info("Creating initial backup for world: " + worldName);
                getServer().getScheduler().runTaskLater(this, new Runnable() {
                    @Override
                    public void run() {
                        pvpMap.createWorldBackup();
                    }
                }, 1);
            } else {
                getLogger().severe("Failed to create backups directory.");
            }
        } else {
            getLogger().info("backup folder already exists");
        }

        File worldFolder = new File(Bukkit.getServer().getWorldContainer(), worldName);
        if(!worldFolder.exists()) {
            getLogger().info("warning: blurbattle pvp world does not exist, cannot continue.");
            getLogger().info("to create a new world, type  '/mv create [WorldName] normal' in the console to create the arena world");
            File backupFolder = new File(Blurbattle.getInstance().dataFolder, "backups");
            pvpMap.deleteFolder(backupFolder);
            disablePlugin();
        }


    }

    @Override
    public void onDisable() {
        getLogger().info(ChatColor.GREEN + "Blurbattle is now shutting down");
    }

    public void disablePlugin() {
        Bukkit.getLogger().warning("Disabling Blurbattle plugin...");
        Bukkit.getPluginManager().disablePlugin(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /blurbattle <player>");
            return true;
        }
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("confirm")) {
                // Handle the confirm command
               UUID challengerId = null;
                for (Map.Entry<UUID, UUID> entry : battleRequests.entrySet()) {
                    if (entry.getValue().equals(player.getUniqueId())) {
                        challengerId = entry.getKey();
                        break;
                    }
                }

                if (challengerId == null) {
                    player.sendMessage(ChatColor.RED + "No pending battle request found.");
                    return true;
                }

                Player challenger = Bukkit.getPlayer(challengerId);
                if (challenger == null || !challenger.isOnline()) {
                    player.sendMessage(ChatColor.RED + "The challenger is no longer online.");
                    battleRequests.remove(challengerId);
                    return true;
                }
                UUID opponentId = battleRequests.get(challengerId);
                battleplayers.put(challengerId, opponentId);
                battleplayers.put(opponentId, challengerId);
                originalLocations.put(player.getUniqueId(), player.getLocation());
                originalLocations.put(challengerId, challenger.getLocation());
                opoglocation.put(player.getUniqueId(), player.getLocation());
                opoglocation.put(challengerId, challenger.getLocation());
                openBettingMenu(challenger, player);
                battleRequests.remove(challengerId);
                return true;
            } else if (args[0].equalsIgnoreCase("cancel")) {
                UUID challengerId = player.getUniqueId();
                UUID opponentId = battleRequests.get(challengerId);

                if (opponentId == null) {
                    // Check if the player is the opponent instead
                    for (Map.Entry<UUID, UUID> entry : battleRequests.entrySet()) {
                        if (entry.getValue().equals(challengerId)) {
                            challengerId = entry.getKey();
                            opponentId = entry.getValue();
                            break;
                        }
                    }
                }

                if (opponentId == null) {
                    player.sendMessage(ChatColor.RED + "No pending battle request found.");
                    return true;
                }


                // Then remove from battleRequests
                battleRequests.remove(challengerId);
                battleRequests.remove(opponentId);

                // Get Player objects
                Player challenger = Bukkit.getPlayer(challengerId);
                Player opponent = Bukkit.getPlayer(opponentId);

                // Send messages
                if (player.getUniqueId().equals(challengerId)) {
                    // The sender of the request cancels
                    if (opponent != null) {
                        opponent.sendMessage(ChatColor.RED + challenger.getName() + " has canceled the battle request.");
                    }
                    player.sendMessage(ChatColor.RED + "You canceled the battle request.");
                } else {
                    // The receiver of the request cancels
                    if (challenger != null) {
                        challenger.sendMessage(ChatColor.RED + opponent.getName() + " has canceled the battle request.");
                    }
                    player.sendMessage(ChatColor.RED + "You canceled the battle request.");
                }

                return true;
            } else {
                // Handle the challenge command
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    player.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }

                if (battleRequests.containsKey(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "You already have a pending battle request.");
                    return true;
                }
                if(player == target) {
                    player.sendMessage(ChatColor.RED + "You can't send a request to yourself");
                    return true;
                }
                if(target.getUniqueId() == battleRequests.get(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "That player already sent a Request to you");
                    return true;
                }
                if(battleplayers.containsKey(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "You can't send another request while in battle or while the world is resetting");
                    return true;
                }
                if(!battleplayers.isEmpty()) {
                    player.sendMessage(ChatColor.GOLD + "There is a battle going on, please wait a little longer and try again.");
                    return true;
                }

                battleRequests.put(player.getUniqueId(), target.getUniqueId());
                battleRequests.put(target.getUniqueId(), player.getUniqueId());
                target.sendMessage(ChatColor.YELLOW + player.getName() + " has challenged you to a battle! Type /blurbattle confirm to accept or /blurbattle cancel to deny.");
                player.sendMessage(ChatColor.GREEN + "Battle request sent to " + target.getName() + ".");
                getLogger().info(battleplayers.toString());

                // Timeout for the request
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (battleRequests.containsKey(player.getUniqueId())) {
                            battleRequests.remove(player.getUniqueId());
                            battleRequests.remove(target.getUniqueId());
                            player.sendMessage(ChatColor.RED + "Your battle request to " + target.getName() + " has expired.");
                        }
                    }
                }.runTaskLater(this, 20 * 60); // 60 seconds
                return true;
            }
        }

        player.sendMessage(ChatColor.RED + "Usage: /blurbattle <player> or /blurbattle confirm");
        return true;
    }

    private void openBettingMenu(Player player1, Player player2) {
        BettingInventory inventory1 = new BettingInventory(player1, this);
        BettingInventory inventory2 = new BettingInventory(player2, this);

        bettingInventories.put(player1.getUniqueId(), inventory1); // Store BettingInventory objects
        bettingInventories.put(player2.getUniqueId(), inventory2);

        inventory1.openInventory();
        inventory2.openInventory();
    }
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(ChatColor.BLUE + "Your Bet")) {
            Player player = (Player) event.getPlayer();
            if (bettingInventories.containsKey(player.getUniqueId()) && !isReopeningInventory) {
                isReopeningInventory = true; // Set flag to prevent recursive calls
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    if (player.isOnline()) {
                        BettingInventory bettingInventory = bettingInventories.get(player.getUniqueId());
                        if (bettingInventory != null) {
                            player.openInventory(bettingInventory.getInventory());
                        }

                    }
                    isReopeningInventory = false; // Reset flag after reopening
                }, 2L); // Delay of 40 ticks (200ms)
            }
        }
    }
    public void storePlayerInventory(Player player) {
        UUID playerId = player.getUniqueId();
        List<ItemStack> inventoryItems = new ArrayList<>();

        // Iterate through the player's inventory
        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack != null && itemStack.getType() != Material.AIR) {
                // Clone the item to prevent modification of the original stack later
                ItemStack clonedItem = itemStack.clone();
                inventoryItems.add(clonedItem);
            }
        }

        // Store the list of items in the map
        playerInventories.put(playerId, inventoryItems);
        player.sendMessage(ChatColor.GREEN + "Your inventory has been stored.");
    }

    // Method to retrieve and restore a player's inventory
    public void restorePlayerInventory(Player player) {
        UUID playerId = player.getUniqueId();

        // Check if the player's inventory is stored
        if (playerInventories.containsKey(playerId)) {
            List<ItemStack> storedItems = playerInventories.get(playerId);

            // Clear current inventory before restoring the stored items
            player.getInventory().clear();

            // Add the stored items back to the player's inventory
            for (ItemStack itemStack : storedItems) {
                player.getInventory().addItem(itemStack);
            }

            player.sendMessage(ChatColor.GREEN + "Your inventory has been restored.");
        } else {
            player.sendMessage(ChatColor.RED + "No stored inventory found.");
        }
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        battleRequests.remove(battleRequests.get(playerId));
        battleRequests.remove(playerId);
        UUID opponentId = battleplayers.get(playerId);

        battleRequests.remove(playerId);
        originalLocations.remove(playerId);
        Player player = event.getPlayer();

        originalLocations.remove(playerId);

        // Check if player has a betting inventory
        if (bettingInventories.containsKey(playerId)) {
            BettingInventory bettingInventory = bettingInventories.get(playerId);

            // Find the other player (assuming there are only two players)
            UUID otherPlayerId = getOtherPlayerId(playerId);
            Player otherPlayer = Bukkit.getPlayer(otherPlayerId);

            // Return items to the quitting player
            for (int i = 0; i < bettingInventory.getInventory().getSize(); i++) {
                ItemStack itemStack = bettingInventory.getInventory().getItem(i);

                if (itemStack != null && itemStack.getType() != Material.AIR) {
                    // Check for specific items and remove them from the ItemStack
                    removeItemFromItemStack(itemStack, ChatColor.GREEN + "Start Battle");
                    removeItemFromItemStack(itemStack, ChatColor.RED + "Cancel Bet");

                    // Add the modified ItemStack (if any) to the player's inventory
                    event.getPlayer().getInventory().addItem(itemStack);

                }
            }

            // If the other player is still online, return their items
            if (otherPlayer != null) {
                BettingInventory otherBettingInventory = bettingInventories.get(otherPlayerId);
                if (otherBettingInventory != null) {
                    for (int i = 0; i < otherBettingInventory.getInventory().getSize(); i++) {
                        ItemStack itemStack = otherBettingInventory.getInventory().getItem(i);
                        if (itemStack != null && itemStack.getType() != Material.AIR) {
                            otherPlayer.getInventory().addItem(itemStack);
                            ItemMeta itemMeta = itemStack.getItemMeta();
                            if (itemMeta != null && itemMeta.hasDisplayName() &&
                                    (itemMeta.getDisplayName().equals(ChatColor.GREEN + "Start Battle") ||
                                            itemMeta.getDisplayName().equals(ChatColor.RED + "Cancel Bet"))) {
                                // ... (remove from otherBettingInventory)
                                removeItemByName(otherPlayer, ChatColor.GREEN + "Start Battle");
                                removeItemByName(otherPlayer, ChatColor.RED + "Cancel Bet");

                            }
                        }
                    }
                }

                // Notify the other player
                otherPlayer.sendMessage(ChatColor.RED + event.getPlayer().getName() + " has left, the battle has been cancelled.");

                // Close the other player's inventory
                otherPlayer.closeInventory();

            }

            // Remove betting inventories for both players
            bettingInventories.remove(playerId);
            bettingInventories.remove(otherPlayerId);
            readyPlayers.remove(otherPlayerId);
            readyPlayers.remove(playerId);
            battleplayers.remove(playerId);
            battleplayers.remove(otherPlayerId);
        }
        if (player.getWorld().getName().equals(worldName)) {
            for (UUID uuid : battleplayers.keySet()) {
                if (battleplayers.get(uuid).equals(player.getUniqueId()) ||
                        battleplayers.containsKey(player.getUniqueId()) && battleplayers.get(player.getUniqueId()).equals(uuid)) {
                    opponentId = uuid;
                    break;
                }
            }
            Location opponentStoredLocation = Blurbattle.getInstance().opoglocation.getOrDefault(player.getUniqueId(), null);
            double x = opponentStoredLocation.getX();
            double y = opponentStoredLocation.getY();
            double z = opponentStoredLocation.getZ();
            float yaw = opponentStoredLocation.getYaw();
            float pitch = opponentStoredLocation.getPitch();
            World world = Bukkit.getWorld(mainworldName);

            Location respawnLocation = new Location(world, x, y, z, yaw, pitch);
            player.spigot().respawn(); // Force the player to respawn
            player.teleport(respawnLocation); // Teleport to the desired location
            restorePlayerInventory(player);
            if (opponentId != null) {

                Player opponent = Bukkit.getPlayer(opponentId);

                // Handle the loss

                pvpMap.handleLoss(player, opponent, opponentId);
            }
        }
    }
    private void removeItemFromItemStack(ItemStack itemStack, String itemName) {
        int amountToRemove = 0; // Stores the total amount to remove

        // Check if the itemStack has the desired display name
        if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName() &&
                itemStack.getItemMeta().getDisplayName().equals(itemName)) {
            amountToRemove = itemStack.getAmount(); // Remove all if it matches exactly
        }

        // Reduce the amount based on the desired removal amount
        itemStack.setAmount(Math.max(itemStack.getAmount() - amountToRemove, 0));
    }


    public static void removeItemByName(Player player, String itemName) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                    item.getItemMeta().getDisplayName().equals(itemName)) {
                player.getInventory().removeItem(item);
                break; // Stop after removing the first matching item
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Handle the "Your Bet" inventory
        if (event.getView().getTitle().equals(ChatColor.BLUE + "Your Bet")) {
            if (event.getSlot() == 26) {
                event.setCancelled(true);  // Prevent item from moving
                Player player = (Player) event.getWhoClicked();





                // Check if player is already ready
                if (readyPlayers.containsKey(player.getUniqueId())) {
                    player.sendMessage(ChatColor.YELLOW + "You are already ready.");
                    return;
                }

                // Mark player as ready

                UUID playerUUID = player.getUniqueId();
                readyPlayers.put(playerUUID, true);
                player.sendMessage(ChatColor.GREEN + "You are ready for the battle.");
                // Check if both players are ready
                UUID opponentUUID = battleplayers.get(playerUUID);


                // Check if both players are ready
                if (opponentUUID != null && readyPlayers.containsKey(playerUUID) && readyPlayers.containsKey(opponentUUID)) {
                    Player opponent = Bukkit.getPlayer(opponentUUID);
                    if (opponent != null && readyPlayers.containsKey(player.getUniqueId()) && readyPlayers.containsKey(opponentUUID)) {
                        // Assuming you have a method to get items for the players (e.g., from betting inventories)
                        List<ItemStack> playerItems = getPlayerBet(player);   // You will define this method to get the player's items
                        List<ItemStack> opponentItems = getPlayerBet(opponent); // Similarly, for the opponent

                        // Store the player and their items in the HashMap
                        playerBets.put(player, playerItems);
                        playerBets.put(opponent, opponentItems);

                        // Call the method to start the battle
                        storePlayerInventory(player);
                        storePlayerInventory(opponent);
                        startBattle(player, opponentUUID);
                    }
                } else {
                    player.sendMessage(ChatColor.YELLOW + "Waiting for your opponent to ready up.");

                }
            } else if (event.getSlot() == 25) {
                event.setCancelled(true);  // Prevent item from moving
                Player clickedPlayer = (Player) event.getWhoClicked();
                UUID otherPlayerId = getOtherPlayerId(clickedPlayer.getUniqueId());
                Player otherPlayer = Bukkit.getPlayer(otherPlayerId);
                if (otherPlayer != null) {
                    // Notify both players that the bet was canceled
                    otherPlayer.sendMessage(ChatColor.RED + clickedPlayer.getName() + " has canceled the bet.");
                    clickedPlayer.sendMessage(ChatColor.RED + "You have canceled the bet.");


                    // Close both inventories
                    clickedPlayer.closeInventory();
                    otherPlayer.closeInventory();

                    returnItemsToPlayer(clickedPlayer, otherPlayerId);
                    returnItemsToPlayer(otherPlayer, clickedPlayer.getUniqueId());

                    // Remove betting-related data to ensure the bet is fully canceled
                    bettingInventories.remove(clickedPlayer.getUniqueId());
                    bettingInventories.remove(otherPlayerId);

                    // Optionally clear ready state if you want to reset it when a bet is canceled
                    readyPlayers.remove(clickedPlayer.getUniqueId());
                    readyPlayers.remove(otherPlayerId);

                    // If you want to cancel the battle request as well:
                    battleRequests.remove(clickedPlayer.getUniqueId());
                    battleRequests.remove(otherPlayerId);
                }
            }
        }


    }

    private void returnItemsToPlayer(Player player, UUID otherPlayerId) {
        // Only return items if not already done
        if (!bettingInventories.containsKey(player.getUniqueId())) {
            return;  // No betting inventory, nothing to return
        }

        BettingInventory bettingInventory = bettingInventories.get(player.getUniqueId());

        // Return items to the quitting player
        for (int i = 0; i < bettingInventory.getInventory().getSize(); i++) {
            ItemStack itemStack = bettingInventory.getInventory().getItem(i);

            if (itemStack != null && itemStack.getType() != Material.AIR) {
                // Check for specific items and remove them from the ItemStack
                removeItemFromItemStack(itemStack, ChatColor.GREEN + "Start Battle");
                removeItemFromItemStack(itemStack, ChatColor.RED + "Cancel Bet");

                // Add the modified ItemStack (if any) to the player's inventory
                player.getInventory().addItem(itemStack);
            }
        }
    }
    private List<ItemStack> getPlayerBet(Player player) {
        // Convert player's inventory items from array to list
        BettingInventory bettingInventory = bettingInventories.get(player.getUniqueId());
        if (bettingInventory == null) {
            return new ArrayList<>(); // Return an empty list if no betting inventory is found
        }

        // Convert betting inventory items from array to list
        ItemStack[] inventoryItems = bettingInventory.getInventory().getContents();
        List<ItemStack> playerItems = new ArrayList<>();

        // Convert the array to a list, ignoring null items
        for (ItemStack item : inventoryItems) {
            if (item != null) {
                playerItems.add(item);
            }
        }

        return playerItems;
    }
    private List<ItemStack> getPlayerInventory(Player player) {
        // Convert player's inventory items from array to list
        ItemStack[] inventoryItems = player.getInventory().getContents();
        List<ItemStack> playerItems = new ArrayList<>();

        // Convert the array to a list, ignoring null items
        for (ItemStack item : inventoryItems) {
            if (item != null) {
                playerItems.add(item);
            }
        }

        return playerItems;
    }

    // Get the UUID of the other player (assuming there are only two players)
    public UUID getOtherPlayerId(UUID playerId) {
        for (UUID id : bettingInventories.keySet()) {
            if (!id.equals(playerId)) {
                return id; // Return the other player's ID
            }
        }
        return null; // Should not happen if there are exactly two players
    }



    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        final UUID finalplayerId = playerId;
        UUID opponentId = battleplayers.get(playerId);
        battleRequests.remove(battleRequests.get(playerId));
        battleRequests.remove(playerId);
        originalLocations.remove(playerId);
        Player player = event.getEntity();

        // Check if the death occurred in the "blurbattle" world
        if (player.getWorld().getName().equals(worldName)) {

            // Prevent the default death screen
            event.setDeathMessage(null);
            event.setDroppedExp(0);
            event.getDrops().clear();
            Bukkit.getScheduler().runTask(Blurbattle.getInstance(), () -> {
                World world = Bukkit.getWorld(mainworldName);
                Location opponentStoredLocation = Blurbattle.getInstance().opoglocation.getOrDefault(player.getUniqueId(), null);
                double x = opponentStoredLocation.getX();
                double y = opponentStoredLocation.getY();
                double z = opponentStoredLocation.getZ();
                float yaw = opponentStoredLocation.getYaw();
                float pitch = opponentStoredLocation.getPitch();

                Location respawnLocation = new Location(world, x, y, z, yaw, pitch);
                player.spigot().respawn(); // Force the player to respawn
                player.teleport(respawnLocation); // Teleport to the desired location
                restorePlayerInventory(player);
            });

            // Find the opponent
            for (UUID uuid : battleplayers.keySet()) {
                if (battleplayers.get(uuid).equals(player.getUniqueId()) ||
                        battleplayers.containsKey(player.getUniqueId()) && battleplayers.get(player.getUniqueId()).equals(uuid)) {
                    opponentId = uuid;
                    break;
                }
            }

            if (opponentId != null) {

                Player opponent = Bukkit.getPlayer(opponentId);

                // Handle the loss
                pvpMap.handleLoss(player, opponent, opponentId);
            }
        }        // Check if player has a betting inventory
        if (bettingInventories.containsKey(playerId)) {
            BettingInventory bettingInventory = bettingInventories.get(playerId);

            // Find the other player (assuming there are only two players)
            UUID otherPlayerId = getOtherPlayerId(playerId);
            Player otherPlayer = Bukkit.getPlayer(otherPlayerId);


            if (otherPlayer != null) {
                BettingInventory otherBettingInventory = bettingInventories.get(otherPlayerId);
                if (otherBettingInventory != null) {
                    for (int i = 0; i < otherBettingInventory.getInventory().getSize(); i++) {
                        ItemStack itemStack = otherBettingInventory.getInventory().getItem(i);
                        if (itemStack != null && itemStack.getType() != Material.AIR) {
                            otherPlayer.getInventory().addItem(itemStack);
                            ItemMeta itemMeta = itemStack.getItemMeta();
                            if (itemMeta != null && itemMeta.hasDisplayName() &&
                                    (itemMeta.getDisplayName().equals(ChatColor.GREEN + "Start Battle") ||
                                            itemMeta.getDisplayName().equals(ChatColor.RED + "Cancel Bet"))) {
                                // ... (remove from otherBettingInventory)
                                removeItemByName(otherPlayer, ChatColor.GREEN + "Start Battle");
                                removeItemByName(otherPlayer, ChatColor.RED + "Cancel Bet");
                            }
                        }
                    }
                }

                // Notify the other player
                otherPlayer.sendMessage(ChatColor.RED + event.getPlayer().getName() + " has died.");

                // Close the other player's inventory
                otherPlayer.closeInventory();
            }

            // Remove betting inventories for both players
            bettingInventories.remove(playerId);
            bettingInventories.remove(otherPlayerId);
            readyPlayers.remove(otherPlayerId);
            readyPlayers.remove(playerId);
            battleplayers.remove(playerId);
            battleplayers.remove(otherPlayerId);
        }

    }
    private void startBattle(Player player, UUID opponentUUID) {

        pvpMap.startBattle(player, opponentUUID); // Call startBattle from the pvpmap instance
    }

}


