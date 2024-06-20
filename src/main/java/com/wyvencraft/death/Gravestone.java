package com.wyvencraft.death;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.MemorySection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Gravestone {
    private final UUID owner;
    private final Location location;
    private final Inventory inventory;
    private boolean unlocked;
    private final long created;

    public Gravestone(UUID owner, Location location, Inventory inventory, long created, boolean unlocked) {
        this.owner = owner;
        this.location = location;
        this.inventory = inventory;
        this.unlocked = unlocked;
        this.created = created;
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
}
