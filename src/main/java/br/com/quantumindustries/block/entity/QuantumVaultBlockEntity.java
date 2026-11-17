package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.core.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class QuantumVaultBlockEntity extends BlockEntity {
    public static final int MAX_CAPACITY = 512;

    private ItemStack storedTemplate = ItemStack.EMPTY;
    private int storedCount = 0;

    public QuantumVaultBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.QUANTUM_VAULT_ENTITY.get(), pos, state);
    }

    public ItemStack storedItem() {
        return storedTemplate.isEmpty() || storedCount <= 0
                ? ItemStack.EMPTY
                : storedTemplate.copyWithCount(Math.min(storedCount, storedTemplate.getMaxStackSize()));
    }

    public int storedCount() {
        return storedCount;
    }

    public void setInitialItem(ItemStack stack) {
        if (stack.isEmpty()) {
            storedTemplate = ItemStack.EMPTY;
            storedCount = 0;
        } else {
            storedTemplate = stack.copyWithCount(1);
            storedCount = stack.getCount();
        }
        sync();
    }

    public int insert(ItemStack stack) {
        if (stack.isEmpty()) return 0;

        if (storedTemplate.isEmpty() || storedCount <= 0) {
            int toInsert = Math.min(stack.getCount(), MAX_CAPACITY);
            storedTemplate = stack.copyWithCount(1);
            storedCount = toInsert;
            stack.shrink(toInsert);
            sync();
            return toInsert;
        }

        if (ItemStack.isSameItemSameComponents(storedTemplate, stack) && storedCount < MAX_CAPACITY) {
            int toInsert = Math.min(stack.getCount(), MAX_CAPACITY - storedCount);
            storedCount += toInsert;
            stack.shrink(toInsert);
            sync();
            return toInsert;
        }

        return 0;
    }

    public ItemStack extract(boolean fullStack) {
        if (storedTemplate.isEmpty() || storedCount <= 0) {
            return ItemStack.EMPTY;
        }
        int maxTake = fullStack ? storedTemplate.getMaxStackSize() : 1;
        int taken = Math.min(storedCount, maxTake);
        ItemStack result = storedTemplate.copyWithCount(taken);
        storedCount -= taken;
        if (storedCount <= 0) {
            storedCount = 0;
            storedTemplate = ItemStack.EMPTY;
        }
        sync();
        return result;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("stored_count", storedCount);
        if (!storedTemplate.isEmpty() && storedCount > 0) {
            out.store("stored_item", ItemStack.CODEC, storedTemplate);
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        storedCount = Math.clamp(in.getIntOr("stored_count", 0), 0, MAX_CAPACITY);
        storedTemplate = in.read("stored_item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        if (storedTemplate.isEmpty() || storedCount <= 0) {
            storedTemplate = ItemStack.EMPTY;
            storedCount = 0;
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

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null || level.isClientSide()) return;
        while (storedCount > 0 && !storedTemplate.isEmpty()) {
            int dropCount = Math.min(storedCount, storedTemplate.getMaxStackSize());
            Block.popResource(level, pos, storedTemplate.copyWithCount(dropCount));
            storedCount -= dropCount;
        }
        storedTemplate = ItemStack.EMPTY;
    }
}
