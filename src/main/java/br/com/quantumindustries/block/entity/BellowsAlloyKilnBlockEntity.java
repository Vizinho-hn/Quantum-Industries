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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Era I bellows-driven crucible furnace for smelting Copper + Tin (Bronze) and Copper + Zinc (Brass). */
public final class BellowsAlloyKilnBlockEntity extends BlockEntity {
    public static final int FUEL_PER_COAL = 1200, HEAT_PER_PUFF = 100, MAX_HEAT = 400, SMELT_TICKS = 80;
    private int fuel, heat, progress;
    private long lastPuff = -10;
    private ItemStack inputA = ItemStack.EMPTY;
    private ItemStack inputB = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;

    public BellowsAlloyKilnBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.BELLOWS_ALLOY_KILN_ENTITY.get(), pos, state);
    }

    public Component status() {
        if (!output.isEmpty()) {
            return Component.translatable("gui.quantumindustries.kiln.output_ready", output.getCount(), output.getHoverName());
        }
        return Component.translatable("message.quantumindustries.kiln.status", fuel, heat, progress * 100 / SMELT_TICKS);
    }

    public boolean insert(ItemStack stack) {
        if (!(level instanceof ServerLevel)) return false;
        if ((stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) && fuel <= FUEL_PER_COAL * 4) {
            fuel += FUEL_PER_COAL;
            stack.shrink(1);
            changed();
            return true;
        }
        if (br.com.quantumindustries.ModItems.CHEMICAL_PRODUCTS.containsKey("fluorite")
                && stack.is(br.com.quantumindustries.ModItems.CHEMICAL_PRODUCTS.get("fluorite").get())
                && fuel <= FUEL_PER_COAL * 4) {
            fuel += FUEL_PER_COAL / 2;
            heat = Math.min(MAX_HEAT, heat + HEAT_PER_PUFF + 50);
            stack.shrink(1);
            changed();
            return true;
        }
        if (!AlloyCatalog.isValidIngredient(stack, 1)) return false;

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
        if (inputB.isEmpty() && AlloyCatalog.canPair(inputA, stack, 1)) {
            inputB = stack.copyWithCount(1);
            stack.shrink(1);
            changed();
            return true;
        }
        return false;
    }

    public boolean puff() {
        if (!(level instanceof ServerLevel server) || fuel <= 0 || server.getGameTime() - lastPuff < 10) return false;
        lastPuff = server.getGameTime();
        heat = Math.min(MAX_HEAT, heat + HEAT_PER_PUFF);
        server.sendParticles(ParticleTypes.FLAME, worldPosition.getX() + 0.5, worldPosition.getY() + 0.6, worldPosition.getZ() + 0.5, 4, 0.12, 0.08, 0.12, 0.01);
        changed();
        return true;
    }

    public ItemStack take(boolean includeInputs) {
        if (!(level instanceof ServerLevel)) return ItemStack.EMPTY;
        if (!output.isEmpty()) {
            ItemStack result = output.copy();
            output = ItemStack.EMPTY;
            changed();
            return result;
        }
        if (includeInputs) {
            if (!inputB.isEmpty()) {
                ItemStack result = inputB.copy();
                inputB = ItemStack.EMPTY;
                progress = 0;
                changed();
                return result;
            }
            if (!inputA.isEmpty()) {
                ItemStack result = inputA.copy();
                inputA = ItemStack.EMPTY;
                progress = 0;
                changed();
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    private boolean canOutput(AlloyCatalog.Recipe recipe) {
        if (recipe == null) return false;
        ItemStack result = recipe.assemble();
        if (output.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    public void tick() {
        if (!(level instanceof ServerLevel server)) return;
        if (heat <= 0 || fuel <= 0) return;
        heat--;
        fuel--;
        var recipe = AlloyCatalog.findMatch(inputA, inputB, 1);
        if (canOutput(recipe)) {
            progress++;
            if (progress % 25 == 0) {
                server.playSound(null, worldPosition, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.4f, 1.0f);
                server.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 0.9, worldPosition.getZ() + 0.5, 2, 0.08, 0.05, 0.08, 0.01);
            }
            if (progress >= SMELT_TICKS) {
                ItemStack produced = recipe.assemble();
                recipe.consume(inputA, inputB);
                if (inputA.isEmpty()) inputA = ItemStack.EMPTY;
                if (inputB.isEmpty()) inputB = ItemStack.EMPTY;
                if (output.isEmpty()) output = produced;
                else output.grow(produced.getCount());
                progress = 0;
                server.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.35f, 1.2f);
            }
        } else {
            progress = 0;
        }
        if (server.getGameTime() % 10 == 0 || !output.isEmpty()) changed();
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("fuel", fuel);
        out.putInt("heat", heat);
        out.putInt("progress", progress);
        out.putLong("last_puff", lastPuff);
        if (!inputA.isEmpty()) out.store("input_a", ItemStack.CODEC, inputA);
        if (!inputB.isEmpty()) out.store("input_b", ItemStack.CODEC, inputB);
        if (!output.isEmpty()) out.store("output", ItemStack.CODEC, output);
    }

    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        fuel = Math.max(0, in.getIntOr("fuel", 0));
        heat = Math.clamp(in.getIntOr("heat", 0), 0, MAX_HEAT);
        progress = Math.clamp(in.getIntOr("progress", 0), 0, SMELT_TICKS);
        lastPuff = in.getLongOr("last_puff", -10);
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
