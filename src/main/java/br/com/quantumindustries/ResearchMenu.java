package br.com.quantumindustries;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

public class ResearchMenu extends AbstractContainerMenu {
    private final Player owner;
    private final ContainerLevelAccess access;
    private final ContainerData data = new SimpleContainerData(4);

    public ResearchMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public ResearchMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(ModItems.RESEARCH_MENU.get(), id);
        this.owner = inventory.player;
        this.access = access;
        addStandardInventorySlots(inventory, 14, 153);
        addDataSlots(data);
        if (!owner.level().isClientSide()) {
            CompoundTag saved = saved();
            for (int i = 0; i < 4; i++) data.set(i, saved.getIntOr("quantumResearch" + i, 0));
        }
    }

    private CompoundTag saved() {
        CompoundTag root = owner.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG))
            root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return root.getCompoundOrEmpty(Player.PERSISTED_NBT_TAG);
    }

    private void save() {
        for (int i = 0; i < 4; i++) saved().putInt("quantumResearch" + i, data.get(i));
    }

    public int stage() { return data.get(0); }
    public int remaining() { return data.get(1); }
    public int operation() { return data.get(2); }

    @Override
    public void broadcastChanges() {
        if (!owner.level().isClientSide() && stillValid(owner) && remaining() > 0) {
            data.set(1, remaining() - 1);
            if (remaining() == 0) {
                data.set(0, Math.max(stage(), operation()));
                data.set(2, 0);
            }
            save();
        }
        super.broadcastChanges();
    }

    private boolean consume(Item item) {
        Inventory inventory = owner.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) {
                stack.shrink(1);
                inventory.setChanged();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean clickMenuButton(Player player, int action) {
        if (player != owner || player.level().isClientSide() || !stillValid(player) || remaining() > 0)
            return false;
        if (action == 0 && stage() == 0 && consume(Items.COPPER_INGOT)) {
            data.set(2, 1);
            data.set(1, 100);
        } else if (action == 1 && stage() == 1 && consume(Items.COPPER_INGOT)) {
            data.set(2, 2);
            data.set(1, 600);
        } else if (action == 2 && stage() == 2 && consume(Items.COPPER_INGOT)) {
            ItemStack result = new ItemStack(ModItems.COPPER_CABLE.get(), 4);
            if (!owner.getInventory().add(result)) owner.drop(result, false);
        } else if (action == 3 && stage() >= 1 && consume(Items.PAPER)) {
            ItemStack report = new ItemStack(ModItems.ANALYSIS_REPORT.get());
            if (!owner.getInventory().add(report)) owner.drop(report, false);
        } else {
            return false;
        }
        save();
        broadcastChanges();
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModItems.RESEARCH_TABLE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < 27) {
            if (!moveItemStackTo(stack, 27, 36, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 27, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }
}

