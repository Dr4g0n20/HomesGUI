package net.dr4g0n20.homesgui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

public final class HomeTeleportManager implements Listener {

    private final HomesGUI plugin;
    private final Map<UUID, BukkitTask> countdownTasks = new HashMap<UUID, BukkitTask>();
    private final Map<UUID, Long> cooldowns = new HashMap<UUID, Long>();
    private final Map<UUID, UnsafeConfirmation> unsafeConfirmations = new HashMap<UUID, UnsafeConfirmation>();

    public HomeTeleportManager(HomesGUI plugin) {
        this.plugin = plugin;
    }

    public void startTeleport(Player player, Location destination, String homeName, Runnable onSuccess) {
        UUID uuid = player.getUniqueId();

        if (countdownTasks.containsKey(uuid)) {
            player.sendMessage(ChatColor.RED + "You already have a teleport countdown running.");
            return;
        }

        long now = System.currentTimeMillis();
        long cooldownEnd = cooldowns.containsKey(uuid) ? cooldowns.get(uuid) : 0L;
        if (cooldownEnd > now) {
            long secondsLeft = (cooldownEnd - now + 999L) / 1000L;
            player.sendMessage(ChatColor.RED + "You must wait " + secondsLeft
                    + " seconds before teleporting again.");
            return;
        }

        if (isUnsafe(destination)) {
            long window = Math.max(1, plugin.getConfig().getInt("unsafe-confirmation-window", 10)) * 1000L;
            UnsafeConfirmation previous = unsafeConfirmations.get(uuid);
            String destinationKey = destination.getWorld().getName() + ":" + destination.getBlockX()
                    + ":" + destination.getBlockY() + ":" + destination.getBlockZ();
            if (previous == null || !previous.destinationKey.equals(destinationKey)
                    || now - previous.timestamp > window) {
                unsafeConfirmations.put(uuid, new UnsafeConfirmation(destinationKey, now));
                player.sendMessage(ChatColor.RED + "Warning: " + ChatColor.YELLOW
                        + "This home may be unsafe (hazardous blocks or no safe floor).");
                player.sendMessage(ChatColor.YELLOW + "Click the same home again within "
                        + (window / 1000L) + " seconds to confirm teleporting.");
                return;
            }
            unsafeConfirmations.remove(uuid);
        } else {
            unsafeConfirmations.remove(uuid);
        }

        int delay = Math.max(0, plugin.getConfig().getInt("teleport-delay", 5));
        if (delay == 0) {
            finishTeleport(player, destination, homeName, onSuccess);
            return;
        }

        final int[] secondsRemaining = {delay};
        sendActionBar(player, ChatColor.GREEN + "Teleporting to " + homeName
                + ChatColor.GRAY + " in " + secondsRemaining[0] + "...");

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelCountdown(uuid);
                    return;
                }

                secondsRemaining[0]--;
                if (secondsRemaining[0] <= 0) {
                    cancelCountdown(uuid);
                    finishTeleport(player, destination, homeName);
                    return;
                }

                sendActionBar(player, ChatColor.GREEN + "Teleporting to " + homeName
                        + ChatColor.GRAY + " in " + secondsRemaining[0] + "...");
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
            }
        }, 20L, 20L);

        countdownTasks.put(uuid, task);
    }

    private static final class UnsafeConfirmation {
        private final String destinationKey;
        private final long timestamp;

        private UnsafeConfirmation(String destinationKey, long timestamp) {
            this.destinationKey = destinationKey;
            this.timestamp = timestamp;
        }
    }

    public boolean isUnsafe(Location location) {
        if (location == null || location.getWorld() == null) {
            return true;
        }

        Block feet = location.getBlock();
        Block head = feet.getRelative(0, 1, 0);
        Block floor = feet.getRelative(0, -1, 0);

        if (isHazard(feet.getType()) || isHazard(head.getType()) || isHazard(floor.getType())) {
            return true;
        }

        return floor.getType() == Material.AIR || floor.isLiquid()
                || feet.getType().isSolid() || head.getType().isSolid();
    }

    public String getUnsafeReason(Location location) {
        if (location == null || location.getWorld() == null) {
            return "The home world is unavailable.";
        }

        Block feet = location.getBlock();
        Block head = feet.getRelative(0, 1, 0);
        Block floor = feet.getRelative(0, -1, 0);

        if (isHazard(feet.getType())) return "Hazard at feet: " + feet.getType().name().toLowerCase().replace('_', ' ') + ".";
        if (isHazard(head.getType())) return "Hazard at head: " + head.getType().name().toLowerCase().replace('_', ' ') + ".";
        if (isHazard(floor.getType())) return "Hazard beneath you: " + floor.getType().name().toLowerCase().replace('_', ' ') + ".";
        if (floor.getType() == Material.AIR || floor.isLiquid()) return "There is no solid floor beneath this home.";
        if (feet.getType().isSolid()) return "The space where you would stand is obstructed.";
        if (head.getType().isSolid()) return "There is a block obstructing the space above you.";
        return "This location may be unsafe.";
    }

    private boolean isHazard(Material material) {
        return material == Material.LAVA
                || material == Material.STATIONARY_LAVA
                || material == Material.FIRE
                || material == Material.CACTUS
                || material == Material.MAGMA;
    }

    private void finishTeleport(Player player, Location destination, String homeName, Runnable onSuccess) {
        Location from = player.getLocation();
        if (!player.teleport(destination)) {
            player.sendMessage(ChatColor.RED + "Teleport failed.");
            return;
        }

        Location to = player.getLocation();
        playTeleportEffects(from);
        playTeleportEffects(to);

        if (onSuccess != null) {
            onSuccess.run();
        }

        int cooldown = Math.max(0, plugin.getConfig().getInt("teleport-cooldown", 30));
        if (cooldown > 0) {
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + cooldown * 1000L);
        }

        sendActionBar(player, ChatColor.GREEN + "Teleported to " + homeName + "!");
        player.sendMessage(ChatColor.GREEN + "Teleported to " + homeName + ".");
    }


    private void playTeleportEffects(Location location) {
        if (location == null || location.getWorld() == null) return;

        if (plugin.getConfig().getBoolean("teleport-effects.particles-enabled", true)) {
            String particleName = plugin.getConfig().getString("teleport-effects.particle", "PORTAL");
            int count = Math.max(0, plugin.getConfig().getInt("teleport-effects.particle-count", 40));
            try {
                Particle particle = Particle.valueOf(particleName.toUpperCase(java.util.Locale.ROOT));
                location.getWorld().spawnParticle(particle, location, count, 0.5, 1.0, 0.5, 0.2);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Unknown teleport particle '" + particleName + "'. Using PORTAL.");
                location.getWorld().spawnParticle(Particle.PORTAL, location, count, 0.5, 1.0, 0.5, 0.2);
            }
        }

        if (plugin.getConfig().getBoolean("teleport-effects.sound-enabled", true)) {
            String soundName = plugin.getConfig().getString("teleport-effects.sound", "ENTITY_ENDERMEN_TELEPORT");
            float volume = (float) plugin.getConfig().getDouble("teleport-effects.sound-volume", 1.0);
            float pitch = (float) plugin.getConfig().getDouble("teleport-effects.sound-pitch", 1.0);
            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase(java.util.Locale.ROOT));
                location.getWorld().playSound(location, sound, volume, pitch);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Unknown teleport sound '" + soundName + "'. Using ENTITY_ENDERMEN_TELEPORT.");
                location.getWorld().playSound(location, Sound.ENTITY_ENDERMEN_TELEPORT, volume, pitch);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!countdownTasks.containsKey(uuid)
                || !plugin.getConfig().getBoolean("cancel-on-move", true)
                || event.getTo() == null) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getWorld() != to.getWorld()
                || from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ()) {
            cancelCountdown(uuid);
            event.getPlayer().sendMessage(ChatColor.RED + "Teleport cancelled because you moved.");
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        UUID uuid = player.getUniqueId();
        if (countdownTasks.containsKey(uuid)
                && plugin.getConfig().getBoolean("cancel-on-damage", true)) {
            cancelCountdown(uuid);
            player.sendMessage(ChatColor.RED + "Teleport cancelled because you took damage.");
        }
    }

    private void cancelCountdown(UUID uuid) {
        BukkitTask task = countdownTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }
}
