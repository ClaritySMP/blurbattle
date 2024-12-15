package org.neo.blurbattle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

public final class Blurbattle extends JavaPlugin implements Listener {

    public final HashMap<UUID, UUID> battleRequests = new HashMap<>();
    public final HashMap<UUID, Location> originalLocations = new HashMap<>();
    public final HashMap<UUID, BettingInventory> bettingInventories = new HashMap<>();
    private final HashMap<UUID, Double> originalHealth = new HashMap<>();
    private final HashMap<UUID, Integer> originalHunger = new HashMap<>();
    public final HashMap<UUID, Boolean> readyPlayers = new HashMap<>();
    private boolean isBattleReady = false;
    public boolean isReopeningInventory = false;
    private static Blurbattle instance;

    public HashMap<UUID, BettingInventory> getBettingInventories() {
        return bettingInventories;
    }
    public static Blurbattle getInstance() {
        if (instance == null) {
            instance = new Blurbattle();
        }
        return instance;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1){
            List<String> playerNames = new ArrayList<>();
            Player[] players = new Player[Bukkit.getServer().getOnlinePlayers().size()];
            Bukkit.getServer().getOnlinePlayers().toArray(players);
            for (int i = 0; i < players.length; i++) {
                playerNames.add(players[i].getName());
            }
                return playerNames;
            } else if (args.length == 2){
                List<String> arguments = new ArrayList<>();
                arguments.add("Daddy");
                arguments.add("JamesHarden#1");

                return arguments;
            }
            return null;
        }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        getCommand("blurbattle").setTabCompleter(new tabcomplete());

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



    }

    @Override
    public void onDisable() {
        getLogger().info(ChatColor.GREEN + "Blurbattle is now shutting down");
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

                // Remove battle requests
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


                battleRequests.put(player.getUniqueId(), target.getUniqueId());
                target.sendMessage(ChatColor.YELLOW + player.getName() + " has challenged you to a battle! Type /blurbattle confirm to accept or /blurbattle cancel to deny.");
                player.sendMessage(ChatColor.GREEN + "Battle request sent to " + target.getName() + ".");

                // Timeout for the request
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (battleRequests.containsKey(player.getUniqueId())) {
                            battleRequests.remove(player.getUniqueId());
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
    // todo, add inventory failsafe
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        battleRequests.remove(playerId);

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
                readyPlayers.put(player.getUniqueId(), true);
                player.sendMessage(ChatColor.GREEN + "You are ready for the battle.");

                // Check if both players are ready
                UUID opponentUUID = battleRequests.get(player.getUniqueId());
                if (opponentUUID != null && readyPlayers.containsKey(opponentUUID)) {
                    startBattle(player, opponentUUID);
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
        battleRequests.remove(playerId);
        originalLocations.remove(playerId);

        // Check if player has a betting inventory
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
        }

    }
    // todo, from here on out, new class
    public void startBattle(Player player, UUID opponentUUID) {
        Player opponent = Bukkit.getPlayer(opponentUUID);

        if (opponent != null && opponent.isOnline()) {
            // ... (Your existing battle start logic: teleport, save health, etc.) ...

            // Clear ready status and battle request
            readyPlayers.remove(player.getUniqueId());
            readyPlayers.remove(opponentUUID);
            battleRequests.remove(opponentUUID);

        } else {
            player.sendMessage(ChatColor.RED + "The opponent is no longer online.");
            readyPlayers.remove(player.getUniqueId()); // Remove player's ready status
        }
    }

    private void handleLoss(Player player, Player opponent, UUID opponentId) {         // Teleport the player back to the normal world (e.g., "world")
        World world = Bukkit.getWorld("world"); // Replace "world" with the actual name of your normal world
        Location spawnLocation = world.getSpawnLocation();
        player.teleport(spawnLocation);

        // Restore health and hunger
        if (originalHealth.containsKey(player.getUniqueId()) && originalHunger.containsKey(player.getUniqueId())) {
            player.setHealth(originalHealth.get(player.getUniqueId()));
            player.setFoodLevel(originalHunger.get(player.getUniqueId()));
        }

        // Announce the winner
        opponent.sendMessage(ChatColor.GREEN + "You have won the battle!");
        player.sendMessage(ChatColor.RED + "You have lost the battle.");

        // Give the winner the betted items
        // (This part needs further implementation based on how you store betted items)
        // ... (logic to transfer items from both players' betting inventories to the winner) ...

        // Clear related data
        battleRequests.remove(player.getUniqueId());
        battleRequests.remove(opponentId);
        originalLocations.remove(player.getUniqueId());
        originalLocations.remove(opponentId);
        originalHealth.remove(player.getUniqueId());
        originalHealth.remove(opponentId);
        originalHunger.remove(player.getUniqueId());
        originalHunger.remove(opponentId);
        bettingInventories.remove(player.getUniqueId());
        bettingInventories.remove(opponentId);
    }

}


