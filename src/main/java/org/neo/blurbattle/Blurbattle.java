package org.neo.blurbattle;
//todo add failsafes
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.inventory.InventoryCloseEvent;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

public final class Blurbattle extends JavaPlugin implements Listener {

    private final HashMap<UUID, UUID> battleRequests = new HashMap<>();
    private final HashMap<UUID, Location> originalLocations = new HashMap<>();
    private final HashMap<UUID, Inventory> bettingInventories = new HashMap<>();
    private final String START_BATTLE_ITEM_NAME = ChatColor.GREEN + "Start Battle";
    private final HashMap<UUID, Double> originalHealth = new HashMap<>();
    private final HashMap<UUID, Integer> originalHunger = new HashMap<>();
    private final HashMap<UUID, Boolean> readyPlayers = new HashMap<>();
    private boolean isBattleReady = false;
    private boolean isReopeningInventory = false;

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
        Bukkit.getPluginManager().registerEvents(this, this);

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

                // Get the opponent's UUID
                UUID opponentId = battleRequests.get(challengerId);

                // Remove battle requests
                battleRequests.remove(challengerId);
                battleRequests.remove(opponentId);

                // Get Player objects
                Player challenger = Bukkit.getPlayer(challengerId);

                // Send messages
                if (challenger != null) {
                    challenger.sendMessage(ChatColor.RED + "The battle request has been canceled.");
                }
                player.sendMessage(ChatColor.RED + "You canceled the battle request.");

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
        //todo add another item to cancel it telling the other person
        Inventory inventory1 = Bukkit.createInventory(null, 27, ChatColor.BLUE + "Your Bet");
        Inventory inventory2 = Bukkit.createInventory(null, 27, ChatColor.BLUE + "Your Bet");

        bettingInventories.put(player1.getUniqueId(), inventory1);
        bettingInventories.put(player2.getUniqueId(), inventory2);



        ItemStack startBattleItem = new ItemStack(org.bukkit.Material.NETHER_STAR);
        ItemMeta meta = startBattleItem.getItemMeta();
        meta.setDisplayName(START_BATTLE_ITEM_NAME);
        startBattleItem.setItemMeta(meta);

        inventory1.setItem(26, startBattleItem);
        inventory2.setItem(26, startBattleItem);

        player1.openInventory(inventory1);
        player2.openInventory(inventory2);
    }
    //todo close menu
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        battleRequests.remove(playerId);
        bettingInventories.remove(playerId);
        originalLocations.remove(playerId);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.BLUE + "Your Bet")) {
            if (event.getSlot() == 26) {
                event.setCancelled(true);

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
            }
        }
    }
    //todo remove debug
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(ChatColor.BLUE + "Your Bet")) {
            Player player = (Player) event.getPlayer();
            if (bettingInventories.containsKey(player.getUniqueId()) && !isReopeningInventory) {
                isReopeningInventory = true; // Set flag to prevent recursive calls
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    if (player.isOnline()) {
                        player.openInventory(bettingInventories.get(player.getUniqueId()));
                    }
                    isReopeningInventory = false; // Reset flag after reopening
                }, 4L); // Delay of 40 ticks (200ms)
            }
        }
    }





    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player.getWorld().getName().equals("blurbattle")) {
            // Prevent the default death screen
            event.setDeathMessage(null);
            event.setDroppedExp(0);
            event.getDrops().clear();

            // Find the opponent
            UUID opponentId = null;
            for (UUID uuid : battleRequests.keySet()) {
                if (battleRequests.get(uuid).equals(player.getUniqueId()) ||
                        battleRequests.containsKey(player.getUniqueId()) && battleRequests.get(player.getUniqueId()).equals(uuid)) {
                    opponentId = uuid;
                    break;
                }
            }

            if (opponentId != null) {
                Player opponent = Bukkit.getPlayer(opponentId);

                // Handle the loss
                handleLoss(player, opponent, opponentId);
            }
        }
    }
    private void startBattle(Player player, UUID opponentUUID) {
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


