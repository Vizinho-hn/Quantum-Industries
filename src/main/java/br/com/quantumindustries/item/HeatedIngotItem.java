package br.com.quantumindustries.item;

import br.com.quantumindustries.material.HeatTreatment;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Incandescent metal ingot ready for mechanical pressing or manual hammering.
 * Automatically reverts to its base cold ingot once its thermal window expires.
 */
public class HeatedIngotItem extends Item {
    private final Component metalType;
    private final int estimatedTemperature;

    public HeatedIngotItem(Component metalType, int estimatedTemperature, Properties properties) {
        super(properties);
        this.metalType = metalType;
        this.estimatedTemperature = estimatedTemperature;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (!stack.has(DataComponents.CUSTOM_DATA)) {
            CompoundTag tag = new CompoundTag();
            tag.putLong("hot_until", level.getServer().overworld().getGameTime() + HeatTreatment.HOT_TICKS);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            return;
        }
        if (HeatTreatment.isHot(stack, level)) return;

        ItemStack cooled = HeatTreatment.cooled(stack);
        if (slot != null && entity instanceof LivingEntity living && living.getItemBySlot(slot) == stack) {
            living.setItemSlot(slot, cooled);
            return;
        }
        if (entity instanceof Player player) {
            var inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                if (inv.getItem(i) == stack) {
                    inv.setItem(i, cooled);
                    return;
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipOutput, TooltipFlag tooltipFlag) {
        tooltipOutput.accept(Component.translatable("tooltip.quantumindustries.heated.header"));
        tooltipOutput.accept(Component.translatable("tooltip.quantumindustries.heated.metal", this.metalType));
        tooltipOutput.accept(Component.translatable("tooltip.quantumindustries.heated.temperature", this.estimatedTemperature));
        tooltipOutput.accept(Component.translatable("tooltip.quantumindustries.heated.usage"));
        super.appendHoverText(stack, context, tooltipDisplay, tooltipOutput, tooltipFlag);
    }
}
