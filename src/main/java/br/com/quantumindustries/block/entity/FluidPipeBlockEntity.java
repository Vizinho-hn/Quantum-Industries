package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.fluid.FluidInspectable;
import br.com.quantumindustries.fluid.FluidTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

/** 250 mB segment; passive lines only flow level/down, pressure propagates uphill. */
public final class FluidPipeBlockEntity extends BlockEntity implements FluidInspectable {
    public static final int CAPACITY=250, MAX_RATE=50;
    private final FluidStacksResourceHandler tank=new FluidStacksResourceHandler(1,CAPACITY) {
        @Override protected void onContentsChanged(int slot,net.neoforged.neoforge.fluids.FluidStack stack) {
            FluidPipeBlockEntity.this.changed();
        }
    };
    private int pressureTicks, lastFlow;
    private long lastFlowTime=-1;
    public FluidPipeBlockEntity(BlockPos pos,BlockState state){super(ModBlocks.FLUID_PIPE_ENTITY.get(),pos,state);}
    public FluidStacksResourceHandler tank(){return tank;}
    public void pressurize(int ticks){pressureTicks=Math.max(pressureTicks,ticks);changed();}
    @Override public int storedFluid(){return tank.getAmountAsInt(0);}
    @Override public int fluidCapacity(){return CAPACITY;}
    @Override public Component fluidTitle(){
        return Component.translatable(getBlockState().is(ModBlocks.GLASS_PIPE.get())
                ?"block.quantumindustries.glass_pipe":"block.quantumindustries.fluid_pipe");
    }
    @Override public Component fluidDetail(){return Component.translatable("message.quantumindustries.fluid.line",lastFlow,pressureTicks);}
    public void tick(){
        if(!(level instanceof ServerLevel server))return;
        if(pressureTicks>0)pressureTicks--;
        if(storedFluid()<=0){lastFlow=0;return;}
        for(Direction dir: new Direction[]{Direction.DOWN,Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST,Direction.UP}){
            if(dir==Direction.UP&&pressureTicks<=0)continue;
            BlockPos next=worldPosition.relative(dir);
            if(!server.hasChunkAt(next))continue;
            var neighbor=server.getBlockEntity(next);
            if(neighbor instanceof FluidPipeBlockEntity other && storedFluid()<=other.storedFluid()+15)continue;
            var target=FluidTransfer.handler(server,next,dir.getOpposite());
            int moved=FluidTransfer.move(tank,target,MAX_RATE);
            if(moved<=0)continue;
            lastFlow=moved;lastFlowTime=server.getGameTime();
            if(neighbor instanceof FluidPipeBlockEntity pipe && pressureTicks>0)pipe.pressurize(20);
            if(getBlockState().is(ModBlocks.GLASS_PIPE.get())&&server.getGameTime()%6==0)
                server.sendParticles(ParticleTypes.SPLASH,worldPosition.getX()+0.5,worldPosition.getY()+0.5,
                        worldPosition.getZ()+0.5,1,0.03,0.03,0.03,0.0);
            changed();break;
        }
        if(server.getGameTime()-lastFlowTime>2&&lastFlow!=0){lastFlow=0;changed();}
    }
    private void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);}
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);tank.serialize(out);out.putInt("pressure_ticks",pressureTicks);
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);tank.deserialize(in);
        pressureTicks=Math.clamp(in.getIntOr("pressure_ticks",0),0,20);
        lastFlow=Math.clamp(in.getIntOr("visual_flow",0),0,MAX_RATE);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider regs){
        CompoundTag tag=saveCustomOnly(regs);
        tag.putInt("visual_flow",lastFlow);tag.putInt("visual_pressure",pressureTicks);
        return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
