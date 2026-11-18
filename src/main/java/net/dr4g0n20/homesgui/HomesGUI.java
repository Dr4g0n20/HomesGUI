package net.dr4g0n20.homesgui;

import org.bukkit.plugin.java.JavaPlugin;

public final class HomesGUI extends JavaPlugin {

    private HomeMenu homeMenu;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        homeMenu = new HomeMenu(this);
        getCommand("home").setExecutor(homeMenu);
        getCommand("homes").setExecutor(homeMenu);
        getServer().getPluginManager().registerEvents(homeMenu, this);

        getLogger().info("HomesGUI has been enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("HomesGUI has been disabled.");
    }
}
