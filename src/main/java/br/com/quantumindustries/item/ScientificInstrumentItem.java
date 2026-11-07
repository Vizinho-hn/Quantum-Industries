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
    private final String assayPhase;
    private final String description;

    public ScientificInstrumentItem(String assayPhase, String description, Properties properties) {
        super(properties);
        this.assayPhase = assayPhase;
        this.description = description;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipOutput, TooltipFlag tooltipFlag) {
        tooltipOutput.accept(Component.literal("§d🔬 Instrumento de Ensaio Empírico"));
        tooltipOutput.accept(Component.literal("§7Fase do Ensaio: §b" + this.assayPhase));
        tooltipOutput.accept(Component.literal("§7" + this.description));
        tooltipOutput.accept(Component.literal("§8Utilizado nos slots de instrumentos da Bancada de Ensaio."));
        super.appendHoverText(stack, context, tooltipDisplay, tooltipOutput, tooltipFlag);
    }
}
