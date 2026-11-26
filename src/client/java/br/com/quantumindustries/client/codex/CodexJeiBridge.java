package br.com.quantumindustries.client.codex;

import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;

/** Optional runtime hook so QuantumGuidebookScreen can open JEI recipes without hard JEI imports. */
public final class CodexJeiBridge {
    private static volatile Predicate<ItemStack> recipeOpener;

    private CodexJeiBridge() {}

    public static void install(Predicate<ItemStack> opener) {
        recipeOpener = opener;
    }

    public static void clear() {
        recipeOpener = null;
    }

    public static boolean showRecipe(ItemStack stack) {
        Predicate<ItemStack> opener = recipeOpener;
        if (opener == null || stack == null || stack.isEmpty()) return false;
        return opener.test(stack);
    }
}
