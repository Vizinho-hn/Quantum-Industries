package br.com.quantumindustries.mixin;

import br.com.quantumindustries.recipe.ResearchCraftingAccess;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CraftingMenu.class)
abstract class CraftingMenuMixin {
    @WrapMethod(method = "slotChangedCraftingGrid")
    private static void quantumindustries$withResearchOwner(AbstractContainerMenu menu, ServerLevel level,
            Player player, CraftingContainer grid, ResultContainer result, RecipeHolder<CraftingRecipe> hint,
            Operation<Void> original) {
        ResearchCraftingAccess.withCrafter(player, () -> original.call(menu, level, player, grid, result, hint));
    }
}
