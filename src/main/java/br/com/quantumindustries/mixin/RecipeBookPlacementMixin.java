package br.com.quantumindustries.mixin;

import br.com.quantumindustries.recipe.FoundationRecipe;
import br.com.quantumindustries.recipe.ResearchCraftingAccess;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.RecipeBookMenu.PostPlaceAction;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractCraftingMenu.class)
abstract class RecipeBookPlacementMixin {
    @WrapMethod(method = "handlePlacement")
    private PostPlaceAction quantumindustries$placeKnownRecipe(boolean useMaxItems, boolean bypassRoomCheck,
            RecipeHolder<?> recipe, ServerLevel level, Inventory inventory, Operation<PostPlaceAction> original) {
        if (!(recipe.value() instanceof FoundationRecipe foundation)) {
            return original.call(useMaxItems, bypassRoomCheck, recipe, level, inventory);
        }
        if (!foundation.unlocked(inventory.player)) return PostPlaceAction.NOTHING;
        return ResearchCraftingAccess.asCrafter(inventory.player,
                () -> original.call(useMaxItems, bypassRoomCheck, recipe, level, inventory));
    }
}
