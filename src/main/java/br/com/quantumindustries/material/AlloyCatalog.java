package br.com.quantumindustries.material;

import br.com.quantumindustries.ModItems;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Catálogo canônico de ligas metalúrgicas e processos de fundição.
 * Separa ligas de Tier 1 (Forno de Ligas a Fole / Elétrica) e Tier 2 (Exclusivas da Alloy Smelter Elétrica).
 * Aceita tanto lingotes quanto pós minerais purificados (beneficiados no Britador de Mandíbulas).
 */
public final class AlloyCatalog {

    public record AlloyInfo(
            String id,
            String namePt,
            String nameEn,
            String formula,
            int tier,
            String descriptionPt
    ) {
        public String ingotId() { return id + "_ingot"; }
        public String nuggetId() { return id + "_nugget"; }
        public String sheetId() { return id + "_sheet"; }
    }

    public static final List<AlloyInfo> ALLOYS = List.of(
            new AlloyInfo("bronze", "Bronze", "Bronze", "Cu₃Sn", 1,
                    "Liga clássica de cobre e estanho com baixo atrito e alta resistência ao desgaste."),
            new AlloyInfo("brass", "Latão", "Brass", "Cu₃Zn", 1,
                    "Liga dúctil e resistente à corrosão usada em instrumentos, válvulas e mancais."),
            new AlloyInfo("invar", "Invar", "Invar", "Fe₂Ni", 2,
                    "Liga ferro-níquel de baixíssima dilatação térmica para câmaras e termostatos."),
            new AlloyInfo("electrum", "Electrum", "Electrum", "AuAg", 2,
                    "Liga nobre ouro-prata de altíssima condutividade elétrica e baixa perda resistiva."),
            new AlloyInfo("constantan", "Constantan", "Constantan", "CuNi", 2,
                    "Liga termoelétrica cobre-níquel com resistividade constante sob variação de temperatura."),
            new AlloyInfo("steel", "Aço Carbono", "Carbon Steel", "Fe-C", 2,
                    "Liga estrutural de alta resistência mecânica obtida por refino de ferro com carbono.")
    );

    public record Recipe(
            String alloyId,
            int tier,
            List<Supplier<Item>> inputsA,
            int countA,
            List<Supplier<Item>> inputsB,
            int countB,
            Supplier<Item> outputIngot,
            int outputCount,
            int workTicks,
            int totalFe
    ) {
        public Recipe(
                String alloyId,
                int tier,
                Supplier<Item> inputA,
                int countA,
                Supplier<Item> inputB,
                int countB,
                Supplier<Item> outputIngot,
                int outputCount,
                int workTicks,
                int totalFe
        ) {
            this(alloyId, tier, List.of(inputA), countA, List.of(inputB), countB, outputIngot, outputCount, workTicks, totalFe);
        }

        public boolean isInputA(ItemStack stack) {
            if (stack.isEmpty()) return false;
            for (var s : inputsA) if (stack.is(s.get())) return true;
            return false;
        }

        public boolean isInputB(ItemStack stack) {
            if (stack.isEmpty()) return false;
            for (var s : inputsB) if (stack.is(s.get())) return true;
            return false;
        }

        public boolean matches(ItemStack stackA, ItemStack stackB) {
            return (isInputA(stackA) && stackA.getCount() >= countA && isInputB(stackB) && stackB.getCount() >= countB)
                || (isInputB(stackA) && stackA.getCount() >= countB && isInputA(stackB) && stackB.getCount() >= countA);
        }

        public boolean acceptsIngredient(ItemStack stack) {
            return isInputA(stack) || isInputB(stack);
        }

        public void consume(ItemStack stackA, ItemStack stackB) {
            if (isInputA(stackA) && isInputB(stackB)) {
                stackA.shrink(countA);
                stackB.shrink(countB);
            } else if (isInputB(stackA) && isInputA(stackB)) {
                stackA.shrink(countB);
                stackB.shrink(countA);
            }
        }

        public ItemStack assemble() {
            return new ItemStack(outputIngot.get(), outputCount);
        }
    }

    public static final List<Recipe> RECIPES = List.of(
            // Tier 1: Forno de Ligas a Fole & Alloy Smelter Elétrica (aceita lingotes ou pós beneficiados)
            new Recipe("bronze", 1,
                    List.of(() -> Items.COPPER_INGOT, () -> ModItems.METAL_DUSTS.get("copper").get()), 3,
                    List.of(() -> ModItems.INGOTS.get("tin").get(), () -> ModItems.METAL_DUSTS.get("tin").get()), 1,
                    () -> ModItems.ALLOY_INGOTS.get("bronze").get(), 4,
                    80, 80),
            new Recipe("brass", 1,
                    List.of(() -> Items.COPPER_INGOT, () -> ModItems.METAL_DUSTS.get("copper").get()), 3,
                    List.of(() -> ModItems.INGOTS.get("zinc").get(), () -> ModItems.METAL_DUSTS.get("zinc").get()), 1,
                    () -> ModItems.ALLOY_INGOTS.get("brass").get(), 4,
                    80, 80),
            // Tier 2: Alloy Smelter Elétrica (Era II+)
            new Recipe("invar", 2,
                    List.of(() -> Items.IRON_INGOT, () -> ModItems.METAL_DUSTS.get("iron").get()), 2,
                    List.of(() -> ModItems.INGOTS.get("nickel").get(), () -> ModItems.METAL_DUSTS.get("nickel").get()), 1,
                    () -> ModItems.ALLOY_INGOTS.get("invar").get(), 3,
                    100, 120),
            new Recipe("electrum", 2,
                    List.of(() -> Items.GOLD_INGOT, () -> ModItems.METAL_DUSTS.get("gold").get()), 1,
                    List.of(() -> ModItems.INGOTS.get("silver").get(), () -> ModItems.METAL_DUSTS.get("silver").get()), 1,
                    () -> ModItems.ALLOY_INGOTS.get("electrum").get(), 2,
                    100, 120),
            new Recipe("constantan", 2,
                    List.of(() -> Items.COPPER_INGOT, () -> ModItems.METAL_DUSTS.get("copper").get()), 1,
                    List.of(() -> ModItems.INGOTS.get("nickel").get(), () -> ModItems.METAL_DUSTS.get("nickel").get()), 1,
                    () -> ModItems.ALLOY_INGOTS.get("constantan").get(), 2,
                    100, 120),
            new Recipe("steel", 2,
                    List.of(() -> Items.IRON_INGOT, () -> ModItems.METAL_DUSTS.get("iron").get()), 1,
                    List.of(() -> ModItems.CHEMICAL_PRODUCTS.get("graphite").get(), () -> Items.COAL), 2,
                    () -> ModItems.ALLOY_INGOTS.get("steel").get(), 1,
                    120, 160)
    );

    private AlloyCatalog() {}

    public static Recipe findMatch(ItemStack stackA, ItemStack stackB, int maxTier) {
        if (stackA.isEmpty() || stackB.isEmpty()) return null;
        for (Recipe recipe : RECIPES) {
            if (recipe.tier() <= maxTier && recipe.matches(stackA, stackB)) {
                return recipe;
            }
        }
        return null;
    }

    public static boolean isValidIngredient(ItemStack stack, int maxTier) {
        if (stack.isEmpty()) return false;
        for (Recipe recipe : RECIPES) {
            if (recipe.tier() <= maxTier && recipe.acceptsIngredient(stack)) {
                return true;
            }
        }
        return false;
    }

    public static boolean canPair(ItemStack first, ItemStack second, int maxTier) {
        if (first.isEmpty() || second.isEmpty()) return false;
        for (Recipe recipe : RECIPES) {
            if (recipe.tier() <= maxTier) {
                boolean ab = recipe.isInputA(first) && recipe.isInputB(second);
                boolean ba = recipe.isInputB(first) && recipe.isInputA(second);
                if (ab || ba) return true;
            }
        }
        return false;
    }
}
