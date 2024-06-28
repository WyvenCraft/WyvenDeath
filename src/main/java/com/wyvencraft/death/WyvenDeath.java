package com.wyvencraft.death;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class WyvenDeath extends JavaPlugin {
    private final GravestoneManager gravestoneManager = new GravestoneManager(this);

    @Override
    public void onEnable() {
//         // Register command
        this.getCommand("kill").setExecutor(new KillCommand());

        registerListeners();

        // Load gravestones from file
        new BukkitRunnable() {
            @Override
            public void run() {
                gravestoneManager.loadGravestones();
            }
        }.runTaskLater(this, 20L);
    }

    private void registerListeners() {
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new DeathListener(this, gravestoneManager), this);
        pm.registerEvents(new GravestoneListener(this, gravestoneManager), this);
    }

    @Override
    public void onDisable() {
        // Save gravestones to file
        gravestoneManager.saveGravestones();
        gravestoneManager.unloadGravestones();
    }

    public GravestoneManager getGravestoneManager() {
        return gravestoneManager;
    }
}
