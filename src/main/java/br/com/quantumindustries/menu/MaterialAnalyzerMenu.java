package br.com.quantumindustries.menu;

import br.com.quantumindustries.core.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.*;
import net.neoforged.neoforge.network.IContainerFactory;

public class MaterialAnalyzerMenu extends AbstractContainerMenu implements IContainerFactory<MaterialAnalyzerMenu> {
    private int progress = 0;
    private int maxProgress = 100;

    private static final int SLOT_SAMPLE = 0;
    private static final int SLOT_TOOL = 1;
    private static final int SLOT_OUTPUT = 2;
    private static final int CONTAINER_SIZE = 3;

    private final ContainerData data;
    private final SimpleContainer itemHandler;

    public MaterialAnalyzerMenu(int containerId, Inventory inventory) {
        super(ModMenuTypes.MATERIAL_ANALYZER_MENU.get(), containerId);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> MaterialAnalyzerMenu.this.progress;
                    case 1 -> MaterialAnalyzerMenu.this.maxProgress;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> MaterialAnalyzerMenu.this.progress = value;
                    case 1 -> MaterialAnalyzerMenu.this.maxProgress = value;
                }
            }

            @Override
            public int getCount() { return 2; }
        };
        this.itemHandler = new SimpleContainer(CONTAINER_SIZE) {
            @Override
            public void setItem(int slot, ItemStack stack) {
                super.setItem(slot, stack);
            }

            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                return switch (slot) {
                    case SLOT_SAMPLE -> true;
                    case SLOT_TOOL -> stack.is(Items.STICK);
                    case SLOT_OUTPUT -> false;
                    default -> false;
                };
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                return 1;
            }
        };

        addSlot(new Slot(itemHandler, SLOT_SAMPLE, 36, 36) {
            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        });

        addSlot(new Slot(itemHandler, SLOT_TOOL, 116, 36) {
            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        });

        addSlot(new Slot(itemHandler, SLOT_OUTPUT, 196, 36) {
            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        });

        addDataSlots(data);
        layoutPlayerInventory(inventory);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            itemstack = current.copy();
            if (index == SLOT_OUTPUT) {
                if (!this.moveItemStackTo(current, 3, 39, false)) return ItemStack.EMPTY;
            } else if (index < 3) {
                return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(current, 0, 3, true)) return ItemStack.EMPTY;
            }
            if (current.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    private void layoutPlayerInventory(Inventory inv) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                addSlot(new Slot(inv, col + row * 9 + 9, 72 + col * 18, 162 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            addSlot(new Slot(inv, col, 72 + col * 18, 216));
        }
    }

    public static MaterialAnalyzerMenu createMenu(int windowId, Inventory playerInv, RegistryFriendlyByteBuf extraData) {
        return new MaterialAnalyzerMenu(windowId, playerInv);
    }

    @Override
    public MaterialAnalyzerMenu create(int windowId, Inventory playerInv, RegistryFriendlyByteBuf extraData) {
        return createMenu(windowId, playerInv, extraData);
    }
}