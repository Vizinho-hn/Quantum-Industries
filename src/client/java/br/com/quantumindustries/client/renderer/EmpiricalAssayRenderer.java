package br.com.quantumindustries.client.renderer;

import br.com.quantumindustries.block.entity.EmpiricalAssayBenchBlockEntity;
import br.com.quantumindustries.assay.*;
import br.com.quantumindustries.client.assay.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class EmpiricalAssayRenderer implements BlockEntityRenderer<EmpiricalAssayBenchBlockEntity,EmpiricalAssayRenderer.State> {
    private final net.minecraft.client.renderer.item.ItemModelResolver items;
    public static final class State extends BlockEntityRenderState {
        AssayMotion motion;AssayCatalog.Material material;float rotation;int tools;
        final net.minecraft.client.renderer.item.ItemStackRenderState sample=new net.minecraft.client.renderer.item.ItemStackRenderState();
    }
    public EmpiricalAssayRenderer(BlockEntityRendererProvider.Context context){items=context.itemModelResolver();}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(EmpiricalAssayBenchBlockEntity entity,State state,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){
        BlockEntityRenderer.super.extractRenderState(entity,state,partial,camera,overlay);
        state.motion=new AssayMotion(entity.method(),entity.progress+(entity.busy()?partial:0));
        state.material=AssayCatalog.material(entity.sample());state.tools=entity.toolMask();
        state.sample.clear();items.updateForTopItem(state.sample,entity.sample(),net.minecraft.world.item.ItemDisplayContext.FIXED,entity.getLevel(),null,0);
        state.rotation=switch(entity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)){case EAST->-90;case SOUTH->180;case WEST->90;default->0;};
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees(state.rotation));pose.translate(-.5,0,-.5);
        collector.submitCustomGeometry(pose,RenderTypes.entityCutout(AssayApparatus.TEXTURE),(p,v)->{
            PoseStack stack=new PoseStack();stack.last().pose().set(p.pose());stack.last().normal().set(p.normal());
            AssayApparatus.draw(stack,v,state.motion,state.material,false,state.lightCoords,state.tools);
        });
        AssaySample.submit(pose,collector,state.sample,state.motion,state.material,state.lightCoords);
        AssayVfx.submit(pose,collector,state.motion,state.material);
        pose.popPose();
    }
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(EmpiricalAssayBenchBlockEntity entity){
        var partner=entity.getBlockPos().relative(entity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise());
        return new net.minecraft.world.phys.AABB(entity.getBlockPos()).minmax(new net.minecraft.world.phys.AABB(partner)).expandTowards(0,1.1,0);
    }
}
