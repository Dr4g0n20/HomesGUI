package net.dr4g0n20.homesgui;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class HomeMenu implements CommandExecutor, Listener {

    private static final String MENU_TITLE = ChatColor.DARK_GREEN + "Your Homes";
    private static final int[] HOME_SLOTS = {10, 11, 12, 13, 14, 15, 16};

    private final HomesGUI plugin;
    private final HomeStorage storage;
    private final HomeTeleportManager teleportManager;
    private final Set<Integer> homeSlots = new HashSet<Integer>();

    public HomeMenu(HomesGUI plugin, HomeTeleportManager teleportManager) {
        this.plugin = plugin;
        this.storage = new HomeStorage(plugin);
        this.teleportManager = teleportManager;
        for (int slot : HOME_SLOTS) {
            homeSlots.add(slot);
        }
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

        ItemStack filler = createItem(Material.STAINED_GLASS_PANE, 1, (short) 7,
                ChatColor.GRAY + " ", Arrays.asList());

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }

        for (int i = 0; i < HOME_SLOTS.length; i++) {
            int homeNumber = i + 1;
            boolean saved = storage.hasHome(player, homeNumber);
            String homeName = storage.getHomeName(player, homeNumber);

            ItemStack bed = createItem(Material.BED, 1, (short) 0,
                    ChatColor.GREEN + homeName,
                    saved
                            ? Arrays.asList(
                                    ChatColor.GRAY + "Saved home",
                                    ChatColor.YELLOW + "Left-click to teleport",
                                    ChatColor.YELLOW + "Right-click to overwrite")
                            : Arrays.asList(
                                    ChatColor.GRAY + "Currently empty",
                                    ChatColor.YELLOW + "Click to save your location"));
            inventory.setItem(HOME_SLOTS[i], bed);
        }

        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (!MENU_TITLE.equals(event.getView().getTitle())) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        int slot = event.getRawSlot();
        if (!homeSlots.contains(slot)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int homeNumber = slot - HOME_SLOTS[0] + 1;

        if (!storage.hasHome(player, homeNumber)) {
            try {
                storage.saveHome(player, homeNumber, player.getLocation());
                player.sendMessage(ChatColor.GREEN + "Saved Home " + homeNumber + "!");
                openMenu(player);
            } catch (IOException exception) {
                player.sendMessage(ChatColor.RED + "Your home could not be saved. Please try again.");
                plugin.getLogger().severe("Failed to save home for " + player.getName() + ": "
                        + exception.getMessage());
            }
            return;
        }

        org.bukkit.Location destination = storage.getHome(player, homeNumber);
        if (destination == null) {
            player.sendMessage(ChatColor.RED + "That home is in a world that is not loaded.");
            return;
        }

        String homeName = storage.getHomeName(player, homeNumber);
        player.closeInventory();
        teleportManager.startTeleport(player, destination, homeName);
    }

    private ItemStack createItem(Material material, int amount, short durability,
            String displayName, java.util.List<String> lore) {
        ItemStack item = new ItemStack(material, amount, durability);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
