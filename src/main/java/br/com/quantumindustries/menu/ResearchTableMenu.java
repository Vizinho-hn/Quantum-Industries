package br.com.quantumindustries.menu;

import br.com.quantumindustries.block.entity.ResearchTableBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.core.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.IContainerFactory;

public class ResearchTableMenu extends AbstractContainerMenu implements IContainerFactory<ResearchTableMenu> {
    private final ResearchTableBlockEntity blockEntity;
    private final ContainerData data;
    private final ContainerLevelAccess levelAccess;

    public ResearchTableMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, (ResearchTableBlockEntity) inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(3));
    }

    public ResearchTableMenu(int containerId, Inventory inv, ResearchTableBlockEntity entity, ContainerData data) {
        super(ModMenuTypes.RESEARCH_TABLE_MENU.get(), containerId);
        this.blockEntity = entity;
        this.data = data;
        this.levelAccess = ContainerLevelAccess.create(entity.getLevel(), entity.getBlockPos());

        // Slot 0: Amostra (X=24, Y=38)
        addSlot(new Slot(entity.getItemHandler(), 0, 24, 38));
        // Slot 1: Sonda / Ferramenta de Teste (X=24, Y=78)
        addSlot(new Slot(entity.getItemHandler(), 1, 24, 78));
        // Slot 2: Saída (X=134, Y=58) - central right
        addSlot(new Slot(entity.getItemHandler(), 2, 134, 58));

        addDataSlots(data);
        layoutPlayerInventory(inv);
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
                this.addSlot(new Slot(inv, col + row * 9 + 9, 48 + col * 18, 142 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(inv, col, 48 + col * 18, 200));
        }
    }
}