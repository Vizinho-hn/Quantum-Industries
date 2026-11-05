package br.com.quantumindustries.client.assay;

import br.com.quantumindustries.assay.*;
import br.com.quantumindustries.assay.AssayCatalog.*;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.resources.Identifier;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** One editable Blockbench geometry and one set of transforms for world and GUI. */
public final class AssayApparatus {
    public static final Identifier TEXTURE=Identifier.fromNamespaceAndPath("quantumindustries","textures/block/empirical_workshop.png");
    private record Box(String group,float[] from,float[] to,int material){}
    private static final List<Box> BOXES=load();
    private static List<Box> load(){
        try(var stream=AssayApparatus.class.getResourceAsStream("/assets/quantumindustries/assay/workshop.json")){
            if(stream==null)throw new IOException("Missing apparatus geometry");
            var root=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
            List<Box> boxes=new ArrayList<>();for(var el:root.getAsJsonArray("boxes")){
                var b=el.getAsJsonObject();boxes.add(new Box(b.get("group").getAsString(),vec(b.getAsJsonArray("from")),vec(b.getAsJsonArray("to")),b.get("material").getAsInt()));
            }return List.copyOf(boxes);
        }catch(IOException e){throw new IllegalStateException(e);}
    }
    private static float[] vec(JsonArray a){return new float[]{a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat()};}
    public static void draw(PoseStack pose,VertexConsumer out,AssayMotion motion,Material material,boolean full,int light,int tools){
        pose.pushPose();pose.scale(1/16f,1/16f,1/16f);
        for(var box:BOXES){
            if(!full&&box.group.equals("base"))continue;
            if(!visible(box.group,tools))continue;
            pose.pushPose();transform(pose,box.group,motion);
            int color=box.group.equals("liquid")?0xFF497FA5:0xFFFFFFFF;
            if(box.group.equals("liquid")&&motion.method()==Method.REACTION&&material!=null&&material.reactive())color=blend(0xFF497FA5,material.reactionColor(),motion.working());
            cuboid(pose,out,box.from,box.to,box.group.equals("liquid")?10:box.material,color,light);pose.popPose();
        }
        if((tools&4)!=0&&motion.method()==Method.REACTION&&motion.ticks()>=105&&motion.ticks()<155){
            float fall=(motion.ticks()%18)/18f;
            cuboid(pose,out,new float[]{25.2f,21.4f-fall*3,11.8f},new float[]{25.45f,21.85f-fall*3,12.05f},8,0xFFB4DBD1,light);
        }
        if((tools&4)!=0&&material!=null&&material.reactive()&&motion.method()==Method.REACTION&&motion.ticks()>=75&&motion.ticks()<185){
            for(int i=0;i<3;i++){
                float y=16.6f+((motion.ticks()*.025f+i*.55f)%1.5f);
                cuboid(pose,out,new float[]{23.6f+i,y,11.4f+i*.4f},new float[]{23.8f+i,y+.2f,11.6f+i*.4f},3,0xFFDDECE2,light);
            }
        }
        pose.popPose();
    }
    public static boolean visible(String group,int tools){
        if(group.equals("lupa")||group.equals("inspection_support"))return (tools&1)!=0;
        if(group.startsWith("bellows"))return (tools&2)!=0;
        if(group.equals("reaction_fixed")||group.equals("dropper")||group.equals("liquid"))return (tools&4)!=0;
        return true;
    }
    public static int blend(int a,int b,float t){int out=0xFF000000;for(int shift:new int[]{0,8,16})out|=(int)(((a>>shift)&255)*(1-t)+((b>>shift)&255)*t)<<shift;return out;}
    private static void transform(PoseStack pose,String group,AssayMotion m){
        switch(group){
            case "tray"->{} // The dish stays on the bench; only the actual item moves.
            case "head"->pose.translate(0,-2.2f*m.pressure(),0);
            case "screw"->{pose.translate(8,-2.2f*m.pressure(),7);pose.mulPose(Axis.YP.rotationDegrees(1440*m.pressure()));pose.translate(-8,0,-7);}
            case "lupa"->{if(m.method()==Method.INSPECTION){pose.translate(0,-3*m.deployed(),0);pose.translate(16,24,13);pose.mulPose(Axis.YP.rotationDegrees((float)Math.sin(m.ticks()*.045)*10*m.deployed()));pose.translate(-16,-24,-13);}}
            case "bellows_lid"->{pose.translate(28,16.3,4.5);pose.mulPose(Axis.ZP.rotationDegrees(16*(1-m.bellows())));pose.translate(-28,-16.3,-4.5);}
            case "bellows_leather"->{pose.translate(0,16,0);pose.scale(1,1+.75f*(1-m.bellows()),1);pose.translate(0,-16,0);}
            case "dropper"->{if(m.method()==Method.REACTION){pose.translate(-4*AssayMotion.smooth(AssayMotion.ramp(m.ticks(),80,105))*m.deployed(),0,0);pose.translate(29,24,12);pose.mulPose(Axis.ZP.rotationDegrees(-18*m.deployed()));pose.translate(-29,-24,-12);}}
            case "liquid"->{if(m.method()==Method.REACTION){pose.translate(0,16.4,0);pose.scale(1,1+(float)Math.sin(m.ticks()*.3)*.05f*m.deployed(),1);pose.translate(0,-16.4,0);}}
            default->{}
        }
    }
    private static final int[][] FACES={{0,1,3,2},{5,4,6,7},{4,0,2,6},{1,5,7,3},{2,3,7,6},{4,5,1,0}};
    private static final float[][] NORMALS={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};
    private static void cuboid(PoseStack pose,VertexConsumer out,float[] f,float[] t,int material,int color,int light){
        float[][] v={{f[0],f[1],f[2]},{t[0],f[1],f[2]},{f[0],t[1],f[2]},{t[0],t[1],f[2]}, {f[0],f[1],t[2]},{t[0],f[1],t[2]},{f[0],t[1],t[2]},{t[0],t[1],t[2]}};
        float u=(material%8)*.125f,w=(material/8)*.25f;
        for(int face=0;face<6;face++)for(int i=0;i<4;i++){
            var p=v[FACES[face][i]];float[] n=NORMALS[face];
            out.addVertex(pose.last(),p[0],p[1],p[2]).setColor(color).setUv(u+(i==1||i==2?.124f:.001f),w+(i>=2?.001f:.249f))
                .setOverlay(655360).setLight(light).setNormal(pose.last(),n[0],n[1],n[2]);
        }
    }
}
