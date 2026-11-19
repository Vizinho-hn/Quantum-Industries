package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.block.PneumaticTubeBlock;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.logistics.ItemRouting;
import br.com.quantumindustries.logistics.ConfigurableConduit;
import br.com.quantumindustries.logistics.ConnectionModes;
import br.com.quantumindustries.logistics.ConnectionMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** One physically resident packet per segment, persisted on chunk unload. */
public final class PneumaticTubeBlockEntity extends BlockEntity implements ConfigurableConduit {
    public static final int HOP_TICKS = 5;
    private ItemStack packet = ItemStack.EMPTY;
    private Direction inlet = Direction.DOWN;
    private int progress;
    private long enteredAt;
    private final ConnectionModes connections = new ConnectionModes();

    public PneumaticTubeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.PNEUMATIC_TUBE_ENTITY.get(), pos, state);
    }
    public ItemStack packet() { return packet; }
    public float fraction(float partialTick) { return Math.min(1f, (progress + partialTick) / HOP_TICKS); }
    public Direction inlet() { return inlet; }
    public long enteredAt() { return enteredAt; }
    @Override public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) {
            BlockState state = getBlockState();
            BlockState connected = PneumaticTubeBlock.computeState(server, worldPosition, state);
            if (connected != state) server.setBlock(worldPosition, connected, Block.UPDATE_ALL);
        }
    }
    @Override public ConnectionModes connectionModes() { return connections; }
    @Override public void connectionChanged() {
        changed();
        if (level instanceof ServerLevel server) {
            BlockState state = PneumaticTubeBlock.computeState(server, worldPosition, getBlockState());
            server.setBlock(worldPosition, state, Block.UPDATE_ALL);
            server.updateNeighborsAt(worldPosition, state.getBlock());
        }
    }

    public boolean accept(ItemStack incoming, Direction from) {
        if (!(level instanceof ServerLevel) || !packet.isEmpty() || incoming.isEmpty()
                || !connectionMode(from).allowsInput()) return false;
        packet = incoming.copy();
        incoming.setCount(0);
        inlet = from;
        progress = 0;
        enteredAt=((ServerLevel)level).getGameTime();
        changed();
        return true;
    }

    public void tick() {
        if (!(level instanceof ServerLevel server)) return;
        if (packet.isEmpty()) {
            if (server.getGameTime() % HOP_TICKS == 0) pullFromInventory(server);
            return;
        }
        if (++progress < HOP_TICKS) return;
        progress = HOP_TICKS;
        // Do not immediately route back into the block that sent the packet.
        for (Direction direction : Direction.values()) {
            if (direction == inlet || !connectionMode(direction).allowsOutput()) continue;
            BlockPos next = worldPosition.relative(direction);
            if (ItemRouting.insert(server, next, packet, direction.getOpposite())) {
                if (packet.isEmpty()) {
                    packet = ItemStack.EMPTY;
                    progress = 0;
                }
                changed();
                return;
            }
        }
    }

    private void pullFromInventory(ServerLevel server) {
        for (Direction direction : Direction.values()) {
            if (connectionMode(direction) != ConnectionMode.PULL) continue;
            BlockPos source = worldPosition.relative(direction);
            if (ItemRouting.preview(server, source, 1).isEmpty()) continue;
            ItemStack extracted = ItemRouting.extract(server, source, 1);
            if (!extracted.isEmpty()) {
                accept(extracted, direction);
                return;
            }
        }
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        connections.save(out);
        if (!packet.isEmpty()) out.store("packet", ItemStack.CODEC, packet);
        out.putInt("inlet", inlet.get3DDataValue());
        out.putInt("progress", progress);
        out.putLong("entered_at", enteredAt);
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        connections.load(in);
        packet = in.read("packet", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        inlet = Direction.from3DDataValue(in.getIntOr("inlet", 0));
        progress = Math.clamp(in.getIntOr("progress", 0), 0, HOP_TICKS);
        enteredAt = in.getLongOr("entered_at", 0L);
    }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server && !packet.isEmpty()) {
            Block.popResource(server, pos, packet.copy());
            packet = ItemStack.EMPTY;
        }
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider lookups) { return saveCustomOnly(lookups); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
