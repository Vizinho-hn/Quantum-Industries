package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Optional;

public class ResearchTableBlockEntity extends BlockEntity implements MenuProvider {
    private static final int SLOT_SAMPLE = 0;
    private static final int SLOT_TOOL = 1;
    private static final int SLOT_OUTPUT = 2;
    private static final int CONTAINER_SIZE = 3;
    private static final int MAX_PROGRESS = 200;

    private final SimpleContainer itemHandler = createHandler();
    public final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> hasValidItems ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> maxProgress = value;
                case 2 -> hasValidItems = value == 1;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    private int progress = 0;
    private int maxProgress = MAX_PROGRESS;
    private boolean hasValidItems = false;

    public ResearchTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.RESEARCH_TABLE_ENTITY.get(), pos, state);
    }

    private SimpleContainer createHandler() {
        return new SimpleContainer(CONTAINER_SIZE) {
            @Override
            public void setItem(int slot, ItemStack stack) {
                super.setItem(slot, stack);
                setChanged();
                if (level != null && !level.isClientSide()) {
                    validateItems();
                }
            }

            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                return switch (slot) {
                    case SLOT_SAMPLE -> stack.is(Items.COPPER_INGOT) || stack.is(Items.RAW_COPPER);
                    case SLOT_TOOL -> stack.is(ModItems.TEST_PROBES.get());
                    case SLOT_OUTPUT -> false;
                    default -> false;
                };
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                return switch (stack) {
                    case ItemStack s when s.is(ModItems.TEST_PROBES.get()) || s.is(Items.COPPER_INGOT) || s.is(Items.RAW_COPPER) -> 1;
                    default -> super.getMaxStackSize(stack);
                };
            }
        };
    }

    public SimpleContainer getItemHandler() {
        return itemHandler;
    }

    public void validateItems() {
        ItemStack sample = itemHandler.getItem(SLOT_SAMPLE);
        ItemStack tool = itemHandler.getItem(SLOT_TOOL);

        boolean valid = (sample.is(Items.COPPER_INGOT) || sample.is(Items.RAW_COPPER))
                && tool.is(ModItems.TEST_PROBES.get());

        hasValidItems = valid;
        dataAccess.set(2, valid ? 1 : 0);

        if (!valid && progress > 0) {
            progress = 0;
            dataAccess.set(0, 0);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ResearchTableBlockEntity entity) {
        if (level.isClientSide()) return;

        entity.validateItems();

        if (entity.hasValidItems) {
            entity.progress++;
            entity.dataAccess.set(0, entity.progress);

            if (entity.progress >= entity.maxProgress) {
                entity.completeResearch(level);
                entity.progress = 0;
                entity.dataAccess.set(0, 0);
            }
            entity.setChanged();
        }
    }

    private void completeResearch(Level level) {
        if (!level.isClientSide() && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            ItemStack sample = itemHandler.getItem(SLOT_SAMPLE);
            ItemStack tool = itemHandler.getItem(SLOT_TOOL);

            if (!sample.isEmpty() && !tool.isEmpty()) {
                sample.shrink(1);
                tool.hurtAndBreak(1, serverLevel, null, item -> {});

                ItemStack output = new ItemStack(ModItems.RESEARCH_DATA.get());
                ItemStack existingOutput = itemHandler.getItem(SLOT_OUTPUT);
                if (existingOutput.isEmpty()) {
                    itemHandler.setItem(SLOT_OUTPUT, output);
                } else if (ItemStack.isSameItemSameComponents(existingOutput, output)) {
                    existingOutput.grow(1);
                }

                level.blockEvent(worldPosition, getBlockState().getBlock(), 1, 0);
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.quantumindustries.scientific_analyzer");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new br.com.quantumindustries.menu.ResearchTableMenu(containerId, inventory, this, dataAccess);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        CompoundTag tag = new CompoundTag();
        // TODO: Save items using data component system when API is clarified
        // For now, only save progress data (items are consumed during research)
        tag.putInt("progress", progress);
        tag.putInt("maxProgress", maxProgress);
        tag.putBoolean("hasValidItems", hasValidItems);

        output.store("data", CompoundTag.CODEC, tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        Optional<CompoundTag> tagOpt = input.read("data", CompoundTag.CODEC);
        if (tagOpt.isPresent()) {
            CompoundTag tag = tagOpt.get();
            progress = tag.getInt("progress").orElse(0);
            maxProgress = tag.getInt("maxProgress").orElse(MAX_PROGRESS);
            hasValidItems = tag.getBoolean("hasValidItems").orElse(false);
            dataAccess.set(0, progress);
            dataAccess.set(1, maxProgress);
            dataAccess.set(2, hasValidItems ? 1 : 0);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        dropContents();
    }

    private void dropContents() {
        if (level == null || level.isClientSide()) return;
        for (int i = 0; i < itemHandler.getContainerSize(); i++) {
            ItemStack stack = itemHandler.getItem(i);
            if (!stack.isEmpty()) {
                Block.popResource(level, worldPosition, stack);
                itemHandler.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public boolean hasValidItems() {
        return hasValidItems;
    }
}