package de.joris.aplejuice.client;

import java.util.*;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;

/** Cached entries are immutable after creation, so submitted render states are never mutated. */
public final class BadgeModels {
    private static final int MODEL_REFRESH_TICKS = 20;
    public record Icon(String itemId, ItemStackRenderState model) {}
    private record Entry(Player owner, long expires, List<Icon> icons) {}
    private static final Map<UUID, Entry> cache = new HashMap<>();
    private static long builds;
    public static void registerReloadListener() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            public Identifier getFabricId() { return Identifier.fromNamespaceAndPath("aplejuice", "badge_models"); }
            public void onResourceManagerReload(ResourceManager manager) { clear(); }
        });
    }
    public static void clear() { cache.clear(); }
    public static void retain(Set<UUID> watched) { cache.keySet().retainAll(watched); }
    public static long buildCount() { return builds; }
    public static List<Icon> forPlayer(Player player) {
        long tick = AplejuiceClient.clientTick();
        var entry = cache.get(player.getUUID());
        if (entry != null && entry.owner == player && tick < entry.expires) return entry.icons;
        var icons = new ArrayList<Icon>();
        var mc = Minecraft.getInstance();
        for (String id : AplejuiceClient.config.items) {
            Item item = AplejuiceClient.item(id);
            if (item == Items.AIR) continue;
            var model = new ItemStackRenderState();
            mc.getItemModelResolver().updateForLiving(model, new ItemStack(item), ItemDisplayContext.GUI, player);
            icons.add(new Icon(id, model)); builds++;
        }
        var result = List.copyOf(icons);
        if (cache.size() >= AplejuiceClient.MAX_TRACKED_PLAYERS && !cache.containsKey(player.getUUID()))
            cache.remove(cache.keySet().iterator().next());
        cache.put(player.getUUID(), new Entry(player, tick + MODEL_REFRESH_TICKS, result));
        return result;
    }
}
