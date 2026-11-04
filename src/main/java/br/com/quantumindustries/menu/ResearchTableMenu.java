package br.com.quantumindustries.menu;

import br.com.quantumindustries.block.entity.ResearchTableBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.core.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.IContainerFactory;

public class ResearchTableMenu extends AbstractContainerMenu implements IContainerFactory<ResearchTableMenu> {
    private final ContainerData data;
    private final ContainerLevelAccess levelAccess;

    public ResearchTableMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, resolveBlockEntity(inv, extraData), new SimpleContainerData(3));
    }

    public ResearchTableMenu(int containerId, Inventory inv, ResearchTableBlockEntity entity, ContainerData data) {
        super(ModMenuTypes.RESEARCH_TABLE_MENU.get(), containerId);
        this.data = data;
        this.levelAccess = entity == null
                ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(inv.player.level(), entity.getBlockPos());
        Container inventory = entity == null ? createUnavailableInventory() : entity.getItemHandler();

        addSlot(new Slot(inventory, 0, 21, 72));
        addSlot(new Slot(inventory, 1, 21, 108));
        addSlot(new Slot(inventory, 2, 66, 90));

        addDataSlots(data);
        layoutPlayerInventory(inv);
    }

    private static ResearchTableBlockEntity resolveBlockEntity(Inventory inventory, FriendlyByteBuf extraData) {
        if (extraData.readableBytes() < Long.BYTES) {
            return null;
        }
        BlockPos pos = extraData.readBlockPos();
        return inventory.player.level().getBlockEntity(pos) instanceof ResearchTableBlockEntity entity
                ? entity
                : null;
    }

    private static Container createUnavailableInventory() {
        return new SimpleContainer(3) {
            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                return false;
            }
        };
    }

    // Static factory method for MenuType (MenuSupplier)
    public static ResearchTableMenu createMenu(int windowId, Inventory playerInv, RegistryFriendlyByteBuf extraData) {
        return new ResearchTableMenu(windowId, playerInv, extraData);
    }

    @Override
    public ResearchTableMenu create(int windowId, Inventory playerInv, RegistryFriendlyByteBuf extraData) {
        return createMenu(windowId, playerInv, extraData);
    }

    public boolean isActive() {
        return this.data.get(0) > 0;
    }

    public int getProgress() {
        return this.data.get(0);
    }

    public int getMaxProgress() {
        return this.data.get(1);
    }

    public boolean hasToolAndSample() {
        return this.data.get(2) == 1;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            itemstack = current.copy();
            if (index < 3) {
                if (!this.moveItemStackTo(current, 3, 39, true)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(current, 0, 3, false)) {
                return ItemStack.EMPTY;
            }
            if (current.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.levelAccess, player, ModBlocks.RESEARCH_TABLE.get());
    }

    private void layoutPlayerInventory(Inventory inv) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 72 + col * 18, 162 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(inv, col, 72 + col * 18, 216));
        }
    }
}