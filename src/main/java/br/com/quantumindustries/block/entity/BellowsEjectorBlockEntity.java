package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.logistics.ItemRouting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class BellowsEjectorBlockEntity extends BlockEntity implements EnergyInspectable {
    public static final int CAPACITY = 100, PULSE_FE = 10;
    private final SimpleEnergyHandler battery = new SimpleEnergyHandler(CAPACITY, 32, PULSE_FE);
    private long lastPulse = -10;
    public BellowsEjectorBlockEntity(BlockPos pos, BlockState state) { super(ModBlocks.BELLOWS_EJECTOR_ENTITY.get(),pos,state); }
    @Override public int energy() { return (int)battery.getAmountAsLong(); }
    @Override public int capacity() { return CAPACITY; }
    @Override public Component inspectTitle() { return Component.translatable("block.quantumindustries.bellows_ejector"); }
    @Override public Component inspectDetail() {
        return Component.translatable("message.quantumindustries.pneumatic.status",energy(),CAPACITY);
    }
    @Override public int receiveEnergy(int max) {
        if (!(level instanceof ServerLevel)) return 0;
        int got;
        try (var tx=Transaction.openRoot()) {
            got=battery.insert(Math.min(32,Math.max(0,max)),tx);if(got>0)tx.commit();
        }
        if(got>0)changed();
        return got;
    }
    public boolean pulse(int count, boolean powered) {
        if (!(level instanceof ServerLevel server) || server.getGameTime()-lastPulse<10
                || (powered && energy()<PULSE_FE)) return false;
        Direction output=getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        BlockPos from=worldPosition.relative(output.getOpposite()), to=worldPosition.relative(output);
        if (!(server.getBlockEntity(to) instanceof PneumaticTubeBlockEntity tube)
                || !tube.packet().isEmpty()) return false;
        ItemStack peek=ItemRouting.preview(server,from,count);
        if(peek.isEmpty()) return false;
        ItemStack extracted=ItemRouting.extract(server,from,peek.getCount());
        if(extracted.isEmpty()) return false;
        ItemStack packet=extracted.copy();
        if(!tube.accept(packet,output.getOpposite())) {
            // Server operations are synchronous; failed route returns every extracted item.
            ItemRouting.insert(server,from,extracted,output);
            return false;
        }
        if(powered) {
            try(var tx=Transaction.openRoot()){battery.extract(PULSE_FE,tx);tx.commit();}
        }
        lastPulse=server.getGameTime();
        changed();
        return true;
    }
    public void tick() {
        if(level instanceof ServerLevel && energy()>=PULSE_FE) pulse(1,true);
    }
    private void changed(){
        setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);
    }
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);out.putInt("stored_fe",energy());out.putLong("last_pulse",lastPulse);
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);battery.set(Math.clamp(in.getIntOr("stored_fe",0),0,CAPACITY));
        lastPulse=in.getLongOr("last_pulse",-10);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registry){return saveCustomOnly(registry);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
