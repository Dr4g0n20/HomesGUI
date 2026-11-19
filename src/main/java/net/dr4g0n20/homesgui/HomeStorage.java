package net.dr4g0n20.homesgui;

import java.io.File;
import java.io.IOException;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public final class HomeStorage {
    private final HomesGUI plugin;
    private final File homesFolder;

    public HomeStorage(HomesGUI plugin) {
        this.plugin = plugin;
        this.homesFolder = new File(plugin.getDataFolder(), "homes");
        if (!homesFolder.exists() && !homesFolder.mkdirs()) plugin.getLogger().warning("Could not create homes data folder.");
    }

    private File getPlayerFile(Player player) {
        return new File(homesFolder, player.getUniqueId().toString() + ".yml");
    }

    public boolean hasHome(Player player, int homeNumber) {
        if (homeNumber < 1 || homeNumber > 7) return false;
        File file = getPlayerFile(player);
        if (!file.exists()) return false;
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        return data.contains("homes." + homeNumber + ".world");
    }

    public long getLastVisited(Player player, int homeNumber) {
        File file = getPlayerFile(player);
        if (!file.exists()) return 0L;
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        return data.getLong("homes." + homeNumber + ".last-visited", 0L);
    }

    public void markHomeVisited(Player player, int homeNumber) throws IOException {
        File file = getPlayerFile(player);
        if (!file.exists()) throw new IOException("Player homes file does not exist.");
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        String path = "homes." + homeNumber;
        if (!data.contains(path + ".world")) throw new IOException("Home is not saved.");
        data.set(path + ".last-visited", System.currentTimeMillis());
        data.save(file);
    }

    public Location getHome(Player player, int homeNumber) {
        if (homeNumber < 1 || homeNumber > 7) return null;
        File file = getPlayerFile(player);
        if (!file.exists()) return null;
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        String path = "homes." + homeNumber;
        if (!data.contains(path + ".world")) return null;
        String worldName = data.getString(path + ".world");
        if (plugin.getServer().getWorld(worldName) == null) return null;
        return new Location(plugin.getServer().getWorld(worldName), data.getDouble(path + ".x"),
                data.getDouble(path + ".y"), data.getDouble(path + ".z"),
                (float) data.getDouble(path + ".yaw"), (float) data.getDouble(path + ".pitch"));
    }

    public String getHomeName(Player player, int homeNumber) {
        File file = getPlayerFile(player);
        if (!file.exists()) return "Home " + homeNumber;
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        return data.getString("homes." + homeNumber + ".name", "Home " + homeNumber);
    }

    public void saveHome(Player player, int homeNumber, Location location) throws IOException {
        if (homeNumber < 1 || homeNumber > 7) throw new IllegalArgumentException("Home number must be between 1 and 7.");
        File file = getPlayerFile(player);
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        String path = "homes." + homeNumber;
        data.set(path + ".name", data.getString(path + ".name", "Home " + homeNumber));
        data.set(path + ".world", location.getWorld().getName());
        data.set(path + ".x", location.getX());
        data.set(path + ".y", location.getY());
        data.set(path + ".z", location.getZ());
        data.set(path + ".yaw", location.getYaw());
        data.set(path + ".pitch", location.getPitch());
        data.save(file);
    }

    public void renameHome(Player player, int homeNumber, String name) throws IOException {
        File file = getPlayerFile(player);
        if (!file.exists()) throw new IOException("Player homes file does not exist.");
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        String path = "homes." + homeNumber;
        if (!data.contains(path + ".world")) throw new IOException("Home is not saved.");
        data.set(path + ".name", name);
        data.save(file);
    }

    public void deleteHome(Player player, int homeNumber) throws IOException {
        File file = getPlayerFile(player);
        if (!file.exists()) return;
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        data.set("homes." + homeNumber, null);
        data.save(file);
    }
}
