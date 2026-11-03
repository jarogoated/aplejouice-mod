package de.joris.aplejuice.client.mixin;

import de.joris.aplejuice.client.WaterSourceRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelWaterMixin {
    @Inject(method="sendBlockUpdated", at=@At("TAIL"))
    private void aplejuice$waterChanged(BlockPos pos, BlockState previous, BlockState current, int flags, CallbackInfo ci) {
        WaterSourceRenderer.blockChanged((ClientLevel)(Object)this, pos);
    }
}
