package de.joris.aplejuice;

import java.util.*;

/** Session counters survive chunk unloads; observed deaths are reset by the client lifecycle. */
public final class CounterStore {
    private final Map<UUID, Map<String, Long>> counts = new HashMap<>();
    public void add(UUID player, String item, int amount) {
        if (amount <= 0) return;
        counts.computeIfAbsent(player, ignored -> new HashMap<>())
                .merge(item, (long) amount, (a, b) -> a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b);
    }
    public long get(UUID player, String item) { return counts.getOrDefault(player, Map.of()).getOrDefault(item, 0L); }
    public void reset(UUID player) { counts.remove(player); }
    public void resetItem(String item) { counts.values().forEach(row -> row.remove(item)); }
    public void clear() { counts.clear(); }
}
