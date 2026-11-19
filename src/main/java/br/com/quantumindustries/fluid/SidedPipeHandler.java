package br.com.quantumindustries.fluid;

import br.com.quantumindustries.logistics.ConnectionModes;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Reads the current port configuration so cached capabilities cannot bypass a later change. */
public record SidedPipeHandler(ResourceHandler<FluidResource> tank, ConnectionModes modes, Direction side)
        implements ResourceHandler<FluidResource> {
    @Override public int size() { return tank.size(); }
    @Override public FluidResource getResource(int slot) { return tank.getResource(slot); }
    @Override public long getAmountAsLong(int slot) { return tank.getAmountAsLong(slot); }
    @Override public long getCapacityAsLong(int slot, FluidResource resource) { return tank.getCapacityAsLong(slot, resource); }
    @Override public boolean isValid(int slot, FluidResource resource) { return modes.get(side).allowsInput() && tank.isValid(slot, resource); }
    @Override public int insert(int slot, FluidResource resource, int amount, TransactionContext transaction) {
        return modes.get(side).allowsInput() ? tank.insert(slot, resource, amount, transaction) : 0;
    }
    @Override public int extract(int slot, FluidResource resource, int amount, TransactionContext transaction) {
        return modes.get(side).allowsOutput() ? tank.extract(slot, resource, amount, transaction) : 0;
    }
}
