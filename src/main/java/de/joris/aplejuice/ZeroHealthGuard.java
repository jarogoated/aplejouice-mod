package de.joris.aplejuice;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** A pop proves survival; a transient zero-health update must not reset a living player's round. */
public final class ZeroHealthGuard {
    public static final int GRACE_TICKS = 5;
    private final Map<UUID, Long> zeroSince = new HashMap<>(), lastPop = new HashMap<>();
    public void popped(UUID id, long tick) { zeroSince.remove(id); lastPop.put(id, tick); }
    public boolean confirmsDeath(UUID id, boolean zeroHealth, long tick) {
        if (!zeroHealth || tick - lastPop.getOrDefault(id, Long.MIN_VALUE / 2) <= GRACE_TICKS) {
            zeroSince.remove(id); return false;
        }
        return tick - zeroSince.computeIfAbsent(id, ignored -> tick) >= GRACE_TICKS;
    }
    public void retain(Set<UUID> ids) { zeroSince.keySet().retainAll(ids); lastPop.keySet().retainAll(ids); }
    public void clear() { zeroSince.clear(); lastPop.clear(); }
}
