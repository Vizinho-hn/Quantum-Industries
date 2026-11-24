package br.com.quantumindustries.client.render;

import br.com.quantumindustries.block.entity.MechanicalConveyorBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid="quantumindustries",value=Dist.CLIENT)
public final class MechanicalConveyorRenderer implements BlockEntityRenderer<MechanicalConveyorBlockEntity,MechanicalConveyorRenderer.State> {
    private final ItemModelResolver models;
    public MechanicalConveyorRenderer(BlockEntityRendererProvider.Context context) { models=context.itemModelResolver(); }
    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        Direction facing = Direction.NORTH;
        double fraction;
    }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlocks.MECHANICAL_CONVEYOR_ENTITY.get(),MechanicalConveyorRenderer::new);
    }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(MechanicalConveyorBlockEntity belt,State state,float partialTick,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(belt,state,partialTick,camera,overlay);
        state.item.clear();
        var packet=belt.packet();
        if(packet.isEmpty()||belt.getLevel()==null)return;
        state.facing=belt.facing(); state.fraction=belt.fraction();
        models.updateForTopItem(state.item,packet.copyWithCount(1),ItemDisplayContext.FIXED,belt.getLevel(),null,0);
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera) {
        if(state.item.isEmpty())return;
        pose.pushPose();
        double travel=state.fraction-.5;
        pose.translate(.5+state.facing.getStepX()*travel,.65,.5+state.facing.getStepZ()*travel);
        pose.scale(.42f,.42f,.42f);
        state.item.submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);
        pose.popPose();
    }
}
