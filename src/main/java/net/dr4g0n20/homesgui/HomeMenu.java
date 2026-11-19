package net.dr4g0n20.homesgui;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class HomeMenu implements CommandExecutor, Listener {
    private static final String MENU_TITLE = ChatColor.DARK_GREEN + "Your Homes";
    private static final int[] HOME_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    private static final long CONFIRM_WINDOW_MS = 3000L;

    private final HomesGUI plugin;
    private final HomeStorage storage;
    private final HomeTeleportManager teleportManager;
    private final Set<Integer> homeSlots = new HashSet<Integer>();
    private final Map<String, Long> pendingActions = new HashMap<String, Long>();
    private final Map<UUID, Integer> pendingRenames = new HashMap<UUID, Integer>();

    public HomeMenu(HomesGUI plugin, HomeTeleportManager teleportManager) {
        this.plugin = plugin;
        this.storage = new HomeStorage(plugin);
        this.teleportManager = teleportManager;
        for (int slot : HOME_SLOTS) homeSlots.add(slot);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }
        openMenu((Player) sender);
        return true;
    }

    public void openMenu(Player player) {
        Inventory inventory = plugin.getServer().createInventory(null, 27, MENU_TITLE);
        ItemStack filler = createItem(Material.STAINED_GLASS_PANE, 1, (short) 7, ChatColor.GRAY + " ", Arrays.asList());
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, filler);

        for (int i = 0; i < HOME_SLOTS.length; i++) {
            int number = i + 1;
            boolean saved = storage.hasHome(player, number);
            String name = storage.getHomeName(player, number);
            org.bukkit.Location savedLocation = saved ? storage.getHome(player, number) : null;
            boolean worldUnavailable = saved && savedLocation == null;
            boolean unsafe = saved && (worldUnavailable || teleportManager.isUnsafe(savedLocation));
            short bedColor = saved ? (unsafe ? (short) 14 : (short) 13) : (short) 0;
            String bedNameColor = saved ? (unsafe ? ChatColor.RED.toString() : ChatColor.GREEN.toString()) : ChatColor.WHITE.toString();

            java.util.List<String> lore = new java.util.ArrayList<String>();
            if (!saved) {
                lore.add(ChatColor.GRAY + "Currently empty");
                lore.add(ChatColor.YELLOW + "Click to save your location");
            } else {
                lore.add(unsafe ? ChatColor.RED + "Unsafe home" : ChatColor.GREEN + "Safe home");
                if (worldUnavailable) {
                    lore.add(ChatColor.RED + "World is not currently loaded.");
                } else {
                    lore.add(ChatColor.GRAY + "World: " + ChatColor.WHITE + savedLocation.getWorld().getName());
                    String dimension;
                    switch (savedLocation.getWorld().getEnvironment()) {
                        case NETHER:
                            dimension = "Nether";
                            break;
                        case THE_END:
                            dimension = "The End";
                            break;
                        default:
                            dimension = "Overworld";
                            break;
                    }
                    lore.add(ChatColor.GRAY + "Dimension: " + ChatColor.WHITE + dimension);
                    org.bukkit.block.Biome biome = savedLocation.getWorld().getBiome(
                            savedLocation.getBlockX(), savedLocation.getBlockZ());
                    lore.add(ChatColor.GRAY + "Biome: " + ChatColor.WHITE + formatBiome(biome.name()));
                    lore.add(ChatColor.GRAY + "Coordinates: " + ChatColor.WHITE
                            + "X: " + savedLocation.getBlockX() + " | Y: " + savedLocation.getBlockY()
                            + " | Z: " + savedLocation.getBlockZ());
                    if (savedLocation.getWorld().equals(player.getWorld())) {
                        double distance = player.getLocation().distance(savedLocation);
                        lore.add(ChatColor.GRAY + "Distance: " + ChatColor.WHITE
                                + String.format(java.util.Locale.US, "%.1f blocks away", distance));
                    } else {
                        lore.add(ChatColor.GRAY + "Distance: " + ChatColor.WHITE + "Different world");
                    }
                    long createdAt = storage.getCreatedAt(player, number);
                    if (createdAt > 0L) {
                        lore.add(ChatColor.GRAY + "Created: " + ChatColor.WHITE
                                + new java.text.SimpleDateFormat("dd MMM yyyy").format(new java.util.Date(createdAt)));
                    } else {
                        lore.add(ChatColor.GRAY + "Created: " + ChatColor.WHITE + "Not recorded (older home)");
                    }
                    long lastVisited = storage.getLastVisited(player, number);
                    lore.add(ChatColor.GRAY + "Last visited: " + ChatColor.WHITE
                            + (lastVisited > 0L ? formatTimeSince(lastVisited) : "Never"));
                    if (unsafe) {
                        lore.add(ChatColor.RED + teleportManager.getUnsafeReason(savedLocation));
                    } else {
                        lore.add(ChatColor.GREEN + "No detected hazards.");
                    }
                }
                lore.add(ChatColor.YELLOW + "Left-click: teleport");
                lore.add(ChatColor.YELLOW + "Right-click twice: overwrite");
                lore.add(ChatColor.YELLOW + "Shift + right-click twice: delete");
                lore.add(ChatColor.YELLOW + "Click then press Q: rename");
            }
            ItemStack bed = createItem(Material.BED, 1, bedColor, bedNameColor + name, lore);
            inventory.setItem(HOME_SLOTS[i], bed);
        }
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player) || !MENU_TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) return;
        int slot = event.getRawSlot();
        if (!homeSlots.contains(slot)) return;
        Player player = (Player) event.getWhoClicked();
        int number = slot - HOME_SLOTS[0] + 1;

        if (event.getClick() == ClickType.DROP || event.getClick() == ClickType.CONTROL_DROP) {
            if (!storage.hasHome(player, number)) {
                player.sendMessage(ChatColor.RED + "You cannot rename an empty home.");
                return;
            }
            pendingRenames.put(player.getUniqueId(), number);
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Type the new home name in chat, or type cancel.");
            return;
        }

        if (!storage.hasHome(player, number)) {
            try {
                storage.saveHome(player, number, player.getLocation());
                player.sendMessage(ChatColor.GREEN + "Saved Home " + number + "!");
                openMenu(player);
            } catch (IOException ex) {
                player.sendMessage(ChatColor.RED + "Your home could not be saved.");
                plugin.getLogger().severe("Failed to save home: " + ex.getMessage());
            }
            return;
        }

        if (event.getClick().isLeftClick()) {
            org.bukkit.Location destination = storage.getHome(player, number);
            if (destination == null) {
                player.sendMessage(ChatColor.RED + "That home is in a world that is not loaded.");
                return;
            }
            String name = storage.getHomeName(player, number);
            player.closeInventory();
            teleportManager.startTeleport(player, destination, name, new Runnable() {
                @Override
                public void run() {
                    try {
                        storage.markHomeVisited(player, number);
                    } catch (IOException ex) {
                        plugin.getLogger().warning("Could not update last-visited time: " + ex.getMessage());
                    }
                }
            });
            return;
        }

        if (!event.getClick().isRightClick()) return;
        boolean delete = event.isShiftClick();
        String action = delete ? "delete" : "overwrite";
        String key = player.getUniqueId() + ":" + number + ":" + action;
        long now = System.currentTimeMillis();
        Long previous = pendingActions.get(key);

        if (previous != null && now - previous <= CONFIRM_WINDOW_MS) {
            pendingActions.remove(key);
            try {
                if (delete) {
                    storage.deleteHome(player, number);
                    player.sendMessage(ChatColor.GREEN + "Deleted Home " + number + ".");
                } else {
                    storage.saveHome(player, number, player.getLocation());
                    player.sendMessage(ChatColor.GREEN + "Overwrote Home " + number + ".");
                }
                openMenu(player);
            } catch (IOException ex) {
                player.sendMessage(ChatColor.RED + "Could not update that home.");
                plugin.getLogger().severe("Home action failed: " + ex.getMessage());
            }
        } else {
            pendingActions.put(key, now);
            player.sendMessage(delete
                    ? ChatColor.RED + "Shift + right-click this home again within 3 seconds to delete it."
                    : ChatColor.YELLOW + "Right-click this home again within 3 seconds to overwrite it.");
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        final Integer number = pendingRenames.remove(event.getPlayer().getUniqueId());
        if (number == null) return;
        event.setCancelled(true);
        final Player player = event.getPlayer();
        final String name = ChatColor.stripColor(event.getMessage()).trim();
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (name.equalsIgnoreCase("cancel")) {
                    player.sendMessage(ChatColor.GRAY + "Home renaming cancelled.");
                    openMenu(player);
                    return;
                }
                if (name.isEmpty() || name.length() > 32) {
                    player.sendMessage(ChatColor.RED + "Home names must be between 1 and 32 characters.");
                    openMenu(player);
                    return;
                }
                try {
                    storage.renameHome(player, number, name);
                    player.sendMessage(ChatColor.GREEN + "Home renamed to " + name + ".");
                    openMenu(player);
                } catch (IOException ex) {
                    player.sendMessage(ChatColor.RED + "Could not rename that home.");
                    plugin.getLogger().severe("Home rename failed: " + ex.getMessage());
                }
            }
        });
    }

    private String formatBiome(String biomeName) {
        String[] words = biomeName.toLowerCase(java.util.Locale.US).split("_");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (formatted.length() > 0) formatted.append(' ');
            formatted.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return formatted.toString();
    }

    private String formatTimeSince(long timestamp) {
        long elapsed = Math.max(0L, System.currentTimeMillis() - timestamp);
        long minutes = elapsed / 60000L;
        if (minutes < 1L) return "Just now";
        if (minutes < 60L) return minutes + (minutes == 1L ? " minute ago" : " minutes ago");
        long hours = minutes / 60L;
        if (hours < 24L) return hours + (hours == 1L ? " hour ago" : " hours ago");
        long days = hours / 24L;
        if (days < 30L) return days + (days == 1L ? " day ago" : " days ago");
        long months = days / 30L;
        if (months < 12L) return months + (months == 1L ? " month ago" : " months ago");
        long years = days / 365L;
        return years + (years == 1L ? " year ago" : " years ago");
    }

    private ItemStack createItem(Material material, int amount, short durability, String displayName, java.util.List<String> lore) {
        ItemStack item = new ItemStack(material, amount, durability);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
