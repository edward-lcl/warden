package io.github.edwardlcl.warden.detection;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks item-gain events per player within a fixed time window. Pure Java with no Bukkit
 * dependency so the detection math can be unit tested without a live server.
 */
public class SlidingWindowCounter {

    private final long windowMillis;
    private final Map<UUID, Deque<long[]>> events = new HashMap<>();

    public SlidingWindowCounter(long windowMillis) {
        this.windowMillis = windowMillis;
    }

    /**
     * Records {@code quantity} items gained by {@code player} at {@code nowMillis}, then returns the
     * total quantity gained by that player within the trailing window.
     */
    public int record(UUID player, int quantity, long nowMillis) {
        Deque<long[]> deque = events.computeIfAbsent(player, k -> new ArrayDeque<>());
        deque.addLast(new long[] {nowMillis, quantity});
        return countWithin(deque, nowMillis);
    }

    /** Returns the total quantity gained by {@code player} within the trailing window. */
    public int countWithin(UUID player, long nowMillis) {
        Deque<long[]> deque = events.get(player);
        if (deque == null) {
            return 0;
        }
        return countWithin(deque, nowMillis);
    }

    private int countWithin(Deque<long[]> deque, long nowMillis) {
        long cutoff = nowMillis - windowMillis;
        while (!deque.isEmpty() && deque.peekFirst()[0] <= cutoff) {
            deque.removeFirst();
        }
        int total = 0;
        for (long[] entry : deque) {
            total += (int) entry[1];
        }
        return total;
    }

    public void reset(UUID player) {
        events.remove(player);
    }

    public void clear() {
        events.clear();
    }
}
