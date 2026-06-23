package io.github.edwardlcl.warden.detection;

import io.github.edwardlcl.warden.WardenPlugin;
import io.github.edwardlcl.warden.alert.AlertManager;
import io.github.edwardlcl.warden.log.TransactionLogger;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Detects abnormal rates of item acquisition that indicate duplication exploits. Uses a
 * {@link SlidingWindowCounter} over a one-second window and flags players who gain more than the
 * configured threshold.
 */
public class DupeDetector implements Listener {

    private static final long WINDOW_MILLIS = 1000L;

    private final WardenPlugin plugin;
    private final AlertManager alertManager;
    private final TransactionLogger transactionLogger;
    private final SlidingWindowCounter counter = new SlidingWindowCounter(WINDOW_MILLIS);
    private final Map<UUID, AtomicInteger> flagCounts = new HashMap<>();

    private final int threshold;

    public DupeDetector(
            WardenPlugin plugin, AlertManager alertManager, TransactionLogger transactionLogger) {
        this.plugin = plugin;
        this.alertManager = alertManager;
        this.transactionLogger = transactionLogger;
        this.threshold = plugin.getConfig().getInt("thresholds.max-items-gained-per-second", 64);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Item item = event.getItem();
        ItemStack stack = item.getItemStack();
        int gained = stack.getAmount();
        transactionLogger.logItemPickup(player, stack.getType().name(), gained, player.getLocation());
        evaluate(player, stack.getType().name(), gained, player.getLocation());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType().isAir()) {
            return;
        }
        // A shift-click or pickup-all can move a full stack into the player's possession.
        int gained = current.getAmount();
        evaluate(player, current.getType().name(), gained, player.getLocation());
    }

    private void evaluate(Player player, String itemType, int gained, Location location) {
        if (gained <= 0) {
            return;
        }
        int windowTotal = counter.record(player.getUniqueId(), gained, System.currentTimeMillis());
        if (windowTotal > threshold) {
            flag(player, itemType, windowTotal, location);
        }
    }

    private void flag(Player player, String itemType, int windowTotal, Location location) {
        flagCounts.computeIfAbsent(player.getUniqueId(), k -> new AtomicInteger()).incrementAndGet();
        String context =
                String.format(
                        "%s gained %d %s within 1s (threshold %d) at %s",
                        player.getName(),
                        windowTotal,
                        itemType,
                        threshold,
                        formatLocation(location));
        alertManager.alert(player.getUniqueId(), player.getName(), "DUPE", context);
    }

    private static String formatLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return "unknown";
        }
        return String.format(
                "%s(%d,%d,%d)",
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }

    public int getFlagCount(UUID player) {
        AtomicInteger count = flagCounts.get(player);
        return count == null ? 0 : count.get();
    }

    public Map<UUID, AtomicInteger> getFlagCounts() {
        return flagCounts;
    }

    public int getThreshold() {
        return threshold;
    }
}
