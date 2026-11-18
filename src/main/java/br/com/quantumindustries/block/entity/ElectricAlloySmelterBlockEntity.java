package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.material.AlloyCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
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

/** Era II FE-powered induction alloy furnace: smelts both Tier 1 (Bronze, Brass) and Tier 2 (Invar, Electrum, Constantan, Steel) alloys. */
public final class ElectricAlloySmelterBlockEntity extends BlockEntity implements EnergyInspectable {
    public static final int CAPACITY = 1200;
    public static final int MAX_RECEIVE = 64;
    public static final int ENERGY_PER_TICK = 4;

    private final SimpleEnergyHandler battery = new SimpleEnergyHandler(CAPACITY, MAX_RECEIVE, ENERGY_PER_TICK);
    private ItemStack inputA = ItemStack.EMPTY;
    private ItemStack inputB = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private int progress;

    public ElectricAlloySmelterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.ELECTRIC_ALLOY_SMELTER_ENTITY.get(), pos, state);
    }

    @Override public int energy() { return (int) battery.getAmountAsLong(); }
    @Override public int capacity() { return CAPACITY; }
    @Override public Component inspectTitle() {
        return Component.translatable("block.quantumindustries.electric_alloy_smelter");
    }

    @Override public Component inspectDetail() {
        var recipe = AlloyCatalog.findMatch(inputA, inputB, 2);
        if (progress > 0 && recipe != null) {
            return Component.translatable("gui.quantumindustries.smelter.progress", progress * 100 / recipe.workTicks());
        }
        if (!output.isEmpty()) {
            return Component.translatable("gui.quantumindustries.smelter.output_ready", output.getCount(), output.getHoverName());
        }
        if (recipe != null) {
            return Component.translatable("gui.quantumindustries.smelter.waiting_energy");
        }
        return Component.translatable("gui.quantumindustries.smelter.idle");
    }

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

    public boolean insertIngredient(ItemStack stack) {
        if (!(level instanceof ServerLevel) || !AlloyCatalog.isValidIngredient(stack, 2)) return false;
        if (!inputA.isEmpty() && ItemStack.isSameItemSameComponents(inputA, stack) && inputA.getCount() < 16) {
            inputA.grow(1);
            stack.shrink(1);
            changed();
            return true;
        }
        if (!inputB.isEmpty() && ItemStack.isSameItemSameComponents(inputB, stack) && inputB.getCount() < 16) {
            inputB.grow(1);
            stack.shrink(1);
            changed();
            return true;
        }
        if (inputA.isEmpty()) {
            inputA = stack.copyWithCount(1);
            stack.shrink(1);
            changed();
            return true;
        }
        if (inputB.isEmpty() && AlloyCatalog.canPair(inputA, stack, 2)) {
            inputB = stack.copyWithCount(1);
            stack.shrink(1);
            changed();
            return true;
        }
        return false;
    }

    public ItemStack output() { return output; }

    public ItemStack extractOutput(int maximum) {
        if (!(level instanceof ServerLevel) || output.isEmpty() || maximum <= 0) return ItemStack.EMPTY;
        ItemStack result = output.split(Math.min(maximum, output.getCount()));
        if (output.isEmpty()) output = ItemStack.EMPTY;
        changed();
        return result;
    }

    public ItemStack extractResult(boolean allowInput) {
        if (!(level instanceof ServerLevel)) return ItemStack.EMPTY;
        if (!output.isEmpty()) {
            ItemStack taken = output.copy();
            output = ItemStack.EMPTY;
            changed();
            return taken;
        }
        if (allowInput) {
            if (!inputB.isEmpty()) {
                ItemStack taken = inputB.copy();
                inputB = ItemStack.EMPTY;
                progress = 0;
                changed();
                return taken;
            }
            if (!inputA.isEmpty()) {
                ItemStack taken = inputA.copy();
                inputA = ItemStack.EMPTY;
                progress = 0;
                changed();
                return taken;
            }
        }
        return ItemStack.EMPTY;
    }

    private boolean canProcess(AlloyCatalog.Recipe recipe) {
        if (recipe == null) return false;
        ItemStack result = recipe.assemble();
        if (output.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    public void tick() {
        if (!(level instanceof ServerLevel server)) return;
        var recipe = AlloyCatalog.findMatch(inputA, inputB, 2);
        if (!canProcess(recipe)) {
            if (progress != 0) {
                progress = 0;
                changed();
            }
            return;
        }
        int prevTarget = progress * recipe.totalFe() / recipe.workTicks();
        int nextTarget = (progress + 1) * recipe.totalFe() / recipe.workTicks();
        int stepFe = Math.max(0, nextTarget - prevTarget);
        if (energy() < stepFe) return;
        if (stepFe > 0) {
            try (var transaction = Transaction.openRoot()) {
                if (battery.extract(stepFe, transaction) < stepFe) return;
                transaction.commit();
            }
        }
        progress++;
        if (progress % 20 == 0) {
            server.playSound(null, worldPosition, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.35f, 1.15f);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 0.75, worldPosition.getZ() + 0.5,
                    3, 0.1, 0.05, 0.1, 0.01);
        }
        if (progress >= recipe.workTicks()) {
            progress = 0;
            ItemStack produced = recipe.assemble();
            recipe.consume(inputA, inputB);
            if (inputA.isEmpty()) inputA = ItemStack.EMPTY;
            if (inputB.isEmpty()) inputB = ItemStack.EMPTY;
            if (output.isEmpty()) output = produced;
            else output.grow(produced.getCount());
            server.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.35f, 1.25f);
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
        if (!inputA.isEmpty()) out.store("input_a", ItemStack.CODEC, inputA);
        if (!inputB.isEmpty()) out.store("input_b", ItemStack.CODEC, inputB);
        if (!output.isEmpty()) out.store("output", ItemStack.CODEC, output);
    }

    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        battery.set(Math.clamp(in.getIntOr("stored_fe", 0), 0, CAPACITY));
        progress = Math.clamp(in.getIntOr("progress", 0), 0, 200);
        inputA = in.read("input_a", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        inputB = in.read("input_b", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        output = in.read("output", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server) {
            if (!inputA.isEmpty()) { Block.popResource(server, pos, inputA.copy()); inputA = ItemStack.EMPTY; }
            if (!inputB.isEmpty()) { Block.popResource(server, pos, inputB.copy()); inputB = ItemStack.EMPTY; }
            if (!output.isEmpty()) { Block.popResource(server, pos, output.copy()); output = ItemStack.EMPTY; }
        }
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
