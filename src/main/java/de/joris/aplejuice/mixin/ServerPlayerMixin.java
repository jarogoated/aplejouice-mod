package de.joris.aplejuice.mixin;

import de.joris.aplejuice.Aplejuice;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "awardStat(Lnet/minecraft/stats/Stat;I)V", at = @At("TAIL"))
    private void aplejuice$usage(Stat<?> stat, int amount, CallbackInfo ci) {
        Aplejuice.onStat((ServerPlayer) (Object) this, stat, amount);
    }
}
