package br.com.quantumindustries.client.render;

import br.com.quantumindustries.block.entity.BellowsBrazierBlockEntity;
import br.com.quantumindustries.block.entity.ElectricAlloySmelterBlockEntity;
import br.com.quantumindustries.block.entity.WireDrawingBenchBlockEntity;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Renders the active input/output workpiece resting on the grate or drawbed of Era II processing machines. */
@EventBusSubscriber(modid = "quantumindustries", value = Dist.CLIENT)
public final class MachineWorkpieceRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, MachineWorkpieceRenderer.State> {
    private final ItemModelResolver models;

    public MachineWorkpieceRenderer(BlockEntityRendererProvider.Context context) {
        this.models = context.itemModelResolver();
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        Direction facing = Direction.NORTH;
        float x = 0.5f, y = 0.72f, z = 0.5f;
        float scale = 0.45f;
        boolean working;
        long gameTime;
    }

    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlocks.BELLOWS_BRAZIER_ENTITY.get(), MachineWorkpieceRenderer::new);
        event.registerBlockEntityRenderer(ModBlocks.WIRE_DRAWING_BENCH_ENTITY.get(), MachineWorkpieceRenderer::new);
        event.registerBlockEntityRenderer(ModBlocks.ELECTRIC_ALLOY_SMELTER_ENTITY.get(), MachineWorkpieceRenderer::new);
    }

    @Override public State createRenderState() { return new State(); }

    @Override public void extractRenderState(T entity, State state, float partialTick,
            Vec3 camera, ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, camera, overlay);
        state.item.clear();
        state.working = false;
        Level level = entity.getLevel();
        if (level == null) return;
        state.gameTime = level.getGameTime();
        state.facing = entity.getBlockState().getValueOrElse(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);

        ItemStack shown = ItemStack.EMPTY;
        if (entity instanceof BellowsBrazierBlockEntity brazier) {
            shown = !brazier.output().isEmpty() ? brazier.output() : brazier.input();
            state.x = 0.5f; state.y = 11.7f / 16f; state.z = 0.5f; state.scale = 0.48f;
            state.working = brazier.progress() > 0;
        } else if (entity instanceof WireDrawingBenchBlockEntity bench) {
            shown = !bench.output().isEmpty() ? bench.output() : bench.input();
            state.x = 0.5f; state.y = 11.4f / 16f; state.z = 0.5f; state.scale = 0.42f;
            state.working = bench.progress() > 0;
        } else if (entity instanceof ElectricAlloySmelterBlockEntity smelter) {
            shown = !smelter.output().isEmpty() ? smelter.output()
                    : (!smelter.inputA().isEmpty() ? smelter.inputA() : smelter.inputB());
            state.x = 0.5f; state.y = 16.05f / 16f; state.z = 0.5f; state.scale = 0.40f;
            state.working = smelter.progress() > 0;
        }

        if (shown.isEmpty()) return;
        models.updateForTopItem(state.item, shown.copyWithCount(1), ItemDisplayContext.FIXED, level, null, 0);
    }

    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.item.isEmpty()) return;
        pose.pushPose();
        float jitter = state.working ? (float) Math.sin(state.gameTime * 0.45) * 0.015f : 0.0f;
        pose.translate(state.x, state.y + jitter, state.z);
        pose.mulPose(Axis.YP.rotationDegrees(180 - state.facing.toYRot()));
        pose.mulPose(Axis.XP.rotationDegrees(90f));
        pose.scale(state.scale, state.scale, state.scale);
        state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }
}
