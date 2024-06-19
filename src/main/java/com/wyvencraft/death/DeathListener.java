package com.wyvencraft.death;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.Objects;

public class DeathListener implements Listener {
    private final WyvenDeath plugin;
    private final GravestoneManager gravestoneManager;

    public DeathListener(WyvenDeath plugin, GravestoneManager gravestoneManager) {
        this.plugin = plugin;
        this.gravestoneManager = gravestoneManager;
    }

//    @EventHandler
//    public void onPlayerDeathForce(PlayerDeathEvent event) {
//        EntityDamageEvent entityDamageEvent = new EntityDamageEvent(event.getEntity(), EntityDamageEvent.DamageCause.CUSTOM, DamageSource.builder(DamageType.GENERIC).build(), event.getEntity().getHealth() + 1);
//        Bukkit.getPluginManager().callEvent(entityDamageEvent);
//    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        if (player.getHealth() - event.getFinalDamage() > 0) return;
        System.out.println(player.hasMetadata("respawning"));

        event.setCancelled(true);

        Location deathLocation = player.getLocation();

        if (!player.getInventory().isEmpty()) {
            Inventory gravestoneInventory = plugin.getServer().createInventory(null, 27, "Gravestone");

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
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.setMetadata("respawning", new FixedMetadataValue(plugin, true));

        // Set player to adventure mode and allow flight
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setInvulnerable(true);
        player.setInvisible(true);

//        Send a message to the player with coordinates of the gravestone
        player.sendMessage("Your gravestone has been placed at " + deathLocation.getBlockX() + ", " + deathLocation.getBlockY() + ", " + deathLocation.getBlockZ());
//        how many blocks away from the gravestone the player is

        // Restrict player's movement and handle respawn
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.hasMetadata("respawning")) {
                    gravestoneManager.restrictMovement(player, deathLocation);
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

        System.out.println(player.hasMetadata("respawning"));

        if (player.hasMetadata("respawning")) {
            Gravestone gravestone = gravestoneManager.getGravestoneByOwner(player.getUniqueId());
            gravestoneManager.respawnPlayer(player, gravestone.getLocation());
        }
    }
}
