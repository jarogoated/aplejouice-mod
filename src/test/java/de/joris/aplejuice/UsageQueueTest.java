package de.joris.aplejuice;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UsageQueueTest {
    private record Snapshot(UUID player, String item, long tick) {}

    @Test void counts4096UsesInOneTickWithoutHittingThe128SnapshotLimit() {
        var queue = new UsageQueue<Snapshot>(128);
        var store = new CounterStore(); var player = UUID.randomUUID();
        var key = new Snapshot(player, "minecraft:cobweb", 42);
        for (int i = 0; i < 4096; i++) queue.add(key, (snapshot, amount) -> fail("Repeated uses should merge"));
        assertEquals(1, queue.size());
        queue.drain(snapshot -> snapshot.tick() < 42, (snapshot, amount) -> fail("Too early"));
        assertEquals(1, queue.size());
        queue.drain(snapshot -> true, (snapshot, amount) -> store.add(snapshot.player(), snapshot.item(), amount));
        assertEquals(4096, store.get(player, "minecraft:cobweb"));
        queue.drain(snapshot -> true, (snapshot, amount) -> fail("Already drained"));
    }

    @Test void differentPlayersItemsAndTicksRemainSeparateAndOverflowIsResolvedNotSilentlyDropped() {
        var queue = new UsageQueue<Snapshot>(128);
        var resolved = new HashMap<Snapshot, Integer>(); var player = UUID.randomUUID();
        for (int tick = 0; tick < 700; tick++) {
            var key = new Snapshot(player, tick % 2 == 0 ? "minecraft:cobweb" : "minecraft:end_crystal", tick);
            for (int i = 0; i < 3; i++) queue.add(key, (snapshot, amount) -> resolved.merge(snapshot, amount, Integer::sum));
            assertTrue(queue.size() <= 128);
        }
        queue.drain(snapshot -> true, (snapshot, amount) -> resolved.merge(snapshot, amount, Integer::sum));
        assertEquals(700, resolved.size());
        assertTrue(resolved.values().stream().allMatch(amount -> amount == 3));
        assertEquals(2100, resolved.values().stream().mapToInt(Integer::intValue).sum());
    }

    @Test void deathCancelsOnlyThatPlayersPendingUsesAndResetClearsTheQueue() {
        var queue = new UsageQueue<Snapshot>(128);
        var dead = UUID.randomUUID(); var alive = UUID.randomUUID(); var total = new AtomicInteger();
        for (int i = 0; i < 64; i++) {
            queue.add(new Snapshot(dead, "minecraft:cobweb", 1), (key, count) -> fail());
            queue.add(new Snapshot(alive, "minecraft:end_crystal", 1), (key, count) -> fail());
        }
        queue.removeIf(key -> key.player().equals(dead));
        queue.drain(key -> true, (key, count) -> { assertEquals(alive, key.player()); total.addAndGet(count); });
        assertEquals(64, total.get());
        queue.add(new Snapshot(alive, "minecraft:cobweb", 2), (key, count) -> fail());
        queue.clear(); assertEquals(0, queue.size());
    }
}
