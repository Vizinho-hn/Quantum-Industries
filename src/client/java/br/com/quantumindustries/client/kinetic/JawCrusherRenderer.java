package br.com.quantumindustries.client.kinetic;

import br.com.quantumindustries.block.JawCrusherBlock;
import br.com.quantumindustries.block.entity.JawCrusherBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.mechanical.MechanicalParticipant;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = "quantumindustries", value = Dist.CLIENT)
public final class JawCrusherRenderer implements BlockEntityRenderer<JawCrusherBlockEntity, JawCrusherRenderer.State> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("quantumindustries", "textures/block/jaw_crusher_a.png");
    // The jaw's linkage is authored in Blender units before being fitted to a single block.
    private static final float MODEL_SCALE = 1.365f / 16;
    private static final float PORT_CENTER_X = 1.0f / 16.0f;
    private static final float PORT_CENTER_Y = 8.0f / 16.0f;
    private static final float PORT_CENTER_Z = 8.0f / 16.0f;
    private static final float PORT_LENGTH = 2.0f / 16.0f;
    private static final float PORT_SECTION = 4.0f / 16.0f;
    private final ItemModelResolver items;

    public JawCrusherRenderer(BlockEntityRendererProvider.Context context) {
        items = context.itemModelResolver();
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState input = new ItemStackRenderState();
        final ItemStackRenderState output = new ItemStackRenderState();
        Direction facing;
        float angle;
        float scale;
        boolean coupled;
        boolean formed;
        boolean narrow;
        boolean refined;
        boolean hopperAbove;
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlocks.JAW_CRUSHER_ENTITY.get(), JawCrusherRenderer::new);
    }

    @Override public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(JawCrusherBlockEntity entity, State state, float partialTick,
            Vec3 camera, ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, camera, overlay);
        state.facing = entity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        state.input.clear();
        state.output.clear();
        var level = entity.getLevel();
        ClientKineticTracker.applyPending(entity);
        state.formed = entity.getBlockState().getValue(JawCrusherBlock.FORMED);
        state.refined = entity.getBlockState().getValue(JawCrusherBlock.REFINED);
        state.narrow = entity.getBlockState().getValue(JawCrusherBlock.NARROW);
        var hopperPos = entity.getBlockPos().above(state.formed ? 2 : 1);
        state.hopperAbove = level != null && level.getBlockState(hopperPos)
                .getBlock() instanceof net.minecraft.world.level.block.HopperBlock
                && level.getBlockState(hopperPos).getValue(net.minecraft.world.level.block.HopperBlock.FACING) == Direction.DOWN;
        state.scale = state.formed ? JawCrusherBlock.FORMED_SCALE : 1;
        Direction drive = JawCrusherBlock.driveFace(entity.getBlockState());
        state.coupled = level != null && level.getBlockEntity(entity.getBlockPos().relative(drive))
                instanceof MechanicalParticipant shaft && shaft.connectsMechanical(drive.getOpposite());
        if (level == null) {
            state.angle = 0;
            return;
        }
        double time = level.getGameTime() + partialTick;
        float direction = QksBlockEntityRenderer.worldAxisSign(Direction.Axis.X, state.facing);
        state.angle = (float) (entity.interpolatedAngleRadians(time) * direction
                + entity.visualTurns(time) * (2 * Math.PI));
        if (!entity.input().isEmpty()) {
            items.updateForTopItem(state.input, entity.input(), ItemDisplayContext.FIXED, level, null, 0);
        }
        if (!entity.output().isEmpty()) {
            items.updateForTopItem(state.output, entity.output(), ItemDisplayContext.FIXED, level, null, 0);
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        QksCompiledModel model = QksModelCache.get("jaw_crusher");
        pose.pushPose();
        pose.translate(.5f, .5f, .5f);
        pose.mulPose(Axis.YP.rotationDegrees(180 - state.facing.toYRot()));
        pose.translate(-.5f, -.5f, -.5f);
        if (state.refined) {
            submitRefined(state, pose, collector);
            pose.popPose();
            return;
        }
        if (state.coupled) submitCoupling(model, state, pose, collector);
        if (state.formed) {
            pose.translate(JawCrusherBlock.DRIVE_ANCHOR_X, 0, .5f);
            pose.scale(state.narrow ? JawCrusherBlock.NARROW_WIDTH_SCALE : state.scale, state.scale, state.scale);
            pose.translate(-JawCrusherBlock.DRIVE_ANCHOR_X, 0, -JawCrusherBlock.AUTHORED_WHEEL_Z);
        }
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TEXTURE), (pass, consumer) -> {
            PoseStack work = new PoseStack();
            work.last().pose().set(pass.pose());
            work.last().normal().set(pass.normal());

            var wheel = model.bone("rotor");
            var rod = model.bone("connecting_rod");
            var slider = model.bone("slider");
            // The shaft grid fixes the hub at half a block from the floor and the cell edge.
            float wheelDy = .5f / state.scale - wheel.pivotY();
            float wheelDz = state.formed ? 0 : .5f - wheel.pivotZ();
            float sliderDy = state.formed ? .34f / state.scale - slider.pivotY() : 0;
            float radialY = rod.pivotY() - wheel.pivotY(), radialZ = rod.pivotZ() - wheel.pivotZ();
            float sin = (float) Math.sin(state.angle), cos = (float) Math.cos(state.angle);
            float pinY = wheel.pivotY() + wheelDy + radialY * cos - radialZ * sin;
            float pinZ = wheel.pivotZ() + wheelDz + radialY * sin + radialZ * cos;
            float restY = slider.pivotY() - rod.pivotY(), restZ = slider.pivotZ() - rod.pivotZ();
            float fittedY = restY + sliderDy - wheelDy;
            float rodLength = (float) Math.hypot(fittedY, restZ);
            float dy = slider.pivotY() + sliderDy - pinY;
            float dz = -(float) Math.sqrt(Math.max(0, rodLength * rodLength - dy * dy));
            float shift = pinZ + dz - slider.pivotZ() - wheelDz;

            int light = state.lightCoords;
            work.pushPose();
            if (state.formed && state.hopperAbove) work.scale(1, 2 / JawCrusherBlock.FORMED_SCALE, 1);
            QksCompiledModel.submitBone(model.bone(state.hopperAbove
                    ? (state.formed ? (state.narrow ? "frame_hopper_narrow" : "frame_hopper_wide") : "frame_hopper_compact")
                    : "frame"), work.last(), consumer, light);
            work.popPose();
            submitPart(wheel, work, consumer, light, wheelDy, wheelDz, state.angle);
            work.pushPose();
            if (state.coupled) work.translate(.09f, 0, 0);
            submitPart(model.bone("crank_pin"), work, consumer, light, wheelDy, wheelDz, state.angle);
            submitRod(rod, work, consumer, light, pinY, pinZ, dy, dz, restY, restZ);
            submitPart(model.bone("slider_guide"), work, consumer, light, sliderDy, wheelDz, 0);
            submitPart(slider, work, consumer, light, sliderDy, wheelDz + shift, 0);
            work.popPose();
            submitPart(model.bone("moving_jaw"), work, consumer, light, 0, 0, -.26f * shift / MODEL_SCALE);
        });
        submitStack(state.input, pose, collector, state.lightCoords, .58f, .80f, .63f, .35f, 68, state.scale);
        submitStack(state.output, pose, collector, state.lightCoords, .58f, .127f, .158f, .31f, 48, state.scale);
        pose.popPose();
    }

    private static void submitRefined(State state, PoseStack pose, SubmitNodeCollector collector) {
        boolean wide = state.formed;
        var model = QksModelCache.get(wide ? "jaw_crusher_multiblock" : "jaw_crusher_compact");
        float scale = wide ? 2 : 1;
        float centerZ = wide ? .75f : .5f;
        float eccentric = 1.15f / (16 * scale);
        float sin = (float) Math.sin(state.angle);
        float cos = (float) Math.cos(state.angle);
        // The slider stays at its rail height while the pin circles the hub.
        float restY = -4.15f / (16 * scale);
        float restZ = (wide ? -16 : -5.6f) / (16 * scale);
        float pinY = .5f / scale + eccentric * cos;
        float pinZ = centerZ + eccentric * sin;
        float dy = 5f / (16 * scale) - pinY;
        float dz = -(float) Math.sqrt(Math.max(0, restY * restY + restZ * restZ - dy * dy));
        float travel = pinZ + dz - (centerZ + restZ);
        submitCoupling(QksModelCache.get("jaw_crusher"), state, pose, collector);
        pose.pushPose();
        if (wide) {
            pose.translate(0, 0, -1);
            pose.scale(2, 2, 2);
        }
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TEXTURE), (pass, consumer) -> {
            PoseStack work = new PoseStack();
            work.last().pose().set(pass.pose());
            work.last().normal().set(pass.normal());
            int light = state.lightCoords;
            QksCompiledModel.submitBone(model.bone("frame"), work.last(), consumer, light);
            work.pushPose();
            work.translate(0, .5f / scale, centerZ);
            work.mulPose(Axis.XP.rotation(state.angle));
            work.translate(0, -.5f / scale, -centerZ);
            QksCompiledModel.submitBone(model.bone("rotor"), work.last(), consumer, light);
            QksCompiledModel.submitBone(model.bone("crank_pin"), work.last(), consumer, light);
            work.popPose();
            work.pushPose();
            work.translate(0, pinY, pinZ);
            work.mulPose(Axis.XP.rotation((float) (Math.atan2(dz, dy) - Math.atan2(restZ, restY))));
            work.translate(0, -.5f / scale - eccentric, -centerZ);
            QksCompiledModel.submitBone(model.bone("connecting_rod"), work.last(), consumer, light);
            work.popPose();
            work.translate(0, 0, travel * .6f);
            QksCompiledModel.submitBone(model.bone("moving_jaw"), work.last(), consumer, light);
        });
        pose.popPose();
        float x = wide ? 1.5f : .5f;
        submitStack(state.input, pose, collector, state.lightCoords, x, .87f, .5f, .23f, 68, 1);
        submitStack(state.output, pose, collector, state.lightCoords, x, .29f, wide ? -.72f : .16f, .21f, 48, 1);
    }

    @Override
    public AABB getRenderBoundingBox(JawCrusherBlockEntity entity) {
        AABB base = new AABB(entity.getBlockPos());
        return entity.getBlockState().getValue(JawCrusherBlock.FORMED) ? base.inflate(1, 0, 1).expandTowards(0, 1, 0) : base;
    }

    /** Straight rotating axle on the same centerline as the neighboring shaft. */
    private static void submitCoupling(QksCompiledModel model, State state, PoseStack pose,
            SubmitNodeCollector collector) {
        var shell = model.bone("drive_coupling");
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TEXTURE), (pass, consumer) -> {
            PoseStack work = new PoseStack();
            work.last().pose().set(pass.pose());
            work.last().normal().set(pass.normal());
            float x = state.refined && !state.formed ? 1 - PORT_CENTER_X : PORT_CENTER_X;
            couplingBox(shell, work, consumer, state.lightCoords, x, PORT_CENTER_Y, PORT_CENTER_Z,
                    PORT_LENGTH, PORT_SECTION, PORT_SECTION, state.angle);
        });
    }

    private static void submitRod(QksCompiledModel.CompiledBone rod, PoseStack pose,
            com.mojang.blaze3d.vertex.VertexConsumer consumer, int light, float pinY, float pinZ,
            float dy, float dz, float restY, float restZ) {
        pose.pushPose();
        pose.translate(rod.pivotX(), pinY, pinZ);
        pose.mulPose(Axis.XP.rotation((float) Math.atan2(dz, dy)));
        pose.scale(1, (float) (Math.hypot(dy, dz) / Math.hypot(restY, restZ)), 1);
        pose.mulPose(Axis.XP.rotation(-(float) Math.atan2(restZ, restY)));
        pose.translate(-rod.pivotX(), -rod.pivotY(), -rod.pivotZ());
        QksCompiledModel.submitBone(rod, pose.last(), consumer, light);
        pose.popPose();
    }

    private static void couplingBox(QksCompiledModel.CompiledBone shell, PoseStack pose,
            com.mojang.blaze3d.vertex.VertexConsumer consumer, int light, float x, float y, float z,
            float sx, float sy, float sz, float angle) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(Axis.XP.rotation(angle));
        pose.scale(sx, sy, sz);
        pose.translate(-.5f, -.5f, -.5f);
        QksCompiledModel.submitBone(shell, pose.last(), consumer, light);
        pose.popPose();
    }

    private static void submitPart(QksCompiledModel.CompiledBone bone, PoseStack pose,
            com.mojang.blaze3d.vertex.VertexConsumer consumer, int light, float dy, float dz, float angle) {
        pose.pushPose();
        pose.translate(bone.pivotX(), bone.pivotY() + dy, bone.pivotZ() + dz);
        pose.mulPose(Axis.XP.rotation(angle));
        pose.translate(-bone.pivotX(), -bone.pivotY(), -bone.pivotZ());
        QksCompiledModel.submitBone(bone, pose.last(), consumer, light);
        pose.popPose();
    }

    private static void submitStack(ItemStackRenderState stack, PoseStack pose, SubmitNodeCollector collector,
            int light, float x, float y, float z, float scale, float tilt, float modelScale) {
        if (stack.isEmpty()) return;
        pose.pushPose();
        pose.translate(x, y, z);
        pose.scale(1 / modelScale, 1 / modelScale, 1 / modelScale);
        pose.mulPose(Axis.XP.rotationDegrees(tilt));
        pose.scale(scale, scale, scale);
        stack.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }
}
