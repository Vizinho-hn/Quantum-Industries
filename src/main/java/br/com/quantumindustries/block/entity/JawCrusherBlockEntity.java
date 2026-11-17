package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.material.CrushingCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Hybrid Jaw Crusher: operates manually via Hand Crank strokes in Era I (3 strokes)
 * or automatically with Forge Energy in Era II (60 FE over 20 ticks).
 * Doubles raw ore yield into 2x purified mineral dust and mills 1x wheat into 2x flour.
 */
public final class JawCrusherBlockEntity extends BlockEntity implements EnergyInspectable {
    public static final int CAPACITY = 480;
    public static final int MAX_RECEIVE = 48;
    public static final int WORK_TICKS = 20;
    public static final int FE_PER_TICK = 3;

    private final SimpleEnergyHandler battery = new SimpleEnergyHandler(CAPACITY, MAX_RECEIVE, FE_PER_TICK);
    private ItemStack input = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private int progress;
    private long lastStroke = -10;

    public JawCrusherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.JAW_CRUSHER_ENTITY.get(), pos, state);
    }

    @Override
    public int energy() {
        return (int) battery.getAmountAsLong();
    }

    @Override
    public int capacity() {
        return CAPACITY;
    }

    @Override
    public Component inspectTitle() {
        return Component.translatable("block.quantumindustries.jaw_crusher");
    }

    @Override
    public Component inspectDetail() {
        return Component.translatable("message.quantumindustries.jaw_crusher.status",
                progress, WORK_TICKS, energy(),
                input.isEmpty() ? Component.literal("-") : input.getHoverName(),
                output.isEmpty() ? 0 : output.getCount());
    }

    @Override
    public int receiveEnergy(int offered) {
        if (!(level instanceof ServerLevel) || offered <= 0 || energy() >= CAPACITY) return 0;
        int inserted;
        try (var tx = Transaction.openRoot()) {
            inserted = battery.insert(Math.min(MAX_RECEIVE, offered), tx);
            if (inserted > 0) tx.commit();
        }
        if (inserted > 0) changed();
        return inserted;
    }

    public ItemStack input() { return input; }
    public ItemStack output() { return output; }
    public int progress() { return progress; }

    private boolean canRun() {
        ItemStack result = CrushingCatalog.resultFor(input);
        return !result.isEmpty() && (output.isEmpty()
                || ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize());
    }

    public boolean insert(ItemStack stack) {
        if (!(level instanceof ServerLevel) || !CrushingCatalog.isValidInput(stack)) return false;
        if (!input.isEmpty() && (!ItemStack.isSameItemSameComponents(input, stack)
                || input.getCount() >= input.getMaxStackSize())) return false;
        if (input.isEmpty()) input = stack.copyWithCount(1);
        else input.grow(1);
        stack.shrink(1);
        changed();
        return true;
    }

    public boolean manualStroke() {
        if (!(level instanceof ServerLevel server) || !canRun() || server.getGameTime() - lastStroke < 8) return false;
        lastStroke = server.getGameTime();
        progress = Math.min(WORK_TICKS, progress + 7);
        server.playSound(null, worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.45f, 0.9f);
        finishIfReady(server);
        changed();
        return true;
    }

    public ItemStack take(boolean takeInput) {
        if (!(level instanceof ServerLevel)) return ItemStack.EMPTY;
        ItemStack result = ItemStack.EMPTY;
        if (!output.isEmpty()) {
            result = output.copy();
            output = ItemStack.EMPTY;
        } else if (takeInput && !input.isEmpty()) {
            result = input.copy();
            input = ItemStack.EMPTY;
            progress = 0;
        }
        if (!result.isEmpty()) changed();
        return result;
    }

    public void tick() {
        if (!(level instanceof ServerLevel server) || !canRun() || energy() < FE_PER_TICK) return;
        try (var tx = Transaction.openRoot()) {
            if (battery.extract(FE_PER_TICK, tx) < FE_PER_TICK) return;
            tx.commit();
        }
        progress++;
        if (progress == WORK_TICKS / 2) {
            server.playSound(null, worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.35f, 1.05f);
        }
        finishIfReady(server);
        changed();
    }

    private void finishIfReady(ServerLevel server) {
        if (progress < WORK_TICKS || !canRun()) return;
        ItemStack result = CrushingCatalog.resultFor(input);
        server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, input.getItem()),
                worldPosition.getX() + 0.5, worldPosition.getY() + 0.65, worldPosition.getZ() + 0.5,
                8, 0.1, 0.05, 0.1, 0.03);
        input.shrink(1);
        if (output.isEmpty()) output = result;
        else output.grow(result.getCount());
        progress = 0;
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("stored_fe", energy());
        out.putInt("progress", progress);
        out.putLong("last_stroke", lastStroke);
        if (!input.isEmpty()) out.store("input", ItemStack.CODEC, input);
        if (!output.isEmpty()) out.store("output", ItemStack.CODEC, output);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        battery.set(Math.clamp(in.getIntOr("stored_fe", 0), 0, CAPACITY));
        progress = Math.clamp(in.getIntOr("progress", 0), 0, WORK_TICKS);
        lastStroke = in.getLongOr("last_stroke", -10);
        input = in.read("input", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        output = in.read("output", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server) {
            if (!input.isEmpty()) {
                Block.popResource(server, pos, input.copy());
                input = ItemStack.EMPTY;
            }
            if (!output.isEmpty()) {
                Block.popResource(server, pos, output.copy());
                output = ItemStack.EMPTY;
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
