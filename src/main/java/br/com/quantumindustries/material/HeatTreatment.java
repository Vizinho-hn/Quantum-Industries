package br.com.quantumindustries.material;

import br.com.quantumindustries.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.Items;

/** Time-bounded thermal workability, not an infinite stockpile of molten metal. */
public final class HeatTreatment {
    public static final long HOT_TICKS = 600;
    private HeatTreatment() {}
    public static ItemStack heat(ItemStack original, ServerLevel server) {
        ItemStack result;
        if (original.is(Items.IRON_INGOT)) result = new ItemStack(ModItems.HEATED_IRON_INGOT.get());
        else if (original.is(Items.COPPER_INGOT)) result = new ItemStack(ModItems.HEATED_COPPER_INGOT.get());
        else return ItemStack.EMPTY;
        CompoundTag tag = new CompoundTag();
        tag.putLong("hot_until", server.getServer().overworld().getGameTime() + HOT_TICKS);
        result.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return result;
    }
    public static boolean isHeated(ItemStack stack) {
        return stack.is(ModItems.HEATED_IRON_INGOT.get()) || stack.is(ModItems.HEATED_COPPER_INGOT.get());
    }
    public static boolean isHot(ItemStack stack, ServerLevel server) {
        return isHeated(stack) && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getLong("hot_until").orElse(0L) > server.getServer().overworld().getGameTime();
    }
    public static ItemStack cooled(ItemStack stack) {
        if (stack.is(ModItems.HEATED_IRON_INGOT.get())) return new ItemStack(Items.IRON_INGOT, stack.getCount());
        if (stack.is(ModItems.HEATED_COPPER_INGOT.get())) return new ItemStack(Items.COPPER_INGOT, stack.getCount());
        return stack.copy();
    }
}
