package io.github.edwardlcl.warden.command;

import io.github.edwardlcl.warden.WardenPlugin;
import io.github.edwardlcl.warden.detection.DupeDetector;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class WardenCommand implements CommandExecutor {

    private final WardenPlugin plugin;
    private final DupeDetector dupeDetector;
    private final long startTimeMillis = System.currentTimeMillis();

    public WardenCommand(WardenPlugin plugin, DupeDetector dupeDetector) {
        this.plugin = plugin;
        this.dupeDetector = dupeDetector;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("warden.admin")) {
            sender.sendMessage(ChatColor.RED + "You lack permission to use Warden.");
            return true;
        }
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "status" -> handleStatus(sender);
            case "reload" -> handleReload(sender);
            case "stats" -> handleStats(sender);
            default -> sendUsage(sender);
        }
        return true;
    }

    private void handleStatus(CommandSender sender) {
        long uptimeSeconds = (System.currentTimeMillis() - startTimeMillis) / 1000L;
        int totalFlags =
                dupeDetector.getFlagCounts().values().stream().mapToInt(AtomicInteger::get).sum();
        sender.sendMessage(ChatColor.GREEN + "=== Warden Status ===");
        sender.sendMessage(ChatColor.YELLOW + "Uptime: " + ChatColor.WHITE + uptimeSeconds + "s");
        sender.sendMessage(
                ChatColor.YELLOW + "Threshold: " + ChatColor.WHITE
                        + dupeDetector.getThreshold() + " items/s");
        sender.sendMessage(ChatColor.YELLOW + "Total flags: " + ChatColor.WHITE + totalFlags);
        sender.sendMessage(
                ChatColor.YELLOW + "Players flagged: " + ChatColor.WHITE
                        + dupeDetector.getFlagCounts().size());
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "[Warden] Config reloaded.");
    }

    private void handleStats(CommandSender sender) {
        Map<UUID, AtomicInteger> flags = dupeDetector.getFlagCounts();
        if (flags.isEmpty()) {
            sender.sendMessage(ChatColor.GREEN + "[Warden] No players flagged.");
            return;
        }
        sender.sendMessage(ChatColor.GREEN + "=== Warden Per-Player Flags ===");
        for (Map.Entry<UUID, AtomicInteger> entry : flags.entrySet()) {
            OfflinePlayer player = plugin.getServer().getOfflinePlayer(entry.getKey());
            String name = player.getName() != null ? player.getName() : entry.getKey().toString();
            sender.sendMessage(
                    ChatColor.YELLOW + name + ": " + ChatColor.WHITE + entry.getValue().get()
                            + " flags");
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GRAY + "Usage: /warden <status|reload|stats>");
    }
}
