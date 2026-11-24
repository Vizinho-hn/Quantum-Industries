package br.com.quantumindustries.fluid;

import br.com.quantumindustries.block.entity.SteamBoilerBlockEntity;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** The boiler's external water port is input-only, even for cached handlers. */
public record BoilerWaterInlet(SteamBoilerBlockEntity boiler, Direction side) implements ResourceHandler<FluidResource> {
    @Override public int size(){return boiler.tank().size();}
    @Override public FluidResource getResource(int slot){return boiler.tank().getResource(slot);}
    @Override public long getAmountAsLong(int slot){return boiler.tank().getAmountAsLong(slot);}
    @Override public long getCapacityAsLong(int slot,FluidResource resource){return boiler.tank().getCapacityAsLong(slot,resource);}
    @Override public boolean isValid(int slot,FluidResource resource){return side!=boiler.outlet() && boiler.tank().isValid(slot,resource);}
    @Override public int insert(int slot,FluidResource resource,int amount,TransactionContext tx){
        return isValid(slot,resource)?boiler.tank().insert(slot,resource,amount,tx):0;
    }
    @Override public int extract(int slot,FluidResource resource,int amount,TransactionContext tx){return 0;}
}
