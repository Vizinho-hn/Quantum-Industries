package br.com.quantumindustries.material;

import br.com.quantumindustries.ModItems;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Catálogo de cominuição e beneficiamento mineral do Britador de Mandíbulas (Jaw Crusher).
 * Converte 1 minério bruto em 2 pós purificados (duplicação de rendimento) e realiza
 * moagem industrial de trigo (2x farinha) e carvão (2x pó de grafite).
 */
public final class CrushingCatalog {

    public record DustInfo(
            String id,
            String namePt,
            String nameEn,
            String formula,
            int color,
            Supplier<Item> rawInput,
            Supplier<Item> smeltedIngot
    ) {
        public String dustId() {
            return id + "_dust";
        }
    }

    public static final List<DustInfo> METAL_DUSTS = List.of(
            new DustInfo("copper", "Cobre", "Copper", "Cu", 0xFFD77A42,
                    () -> Items.RAW_COPPER, () -> Items.COPPER_INGOT),
            new DustInfo("iron", "Ferro", "Iron", "Fe", 0xFFB8BFC0,
                    () -> Items.RAW_IRON, () -> Items.IRON_INGOT),
            new DustInfo("gold", "Ouro", "Gold", "Au", 0xFFECC34C,
                    () -> Items.RAW_GOLD, () -> Items.GOLD_INGOT),
            new DustInfo("tin", "Estanho", "Tin", "Sn", 0xFFD0DCE5,
                    () -> ModItems.RAW_MATERIALS.get("tin").get(), () -> ModItems.INGOTS.get("tin").get()),
            new DustInfo("zinc", "Zinco", "Zinc", "Zn", 0xFFA8B8B0,
                    () -> ModItems.RAW_MATERIALS.get("zinc").get(), () -> ModItems.INGOTS.get("zinc").get()),
            new DustInfo("lead", "Chumbo", "Lead", "Pb", 0xFF5C6274,
                    () -> ModItems.RAW_MATERIALS.get("lead").get(), () -> ModItems.INGOTS.get("lead").get()),
            new DustInfo("nickel", "Níquel", "Nickel", "Ni", 0xFFC2BA9B,
                    () -> ModItems.RAW_MATERIALS.get("nickel").get(), () -> ModItems.INGOTS.get("nickel").get()),
            new DustInfo("silver", "Prata", "Silver", "Ag", 0xFFDCE6EE,
                    () -> ModItems.RAW_MATERIALS.get("silver").get(), () -> ModItems.INGOTS.get("silver").get())
    );

    private CrushingCatalog() {}

    public static ItemStack resultFor(ItemStack input) {
        if (input.isEmpty()) return ItemStack.EMPTY;
        for (var dust : METAL_DUSTS) {
            if (input.is(dust.rawInput().get())) {
                var item = ModItems.METAL_DUSTS.get(dust.id());
                if (item != null) return new ItemStack(item.get(), 2);
            }
        }
        if (input.is(Items.WHEAT)) {
            return new ItemStack(ModItems.FLOUR.get(), 2);
        }
        if (input.is(Items.COAL) && ModItems.CHEMICAL_PRODUCTS.containsKey("graphite")) {
            return new ItemStack(ModItems.CHEMICAL_PRODUCTS.get("graphite").get(), 2);
        }
        return ItemStack.EMPTY;
    }

    public static boolean isValidInput(ItemStack input) {
        return !resultFor(input).isEmpty();
    }
}
