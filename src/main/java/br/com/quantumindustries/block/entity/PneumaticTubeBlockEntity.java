package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.logistics.ItemRouting;
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
public final class PneumaticTubeBlockEntity extends BlockEntity {
    public static final int HOP_TICKS = 5;
    private ItemStack packet = ItemStack.EMPTY;
    private Direction inlet = Direction.DOWN;
    private int progress;

    public PneumaticTubeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.PNEUMATIC_TUBE_ENTITY.get(), pos, state);
    }
    public ItemStack packet() { return packet; }
    public float fraction(float partialTick) { return Math.min(1f, (progress + partialTick) / HOP_TICKS); }
    public Direction inlet() { return inlet; }

    public boolean accept(ItemStack incoming, Direction from) {
        if (!(level instanceof ServerLevel) || !packet.isEmpty() || incoming.isEmpty()) return false;
        packet = incoming.copy();
        incoming.setCount(0);
        inlet = from;
        progress = 0;
        changed();
        return true;
    }

    public void tick() {
        if (!(level instanceof ServerLevel server) || packet.isEmpty()) return;
        if (++progress < HOP_TICKS) return;
        progress = HOP_TICKS;
        // Do not immediately route back into the block that sent the packet.
        for (Direction direction : Direction.values()) {
            if (direction == inlet) continue;
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

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!packet.isEmpty()) out.store("packet", ItemStack.CODEC, packet);
        out.putInt("inlet", inlet.get3DDataValue());
        out.putInt("progress", progress);
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        packet = in.read("packet", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        inlet = Direction.from3DDataValue(in.getIntOr("inlet", 0));
        progress = Math.clamp(in.getIntOr("progress", 0), 0, HOP_TICKS);
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
