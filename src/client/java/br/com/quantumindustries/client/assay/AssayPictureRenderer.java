package br.com.quantumindustries.client.assay;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;

public final class AssayPictureRenderer extends PictureInPictureRenderer<AssayPictureState> {
    public AssayPictureRenderer(MultiBufferSource.BufferSource source){super(source);}
    @Override public Class<AssayPictureState> getRenderStateClass(){return AssayPictureState.class;}
    @Override protected float getTranslateY(int height,int guiScale){return height/2f;}
    @Override protected String getTextureLabel(){return "empirical_apparatus";}
    @Override protected void renderToTexture(AssayPictureState state,PoseStack pose){
        Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
        pose.scale(1,-1,1);pose.mulPose(Axis.XP.rotationDegrees(-25));pose.mulPose(Axis.YP.rotationDegrees(-25));
        switch(state.focus()){
            case MECHANICAL->pose.translate(-.5,-1.43,-.48);
            case INSPECTION->pose.translate(-1,-1.25,-.5);
            case HEATING->pose.translate(-1.55,-1.16,-.25);
            case REACTION->pose.translate(-1.55,-1.3,-.75);
        }
        AssayApparatus.draw(pose,bufferSource.getBuffer(RenderTypes.entityCutout(AssayApparatus.TEXTURE)),state.motion(),state.material(),true,0x00F000F0,state.tools());
        var features=Minecraft.getInstance().gameRenderer.getFeatureRenderDispatcher();
        AssaySample.submit(pose,features.getSubmitNodeStorage(),state.sample(),state.motion(),state.material(),0x00F000F0);
        features.renderAllFeatures();
        if(AssayVfx.burning(state.motion()))AssayVfx.draw(pose,bufferSource.getBuffer(RenderTypes.entityTranslucentEmissive(AssayVfx.FLAME)),state.motion(),state.material());
    }
}
