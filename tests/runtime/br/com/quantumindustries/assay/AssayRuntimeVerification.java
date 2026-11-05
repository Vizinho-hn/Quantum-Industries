package br.com.quantumindustries.assay;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.assay.AssayCatalog.*;
import br.com.quantumindustries.block.entity.EmpiricalAssayBenchBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.menu.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import java.nio.file.*;
import java.util.UUID;

/** Opt-in real dedicated-server verification of the physical experiment lifecycle. */
@EventBusSubscriber(modid="quantumindustries")
public final class AssayRuntimeVerification {
    private static int checks;
    private static final BlockPos POS=new BlockPos(0,100,0);
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static FakePlayer player(ServerLevel level){var p=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"AssayTest"));p.setPos(.5,100,-2);return p;}
    private static EmpiricalAssayBenchBlockEntity place(ServerLevel level,BlockPos pos,Direction facing){
        var state=ModBlocks.EMPIRICAL_ASSAY_BENCH.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,facing);
        level.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        level.setBlock(pos,state,2);level.setBlock(pos.relative(facing.getClockWise()),state.setValue(BlockStateProperties.BED_PART,BedPart.HEAD),2);
        return (EmpiricalAssayBenchBlockEntity)level.getBlockEntity(pos);
    }
    private static EmpiricalAssayBenchMenu open(FakePlayer player,BlockPos pos){var m=new EmpiricalAssayBenchMenu(1,player.getInventory(),pos);player.containerMenu=m;return m;}
    private static void tools(EmpiricalAssayBenchMenu m){m.getSlot(1).set(new ItemStack(ModItems.MAGNIFYING_GLASS.get()));m.getSlot(2).set(new ItemStack(ModItems.MANUAL_BELLOWS.get()));m.getSlot(3).set(new ItemStack(ModItems.ASSAY_FLASK.get()));}
    private static void ticks(EmpiricalAssayBenchBlockEntity bench,FakePlayer author,int n){
        for(int i=0;i<n;i++){bench.tick();AssayDelivery.of((ServerLevel)author.level()).deliver(author);if(author.containerMenu instanceof EmpiricalAssayBenchMenu m)m.serverTick(author);}
    }
    @SubscribeEvent public static void started(ServerStartedEvent event)throws Exception{
        if(!Boolean.getBoolean("quantumindustries.verifyAssay"))return;
        try{run(event.getServer().overworld());Files.writeString(Path.of("../result.txt"),"PASS: "+checks+" server assertions\n");}
        catch(Throwable error){Files.writeString(Path.of("../result.txt"),"FAIL: "+error+"\n");error.printStackTrace();}
        finally{event.getServer().halt(false);}
    }
    private static void run(ServerLevel level){
        var author=player(level);var other=player(level);var bench=place(level,POS,Direction.NORTH);var menu=open(author,POS);
        check(bench.toolMask()==0,"empty slots do not install any instruments");tools(menu);
        check(bench.toolMask()==7,"three inserted instruments installed");
        for(int slot=1;slot<=3;slot++){var stack=menu.getSlot(slot).getItem().copy();menu.getSlot(slot).set(ItemStack.EMPTY);check((bench.toolMask()&(1<<(slot-1)))==0,"removed instrument disappears");menu.getSlot(slot).set(stack);}
        check(Method.values().length==4,"four methods");check(AssayCatalog.TEST_TICKS==200,"ten-second assays");
        check(!menu.clickMenuButton(author,0),"empty start rejected");check(!menu.clickMenuButton(other,0),"foreign menu rejected");check(!menu.clickMenuButton(author,999),"invalid action rejected");
        for(var material:Material.values()){
            menu.getSlot(0).set(material.sample());tools(menu);
            for(var method:Method.values()){
                check(menu.clickMenuButton(author,1+method.ordinal()),"select "+method);
                if(method!=Method.MECHANICAL){int slot=method==Method.INSPECTION?1:method==Method.HEATING?2:3;var tool=menu.getSlot(slot).getItem().copy();menu.getSlot(slot).set(ItemStack.EMPTY);check(!menu.clickMenuButton(author,0),"required tool "+method);menu.getSlot(slot).set(tool);}
                check(menu.clickMenuButton(author,0),"start "+material+" "+method);check(menu.getSlot(0).getItem().isEmpty(),"one sample consumed for entire research");
                check(!menu.clickMenuButton(author,0),"reject double start");check(!menu.getSlot(1).mayPickup(author),"lock tools during test");
                ticks(bench,author,75);check(!AssayProfile.of(author).knows(material,method),"no early discovery");
                menu.removed(author);author.containerMenu=author.inventoryMenu;
                // Closing and walking away must not stop world-owned work.
                author.setPos(30,100,30);ticks(bench,author,124);check(bench.busy(),"running through tick 199");
                ticks(bench,author,1);check(!bench.busy(),"finished exactly at tick 200");check(AssayProfile.of(author).knows(material,method),"discovery with HUD closed");
                author.setPos(.5,100,-2);menu=open(author,POS);menu.clickMenuButton(author,10+material.ordinal());
                check(!menu.clickMenuButton(author,0),"known test does not rerun");
                if(method!=Method.MECHANICAL){var view=open(other,POS);check(!view.ready(),"other researcher cannot take reserved sample");other.containerMenu=other.inventoryMenu;}
            }
            check(menu.discoveries()==4,"four results per material");check(AssayProfile.of(author).reserved()==null,"research reserve cleared after fourth result");
        }
        check(menu.totalDiscoveries()==40,"all forty discoveries");check(AssayProfile.of(other).mask(Material.COPPER)==0,"authors independent");
        // Stable-key migration: never call an old galvanic result a chemical reaction.
        var legacy=player(level);legacy.getPersistentData().putInt("quantum_empirical_copper",3);
        var profile=AssayProfile.of(legacy);check(profile.knows(Material.COPPER,Method.MECHANICAL),"legacy press");check(profile.knows(Material.COPPER,Method.HEATING),"legacy heat");check(!profile.knows(Material.COPPER,Method.REACTION),"legacy galvanic is not chemistry");check(!profile.knows(Material.COPPER,Method.INSPECTION),"inspection requires new observation");
        check(legacy.getPersistentData().getCompound(AssayProfile.KEY).orElseThrow().getCompound("discoveries").orElseThrow().getBoolean("copper/galvanic").orElse(false),"galvanic history retained");
        // Persist active machine and finish with no online author; queue survives machine removal.
        var absent=player(level);bench=place(level,POS,Direction.NORTH);bench.inventory().setItem(0,new ItemStack(Items.RAW_COPPER));
        check(bench.start(absent,Material.COPPER,Method.MECHANICAL),"start offline case");ticks(bench,author,90);
        var saved=bench.saveWithFullMetadata(level.registryAccess());
        var restored=(EmpiricalAssayBenchBlockEntity)BlockEntity.loadStatic(POS,bench.getBlockState(),saved,level.registryAccess());
        check(restored!=null&&restored.progress==90&&restored.method()==Method.MECHANICAL,"active state survives NBT roundtrip");
        check(restored.sample().is(Items.RAW_COPPER),"reserved sample preserves actual raw item model identity");
        restored.setLevel(level);for(int i=0;i<110;i++)restored.tick();check(!restored.busy(),"offline completion frees apparatus");
        check(!AssayProfile.of(absent).knows(Material.COPPER,Method.MECHANICAL),"result awaits disconnected author");
        var encoded=AssayDelivery.TYPE.codec().encodeStart(com.mojang.serialization.JsonOps.INSTANCE,AssayDelivery.of(level)).getOrThrow();
        var diskQueue=AssayDelivery.TYPE.codec().parse(com.mojang.serialization.JsonOps.INSTANCE,encoded).getOrThrow();
        diskQueue.deliver(absent);check(AssayProfile.of(absent).knows(Material.COPPER,Method.MECHANICAL),"queued result delivered once");
        AssayDelivery.of(level).deliver(absent);check(AssayProfile.of(absent).mask(Material.COPPER)==(1<<Method.MECHANICAL.ordinal()),"delivery idempotent");
        var interrupted=player(level);bench=place(level,POS,Direction.NORTH);bench.inventory().setItem(0,new ItemStack(Items.COPPER_INGOT,2));
        check(bench.start(interrupted,Material.COPPER,Method.HEATING),"start break case");check(bench.inventory().getItem(0).getCount()==1,"consume only one from stack");
        for(int i=0;i<35;i++)bench.tick();
        bench.preRemoveSideEffects(POS,bench.getBlockState());
        check(bench.inventory().getItem(0).isEmpty(),"physical items removed once on break");
        check(!AssayProfile.of(interrupted).knows(Material.COPPER,Method.HEATING),"breaking does not grant unfinished result");
        for(Direction facing:Direction.Plane.HORIZONTAL){
            bench=place(level,POS,facing);var local=bench.apparatus(24,16,8);var delta=local.subtract(POS.getX()+.5,POS.getY(),POS.getZ()+.5);
            var right=facing.getClockWise();check(Math.abs(delta.x-right.getStepX())<.0001&&Math.abs(delta.z-right.getStepZ())<.0001,"apparatus facing "+facing);
            check(level.getBlockEntity(POS.relative(right))==null,"single block entity for both halves");
        }
        // Motion contact: preparation and return close cleanly; mechanical travel reaches sample height.
        check(new AssayMotion(Method.MECHANICAL,0).deployed()==0,"initial pose");check(new AssayMotion(Method.MECHANICAL,145).pressure()==1,"press contact hold");check(new AssayMotion(Method.MECHANICAL,200).deployed()==0,"return pose");
        check(new AssayMotion(Method.MECHANICAL,70).deformation()==0,"sample does not flatten before ram contact");
        for(int t:new int[]{120,145,180}){
            var motion=new AssayMotion(Method.MECHANICAL,t);
            float ramBottom=18.8f-2.2f*motion.pressure();
            float sampleTop=16.25f+.8f*(1-.55f*motion.deformation());
            check(Math.abs(ramBottom-sampleTop)<.025f,"ram stays in contact with compressed sample at "+t);
        }
        check(new AssayMotion(Method.MECHANICAL,195).deformation()>.999f,"metal retains strain as ram retracts");
        checks+=ResearchRuntimeVerification.run(level);
    }
}
