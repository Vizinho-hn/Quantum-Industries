package br.com.quantumindustries.client.assay;

import br.com.quantumindustries.assay.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;

/** Renders the real submitted item, including its resource-pack model, in both views. */
public final class AssaySample {
    public static void submit(PoseStack pose,SubmitNodeCollector out,ItemStackRenderState item,AssayMotion motion,AssayCatalog.Material material,int light){
        if(item.isEmpty())return;
        float fade=motion.method()==AssayCatalog.Method.REACTION?1-AssayMotion.ramp(motion.ticks(),75,95):1;
        if(fade<=0)return;
        var bounds=item.getModelBoundingBox();
        double footprint=Math.max(.001,Math.max(bounds.getXsize(),bounds.getYsize()));
        float strain=motion.deformation();
        pose.pushPose();
        double drop=motion.method()==AssayCatalog.Method.REACTION?-4*AssayMotion.smooth(AssayMotion.ramp(motion.ticks(),45,75)):0;
        pose.translate((16+motion.trayX())/16,(16.25+motion.trayY()+drop)/16,(8+motion.trayZ())/16);
        if(motion.method()==AssayCatalog.Method.INSPECTION)pose.mulPose(Axis.YP.rotationDegrees((float)Math.sin(motion.ticks()*.04)*20*motion.deployed()));
        if(motion.method()==AssayCatalog.Method.HEATING&&motion.working()>.4f)light=0x00F000F0;
        int pieces=material!=null&&!material.malleable()&&strain>.45f?4:1;
        for(int i=0;i<pieces;i++){
            pose.pushPose();
            if(pieces>1){pose.translate((i%2==0?-1:1)*(1+strain)/24,0,(i<2?-1:1)*(1+strain)/24);pose.scale(.48f,1,.48f);}
            pose.scale((float)(.25/footprint)*(1+.35f*strain)*fade,(float)(.05/Math.max(.001,bounds.getZsize()))*(1-.55f*strain)*fade,(float)(.25/footprint)*(1+.15f*strain)*fade);
            pose.mulPose(Axis.XP.rotationDegrees(90));
            pose.translate(-(bounds.minX+bounds.maxX)/2,-(bounds.minY+bounds.maxY)/2,-bounds.maxZ);
            item.submit(pose,out,light,655360,0);pose.popPose();
        }
        pose.popPose();
    }
}
