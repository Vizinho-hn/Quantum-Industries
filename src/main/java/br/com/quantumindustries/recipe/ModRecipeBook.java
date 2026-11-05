package br.com.quantumindustries.recipe;

import br.com.quantumindustries.QuantumIndustries;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

@EventBusSubscriber(modid = QuantumIndustries.MODID)
public final class ModRecipeBook {
    private ModRecipeBook() {}

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(ModRecipeBook::sync);
    }

    public static void sync(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        List<RecipeHolder<?>> award = new ArrayList<>();
        List<RecipeHolder<?>> remove = new ArrayList<>();
        for (var recipe : serverPlayer.level().getServer().getRecipeManager().getRecipes()) {
            if (!recipe.id().identifier().getNamespace().equals(QuantumIndustries.MODID)
                    || !(recipe.value() instanceof CraftingRecipe)) continue;
            if (recipe.value() instanceof FoundationRecipe foundation && !foundation.unlocked(player)) {
                remove.add(recipe);
            } else {
                award.add(recipe);
            }
        }
        serverPlayer.resetRecipes(remove);
        serverPlayer.awardRecipes(award);
        if (player.containerMenu instanceof AbstractCraftingMenu menu) {
            menu.slotsChanged(menu.getInputGridSlots().getFirst().container);
        }
    }
}
