package br.com.quantumindustries.mixin;

import br.com.quantumindustries.recipe.ResearchCraftingAccess;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ResultSlot.class)
abstract class CraftingResultSlotMixin {
    @Shadow @Final private Player player;

    // Vanilla matches the recipe again when its output is taken, to determine container remainders.
    @WrapMethod(method = "getRemainingItems")
    private NonNullList<ItemStack> quantumindustries$withResearchOwner(CraftingInput grid, Level level,
            Operation<NonNullList<ItemStack>> original) {
        return ResearchCraftingAccess.asCrafter(player, () -> original.call(grid, level));
    }
}
