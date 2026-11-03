package de.joris.aplejuice.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

/** Bounded, loaded-chunk-only discovery; immutable drawing snapshots and no scans in render callbacks. */
public final class WaterSourceRenderer {
    private static final int RADIUS = 12, CHECKS_PER_TICK = 1536, MAX_MARKERS = 256;
    private record Marker(int x, int y, int z, float height) {}
    private static final List<BlockPos> OFFSETS = offsets();
    private static final Map<Long,Marker> found = new LinkedHashMap<>();
    private static final RenderStateDataKey<List<Marker>> SOURCES = RenderStateDataKey.create(() -> "aplejuice_water_sources");
    private static final RenderType LAYER = RenderType.create("aplejuice_water_sources", RenderSetup.builder(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("aplejuice", "pipeline/water_sources"))
                    .withCull(false).build()).sortOnUpload().createRenderSetup());
    private static ClientLevel level;
    private static BlockPos origin = BlockPos.ZERO;
    private static int cursor;
    private static List<Marker> markers = List.of();
    private static boolean dirty;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(WaterSourceRenderer::tick);
        WorldRenderEvents.END_EXTRACTION.register(context -> {
            if (dirty) { markers=List.copyOf(found.values()); dirty=false; }
            context.worldState().setData(SOURCES,
                    AplejuiceClient.config.waterSources && Minecraft.getInstance().level == level ? markers : List.of());
        });
        WorldRenderEvents.END_MAIN.register(context -> {
            var sources = context.worldState().getDataOrDefault(SOURCES, List.of());
            if (sources.isEmpty()) return;
            var camera = context.worldState().cameraRenderState.pos;
            var pose = context.matrices();
            if (pose == null) return;
            var buffer = context.consumers().getBuffer(LAYER);
            for (var marker : sources) {
                if (camera.distanceToSqr(marker.x+.5, marker.y+.5, marker.z+.5) > RADIUS*RADIUS) continue;
                pose.pushPose();
                pose.translate(marker.x-camera.x, marker.y+marker.height+.008-camera.y, marker.z-camera.z);
                var matrix = pose.last().pose();
                // A clear frame and subtle fill distinguish source surfaces without replacing water textures.
                quad(buffer,matrix,.025F,.025F,.975F,.975F,0x303DE4FF);
                quad(buffer,matrix,.025F,.025F,.975F,.065F,0xE66AF0FF);
                quad(buffer,matrix,.025F,.935F,.975F,.975F,0xE66AF0FF);
                quad(buffer,matrix,.025F,.065F,.065F,.935F,0xE66AF0FF);
                quad(buffer,matrix,.935F,.065F,.975F,.935F,0xE66AF0FF);
                quad(buffer,matrix,.44F,.32F,.56F,.68F,0xF0B9FAFF);
                quad(buffer,matrix,.32F,.44F,.68F,.56F,0xF0B9FAFF);
                pose.popPose();
            }
            if (context.consumers() instanceof MultiBufferSource.BufferSource immediate) immediate.endBatch(LAYER);
        });
    }
    private static void tick(Minecraft client) {
        if (!AplejuiceClient.config.waterSources || client.level == null || client.player == null) {
            clear(); return;
        }
        if (client.level != level) { clear(); level=client.level; }
        // Removed sources disappear by the next tick even if a mod suppresses the block-update callback.
        var iterator=found.entrySet().iterator();
        while (iterator.hasNext()) {
            var marker=iterator.next().getValue();
            var pos=new BlockPos(marker.x,marker.y,marker.z);
            if (!near(client,pos) || !level.hasChunkAt(pos) || !isSource(pos)) {
                iterator.remove(); dirty=true;
            }
        }
        if (cursor == 0) origin=client.player.blockPosition();
        var pos = new BlockPos.MutableBlockPos();
        for (int budget=0; budget<CHECKS_PER_TICK && cursor<OFFSETS.size(); budget++,cursor++) {
            var delta=OFFSETS.get(cursor);
            pos.set(origin.getX()+delta.getX(),origin.getY()+delta.getY(),origin.getZ()+delta.getZ());
            update(client,pos);
        }
        if (cursor==OFFSETS.size()) cursor=0;
    }
    /** Invoked after local predictions and server-confirmed changes, with the new state already installed. */
    public static void blockChanged(ClientLevel world, BlockPos pos) {
        if (AplejuiceClient.config == null || !AplejuiceClient.config.waterSources) return;
        var client=Minecraft.getInstance();
        if (!client.isSameThread() || client.player == null || client.level != world || !near(client,pos)) return;
        if (world != level) { clear(); level=world; }
        update(client,pos);
        // Source surface height depends on the fluid directly above it.
        update(client,pos.below());
    }
    private static boolean near(Minecraft client, BlockPos pos) {
        return client.player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5) <= RADIUS*RADIUS;
    }
    private static boolean isSource(BlockPos pos) {
        var state=level.getBlockState(pos);
        return state.is(Blocks.WATER) && state.getFluidState().isSource();
    }
    private static void update(Minecraft client, BlockPos pos) {
        long key=pos.asLong();
        if (pos.getY()<level.getMinY() || pos.getY()>=level.getMaxY() || !near(client,pos)
                || !level.hasChunkAt(pos) || !isSource(pos)) {
            if (found.remove(key) != null) dirty=true;
            return;
        }
        if (found.size()>=MAX_MARKERS && !found.containsKey(key)) return;
        var marker=new Marker(pos.getX(),pos.getY(),pos.getZ(),level.getFluidState(pos).getHeight(level,pos));
        if (!marker.equals(found.put(key,marker))) dirty=true;
    }
    private static List<BlockPos> offsets() {
        var offsets=new ArrayList<BlockPos>();
        for (int x=-RADIUS;x<=RADIUS;x++) for (int y=-RADIUS;y<=RADIUS;y++) for (int z=-RADIUS;z<=RADIUS;z++)
            if (x*x+y*y+z*z<=RADIUS*RADIUS) offsets.add(new BlockPos(x,y,z));
        offsets.sort(Comparator.comparingInt(p -> p.getX()*p.getX()+p.getY()*p.getY()+p.getZ()*p.getZ()));
        return List.copyOf(offsets);
    }
    private static void clear() { level=null; cursor=0; markers=List.of(); found.clear(); dirty=false; }
    private static void quad(com.mojang.blaze3d.vertex.VertexConsumer buffer, org.joml.Matrix4f matrix,
                             float x,float z,float right,float bottom,int color) {
        buffer.addVertex(matrix,x,0,z).setColor(color);
        buffer.addVertex(matrix,x,0,bottom).setColor(color);
        buffer.addVertex(matrix,right,0,bottom).setColor(color);
        buffer.addVertex(matrix,right,0,z).setColor(color);
    }
    private WaterSourceRenderer() {}
}
