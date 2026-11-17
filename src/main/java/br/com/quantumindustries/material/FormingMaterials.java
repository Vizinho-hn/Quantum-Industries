package br.com.quantumindustries.material;

import br.com.quantumindustries.ModItems;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class FormingMaterials {
    public record Metal(String key, Supplier<Item> ingot, Supplier<? extends Item> sheet) {}

    private FormingMaterials() {}

    public static Metal find(ItemStack stack) {
        for (Metal metal : Metals.ALL) if (stack.is(metal.ingot().get())) return metal;
        if (stack.is(ModItems.HEATED_IRON_INGOT.get())) return Metals.ALL.get(0);
        if (stack.is(ModItems.HEATED_COPPER_INGOT.get())) return Metals.ALL.get(1);
        if (stack.is(ModItems.HEATED_BRONZE_INGOT.get())) return Metals.ALL.get(6);
        if (stack.is(ModItems.HEATED_BRASS_INGOT.get())) return Metals.ALL.get(7);
        if (stack.is(ModItems.HEATED_STEEL_INGOT.get())) return Metals.ALL.get(11);
        return null;
    }

    private static final class Metals {
        private static final List<Metal> ALL = List.of(
                new Metal("iron", () -> Items.IRON_INGOT, ModItems.IRON_SHEET),
                new Metal("copper", () -> Items.COPPER_INGOT, ModItems.COPPER_SHEET),
                new Metal("gold", () -> Items.GOLD_INGOT, ModItems.GOLD_SHEET),
                new Metal("lead", () -> ModItems.INGOTS.get("lead").get(), ModItems.LEAD_SHEET),
                new Metal("tin", () -> ModItems.INGOTS.get("tin").get(), ModItems.TIN_SHEET),
                new Metal("zinc", () -> ModItems.INGOTS.get("zinc").get(), ModItems.ZINC_SHEET),
                new Metal("bronze", () -> ModItems.ALLOY_INGOTS.get("bronze").get(), () -> ModItems.ALLOY_SHEETS.get("bronze").get()),
                new Metal("brass", () -> ModItems.ALLOY_INGOTS.get("brass").get(), () -> ModItems.ALLOY_SHEETS.get("brass").get()),
                new Metal("invar", () -> ModItems.ALLOY_INGOTS.get("invar").get(), () -> ModItems.ALLOY_SHEETS.get("invar").get()),
                new Metal("electrum", () -> ModItems.ALLOY_INGOTS.get("electrum").get(), () -> ModItems.ALLOY_SHEETS.get("electrum").get()),
                new Metal("constantan", () -> ModItems.ALLOY_INGOTS.get("constantan").get(), () -> ModItems.ALLOY_SHEETS.get("constantan").get()),
                new Metal("steel", () -> ModItems.ALLOY_INGOTS.get("steel").get(), () -> ModItems.ALLOY_SHEETS.get("steel").get()));
    }
}
