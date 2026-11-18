package net.dr4g0n20.homesgui;

import org.bukkit.plugin.java.JavaPlugin;

public final class HomesGUI extends JavaPlugin {

    private HomeMenu homeMenu;
    private HomeTeleportManager teleportManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        teleportManager = new HomeTeleportManager(this);
        homeMenu = new HomeMenu(this, teleportManager);

        getCommand("home").setExecutor(homeMenu);
        getCommand("homes").setExecutor(homeMenu);

        getServer().getPluginManager().registerEvents(homeMenu, this);
        getServer().getPluginManager().registerEvents(teleportManager, this);

        getLogger().info("HomesGUI has been enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("HomesGUI has been disabled.");
    }
}
