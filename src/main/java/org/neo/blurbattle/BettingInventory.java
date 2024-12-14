package org.neo.blurbattle;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.UUID;
import java.util.HashMap;

public class BettingInventory implements Listener {
    private final Player player;
    private final Blurbattle plugin;
    private final Inventory inventory;
    private final String START_BATTLE_ITEM_NAME = ChatColor.GREEN + "Start Battle";
    private final HashMap<UUID, BettingInventory> bettingInventories;
    public BettingInventory(Player player, Blurbattle plugin) {
        this.player = player;
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(null, 27, ChatColor.BLUE + "Your Bet");
        this.bettingInventories = plugin.getBettingInventories();
        addStartBattleItem();
        Bukkit.getPluginManager().registerEvents(this, plugin);

    }
    private void addStartBattleItem() {
        ItemStack startBattleItem = new ItemStack(org.bukkit.Material.NETHER_STAR);
        ItemMeta meta = startBattleItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Start Battle");
            startBattleItem.setItemMeta(meta);
        }
        inventory.setItem(26, startBattleItem); // Slot 26 is the last slot in a 27-slot inventory
    }

    public void openInventory() {
        player.openInventory(inventory);
    }

    public void openBettingMenu(Player player1, Player player2, HashMap<UUID, BettingInventory> bettingInventories) {
        //todo add another item to cancel it telling the other person
        Inventory inventory1 = Bukkit.createInventory(null, 27, ChatColor.BLUE + "Your Bet");
        Inventory inventory2 = Bukkit.createInventory(null, 27, ChatColor.BLUE + "Your Bet");

        bettingInventories.put(player1.getUniqueId(), new BettingInventory(player1, plugin));
        bettingInventories.put(player2.getUniqueId(), new BettingInventory(player2, plugin));



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
        Blurbattle.getInstance().battleRequests.remove(playerId);
        Blurbattle.getInstance().bettingInventories.remove(playerId);
        Blurbattle.getInstance().originalLocations.remove(playerId);
    }






    public Inventory getInventory() {
        return this.inventory;
    }

}