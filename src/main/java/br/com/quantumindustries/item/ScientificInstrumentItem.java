package br.com.quantumindustries.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;

/**
 * Empirical scientific instrument for the 4-step material assay workbench.
 */
public class ScientificInstrumentItem extends Item {
    private final Component assayPhase;
    private final Component description;

    public ScientificInstrumentItem(Component assayPhase, Component description, Properties properties) {
        super(properties);
        this.assayPhase = assayPhase;
        this.description = description;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipOutput, TooltipFlag tooltipFlag) {
        tooltipOutput.accept(Component.translatable("tooltip.quantumindustries.instrument.header"));
        tooltipOutput.accept(Component.translatable("tooltip.quantumindustries.instrument.phase", this.assayPhase));
        tooltipOutput.accept(this.description);
        tooltipOutput.accept(Component.translatable("tooltip.quantumindustries.instrument.usage"));
        super.appendHoverText(stack, context, tooltipDisplay, tooltipOutput, tooltipFlag);
    }
}
