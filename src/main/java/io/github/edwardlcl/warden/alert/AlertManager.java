package io.github.edwardlcl.warden.alert;

import io.github.edwardlcl.warden.WardenPlugin;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Delivers exploit alerts to server operators and, optionally, a Discord webhook. Alerts are
 * throttled per (player, type) via {@link CooldownTracker}.
 */
public class AlertManager {

    private static final String ADMIN_PERMISSION = "warden.admin";

    private final WardenPlugin plugin;
    private final CooldownTracker cooldown;
    private final boolean notifyOps;
    private final String webhookUrl;

    public AlertManager(WardenPlugin plugin) {
        this.plugin = plugin;
        long cooldownSeconds = plugin.getConfig().getLong("thresholds.alert-cooldown-seconds", 30);
        this.cooldown = new CooldownTracker(cooldownSeconds * 1000L);
        this.notifyOps = plugin.getConfig().getBoolean("alerts.notify-ops", true);
        this.webhookUrl = plugin.getConfig().getString("alerts.discord-webhook", "");
    }

    /** Alert tied to a specific player; cooldown keyed per player+type. */
    public void alert(UUID playerId, String playerName, String type, String message) {
        String key = playerId + ":" + type;
        if (!cooldown.tryFire(key, System.currentTimeMillis())) {
            return;
        }
        dispatch(type, message);
    }

    /** Alert not tied to a player (e.g. TNT rig); cooldown keyed by type only. */
    public void alertLocation(String type, String message) {
        if (!cooldown.tryFire(type, System.currentTimeMillis())) {
            return;
        }
        dispatch(type, message);
    }

    private void dispatch(String type, String message) {
        String formatted = "[Warden] " + type + ": " + message;
        plugin.getLogger().warning(formatted);

        if (notifyOps) {
            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission(ADMIN_PERMISSION))
                    .forEach(p -> p.sendMessage(ChatColor.RED + "[Warden] " + ChatColor.YELLOW + type
                            + ": " + ChatColor.WHITE + message));
        }

        if (webhookUrl != null && !webhookUrl.isBlank()) {
            postWebhookAsync(formatted);
        }
    }

    private void postWebhookAsync(String content) {
        new BukkitRunnable() {
            @Override
            public void run() {
                postWebhook(content);
            }
        }.runTaskAsynchronously(plugin);
    }

    private void postWebhook(String content) {
        try {
            HttpURLConnection conn =
                    (HttpURLConnection) URI.create(webhookUrl).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setDoOutput(true);
            String payload = "{\"content\":\"" + jsonEscape(content) + "\"}";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            if (code >= 300) {
                plugin.getLogger().warning("Discord webhook returned HTTP " + code);
            }
            conn.disconnect();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to post Discord webhook: " + e.getMessage());
        }
    }

    static String jsonEscape(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
