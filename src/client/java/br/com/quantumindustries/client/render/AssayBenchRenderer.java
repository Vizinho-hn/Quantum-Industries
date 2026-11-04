package br.com.quantumindustries.client.render;

import br.com.quantumindustries.block.entity.EmpiricalAssayBenchBlockEntity;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid="quantumindustries", value=Dist.CLIENT)
public final class AssayBenchRenderer implements BlockEntityRenderer<EmpiricalAssayBenchBlockEntity, AssayBenchRenderer.State> {
    // Coordinates in the north-facing two-block model; the renderer belongs to the foot.
    private static final float[][] POSITIONS={{1.16f,1.04f,.52f},{1.60f,1.04f,.28f},{1.38f,1.04f,.75f},{.72f,1.04f,.76f}};
    private final ItemModelResolver items;
    public AssayBenchRenderer(BlockEntityRendererProvider.Context context) { items=context.itemModelResolver(); }
    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState[] items={new ItemStackRenderState(),new ItemStackRenderState(),new ItemStackRenderState(),new ItemStackRenderState()};
        Direction facing=Direction.NORTH;
    }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlocks.EMPIRICAL_ASSAY_ENTITY.get(),AssayBenchRenderer::new);
    }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(EmpiricalAssayBenchBlockEntity bench,State state,float dt,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(bench,state,dt,camera,overlay);
        state.facing=bench.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        for(int i=0;i<4;i++) {
            state.items[i].clear();
            items.updateForTopItem(state.items[i],bench.inventory().getItem(i),ItemDisplayContext.FIXED,bench.getLevel(),null,i);
        }
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera) {
        pose.pushPose();
        pose.translate(.5f,0,.5f);
        pose.mulPose(Axis.YP.rotationDegrees(180-state.facing.toYRot()));
        pose.translate(-.5f,0,-.5f);
        for(int i=0;i<4;i++) if(!state.items[i].isEmpty()) {
            pose.pushPose();
            float[] p=POSITIONS[i];pose.translate(p[0],p[1]+(i==3?.16f:0),p[2]);
            if(i!=3)pose.mulPose(Axis.XP.rotationDegrees(90));
            pose.scale(.32f,.32f,.32f);
            state.items[i].submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);
            pose.popPose();
        }
        pose.popPose();
    }
    @Override public boolean shouldRenderOffScreen() { return true; }
}
