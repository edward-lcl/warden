package io.github.edwardlcl.warden;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.edwardlcl.warden.alert.CooldownTracker;
import io.github.edwardlcl.warden.detection.SlidingWindowCounter;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Tests the pure detection math: sliding-window item counting, threshold crossing, and alert
 * cooldown suppression. These run without a live Bukkit server.
 */
class DupeDetectorTest {

    @Test
    void slidingWindowSumsItemsWithinWindow() {
        SlidingWindowCounter counter = new SlidingWindowCounter(1000L);
        UUID player = UUID.randomUUID();

        assertEquals(10, counter.record(player, 10, 1000L));
        assertEquals(30, counter.record(player, 20, 1500L));
        assertEquals(45, counter.record(player, 15, 1900L));
    }

    @Test
    void slidingWindowEvictsExpiredEvents() {
        SlidingWindowCounter counter = new SlidingWindowCounter(1000L);
        UUID player = UUID.randomUUID();

        counter.record(player, 50, 1000L);
        // 1100ms later the first event has aged out of the 1000ms window.
        assertEquals(20, counter.record(player, 20, 2100L));
    }

    @Test
    void thresholdTriggersWhenExceeded() {
        SlidingWindowCounter counter = new SlidingWindowCounter(1000L);
        UUID player = UUID.randomUUID();
        int threshold = 64;

        assertFalse(counter.record(player, 64, 1000L) > threshold);
        assertTrue(counter.record(player, 1, 1100L) > threshold);
    }

    @Test
    void separatePlayersAreCountedIndependently() {
        SlidingWindowCounter counter = new SlidingWindowCounter(1000L);
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        assertEquals(64, counter.record(a, 64, 1000L));
        assertEquals(5, counter.record(b, 5, 1000L));
    }

    @Test
    void cooldownSuppressesDuplicateAlerts() {
        CooldownTracker cooldown = new CooldownTracker(30_000L);
        String key = UUID.randomUUID() + ":DUPE";

        assertTrue(cooldown.tryFire(key, 1000L));
        assertFalse(cooldown.tryFire(key, 5000L));
        assertFalse(cooldown.tryFire(key, 30_000L));
        assertTrue(cooldown.tryFire(key, 31_001L));
    }

    @Test
    void cooldownIsPerKey() {
        CooldownTracker cooldown = new CooldownTracker(30_000L);

        assertTrue(cooldown.tryFire("a:DUPE", 1000L));
        assertTrue(cooldown.tryFire("b:DUPE", 1000L));
        assertFalse(cooldown.tryFire("a:DUPE", 1000L));
    }
}
