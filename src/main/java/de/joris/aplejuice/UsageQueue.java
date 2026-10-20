package de.joris.aplejuice;

import java.util.LinkedHashMap;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/** Bound distinct unresolved snapshots, not the number of uses in a burst. */
public final class UsageQueue<K> {
    private final int capacity;
    private final LinkedHashMap<K, Integer> counts = new LinkedHashMap<>();
    public UsageQueue(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
        this.capacity = capacity;
    }
    public void add(K key, BiConsumer<K, Integer> overflow) {
        if (!counts.containsKey(key) && counts.size() >= capacity) {
            var iterator = counts.entrySet().iterator();
            var oldest = iterator.next();
            K oldKey = oldest.getKey(); int amount = oldest.getValue(); iterator.remove();
            overflow.accept(oldKey, amount);
        }
        counts.merge(key, 1, Math::addExact);
    }
    public void drain(Predicate<K> ready, BiConsumer<K, Integer> consumer) {
        var iterator = counts.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (!ready.test(entry.getKey())) continue;
            K key = entry.getKey(); int amount = entry.getValue(); iterator.remove();
            consumer.accept(key, amount);
        }
    }
    public void removeIf(Predicate<K> predicate) { counts.keySet().removeIf(predicate); }
    public int size() { return counts.size(); }
    public void clear() { counts.clear(); }
}
