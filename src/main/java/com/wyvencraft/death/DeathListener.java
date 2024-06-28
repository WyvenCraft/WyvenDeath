package com.wyvencraft.death;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class DeathListener implements Listener {
    private final WyvenDeath plugin;
    private final GravestoneManager gravestoneManager;

    public DeathListener(WyvenDeath plugin, GravestoneManager gravestoneManager) {
        this.plugin = plugin;
        this.gravestoneManager = gravestoneManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (player.getHealth() - event.getFinalDamage() > 0) return;

        event.setCancelled(true);

        Location deathLocation = player.getLocation().getBlock().getLocation();

        if (!player.getInventory().isEmpty()) {
            Inventory gravestoneInventory = Bukkit.createInventory(null, 27, "Gravestone");

            // Store player's items in the virtual inventory
            player.getInventory().forEach(item -> {
                if (item != null) {
                    gravestoneInventory.addItem(item);
                }
            });

            // Clear player's inventory
            player.getInventory().clear();

            // Create and manage the gravestone
            gravestoneManager.createGravestone(player.getUniqueId(), deathLocation, gravestoneInventory, System.currentTimeMillis());
        }

        // Set player metadata
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20);

        gravestoneManager.spectator(player, true);

//        Send a message to the player with coordinates of the gravestone
        player.sendMessage("You died at " + deathLocation.getBlockX() + ", " + deathLocation.getBlockY() + ", " + deathLocation.getBlockZ());

        // Restrict player's movement and handle respawn
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.hasMetadata("respawning")) {
                    restrictMovement(player, deathLocation);
                } else {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Respawn player after 10 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                gravestoneManager.respawnPlayer(player, deathLocation);
            }
        }.runTaskLater(plugin, 100L);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (player.hasMetadata("respawning")) {
            Gravestone[] gravestones = gravestoneManager.getGravestoneByOwner(player.getUniqueId());
            gravestoneManager.respawnPlayer(player, gravestones[0].getLocation());
        }
    }

    private void restrictMovement(Player player, Location deathLocation) {
        Location playerLocation = player.getLocation();
        double distance = playerLocation.distance(deathLocation);
        double maxRadius = 10.0;

        if (distance > maxRadius) {
            Vector direction = deathLocation.toVector().subtract(playerLocation.toVector()).normalize();
            player.setVelocity(direction.multiply(0.5));
        }
    }
}
