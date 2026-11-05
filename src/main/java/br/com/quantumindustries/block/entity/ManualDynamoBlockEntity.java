package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.core.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class ManualDynamoBlockEntity extends BlockEntity {
    public static final int CAPACITY = 240;
    public static final int PER_TURN = 12;
    public static final int TURN_TICKS = 10;

    private final SimpleEnergyHandler battery = new SimpleEnergyHandler(CAPACITY, PER_TURN, 0);
    private boolean installed;
    private boolean refundable;
    private long lastTurn = -TURN_TICKS;

    public ManualDynamoBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MANUAL_DYNAMO_ENTITY.get(), pos, state);
    }

    public int energy() { return (int) battery.getAmountAsLong(); }
    public boolean installed() { return installed; }
    public long lastTurn() { return lastTurn; }

    public boolean install(boolean paid) {
        if (!(level instanceof ServerLevel) || installed) return false;
        installed = true;
        refundable = paid;
        changed();
        return true;
    }

    /** Clears the installed state before the caller returns or drops the item. */
    public boolean detach() {
        if (!(level instanceof ServerLevel) || !installed) return false;
        installed = false;
        boolean paid = refundable;
        refundable = false;
        changed();
        return paid;
    }

    public boolean turn() {
        if (!(level instanceof ServerLevel server) || !installed || energy() >= CAPACITY) return false;
        long now = server.getGameTime();
        if (now - lastTurn < TURN_TICKS) return false;
        try (var transaction = Transaction.openRoot()) {
            if (battery.insert(PER_TURN, transaction) == 0) return false;
            transaction.commit();
        }
        lastTurn = now;
        changed();
        return true;
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("stored_fe", energy());
        output.putBoolean("crank_installed", installed);
        output.putBoolean("crank_refundable", refundable);
        output.putLong("last_turn", lastTurn);
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        battery.set(Math.clamp(input.getIntOr("stored_fe", 0), 0, CAPACITY));
        installed = input.getBooleanOr("crank_installed", false);
        refundable = installed && input.getBooleanOr("crank_refundable", false);
        lastTurn = input.getLongOr("last_turn", -TURN_TICKS);
    }

    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server && installed && refundable) {
            installed = false;
            refundable = false;
            Block.popResource(server, pos, new net.minecraft.world.item.ItemStack(
                    br.com.quantumindustries.ModItems.HAND_CRANK.get()));
        }
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
