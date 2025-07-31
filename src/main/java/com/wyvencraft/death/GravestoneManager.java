package com.wyvencraft.death;

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
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class GravestoneManager {
    private final WyvenDeath plugin;
    private final Map<Location, Gravestone> gravestones;

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
        gravestone.startTask(plugin);
        gravestones.put(location, gravestone);

        // TODO Change the block type to a custom gravestone block
        Block block = gravestone.getLocation().getBlock();
        block.setType(Material.STONE);
    }

    public void unloadGravestones() {
        for (Gravestone gravestone : gravestones.values()) {
            gravestone.cancelTasks();
            Block block = gravestone.getLocation().getBlock();
            if (block.getType() == Material.STONE) {
                block.setType(Material.AIR);
            }
        }

        gravestones.clear();
    }

    public Gravestone getGravestoneByLocation(Location location) {
        return gravestones.getOrDefault(location, null);
    }

    public void removeGravestoneFromConfig(UUID uuid) {
        gravestonesConfig.set(uuid.toString(), null);
        saveGravestones();
    }

    public void respawnPlayer(Player player, Location deathLocation) {
        int minRadius = 20;
        int maxRadius = 25;
        Location respawnLocation = player.getRespawnLocation();

        if (respawnLocation != null && respawnLocation.distance(deathLocation) <= maxRadius) {
            player.teleport(respawnLocation);
        } else {
            Random rand = new Random();
            int x = rand.nextInt((maxRadius - minRadius) + 1) + minRadius;
            int z = rand.nextInt((maxRadius - minRadius) + 1) + minRadius;
            respawnLocation = deathLocation.clone().add(x, 0, z);

            respawnLocation = respawnLocation.getWorld().getHighestBlockAt(respawnLocation).getLocation();

            Vector direction = deathLocation.toVector().subtract(respawnLocation.toVector());
            respawnLocation.setDirection(direction);

            player.teleport(respawnLocation);
        }

//        Display the distance between the player and the gravestone with 2 decimal places
        double distance = player.getLocation().distance(deathLocation);
        player.sendMessage("§7You respawned §a" + String.format("%.2f", distance) + " §7blocks away from your death location.");

        spectator(player, false);
    }

    public void spectator(Player player, boolean enable) {
        if (enable) {
            player.setMetadata("respawning", new FixedMetadataValue(plugin, true));

            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlight(true);
            player.setFlying(true);
            player.setInvulnerable(true);
            player.setInvisible(true);
        } else {
            player.removeMetadata("respawning", plugin);
            player.setGameMode(GameMode.SURVIVAL);
            player.setAllowFlight(false);
            player.setFlying(false);
            player.setInvulnerable(false);
            player.setInvisible(false);
        }
    }

    public void explodeGravestone(Gravestone gravestone) {
        Inventory inventory = gravestone.getInventory();
        Location location = gravestone.getLocation();

        if (location == null || location.getWorld() == null) {
            return; // Invalid location, cannot explode
        }

        if (location.getWorld() != null) {
            location.getWorld().createExplosion(location, 4.0F, false, false);

            for (ItemStack item : inventory.getContents()) {
                if (item != null) {
                    location.getWorld().dropItemNaturally(location, item);
                }
            }
        }
        
        Block block = location.getBlock();
        if (block.getType() == Material.STONE) {
            block.setType(Material.AIR);
        }

        removeGravestone(gravestone);
    }

    public void saveGravestones() {
        for (Map.Entry<Location, Gravestone> entry : gravestones.entrySet()) {
            Gravestone gravestone = entry.getValue();
            ConfigurationSection section = gravestonesConfig.createSection(gravestone.getOwner().toString());
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
        gravestone.getLocation().getBlock().setType(Material.AIR);
        gravestone.cancelTasks();
        gravestones.remove(gravestone.getLocation());
        removeGravestoneFromConfig(gravestone.getOwner());
    }

    public Gravestone[] getGravestoneByOwner(UUID uniqueId) {
        return gravestones.values().stream()
                .filter(gravestone -> gravestone.getOwner().equals(uniqueId))
                .toArray(Gravestone[]::new);
    }

    public Gravestone getGravestoneByInventory(Inventory inventory) {
        return gravestones.values().stream()
                .filter(gravestone -> gravestone.getInventory().equals(inventory))
                .findFirst()
                .orElse(null);
    }
}
