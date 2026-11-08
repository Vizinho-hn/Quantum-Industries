package br.com.quantumindustries.block.entity;

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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Lead-Acid Accumulator (Era II) - rechargeable FE storage (50,000 FE), connects to cable network. */
public final class LeadAcidAccumulatorBlockEntity extends BlockEntity implements EnergyInspectable {
    public static final int CAPACITY = 50000;
    public static final int TRANSFER_RATE = 256;

    private final SimpleEnergyHandler energyHandler = new SimpleEnergyHandler(CAPACITY, TRANSFER_RATE, TRANSFER_RATE);

    public LeadAcidAccumulatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.LEAD_ACID_ACCUMULATOR_ENTITY.get(), pos, state);
    }

    @Override public int energy() { return (int) energyHandler.getAmountAsLong(); }
    @Override public int capacity() { return CAPACITY; }
    @Override public Component inspectTitle() {
        return Component.translatable("block.quantumindustries.lead_acid_accumulator");
    }

    @Override public Component inspectDetail() {
        int stored = energy();
        int pct = (int) (stored * 100L / CAPACITY);
        return Component.translatable("block.quantumindustries.lead_acid_accumulator.status", stored, CAPACITY, pct);
    }

    @Override public int receiveEnergy(int maxReceive) {
        if (!(level instanceof ServerLevel) || maxReceive <= 0 || energy() >= CAPACITY) return 0;
        int inserted;
        try (var transaction = Transaction.openRoot()) {
            inserted = energyHandler.insert(Math.min(TRANSFER_RATE, maxReceive), transaction);
            if (inserted > 0) transaction.commit();
        }
        if (inserted > 0) changed();
        return inserted;
    }

    @Override public int extractEnergy(int maxExtract) {
        if (!(level instanceof ServerLevel) || maxExtract <= 0 || energy() <= 0) return 0;
        int extracted;
        try (var transaction = Transaction.openRoot()) {
            extracted = energyHandler.extract(Math.min(TRANSFER_RATE, maxExtract), transaction);
            if (extracted > 0) transaction.commit();
        }
        if (extracted > 0) changed();
        return extracted;
    }

    public void setStoredEnergy(int amount) {
        energyHandler.set(Math.clamp(amount, 0, CAPACITY));
        changed();
    }

    public int getStoredEnergy() { return energy(); }

    public Component getStatusMessage() {
        int stored = energy();
        int pct = (int) (stored * 100L / CAPACITY);
        return Component.translatable("block.quantumindustries.lead_acid_accumulator.chat_status", stored, CAPACITY, pct);
    }

    public void tick() {
        if (!(level instanceof ServerLevel server) || energy() <= 0) return;
        CableNetwork.transfer(server, worldPosition,
                this::energy, this::extractEnergy, TRANSFER_RATE);
        for (Direction dir : Direction.values()) {
            if (energy() <= 0) break;
            if (server.getBlockEntity(worldPosition.relative(dir)) instanceof EnergyInspectable target
                    && (target instanceof ElectromechanicalPressBlockEntity || target instanceof WireDrawingBenchBlockEntity || target instanceof LoadRelayBlockEntity
                            || target instanceof BellowsEjectorBlockEntity || target instanceof PeristalticPumpBlockEntity)) {
                int available = Math.min(TRANSFER_RATE, energy());
                int accepted = target.receiveEnergy(available);
                if (accepted > 0) extractEnergy(accepted);
            }
        }
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("stored_fe", energy());
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energyHandler.set(Math.clamp(input.getIntOr("stored_fe", 0), 0, CAPACITY));
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("stored_fe", energy());
    }
}
