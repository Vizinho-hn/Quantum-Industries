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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 40 FE per discrete physical step, no AFK standing generation.
 * Tick and per-entity cooldowns cap passive mob harvesting rates.
 */
public final class PiezoSwitchBlockEntity extends BlockEntity implements EnergyInspectable {
    public static final int CAPACITY = 200, PULSE = 40, MAX_OUTPUT = 40;
    private final SimpleEnergyHandler battery = new SimpleEnergyHandler(CAPACITY, PULSE, MAX_OUTPUT);
    private final Map<UUID, Long> lastFootstep = new HashMap<>();
    private long lastPistonImpact = -20;

    public PiezoSwitchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.PIEZO_SWITCH_ENTITY.get(), pos, state);
    }
    @Override public int energy() { return (int) battery.getAmountAsLong(); }
    @Override public int capacity() { return CAPACITY; }
    @Override public Component inspectTitle() { return Component.translatable("block.quantumindustries.piezo_switch"); }
    public void impact(Entity entity) {
        if (!(level instanceof ServerLevel server)) return;
        // Stationary entities and mobs pushed by machinery cannot generate
        // unlimited pulses by just remaining on the plate.
        if (entity.getDeltaMovement().horizontalDistanceSqr() < 0.0025) return;
        long now = server.getGameTime();
        UUID id = entity.getUUID();
        if (now - lastFootstep.getOrDefault(id, -20L) < 20) return;
        lastFootstep.put(id, now);
        if (lastFootstep.size() > 128) lastFootstep.entrySet().removeIf(e -> now - e.getValue() > 100);
        generatePulse();
    }
    public void pistonImpact() {
        if (!(level instanceof ServerLevel server)) return;
        if (server.getGameTime() - lastPistonImpact < 20) return;
        lastPistonImpact = server.getGameTime();
        generatePulse();
    }
    private void generatePulse() {
        if (energy() > CAPACITY - PULSE) return;
        try (var tx = Transaction.openRoot()) {
            if (battery.insert(PULSE, tx) == PULSE) tx.commit();
        }
        changed();
    }
    public int extractEnergy(int max) {
        if (!(level instanceof ServerLevel) || max <= 0) return 0;
        int extracted;
        try (var tx = Transaction.openRoot()) {
            extracted = battery.extract(Math.min(MAX_OUTPUT,max), tx);
            if (extracted > 0) tx.commit();
        }
        if (extracted > 0) changed();
        return extracted;
    }
    public void tick() {
        if (!(level instanceof ServerLevel server) || energy() <= 0) return;
        CableNetwork.transfer(server, worldPosition, this::energy, this::extractEnergy, MAX_OUTPUT);
        for (Direction direction : Direction.values()) {
            if (server.getBlockEntity(worldPosition.relative(direction)) instanceof EnergyInspectable target
                    && !(target instanceof PiezoSwitchBlockEntity) && energy() > 0) {
                int amount = target.receiveEnergy(Math.min(energy(), MAX_OUTPUT));
                if (amount > 0) extractEnergy(amount);
            }
        }
    }
    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("stored_fe", energy());
        out.putLong("piston_pulse", lastPistonImpact);
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        battery.set(Math.clamp(in.getIntOr("stored_fe", 0), 0, CAPACITY));
        lastPistonImpact = in.getLongOr("piston_pulse", -20);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
