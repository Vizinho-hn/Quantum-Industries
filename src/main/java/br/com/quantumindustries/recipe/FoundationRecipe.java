package br.com.quantumindustries.recipe;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.research.ResearchCatalog;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

public final class FoundationRecipe extends CustomRecipe {
    public static final FoundationRecipe INSTANCE = new FoundationRecipe();

    private record Pattern(String study, Supplier<? extends ItemLike> output, int count, int width, ItemLike... cells) {
        boolean fits(CraftingInput grid) {
            if (grid.width() != width || grid.height() != cells.length / width) return false;
            for (int i = 0; i < cells.length; i++) {
                var stack = grid.getItem(i % width, i / width);
                if (cells[i] == null ? !stack.isEmpty() : !stack.is(cells[i].asItem())) return false;
            }
            return true;
        }
    }

    public record Preview(ItemStack output, int width, List<ItemStack> ingredients) {}

    public static List<Preview> previews(int research) {
        // All crafting-table previews use the same grid in the research notebook.
        String key = ResearchCatalog.STUDIES.get(research).key();
        if (key.equals("copper_wire")) return List.of(CopperWireRecipe.preview());
        return patterns().stream().filter(pattern -> pattern.study().equals(key)).map(pattern -> {
            var ingredients = java.util.Arrays.stream(pattern.cells())
                    .map(item -> item == null ? ItemStack.EMPTY : new ItemStack(item)).toList();
            return new Preview(new ItemStack(pattern.output().get(), pattern.count()), pattern.width(), ingredients);
        }).toList();
    }

    private static List<Pattern> patterns() {
        var iron = Items.IRON_INGOT;
        var nugget = Items.IRON_NUGGET;
        var stick = Items.STICK;
        var plank = Items.OAK_PLANKS;
        var paper = Items.PAPER;
        var wire = ModItems.COPPER_WIRE.get();
        var shaft = ModItems.MECHANICAL_SHAFT.get();
        var gear = ModItems.IRON_GEAR.get();
        var sheet = ModItems.IRON_SHEET.get();
        var coil = ModItems.COPPER_COIL.get();
        var insulated = ModItems.INSULATED_COPPER_WIRE.get();
        return List.of(
            new Pattern("manual_forming", ModItems.MANUAL_HAMMER, 1, 3, nugget,nugget,nugget, null,stick,null, null,stick,null),
            new Pattern("manual_forming", ModItems.FORMING_BENCH, 1, 3, iron,iron,iron, plank,null,plank, plank,null,plank),
            new Pattern("basic_mechanics", ModItems.IRON_GEAR, 1, 3, null,nugget,null, nugget,iron,nugget, null,nugget,null),
            new Pattern("mechanical_transmission", ModItems.MECHANICAL_SHAFT, 2, 1, iron,iron),
            new Pattern("mechanical_transmission", ModItems.HAND_CRANK, 1, 2, shaft,gear, null,stick),
            new Pattern("electrical_insulation", ModItems.INSULATED_COPPER_WIRE, 2, 2, wire,wire, Items.LEATHER,paper),
            new Pattern("copper_winding", ModItems.COPPER_COIL, 1, 3, wire,stick,wire, wire,paper,wire),
            new Pattern("electromechanical_structure", ModItems.ELECTROMECHANICAL_FRAME, 1, 3, plank,ModItems.IRON_SHEET.get(),plank,
                null,shaft,null, plank,ModItems.IRON_SHEET.get(),plank),
            new Pattern("electromechanical_structure", ModItems.PRIMITIVE_MECHANICAL_CORE, 1, 3,
                sheet, gear, sheet,
                nugget, ModItems.ELECTROMECHANICAL_FRAME.get(), nugget,
                null, shaft, null),
            new Pattern("manual_dynamo_design", ModItems.MANUAL_DYNAMO, 1, 3,
                coil, gear, coil,
                sheet, ModItems.PRIMITIVE_MECHANICAL_CORE.get(), sheet,
                insulated, shaft, insulated)
        );
    }

    @Override public boolean matches(CraftingInput grid, Level level) {
        return patterns().stream().anyMatch(pattern -> pattern.fits(grid)
                && ResearchCraftingAccess.allows(level, pattern.study()));
    }

    @Override public ItemStack assemble(CraftingInput grid) {
        return patterns().stream().filter(pattern -> pattern.fits(grid)).findFirst()
                .map(pattern -> new ItemStack(pattern.output().get(), pattern.count())).orElse(ItemStack.EMPTY);
    }

    @Override public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return ModRecipes.FOUNDATIONS.get();
    }
}
