package io.github.edwardlcl.warden.detection;

import io.github.edwardlcl.warden.WardenPlugin;
import io.github.edwardlcl.warden.alert.AlertManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Watches explosions for the classic TNT-duplication signature: a piston/observer rig that pushes a
 * TNT block such that the block reappears in the same chunk shortly after it explodes.
 */
public class TntDupeListener implements Listener {

    private static final long REAPPEAR_CHECK_TICKS = 5L;

    private final WardenPlugin plugin;
    private final AlertManager alertManager;
    private final boolean enabled;

    public TntDupeListener(WardenPlugin plugin, AlertManager alertManager) {
        this.plugin = plugin;
        this.alertManager = alertManager;
        this.enabled = plugin.getConfig().getBoolean("detection.tnt-dupe", true);
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        if (enabled) {
            scheduleReappearCheck(event.getBlock().getLocation());
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (enabled && event.getEntity().getType().name().equals("PRIMED_TNT")) {
            scheduleReappearCheck(event.getLocation());
        }
    }

    private void scheduleReappearCheck(Location origin) {
        if (origin == null || origin.getWorld() == null) {
            return;
        }
        if (!hasPistonObserverNearby(origin)) {
            return;
        }
        Location snapshot = origin.clone();
        new BukkitRunnable() {
            @Override
            public void run() {
                Block block = snapshot.getBlock();
                if (block.getType() == Material.TNT) {
                    alertManager.alertLocation(
                            "TNT_DUPE",
                            String.format(
                                    "TNT reappeared post-explosion near piston/observer rig at %s",
                                    formatLocation(snapshot)));
                }
            }
        }.runTaskLater(plugin, REAPPEAR_CHECK_TICKS);
    }

    private boolean hasPistonObserverNearby(Location origin) {
        boolean piston = false;
        boolean observer = false;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    Material type = origin.clone().add(dx, dy, dz).getBlock().getType();
                    if (type == Material.PISTON || type == Material.STICKY_PISTON) {
                        piston = true;
                    } else if (type == Material.OBSERVER) {
                        observer = true;
                    }
                }
            }
        }
        return piston && observer;
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
}
