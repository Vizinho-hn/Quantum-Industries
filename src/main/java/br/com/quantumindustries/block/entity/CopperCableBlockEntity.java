package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.block.CopperCableBlock;
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
    private int flow;
    private int loss;
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
    @Override public int energy() { return flow; }
    @Override public int capacity() { return getBlockState().is(ModBlocks.COPPER_BUSBAR.get()) ? 32 : 64; }
    @Override public Component inspectTitle() {
        return Component.translatable(getBlockState().is(ModBlocks.COPPER_BUSBAR.get())
                ? "block.quantumindustries.copper_busbar" : "block.quantumindustries.insulated_conduit");
    }
    @Override public Component inspectDetail() {
        return Component.translatable("gui.quantumindustries.cable.flow", flow, loss);
    }
    public int flow() { return flow; }
    @Override public ConnectionModes connectionModes() { return connections; }
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
        // A junction can be part of several deliveries in one tick.
        if (lastFlow != time) { flow = 0; loss = 0; lastFlow = time; }
        if (amount > capacity() - flow) throw new IllegalStateException("Cable transfer limit exceeded");
        flow += amount;
        loss += dissipated;
        sync();
    }
    public void tick() {
        if (level instanceof ServerLevel server && flow > 0 && server.getGameTime() - lastFlow > 1) {
            flow = 0;
            loss = 0;
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
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveCustomOnly(registries);
        tag.putInt("cable_flow", flow);
        tag.putInt("cable_loss", loss);
        return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
