package br.com.quantumindustries.item;

import br.com.quantumindustries.logistics.ConfigurableConduit;
import br.com.quantumindustries.logistics.ConnectionMode;
import br.com.quantumindustries.logistics.ConnectionTarget;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import java.util.function.Consumer;

public final class ConfigurationWrenchItem extends Item {
    public ConfigurationWrenchItem(Properties properties) { super(properties); }

    @Override public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        if (!level.hasChunkAt(pos)) return InteractionResult.FAIL;
        if (!(level.getBlockEntity(pos) instanceof ConfigurableConduit conduit)) return InteractionResult.PASS;
        var player = context.getPlayer();
        if (player == null || !player.mayBuild() || player.distanceToSqr(pos.getCenter()) > 64
                || !level.mayInteract(player, pos)) return InteractionResult.FAIL;
        if (level instanceof ServerLevel && player instanceof ServerPlayer operator) {
            var side = ConnectionTarget.face(pos, context.getClickLocation(), context.getClickedFace());
            var mode = player.isShiftKeyDown() ? ConnectionMode.AUTO : conduit.connectionMode(side).next();
            conduit.connectionModes().set(side, mode);
            conduit.connectionChanged();
            operator.sendOverlayMessage(Component.translatable("gui.quantumindustries.connection.changed",
                    Component.translatable("gui.quantumindustries.connection.face." + side.getName()), mode.label()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.quantumindustries.configuration_wrench.use"));
        tooltip.accept(Component.translatable("tooltip.quantumindustries.configuration_wrench.reset"));
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
