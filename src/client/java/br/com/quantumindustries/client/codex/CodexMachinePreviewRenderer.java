package br.com.quantumindustries.client.codex;

import br.com.quantumindustries.block.EmpiricalAssayBenchBlock;
import br.com.quantumindustries.block.MechanicalConveyorBlock;
import br.com.quantumindustries.client.kinetic.QksCompiledModel;
import br.com.quantumindustries.client.kinetic.QksModelCache;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import org.joml.Vector3fc;

/** Renders real block, multiblock, and QKS machine models inside the Codex 3D viewport. */
@EventBusSubscriber(modid = "quantumindustries", value = Dist.CLIENT)
public final class CodexMachinePreviewRenderer extends PictureInPictureRenderer<CodexMachinePreviewState> {
    private static final String MOD = "quantumindustries";
    private static final int FULL_BRIGHT = 0x00F000F0;

    private record QksSpec(String modelPath, Identifier texture) {}

    private record PlacedState(BlockState state, float ox, float oy, float oz) {}

    private static final Map<String, QksSpec> QKS_MODELS = Map.ofEntries(
            Map.entry("mechanical_sawmill", new QksSpec("mechanical_sawmill", tex("mechanical_workshop"))),
            Map.entry("electric_sawmill", new QksSpec("electric_sawmill", tex("electric_sawmill"))),
            Map.entry("manual_dynamo", new QksSpec("manual_dynamo", tex("dynamo_materials"))),
            Map.entry("mechanical_shaft_block", new QksSpec("mechanical_shaft_block", tex("mechanical_shaft_a"))),
            Map.entry("primitive_water_wheel", new QksSpec("primitive_water_wheel", tex("water_wheel_d"))),
            Map.entry("medium_water_wheel", new QksSpec("medium_water_wheel", tex("water_wheel_d"))),
            Map.entry("large_water_wheel", new QksSpec("large_water_wheel", tex("water_wheel_d"))),
            Map.entry("small_cogwheel", new QksSpec("small_cogwheel", tex("small_cogwheel"))),
            Map.entry("large_cogwheel", new QksSpec("large_cogwheel", tex("large_cogwheel"))),
            Map.entry("iron_flywheel", new QksSpec("iron_flywheel", tex("iron_flywheel"))),
            Map.entry("mechanical_pulley", new QksSpec("mechanical_pulley", tex("mechanical_pulley")))
    );

    public CodexMachinePreviewRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @SubscribeEvent
    public static void register(RegisterPictureInPictureRenderersEvent event) {
        event.register(CodexMachinePreviewState.class, CodexMachinePreviewRenderer::new);
    }

    private static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(MOD, "textures/block/" + name + ".png");
    }

    @Override
    public Class<CodexMachinePreviewState> getRenderStateClass() {
        return CodexMachinePreviewState.class;
    }

    @Override
    protected String getTextureLabel() {
        return "quantum_codex_machine_preview";
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height * 0.56f;
    }

    public static boolean supports3dPreview(Item item) {
        if (!(item instanceof BlockItem)) return false;
        Identifier key = BuiltInRegistries.ITEM.getKey(item);
        return QKS_MODELS.containsKey(key.getPath()) || item instanceof BlockItem;
    }

    @Override
    protected void renderToTexture(CodexMachinePreviewState state, PoseStack pose) {
        Minecraft mc = Minecraft.getInstance();
        mc.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);

        // PiP outer matrix scales (s, s, -s) in Y-down space. XP(180 + pitch) converts to
        // right-handed Y-up (+X right, +Y up, +Z toward camera) while looking DOWN onto the tabletop.
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(180.0f + state.pitchDeg()));
        pose.mulPose(Axis.YP.rotationDegrees(state.yawDeg()));

        Identifier key = BuiltInRegistries.ITEM.getKey(state.item());
        QksSpec qks = QKS_MODELS.get(key.getPath());
        if (qks != null) {
            renderQksModel(qks, pose);
        } else if (state.item() instanceof BlockItem blockItem) {
            renderBlockStates(mc, previewStates(blockItem.getBlock()), pose);
        }
        pose.popPose();
    }

    private static List<PlacedState> previewStates(Block block) {
        BlockState base = block.defaultBlockState();
        if (base.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            base = base.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
        }
        if (block instanceof EmpiricalAssayBenchBlock) {
            // Render the complete two-block empirical assay workbench (FOOT + HEAD).
            BlockState foot = base.setValue(BlockStateProperties.BED_PART, BedPart.FOOT);
            BlockState head = base.setValue(BlockStateProperties.BED_PART, BedPart.HEAD);
            return List.of(new PlacedState(foot, 0f, 0f, 0f), new PlacedState(head, -1f, 0f, 0f));
        }
        if (block instanceof MechanicalConveyorBlock) {
            base = base.setValue(MechanicalConveyorBlock.GROUNDED, true);
        }
        return List.of(new PlacedState(base, 0f, 0f, 0f));
    }

    private void renderQksModel(QksSpec spec, PoseStack pose) {
        QksCompiledModel model = QksModelCache.get(spec.modelPath());
        float[] bounds = {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
        for (String root : model.rootBoneNames()) {
            collectBoneBounds(model, root, bounds);
        }
        applyAutoFrame(pose, bounds);

        VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.entityCutout(spec.texture()));
        for (String root : model.rootBoneNames()) {
            submitQksBone(model, root, pose, consumer);
        }
    }

    private static void collectBoneBounds(QksCompiledModel model, String name, float[] b) {
        if ("crank".equals(name)) return;
        QksCompiledModel.CompiledBone bone = model.bone(name);
        float[] q = bone.quads();
        for (int i = 0; i < q.length; i += QksCompiledModel.FLOATS_PER_VERTEX) {
            float x = q[i], y = q[i + 1], z = q[i + 2];
            if (x < b[0]) b[0] = x;
            if (y < b[1]) b[1] = y;
            if (z < b[2]) b[2] = z;
            if (x > b[3]) b[3] = x;
            if (y > b[4]) b[4] = y;
            if (z > b[5]) b[5] = z;
        }
        for (String child : model.childBoneNames(name)) {
            collectBoneBounds(model, child, b);
        }
    }

    private static void submitQksBone(QksCompiledModel model, String name, PoseStack pose, VertexConsumer consumer) {
        if ("crank".equals(name)) return;
        QksCompiledModel.CompiledBone bone = model.bone(name);
        pose.pushPose();
        pose.translate(bone.pivotX(), bone.pivotY(), bone.pivotZ());
        if (bone.defaultRotZ() != 0) pose.mulPose(Axis.ZP.rotationDegrees(bone.defaultRotZ()));
        if (bone.defaultRotY() != 0) pose.mulPose(Axis.YP.rotationDegrees(bone.defaultRotY()));
        if (bone.defaultRotX() != 0) pose.mulPose(Axis.XP.rotationDegrees(bone.defaultRotX()));
        pose.translate(-bone.pivotX(), -bone.pivotY(), -bone.pivotZ());
        QksCompiledModel.submitBone(bone, pose.last(), consumer, FULL_BRIGHT);
        for (String child : model.childBoneNames(name)) {
            submitQksBone(model, child, pose, consumer);
        }
        pose.popPose();
    }

    private void renderBlockStates(Minecraft mc, List<PlacedState> states, PoseStack pose) {
        var modelSet = mc.getModelManager().getBlockStateModelSet();
        RandomSource random = RandomSource.create(42L);
        List<List<BakedQuad>> stateQuads = new ArrayList<>(states.size());
        float[] bounds = {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};

        for (PlacedState placed : states) {
            BlockStateModel model = modelSet.get(placed.state());
            List<BlockStateModelPart> parts = new ArrayList<>();
            random.setSeed(42L);
            model.collectParts(random, parts);
            List<BakedQuad> quads = new ArrayList<>();
            for (BlockStateModelPart part : parts) {
                addQuads(part.getQuads(null), quads, placed, bounds);
                for (Direction dir : Direction.values()) {
                    addQuads(part.getQuads(dir), quads, placed, bounds);
                }
            }
            stateQuads.add(quads);
        }

        if (bounds[0] > bounds[3]) return;
        applyAutoFrame(pose, bounds);

        QuadInstance quadInstance = new QuadInstance();
        quadInstance.setColor(0xFFFFFFFF);
        quadInstance.setLightCoords(FULL_BRIGHT);
        quadInstance.setOverlayCoords(OverlayTexture.NO_OVERLAY);

        for (int i = 0; i < states.size(); i++) {
            PlacedState placed = states.get(i);
            pose.pushPose();
            pose.translate(placed.ox(), placed.oy(), placed.oz());
            for (BakedQuad quad : stateQuads.get(i)) {
                RenderType rt = quad.materialInfo().itemRenderType();
                if (rt == null) rt = RenderTypes.cutoutMovingBlock();
                VertexConsumer consumer = bufferSource.getBuffer(rt);
                consumer.putBakedQuad(pose.last(), quad, quadInstance);
            }
            pose.popPose();
        }
    }

    private static void addQuads(List<BakedQuad> src, List<BakedQuad> dst, PlacedState placed, float[] b) {
        for (BakedQuad q : src) {
            dst.add(q);
            for (int v = 0; v < 4; v++) {
                Vector3fc p = q.position(v);
                float x = p.x() + placed.ox();
                float y = p.y() + placed.oy();
                float z = p.z() + placed.oz();
                if (x < b[0]) b[0] = x;
                if (y < b[1]) b[1] = y;
                if (z < b[2]) b[2] = z;
                if (x > b[3]) b[3] = x;
                if (y > b[4]) b[4] = y;
                if (z > b[5]) b[5] = z;
            }
        }
    }

    private static void applyAutoFrame(PoseStack pose, float[] b) {
        if (b[0] > b[3]) return;
        float dx = Math.max(0.15f, b[3] - b[0]);
        float dy = Math.max(0.15f, b[4] - b[1]);
        float dz = Math.max(0.15f, b[5] - b[2]);
        float diag = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float fit = 2.08f / Math.max(0.85f, diag);
        float cx = (b[0] + b[3]) * 0.5f;
        float cy = (b[1] + b[4]) * 0.5f;
        float cz = (b[2] + b[5]) * 0.5f;
        pose.scale(fit, fit, fit);
        pose.translate(-cx, -cy, -cz);
    }
}
