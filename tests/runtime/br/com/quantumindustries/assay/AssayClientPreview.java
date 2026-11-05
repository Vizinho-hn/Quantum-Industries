package br.com.quantumindustries.assay;

import br.com.quantumindustries.client.gui.EmpiricalAssayBenchScreen;
import net.minecraft.client.*;
import net.minecraft.client.input.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Captures genuine GUI and world rendering in the disposable preview world. */
@EventBusSubscriber(modid="quantumindustries",value=Dist.CLIENT)
public final class AssayClientPreview {
    private static int ticks;private static boolean started;
    private static void capture(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),1,c->System.out.println("ASSAY_SCREENSHOT "+name));}
    private static void click(EmpiricalAssayBenchScreen s,int x,int y){int left=(s.width-380)/2,top=(s.height-236)/2;s.mouseClicked(new MouseButtonEvent(left+x,top+y,new MouseButtonInfo(0,0)),false);}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(!Boolean.getBoolean("quantumindustries.previewAssay"))return;var mc=Minecraft.getInstance();
        if(!started){if(!(mc.screen instanceof EmpiricalAssayBenchScreen))return;started=true;}
        ticks++;int cycle=(ticks-1)/250,step=(ticks-1)%250+1;
        if(cycle==0&&step==5)capture(mc,"v3-empty-tools.png");
        if(cycle==0&&step==8)AssayPreviewSetup.installTools=true;
        if(cycle<4){
            if(step==10&&mc.screen instanceof EmpiricalAssayBenchScreen s)click(s,106+cycle*48,140);
            if(step==20&&mc.screen instanceof EmpiricalAssayBenchScreen s)click(s,324,168);
            if(step==90){
                var b=mc.level.getBlockEntity(new net.minecraft.core.BlockPos(0,100,0));
                if(b==null||mc.getBlockEntityRenderDispatcher().getRenderer(b).getClass()!=br.com.quantumindustries.client.renderer.EmpiricalAssayRenderer.class)
                    throw new AssertionError("The live apparatus renderer must be registered exactly once");
                capture(mc,"v2-"+cycle+"-hud.png");
            }
            if(step==100&&mc.player!=null){mc.player.closeContainer();mc.options.hideGui=true;}
            if(step==135)capture(mc,"v2-"+cycle+"-world.png");
            if(step==230){AssayPreviewSetup.photoStage=cycle+1;mc.options.hideGui=false;}
        }
        if(ticks==1020)capture(mc,"v2-complete.png");
        if(ticks==1050)mc.stop();
    }
}
