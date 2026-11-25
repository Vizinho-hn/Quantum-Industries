package br.com.quantumindustries.logistics;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStackResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** An automation view of a machine slot. Simulations never notify the machine or change its progress. */
public final class MachineItemSlot extends ItemStackResourceHandler {
    private final BlockEntity machine;
    private final Supplier<ItemStack> read;
    private final Consumer<ItemStack> write;
    private final Predicate<ItemStack> accepts;
    private final IntSupplier capacity;
    private final Consumer<ItemStack> committed;

    public MachineItemSlot(BlockEntity machine, Supplier<ItemStack> read, Consumer<ItemStack> write,
            Predicate<ItemStack> accepts, int capacity, Consumer<ItemStack> committed) {
        this(machine, read, write, accepts, () -> capacity, committed);
    }

    public MachineItemSlot(BlockEntity machine, Supplier<ItemStack> read, Consumer<ItemStack> write,
            Predicate<ItemStack> accepts, IntSupplier capacity, Consumer<ItemStack> committed) {
        this.machine = machine;
        this.read = read;
        this.write = write;
        this.accepts = accepts;
        this.capacity = capacity;
        this.committed = committed;
    }

    @Override protected ItemStack getStack() { return read.get(); }
    @Override protected void setStack(ItemStack stack) { write.accept(stack); }

    @Override
    protected boolean isValid(ItemResource resource) {
        return accepts != null && available() && accepts.test(resource.toStack());
    }

    @Override
    protected int getCapacity(ItemResource resource) {
        int limit = capacity.getAsInt();
        return resource.isEmpty() ? limit : Math.min(limit, resource.getMaxStackSize());
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        return accepts == null && available() ? super.extract(index, resource, amount, transaction) : 0;
    }

    @Override
    protected void onRootCommit(ItemStack before) {
        if (available()) committed.accept(before);
    }

    private boolean available() {
        return machine.getLevel() instanceof ServerLevel && !machine.isRemoved();
    }
}
