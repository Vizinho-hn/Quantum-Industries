package br.com.quantumindustries.recipe;

import br.com.quantumindustries.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Three copper ingots in one horizontal row => six wires, only after
 * completing BOTH discoveries in the world-shared research profile.
 *
 * Because matches() checks server SavedData, manually placing the ingredients
 * cannot bypass the tree unlock (unlike hiding a vanilla recipe-book entry).
 */
public final class CopperWireRecipe extends CustomRecipe {
    public static final CopperWireRecipe INSTANCE = new CopperWireRecipe();

    private CopperWireRecipe() {}

    static FoundationRecipe.Preview preview() {
        return new FoundationRecipe.Preview(result(), 3, java.util.List.of(
                new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.COPPER_INGOT),
                new ItemStack(Items.COPPER_INGOT)));
    }

    private static ItemStack result() {
        return new ItemStack(ModItems.COPPER_WIRE.get(), 6);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (!ResearchCraftingAccess.allows(level, "conductivity")
                || !ResearchCraftingAccess.allows(level, "copper_wire")) return false;
        if (input.width() != 3 || input.ingredientCount() != 3) return false;

        for (int row = 0; row < input.height(); row++) {
            boolean matches = true;
            for (int x = 0; x < 3; x++) {
                if (!input.getItem(x, row).is(Items.COPPER_INGOT)) {
                    matches = false;
                    break;
                }
            }
            if (matches) return true;
        }
        return false;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return result();
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return ModRecipes.COPPER_WIRE.get();
    }
}
