package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.block.GasTransporterBlock;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.gas.IndustrialGas;
import br.com.quantumindustries.logistics.ConfigurableConduit;
import br.com.quantumindustries.logistics.ConnectionModes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** One sealed gas segment. A single server thread debits and credits each handoff. */
public final class GasTransporterBlockEntity extends BlockEntity implements ConfigurableConduit {
    public static final int CAPACITY = 200, MAX_RATE = 20, MAX_KPA = 400;
    private final ConnectionModes modes = new ConnectionModes();
    private IndustrialGas gas;
    private int amount, received, sent, lastFlow;
    private Direction lastDirection;
    private Direction visualDirection;
    private long budgetTick = Long.MIN_VALUE;

    public GasTransporterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.GAS_TRANSPORTER_ENTITY.get(), pos, state);
    }
    public IndustrialGas gas() { return gas; }
    public int amount() { return amount; }
    public int pressureKpa() { return gas == null ? 0 : gas.pressureKpa(amount, CAPACITY); }
    public Direction flowDirection() { return visualDirection; }
    public int flow() { return lastFlow; }
    @Override public ConnectionModes connectionModes() { return modes; }
    @Override public void onLoad() { super.onLoad(); connectionChanged(); }
    @Override public void connectionChanged() {
        if (!(level instanceof ServerLevel server)) return;
        var state = GasTransporterBlock.connected(server, worldPosition, getBlockState());
        server.setBlock(worldPosition, state, Block.UPDATE_ALL);
        server.updateNeighborsAt(worldPosition, state.getBlock());
        sync();
    }

    private void budget(long now) {
        if (budgetTick == now) return;
        budgetTick = now;
        received = sent = 0;
        lastDirection = null;
    }
    private int room(IndustrialGas resource, Direction face, long now) {
        budget(now);
        if (!connectionMode(face).allowsInput() || (gas != null && gas != resource)) return 0;
        return Math.min(CAPACITY - amount, MAX_RATE - received);
    }
    private void add(IndustrialGas resource, int accepted, Direction travel) {
        gas = resource;
        amount += accepted;
        received += accepted;
        lastDirection = travel;
        setChanged();
    }
    public void tick() {
        if (!(level instanceof ServerLevel server)) return;
        long now = server.getGameTime();
        budget(now);
        int start = Math.floorMod(now + worldPosition.asLong(), Direction.values().length);
        for (int i = 0; i < 6; i++) {
            var side = Direction.values()[(start + i) % 6];
            BlockPos next = worldPosition.relative(side);
            if (!server.hasChunkAt(next)) continue;
            var neighbor = server.getBlockEntity(next);
            var face = side.getOpposite();
            if (neighbor instanceof SteamBoilerBlockEntity boiler) {
                int offer = Math.min(room(IndustrialGas.STEAM, side, now),
                        Math.max(0, (boiler.outletPressureKpa() - pressureKpa()) / 4));
                int taken = boiler.takeSteam(Math.min(offer, boiler.steam()), face);
                if (taken > 0) add(IndustrialGas.STEAM, taken, face);
            } else if (amount > 0 && sent < MAX_RATE && connectionMode(side).allowsOutput()) {
                int offer = Math.min(Math.min(amount, MAX_RATE - sent), MAX_RATE / 2);
                int moved = 0;
                if (neighbor instanceof GasTransporterBlockEntity pipe) {
                    moved = Math.min(offer, Math.min(pipe.room(gas, face, now), (pressureKpa() - pipe.pressureKpa()) / 4));
                    if (moved > 0) pipe.add(gas, moved, side);
                } else if (neighbor instanceof RotaryConversionBlockEntity engine) {
                    moved = engine.receiveGas(gas, offer, face, pressureKpa());
                }
                if (moved > 0) {
                    amount -= moved;
                    sent += moved;
                    lastDirection = side;
                    if (amount == 0) gas = null;
                    setChanged();
                }
            }
        }
        int flow = Math.max(sent, received);
        if (lastFlow != flow || visualDirection != lastDirection || flow > 0 && now % 5 == 0) {
            lastFlow = flow;
            visualDirection = lastDirection;
            sync();
        }
    }
    private void sync() {
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        modes.save(output);
        if (gas != null) output.putString("gas", gas.id.toString());
        output.putInt("amount", amount);
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        modes.load(input);
        gas = IndustrialGas.byId(input.getStringOr("gas", ""));
        amount = gas == null ? 0 : Math.clamp(input.getIntOr("amount", 0), 0, CAPACITY);
        if (amount == 0) gas = null;
        int direction = input.getIntOr("flow_direction", -1);
        visualDirection = direction < 0 || direction >= 6 ? null : Direction.from3DDataValue(direction);
        lastFlow = Math.clamp(input.getIntOr("flow", 0), 0, MAX_RATE);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider lookup) {
        var tag = saveCustomOnly(lookup);
        tag.putInt("flow", lastFlow);
        if (visualDirection != null) tag.putInt("flow_direction", visualDirection.get3DDataValue());
        return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
