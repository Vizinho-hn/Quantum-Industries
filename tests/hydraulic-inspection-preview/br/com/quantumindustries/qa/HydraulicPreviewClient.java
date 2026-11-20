package br.com.quantumindustries.qa;
import br.com.quantumindustries.client.inspection.ClientHydraulicInspection;
import br.com.quantumindustries.inspection.*;
import java.nio.file.*;
import java.util.List;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;
@EventBusSubscriber(modid="quantumindustries",value=Dist.CLIENT)
public final class HydraulicPreviewClient {
    private static final List<String> SHOTS=List.of("01-single-flow.png","02-supporting-flows.png","03-opposing-flows.png","04-cancellation.png","05-large-perimeter.png","06-flywheel-coasting.png");
    private static boolean initialized,finished;
    private static int total,stage=1,age,checks,cleanup;
    private static HydraulicInspection delayed;
    private static boolean[] taken=new boolean[6];
    private static void check(boolean ok,String text){checks++;if(!ok)throw new AssertionError(text);}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(!Boolean.getBoolean("quantumindustries.previewHydraulicInspection")||finished)return;
        var game=Minecraft.getInstance();
        try{
            if(!initialized){GLFW.glfwHideWindow(game.getWindow().handle());Files.createDirectories(game.gameDirectory.toPath().resolve("screenshots"));initialized=true;}
            if(++total>2400)throw new AssertionError("Preview startup/capture timeout");
            if(HydraulicPreviewSetup.failure!=null)throw new AssertionError(HydraulicPreviewSetup.failure);
            if(game.level==null||game.player==null||!HydraulicPreviewSetup.ready)return;
            game.options.hideGui=false;game.options.keyShift.setDown(true);
            if(HydraulicPreviewSetup.applied!=stage)return;
            age++;
            int wait=stage==6?105:60;
            if(!taken[stage-1]&&age>=wait){
                var data=ClientHydraulicInspection.active();
                if(data==null){if(age>wait+90)throw new AssertionError("No real server hydraulic packet at stage "+stage+" aim="+ClientHydraulicInspection.aimedWheel()+" player="+game.player.position());return;}
                check(data.target().equals(HydraulicPreviewSetup.CENTER),"current target");
                if(stage<=3)check(data.positive()>0,"supporting contribution received");
                if(stage==3||stage==4)check(data.negative()<0,"opposing contribution received");
                if(stage==4)check(Math.abs(data.resultant())<1e-6,"cancellation");
                if(stage==5)check(data.samples().size()>=5&&data.samples().stream().anyMatch(s->Math.abs(s.rz())==2),"large rim positions");
                if(stage==6)check(data.samples().isEmpty()&&Math.abs(data.omega())>1e-6,"water removed while flywheel coasts");
                capture(game,SHOTS.get(stage-1));taken[stage-1]=true;delayed=data;
                // Correlated replies: delayed IDs, old dimensions and old targets cannot replace the active data.
                var wrong=new HydraulicInspection(data.dimension(),data.target(),0,data.serverTick(),data.axle(),99,0,List.of());
                ClientHydraulicInspection.accept(wrong);check(ClientHydraulicInspection.active()==data,"delayed packet discarded");
                var wrongDimension = new HydraulicInspection(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft","the_nether"),
                        data.target(),data.requestId(),data.serverTick(),data.axle(),99,0,List.of());
                ClientHydraulicInspection.accept(wrongDimension);check(ClientHydraulicInspection.active()==data,"old-dimension reply discarded");
                var wrongTarget = new HydraulicInspection(data.dimension(),data.target().east(),data.requestId(),data.serverTick(),data.axle(),99,0,List.of());
                ClientHydraulicInspection.accept(wrongTarget);check(ClientHydraulicInspection.active()==data,"other-target reply discarded");
            }
            if(taken[stage-1]&&age>wait+12){
                if(stage<6){stage++;HydraulicPreviewSetup.requested=stage;age=0;return;}
                if(cleanup==0){
                    game.options.keyShift.setDown(false);game.player.setShiftKeyDown(false);
                    check(ClientHydraulicInspection.active()==null,"Shift release clears immediately");cleanup=1;return;
                }
                if(cleanup==1){
                    if(ClientHydraulicInspection.active()==null)return;
                    var helmet=game.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
                    game.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.item.ItemStack.EMPTY);
                    check(ClientHydraulicInspection.active()==null,"removed goggles clear immediately");
                    game.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,helmet);cleanup=2;return;
                }
                if(cleanup==2){
                    if(ClientHydraulicInspection.active()==null)return;
                    float head=game.player.yHeadRot,previous=game.player.yHeadRotO;
                    float body=game.player.getYRot(),bodyPrevious=game.player.yRotO;
                    game.player.setYRot(body+180);game.player.yRotO=body+180;
                    game.player.setYHeadRot(head+180);game.player.yHeadRotO=head+180;
                    check(ClientHydraulicInspection.active()==null,"look-away clears immediately");
                    game.player.setYHeadRot(head);game.player.yHeadRotO=previous;
                    game.player.setYRot(body);game.player.yRotO=bodyPrevious;cleanup=3;return;
                }
                if(cleanup==3){if(ClientHydraulicInspection.active()==null)return;HydraulicPreviewSetup.destroyRequested=true;cleanup=4;return;}
                if(cleanup==4){
                    if(game.level.getBlockEntity(HydraulicPreviewSetup.CENTER)!=null)return;
                    check(ClientHydraulicInspection.active()==null,"destroyed target clears client overlay");
                    HydraulicPreviewSetup.dimensionRequested=true;cleanup=5;return;
                }
                if(game.level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD))return;
                ClientHydraulicInspection.accept(delayed);
                check(ClientHydraulicInspection.active()==null,"actual dimension transition rejects old-world reply");
                for(String shot:SHOTS){var f=game.gameDirectory.toPath().resolve("screenshots").resolve(shot);check(Files.exists(f)&&ImageIO.read(f.toFile())!=null,"decoded screenshot "+shot);}
                Files.writeString(game.gameDirectory.toPath().resolve("preview-result.txt"),"PASS: "+checks+" client checks, six real server-authored hydraulic screenshots; visual review required\n");
                finished=true;game.stop();
            }
        }catch(Throwable e){
            e.printStackTrace();finished=true;
            try{Files.writeString(game.gameDirectory.toPath().resolve("preview-result.txt"),"FAIL: "+e+"\n");}catch(Exception ignored){}
            game.stop();
        }
    }
    private static void capture(Minecraft game,String name){
        Screenshot.takeScreenshot(game.getMainRenderTarget(),image->{
            try(image){image.writeToFile(game.gameDirectory.toPath().resolve("screenshots").resolve(name));}
            catch(Exception e){throw new RuntimeException(e);}
        });
    }
}
