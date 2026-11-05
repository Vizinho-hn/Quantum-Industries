package br.com.quantumindustries.recipe;

import br.com.quantumindustries.QuantumIndustries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    private ModRecipes() {}

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, QuantumIndustries.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ShapedRecipe>> FOUNDATIONS =
            SERIALIZERS.register("foundations", () -> FoundationRecipe.SERIALIZER);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ShapedRecipe>> COPPER_WIRE =
            SERIALIZERS.register("copper_wire", () -> FoundationRecipe.COPPER_WIRE_SERIALIZER);

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}
