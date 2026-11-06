package br.com.quantumindustries.mechanical.preview;

import br.com.quantumindustries.client.MechanicalRecipeCache;
import br.com.quantumindustries.client.ResearchTableScreen;
import br.com.quantumindustries.research.ResearchCatalog;
import java.nio.file.Files;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid="quantumindustries",value=Dist.CLIENT)
public final class MechanicalPreviewClient {
    private static int ticks;
    private static void field(Object target,String name,int value)throws Exception {
        var f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.setInt(target,value);
    }
    private static void capture(Minecraft game,String name){
        Screenshot.grab(game.gameDirectory,name,game.getMainRenderTarget(),1,c->System.out.println("MECHANICAL_SCREENSHOT "+name));
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event)throws Exception {
        if(!Boolean.getBoolean("quantumindustries.previewMechanical"))return;
        var game=Minecraft.getInstance();
        if(game.player==null || MechanicalPreviewSetup.stage==0)return;
        ticks++;
        if(ticks==1){GLFW.glfwHideWindow(game.getWindow().handle());game.options.hideGui=true;}
        if(ticks==80)capture(game,"01-rotating-a.png");
        if(ticks==87)capture(game,"02-rotating-b.png");
        if(ticks==100)MechanicalPreviewSetup.stage=2;
        if(ticks==130)capture(game,"03-stopped-a.png");
        if(ticks==137)capture(game,"04-stopped-b.png");
        if(ticks==160){game.options.hideGui=false;MechanicalPreviewSetup.stage=3;}
        if(ticks==200){
            if(!(game.screen instanceof ResearchTableScreen))throw new AssertionError("Research notebook did not open");
            if(MechanicalRecipeCache.all().size()!=11)throw new AssertionError("Client did not receive all eleven processing recipes");
            int id=ResearchCatalog.indexOf("mechanical_transmission");
            var method=game.screen.getClass().getDeclaredMethod("unlockedRecipes");method.setAccessible(true);
            var rows=(java.util.List<?>)method.invoke(game.screen);
            field(game.screen,"recipePage",rows.indexOf(id)/2);
            field(game.screen,"tab",2);field(game.screen,"selected",id);field(game.screen,"guideResearch",id);field(game.screen,"guidePage",2);
        }
        if(ticks==230){
            var selected=game.screen.getClass().getDeclaredField("selected");selected.setAccessible(true);
            if(selected.getInt(game.screen)!=ResearchCatalog.indexOf("mechanical_transmission"))throw new AssertionError("Mechanical recipe page did not retain its selection");
            capture(game,"05-processing-recipes.png");
        }
        if(ticks==245)field(game.screen,"guidePage",1);
        if(ticks==265)capture(game,"06-machine-crafts.png");
        if(ticks==280){field(game.screen,"tab",0);field(game.screen,"selected",ResearchCatalog.indexOf("hydraulic_power"));}
        if(ticks==305)capture(game,"07-research-tree.png");
        if(ticks==325){Files.writeString(game.gameDirectory.toPath().resolve("preview-result.txt"),"PASS: actual client received 11 server processes; captured running/stopped rotors, notebook recipes and research tree\n");}
        if(ticks==345)game.stop();
    }
}
