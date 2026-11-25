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
        List<String> requirements() {
            if (id().equals("steam_boiler") || id().equals("steam_engine")) return List.of(study, "thermal_annealing");
            return (id().equals("mechanical_sawmill") || id().equals("compact_gearbox") || id().equals("mechanical_conveyor")) ? List.of(study, "metal_sheets") : List.of(study);
        }

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
            return new Preview(new ItemStack(output.get(), count), width, ingredients, requirements());
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

    public record Preview(ItemStack output, int width, List<ItemStack> ingredients, List<String> requiredResearch) {}

    public static List<Preview> previews(int research) {
        String key = ResearchCatalog.STUDIES.get(research).key();
        return java.util.stream.Stream.concat(Patterns.ALL.stream(), legacyPreviews().stream())
                .filter(pattern -> pattern.study().equals(key)).map(Pattern::preview).toList();
    }

    // Preview-only: vanilla recipes retain their plank tags and crafting behavior.
    private static List<Pattern> legacyPreviews() {
        var p=Items.OAK_PLANKS;var i=Items.IRON_INGOT;var b=ModItems.ALLOY_INGOTS.get("bronze").get();
        var s=ModItems.MECHANICAL_SHAFT_BLOCK.get();var g=ModItems.IRON_GEAR.get();
        return List.of(
            new Pattern("mechanical_transmission", ModItems.SMALL_COGWHEEL,1,3,null,p,null,i,s,i,null,p,null),
            new Pattern("mechanical_transmission", ModItems.LARGE_COGWHEEL,1,3,p,i,p,i,ModItems.SMALL_COGWHEEL.get(),i,p,i,p),
            new Pattern("mechanical_transmission", ModItems.IRON_FLYWHEEL,1,3,i,b,i,g,s,g,i,b,i),
            new Pattern("mechanical_transmission", ModItems.MECHANICAL_PULLEY,1,3,b,i,b,p,s,p,b,i,b),
            new Pattern("mechanical_transmission", ModItems.DRIVE_BELT,1,3,Items.LEATHER,Items.LEATHER,Items.LEATHER,b,null,b,Items.LEATHER,Items.LEATHER,Items.LEATHER),
            new Pattern("hydraulic_power", ModItems.MEDIUM_WATER_WHEEL,1,3,p,i,p,p,ModItems.PRIMITIVE_WATER_WHEEL.get(),p,p,i,p),
            new Pattern("hydraulic_power", ModItems.LARGE_WATER_WHEEL,1,3,p,i,p,b,ModItems.MEDIUM_WATER_WHEEL.get(),b,p,i,p),
            new Pattern("conductivity", ModItems.ENERGY_INSPECTION_GOGGLES,1,3,Items.COPPER_INGOT,ModItems.COPPER_WIRE.get(),Items.COPPER_INGOT,Items.GLASS,Items.LEATHER,Items.GLASS)
        );
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
            new Pattern("material_observation", ModItems.MANUAL_GRAIN_MILL, 1, 3,
                plank, Items.COBBLESTONE, plank,
                plank, Items.IRON_INGOT, plank,
                plank, Items.STONE, plank),
            new Pattern("material_observation", ModItems.EMPIRICAL_ASSAY_BENCH, 1, 3,
                iron, nugget, iron, plank, plank, plank, Items.OAK_LOG, null, Items.OAK_LOG),
            new Pattern("copper_wire", ModItems.COPPER_WIRE, 6, 3, Items.COPPER_INGOT, Items.COPPER_INGOT, Items.COPPER_INGOT),
            new Pattern("manual_forming", ModItems.MANUAL_HAMMER, 1, 3, nugget,nugget,nugget, null,stick,null, null,stick,null),
            new Pattern("manual_forming", ModItems.FORMING_BENCH, 1, 3, iron,iron,iron, plank,null,plank, plank,null,plank),
            new Pattern("basic_mechanics", ModItems.IRON_GEAR, 1, 3, null,nugget,null, nugget,iron,nugget, null,nugget,null),
            new Pattern("basic_mechanics", ModItems.CONFIGURATION_WRENCH, 1, 3, iron,null,iron, null,gear,null, null,stick,null),
            new Pattern("mechanical_transmission", ModItems.MECHANICAL_SHAFT, 2, 1, iron,iron),
            new Pattern("mechanical_transmission", ModItems.HAND_CRANK, 1, 2, shaft,gear, null,stick),
            new Pattern("mechanical_transmission", ModItems.MECHANICAL_SHAFT_BLOCK, 2, 3, plank,shaft,plank, plank,shaft,plank),
            new Pattern("mechanical_transmission", ModItems.MECHANICAL_CONVEYOR, 2, 3,
                Items.LEATHER, Items.LEATHER, Items.LEATHER,
                gear, shaft, gear,
                plank, sheet, plank),
            new Pattern("mechanical_transmission", ModItems.COMPACT_GEARBOX, 1, 3,
                sheet, shaft, sheet,
                gear, plank, gear,
                sheet, shaft, sheet),
            new Pattern("mechanical_transmission", ModItems.MECHANICAL_SAWMILL, 1, 3,
                sheet, iron, sheet, plank, gear, plank, plank, shaft, plank),
            new Pattern("hydraulic_power", ModItems.PRIMITIVE_WATER_WHEEL, 1, 3,
                plank, plank, plank, gear, shaft, gear, plank, plank, plank),
            new Pattern("electrical_insulation", ModItems.INSULATED_COPPER_WIRE, 2, 2, wire,wire, Items.LEATHER,paper),
            new Pattern("copper_winding", ModItems.COPPER_COIL, 1, 3, wire,stick,wire, wire,paper,wire),
            new Pattern("electromechanical_structure", ModItems.ELECTROMECHANICAL_FRAME, 1, 3, plank,ModItems.IRON_SHEET.get(),plank,
                null,shaft,null, plank,ModItems.IRON_SHEET.get(),plank),
            new Pattern("electromechanical_structure", ModItems.PRIMITIVE_MECHANICAL_CORE, 1, 3,
                sheet, gear, sheet,
                nugget, ModItems.ELECTROMECHANICAL_FRAME.get(), nugget,
                null, shaft, null),
            new Pattern("manual_dynamo_design", ModItems.MANUAL_DYNAMO, 1, 3,
                coil, ModItems.PERMANENT_MAGNET.get(), coil,
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
            new Pattern("load_protection", ModItems.ELECTRICAL_NETWORK_ANALYZER, 1, 3,
                ModItems.COPPER_SHEET.get(), Items.GLASS, ModItems.COPPER_SHEET.get(),
                ModItems.COPPER_COIL.get(), Items.COMPARATOR, ModItems.COPPER_COIL.get(),
                sheet, ModItems.COPPER_BUSBAR.get(), sheet),
            new Pattern("load_protection", ModItems.INDUSTRIAL_CIRCUIT_SWITCH, 1, 3,
                null, Items.LEVER, null,
                ModItems.COPPER_SHEET.get(), Items.TERRACOTTA, ModItems.COPPER_SHEET.get(),
                sheet, insulated, sheet),
            new Pattern("thermal_annealing", ModItems.BELLOWS_BRAZIER, 1, 3,
                Items.COBBLESTONE, Items.QUARTZ, Items.COBBLESTONE,
                Items.COBBLESTONE, Items.CAMPFIRE, Items.COBBLESTONE,
                iron, iron, iron),
            new Pattern("wire_drawing", ModItems.WIRE_DRAWING_BENCH, 1, 3,
                sheet, gear, sheet,
                plank, ModItems.PRIMITIVE_MECHANICAL_CORE.get(), plank,
                iron, shaft, iron),
            new Pattern("galvanic_storage", ModItems.VOLTAIC_PILE, 1, 3,
                ModItems.COPPER_SHEET.get(), insulated, ModItems.COPPER_SHEET.get(),
                plank, Items.LAPIS_LAZULI, plank,
                sheet, coil, sheet),
            // Era II - Lead-Acid Accumulator (rechargeable, connects to cable network)
            new Pattern("galvanic_storage", ModItems.LEAD_ACID_ACCUMULATOR, 1, 3,
                ModItems.LEAD_SHEET.get(), insulated, ModItems.COPPER_SHEET.get(),
                plank, ModItems.GALVANIC_ELECTROLYTE.get(), plank,
                ModItems.LEAD_SHEET.get(), coil, sheet),
            new Pattern("basic_magnetism", ModItems.PERMANENT_MAGNET, 1, 3,
                iron, Items.REDSTONE, iron),
            new Pattern("electromagnetic_excitation", ModItems.LAMINATED_IRON_CORE, 1, 3,
                sheet, paper, sheet, sheet, paper, sheet),
            new Pattern("electromagnetic_excitation", ModItems.EXCITATION_COIL, 1, 3,
                insulated, ModItems.LAMINATED_IRON_CORE.get(), insulated,
                insulated, coil, insulated),
            new Pattern("electromagnetic_excitation", ModItems.ELECTROMAGNETIC_ROTOR, 1, 3,
                sheet, shaft, sheet, coil, ModItems.EXCITATION_COIL.get(), coil),
            new Pattern("electromagnetic_excitation", ModItems.GENERATOR_STATOR, 1, 3,
                coil, ModItems.LAMINATED_IRON_CORE.get(), coil,
                sheet, null, sheet, coil, sheet, coil),
            new Pattern("alternating_current", ModItems.PRIMITIVE_RECTIFIER, 1, 3,
                wire, Items.QUARTZ, wire, ModItems.COPPER_SHEET.get(), Items.REDSTONE, ModItems.COPPER_SHEET.get()),
            new Pattern("alternating_current", ModItems.VOLTAGE_REGULATOR, 1, 3,
                insulated, Items.REDSTONE, insulated, coil, ModItems.PRIMITIVE_RECTIFIER.get(), coil),
            new Pattern("industrial_generation", ModItems.ELECTROMAGNETIC_GENERATOR, 1, 3,
                sheet, ModItems.GENERATOR_STATOR.get(), sheet,
                shaft, ModItems.ELECTROMAGNETIC_ROTOR.get(), gear,
                ModItems.PRIMITIVE_RECTIFIER.get(), ModItems.VOLTAGE_REGULATOR.get(), insulated),
            new Pattern("mechanical_energy_storage", ModItems.GRAVITY_WINCH, 1, 3,
                Items.DARK_OAK_PLANKS, shaft, Items.DARK_OAK_PLANKS,
                sheet, ModItems.MECHANICAL_PULLEY.get(), sheet,
                Items.DARK_OAK_PLANKS, null, Items.DARK_OAK_PLANKS),
            new Pattern("mechanical_energy_storage", ModItems.MAINSPRING_DRUM, 1, 3,
                ModItems.COPPER_SHEET.get(), sheet, ModItems.COPPER_SHEET.get(),
                ModItems.BRONZE_GEAR.get(), shaft, gear,
                Items.DARK_OAK_PLANKS, Items.DARK_OAK_PLANKS, Items.DARK_OAK_PLANKS),
            new Pattern("electromechanical_forming", ModItems.ELECTRIC_MOTOR, 1, 3,
                sheet, coil, sheet,
                shaft, ModItems.PRIMITIVE_MECHANICAL_CORE.get(), coil,
                insulated, ModItems.ELECTROMECHANICAL_FRAME.get(), insulated),
            new Pattern("fluid_hydrostatics", ModItems.STEAM_BOILER, 1, 3,
                ModItems.COPPER_SHEET.get(), ModItems.FLUID_PIPE.get(), ModItems.COPPER_SHEET.get(),
                sheet, Items.CAULDRON, sheet,
                ModItems.REFRACTORY_BRICK.get(), Items.FURNACE, ModItems.REFRACTORY_BRICK.get()),
            new Pattern("fluid_hydrostatics", ModItems.STEAM_ENGINE, 1, 3,
                sheet, Items.PISTON, sheet,
                ModItems.FLUID_PIPE.get(), ModItems.PRIMITIVE_MECHANICAL_CORE.get(), shaft,
                sheet, ModItems.BRONZE_GEAR.get(), sheet),
            new Pattern("electric_alloy_smelting", ModItems.COPPER_HEATSINK, 1, 3,
                ModItems.COPPER_SHEET.get(), Items.COPPER_INGOT, ModItems.COPPER_SHEET.get(),
                ModItems.COPPER_SHEET.get(), ModItems.COPPER_SHEET.get(), ModItems.COPPER_SHEET.get()),
            new Pattern("electric_alloy_smelting", ModItems.ALUMINUM_HEATSINK, 1, 3,
                ModItems.INGOTS.get("aluminum").get(), ModItems.COPPER_SHEET.get(), ModItems.INGOTS.get("aluminum").get(),
                ModItems.INGOTS.get("aluminum").get(), ModItems.INGOTS.get("aluminum").get(), ModItems.INGOTS.get("aluminum").get()),
            new Pattern("electromagnetic_induction", ModItems.THERMAL_FAN, 1, 3,
                sheet, ModItems.COPPER_COIL.get(), sheet,
                sheet, ModItems.ELECTROMAGNETIC_ROTOR.get(), sheet),
            new Pattern("electric_alloy_smelting", ModItems.THERMOELECTRIC_GENERATOR, 1, 3,
                ModItems.COPPER_SHEET.get(), ModItems.CONSTANTAN_WIRE.get(), ModItems.COPPER_SHEET.get(),
                insulated, ModItems.CONSTANTAN_COIL.get(), insulated,
                ModItems.REFRACTORY_BRICK.get(), sheet, ModItems.REFRACTORY_BRICK.get()),
            new Pattern("electromechanical_forming", ModItems.ELECTROMECHANICAL_PRESS, 1, 3,
                sheet, coil, sheet,
                gear, ModItems.PRIMITIVE_MECHANICAL_CORE.get(), shaft,
                sheet, ModItems.FORMING_BENCH.get(), sheet),
            new Pattern("alloy_metallurgy", ModItems.BELLOWS_ALLOY_KILN, 1, 3,
                Items.TERRACOTTA, sheet, Items.TERRACOTTA,
                Items.BRICKS, Items.CAMPFIRE, Items.BRICKS,
                Items.SMOOTH_STONE, Items.FURNACE, Items.SMOOTH_STONE),
            new Pattern("alloy_metallurgy", ModItems.BRONZE_GEAR, 2, 3,
                null, ModItems.ALLOY_INGOTS.get("bronze").get(), null,
                ModItems.ALLOY_INGOTS.get("bronze").get(), ModItems.PRIMITIVE_MECHANICAL_CORE.get(), ModItems.ALLOY_INGOTS.get("bronze").get(),
                null, ModItems.ALLOY_INGOTS.get("bronze").get(), null),
            new Pattern("electric_wood_processing", ModItems.ELECTRIC_SAWMILL, 1, 3,
                sheet, coil, sheet,
                ModItems.BRONZE_GEAR.get(), ModItems.MECHANICAL_SAWMILL.get(), ModItems.PRIMITIVE_MECHANICAL_CORE.get(),
                insulated, ModItems.ELECTROMECHANICAL_FRAME.get(), insulated),
            new Pattern("joule_heating", ModItems.ELECTRIC_FURNACE, 1, 3,
                sheet, coil, sheet,
                ModItems.REFRACTORY_BRICK.get(), ModItems.ELECTROMECHANICAL_FRAME.get(), ModItems.REFRACTORY_BRICK.get(),
                insulated, Items.FURNACE, insulated),
            new Pattern("electric_alloy_smelting", ModItems.ELECTRIC_ALLOY_SMELTER, 1, 3,
                ModItems.CONSTANTAN_COIL.get(), ModItems.ELECTROMECHANICAL_FRAME.get(), ModItems.CONSTANTAN_COIL.get(),
                ModItems.REFRACTORY_BRICK.get(), ModItems.BELLOWS_ALLOY_KILN.get(), ModItems.REFRACTORY_BRICK.get(),
                insulated, ModItems.LOAD_RELAY.get(), insulated),
            new Pattern("electric_alloy_smelting", ModItems.CONSTANTAN_COIL, 1, 3,
                null, ModItems.CONSTANTAN_WIRE.get(), null,
                ModItems.CONSTANTAN_WIRE.get(), ModItems.REFRACTORY_BRICK.get(), ModItems.CONSTANTAN_WIRE.get(),
                null, ModItems.CONSTANTAN_WIRE.get(), null),
            new Pattern("mineral_beneficiation", ModItems.JAW_CRUSHER, 1, 3,
                sheet, Items.PISTON, sheet,
                gear, ModItems.FORMING_BENCH.get(), gear,
                sheet, shaft, sheet),
            new Pattern("thermal_annealing", ModItems.REFRACTORY_BRICK, 4, 3,
                Items.BRICK, Items.CLAY_BALL, Items.BRICK,
                Items.CLAY_BALL, Items.MAGMA_BLOCK, Items.CLAY_BALL,
                Items.BRICK, Items.CLAY_BALL, Items.BRICK),
            new Pattern("chemical_leaching", ModItems.PHOSPHATE_FERTILIZER, 4, 3,
                Items.BONE_MEAL, ModItems.HYDROCHLORIC_ACID.get(), Items.BONE_MEAL,
                ModItems.HYDROCHLORIC_ACID.get(), Items.SAND, ModItems.HYDROCHLORIC_ACID.get(),
                Items.BONE_MEAL, ModItems.HYDROCHLORIC_ACID.get(), Items.BONE_MEAL),
            new Pattern("scientific_analyzer_tier_2", ModItems.SCIENTIFIC_ANALYZER, 1, 3,
                Items.DEEPSLATE, Items.BOOK, Items.DEEPSLATE,
                Items.LAPIS_LAZULI, null, Items.LAPIS_LAZULI,
                plank, plank, plank)
        );
    }

    public boolean unlocked(Player player) {
        return craft.requirements().stream().allMatch(key -> ResearchCraftingAccess.knows(player, key))
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
