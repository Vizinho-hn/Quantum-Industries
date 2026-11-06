package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.material.FormingMaterials;
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

/** Era II FE-powered forming press: stamps ingots into metal sheets using 60 FE over 20 ticks without hammer wear. */
public final class ElectromechanicalPressBlockEntity extends BlockEntity implements EnergyInspectable {
    public static final int CAPACITY = 480;
    public static final int MAX_RECEIVE = 48;
    public static final int WORK_TICKS = 20;
    public static final int ENERGY_PER_TICK = 3;
    public static final int ENERGY_PER_OPERATION = WORK_TICKS * ENERGY_PER_TICK;

    private final SimpleEnergyHandler battery = new SimpleEnergyHandler(CAPACITY, MAX_RECEIVE, ENERGY_PER_TICK);
    private ItemStack input = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private int progress;

    public ElectromechanicalPressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.ELECTROMECHANICAL_PRESS_ENTITY.get(), pos, state);
    }

    @Override public int energy() { return (int) battery.getAmountAsLong(); }
    @Override public int capacity() { return CAPACITY; }
    @Override public Component inspectTitle() {
        return Component.translatable("block.quantumindustries.electromechanical_press");
    }

    @Override public Component inspectDetail() {
        if (progress > 0) {
            return Component.translatable("gui.quantumindustries.press.progress", progress * 100 / WORK_TICKS);
        }
        if (!output.isEmpty()) {
            return Component.translatable("gui.quantumindustries.press.output_ready", output.getCount(), output.getHoverName());
        }
        if (!input.isEmpty()) {
            return Component.translatable("gui.quantumindustries.press.waiting_energy");
        }
        return Component.translatable("gui.quantumindustries.press.idle");
    }

    public ItemStack input() { return input; }
    public ItemStack output() { return output; }
    public int progress() { return progress; }

    @Override public int receiveEnergy(int maxReceive) {
        if (!(level instanceof ServerLevel) || maxReceive <= 0 || energy() >= CAPACITY) return 0;
        int inserted;
        try (var transaction = Transaction.openRoot()) {
            inserted = battery.insert(Math.min(MAX_RECEIVE, maxReceive), transaction);
            if (inserted > 0) transaction.commit();
        }
        if (inserted > 0) changed();
        return inserted;
    }

    public boolean insertIngot(ItemStack stack) {
        var metal = FormingMaterials.find(stack);
        if (!(level instanceof ServerLevel) || metal == null) return false;
        if (input.isEmpty()) {
            input = stack.copyWithCount(1);
            stack.shrink(1);
            changed();
            return true;
        }
        if (ItemStack.isSameItemSameComponents(input, stack) && input.getCount() < input.getMaxStackSize()) {
            input.grow(1);
            stack.shrink(1);
            changed();
            return true;
        }
        return false;
    }

    public ItemStack extractResult(boolean allowInput) {
        if (!(level instanceof ServerLevel)) return ItemStack.EMPTY;
        if (!output.isEmpty()) {
            ItemStack taken = output.copy();
            output = ItemStack.EMPTY;
            changed();
            return taken;
        }
        if (allowInput && !input.isEmpty()) {
            ItemStack taken = input.copy();
            input = ItemStack.EMPTY;
            progress = 0;
            changed();
            return taken;
        }
        return ItemStack.EMPTY;
    }

    private boolean canProcess(FormingMaterials.Metal metal) {
        if (metal == null) return false;
        ItemStack result = new ItemStack(metal.sheet().get());
        if (output.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(output, result) && output.getCount() < output.getMaxStackSize();
    }

    public void tick() {
        if (!(level instanceof ServerLevel server)) return;
        var metal = FormingMaterials.find(input);
        if (!canProcess(metal)) {
            if (progress != 0) {
                progress = 0;
                changed();
            }
            return;
        }
        if (energy() < ENERGY_PER_TICK) return;
        try (var transaction = Transaction.openRoot()) {
            if (battery.extract(ENERGY_PER_TICK, transaction) < ENERGY_PER_TICK) return;
            transaction.commit();
        }
        progress++;
        if (progress == WORK_TICKS / 2) {
            server.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.35f, 0.9f);
        }
        if (progress >= WORK_TICKS) {
            progress = 0;
            ItemStack sheet = new ItemStack(metal.sheet().get());
            server.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3f, 1.15f);
            server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, input.getItem()),
                    worldPosition.getX() + 0.5, worldPosition.getY() + 0.55, worldPosition.getZ() + 0.5,
                    6, 0.08, 0.04, 0.08, 0.02);
            input.shrink(1);
            if (output.isEmpty()) output = sheet;
            else output.grow(1);
        }
        changed();
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("stored_fe", energy());
        out.putInt("progress", progress);
        if (!input.isEmpty()) out.store("input", ItemStack.CODEC, input);
        if (!output.isEmpty()) out.store("output", ItemStack.CODEC, output);
    }

    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        battery.set(Math.clamp(in.getIntOr("stored_fe", 0), 0, CAPACITY));
        progress = Math.clamp(in.getIntOr("progress", 0), 0, WORK_TICKS);
        input = in.read("input", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        output = in.read("output", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
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

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
