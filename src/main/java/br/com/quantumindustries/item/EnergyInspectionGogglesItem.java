package br.com.quantumindustries.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;

/**
 * Head-slot equippable goggles for inspecting electrical, flux, and mechanical machine states.
 */
public class EnergyInspectionGogglesItem extends Item {
    public EnergyInspectionGogglesItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipOutput, TooltipFlag tooltipFlag) {
        tooltipOutput.accept(Component.translatable("tooltip.quantumindustries.energyinspectiongoggles.line_0"));
        tooltipOutput.accept(Component.translatable("tooltip.quantumindustries.energyinspectiongoggles.line_1"));
        tooltipOutput.accept(Component.translatable("tooltip.quantumindustries.energyinspectiongoggles.line_2"));
        tooltipOutput.accept(Component.translatable("tooltip.quantumindustries.energyinspectiongoggles.line_3"));
        super.appendHoverText(stack, context, tooltipDisplay, tooltipOutput, tooltipFlag);
    }
}
