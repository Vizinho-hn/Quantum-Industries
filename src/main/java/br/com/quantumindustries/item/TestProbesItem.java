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
            String blockName = state.getBlock().getName().getString();
            serverPlayer.sendOverlayMessage(Component.literal("§e[Sonda de Diagnóstico] §fAlvo: §b" + blockName + " §7@ (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")"));
            ItemStack stack = context.getItemInHand();
            stack.hurtAndBreak(1, serverPlayer, player.getEquipmentSlotForItem(stack));
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipOutput, TooltipFlag tooltipFlag) {
        tooltipOutput.accept(Component.literal("§e🔍 Sonda de Diagnóstico Quântico"));
        tooltipOutput.accept(Component.literal("§7Clique com botão direito em máquinas ou condutores"));
        tooltipOutput.accept(Component.literal("§7para aferir integridade estrutural e continuidade de sinal."));
        tooltipOutput.accept(Component.literal("§8Ciclos de Teste: §f" + (stack.getMaxDamage() - stack.getDamageValue()) + "/" + stack.getMaxDamage()));
        super.appendHoverText(stack, context, tooltipDisplay, tooltipOutput, tooltipFlag);
    }
}
