package br.com.quantumindustries.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.function.Consumer;

/**
 * Diagnostic probes to inspect blocks, machines, and conduits in Quantum Industries.
 */
public class TestProbesItem extends Item {
    public TestProbesItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        BlockState state = level.getBlockState(pos);

        if (level instanceof ServerLevel && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.quantumindustries.probe.target", state.getBlock().getName(), pos.getX(), pos.getY(), pos.getZ()));
            ItemStack stack = context.getItemInHand();
            stack.hurtAndBreak(1, serverPlayer, player.getEquipmentSlotForItem(stack));
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipOutput, TooltipFlag tooltipFlag) {
        tooltipOutput.accept(Component.translatable("tooltip.quantumindustries.testprobes.line_0"));
        tooltipOutput.accept(Component.translatable("tooltip.quantumindustries.testprobes.line_1"));
        tooltipOutput.accept(Component.translatable("tooltip.quantumindustries.testprobes.line_2"));
        tooltipOutput.accept(Component.translatable("tooltip.quantumindustries.probe.cycles", stack.getMaxDamage() - stack.getDamageValue(), stack.getMaxDamage()));
        super.appendHoverText(stack, context, tooltipDisplay, tooltipOutput, tooltipFlag);
    }
}
