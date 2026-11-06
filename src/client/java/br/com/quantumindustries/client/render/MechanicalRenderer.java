package br.com.quantumindustries.client.render;

import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.mechanical.RotationalBlockEntity;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Rotates actual voxel geometry; static frames remain in the baked block model. */
@EventBusSubscriber(modid = "quantumindustries", value = Dist.CLIENT)
public final class MechanicalRenderer<T extends RotationalBlockEntity> implements BlockEntityRenderer<T, MechanicalRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("quantumindustries", "textures/block/mechanical_workshop.png");
    private record Box(float[] from, float[] to, int material, String group) {}
    private static final Map<String, List<Box>> PARTS = load();
    public MechanicalRenderer(BlockEntityRendererProvider.Context context) {}
    public static final class State extends BlockEntityRenderState {
        String model;
        float angle, facing;
        Direction.Axis axle = Direction.Axis.Z;
        boolean crank;
    }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlocks.MECHANICAL_SAWMILL_ENTITY.get(), MechanicalRenderer::new);
        event.registerBlockEntityRenderer(ModBlocks.MECHANICAL_SHAFT_ENTITY.get(), MechanicalRenderer::new);
        event.registerBlockEntityRenderer(ModBlocks.PRIMITIVE_WATER_WHEEL_ENTITY.get(), MechanicalRenderer::new);
    }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(T entity, State state, float partial, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partial, camera, overlay);
        state.model = BuiltInRegistries.BLOCK.getKey(entity.getBlockState().getBlock()).getPath();
        state.angle = entity.getLevel() == null ? 0 : entity.rotation(entity.getLevel().getGameTime() + partial);
        state.crank = entity.installed();
        state.axle = entity.axle();
        state.facing = entity.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? 180 - entity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot() : 0;
    }
    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        var boxes = PARTS.getOrDefault(state.model, List.of());
        pose.pushPose();
        pose.translate(.5, .5, .5);
        if (state.model.equals("mechanical_shaft_block")) {
            if (state.axle == Direction.Axis.X) pose.mulPose(Axis.YP.rotationDegrees(90));
            if (state.axle == Direction.Axis.Y) pose.mulPose(Axis.XP.rotationDegrees(90));
        } else pose.mulPose(Axis.YP.rotationDegrees(state.facing));
        pose.mulPose((state.model.equals("mechanical_sawmill") ? Axis.XP : Axis.ZP).rotationDegrees(state.angle));
        pose.translate(-.5, -.5, -.5);
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TEXTURE), (p, vertices) -> {
            PoseStack stack = new PoseStack();
            stack.last().pose().set(p.pose()); stack.last().normal().set(p.normal());
            for (Box box : boxes) {
                if (box.group().equals("rotor") || state.crank && box.group().equals("crank")) cuboid(stack, vertices, box, state.lightCoords);
            }
        });
        pose.popPose();
    }
    private static Map<String, List<Box>> load() {
        try (var stream = MechanicalRenderer.class.getResourceAsStream("/assets/quantumindustries/mechanical/parts.json")) {
            if (stream == null) throw new IllegalStateException("Missing mechanical model parts");
            var root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, List<Box>> parts = new HashMap<>();
            root.entrySet().forEach(entry -> {
                List<Box> boxes = new ArrayList<>();
                for (var element : entry.getValue().getAsJsonArray()) {
                    var b = element.getAsJsonObject();
                    boxes.add(new Box(vector(b.getAsJsonArray("from")), vector(b.getAsJsonArray("to")), b.get("material").getAsInt(), b.get("group").getAsString()));
                }
                parts.put(entry.getKey(), List.copyOf(boxes));
            });
            return Map.copyOf(parts);
        } catch (java.io.IOException error) { throw new IllegalStateException(error); }
    }
    private static float[] vector(JsonArray a) { return new float[]{a.get(0).getAsFloat()/16, a.get(1).getAsFloat()/16, a.get(2).getAsFloat()/16}; }
    private static final int[][] FACES = {{0,1,3,2},{5,4,6,7},{4,0,2,6},{1,5,7,3},{2,3,7,6},{4,5,1,0}};
    private static final float[][] NORMALS = {{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};
    private static void cuboid(PoseStack pose, VertexConsumer out, Box box, int light) {
        var f = box.from(); var t = box.to();
        float[][] v = {{f[0],f[1],f[2]},{t[0],f[1],f[2]},{f[0],t[1],f[2]},{t[0],t[1],f[2]},
                {f[0],f[1],t[2]},{t[0],f[1],t[2]},{f[0],t[1],t[2]},{t[0],t[1],t[2]}};
        float u = (box.material()%4)/4f, w = (box.material()/4)/2f;
        for (int face=0; face<6; face++) for (int i=0; i<4; i++) {
            var p = v[FACES[face][i]];var n = NORMALS[face];
            out.addVertex(pose.last(),p[0],p[1],p[2]).setColor(0xFFFFFFFF)
                    .setUv(u+(i==1||i==2?.249f:.001f),w+(i>=2?.001f:.499f))
                    .setOverlay(655360).setLight(light).setNormal(pose.last(),n[0],n[1],n[2]);
        }
    }
}
