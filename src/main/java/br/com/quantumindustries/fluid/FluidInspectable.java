package br.com.quantumindustries.fluid;

import net.minecraft.network.chat.Component;

/** Client-visible fluid diagnostics without misrepresenting mB as FE. */
public interface FluidInspectable {
    int storedFluid();
    int fluidCapacity();
    Component fluidTitle();
    Component fluidDetail();
}
