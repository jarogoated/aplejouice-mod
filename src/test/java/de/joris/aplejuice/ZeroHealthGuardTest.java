package de.joris.aplejuice;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ZeroHealthGuardTest {
    @Test void repeatedPopsInOneTickAreSeparateUsesAndProtectAgainstTransientZeroHealth() {
        var guard = new ZeroHealthGuard(); var store = new CounterStore(); var player = UUID.randomUUID();
        for (int i = 0; i < 1000; i++) {
            guard.popped(player, 42);
            store.add(player, "minecraft:totem_of_undying", 1);
            assertFalse(guard.confirmsDeath(player, true, 42));
        }
        assertEquals(1000, store.get(player, "minecraft:totem_of_undying"));
        for (long tick = 43; tick <= 47; tick++) assertFalse(guard.confirmsDeath(player, true, tick));
        assertFalse(guard.confirmsDeath(player, false, 48));
    }

    @Test void sustainedZeroHealthConfirmsDeathButHealthyUpdateOrAnotherPopRestartsTheGrace() {
        var guard = new ZeroHealthGuard(); var player = UUID.randomUUID();
        assertFalse(guard.confirmsDeath(player, true, 0));
        assertTrue(guard.confirmsDeath(player, true, ZeroHealthGuard.GRACE_TICKS));
        guard.popped(player, 6);
        assertFalse(guard.confirmsDeath(player, true, 10));
        assertFalse(guard.confirmsDeath(player, true, 12));
        assertFalse(guard.confirmsDeath(player, false, 13));
        assertFalse(guard.confirmsDeath(player, true, 14));
        assertTrue(guard.confirmsDeath(player, true, 14 + ZeroHealthGuard.GRACE_TICKS));
    }

    @Test void leavingTheWatchedSetAndDisconnectDoNotLeakOldHealthState() {
        var guard = new ZeroHealthGuard(); var player = UUID.randomUUID();
        guard.confirmsDeath(player, true, 1); guard.retain(Set.of());
        assertFalse(guard.confirmsDeath(player, true, 100));
        guard.clear(); assertFalse(guard.confirmsDeath(player, true, 200));
    }
}
