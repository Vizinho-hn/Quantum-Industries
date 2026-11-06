package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.core.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Wire drawing: four wires per sheet, either hand-cranked or powered by FE. */
public final class WireDrawingBenchBlockEntity extends BlockEntity implements EnergyInspectable {
    public static final int CAPACITY = 240, MAX_RECEIVE = 32, WORK_TICKS = 20, FE_PER_TICK = 3;
    private final SimpleEnergyHandler battery = new SimpleEnergyHandler(CAPACITY, MAX_RECEIVE, FE_PER_TICK);
    private ItemStack input = ItemStack.EMPTY, output = ItemStack.EMPTY;
    private int progress;
    private long lastStroke = -10;

    public WireDrawingBenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.WIRE_DRAWING_BENCH_ENTITY.get(), pos, state);
    }
    @Override public int energy() { return (int) battery.getAmountAsLong(); }
    @Override public int capacity() { return CAPACITY; }
    @Override public Component inspectTitle() {
        return Component.translatable("block.quantumindustries.wire_drawing_bench");
    }
    @Override public Component inspectDetail() {
        return Component.translatable("message.quantumindustries.wire.status", progress, WORK_TICKS, energy());
    }
    @Override public int receiveEnergy(int offered) {
        if (!(level instanceof ServerLevel) || offered <= 0) return 0;
        int inserted;
        try (var tx = Transaction.openRoot()) {
            inserted = battery.insert(Math.min(MAX_RECEIVE, offered), tx);
            if (inserted > 0) tx.commit();
        }
        if (inserted > 0) changed();
        return inserted;
    }
    private ItemStack resultFor(ItemStack item) {
        if (item.is(ModItems.COPPER_SHEET.get())) return new ItemStack(ModItems.COPPER_WIRE.get(), 4);
        if (item.is(ModItems.GOLD_SHEET.get())) return new ItemStack(ModItems.GOLD_WIRE.get(), 4);
        return ItemStack.EMPTY;
    }
    private boolean canRun() {
        ItemStack result = resultFor(input);
        return !result.isEmpty() && (output.isEmpty()
                || ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize());
    }
    public boolean insert(ItemStack stack) {
        if (!(level instanceof ServerLevel) || resultFor(stack).isEmpty()) return false;
        if (!input.isEmpty() && (!ItemStack.isSameItemSameComponents(input, stack)
                || input.getCount() >= input.getMaxStackSize())) return false;
        if (input.isEmpty()) input = stack.copyWithCount(1);
        else input.grow(1);
        stack.shrink(1);
        changed();
        return true;
    }
    public boolean manualStroke() {
        if (!(level instanceof ServerLevel server) || !canRun() || server.getGameTime() - lastStroke < 10) return false;
        lastStroke = server.getGameTime();
        progress = Math.min(WORK_TICKS, progress + 7);
        finishIfReady();
        changed();
        return true;
    }
    public ItemStack take(boolean takeInput) {
        if (!(level instanceof ServerLevel)) return ItemStack.EMPTY;
        ItemStack result = ItemStack.EMPTY;
        if (!output.isEmpty()) { result = output.copy(); output = ItemStack.EMPTY; }
        else if (takeInput && !input.isEmpty()) {
            result = input.copy(); input = ItemStack.EMPTY; progress = 0;
        }
        if (!result.isEmpty()) changed();
        return result;
    }
    public void tick() {
        if (!(level instanceof ServerLevel) || !canRun() || energy() < FE_PER_TICK) return;
        try (var tx = Transaction.openRoot()) {
            if (battery.extract(FE_PER_TICK, tx) < FE_PER_TICK) return;
            tx.commit();
        }
        progress++;
        finishIfReady();
        changed();
    }
    private void finishIfReady() {
        if (progress < WORK_TICKS || !canRun()) return;
        ItemStack result = resultFor(input);
        input.shrink(1);
        if (output.isEmpty()) output = result;
        else output.grow(result.getCount());
        progress = 0;
    }
    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("stored_fe", energy());
        out.putInt("progress", progress);
        out.putLong("last_stroke", lastStroke);
        if (!input.isEmpty()) out.store("input", ItemStack.CODEC, input);
        if (!output.isEmpty()) out.store("output", ItemStack.CODEC, output);
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        battery.set(Math.clamp(in.getIntOr("stored_fe", 0), 0, CAPACITY));
        progress = Math.clamp(in.getIntOr("progress", 0), 0, WORK_TICKS);
        lastStroke = in.getLongOr("last_stroke", -10);
        input = in.read("input", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        output = in.read("output", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server) {
            if (!input.isEmpty()) { Block.popResource(server, pos, input.copy()); input = ItemStack.EMPTY; }
            if (!output.isEmpty()) { Block.popResource(server, pos, output.copy()); output = ItemStack.EMPTY; }
        }
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
