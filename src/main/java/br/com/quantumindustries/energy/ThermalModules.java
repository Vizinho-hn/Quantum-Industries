package br.com.quantumindustries.energy;

import br.com.quantumindustries.ModItems;
import net.minecraft.world.item.ItemStack;

public final class ThermalModules {
    private ThermalModules(){}
    public static boolean accepts(ItemStack stack){
        return stack.is(ModItems.COPPER_HEATSINK.get())||stack.is(ModItems.ALUMINUM_HEATSINK.get())||stack.is(ModItems.THERMAL_FAN.get());
    }
    public static boolean fan(ItemStack stack){return stack.is(ModItems.THERMAL_FAN.get());}
    public static int conductance(ItemStack stack){
        if(stack.is(ModItems.COPPER_HEATSINK.get()))return ThermalExchange.COPPER;
        if(stack.is(ModItems.ALUMINUM_HEATSINK.get()))return ThermalExchange.ALUMINUM;
        return fan(stack)?ThermalExchange.FAN_PASSIVE:0;
    }
}
