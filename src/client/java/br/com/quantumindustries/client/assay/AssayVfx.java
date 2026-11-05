package br.com.quantumindustries.client.assay;

import br.com.quantumindustries.assay.*;
import br.com.quantumindustries.assay.AssayCatalog.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** PixelFX sprites, positioned in apparatus space in both world and GUI. */
public final class AssayVfx {
    public static final Identifier FLAME=Identifier.fromNamespaceAndPath("quantumindustries","textures/assay/burner.png");
    public static boolean burning(AssayMotion m){return m.method()==Method.HEATING&&m.ticks()>=30&&m.ticks()<185;}
    public static void submit(PoseStack pose,SubmitNodeCollector collector,AssayMotion m,Material material){
        if(!burning(m))return;
        collector.submitCustomGeometry(pose,RenderTypes.entityTranslucentEmissive(FLAME),(p,v)->{
            PoseStack stack=new PoseStack();stack.last().pose().set(p.pose());stack.last().normal().set(p.normal());draw(stack,v,m,material);
        });
    }
    public static void draw(PoseStack pose,VertexConsumer out,AssayMotion m,Material material){
        if(!burning(m))return;
        int frame=((int)m.ticks()/2)%20;
        // Flame-test reagent is represented by the kit, not a claim that a bare ingot burns green.
        int color=material==Material.COPPER&&m.working()>.5f?0xFF61FFB0:0xFFFFFFFF;
        float y=16.7f/16,h=(.45f+.15f*m.bellows());float u0=0,u1=1,v0=frame/20f,v1=(frame+1)/20f;
        float[][][] quads={{{21f/16,y,4f/16},{29f/16,y,4f/16},{29f/16,y+h,4f/16},{21f/16,y+h,4f/16}},{{25f/16,y,0f/16},{25f/16,y,8f/16},{25f/16,y+h,8f/16},{25f/16,y+h,0f/16}}};
        for(var quad:quads)for(int i=0;i<4;i++)out.addVertex(pose.last(),quad[i][0],quad[i][1],quad[i][2]).setColor(color)
            .setUv(i==0||i==3?u0:u1,i<2?v1:v0).setOverlay(655360).setLight(0x00F000F0).setNormal(pose.last(),0,0,-1);
    }
}
