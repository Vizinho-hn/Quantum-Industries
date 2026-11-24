package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.energy.CableNetwork;
import br.com.quantumindustries.energy.TransferBudget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
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
    private static final double STEP_DISTANCE = .20;
    private static final long COOLDOWN = 20;
    private final TransferBudget transferBudget = new TransferBudget(MAX_OUTPUT);
    private final Map<UUID, Contact> contacts = new HashMap<>();
    private record Contact(double x, double z, long seen, long pulse, double distance) {}
    private long lastPistonImpact = -20;

    public PiezoSwitchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.PIEZO_SWITCH_ENTITY.get(), pos, state);
    }
    @Override public int energy() { return (int) battery.getAmountAsLong(); }
    @Override public int capacity() { return CAPACITY; }
    @Override public Component inspectTitle() { return Component.translatable("block.quantumindustries.piezo_switch"); }
    private AABB contactArea() {
        return new AABB(worldPosition.getX()+.0625, worldPosition.getY()+.10, worldPosition.getZ()+.0625,
                worldPosition.getX()+.9375, worldPosition.getY()+.23, worldPosition.getZ()+.9375);
    }

    public void impact(Entity entity) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof LivingEntity living)
                || !living.isAlive() || living.isSpectator()) return;
        // The server's player velocity may be zero even while movement packets
        // change position. Use actual foot contact and sampled displacement.
        double feet=entity.getBoundingBox().minY;
        if(Math.abs(feet-(worldPosition.getY()+.125))>.08 || !entity.getBoundingBox().intersects(contactArea()))return;
        long now=server.getGameTime();
        UUID id=entity.getUUID();
        Contact previous=contacts.get(id);
        boolean entering=previous==null||now-previous.seen()>1;
        long lastPulse=previous==null?Long.MIN_VALUE/2:previous.pulse();
        double moved=entering?0:Math.hypot(entity.getX()-previous.x(),entity.getZ()-previous.z());
        double distance=entering?0:previous.distance()+moved;
        // A new load gives one pulse. Continuous contact needs actual movement;
        // a stationary entity must not repeat the pulse after the cooldown.
        if(now-lastPulse>=COOLDOWN&&(entering||moved>.001&&distance>=STEP_DISTANCE)){
            generatePulse();
            lastPulse=now;
            distance=0;
        }
        contacts.put(id,new Contact(entity.getX(),entity.getZ(),now,lastPulse,Math.min(distance,STEP_DISTANCE)));
    }

    private void sampleContacts(ServerLevel server){
        // Thin plates can be missed by vanilla stepOn (supporting-block selection).
        // Scan the small contact volume as a fallback, even with an empty buffer.
        for(LivingEntity entity:server.getEntitiesOfClass(LivingEntity.class,contactArea())){
            if(entity.onGround())impact(entity);
        }
        if(!contacts.isEmpty())contacts.entrySet().removeIf(entry->server.getGameTime()-entry.getValue().seen()>100);
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
        if (!(level instanceof ServerLevel server) || max <= 0) return 0;
        int allowance=transferBudget.extractRemaining(server.getGameTime());
        if(allowance<=0)return 0;
        int extracted;
        try (var tx = Transaction.openRoot()) {
            extracted = battery.extract(Math.min(allowance,max), tx);
            if (extracted > 0) tx.commit();
        }
        if (extracted > 0) {transferBudget.recordExtract(server.getGameTime(),extracted);changed();}
        return extracted;
    }
    public void tick() {
        if (!(level instanceof ServerLevel server)) return;
        sampleContacts(server);
        if(energy()<=0)return;
        CableNetwork.transfer(server, worldPosition, this::energy, this::extractEnergy, transferBudget.extractRemaining(server.getGameTime()));
        for (Direction direction : Direction.values()) {
            int allowance=transferBudget.extractRemaining(server.getGameTime());
            if(allowance<=0||energy()<=0)break;
            if (server.hasChunkAt(worldPosition.relative(direction))
                    && br.com.quantumindustries.energy.EnergyPorts.connects(server.getBlockState(worldPosition.relative(direction)), direction.getOpposite())
                    && server.getBlockEntity(worldPosition.relative(direction)) instanceof EnergyInspectable target
                    && !(target instanceof PiezoSwitchBlockEntity) && energy() > 0) {
                int amount = target.receiveEnergy(Math.min(energy(), allowance));
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
