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
        tooltipOutput.accept(Component.literal("§b⚡ Inspeção Eletromecânica & Quântica"));
        tooltipOutput.accept(Component.literal("§7Equipável na cabeça para revelar vetores de fluxo,"));
        tooltipOutput.accept(Component.literal("§7tensão e status operacional de máquinas industriais."));
        tooltipOutput.accept(Component.literal("§6Calibração Óptica: §eTier 1 (Empírico)"));
        super.appendHoverText(stack, context, tooltipDisplay, tooltipOutput, tooltipFlag);
    }
}
