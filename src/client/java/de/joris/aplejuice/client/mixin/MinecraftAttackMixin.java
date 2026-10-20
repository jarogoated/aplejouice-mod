package de.joris.aplejuice.client.mixin;

import de.joris.aplejuice.client.AplejuiceClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Select the local attack target even when another client mod handles the interaction callback. */
@Mixin(Minecraft.class)
public abstract class MinecraftAttackMixin {
    @Inject(method = "startAttack", at = @At("HEAD"))
    private void aplejuice$attack(CallbackInfoReturnable<Boolean> ci) {
        var client = (Minecraft) (Object) this;
        if (client.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof Player target)
            AplejuiceClient.selectTarget(target);
    }
}
