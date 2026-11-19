package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.block.FluidPipeBlock;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.fluid.FluidInspectable;
import br.com.quantumindustries.fluid.FluidTransfer;
import br.com.quantumindustries.fluid.SidedPipeHandler;
import br.com.quantumindustries.logistics.ConfigurableConduit;
import br.com.quantumindustries.logistics.ConnectionModes;
import br.com.quantumindustries.logistics.ConnectionMode;
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
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

/** 250 mB segment; passive lines only flow level/down, pressure propagates uphill. */
public final class FluidPipeBlockEntity extends BlockEntity implements FluidInspectable, ConfigurableConduit {
    public static final int CAPACITY=250, MAX_RATE=50;
    private final FluidStacksResourceHandler tank=new FluidStacksResourceHandler(1,CAPACITY) {
        @Override protected void onContentsChanged(int slot,net.neoforged.neoforge.fluids.FluidStack stack) {
            FluidPipeBlockEntity.this.changed();
        }
    };
    private int pressureTicks, lastFlow;
    private long lastFlowTime=-1;
    private final ConnectionModes connections = new ConnectionModes();
    public FluidPipeBlockEntity(BlockPos pos,BlockState state){super(ModBlocks.FLUID_PIPE_ENTITY.get(),pos,state);}
    public FluidStacksResourceHandler tank(){return tank;}
    @Override public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) {
            BlockState state = getBlockState();
            BlockState connected = FluidPipeBlock.computeState(server, worldPosition, state);
            if (connected != state) server.setBlock(worldPosition, connected, Block.UPDATE_ALL);
        }
    }
    @Override public ConnectionModes connectionModes() { return connections; }
    @Override public void connectionChanged() {
        changed();
        if (level instanceof ServerLevel server) {
            BlockState state = FluidPipeBlock.computeState(server, worldPosition, getBlockState());
            server.setBlock(worldPosition, state, Block.UPDATE_ALL);
            server.updateNeighborsAt(worldPosition, state.getBlock());
        }
    }
    public net.neoforged.neoforge.transfer.ResourceHandler<FluidResource> sidedTank(Direction side) {
        return side == null ? tank : new SidedPipeHandler(tank, connections, side);
    }
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
        pullFromNeighbor(server);
        if(storedFluid()<=0){lastFlow=0;return;}
        for(Direction dir: new Direction[]{Direction.DOWN,Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST,Direction.UP}){
            if (!connectionMode(dir).allowsOutput()) continue;
            if(dir==Direction.UP&&pressureTicks<=0)continue;
            BlockPos next=worldPosition.relative(dir);
            if(!server.hasChunkAt(next))continue;
            var neighbor=server.getBlockEntity(next);
            if(neighbor instanceof FluidPipeBlockEntity other && connectionMode(dir) == ConnectionMode.AUTO
                    && storedFluid()<=other.storedFluid()+15)continue;
            var target=FluidTransfer.handler(server,next,dir.getOpposite());
            int moved=FluidTransfer.move(tank,target,MAX_RATE);
            if(moved<=0)continue;
            lastFlow=moved;lastFlowTime=server.getGameTime();
            if(neighbor instanceof FluidPipeBlockEntity pipe && pressureTicks>0)pipe.pressurize(20);
            if(getBlockState().is(ModBlocks.GLASS_PIPE.get())&&server.getGameTime()%6==0)
                server.sendParticles(tank.getResource(0).is(Fluids.LAVA)?ParticleTypes.LAVA:ParticleTypes.SPLASH,
                        worldPosition.getX()+0.5,worldPosition.getY()+0.5,
                        worldPosition.getZ()+0.5,1,0.03,0.03,0.03,0.0);
            changed();break;
        }
        if(server.getGameTime()-lastFlowTime>2&&lastFlow!=0){lastFlow=0;changed();}
    }
    private void pullFromNeighbor(ServerLevel server) {
        if (storedFluid() >= CAPACITY) return;
        for (Direction side : Direction.values()) {
            if (connectionMode(side) != ConnectionMode.PULL || side == Direction.DOWN && pressureTicks <= 0) continue;
            BlockPos sourcePos = worldPosition.relative(side);
            if (!server.hasChunkAt(sourcePos) || server.getBlockEntity(sourcePos) instanceof FluidPipeBlockEntity) continue;
            var source = FluidTransfer.handler(server, sourcePos, side.getOpposite());
            int moved = FluidTransfer.move(source, tank, MAX_RATE);
            if (moved <= 0) continue;
            lastFlow = moved;
            lastFlowTime = server.getGameTime();
            changed();
            return;
        }
    }
    private void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);}
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);tank.serialize(out);out.putInt("pressure_ticks",pressureTicks);
        connections.save(out);
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);tank.deserialize(in);
        connections.load(in);
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
