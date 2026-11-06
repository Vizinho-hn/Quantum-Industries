package br.com.quantumindustries.material;

import br.com.quantumindustries.ModItems;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class FormingMaterials {
    public record Metal(String key, Item ingot, Supplier<? extends Item> sheet) {}

    private FormingMaterials() {}

    public static Metal find(ItemStack stack) {
        if (!MaterialProcesses.supports(br.com.quantumindustries.assay.AssayCatalog.material(stack),
                MaterialProcesses.Process.SHEET)) return null;
        for (Metal metal : Metals.ALL) if (stack.is(metal.ingot())) return metal;
        return null;
    }

    private static final class Metals {
        private static final List<Metal> ALL = List.of(
                new Metal("iron", Items.IRON_INGOT, ModItems.IRON_SHEET),
                new Metal("copper", Items.COPPER_INGOT, ModItems.COPPER_SHEET));
    }
}
