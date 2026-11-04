package br.com.quantumindustries.recipe;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.research.WorldResearchData;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

public final class FoundationRecipe extends CustomRecipe {
    public static final FoundationRecipe INSTANCE = new FoundationRecipe();

    private record Pattern(int study, Supplier<? extends ItemLike> output, int count, int width, ItemLike... cells) {
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
        if (research == 1) return List.of(CopperWireRecipe.preview());
        return patterns().stream().filter(pattern -> pattern.study() == research).map(pattern -> {
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
        return List.of(
            new Pattern(4, ModItems.MANUAL_HAMMER, 1, 3, nugget,nugget,nugget, null,stick,null, null,stick,null),
            new Pattern(4, ModItems.FORMING_BENCH, 1, 3, iron,iron,iron, plank,null,plank, plank,null,plank),
            new Pattern(7, ModItems.IRON_GEAR, 1, 3, null,nugget,null, nugget,iron,nugget, null,nugget,null),
            new Pattern(8, ModItems.MECHANICAL_SHAFT, 2, 1, iron,iron),
            new Pattern(8, ModItems.HAND_CRANK, 1, 2, shaft,gear, null,stick),
            new Pattern(6, ModItems.INSULATED_COPPER_WIRE, 2, 2, wire,wire, Items.LEATHER,paper),
            new Pattern(10, ModItems.COPPER_COIL, 1, 3, wire,stick,wire, wire,paper,wire),
            new Pattern(11, ModItems.ELECTROMECHANICAL_FRAME, 1, 3, plank,ModItems.IRON_SHEET.get(),plank,
                null,shaft,null, plank,ModItems.IRON_SHEET.get(),plank),
            new Pattern(13, ModItems.MANUAL_DYNAMO, 1, 3, null,ModItems.COPPER_COIL.get(),null,
                gear,ModItems.ELECTROMECHANICAL_FRAME.get(),ModItems.HAND_CRANK.get(),
                ModItems.INSULATED_COPPER_WIRE.get(),shaft,null)
        );
    }

    @Override public boolean matches(CraftingInput grid, Level level) {
        var player = CopperWireRecipe.crafter();
        if (!(level instanceof ServerLevel server) || player == null) return false;
        var data = WorldResearchData.get(server);
        return patterns().stream().anyMatch(pattern -> pattern.fits(grid) && data.knows(player, pattern.study()));
    }

    @Override public ItemStack assemble(CraftingInput grid) {
        return patterns().stream().filter(pattern -> pattern.fits(grid)).findFirst()
                .map(pattern -> new ItemStack(pattern.output().get(), pattern.count())).orElse(ItemStack.EMPTY);
    }

    @Override public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return ModRecipes.FOUNDATIONS.get();
    }
}
