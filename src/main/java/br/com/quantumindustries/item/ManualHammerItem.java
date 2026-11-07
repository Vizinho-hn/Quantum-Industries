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
 * Heavy industrial forming hammer for metal shaping, plate forging, and machine calibration.
 */
public class ManualHammerItem extends Item {
    public ManualHammerItem(Properties properties) {
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
            if (blockName.toLowerCase().contains("bench") || blockName.toLowerCase().contains("press") || blockName.toLowerCase().contains("sawmill")) {
                serverPlayer.sendOverlayMessage(Component.literal("§c[Martelo de Conformação] §7Mancais calibrados em §f" + blockName));
                ItemStack stack = context.getItemInHand();
                stack.hurtAndBreak(1, serverPlayer, player.getEquipmentSlotForItem(stack));
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipOutput, TooltipFlag tooltipFlag) {
        tooltipOutput.accept(Component.literal("§c🔨 Martelo de Conformação Manual"));
        tooltipOutput.accept(Component.literal("§7Ferramenta de percussão forjada para calibração"));
        tooltipOutput.accept(Component.literal("§7mecânica e conformação de chapas de alta densidade."));
        tooltipOutput.accept(Component.literal("§4Força de Impacto: §fCarga Pesada"));
        tooltipOutput.accept(Component.literal("§8Durabilidade Restante: §f" + (stack.getMaxDamage() - stack.getDamageValue()) + "/" + stack.getMaxDamage()));
        super.appendHoverText(stack, context, tooltipDisplay, tooltipOutput, tooltipFlag);
    }
}
