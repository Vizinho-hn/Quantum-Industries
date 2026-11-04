package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.research.WorldResearchData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Research terminal. Old inventory slots are preserved only for loading saves
 * from v0.1.1. The new terminal does not process or manufacture items.
 * Legacy contents drop safely when the block is broken.
 */
public class ResearchTableBlockEntity extends BlockEntity implements MenuProvider {
    private int legacyProgress;
    private final SimpleContainer legacyInventory = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            super.setChanged();
            ResearchTableBlockEntity.this.setChanged();
        }
    };

    public final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            if (!(level instanceof ServerLevel serverLevel)) return 0;
            WorldResearchData research = WorldResearchData.get(serverLevel);
            return switch (index) {
                case 0 -> research.hasConductivity() ? 1 : 0;
                case 1 -> research.hasCopperWire() ? 1 : 0;
                default -> 0;
            };
        }

        @Override public void set(int index, int value) { /* server-authoritative */ }
        @Override public int getCount() { return 2; }
    };

    public ResearchTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.RESEARCH_TABLE_ENTITY.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.quantumindustries.research_table.title");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new br.com.quantumindustries.menu.ResearchTableMenu(id, inventory, this, dataAccess);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        // Legacy fields deliberately preserved so old contents aren't lost.
        output.putInt("progress", legacyProgress);
        for (int i = 0; i < legacyInventory.getContainerSize(); i++) {
            ItemStack stack = legacyInventory.getItem(i);
            if (!stack.isEmpty()) output.store("slot_" + i, ItemStack.CODEC, stack);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        legacyProgress = input.getIntOr("progress", 0);
        for (int i = 0; i < legacyInventory.getContainerSize(); i++) {
            legacyInventory.setItem(i, input.read("slot_" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY));
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null || level.isClientSide()) return;
        for (int i = 0; i < legacyInventory.getContainerSize(); i++) {
            ItemStack stack = legacyInventory.removeItemNoUpdate(i);
            if (!stack.isEmpty()) Block.popResource(level, pos, stack);
        }
    }
}
