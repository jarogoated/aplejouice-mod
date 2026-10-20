package de.joris.aplejuice;

import java.util.UUID;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.Item;

public final class Aplejuice implements ModInitializer {
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(UsagePayload.TYPE, UsagePayload.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (ServerPlayNetworking.canSend(handler.player, UsagePayload.TYPE))
                sender.sendPacket(new UsagePayload(new UUID(0, 0), "minecraft:air", 0));
        });
    }
    public static void onStat(ServerPlayer source, Stat<?> stat, int amount) {
        if (amount <= 0 || stat.getType() != Stats.ITEM_USED || !(stat.getValue() instanceof Item item)) return;
        var packet = new UsagePayload(source.getUUID(), BuiltInRegistries.ITEM.getKey(item).toString(), amount);
        // Only share actions within the same dimension and normal tracking distance.
        for (ServerPlayer viewer : source.level().getServer().getPlayerList().getPlayers()) {
            if (viewer.level() == source.level() && viewer.distanceToSqr(source) <= 128 * 128
                    && ServerPlayNetworking.canSend(viewer, UsagePayload.TYPE)) ServerPlayNetworking.send(viewer, packet);
        }
    }
}
