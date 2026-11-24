package br.com.quantumindustries.qa;
import br.com.quantumindustries.block.entity.*;
import br.com.quantumindustries.client.inspection.ClientInspectionCache;
import br.com.quantumindustries.inspection.InspectionSnapshot;
import java.nio.file.*;
import java.util.List;
import javax.imageio.ImageIO;
import net.minecraft.client.*;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;
@EventBusSubscriber(modid="quantumindustries",value=Dist.CLIENT)
public final class SawmillPreviewClient {
    private static final List<String> SHOTS=List.of("01-sawmill-running.png","02-sawmill-output-disconnected.png","03-insulated-reserve.png","04-busbar-reserve.png");
    private static int total,age,stage=1,checks;private static boolean initialized,finished;private static boolean[] captured=new boolean[4];
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(!Boolean.getBoolean("quantumindustries.previewSawmill")||finished)return;
        var game=Minecraft.getInstance();
        try{
            if(!initialized){GLFW.glfwHideWindow(game.getWindow().handle());Files.createDirectories(game.gameDirectory.toPath().resolve("screenshots"));initialized=true;}
            if(++total>2200)throw new AssertionError("Preview timed out");
            if(SawmillPreviewSetup.failure!=null)throw new AssertionError(SawmillPreviewSetup.failure);
            if(game.player==null||game.level==null||!SawmillPreviewSetup.ready||SawmillPreviewSetup.applied!=stage)return;
            game.options.hideGui=false;age++;
            int wait=stage==1?125:45;
            if(!captured[stage-1]&&age>=wait){
                var pos=stage<=2?SawmillPreviewSetup.SAW:stage==3?SawmillPreviewSetup.CABLE:SawmillPreviewSetup.BUSBAR;
                check(game.player.pick(6,0,false) instanceof BlockHitResult hit&&hit.getBlockPos().equals(pos),"aim on requested block "+pos);
                if(stage<=2){
                    var saw=(MechanicalSawmillBlockEntity)game.level.getBlockEntity(pos);
                    check(saw.automationBound()&&saw.processingDuration()==100&&saw.input().getCount()>0,"server-authoritative stock/duration/binding synchronized");
                    check(saw.processingState()==MechanicalSawmillBlockEntity.ProcessingState.CUTTING,"cutting state synchronized");
                    if(stage==1){
                        if(saw.inputTransportState()!=2||saw.outputTransportState()!=2){
                            if(age<260)return;
                            throw new AssertionError("No live transfer overlap: in="+saw.inputTransportState()+" out="+saw.outputTransportState()+" stock="+saw.input()+" product="+saw.output()+" progress="+saw.progress());
                        }
                        check(true,"receiving and sending are real recent transfers");
                    }
                    else check(saw.outputTransportState()==0&&saw.output().getCount()>0,"disabled output and retained planks are shown");
                }else{
                    var snap=ClientInspectionCache.forPos(pos);check(snap!=null&&snap.kind()==InspectionSnapshot.Kind.ENERGY&&snap.load()>0&&snap.capacity()==50480,"connected reserve reaches client");
                    check(snap.localLoad()==(stage==3?64:32),"correct cable throughput capacity");
                }
                capture(game,SHOTS.get(stage-1));captured[stage-1]=true;
            }
            if(captured[stage-1]&&age>wait+10){
                if(stage<4){stage++;age=0;SawmillPreviewSetup.requested=stage;return;}
                for(String shot:SHOTS)check(ImageIO.read(game.gameDirectory.toPath().resolve("screenshots").resolve(shot).toFile())!=null,"decoded "+shot);
                Files.writeString(game.gameDirectory.toPath().resolve("preview-result.txt"),"PASS: "+checks+" client HUD checks and four native captures\n");finished=true;game.stop();
            }
        }catch(Throwable e){e.printStackTrace();finished=true;try{Files.writeString(game.gameDirectory.toPath().resolve("preview-result.txt"),"FAIL: "+e+"\n");}catch(Exception ignored){}game.stop();}
    }
    private static void capture(Minecraft game,String name){Screenshot.takeScreenshot(game.getMainRenderTarget(),image->{try(image){image.writeToFile(game.gameDirectory.toPath().resolve("screenshots").resolve(name));}catch(Exception e){throw new RuntimeException(e);}});}
}
