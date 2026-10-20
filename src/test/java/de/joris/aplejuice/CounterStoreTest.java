package de.joris.aplejuice;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CounterStoreTest {
    @Test void separatesPlayersAndItemsAndIgnoresInvalidDeltas() {
        var store = new CounterStore(); var a = UUID.randomUUID(); var b = UUID.randomUUID();
        store.add(a, "minecraft:cobweb", 63); store.add(b, "minecraft:cobweb", 2);
        store.add(a, "minecraft:ender_pearl", 3); store.add(a, "minecraft:cobweb", -20);
        assertEquals(63, store.get(a, "minecraft:cobweb")); assertEquals(2, store.get(b, "minecraft:cobweb"));
        store.reset(a); assertEquals(0, store.get(a, "minecraft:cobweb"));
        assertEquals(2, store.get(b, "minecraft:cobweb"));
    }
    @Test void itemResetAndSessionReset() {
        var store = new CounterStore(); var a = UUID.randomUUID();
        store.add(a, "web", 1); store.add(a, "pearl", 4); store.resetItem("web");
        assertEquals(0, store.get(a, "web")); assertEquals(4, store.get(a, "pearl"));
        store.clear(); assertEquals(0, store.get(a, "pearl"));
    }
}
