package br.com.quantumindustries.client.render;

import br.com.quantumindustries.block.entity.ElectromechanicalPressBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = "quantumindustries", value = Dist.CLIENT)
public final class ElectromechanicalPressRenderer implements BlockEntityRenderer<ElectromechanicalPressBlockEntity, ElectromechanicalPressRenderer.State> {
    private final ItemModelResolver models;

    public ElectromechanicalPressRenderer(BlockEntityRendererProvider.Context context) {
        models = context.itemModelResolver();
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState workpiece = new ItemStackRenderState();
        Direction facing = Direction.NORTH;
        float pressRatio;
        boolean hasOutput;
    }

    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlocks.ELECTROMECHANICAL_PRESS_ENTITY.get(), ElectromechanicalPressRenderer::new);
    }

    @Override public State createRenderState() { return new State(); }

    @Override public void extractRenderState(ElectromechanicalPressBlockEntity press, State state, float partialTick,
            Vec3 camera, ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(press, state, partialTick, camera, overlay);
        state.workpiece.clear();
        state.pressRatio = 0.0f;
        state.hasOutput = false;
        if (press.getLevel() == null) return;
        ItemStack shown = !press.output().isEmpty() ? press.output() : press.input();
        if (shown.isEmpty()) return;
        state.facing = press.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        state.hasOutput = !press.output().isEmpty();
        state.pressRatio = press.progress() > 0 ? Math.min(1.0f, (float) press.progress() / ElectromechanicalPressBlockEntity.WORK_TICKS) : 0.0f;
        models.updateForTopItem(state.workpiece, shown.copyWithCount(1), ItemDisplayContext.FIXED, press.getLevel(), null, 0);
    }

    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.workpiece.isEmpty()) return;
        pose.pushPose();
        float pulse = state.pressRatio > 0.0f ? Math.abs((float) Math.sin(state.pressRatio * Math.PI * 3.0)) : 0.0f;
        float thicknessScale = state.pressRatio > 0.0f ? (0.45f - 0.14f * pulse) : 0.45f;
        pose.translate(0.5f, (state.hasOutput ? 5.9f : 6.1f) / 16f, 0.44f);
        pose.mulPose(Axis.YP.rotationDegrees(180 - state.facing.toYRot()));
        pose.mulPose(Axis.XP.rotationDegrees(90f));
        pose.scale(0.46f, 0.46f, thicknessScale);
        state.workpiece.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }
}
