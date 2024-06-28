package com.wyvencraft.death;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.MemorySection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Gravestone {
    // EXPLODING MUST ALWAYS BE GREATER THAN UNLOCKING
    private static final int EXPLODING_COUNTDOWN_SEC = 20;
    private static final int UNLOCKING_COUNTDOWN_SEC = 10;

    private final UUID owner;
    private final Location location;
    private final Inventory inventory;
    private boolean unlocked;
    private final long created;

    private boolean looting;

    BukkitTask task;

    public Gravestone(UUID owner, Location location, Inventory inventory, long created, boolean unlocked) {
        this.owner = owner;
        this.location = location;
        this.inventory = inventory;
        this.unlocked = unlocked;
        this.created = created;
        this.looting = false;
    }

    public void startTask(WyvenDeath plugin) {
        Player player = Bukkit.getPlayer(owner);

        task = new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (looting) return;

                count++;

                if (!isUnlocked() && count >= UNLOCKING_COUNTDOWN_SEC) {
                    unlock();
                    player.sendMessage("§cGravestone unlocked, for everyone to loot!");
                }

                if (count >= EXPLODING_COUNTDOWN_SEC) {
                    explodeGravestone(plugin);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy("§cGravestone exploded!"));
                }

                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy("§cGravestone will explode in " + (EXPLODING_COUNTDOWN_SEC - count) + " seconds."));
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void cancelTasks() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void explodeGravestone(WyvenDeath plugin) {
        GravestoneManager gravestoneManager = plugin.getGravestoneManager();
        gravestoneManager.explodeGravestone(this);
        cancelTasks();
    }

    public UUID getOwner() {
        return owner;
    }

    public long getCreated() {
        return created;
    }

    public Location getLocation() {
        return location;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void unlock() {
        this.unlocked = true;
    }

    public boolean isOwner(Player player) {
        return player.getUniqueId().equals(owner);
    }

    public Map<String, Object> serializeInventory() {
        Map<String, Object> serializedInventory = new HashMap<>();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null) {
                serializedInventory.put(String.valueOf(i), item.serialize());
            }
        }
        return serializedInventory;
    }

    public static Inventory deserializeInventory(Map<String, Object> serializedInventory, int size) {
        Inventory inventory = Bukkit.createInventory(null, size, "Gravestone");

        for (Map.Entry<String, Object> entry : serializedInventory.entrySet()) {
            int slot = Integer.parseInt(entry.getKey());
            ItemStack item = ItemStack.deserialize(((MemorySection) entry.getValue()).getValues(false));
            inventory.setItem(slot, item);
        }
        return inventory;
    }

    public void setLooting(boolean b) {
        this.looting = b;
    }

    public boolean isLooting() {
        return looting;
    }
}
