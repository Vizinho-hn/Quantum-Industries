package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.block.LoadRelayBlock;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.energy.CableNetwork;
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

/** FE pass-through with 10% low-battery cutoff and 80% redstone alarm. */
public final class LoadRelayBlockEntity extends BlockEntity implements EnergyInspectable {
    private static final int CAPACITY = 96, TRANSFER = 48;
    private final SimpleEnergyHandler battery = new SimpleEnergyHandler(CAPACITY, TRANSFER, TRANSFER);
    private boolean enabled = true;
    public LoadRelayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.LOAD_RELAY_ENTITY.get(), pos, state);
    }
    @Override public int energy() { return (int) battery.getAmountAsLong(); }
    @Override public int capacity() { return CAPACITY; }
    @Override public Component inspectTitle() { return Component.translatable("block.quantumindustries.load_relay"); }
    @Override public Component inspectDetail() {
        return Component.translatable("message.quantumindustries.relay.status", enabled ? "ON" : "OFF", energy());
    }
    @Override public int receiveEnergy(int offered) {
        if (!(level instanceof ServerLevel) || offered <= 0) return 0;
        int received;
        try (var tx=Transaction.openRoot()) {
            received=battery.insert(Math.min(offered,TRANSFER),tx);
            if (received>0) tx.commit();
        }
        if (received>0) changed();
        return received;
    }
    public int extractEnergy(int amount) {
        if (!(level instanceof ServerLevel) || amount <= 0) return 0;
        int extracted;
        try (var tx=Transaction.openRoot()) {
            extracted=battery.extract(Math.min(TRANSFER,amount),tx);
            if (extracted>0) tx.commit();
        }
        if (extracted>0) changed();
        return extracted;
    }
    public void tick() {
        if (!(level instanceof ServerLevel server)) return;
        boolean foundBattery = false, allowed = true, alarm = false;
        for (Direction direction : Direction.values()) {
            if (!(server.getBlockEntity(worldPosition.relative(direction)) instanceof VoltaicPileBlockEntity accumulator)) continue;
            foundBattery = true;
            int percent = accumulator.energy()*100 / Math.max(1,accumulator.capacity());
            if (percent < 10) allowed = false;
            if (percent >= 80) alarm = true;
        }
        enabled = !foundBattery || allowed;
        BlockState state=getBlockState();
        if (state.getValue(BlockStateProperties.POWERED) != alarm) {
            server.setBlock(worldPosition,state.setValue(BlockStateProperties.POWERED,alarm),3);
            server.updateNeighborsAt(worldPosition, ModBlocks.LOAD_RELAY.get());
        }
        if (!enabled || energy() <= 0) return;
        CableNetwork.transfer(server, worldPosition, this::energy, this::extractEnergy, TRANSFER,
                target -> target instanceof ElectromechanicalPressBlockEntity
                        || target instanceof WireDrawingBenchBlockEntity);
        for (Direction direction : Direction.values()) {
            if (energy() <= 0) break;
            if (server.getBlockEntity(worldPosition.relative(direction)) instanceof EnergyInspectable target
                    && (target instanceof ElectromechanicalPressBlockEntity
                        || target instanceof WireDrawingBenchBlockEntity)) {
                int accepted = target.receiveEnergy(Math.min(TRANSFER, energy()));
                if (accepted > 0) extractEnergy(accepted);
            }
        }
    }
    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);
    }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("stored_fe",energy());
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        battery.set(Math.clamp(in.getIntOr("stored_fe",0),0,CAPACITY));
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
