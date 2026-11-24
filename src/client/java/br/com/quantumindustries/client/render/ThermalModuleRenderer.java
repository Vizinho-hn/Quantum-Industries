package br.com.quantumindustries.client.render;

import br.com.quantumindustries.block.entity.ThermoelectricGeneratorBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Four attached model modules, rendered from synchronized machine inventory. */
@EventBusSubscriber(modid="quantumindustries",value=Dist.CLIENT)
public final class ThermalModuleRenderer implements BlockEntityRenderer<ThermoelectricGeneratorBlockEntity,ThermalModuleRenderer.State>{
    private final ItemModelResolver models;
    public ThermalModuleRenderer(BlockEntityRendererProvider.Context context){models=context.itemModelResolver();}
    public static final class State extends BlockEntityRenderState{
        final ItemStackRenderState[] modules={new ItemStackRenderState(),new ItemStackRenderState(),new ItemStackRenderState(),new ItemStackRenderState()};
        float rotation;
    }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){
        event.registerBlockEntityRenderer(ModBlocks.THERMOELECTRIC_GENERATOR_ENTITY.get(),ThermalModuleRenderer::new);
    }
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(ThermoelectricGeneratorBlockEntity entity,State state,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){
        BlockEntityRenderer.super.extractRenderState(entity,state,partial,camera,overlay);
        state.rotation=switch(entity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)){case EAST->-90;case SOUTH->180;case WEST->90;default->0;};
        for(int i=0;i<4;i++){
            state.modules[i].clear();
            var stack=entity.modules().getItem(i);
            if(!stack.isEmpty())models.updateForTopItem(state.modules[i],stack,ItemDisplayContext.FIXED,entity.getLevel(),null,0);
        }
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees(state.rotation));pose.translate(-.5,0,-.5);
        for(int i=0;i<4;i++){
            if(state.modules[i].isEmpty())continue;
            boolean left=i<2;
            pose.pushPose();pose.translate(left?.10:.90,.48,i%2==0?.30:.70);
            pose.mulPose(Axis.ZP.rotationDegrees(left?90:-90));pose.scale(.20f,.20f,.20f);
            state.modules[i].submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();
        }
        pose.popPose();
    }
}
