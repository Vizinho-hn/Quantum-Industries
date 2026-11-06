package br.com.quantumindustries.fluid;

import br.com.quantumindustries.core.ModBlocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Expose the modern 26.1 fluid capability to other mods and vanilla bucket interactions. */
public final class ModFluidCapabilities {
    private ModFluidCapabilities() {}
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlocks.FLUID_PIPE_ENTITY.get(),
                (be, side) -> be.tank());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlocks.SETTLING_VAT_ENTITY.get(),
                (be, side) -> be.tank());
    }
}
