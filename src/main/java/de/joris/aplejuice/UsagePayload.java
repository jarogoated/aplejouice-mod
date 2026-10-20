package de.joris.aplejuice;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UsagePayload(UUID player, String item, int amount) implements CustomPacketPayload {
    public static final Type<UsagePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("aplejuice", "usage_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UsagePayload> CODEC = StreamCodec.of(
        (buf, value) -> { buf.writeUUID(value.player); buf.writeUtf(value.item, 256); buf.writeVarInt(value.amount); },
        buf -> new UsagePayload(buf.readUUID(), buf.readUtf(256), buf.readVarInt()));
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
