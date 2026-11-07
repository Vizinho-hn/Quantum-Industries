package br.com.quantumindustries.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.List;
import java.util.function.Consumer;

/**
 * Industrial component item with technical physical properties and engineering lore.
 */
public class ComponentMaterialItem extends Item {
    private final String title;
    private final List<String> technicalSpecs;

    public ComponentMaterialItem(String title, List<String> technicalSpecs, Properties properties) {
        super(properties);
        this.title = title;
        this.technicalSpecs = technicalSpecs;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipOutput, TooltipFlag tooltipFlag) {
        tooltipOutput.accept(Component.literal(this.title));
        for (String spec : this.technicalSpecs) {
            tooltipOutput.accept(Component.literal(spec));
        }
        super.appendHoverText(stack, context, tooltipDisplay, tooltipOutput, tooltipFlag);
    }
}
