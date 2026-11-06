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
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** 50 mB/t pump. FE-powered operation uses 2 FE/t; a crank provides manual pulses. */
public final class PeristalticPumpBlockEntity extends BlockEntity implements EnergyInspectable {
    private static final int CAPACITY=240,FE_PER_TICK=2,MAX_RATE=50;
    private final SimpleEnergyHandler battery=new SimpleEnergyHandler(CAPACITY,32,FE_PER_TICK);
    private long lastManual=-10;
    private int lastFlow;
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
    /** One transfer operation is atomic for both fluid endpoints. */
    public boolean pump(boolean powered){
        if(!(level instanceof ServerLevel server)||(powered&&energy()<FE_PER_TICK))return false;
        long time=server.getGameTime();
        if(!powered&&time-lastManual<10)return false;
        Direction output=getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        BlockPos inputPos=worldPosition.relative(output.getOpposite()),outputPos=worldPosition.relative(output);
        var source=FluidTransfer.handler(server,inputPos,output);
        var target=FluidTransfer.handler(server,outputPos,output.getOpposite());
        int moved=FluidTransfer.move(source,target,MAX_RATE);
        if(moved<=0){lastFlow=0;return false;}
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
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);battery.set(Math.clamp(in.getIntOr("stored_fe",0),0,CAPACITY));
        lastManual=in.getLongOr("last_manual",-10);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider reg){
        CompoundTag tag=saveCustomOnly(reg);tag.putInt("visual_flow",lastFlow);return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
