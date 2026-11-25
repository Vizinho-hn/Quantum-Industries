package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.block.CopperCableBlock;
import br.com.quantumindustries.block.IndustrialCircuitSwitchBlock;
import br.com.quantumindustries.logistics.ConnectionMode;
import br.com.quantumindustries.energy.qes.ElectricalNetworkState.NetworkStatus;
import net.minecraft.core.Direction;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.logistics.ConfigurableConduit;
import br.com.quantumindustries.logistics.ConnectionModes;
import net.minecraft.core.BlockPos;
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

/** Stateless conductor for FE; records only transient load/loss for inspection. */
public final class CopperCableBlockEntity extends BlockEntity implements EnergyInspectable, ConfigurableConduit {
    private final br.com.quantumindustries.energy.CableTelemetryWindow telemetry = new br.com.quantumindustries.energy.CableTelemetryWindow();
    public br.com.quantumindustries.energy.CableTelemetryWindow.Sample completedTelemetry(long time) { return telemetry.completed(time); }
    private int flow;
    private int loss;
    private int voltageMv;
    private int currentMa;
    private int resistanceMOhm;
    private int qesStatusOrdinal;
    private long qesTick = Long.MIN_VALUE;
    private long lastFlow = Long.MIN_VALUE;
    private final ConnectionModes connections = new ConnectionModes();

    public CopperCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.COPPER_CABLE_ENTITY.get(), pos, state);
    }
    @Override public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) {
            // Rebuild connections for saved cables, including the old facing/shape format.
            BlockState state = getBlockState();
            BlockState connected = CopperCableBlock.computeState(server, worldPosition, state);
            if (connected != state) server.setBlock(worldPosition, connected, Block.UPDATE_ALL);
        }
    }
    @Override public int energy() {
        return getBlockState().getBlock() instanceof IndustrialCircuitSwitchBlock
                && !getBlockState().getValue(IndustrialCircuitSwitchBlock.CLOSED) ? 0 : flow;
    }
    @Override public int capacity() { return getBlockState().is(ModBlocks.COPPER_BUSBAR.get()) ? 32 : 64; }
    @Override public Component inspectTitle() {
        if (getBlockState().getBlock() instanceof IndustrialCircuitSwitchBlock)
            return Component.translatable("block.quantumindustries.industrial_circuit_switch");
        return Component.translatable(getBlockState().is(ModBlocks.COPPER_BUSBAR.get())
                ? "block.quantumindustries.copper_busbar" : "block.quantumindustries.insulated_conduit");
    }
    @Override public Component inspectDetail() {
        if (getBlockState().getBlock() instanceof IndustrialCircuitSwitchBlock)
            return Component.translatable("message.quantumindustries.industrial_circuit_switch.inspect",
                    Component.translatable(getBlockState().getValue(IndustrialCircuitSwitchBlock.CLOSED)
                            ? "message.quantumindustries.industrial_circuit_switch.closed" : "message.quantumindustries.industrial_circuit_switch.open"), energy(), capacity());
        return Component.translatable("gui.quantumindustries.cable.flow", flow, loss);
    }
    public int flow() { return flow; }
    public int loss() { return loss; }
    public int voltageMv() { return energy() > 0 ? voltageMv : 0; }
    public int currentMa() { return energy() > 0 ? currentMa : 0; }
    public int resistanceMOhm() { return energy() > 0 ? resistanceMOhm : 0; }
    public NetworkStatus qesStatus() {
        if (getBlockState().getBlock() instanceof IndustrialCircuitSwitchBlock
                && !getBlockState().getValue(IndustrialCircuitSwitchBlock.CLOSED)) return NetworkStatus.OPEN_CIRCUIT;
        return flow > 0 ? NetworkStatus.values()[qesStatusOrdinal] : NetworkStatus.IDLE;
    }

    /** Recorded immediately before recordTransfer, which publishes the combined update. */
    public void recordQesTelemetry(int voltageMv, int currentMa, int resistanceMOhm, NetworkStatus status) {
        if (!(level instanceof ServerLevel server)) return;
        long time = server.getGameTime();
        if (qesTick != time) {
            clearQesTelemetry();
            qesTick = time;
            this.voltageMv = voltageMv;
        } else {
            this.voltageMv = Math.min(this.voltageMv, voltageMv);
        }
        this.currentMa += currentMa;
        this.resistanceMOhm = resistanceMOhm;
        this.qesStatusOrdinal = Math.max(qesStatusOrdinal, status.ordinal());
    }

    private void clearQesTelemetry() {
        voltageMv = currentMa = resistanceMOhm = 0;
        qesStatusOrdinal = NetworkStatus.IDLE.ordinal();
        qesTick = Long.MIN_VALUE;
    }
    @Override public ConnectionModes connectionModes() { return connections; }
    @Override public ConnectionMode connectionMode(Direction face) {
        if (getBlockState().getBlock() instanceof IndustrialCircuitSwitchBlock)
            return IndustrialCircuitSwitchBlock.permits(getBlockState(), face) ? ConnectionMode.AUTO : ConnectionMode.NONE;
        return connections.get(face);
    }
    @Override public void connectionChanged() {
        sync();
        if (level instanceof ServerLevel server) {
            var state = CopperCableBlock.computeState(server, worldPosition, getBlockState());
            server.setBlock(worldPosition, state, Block.UPDATE_ALL);
            server.updateNeighborsAt(worldPosition, state.getBlock());
        }
    }
    /** Residual edge capacity for the *current* server tick. */
    public int availableThisTick(long time) {
        return Math.max(0, capacity() - (lastFlow == time ? flow : 0));
    }
    public void recordTransfer(int amount, int dissipated, long time) {
        recordTransfer(amount,dissipated,time,0,0);
    }
    public void recordTransfer(int amount, int dissipated, long time, int injected, int delivered) {
        // A junction can be part of several deliveries in one tick.
        if (lastFlow != time) { flow = 0; loss = 0; lastFlow = time; }
        if (amount > capacity() - flow) throw new IllegalStateException("Cable transfer limit exceeded");
        if (qesTick != time) clearQesTelemetry();
        flow += amount;
        loss += dissipated;
        telemetry.record(time, flow, amount, injected, delivered, dissipated);
        sync();
    }
    public void tick() {
        if (level instanceof ServerLevel server && getBlockState().getBlock() instanceof IndustrialCircuitSwitchBlock)
            IndustrialCircuitSwitchBlock.refreshRedstone(server, worldPosition, getBlockState());
        if (level instanceof ServerLevel server && flow > 0 && server.getGameTime() - lastFlow > 1) {
            flow = 0;
            loss = 0;
            clearQesTelemetry();
            sync();
        }
    }
    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        connections.save(out);
        // Cable load is transient: never saved as an energy battery.
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        connections.load(in);
        flow = in.getIntOr("cable_flow", 0);
        loss = in.getIntOr("cable_loss", 0);
        clearQesTelemetry();
        if (flow > 0) {
            voltageMv = in.getIntOr("qes_voltage_mv", 0);
            currentMa = in.getIntOr("qes_current_ma", 0);
            resistanceMOhm = in.getIntOr("qes_resistance_mohm", 0);
            qesStatusOrdinal = Math.clamp(in.getIntOr("qes_status", 0), 0, NetworkStatus.values().length - 1);
        }
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveCustomOnly(registries);
        tag.putInt("cable_flow", flow);
        tag.putInt("cable_loss", loss);
        tag.putInt("qes_voltage_mv", voltageMv());
        tag.putInt("qes_current_ma", currentMa());
        tag.putInt("qes_resistance_mohm", resistanceMOhm());
        tag.putInt("qes_status", qesStatus().ordinal());
        return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
