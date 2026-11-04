package br.com.quantumindustries.recipe;

import br.com.quantumindustries.QuantumIndustries;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Recipe is a server-gated special crafting recipe, never a plain vanilla JSON. */
public final class ModRecipes {
    private ModRecipes() {}

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, QuantumIndustries.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CopperWireRecipe>> COPPER_WIRE =
            SERIALIZERS.register("copper_wire", () -> new RecipeSerializer<>(
                    MapCodec.unit(CopperWireRecipe.INSTANCE),
                    StreamCodec.unit(CopperWireRecipe.INSTANCE)
            ));

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}
