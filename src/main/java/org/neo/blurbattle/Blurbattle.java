package org.neo.blurbattle;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;

public final class Blurbattle extends JavaPlugin implements Listener {

    private final HashMap<UUID, UUID> battleRequests = new HashMap<>();
    private final HashMap<UUID, Location> originalLocations = new HashMap<>();
    private final HashMap<UUID, Inventory> bettingInventories = new HashMap<>();
    private final String START_BATTLE_ITEM_NAME = ChatColor.GREEN + "Start Battle";
    private final HashMap<UUID, Double> originalHealth = new HashMap<>();
    private final HashMap<UUID, Integer> originalHunger = new HashMap<>();


    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("blurbattle").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }

            Player player = (Player) sender;

            if (args.length == 1) {
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage(ChatColor.RED + "Player not found or not online.");
                    return true;
                }

                if (battleRequests.containsKey(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "You already have a pending battle request.");
                    return true;
                }

                battleRequests.put(player.getUniqueId(), target.getUniqueId());
                target.sendMessage(ChatColor.YELLOW + player.getName() + " has challenged you to a battle! Type /blurbattle confirm to accept.");
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

            if (args.length == 1 && args[0].equalsIgnoreCase("confirm")) {
                UUID challengerId = null;
                for (UUID uuid : battleRequests.keySet()) {
                    if (battleRequests.get(uuid).equals(player.getUniqueId())) {
                        challengerId = uuid;
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
            }

            player.sendMessage(ChatColor.RED + "Usage: /blurbattle [player] or /blurbattle confirm");
            return true;
        });
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void openBettingMenu(Player player1, Player player2) {
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
                event.getWhoClicked().sendMessage(ChatColor.RED + "Press Q to start the battle.");
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        // Save original health and hunger


        ItemStack item = event.getItemDrop().getItemStack();
        if (item.getItemMeta() != null && START_BATTLE_ITEM_NAME.equals(item.getItemMeta().getDisplayName())) {
            Player player = event.getPlayer();
            player.sendMessage(ChatColor.GREEN + "Battle starting!");

            // Fetch the challenger and teleport both players
            UUID challengerId = null;
            for (UUID uuid : battleRequests.keySet()) {
                if (battleRequests.get(uuid).equals(player.getUniqueId())) {
                    challengerId = uuid;
                    break;
                }
            }

            if (challengerId != null) {
                Player challenger = Bukkit.getPlayer(challengerId);
                if (challenger != null && challenger.isOnline()) {
                    // Save original locations
                    originalLocations.put(player.getUniqueId(), player.getLocation());
                    originalLocations.put(challenger.getUniqueId(), challenger.getLocation());

                    // Teleport players using Multiverse-Core
                    // TODO change to config file
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            "mvtp " + player.getName() + " blurbattle");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            "mvtp " + challenger.getName() + " blurbattle");

                    // TODO: Fully heal and restore players' hunger after teleport
                    originalHealth.put(player.getUniqueId(), player.getHealth());
                    originalHunger.put(player.getUniqueId(), player.getFoodLevel());

                    originalHealth.put(challenger.getUniqueId(), challenger.getHealth());
                    originalHunger.put(challenger.getUniqueId(), challenger.getFoodLevel());
                    player.setHealth(20.0);
                    player.setFoodLevel(20);
                    challenger.setHealth(20.0);
                    challenger.setFoodLevel(20);

                    // TODO: Initiate the PvP battle logic
                } else {
                    player.sendMessage(ChatColor.RED + "The challenger is no longer online.");
                }
            } else {
                player.sendMessage(ChatColor.RED + "No battle request found.");
            }

            event.getItemDrop().remove();
        }
    }
}