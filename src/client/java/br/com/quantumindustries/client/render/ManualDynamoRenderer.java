package br.com.quantumindustries.client.render;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.block.entity.ManualDynamoBlockEntity;
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = "quantumindustries", value = Dist.CLIENT)
public final class ManualDynamoRenderer implements BlockEntityRenderer<ManualDynamoBlockEntity, ManualDynamoRenderer.State> {
    private final ItemModelResolver models;
    private ItemStack crank;

    public ManualDynamoRenderer(BlockEntityRendererProvider.Context context) {
        models = context.itemModelResolver();
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState crank = new ItemStackRenderState();
        Direction facing = Direction.NORTH;
        float angle;
    }

    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlocks.MANUAL_DYNAMO_ENTITY.get(), ManualDynamoRenderer::new);
    }

    @Override public State createRenderState() { return new State(); }

    @Override public void extractRenderState(ManualDynamoBlockEntity dynamo, State state, float partialTick,
            Vec3 camera, ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(dynamo, state, partialTick, camera, overlay);
        state.crank.clear();
        if (!dynamo.installed() || dynamo.getLevel() == null) return;
        state.facing = dynamo.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        float elapsed = dynamo.getLevel().getGameTime() - dynamo.lastTurn() + partialTick;
        state.angle = 360 * Math.clamp(elapsed / ManualDynamoBlockEntity.TURN_TICKS, 0f, 1f);
        if (crank == null) {
            crank = new ItemStack(ModItems.HAND_CRANK.get());
            crank.set(DataComponents.ITEM_MODEL,
                    Identifier.fromNamespaceAndPath("quantumindustries", "dynamo_crank_installed"));
        }
        models.updateForTopItem(state.crank, crank, ItemDisplayContext.FIXED, dynamo.getLevel(), null, 0);
    }

    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.crank.isEmpty()) return;
        pose.pushPose();
        pose.translate(.5f, 0, .5f);
        pose.mulPose(Axis.YP.rotationDegrees(180 - state.facing.toYRot()));
        pose.translate(6.95f / 16, 8.6f / 16, 0);
        pose.mulPose(Axis.XP.rotationDegrees(state.angle));
        state.crank.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }
}
