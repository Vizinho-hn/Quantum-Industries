package br.com.quantumindustries.assay;

import br.com.quantumindustries.client.gui.EmpiricalAssayBenchScreen;
import br.com.quantumindustries.menu.EmpiricalAssayBenchMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid="quantumindustries",value=Dist.CLIENT)
public final class AssayClientPreview {
    private static int ticks;
    private static int photoTicks;
    private static boolean photos;
    private static void capture(Minecraft mc,String name) {
        Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),1,c->System.out.println("ASSAY_SCREENSHOT "+c.getString()));
    }
    private static void click(EmpiricalAssayBenchScreen screen,int x,int y) {
        int left=(screen.width-350)/2,top=(screen.height-236)/2;
        screen.mouseClicked(new MouseButtonEvent(left+x,top+y,new MouseButtonInfo(0,0)),false);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("quantumindustries.previewAssay"))return;
        Minecraft mc=Minecraft.getInstance();
        if(photos) {
            photoTicks++;
            if(photoTicks==25){capture(mc,"06-bench-iron.png");AssayPreviewSetup.photoStage=2;}
            if(photoTicks==50){capture(mc,"07-bench-gold.png");AssayPreviewSetup.photoStage=3;}
            if(photoTicks==75)capture(mc,"08-bench-empty.png");
            if(photoTicks==95)mc.stop();
            return;
        }
        if(!(mc.screen instanceof EmpiricalAssayBenchScreen screen))return;
        ticks++;
        if(ticks==20)capture(mc,"01-unknown.png");
        if(ticks==30)click(screen,290,165);
        if(ticks==60)capture(mc,"02-running.png");
        if(ticks==110)capture(mc,"03-discovered.png");
        if(ticks==120)click(screen,160,88);
        if(ticks==130)click(screen,290,165);
        if(ticks==210)capture(mc,"04-heating.png");
        if(ticks==220)click(screen,160,120);
        if(ticks==230)click(screen,290,165);
        if(ticks==310)capture(mc,"05-complete.png");
        if(ticks==320 && mc.player!=null) {
            mc.player.closeContainer();
            mc.player.setYRot(180);mc.player.setXRot(22);
            mc.options.hideGui=true;
            AssayPreviewSetup.photoStage=1;photos=true;
        }
    }
}
