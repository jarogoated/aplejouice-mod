package de.joris.aplejuice.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import de.joris.aplejuice.DuelBorderGeometry;
import de.joris.aplejuice.BorderMap;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.resources.Identifier;

/** Normal depth testing, no depth writes, and both faces visible from inside the arena. */
public final class DuelBorderRenderer {
    private static final RenderStateDataKey<BorderMap> MAP = RenderStateDataKey.create(() -> "aplejuice_duel_border");
    private static final java.util.List<DuelBorderGeometry.Quad> MESH = DuelBorderGeometry.mesh();
    private static final RenderType LAYER = RenderType.create("aplejuice_duel_border", RenderSetup.builder(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("aplejuice", "pipeline/duel_border"))
                    .withCull(false).build()).sortOnUpload().createRenderSetup());
    public static void register() {
        WorldRenderEvents.END_EXTRACTION.register(context -> {
            var camera = context.worldState().cameraRenderState.pos;
            var map = BorderMap.byId(AplejuiceClient.config.borderMap);
            context.worldState().setData(MAP, Minecraft.getInstance().level != null
                    && AplejuiceClient.config.duelBorder && map.inRange(camera.x,camera.y,camera.z) ? map : null);
        });
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(context -> {
            var map = context.worldState().getData(MAP);
            if (map == null) return;
            var camera = context.worldState().cameraRenderState.pos;
            var pose = context.matrices();
            if (pose == null) return;
            pose.pushPose();
            pose.translate(map.x()-camera.x, map.y()-camera.y, map.z()-camera.z);
            pose.scale((float)map.radiusX(), (float)map.radiusY(), (float)map.radiusZ());
            var matrix = pose.last().pose();
            var buffer = context.consumers().getBuffer(LAYER);
            for (var q : MESH) {
                buffer.addVertex(matrix,q.ax(),q.ay(),q.az()).setColor(q.color());
                buffer.addVertex(matrix,q.bx(),q.by(),q.bz()).setColor(q.color());
                buffer.addVertex(matrix,q.cx(),q.cy(),q.cz()).setColor(q.color());
                buffer.addVertex(matrix,q.dx(),q.dy(),q.dz()).setColor(q.color());
            }
            pose.popPose();
        });
    }
    private DuelBorderRenderer() {}
}
