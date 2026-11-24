package br.com.quantumindustries.client.jei;

import br.com.quantumindustries.client.MechanicalRecipeCache;
import br.com.quantumindustries.block.entity.WireDrawingBenchBlockEntity;
import br.com.quantumindustries.material.AlloyCatalog;
import br.com.quantumindustries.material.CrushingCatalog;
import br.com.quantumindustries.material.FormingMaterials;
import br.com.quantumindustries.material.HeatTreatment;
import br.com.quantumindustries.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

/** Optional client-only recipe viewer; processing and research remain server-owned. */
@JeiPlugin
public final class QuantumJeiPlugin implements IModPlugin {
    private static final List<String> MACHINES = List.of("electric_sawmill", "mechanical_sawmill", "jaw_crusher",
            "bellows_alloy_kiln", "electric_alloy_smelter", "forming_bench",
            "electromechanical_press", "wire_drawing_bench", "bellows_brazier",
            "settling_vat", "manual_grain_mill");

    record Process(List<List<ItemStack>> inputs, ItemStack output) {}

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("quantumindustries", path); }
    private static Item machine(String name) { return BuiltInRegistries.ITEM.getValue(id(name)); }
    private static IRecipeType<Process> type(String name) { return IRecipeType.create(id(name), Process.class); }

    @Override public Identifier getPluginUid() { return id("jei"); }

    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        for (String name : MACHINES) {
            registration.addRecipeCategories(new Category(name, registration.getJeiHelpers().getGuiHelper()));
        }
    }

    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (String name : MACHINES) registration.addCraftingStation(type(name), machine(name));
    }

    private static List<ItemStack> alternatives(List<Supplier<Item>> items, int count) {
        return items.stream().map(item -> new ItemStack(item.get(), count)).toList();
    }

    @Override public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(type("mechanical_sawmill"), MechanicalRecipeCache.all().stream()
                .map(recipe -> new Process(List.of(recipe.inputs().stream().map(ItemStack::new).toList()), recipe.result()))
                .toList());
        registration.addRecipes(type("electric_sawmill"), MechanicalRecipeCache.all().stream()
                .map(recipe -> new Process(List.of(recipe.inputs().stream().map(ItemStack::new).toList()), recipe.result().copyWithCount(8))).toList());
        for (String name : List.of("bellows_alloy_kiln", "electric_alloy_smelter")) {
            registration.addRecipes(type(name), AlloyCatalog.RECIPES.stream()
                    .filter(recipe -> name.equals("electric_alloy_smelter") || recipe.tier() == 1)
                    .map(recipe -> new Process(List.of(alternatives(recipe.inputsA(), recipe.countA()),
                            alternatives(recipe.inputsB(), recipe.countB())), recipe.assemble())).toList());
        }
        List<Process> crushing = new ArrayList<>(), forming = new ArrayList<>(), drawing = new ArrayList<>();
        List<Process> heating = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack input = new ItemStack(item);
            add(crushing, input, CrushingCatalog.resultFor(input));
            add(drawing, input, WireDrawingBenchBlockEntity.resultForSheet(input));
            var metal = FormingMaterials.find(input);
            if (metal != null) add(forming, input, new ItemStack(metal.sheet().get()));
            if (HeatTreatment.isHeated(input)) add(heating, HeatTreatment.cooled(input), input);
        }
        registration.addRecipes(type("jaw_crusher"), crushing);
        registration.addRecipes(type("forming_bench"), forming);
        registration.addRecipes(type("electromechanical_press"), forming);
        registration.addRecipes(type("wire_drawing_bench"), drawing);
        registration.addRecipes(type("bellows_brazier"), heating);
        registration.addRecipes(type("manual_grain_mill"), List.of(new Process(
                List.of(List.of(new ItemStack(Items.WHEAT))), new ItemStack(ModItems.FLOUR.get()))));
        registration.addRecipes(type("settling_vat"), List.of(
                settling(Items.LAPIS_LAZULI, ModItems.GALVANIC_ELECTROLYTE.get()),
                settling(ModItems.CHEMICAL_PRODUCTS.get("sulfur").get(), ModItems.GALVANIC_ELECTROLYTE.get()),
                settling(ModItems.CHEMICAL_PRODUCTS.get("halite").get(), ModItems.HYDROCHLORIC_ACID.get())));
    }

    private static Process settling(Item reagent, Item result) {
        return new Process(List.of(List.of(new ItemStack(reagent)), List.of(new ItemStack(Items.GLASS_BOTTLE))),
                new ItemStack(result));
    }

    private static void add(List<Process> recipes, ItemStack input, ItemStack output) {
        if (!output.isEmpty()) recipes.add(new Process(List.of(List.of(input)), output));
    }

    private record Category(String name, IGuiHelper gui) implements IRecipeCategory<Process> {
        @Override public IRecipeType<Process> getRecipeType() { return type(name); }
        @Override public Component getTitle() { return Component.translatable("block.quantumindustries." + name); }
        @Override public int getWidth() { return 156; }
        @Override public int getHeight() { return name.equals("electric_sawmill")?48:38; }
        @Override public IDrawable getIcon() { return gui.createDrawableItemLike(machine(name)); }
        @Override public void createRecipeExtras(mezz.jei.api.gui.widgets.IRecipeExtrasBuilder builder,
                Process recipe, IFocusGroup focuses) {
            builder.addDrawable(gui.getRecipeArrow()).setPosition(77, 10);
            if(name.equals("electric_sawmill"))builder.addText(Component.literal("2 s | 160 FE | 4 FE/t"),146,10).setPosition(5,32);
        }
        @Override public void setRecipe(IRecipeLayoutBuilder builder, Process recipe, IFocusGroup focuses) {
            for (int i = 0; i < recipe.inputs().size(); i++) {
                builder.addSlot(RecipeIngredientRole.INPUT, 5 + i * 24, 10).addItemStacks(recipe.inputs().get(i));
            }
            builder.addSlot(RecipeIngredientRole.OUTPUT, 107, 10).add(recipe.output());
            if (name.equals("settling_vat")) {
                builder.addSlot(RecipeIngredientRole.INPUT, 53, 10).add(Fluids.WATER, 1000);
                builder.addSlot(RecipeIngredientRole.OUTPUT, 131, 10).add(ModItems.PIGMENT_SEDIMENT.get());
            }
            if (name.equals("forming_bench")) {
                builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 53, 10)
                        .add(machine("manual_hammer"));
            }
        }
    }
}
