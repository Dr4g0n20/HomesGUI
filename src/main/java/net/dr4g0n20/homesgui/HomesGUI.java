package net.dr4g0n20.homesgui;

import org.bukkit.plugin.java.JavaPlugin;

public final class HomesGUI extends JavaPlugin {

    private HomeMenu homeMenu;
    private HomeTeleportManager teleportManager;
    private OwnerCommands ownerCommands;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        teleportManager = new HomeTeleportManager(this);
        homeMenu = new HomeMenu(this, teleportManager);

        getCommand("home").setExecutor(homeMenu);
        getCommand("homes").setExecutor(homeMenu);

        ownerCommands = new OwnerCommands(this);
        for (String commandName : new String[] {"dheal", "dfeed", "dcoords", "dgive", "dnofall", "dfireres", "dseed", "dlevels", "dlocate", "dsummon", "dfly", "drepair", "dinfo", "dextinguish", "dspawn", "dweather", "dday", "dnight", "dhelp"}) {
            getCommand(commandName).setExecutor(ownerCommands);
        }
        getServer().getPluginManager().registerEvents(ownerCommands, this);
        ownerCommands.hideCommandsFromHelp();

        getServer().getPluginManager().registerEvents(homeMenu, this);
        getServer().getPluginManager().registerEvents(teleportManager, this);

        getLogger().info("HomesGUI has been enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("HomesGUI has been disabled.");
    }
}
