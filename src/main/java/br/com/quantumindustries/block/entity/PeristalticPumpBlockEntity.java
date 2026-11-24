package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.fluid.FluidTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Rear inlet with bottom suction fallback; one transfer of up to 50 mB per operation. */
public final class PeristalticPumpBlockEntity extends BlockEntity implements EnergyInspectable {
    private static final int CAPACITY=240,FE_PER_TICK=2,MAX_RATE=50;
    private final SimpleEnergyHandler battery=new SimpleEnergyHandler(CAPACITY,32,FE_PER_TICK);
    private static final int INLET_CAPACITY = 250;
    private final FluidStacksResourceHandler inletBuffer = new FluidStacksResourceHandler(1, INLET_CAPACITY) {
        @Override
        protected void onContentsChanged(int slot, net.neoforged.neoforge.fluids.FluidStack stack) {
            changed();
        }
    };
    private long lastManual=-10;
    private int lastFlow;
    private final br.com.quantumindustries.energy.MachineEnergyPort energyPort =
        new br.com.quantumindustries.energy.MachineEnergyPort(this, battery, () -> true, () -> false, null, this::changed);

    public net.neoforged.neoforge.transfer.energy.EnergyHandler energyPort() { return energyPort; }

    public PeristalticPumpBlockEntity(BlockPos pos,BlockState state){super(ModBlocks.PERISTALTIC_PUMP_ENTITY.get(),pos,state);}
    @Override public int energy(){return (int)battery.getAmountAsLong();}
    @Override public int capacity(){return CAPACITY;}
    @Override public Component inspectTitle(){return Component.translatable("block.quantumindustries.peristaltic_pump");}
    @Override public Component inspectDetail(){return Component.translatable("message.quantumindustries.pump.status",lastFlow,energy());}
    @Override public int receiveEnergy(int offered){
        if(!(level instanceof ServerLevel)||offered<=0)return 0;
        int got;
        try(var tx=Transaction.openRoot()){got=battery.insert(Math.min(32,offered),tx);if(got>0)tx.commit();}
        if(got>0)changed();return got;
    }
    private boolean isInlet(Direction side) {
        return side == Direction.DOWN || side == getBlockState()
                .getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
    }

    public ResourceHandler<FluidResource> fluidInlet(Direction side) {
        if (!isInlet(side)) return null;
        return new ResourceHandler<>() {
            @Override public int size() { return inletBuffer.size(); }
            @Override public FluidResource getResource(int slot) { return inletBuffer.getResource(slot); }
            @Override public long getAmountAsLong(int slot) { return inletBuffer.getAmountAsLong(slot); }
            @Override public long getCapacityAsLong(int slot, FluidResource fluid) {
                return inletBuffer.getCapacityAsLong(slot, fluid);
            }
            @Override public boolean isValid(int slot, FluidResource fluid) {
                return isInlet(side) && inletBuffer.isValid(slot, fluid);
            }
            @Override public int insert(int slot, FluidResource fluid, int amount, TransactionContext transaction) {
                return level instanceof ServerLevel && !isRemoved() && isValid(slot, fluid)
                        ? inletBuffer.insert(slot, fluid, amount, transaction) : 0;
            }
            @Override public int extract(int slot, FluidResource fluid, int amount, TransactionContext transaction) {
                return 0;
            }
        };
    }

    /** One transfer operation is atomic for both fluid endpoints. */
    public boolean pump(boolean powered){
        if(!(level instanceof ServerLevel server)||(powered&&energy()<FE_PER_TICK))return false;
        long time=server.getGameTime();
        if(!powered&&time-lastManual<10)return false;
        Direction output=getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        BlockPos outputPos=worldPosition.relative(output);
        var target=FluidTransfer.handler(server,outputPos,output.getOpposite());
        int moved=0;
        // Keep existing installations deterministic: rear input has priority.
        // Bottom suction is a fallback, not a second transfer or a second charge.
        if(target!=null){
            moved = FluidTransfer.move(inletBuffer, target, MAX_RATE);
            for(Direction inlet:new Direction[]{output.getOpposite(),Direction.DOWN}){
                if (moved > 0) break;
                BlockPos inputPos=worldPosition.relative(inlet);
                var source=FluidTransfer.handler(server,inputPos,inlet.getOpposite());
                moved=source==null?FluidTransfer.drawRenewableWater(server,inputPos,target,MAX_RATE)
                        :FluidTransfer.move(source,target,MAX_RATE);
            }
        }
        if(moved<=0){if(lastFlow!=0){lastFlow=0;changed();}return false;}
        if(powered){
            try(var tx=Transaction.openRoot()){battery.extract(FE_PER_TICK,tx);tx.commit();}
        }else lastManual=time;
        if(server.getBlockEntity(outputPos) instanceof FluidPipeBlockEntity pipe)pipe.pressurize(20);
        lastFlow=moved;changed();return true;
    }
    public void tick(){if(level instanceof ServerLevel && energy()>=FE_PER_TICK)pump(true);else if(lastFlow!=0){lastFlow=0;changed();}}
    private void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);}
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);out.putInt("stored_fe",energy());out.putLong("last_manual",lastManual);
        inletBuffer.serialize(out.child("pump_inlet"));
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);battery.set(Math.clamp(in.getIntOr("stored_fe",0),0,CAPACITY));
        lastManual=in.getLongOr("last_manual",-10);
        in.child("pump_inlet").ifPresent(inletBuffer::deserialize);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider reg){
        CompoundTag tag=saveCustomOnly(reg);tag.putInt("visual_flow",lastFlow);return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
