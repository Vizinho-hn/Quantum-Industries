package br.com.quantumindustries.client.render;

import br.com.quantumindustries.block.entity.PneumaticTubeBlockEntity;
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
public final class PneumaticTubeRenderer implements BlockEntityRenderer<PneumaticTubeBlockEntity,PneumaticTubeRenderer.State> {
    private final ItemModelResolver models;
    public PneumaticTubeRenderer(BlockEntityRendererProvider.Context context){models=context.itemModelResolver();}
    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState item=new ItemStackRenderState();
        Direction inlet=Direction.NORTH;
        float fraction;
    }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){
        event.registerBlockEntityRenderer(ModBlocks.PNEUMATIC_TUBE_ENTITY.get(),PneumaticTubeRenderer::new);
    }
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(PneumaticTubeBlockEntity tube,State state,float partialTick,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){
        BlockEntityRenderer.super.extractRenderState(tube,state,partialTick,camera,overlay);
        state.item.clear();
        if(tube.packet().isEmpty()||tube.getLevel()==null)return;
        state.inlet=tube.inlet();
        state.fraction=Math.clamp((tube.getLevel().getGameTime()-tube.enteredAt()+partialTick)
                /PneumaticTubeBlockEntity.HOP_TICKS,0f,1f);
        models.updateForTopItem(state.item,tube.packet().copyWithCount(1),ItemDisplayContext.FIXED,tube.getLevel(),null,0);
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        if(state.item.isEmpty())return;
        pose.pushPose();
        double travel=0.44d*(1d-state.fraction);
        pose.translate(0.5d+state.inlet.getStepX()*travel,0.5d+state.inlet.getStepY()*travel,
                0.5d+state.inlet.getStepZ()*travel);
        pose.scale(0.38f,0.38f,0.38f);
        state.item.submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);
        pose.popPose();
    }
}
