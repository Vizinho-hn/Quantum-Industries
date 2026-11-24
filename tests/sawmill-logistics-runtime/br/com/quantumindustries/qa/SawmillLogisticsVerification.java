package br.com.quantumindustries.qa;
import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.block.entity.*;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.inspection.*;
import br.com.quantumindustries.logistics.*;
import br.com.quantumindustries.mechanical.*;
import br.com.quantumindustries.research.WorldResearchData;
import com.mojang.authlib.GameProfile;
import java.nio.file.*;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
@EventBusSubscriber(modid="quantumindustries")
public final class SawmillLogisticsVerification {
    private static final BlockPos SAW=new BlockPos(8,110,8),CABLE=new BlockPos(5,110,3);
    private static ServerLevel level;private static FakePlayer player;private static boolean active,reading;private static int ticks,checks;
    private static void check(boolean ok,String text){checks++;if(!ok)throw new AssertionError("tick="+ticks+" "+text);}
    private static MechanicalSawmillBlockEntity saw(){return (MechanicalSawmillBlockEntity)level.getBlockEntity(SAW);}
    private static ChestBlockEntity chest(BlockPos p){return (ChestBlockEntity)level.getBlockEntity(p);}
    private static void mode(BlockPos p,Direction d,ConnectionMode m){var c=(ConfigurableConduit)level.getBlockEntity(p);c.connectionModes().set(d,m);c.connectionChanged();}
    private static void put(BlockPos p,Block b){level.setBlock(p,b.defaultBlockState(),3);}
    @SubscribeEvent public static void start(ServerStartedEvent event)throws Exception{
        if(!Boolean.getBoolean("quantumindustries.verifySawmillLogistics"))return;
        level=event.getServer().overworld();level.setChunkForced(0,0,true);
        player=new FakePlayer(level,new GameProfile(UUID.fromString("ad64a9dd-26ab-4af5-8cfa-b721e000df77"),"SawmillQA"));player.setPos(8.5,110,10.5);
        reading=Files.exists(Path.of("../restart-ready.txt"));
        try{
            if(reading){
                check(saw().automationBound(),"automation owner survives full restart");
                check(saw().canInsertAutomated(new ItemStack(Items.OAK_LOG)),"offline owner's persisted research permits automation after restart");
                check(chest(SAW.south(2)).getItem(0).getCount()==12,"delivered planks persist");
                check(chest(SAW.north(2)).getItem(0).is(Items.STONE),"rejected inventory slot persists");
            }else{setup();verifyCable(CABLE,ModBlocks.INSULATED_CONDUIT.get());verifyCable(CABLE.south(),ModBlocks.COPPER_BUSBAR.get());}
            active=true;
        }catch(Throwable e){finish(e);}
    }
    private static void setup(){
        WorldResearchData.get(level).unlockAll(player);
        for(int x=2;x<=12;x++)for(int y=107;y<=113;y++)for(int z=1;z<=12;z++)put(new BlockPos(x,y,z),Blocks.AIR);
        level.setBlock(SAW,ModBlocks.MECHANICAL_SAWMILL.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH),3);
        var packet=new ItemStack(Items.OAK_LOG);
        check(ItemRouting.isEndpoint(level,SAW),"sawmill is transport endpoint");
        check(!ItemRouting.insert(level,SAW,packet,Direction.NORTH)&&packet.getCount()==1,"unbound machine rejects automation without consuming item");
        var novice=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"NoviceQA"));novice.setPos(8.5,110,10.5);
        check(!saw().bindAutomation(novice),"another player's knowledge cannot authorize novice");
        ModBlocks.MECHANICAL_SAWMILL.get().setPlacedBy(level,SAW,level.getBlockState(SAW),player,new ItemStack(ModItems.MECHANICAL_SAWMILL.get()));
        check(saw().automationBound(),"placement binds researched operator");
        var dirt=new ItemStack(Items.DIRT);
        check(!saw().insertAutomated(dirt)&&dirt.getCount()==1,"invalid input is preserved");
        WorldResearchData.get(level).clearAll(player);
        check(!saw().canInsertAutomated(packet),"clearing owner's research blocks automation immediately");
        WorldResearchData.get(level).unlockAll(player);
        put(SAW.north(2),Blocks.CHEST);put(SAW.south(2),Blocks.CHEST);
        for(var pos:java.util.List.of(SAW.north(),SAW.south())){
            put(pos,ModBlocks.PNEUMATIC_TUBE.get());for(var d:Direction.values())mode(pos,d,ConnectionMode.NONE);
            mode(pos,Direction.NORTH,ConnectionMode.PULL);mode(pos,Direction.SOUTH,ConnectionMode.PUSH);
        }
        chest(SAW.north(2)).setItem(0,new ItemStack(Items.STONE,8));chest(SAW.north(2)).setItem(1,new ItemStack(Items.OAK_LOG,2));
        level.setBlock(SAW.west(),ModBlocks.MECHANICAL_SHAFT_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.AXIS,Direction.Axis.X),3);
        level.setBlock(SAW.west(2),ModBlocks.PRIMITIVE_WATER_WHEEL.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.EAST),3);
        MechanicalNetwork.invalidate(level);
    }
    private static void verifyCable(BlockPos pos,Block block){
        put(pos.west(),ModBlocks.LEAD_ACID_ACCUMULATOR.get());put(pos.east(),ModBlocks.LEAD_ACID_ACCUMULATOR.get());put(pos,block);
        for(var d:Direction.values())mode(pos,d,ConnectionMode.NONE);
        mode(pos,Direction.WEST,ConnectionMode.AUTO);mode(pos,Direction.EAST,ConnectionMode.AUTO);
        var left=(EnergyInspectable)level.getBlockEntity(pos.west());var right=(EnergyInspectable)level.getBlockEntity(pos.east());
        check(left.receiveEnergy(256)==256&&right.receiveEnergy(128)==128,"seed real connected reserve");
        var cable=(CopperCableBlockEntity)level.getBlockEntity(pos);cable.recordTransfer(16,1,level.getGameTime());
        var snap=new EnergyInspectionProvider().inspect(level,pos,true).orElseThrow();
        check(snap.load()==384&&snap.capacity()==100000,"connected storage sums real buffers without adding transient cable flow");
        check(snap.localCapacity()==16&&snap.localLoad()==cable.capacity(),"flow and throughput remain separate from reserve");
        mode(pos,Direction.EAST,ConnectionMode.NONE);
        snap=new EnergyInspectionProvider().inspect(level,pos,false).orElseThrow();
        check(snap.load()==256&&snap.capacity()==50000,"disconnected storage excluded");
        mode(pos,Direction.EAST,ConnectionMode.AUTO);
        var b=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
        try{InspectionSnapshot.STREAM_CODEC.encode(b,snap);check(InspectionSnapshot.STREAM_CODEC.decode(b).equals(snap),"existing snapshot codec preserves aggregate and local values");}finally{b.release();}
    }
    @SubscribeEvent public static void before(ServerTickEvent.Pre event) {
        if(!active||reading)return;
        var wheel=SAW.west(2);
        for(var side:java.util.List.of(Direction.UP,Direction.DOWN,Direction.NORTH,Direction.SOUTH))put(wheel.relative(side),Blocks.STONE);
        level.setBlock(wheel.south(),Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL,8),2);
        MechanicalNetwork.invalidate(level);
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event)throws Exception{
        if(!active)return;
        try{
            ticks++;
            if(!reading){
                if(ticks==80){
                    check(saw().input().is(Items.OAK_LOG)&&saw().input().getCount()==2,"tube log delivery input="+saw().input()+" source="+chest(SAW.north(2)).getItem(1)+" packet="+((PneumaticTubeBlockEntity)level.getBlockEntity(SAW.north())).packet()+" accepts="+saw().canInsertAutomated(new ItemStack(Items.OAK_LOG)));
                    check(saw().progress()>0&&saw().processingState()==MechanicalSawmillBlockEntity.ProcessingState.CUTTING,"real cutting active progress="+saw().progress()+" state="+saw().processingState()+" net="+KineticNetworkManager.snapshot(level,SAW));
                    var update=saw().getUpdateTag(level.registryAccess());
                    var clientCopy=new MechanicalSawmillBlockEntity(SAW,level.getBlockState(SAW));
                    clientCopy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),update));
                    check(clientCopy.processingDuration()==100&&clientCopy.processingState()==MechanicalSawmillBlockEntity.ProcessingState.CUTTING,"BE update carries HUD duration and state");
                    check(clientCopy.input().getCount()==2,"HUD inventory uses synchronized real stacks");
                    check(ItemRouting.extract(level,SAW,64).isEmpty()&&saw().input().getCount()==2,"automation never extracts unprocessed logs");
                }
                if(ticks==420){
                    check(saw().input().isEmpty()&&saw().output().isEmpty(),"all logs processed and output collected");
                    check(chest(SAW.south(2)).getItem(0).is(Items.OAK_PLANKS)&&chest(SAW.south(2)).getItem(0).getCount()==12,"two logs yield exactly twelve delivered planks");
                    check(chest(SAW.north(2)).getItem(0).getCount()==8&&chest(SAW.north(2)).getItem(1).isEmpty(),"invalid material untouched, source logs consumed once");
                    check(((PneumaticTubeBlockEntity)level.getBlockEntity(SAW.north())).packet().isEmpty()&&((PneumaticTubeBlockEntity)level.getBlockEntity(SAW.south())).packet().isEmpty(),"line drains without stuck packets");
                    var packet=new ItemStack(Items.OAK_LOG);check(saw().insertAutomated(packet),"persistent automation binding survives empty batch");
                    Files.writeString(Path.of("../restart-ready.txt"),"ready");finish(null);
                }
            }else if(ticks==20){
                check(saw().automationBound(),"owner retained after resumed ticking");
                check(new EnergyInspectionProvider().inspect(level,CABLE,false).orElseThrow().load()==384,"stored reserve persists while transient cable flow resets");
                Files.writeString(Path.of("../complete.txt"),"PASS: sawmill transport/research and reserve survive dedicated restart\n");finish(null);
            }
        }catch(Throwable e){finish(e);}
    }
    private static void finish(Throwable error)throws Exception{active=false;Files.writeString(Path.of("../result.txt"),error==null?"PASS: "+checks+" sawmill/cable checks\n":"FAIL: "+error+"\n");if(error!=null)error.printStackTrace();level.getServer().halt(false);}
}
