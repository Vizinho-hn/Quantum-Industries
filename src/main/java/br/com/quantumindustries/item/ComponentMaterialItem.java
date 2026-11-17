package br.com.quantumindustries.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Industrial component item with technical physical properties and engineering lore.
 */
public class ComponentMaterialItem extends Item {
    private final String title;
    private final List<String> technicalSpecs;
    private final Supplier<? extends BlockItem> blockDelegate;

    public ComponentMaterialItem(String title, List<String> technicalSpecs, Properties properties) {
        this(title, technicalSpecs, properties, null);
    }

    public ComponentMaterialItem(String title, List<String> technicalSpecs, Properties properties, Supplier<? extends BlockItem> blockDelegate) {
        super(properties);
        this.title = title;
        this.technicalSpecs = technicalSpecs;
        this.blockDelegate = blockDelegate;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (this.blockDelegate != null) {
            return this.blockDelegate.get().useOn(context);
        }
        return super.useOn(context);
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
