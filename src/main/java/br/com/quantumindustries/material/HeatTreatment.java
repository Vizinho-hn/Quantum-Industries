package br.com.quantumindustries.material;

import br.com.quantumindustries.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.Items;

/** Time-bounded thermal workability for iron, copper, bronze, brass, and carbon steel ingots. */
public final class HeatTreatment {
    public static final long HOT_TICKS = 600;
    private HeatTreatment() {}

    public static boolean canHeat(ItemStack stack) {
        return stack.is(Items.IRON_INGOT)
                || stack.is(Items.COPPER_INGOT)
                || (ModItems.ALLOY_INGOTS.containsKey("bronze") && stack.is(ModItems.ALLOY_INGOTS.get("bronze").get()))
                || (ModItems.ALLOY_INGOTS.containsKey("brass") && stack.is(ModItems.ALLOY_INGOTS.get("brass").get()))
                || (ModItems.ALLOY_INGOTS.containsKey("steel") && stack.is(ModItems.ALLOY_INGOTS.get("steel").get()));
    }

    public static ItemStack heat(ItemStack original, ServerLevel server) {
        ItemStack result;
        if (original.is(Items.IRON_INGOT)) result = new ItemStack(ModItems.HEATED_IRON_INGOT.get());
        else if (original.is(Items.COPPER_INGOT)) result = new ItemStack(ModItems.HEATED_COPPER_INGOT.get());
        else if (ModItems.ALLOY_INGOTS.containsKey("bronze") && original.is(ModItems.ALLOY_INGOTS.get("bronze").get()))
            result = new ItemStack(ModItems.HEATED_BRONZE_INGOT.get());
        else if (ModItems.ALLOY_INGOTS.containsKey("brass") && original.is(ModItems.ALLOY_INGOTS.get("brass").get()))
            result = new ItemStack(ModItems.HEATED_BRASS_INGOT.get());
        else if (ModItems.ALLOY_INGOTS.containsKey("steel") && original.is(ModItems.ALLOY_INGOTS.get("steel").get()))
            result = new ItemStack(ModItems.HEATED_STEEL_INGOT.get());
        else return ItemStack.EMPTY;

        CompoundTag tag = new CompoundTag();
        tag.putLong("hot_until", server.getServer().overworld().getGameTime() + HOT_TICKS);
        result.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return result;
    }

    public static boolean isHeated(ItemStack stack) {
        return stack.is(ModItems.HEATED_IRON_INGOT.get())
                || stack.is(ModItems.HEATED_COPPER_INGOT.get())
                || stack.is(ModItems.HEATED_BRONZE_INGOT.get())
                || stack.is(ModItems.HEATED_BRASS_INGOT.get())
                || stack.is(ModItems.HEATED_STEEL_INGOT.get());
    }

    public static boolean isHot(ItemStack stack, ServerLevel server) {
        return isHeated(stack) && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getLong("hot_until").orElse(0L) > server.getServer().overworld().getGameTime();
    }

    public static ItemStack cooled(ItemStack stack) {
        if (stack.is(ModItems.HEATED_IRON_INGOT.get())) return new ItemStack(Items.IRON_INGOT, stack.getCount());
        if (stack.is(ModItems.HEATED_COPPER_INGOT.get())) return new ItemStack(Items.COPPER_INGOT, stack.getCount());
        if (stack.is(ModItems.HEATED_BRONZE_INGOT.get())) return new ItemStack(ModItems.ALLOY_INGOTS.get("bronze").get(), stack.getCount());
        if (stack.is(ModItems.HEATED_BRASS_INGOT.get())) return new ItemStack(ModItems.ALLOY_INGOTS.get("brass").get(), stack.getCount());
        if (stack.is(ModItems.HEATED_STEEL_INGOT.get())) return new ItemStack(ModItems.ALLOY_INGOTS.get("steel").get(), stack.getCount());
        return stack.copy();
    }
}
