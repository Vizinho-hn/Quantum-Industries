package br.com.quantumindustries.recipe;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.research.ResearchCatalog;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredItem;

public final class FoundationRecipe extends ShapedRecipe {
    private static final Codec<String> RESULT_CODEC = Codec.STRING.validate(id ->
            Patterns.BY_RESULT.containsKey(id) ? DataResult.success(id)
                    : DataResult.error(() -> "Unknown Quantum Industries craft: " + id));

    static final RecipeSerializer<ShapedRecipe> SERIALIZER = new RecipeSerializer<>(
            RESULT_CODEC.fieldOf("craft").xmap(FoundationRecipe::new, FoundationRecipe::craftId),
            StreamCodec.<RegistryFriendlyByteBuf, ShapedRecipe>of(
                    (buf, recipe) -> ByteBufCodecs.STRING_UTF8.encode(buf, craftId(recipe)),
                    buf -> new FoundationRecipe(ByteBufCodecs.STRING_UTF8.decode(buf))));

    static final RecipeSerializer<ShapedRecipe> COPPER_WIRE_SERIALIZER = new RecipeSerializer<>(
            MapCodec.unit(() -> new FoundationRecipe("copper_wire")),
            StreamCodec.of((buf, recipe) -> {}, buf -> new FoundationRecipe("copper_wire")));

    private record Pattern(String study, DeferredItem<?> output, int count, int width, ItemLike... cells) {
        String id() { return output.getId().getPath(); }

        boolean fits(CraftingInput grid) {
            if (grid.width() != width || grid.height() != cells.length / width) return false;
            for (int i = 0; i < cells.length; i++) {
                var stack = grid.getItem(i % width, i / width);
                if (cells[i] == null ? !stack.isEmpty() : !stack.is(cells[i].asItem())) return false;
            }
            return true;
        }

        ShapedRecipePattern shape() {
            return new ShapedRecipePattern(width, cells.length / width, Arrays.stream(cells)
                    .map(item -> Optional.ofNullable(item).map(Ingredient::of)).toList(), Optional.empty());
        }

        Preview preview() {
            var ingredients = Arrays.stream(cells)
                    .map(item -> item == null ? ItemStack.EMPTY : new ItemStack(item)).toList();
            return new Preview(new ItemStack(output.get(), count), width, ingredients);
        }
    }

    private static final class Patterns {
        private static final List<Pattern> ALL = createPatterns();
        private static final Map<String, Pattern> BY_RESULT = ALL.stream()
                .collect(Collectors.toUnmodifiableMap(Pattern::id, Function.identity()));
    }

    private final Pattern craft;

    private FoundationRecipe(String id) {
        this(Patterns.BY_RESULT.get(id));
    }

    private FoundationRecipe(Pattern craft) {
        super(new CommonInfo(true), new CraftingBookInfo(CraftingBookCategory.MISC, ""),
                craft.shape(), new ItemStackTemplate(craft.output().get(), craft.count()));
        this.craft = craft;
    }

    private static String craftId(ShapedRecipe recipe) {
        return ((FoundationRecipe) recipe).craft.id();
    }

    public record Preview(ItemStack output, int width, List<ItemStack> ingredients) {}

    public static List<Preview> previews(int research) {
        String key = ResearchCatalog.STUDIES.get(research).key();
        return Patterns.ALL.stream().filter(pattern -> pattern.study().equals(key)).map(Pattern::preview).toList();
    }

    private static List<Pattern> createPatterns() {
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
            new Pattern("material_observation", ModItems.EMPIRICAL_ASSAY_BENCH, 1, 3,
                iron, nugget, iron, plank, plank, plank, Items.OAK_LOG, null, Items.OAK_LOG),
            new Pattern("copper_wire", ModItems.COPPER_WIRE, 6, 3, Items.COPPER_INGOT, Items.COPPER_INGOT, Items.COPPER_INGOT),
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
                insulated, shaft, insulated),
            new Pattern("electromagnetic_induction", ModItems.COPPER_BUSBAR, 4, 3,
                wire, wire, wire),
            new Pattern("galvanic_storage", ModItems.INSULATED_CONDUIT, 2, 3,
                insulated, ModItems.COPPER_BUSBAR.get(), insulated),
            new Pattern("fluid_hydrostatics", ModItems.FLUID_PIPE, 4, 3,
                ModItems.COPPER_SHEET.get(), Items.LEATHER, ModItems.COPPER_SHEET.get()),
            new Pattern("fluid_hydrostatics", ModItems.GLASS_PIPE, 4, 3,
                Items.GLASS, ModItems.COPPER_WIRE.get(), Items.GLASS),
            new Pattern("fluid_hydrostatics", ModItems.SETTLING_VAT, 1, 3,
                Items.GLASS, Items.GLASS, Items.GLASS,
                ModItems.IRON_SHEET.get(), Items.BUCKET, ModItems.IRON_SHEET.get(),
                ModItems.COPPER_SHEET.get(), Items.CAULDRON, ModItems.COPPER_SHEET.get()),
            new Pattern("pneumatic_transport", ModItems.PNEUMATIC_TUBE, 4, 3,
                ModItems.COPPER_SHEET.get(), Items.GLASS, ModItems.COPPER_SHEET.get()),
            new Pattern("pneumatic_transport", ModItems.BELLOWS_EJECTOR, 1, 3,
                ModItems.COPPER_SHEET.get(), ModItems.MANUAL_BELLOWS.get(), ModItems.COPPER_SHEET.get(),
                Items.IRON_INGOT, ModItems.IRON_GEAR.get(), Items.IRON_INGOT,
                Items.COBBLESTONE, ModItems.MECHANICAL_SHAFT.get(), Items.COBBLESTONE),
            new Pattern("pneumatic_transport", ModItems.GRAVITATIONAL_SORTER, 1, 3,
                Items.IRON_BARS, Items.GLASS, Items.IRON_BARS,
                Items.OAK_PLANKS, ModItems.IRON_GEAR.get(), Items.OAK_PLANKS,
                Items.OAK_PLANKS, Items.HOPPER, Items.OAK_PLANKS),
            new Pattern("peristaltic_mechanics", ModItems.PERISTALTIC_PUMP, 1, 3,
                ModItems.COPPER_SHEET.get(), Items.LEATHER, ModItems.COPPER_SHEET.get(),
                ModItems.IRON_GEAR.get(), ModItems.PRIMITIVE_MECHANICAL_CORE.get(), ModItems.IRON_GEAR.get(),
                ModItems.IRON_SHEET.get(), ModItems.MECHANICAL_SHAFT.get(), ModItems.IRON_SHEET.get()),
            new Pattern("piezoelectricity", ModItems.PIEZO_SWITCH, 1, 3,
                Items.QUARTZ, Items.QUARTZ, Items.QUARTZ,
                sheet, Items.REDSTONE, sheet,
                iron, iron, iron),
            new Pattern("load_protection", ModItems.LOAD_RELAY, 1, 3,
                sheet, Items.COMPARATOR, sheet,
                insulated, ModItems.PRIMITIVE_MECHANICAL_CORE.get(), insulated,
                iron, Items.REDSTONE, iron),
            new Pattern("thermal_annealing", ModItems.BELLOWS_BRAZIER, 1, 3,
                Items.COBBLESTONE, Items.QUARTZ, Items.COBBLESTONE,
                Items.COBBLESTONE, Items.CAMPFIRE, Items.COBBLESTONE,
                iron, iron, iron),
            new Pattern("wire_drawing", ModItems.WIRE_DRAWING_BENCH, 1, 3,
                sheet, gear, sheet,
                plank, ModItems.PRIMITIVE_MECHANICAL_CORE.get(), plank,
                iron, shaft, iron),
            new Pattern("galvanic_storage", ModItems.GALVANIC_ACCUMULATOR, 1, 3,
                ModItems.COPPER_SHEET.get(), insulated, ModItems.COPPER_SHEET.get(),
                plank, Items.LAPIS_LAZULI, plank,
                sheet, coil, sheet),
            new Pattern("electromechanical_forming", ModItems.ELECTROMECHANICAL_PRESS, 1, 3,
                sheet, coil, sheet,
                gear, ModItems.PRIMITIVE_MECHANICAL_CORE.get(), shaft,
                sheet, ModItems.FORMING_BENCH.get(), sheet)
        );
    }

    public boolean unlocked(Player player) {
        return ResearchCraftingAccess.knows(player, craft.study())
                && (!craft.study().equals("copper_wire") || ResearchCraftingAccess.knows(player, "conductivity"));
    }

    @Override public boolean matches(CraftingInput grid, Level level) {
        // Keep the original orientation instead of accepting a mirrored recipe.
        Player crafter = ResearchCraftingAccess.crafter(level);
        return crafter != null && unlocked(crafter) && craft.fits(grid);
    }

    @Override public RecipeSerializer<ShapedRecipe> getSerializer() {
        return craft.id().equals("copper_wire") ? ModRecipes.COPPER_WIRE.get() : ModRecipes.FOUNDATIONS.get();
    }
}
