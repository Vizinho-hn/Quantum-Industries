package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.core.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Physical inputs belong to the bench; discoveries remain individual. */
public final class EmpiricalAssayBenchBlockEntity extends BlockEntity {
    private boolean loading;
    private final SimpleContainer inventory = new SimpleContainer(4) {
        @Override public void setChanged() {
            super.setChanged();
            EmpiricalAssayBenchBlockEntity.this.setChanged();
            if (!loading && level != null && !level.isClientSide())
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    };
    public EmpiricalAssayBenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.EMPIRICAL_ASSAY_ENTITY.get(), pos, state);
    }
    public SimpleContainer inventory() { return inventory; }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int i=0;i<4;i++) if (!inventory.getItem(i).isEmpty())
            output.store("input_"+i, ItemStack.CODEC, inventory.getItem(i));
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        loading=true;
        try {
            for (int i=0;i<4;i++) inventory.setItem(i,input.read("input_"+i,ItemStack.CODEC).orElse(ItemStack.EMPTY));
        } finally { loading=false; }
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos,state);
        if (level==null || level.isClientSide()) return;
        for (int i=0;i<4;i++) {
            ItemStack stack=inventory.removeItemNoUpdate(i);
            if (!stack.isEmpty()) Block.popResource(level,pos,stack);
        }
    }
}
