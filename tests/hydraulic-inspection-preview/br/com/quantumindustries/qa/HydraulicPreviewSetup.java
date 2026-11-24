package br.com.quantumindustries.qa;
import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.mechanical.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
@EventBusSubscriber(modid="quantumindustries")
public final class HydraulicPreviewSetup {
    public static final BlockPos CENTER=new BlockPos(8,110,8);
    public static volatile int requested=1,applied,age;
    public static volatile boolean ready, destroyRequested, dimensionRequested;
    public static volatile String failure;
    private static int ticks;
    private static void put(net.minecraft.server.level.ServerLevel l,BlockPos p,Block b){l.setBlock(p,b.defaultBlockState(),2);}
    private static void water(net.minecraft.server.level.ServerLevel l,BlockPos p){l.setBlock(p,Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL,8),2);}
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){
        if(!Boolean.getBoolean("quantumindustries.previewHydraulicInspection")||!(event.getEntity() instanceof ServerPlayer player)||failure!=null)return;
        try{
            if(++ticks<60)return;
            var l=player.level();
            if(dimensionRequested){
                if(l.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) player.teleport(
                    new net.minecraft.world.level.portal.TeleportTransition(l.getServer().getLevel(net.minecraft.world.level.Level.NETHER),
                        new net.minecraft.world.phys.Vec3(0,120,0),net.minecraft.world.phys.Vec3.ZERO,0,0,
                        net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
                return;
            }
            if(destroyRequested){put(l,CENTER,Blocks.AIR);MechanicalNetwork.invalidate(l);return;}
            if(requested!=applied){
                if(requested==1||requested==5||requested==6){
                    for(int x=-5;x<=6;x++)for(int y=-5;y<=4;y++)for(int z=-5;z<=5;z++)put(l,CENTER.offset(x,y,z),y==-5?Blocks.STONE:Blocks.AIR);
                    var block=requested==5?ModBlocks.LARGE_WATER_WHEEL.get():ModBlocks.PRIMITIVE_WATER_WHEEL.get();
                    l.setBlock(CENTER,block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.EAST),2);
                    if(requested==6){
                        l.setBlock(CENTER.east(),ModBlocks.MECHANICAL_SHAFT_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.AXIS,Direction.Axis.X),2);
                        l.setBlock(CENTER.east(2),ModBlocks.IRON_FLYWHEEL.get().defaultBlockState().setValue(BlockStateProperties.AXIS,Direction.Axis.X),2);
                    }
                    player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);player.setNoGravity(true);
                    player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ModItems.ENERGY_INSPECTION_GOGGLES.get()));
                    double x=requested==6?5:13.3,y=109,z=requested==6?11.7:9.7,tx=8.5,ty=110.5,tz=8.5;
                    float yaw=(float)Math.toDegrees(Math.atan2(-(tx-x),tz-z));
                    float pitch=(float)Math.toDegrees(Math.atan2(y+player.getEyeHeight()-ty,Math.hypot(tx-x,tz-z)));
                    put(l, new BlockPos(requested==6?5:13,108,requested==6?11:9), Blocks.STONE);
                    player.connection.teleport(x,y,z,yaw,pitch);
                }
                applied=requested;age=0;ready=true;
            }
            age++;
            var wanted = new java.util.HashSet<BlockPos>();
            if(applied==5){for(int y=-2;y<=2;y++)wanted.add(CENTER.south(2).above(y));wanted.add(CENTER.north(2));}
            else if(applied!=6||age<85){
                wanted.add(CENTER.south());
                if(applied==2||applied==3){wanted.add(CENTER.above());wanted.add(CENTER.above().north());}
                if(applied==3||applied==4)wanted.add(CENTER.north());
            }
            for(int x=-4;x<=4;x++)for(int y=-4;y<=3;y++)for(int z=-4;z<=4;z++){
                var p=CENTER.offset(x,y,z);
                if(!wanted.contains(p)&&l.getBlockState(p).is(Blocks.WATER))put(l,p,Blocks.AIR);
            }
            // Controlled real fluid block states isolate sampling from channel erosion.
            if(applied==5){for(int y=-2;y<=2;y++)water(l,CENTER.south(2).above(y));water(l,CENTER.north(2));}
            else if(applied!=6||age<85){
                water(l,CENTER.south());
                if(applied==2||applied==3){water(l,CENTER.above());put(l,CENTER.above().north(),Blocks.WATER);}
                if(applied==3||applied==4)water(l,CENTER.north());
                if(applied==4){put(l,CENTER.above(),Blocks.AIR);put(l,CENTER.above().north(),Blocks.AIR);}
            }else{
                for(int x=-1;x<=1;x++)for(int y=-2;y<=2;y++)for(int z=-2;z<=2;z++)if(!CENTER.offset(x,y,z).equals(CENTER)&&l.getBlockState(CENTER.offset(x,y,z)).is(Blocks.WATER))put(l,CENTER.offset(x,y,z),Blocks.AIR);
            }
            MechanicalNetwork.invalidate(l);
        }catch(Throwable e){failure=e.toString();}
    }
}
