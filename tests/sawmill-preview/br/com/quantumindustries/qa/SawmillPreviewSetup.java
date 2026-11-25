package br.com.quantumindustries.qa;
import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.block.entity.*;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.logistics.*;
import br.com.quantumindustries.mechanical.MechanicalNetwork;
import br.com.quantumindustries.research.WorldResearchData;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
@EventBusSubscriber(modid="quantumindustries")
public final class SawmillPreviewSetup {
    public static final BlockPos SAW=new BlockPos(8,110,8),CABLE=new BlockPos(8,110,18),BUSBAR=new BlockPos(8,110,23);
    public static volatile int requested=1,applied;public static volatile boolean ready;public static volatile String failure;
    private static int ticks;
    private static void put(net.minecraft.server.level.ServerLevel l,BlockPos p,Block b){l.setBlock(p,b.defaultBlockState(),3);}
    private static void mode(net.minecraft.server.level.ServerLevel l,BlockPos p,Direction d,ConnectionMode m){var c=(ConfigurableConduit)l.getBlockEntity(p);c.connectionModes().set(d,m);c.connectionChanged();}
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){
        if(!Boolean.getBoolean("quantumindustries.previewSawmill")||!(event.getEntity() instanceof ServerPlayer player)||failure!=null)return;
        try{
            if(++ticks<60)return;
            var l=player.level();
            if(!ready){
                for(int x=3;x<=15;x++)for(int y=109;y<=114;y++)for(int z=3;z<=25;z++)put(l,new BlockPos(x,y,z),y==109?Blocks.STONE:Blocks.AIR);
                player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);player.getInventory().clearContent();
                player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ModItems.ENERGY_INSPECTION_GOGGLES.get()));WorldResearchData.get(l).unlockAll(player);
                camera(player,11.8,110,4.3,SAW);
                l.setBlock(SAW,ModBlocks.MECHANICAL_SAWMILL.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH),3);
                var saw=(MechanicalSawmillBlockEntity)l.getBlockEntity(SAW);saw.bindAutomation(player);
                put(l,SAW.north(2),Blocks.CHEST);put(l,SAW.south(2),Blocks.CHEST);
                ((ChestBlockEntity)l.getBlockEntity(SAW.north(2))).setItem(0,new ItemStack(Items.OAK_LOG,64));
                for(var p:java.util.List.of(SAW.north(),SAW.south())){
                    put(l,p,ModBlocks.PNEUMATIC_TUBE.get());for(var d:Direction.values())mode(l,p,d,ConnectionMode.NONE);
                    mode(l,p,Direction.NORTH,ConnectionMode.PULL);mode(l,p,Direction.SOUTH,ConnectionMode.PUSH);
                }
                l.setBlock(SAW.west(),ModBlocks.MECHANICAL_SHAFT_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.AXIS,Direction.Axis.X),3);
                l.setBlock(SAW.west(2),ModBlocks.PRIMITIVE_WATER_WHEEL.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.EAST),3);
                for(var p:java.util.List.of(CABLE,BUSBAR)){
                    put(l,p.west(),ModBlocks.LEAD_ACID_ACCUMULATOR.get());put(l,p.east(),ModBlocks.ELECTROMECHANICAL_PRESS.get());put(l,p,p.equals(CABLE)?ModBlocks.INSULATED_CONDUIT.get():ModBlocks.COPPER_BUSBAR.get());
                    for(var d:Direction.values())mode(l,p,d,ConnectionMode.NONE);mode(l,p,Direction.WEST,ConnectionMode.PULL);mode(l,p,Direction.EAST,ConnectionMode.PUSH);
                    var stack=new ItemStack(Items.COPPER_INGOT,64);var press=(ElectromechanicalPressBlockEntity)l.getBlockEntity(p.east());player.setPos(p.east().getX()+.5,p.getY(),p.getZ()+1.5);if(!press.bindAutomation(player))throw new AssertionError("press authorization");while(!stack.isEmpty()&&press.insertIngot(stack)){}
                }
                ready=true;
            }
            if(requested!=applied){
                if(requested<=2){camera(player,11.8,110,4.3,SAW);if(requested==2)mode(l,SAW.south(),Direction.NORTH,ConnectionMode.NONE);}
                else{var p=requested==3?CABLE:BUSBAR;camera(player,p.getX()+2.8,110,p.getZ()-3.8,p);}
                applied=requested;
            }
            var wheel=SAW.west(2);
            for(var d:java.util.List.of(Direction.UP,Direction.DOWN,Direction.NORTH,Direction.SOUTH))put(l,wheel.relative(d),Blocks.STONE);
            l.setBlock(wheel.south(),Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL,8),2);MechanicalNetwork.invalidate(l);
            // Real external supply fixture; delivery to consuming presses uses production CableNetwork logic.
            for(var p:java.util.List.of(CABLE,BUSBAR))((EnergyInspectable)l.getBlockEntity(p.west())).receiveEnergy(64);
        }catch(Throwable e){failure=e.toString();}
    }
    private static void camera(ServerPlayer p,double x,double y,double z,BlockPos target){
        double dx=target.getX()+.5-x,dz=target.getZ()+.5-z;
        p.connection.teleport(x,y,z,(float)Math.toDegrees(Math.atan2(-dx,dz)),(float)Math.toDegrees(Math.atan2(y+p.getEyeHeight()-target.getY()-.5,Math.hypot(dx,dz))));
    }
}
