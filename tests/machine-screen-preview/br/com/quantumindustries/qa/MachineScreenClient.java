package br.com.quantumindustries.qa;

import br.com.quantumindustries.client.gui.MachineProcessingScreen;
import java.nio.file.Files;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid="quantumindustries",value=Dist.CLIENT)
public final class MachineScreenClient {
    private static int ticks,boot;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) throws Exception {
        if(!Boolean.getBoolean("quantumindustries.previewMachineScreen"))return;
        var game=Minecraft.getInstance();org.lwjgl.glfw.GLFW.glfwHideWindow(game.getWindow().handle());
        if(MachineScreenSetup.failure!=null || ++boot>1800){Files.writeString(game.gameDirectory.toPath().resolve("screen-client-checks.txt"),"FAIL: "+MachineScreenSetup.failure);game.stop();return;}
        if(MachineScreenSetup.stage==0)return;
        ticks++;
        if(ticks==40 || ticks==85){
            if(!(game.screen instanceof MachineProcessingScreen))throw new AssertionError("Native machine screen missing");
            Screenshot.grab(game.gameDirectory,ticks==40?"01-alloy-smelter.png":"02-jaw-crusher.png",game.getMainRenderTarget(),1,message->{});
            if(ticks==40)MachineScreenSetup.stage=2;
            else Files.writeString(game.gameDirectory.toPath().resolve("screen-client-checks.txt"),"PASS: both native machine screens displayed\n");
        }
        if(ticks==110)game.stop();
    }
}
