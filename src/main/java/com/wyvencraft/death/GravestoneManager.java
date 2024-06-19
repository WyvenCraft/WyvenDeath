package com.wyvencraft.death;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GravestoneManager {
    private final WyvenDeath plugin;
    private final Map<UUID, Gravestone> gravestones;

    private final File gravestonesFile;
    private final FileConfiguration gravestonesConfig;

    public GravestoneManager(WyvenDeath plugin) {
        this.plugin = plugin;
        this.gravestones = new HashMap<>();

        this.gravestonesFile = new File(plugin.getDataFolder(), "gravestones.yml");
        this.gravestonesConfig = YamlConfiguration.loadConfiguration(gravestonesFile);
    }

    public void createGravestone(UUID owner, Location location, Inventory inventory, long created) {
        createGravestone(owner, location, inventory, created, false);
    }


    public void createGravestone(UUID owner, Location location, Inventory inventory, long created, boolean unlocked) {
        Gravestone gravestone = new Gravestone(owner, location, inventory, created, unlocked);
        gravestones.put(gravestone.getOwner(), gravestone);

        // Create the gravestone block
        // TODO Change the block type to a custom gravestone block
        Block block = gravestone.getLocation().getBlock();
        block.setType(Material.STONE);

        // Unlock the gravestone after 5 minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                gravestone.unlock();
                Player owner = Bukkit.getPlayer(gravestone.getOwner());
                if (owner != null) owner.sendMessage("Your gravestone has been unlocked.");
            }
        }.runTaskLater(plugin, 200L);

        // Explode the gravestone after 10 minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                explodeGravestone(gravestone);
                Player owner = Bukkit.getPlayer(gravestone.getOwner());
                if (owner != null) owner.sendMessage("Your gravestone has been exploded.");
            }
        }.runTaskLater(plugin, 600L);
    }

    public void unloadGravestones() {
        for (Gravestone gravestone : gravestones.values()) {
            Block block = gravestone.getLocation().getBlock();
            if (block.getType() == Material.STONE) {
                block.setType(Material.AIR);
            }
        }

        gravestones.clear();
    }

    public Gravestone getGravestoneByLocation(Location location) {
        return gravestones.values().stream()
                .filter(g -> g.getLocation().equals(location))
                .findFirst()
                .orElse(null);
    }

    public void restrictMovement(Player player, Location deathLocation) {
        Location playerLocation = player.getLocation();
        double distance = playerLocation.distance(deathLocation);
        double maxRadius = 10.0;

        if (distance > maxRadius) {
            Vector direction = deathLocation.toVector().subtract(playerLocation.toVector()).normalize();
            player.setVelocity(direction.multiply(0.5));
        }
    }

    public void removeGravestoneFromConfig(UUID uuid) {
        gravestonesConfig.set(uuid.toString(), null);
        saveGravestones();
    }

    public void respawnPlayer(Player player, Location deathLocation) {
        int minRadius = 20;
        int maxRadius = 25;
        Location respawnLocation = player.getBedSpawnLocation();

        if (respawnLocation != null && respawnLocation.distance(deathLocation) <= maxRadius) {
            player.teleport(respawnLocation);
        } else {
            Random rand = new Random();
            int x = rand.nextInt((maxRadius - minRadius) + 1) + minRadius;
            int z = rand.nextInt((maxRadius - minRadius) + 1) + minRadius;
            respawnLocation = deathLocation.clone().add(x, 0, z);
            respawnLocation.setDirection(deathLocation.toVector().subtract(respawnLocation.toVector()));

            respawnLocation = respawnLocation.getWorld().getHighestBlockAt(respawnLocation).getLocation();
            player.teleport(respawnLocation);
        }

        player.sendMessage("You are " + player.getLocation().distance(deathLocation) + " blocks away from your gravestone.");

        player.removeMetadata("respawning", plugin);
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setInvulnerable(false);
        player.setInvisible(false);
    }

    public void explodeGravestone(Gravestone gravestone) {
        Inventory inventory = gravestone.getInventory();
        Location location = gravestone.getLocation();

        location.getWorld().createExplosion(location, 4.0F, false, false);

        for (ItemStack item : inventory.getContents()) {
            if (item != null) {
                location.getWorld().dropItemNaturally(location, item);
            }
        }

        Block block = location.getBlock();
        if (block.getType() == Material.STONE) {
            block.setType(Material.AIR);
        }

        removeGravestone(gravestone);
    }

    public void saveGravestones() {
        for (UUID uuid : gravestones.keySet()) {
            Gravestone gravestone = gravestones.get(uuid);
            ConfigurationSection section = gravestonesConfig.createSection(uuid.toString());
            section.set("location", gravestone.getLocation().serialize());
            section.set("inventory", gravestone.serializeInventory());
            section.set("unlocked", gravestone.isUnlocked());
            section.set("lifeTime", System.currentTimeMillis() - gravestone.getCreated());
        }
        try {
            gravestonesConfig.save(gravestonesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadGravestones() {
        for (String key : gravestonesConfig.getKeys(false)) {
            UUID uuid = UUID.fromString(key);
            ConfigurationSection section = gravestonesConfig.getConfigurationSection(key);
            Location location = Location.deserialize(section.getConfigurationSection("location").getValues(false));
            Inventory inventory = Gravestone.deserializeInventory(section.getConfigurationSection("inventory").getValues(false), 36);
            boolean unlocked = section.getBoolean("unlocked");
            long created = section.getLong("created");

            createGravestone(uuid, location, inventory, created, unlocked);
        }
    }

    public void removeGravestone(Gravestone gravestone) {
        gravestones.remove(gravestone.getOwner());
        removeGravestoneFromConfig(gravestone.getOwner());
    }

    public Gravestone getGravestoneByOwner(UUID uniqueId) {
        return gravestones.get(uniqueId);
    }
}
