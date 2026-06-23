package io.github.edwardlcl.warden.alert;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-key cooldown gate. Pure Java so alert throttling can be unit tested without Bukkit.
 * A key is typically "{playerId}:{alertType}".
 */
public class CooldownTracker {

    private final long cooldownMillis;
    private final Map<String, Long> lastFired = new HashMap<>();

    public CooldownTracker(long cooldownMillis) {
        this.cooldownMillis = cooldownMillis;
    }

    /**
     * Returns true if {@code key} is allowed to fire at {@code nowMillis}, recording the timestamp
     * when it does. Subsequent calls within the cooldown window return false.
     */
    public boolean tryFire(String key, long nowMillis) {
        Long previous = lastFired.get(key);
        if (previous != null && nowMillis - previous < cooldownMillis) {
            return false;
        }
        lastFired.put(key, nowMillis);
        return true;
    }

    public void clear() {
        lastFired.clear();
    }
}
