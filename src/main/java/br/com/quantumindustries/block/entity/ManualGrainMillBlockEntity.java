package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.core.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** A simple hand-driven mill: four strokes convert one wheat into one flour. */
public final class ManualGrainMillBlockEntity extends BlockEntity {
    public static final int STROKES = 4;
    private ItemStack input = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private int progress;
    private long lastStroke = -10;

    public ManualGrainMillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MANUAL_GRAIN_MILL_ENTITY.get(), pos, state);
    }

    public boolean insert(ItemStack held) {
        if (!(level instanceof ServerLevel) || !held.is(Items.WHEAT)
                || input.getCount() >= held.getMaxStackSize()) return false;
        if (input.isEmpty()) input = held.copyWithCount(1);
        else input.grow(1);
        held.shrink(1);
        changed();
        return true;
    }

    public boolean stroke() {
        if (!(level instanceof ServerLevel server) || input.isEmpty()
                || server.getGameTime() - lastStroke < 6) return false;
        if (!output.isEmpty() && (!output.is(ModItems.FLOUR.get())
                || output.getCount() >= output.getMaxStackSize())) return false;

        lastStroke = server.getGameTime();
        progress++;
        if (progress >= STROKES) {
            progress = 0;
            input.shrink(1);
            if (output.isEmpty()) output = new ItemStack(ModItems.FLOUR.get());
            else output.grow(1);
        }
        changed();
        return true;
    }

    /** Sneak-click takes all flour, or returns unused grain if flour is empty. */
    public ItemStack take() {
        if (!(level instanceof ServerLevel)) return ItemStack.EMPTY;
        ItemStack result;
        if (!output.isEmpty()) {
            result = output.copy();
            output = ItemStack.EMPTY;
        } else if (!input.isEmpty()) {
            result = input.copy();
            input = ItemStack.EMPTY;
            progress = 0;
        } else return ItemStack.EMPTY;
        changed();
        return result;
    }

    public Component status() {
        return Component.translatable("gui.quantumindustries.grain_mill.status",
                input.getCount(), output.getCount(), progress, STROKES);
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!input.isEmpty()) out.store("input", ItemStack.CODEC, input);
        if (!output.isEmpty()) out.store("output", ItemStack.CODEC, output);
        out.putInt("progress", progress);
    }

    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        input = in.read("input", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        output = in.read("output", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        progress = Math.clamp(in.getIntOr("progress", 0), 0, STROKES - 1);
    }

    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server) {
            if (!input.isEmpty()) { Block.popResource(server, pos, input.copy()); input = ItemStack.EMPTY; }
            if (!output.isEmpty()) { Block.popResource(server, pos, output.copy()); output = ItemStack.EMPTY; }
            progress = 0;
        }
    }
}
