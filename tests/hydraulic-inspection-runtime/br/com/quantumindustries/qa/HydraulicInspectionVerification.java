package br.com.quantumindustries.qa;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.block.entity.*;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.inspection.*;
import br.com.quantumindustries.mechanical.*;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid="quantumindustries")
public final class HydraulicInspectionVerification {
    private static ServerLevel level;
    private static int checks;
    private static final double EPS=1e-7;
    private static void check(boolean ok,String why){ checks++; if(!ok)throw new AssertionError(why); }
    private static void near(double a,double b,String why){check(Double.isFinite(a)&&Math.abs(a-b)<EPS,why+": "+a+" vs "+b);}
    private static void put(BlockPos p,Block block){ level.setBlock(p,block.defaultBlockState(),2); }
    private static void water(BlockPos p){level.setBlock(p,Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL,8),2);}
    private static void invalidate(){MechanicalNetwork.invalidate(level);}
    private static RotationalBlockEntity wheel(BlockPos pos,int size,Direction.Axis axis){
        level.getChunk(pos.getX()>>4,pos.getZ()>>4);
        for(int x=-4;x<=4;x++)for(int y=-4;y<=4;y++)for(int z=-4;z<=4;z++)put(pos.offset(x,y,z),Blocks.AIR);
        Block block=size==0?ModBlocks.PRIMITIVE_WATER_WHEEL.get():size==1?ModBlocks.MEDIUM_WATER_WHEEL.get():ModBlocks.LARGE_WATER_WHEEL.get();
        level.setBlock(pos,block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,axis==Direction.Axis.X?Direction.EAST:Direction.SOUTH),2);
        invalidate();return (RotationalBlockEntity)level.getBlockEntity(pos);
    }
    private static HydraulicInspection capture(BlockPos pos){invalidate();return HydraulicInspection.capture(level,pos,7);}
    @SubscribeEvent public static void start(ServerStartedEvent event)throws Exception{
        if(!Boolean.getBoolean("quantumindustries.verifyHydraulicInspection"))return;
        level=event.getServer().overworld();
        try{
            for(var axis:List.of(Direction.Axis.X,Direction.Axis.Z))for(int size=0;size<=2;size++){
                BlockPos p=new BlockPos(8+size*16,110,8+(axis==Direction.Axis.X?0:32));
                var rotor=wheel(p,size,axis); int radius=Math.max(1,size);
                Direction positive=axis==Direction.Axis.X?Direction.SOUTH:Direction.WEST;
                water(p.relative(positive,radius));
                var one=capture(p);
                check(one.samples().size()==1,"A/G exact single sample count "+size+axis);
                var sample=one.samples().getFirst();
                check(p.offset(sample.rx(),sample.ry(),sample.rz()).equals(p.relative(positive,radius)),"A/G exact world sample position");
                check(one.resultant()>0,"A/G signed axial torque");
                near(one.resultant(),rotor.kineticDriveTorque(),"A/G local drive equals telemetry sum");
                if(size==0){
                    water(p.above());put(p.above().relative(positive.getOpposite()),Blocks.WATER);
                }else water(p.relative(positive,radius).above());
                var both=capture(p);
                check(both.samples().size()>=2&&both.resultant()>one.resultant(),"B simultaneous supporting flows");
                near(both.resultant(),rotor.kineticDriveTorque(),"B normalized/saturated sum");
                // Remove the second contact, then introduce an equal opposite one.
                if(size==0){put(p.above(),Blocks.AIR);put(p.above().relative(positive.getOpposite()),Blocks.AIR);}
                else put(p.relative(positive,radius).above(),Blocks.AIR);
                water(p.relative(positive.getOpposite(),radius));
                var opposed=capture(p);
                check(opposed.positive()>0&&opposed.negative()<0,"C opposing shares retained at equilibrium");
                near(opposed.resultant(),0,"C exact cancellation");near(opposed.resultant(),rotor.kineticDriveTorque(),"C sum equals local drive");
                check(opposed.relation(opposed.positive())==0,"stationary convention is neutral");
                var moving=new HydraulicInspection(opposed.dimension(),p,8,0,axis,1,0,opposed.samples());
                check(moving.relation(moving.positive())==1&&moving.relation(moving.negative())==-1,"direction classification uses omega");
                var reversed=new HydraulicInspection(opposed.dimension(),p,8,0,axis,-1,0,opposed.samples());
                check(reversed.relation(reversed.positive())==-1&&reversed.relation(reversed.negative())==1,"classification reverses with omega");
                put(p.relative(positive),Blocks.AIR);put(p.relative(positive,radius),Blocks.AIR);put(p.relative(positive.getOpposite(),radius),Blocks.AIR);
                check(capture(p).samples().isEmpty(),"E water removal clears samples");
            }
            dynamics(); codec(); authorization();
            check(br.com.quantumindustries.research.GuidebookCatalog.chapters().stream().anyMatch(c->c.id().equals("water_moves_wheels")&&c.studyKey().equals("hydraulic_power")),"optional chapter uses existing research");
            Files.writeString(Path.of("../result.txt"),"PASS: "+checks+" hydraulic inspection checks A-I; dedicated server, real fluids, codec, two-player authorization and dynamics\n");
        }catch(Throwable error){Files.writeString(Path.of("../result.txt"),"FAIL: "+error+"\n");error.printStackTrace();}
        event.getServer().halt(false);
    }
    private static void dynamics(){
        var initial=new KineticDynamics.State(0,2);
        var slow=KineticDynamics.advanceDetailed(initial,-18,0,4.5,.65,20,.05);
        var fly=KineticDynamics.advanceDetailed(initial,-18,0,24.5,.65,20,.05);
        check(slow.state().angularVelocityRadiansPerSecond()<2&&Math.abs(fly.angularAcceleration())<Math.abs(slow.angularAcceleration()),"D/F opposite torque brakes; inertia reduces acceleration");
        var state=initial;boolean crossed=false;
        for(int i=0;i<100;i++){var next=KineticDynamics.advance(state,-18,0,24.5,.65,20,.05);if(state.angularVelocityRadiansPerSecond()>=0&&next.angularVelocityRadiansPerSecond()<0)crossed=true;state=next;}
        check(crossed&&state.angularVelocityRadiansPerSecond()<0,"D reversal crosses zero with inertia");
        var coast=KineticDynamics.advance(initial,0,0,24.5,.65,20,.05);
        check(coast.angularVelocityRadiansPerSecond()>0&&coast.angularVelocityRadiansPerSecond()<2,"E/F wheel coasts without hydraulic drive");
        BlockPos p=new BlockPos(8,110,80);var rotor=wheel(p,0,Direction.Axis.X);water(p.south());
        double before=capture(p).resultant();
        level.setBlock(p.east(),ModBlocks.IRON_FLYWHEEL.get().defaultBlockState().setValue(BlockStateProperties.AXIS,Direction.Axis.X),2);
        near(capture(p).resultant(),before,"F attached flywheel does not change local water torque");
    }
    private static void codec(){
        var p=new BlockPos(8,110,80);var original=capture(p);
        var b=new RegistryFriendlyByteBuf(Unpooled.buffer(),level.registryAccess());
        try{HydraulicInspectionPayload.STREAM_CODEC.encode(b,new HydraulicInspectionPayload(original));
            check(b.readableBytes()<700,"bounded detail response under 700 bytes");
            var decoded=HydraulicInspectionPayload.STREAM_CODEC.decode(b).data();check(decoded.equals(original),"real registry-aware codec round trip");
        }finally{b.release();}
        try{new HydraulicInspection(original.dimension(),p,1,0,Direction.Axis.X,Double.NaN,0,List.of());check(false,"reject NaN");}catch(IllegalArgumentException expected){checks++;}
        try{new HydraulicInspection(original.dimension(),p,1,0,Direction.Axis.X,0,0,Collections.nCopies(17,new HydraulicTorque.Contribution(0,1,0,0,0,1,1)));check(false,"reject sample overflow");}catch(IllegalArgumentException expected){checks++;}
        var invalid=new RegistryFriendlyByteBuf(Unpooled.buffer(),level.registryAccess());
        try{invalid.writeIdentifier(original.dimension());invalid.writeBlockPos(p);invalid.writeVarLong(1);invalid.writeVarLong(0);invalid.writeByte(0);invalid.writeDouble(0);invalid.writeDouble(0);invalid.writeVarInt(1000000);
            try{HydraulicInspectionPayload.STREAM_CODEC.decode(invalid);check(false,"reject malformed wire count before allocation");}catch(IllegalArgumentException expected){checks++;}
        }finally{invalid.release();}
    }
    private static FakePlayer inspector(BlockPos target,String name){
        var p=new FakePlayer(level,new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)),name));
        p.setPos(target.getX()+3.5,target.getY()-1,target.getZ()+.5);
        double dx=target.getX()+.5-p.getX(),dz=target.getZ()+.5-p.getZ(),dy=target.getY()+.5-p.getEyeY();
        p.setYRot((float)Math.toDegrees(Math.atan2(-dx,dz)));p.setXRot((float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz))));
        p.setYHeadRot(p.getYRot());p.setShiftKeyDown(true);p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ModItems.ENERGY_INSPECTION_GOGGLES.get()));return p;
    }
    private static HydraulicInspection ask(FakePlayer player,BlockPos target,long id){player.tickCount+=5;return HydraulicInspectionService.request(player,new RequestHydraulicPayload(level.dimension().identifier(),target,id));}
    private static void authorization(){
        BlockPos a=new BlockPos(8,110,112),b=new BlockPos(8,110,144);wheel(a,0,Direction.Axis.X);water(a.south());wheel(b,0,Direction.Axis.Z);water(b.west());
        var pa=inspector(a,"HydraulicQA_A");var pb=inspector(b,"HydraulicQA_B");
        var first=ask(pa,a,1);var second=ask(pb,b,2);
        check(first!=null&&second!=null&&first.target().equals(a)&&second.target().equals(b),"H independent targets first="+first+" second="+second+" hitA="+((net.minecraft.world.phys.BlockHitResult)pa.pick(10,1,false)).getBlockPos()+" type="+pa.pick(10,1,false).getType()+" state="+level.getBlockState(a)+" look="+pa.getLookAngle()+" eye="+pa.getEyePosition()+" shift="+pa.isShiftKeyDown()+" goggles="+pa.getItemBySlot(EquipmentSlot.HEAD));
        check(HydraulicInspectionService.request(pa,new RequestHydraulicPayload(level.dimension().identifier(),a,2))==null,"server rejects excessive requests");
        check(ask(pa,b,3)==null,"H cannot inspect remote other network");
        pa.setShiftKeyDown(false);check(ask(pa,a,4)==null,"Shift release invalidates requests");pa.setShiftKeyDown(true);
        pa.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);check(ask(pa,a,5)==null,"removed goggles invalidate requests");pa.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ModItems.ENERGY_INSPECTION_GOGGLES.get()));
        pa.tickCount+=5;check(HydraulicInspectionService.request(pa,new RequestHydraulicPayload(Identifier.fromNamespaceAndPath("minecraft","the_nether"),a,6))==null,"dimension mismatch rejected");
        check(HydraulicInspection.capture(level,new BlockPos(1000000,110,1000000),7)==null,"unloaded chunks not inspected or loaded");
        put(a,Blocks.AIR);check(ask(pa,a,8)==null,"destroyed wheel invalidates request");
    }
}
