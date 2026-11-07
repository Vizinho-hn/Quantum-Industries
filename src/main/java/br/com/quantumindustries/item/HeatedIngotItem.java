package br.com.quantumindustries.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;

/**
 * Incandescent metal ingot ready for mechanical pressing or manual hammering.
 */
public class HeatedIngotItem extends Item {
    private final String metalType;
    private final int estimatedTemperature;

    public HeatedIngotItem(String metalType, int estimatedTemperature, Properties properties) {
        super(properties);
        this.metalType = metalType;
        this.estimatedTemperature = estimatedTemperature;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipOutput, TooltipFlag tooltipFlag) {
        tooltipOutput.accept(Component.literal("§6🔥 Forjado a Quente (Incandescente)"));
        tooltipOutput.accept(Component.literal("§7Metal: §f" + this.metalType));
        tooltipOutput.accept(Component.literal("§cTemperatura Estimada: §e~" + this.estimatedTemperature + "°C"));
        tooltipOutput.accept(Component.literal("§8Processar na Prensa Eletromecânica antes do resfriamento."));
        super.appendHoverText(stack, context, tooltipDisplay, tooltipOutput, tooltipFlag);
    }
}
