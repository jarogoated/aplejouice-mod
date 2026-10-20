package de.joris.aplejuice.client.mixin;

import de.joris.aplejuice.client.AplejuiceClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleBlockUpdate", at = @At("HEAD"))
    private void aplejuice$block(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
        AplejuiceClient.observeBlock(packet.getPos(), packet.getBlockState());
    }
    @Inject(method = "handleChunkBlocksUpdate", at = @At("HEAD"))
    private void aplejuice$blocks(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci) {
        if (AplejuiceClient.prepareObservation() && AplejuiceClient.isObserving()) packet.runUpdates(AplejuiceClient::observeBlock);
    }
    @Inject(method = "handleAddEntity", at = @At("HEAD"))
    private void aplejuice$spawn(ClientboundAddEntityPacket packet, CallbackInfo ci) {
        if (Minecraft.getInstance().isSameThread()) AplejuiceClient.observeEntitySpawn(packet);
    }
    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void aplejuice$totem(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        var client = Minecraft.getInstance();
        if (client.isSameThread() && client.level != null && packet.getEntity(client.level) instanceof Player player) {
            if (packet.getEventId() == 35) AplejuiceClient.observeTotem(player);
            else if (packet.getEventId() == 3) AplejuiceClient.observeDeath(player);
        }
    }
    @Inject(method = "handleSetEquipment", at = @At("HEAD"))
    private void aplejuice$rememberHand(ClientboundSetEquipmentPacket packet, CallbackInfo ci) {
        var client = Minecraft.getInstance();
        if (AplejuiceClient.prepareObservation() && client.level.getEntity(packet.getEntity()) instanceof Player player)
            AplejuiceClient.rememberPlayer(player);
    }
    @Inject(method = "handleSetEquipment", at = @At("TAIL"))
    private void aplejuice$rememberNewHand(ClientboundSetEquipmentPacket packet, CallbackInfo ci) {
        var client = Minecraft.getInstance();
        if (AplejuiceClient.prepareObservation() && client.level.getEntity(packet.getEntity()) instanceof Player player)
            AplejuiceClient.rememberPlayer(player);
    }
}
