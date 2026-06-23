package io.github.edwardlcl.warden.log;

import io.github.edwardlcl.warden.WardenPlugin;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Appends inventory-state changes to a rolling JSON-lines file. One JSON object per line:
 * {"ts":..,"player":..,"event":..,"item":..,"qty":..,"location":..}
 */
public class TransactionLogger {

    private final WardenPlugin plugin;
    private final boolean enabled;
    private Writer writer;

    public TransactionLogger(WardenPlugin plugin) {
        this.plugin = plugin;
        this.enabled =
                plugin.getConfig().getBoolean("logging.log-to-file", true)
                        && plugin.getConfig().getBoolean("logging.log-transactions", true);
        if (enabled) {
            open();
        }
    }

    private void open() {
        try {
            File dir = plugin.getDataFolder();
            if (!dir.exists() && !dir.mkdirs()) {
                plugin.getLogger().warning("Could not create Warden data folder; disabling file log.");
                return;
            }
            File logFile = new File(dir, "warden-transactions.jsonl");
            this.writer =
                    Files.newBufferedWriter(
                            logFile.toPath(),
                            StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.APPEND);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to open transaction log: " + e.getMessage());
            this.writer = null;
        }
    }

    public void logItemPickup(Player player, String item, int qty, Location location) {
        write("item_pickup", player.getName(), item, qty, location);
    }

    public void logItemDrop(Player player, String item, int qty, Location location) {
        write("item_drop", player.getName(), item, qty, location);
    }

    public void logContainerOpen(Player player, Location location) {
        write("container_open", player.getName(), null, 0, location);
    }

    public void logContainerClose(Player player, Location location) {
        write("container_close", player.getName(), null, 0, location);
    }

    public void logPlayerDeath(Player player, Location location) {
        write("player_death", player.getName(), null, 0, location);
    }

    private synchronized void write(
            String event, String player, String item, int qty, Location location) {
        if (writer == null) {
            return;
        }
        try {
            writer.write(toJsonLine(event, player, item, qty, location));
            writer.write('\n');
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write transaction log line: " + e.getMessage());
        }
    }

    static String toJsonLine(String event, String player, String item, int qty, Location location) {
        StringBuilder sb = new StringBuilder(128);
        sb.append('{');
        sb.append("\"ts\":").append(System.currentTimeMillis()).append(',');
        sb.append("\"player\":\"").append(escape(player)).append("\",");
        sb.append("\"event\":\"").append(escape(event)).append("\",");
        sb.append("\"item\":").append(item == null ? "null" : "\"" + escape(item) + "\"").append(',');
        sb.append("\"qty\":").append(qty).append(',');
        sb.append("\"location\":").append(formatLocation(location));
        sb.append('}');
        return sb.toString();
    }

    private static String formatLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return "null";
        }
        return String.format(
                "\"%s(%d,%d,%d)\"",
                escape(location.getWorld().getName()),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }

    static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    public synchronized void flush() {
        if (writer != null) {
            try {
                writer.flush();
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to flush transaction log: " + e.getMessage());
            }
        }
    }

    public synchronized void close() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to close transaction log: " + e.getMessage());
            }
            writer = null;
        }
    }
}
