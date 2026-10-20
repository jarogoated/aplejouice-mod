package de.joris.aplejuice.client.mixin;

import de.joris.aplejuice.BadgePolicy;
import de.joris.aplejuice.client.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void aplejuice$extract(Avatar avatar, AvatarRenderState state, float delta, CallbackInfo ci) {
        var target = (BadgeState) state;
        target.aplejuice$setBadges(List.of()); target.aplejuice$setAnchor(null);
        var config = AplejuiceClient.config;
        var mc = Minecraft.getInstance();
        if (config == null || mc.player == null || !(avatar instanceof Player player) || !AplejuiceClient.prepareObservation()) return;
        if (!BadgePolicy.visible(config.enabled, AplejuiceClient.isWatched(player.getUUID()), player == mc.player,
                state.isInvisible || player.isInvisibleTo(mc.player), mc.options.hideGui, state.distanceToCameraSq, config.range)) return;
        if (!config.showZero) {
            boolean visibleCount = false;
            for (String id : config.items) if (AplejuiceClient.counters.get(player.getUUID(), id) > 0) { visibleCount = true; break; }
            if (!visibleCount) return;
        }
        var badges = new ArrayList<BadgeState.Badge>();
        for (var icon : BadgeModels.forPlayer(player)) {
            long count = AplejuiceClient.counters.get(player.getUUID(), icon.itemId());
            if (BadgePolicy.showCount(config.showZero, count)) badges.add(new BadgeState.Badge(icon.model(), Long.toString(count)));
        }
        // Team rules or name-tag mods may leave vanilla's text and anchor null.
        Vec3 anchor = player.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, player.getYRot(delta));
        target.aplejuice$setAnchor(anchor != null ? anchor : new Vec3(0, player.getBbHeight() + 0.5, 0));
        target.aplejuice$setBadges(List.copyOf(badges));
    }
}
