package net.dr4g0n20.homesgui;

import org.bukkit.plugin.java.JavaPlugin;

public final class HomesGUI extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("HomesGUI has been enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("HomesGUI has been disabled.");
    }
}
