package br.com.quantumindustries.recipe;

import java.util.Comparator;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

public final class MechanicalProcesses {
    private MechanicalProcesses() {}

    public static List<RecipeHolder<MechanicalProcessingRecipe>> all(ServerLevel level) {
        return level.getServer().getRecipeManager().getRecipes().stream()
                .filter(holder -> holder.value() instanceof MechanicalProcessingRecipe)
                .map(holder -> new RecipeHolder<>(holder.id(), (MechanicalProcessingRecipe) holder.value()))
                .sorted(Comparator.comparing(holder -> holder.id().identifier().toString())).toList();
    }

    public static RecipeHolder<MechanicalProcessingRecipe> find(ServerLevel level, ItemStack input) {
        return all(level).stream().filter(holder -> holder.value().accepts(input)).findFirst().orElse(null);
    }
}
