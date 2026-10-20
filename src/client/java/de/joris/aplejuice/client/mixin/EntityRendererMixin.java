package de.joris.aplejuice.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.joris.aplejuice.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Render independently of the server's and other mods' normal name-tag text. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Inject(method = "submit", at = @At("TAIL"))
    private void aplejuice$submit(EntityRenderState rawState, PoseStack pose, SubmitNodeCollector collector,
                                 CameraRenderState camera, CallbackInfo ci) {
        if (!(rawState instanceof AvatarRenderState state) || !(rawState instanceof BadgeState data)) return;
        var badges = data.aplejuice$getBadges();
        var anchor = data.aplejuice$getAnchor();
        if (badges.isEmpty() || anchor == null || !AplejuiceClient.config.enabled) return;
        var font = Minecraft.getInstance().font;
        pose.pushPose();
        if (state.scoreText != null) pose.translate(0, 9 * 1.15F * 0.025F, 0);
        pose.translate(anchor.x, anchor.y + 0.5, anchor.z);
        pose.mulPose(camera.orientation);
        pose.scale(0.025F, -0.025F, 0.025F);
        float totalWidth = 0;
        for (var badge : badges) totalWidth += 18 + font.width(badge.count());
        float x = -(totalWidth - 6) / 2F;
        int y = (state.showExtraEars ? -10 : 0) - 14;
        for (var badge : badges) {
            pose.pushPose(); pose.translate(x + 5, y + 4, 0); pose.scale(10, -10, 0.01F);
            badge.icon().submit(pose, collector, 0xF000F0, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
            collector.submitText(pose, x + 12, y, Component.literal(badge.count()).getVisualOrderText(),
                    true, Font.DisplayMode.NORMAL, 0xF000F0, 0xFFDEC9FF, 0, 0);
            x += 18 + font.width(badge.count());
        }
        pose.popPose();
    }
}
