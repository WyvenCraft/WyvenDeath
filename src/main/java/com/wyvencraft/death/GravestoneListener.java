package com.wyvencraft.death;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;

public class GravestoneListener implements Listener {

    private final WyvenDeath plugin;
    private final GravestoneManager gravestoneManager;

    public GravestoneListener(WyvenDeath plugin, GravestoneManager gravestoneManager) {
        this.plugin = plugin;
        this.gravestoneManager = gravestoneManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();

        if (block == null || block.getType() != Material.STONE) return;

        Player player = event.getPlayer();
        Gravestone gravestone = gravestoneManager.getGravestoneByLocation(block.getLocation());
        System.out.println("Player interacted with gravestone");

        if (gravestone != null) {
            if (gravestone.isUnlocked() || gravestone.isOwner(player)) {
                player.openInventory(gravestone.getInventory());
            } else {
                player.sendMessage("This gravestone is locked. Please wait until it unlocks.");
            }
        }

    }

    @EventHandler
    public void onGravestoneClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        Location location = event.getPlayer().getLocation();

        Gravestone gravestone = gravestoneManager.getGravestoneByLocation(location);

        if (gravestone != null && gravestone.getInventory().equals(inventory)) {
            boolean isEmpty = true;
            for (int i = 0; i < inventory.getSize(); i++) {
                if (inventory.getItem(i) != null) {
                    isEmpty = false;
                    break;
                }
            }

            if (isEmpty) {
                gravestoneManager.removeGravestone(gravestone);
                location.getBlock().setType(Material.AIR);

                event.getPlayer().sendMessage("§aYou fully looted the gravestone!");
            }
        }
    }
}
