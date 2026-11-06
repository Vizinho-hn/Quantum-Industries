package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.core.ModBlocks;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Era II ionic battery tank (2,400 FE) using copper plates and lapis lazuli electrolyte suspension. */
public final class GalvanicAccumulatorBlockEntity extends BlockEntity implements EnergyInspectable {
    public static final int CAPACITY = 2400;
    public static final int MAX_TRANSFER = 48;

    private final SimpleEnergyHandler battery = new SimpleEnergyHandler(CAPACITY, MAX_TRANSFER, MAX_TRANSFER);

    public GalvanicAccumulatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.GALVANIC_ACCUMULATOR_ENTITY.get(), pos, state);
    }

    @Override public int energy() { return (int) battery.getAmountAsLong(); }
    @Override public int capacity() { return CAPACITY; }
    @Override public Component inspectTitle() {
        return Component.translatable("block.quantumindustries.galvanic_accumulator");
    }

    @Override public int receiveEnergy(int maxReceive) {
        if (!(level instanceof ServerLevel) || maxReceive <= 0 || energy() >= CAPACITY) return 0;
        int inserted;
        try (var transaction = Transaction.openRoot()) {
            inserted = battery.insert(Math.min(MAX_TRANSFER, maxReceive), transaction);
            if (inserted > 0) transaction.commit();
        }
        if (inserted > 0) changed();
        return inserted;
    }

    public int extractEnergy(int maxExtract) {
        if (!(level instanceof ServerLevel) || maxExtract <= 0 || energy() <= 0) return 0;
        int extracted;
        try (var transaction = Transaction.openRoot()) {
            extracted = battery.extract(Math.min(MAX_TRANSFER, maxExtract), transaction);
            if (extracted > 0) transaction.commit();
        }
        if (extracted > 0) changed();
        return extracted;
    }

    public void setStoredEnergy(int amount) {
        battery.set(Math.clamp(amount, 0, CAPACITY));
        changed();
    }

    public void tick() {
        if (!(level instanceof ServerLevel server) || energy() <= 0) return;
        for (Direction dir : Direction.values()) {
            if (energy() <= 0) break;
            if (server.getBlockEntity(worldPosition.relative(dir)) instanceof ElectromechanicalPressBlockEntity press) {
                int available = Math.min(MAX_TRANSFER, energy());
                int accepted = press.receiveEnergy(available);
                if (accepted > 0) extractEnergy(accepted);
            }
        }
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("stored_fe", energy());
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        battery.set(Math.clamp(input.getIntOr("stored_fe", 0), 0, CAPACITY));
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
