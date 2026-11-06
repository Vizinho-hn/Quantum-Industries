package br.com.quantumindustries.recipe;

import br.com.quantumindustries.QuantumIndustries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    private ModRecipes() {}

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, QuantumIndustries.MODID);
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, QuantumIndustries.MODID);
    public static final DeferredHolder<RecipeType<?>, RecipeType<MechanicalProcessingRecipe>> MECHANICAL_PROCESSING_TYPE =
            TYPES.register("mechanical_processing", () -> new RecipeType<MechanicalProcessingRecipe>() {
                @Override public String toString() { return "quantumindustries:mechanical_processing"; }
            });
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MechanicalProcessingRecipe>> MECHANICAL_PROCESSING =
            SERIALIZERS.register("mechanical_processing", () -> new RecipeSerializer<>(
                    MechanicalProcessingRecipe.CODEC, MechanicalProcessingRecipe.STREAM_CODEC));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ShapedRecipe>> FOUNDATIONS =
            SERIALIZERS.register("foundations", () -> FoundationRecipe.SERIALIZER);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ShapedRecipe>> COPPER_WIRE =
            SERIALIZERS.register("copper_wire", () -> FoundationRecipe.COPPER_WIRE_SERIALIZER);

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
        TYPES.register(eventBus);
    }
}
