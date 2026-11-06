package br.com.quantumindustries.client;

import br.com.quantumindustries.recipe.MechanicalProcessingRecipe;
import br.com.quantumindustries.recipe.ModRecipes;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;

@EventBusSubscriber(modid = "quantumindustries", value = Dist.CLIENT)
public final class MechanicalRecipeCache {
    private static List<MechanicalProcessingRecipe> recipes = List.of();
    private MechanicalRecipeCache() {}
    public static List<MechanicalProcessingRecipe> all() { return recipes; }
    @SubscribeEvent public static void receive(RecipesReceivedEvent event) {
        if (!event.getRecipeTypes().contains(ModRecipes.MECHANICAL_PROCESSING_TYPE.get())) return;
        recipes = event.getRecipeMap().byType(ModRecipes.MECHANICAL_PROCESSING_TYPE.get()).stream()
                .sorted(Comparator.comparing(holder -> holder.id().identifier().toString()))
                .map(RecipeHolder::value).toList();
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { recipes = List.of(); }
}
