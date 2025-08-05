package com.wyvencraft.death;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GravestoneListener implements Listener {

    private final WyvenDeathPlugin plugin;
    private final GravestoneManager gravestoneManager;

    public GravestoneListener(WyvenDeathPlugin plugin, GravestoneManager gravestoneManager) {
        this.plugin = plugin;
        this.gravestoneManager = gravestoneManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getPlayer().hasMetadata("respawning")) {
            event.setCancelled(true);
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();

        if (block == null || block.getType() != Material.STONE) return;

        Player player = event.getPlayer();
        Gravestone gravestone = gravestoneManager.getGravestoneByLocation(block.getLocation());

        if (gravestone != null) {
            if (gravestone.isUnlocked() || gravestone.isOwner(player)) {
                player.openInventory(gravestone.getInventory());
                gravestone.setLooting(true);
            } else {
                player.sendMessage("This gravestone is locked. Please wait until it unlocks.");
            }
        }
    }

    @EventHandler
    public void onGravestoneClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().contains("Gravestone")) return;

        Inventory inventory = event.getInventory();
        Gravestone gravestone = gravestoneManager.getGravestoneByInventory(inventory);

        if (gravestone != null && gravestone.getInventory().equals(inventory)) {
            boolean isEmpty = true;
            for (ItemStack item : inventory) {
                if (item != null && item.getType() != Material.AIR) {
                    isEmpty = false;
                    break;
                }
            }

            if (isEmpty) {
                gravestoneManager.removeGravestone(gravestone);
                event.getPlayer().sendMessage("§aYou fully looted the gravestone!");
            } else {
                gravestone.setLooting(false);
            }
        }
    }
}
