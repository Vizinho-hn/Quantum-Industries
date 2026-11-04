package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.research.WorldResearchData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ResearchTableBlockEntity extends BlockEntity implements MenuProvider {
    private static final int SAMPLE = 0;
    private static final int TOOL = 1;
    private static final int OUTPUT = 2;
    private static final int MAX_PROGRESS = 200;
    private int progress = 0;

    private final SimpleContainer inventory = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            super.setChanged();
            ResearchTableBlockEntity.this.setChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return switch (slot) {
                case SAMPLE -> stack.is(Items.COPPER_INGOT) || stack.is(Items.RAW_COPPER);
                case TOOL -> stack.is(ModItems.TEST_PROBES.get());
                default -> false;
            };
        }
    };

    public final ContainerData dataAccess = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> MAX_PROGRESS;
                case 2 -> validInputs() ? 1 : 0;
                case 3 -> level instanceof ServerLevel serverLevel
                        && WorldResearchData.get(serverLevel).hasConductivity() ? 1 : 0;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {
            if (index == 0) progress = value;
        }
        @Override public int getCount() { return 4; }
    };

    public ResearchTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.RESEARCH_TABLE_ENTITY.get(), pos, state);
    }

    public SimpleContainer getItemHandler() { return inventory; }

    private boolean validInputs() {
        return (inventory.getItem(SAMPLE).is(Items.COPPER_INGOT)
                || inventory.getItem(SAMPLE).is(Items.RAW_COPPER))
                && inventory.getItem(TOOL).is(ModItems.TEST_PROBES.get());
    }

    private boolean canOutput(ItemStack result) {
        ItemStack current = inventory.getItem(OUTPUT);
        return current.isEmpty() || (ItemStack.isSameItemSameComponents(current, result)
                && current.getCount() + result.getCount() <= current.getMaxStackSize());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ResearchTableBlockEntity entity) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        // After the first discovery, the analyzer works as a research-gated wire fabricator.
        boolean researched = WorldResearchData.get(serverLevel).hasConductivity();
        ItemStack result = researched ? new ItemStack(ModItems.COPPER_WIRE.get(), 2)
                : new ItemStack(ModItems.RESEARCH_DATA.get());
        if (!entity.validInputs() || !entity.canOutput(result)) {
            if (entity.progress != 0) {
                entity.progress = 0;
                entity.setChanged();
            }
            return;
        }
        entity.progress++;
        if (entity.progress >= MAX_PROGRESS) {
            entity.finish(serverLevel, result, researched);
            entity.progress = 0;
        }
        entity.setChanged();
    }

    private void finish(ServerLevel level, ItemStack result, boolean researched) {
        if (!validInputs() || !canOutput(result)) return;
        inventory.getItem(SAMPLE).shrink(1);
        inventory.getItem(TOOL).hurtAndBreak(1, level, null, item -> {});
        ItemStack output = inventory.getItem(OUTPUT);
        if (output.isEmpty()) inventory.setItem(OUTPUT, result.copy());
        else {
            output.grow(result.getCount());
            inventory.setChanged();
        }
        if (!researched) {
            WorldResearchData.get(level).unlockConductivity();
        }
        setChanged();
    }

    @Override public Component getDisplayName() {
        return Component.translatable("gui.quantumindustries.research_table.title");
    }

    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new br.com.quantumindustries.menu.ResearchTableMenu(id, inv, this, dataAccess);
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("progress", progress);
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) output.store("slot_" + i, ItemStack.CODEC, stack);
        }
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        progress = input.getIntOr("progress", 0);
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            inventory.setItem(i, input.read("slot_" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY));
        }
    }

    public void dropContents() {
        if (level == null || level.isClientSide()) return;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.removeItemNoUpdate(i);
            if (!stack.isEmpty()) Block.popResource(level, worldPosition, stack);
        }
    }

    public int getProgress() { return progress; }
    public int getMaxProgress() { return MAX_PROGRESS; }
}
