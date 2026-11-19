package br.com.quantumindustries.item;

import br.com.quantumindustries.logistics.ConfigurableConduit;
import br.com.quantumindustries.logistics.ConnectionMode;
import br.com.quantumindustries.logistics.ConnectionTarget;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.function.Consumer;

public final class ConfigurationWrenchItem extends Item {
    public ConfigurationWrenchItem(Properties properties) { super(properties); }

    @Override public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        if (!level.hasChunkAt(pos)) return InteractionResult.FAIL;
        var player = context.getPlayer();
        if (player == null || !player.mayBuild() || player.distanceToSqr(pos.getCenter()) > 64
                || !level.mayInteract(player, pos)) return InteractionResult.FAIL;

        if (level.getBlockEntity(pos) instanceof ConfigurableConduit conduit) {
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

        BlockState state = level.getBlockState(pos);
        if (!isRotatableModBlock(state)) return InteractionResult.PASS;
        BlockState rotated = rotateState(state, player.isShiftKeyDown());
        if (rotated == null || rotated == state) return InteractionResult.PASS;

        if (level instanceof ServerLevel server && player instanceof ServerPlayer operator) {
            server.setBlock(pos, rotated, Block.UPDATE_ALL);
            operator.sendOverlayMessage(Component.translatable("gui.quantumindustries.wrench.rotated",
                    rotationLabel(rotated)));
        }
        return InteractionResult.SUCCESS;
    }

    private static boolean isRotatableModBlock(BlockState state) {
        if (!"quantumindustries".equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace())) {
            return false;
        }
        if (state.hasProperty(BlockStateProperties.BED_PART)
                || state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            return false;
        }
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                || state.hasProperty(BlockStateProperties.FACING)
                || state.hasProperty(BlockStateProperties.AXIS);
    }

    private static BlockState rotateState(BlockState state, boolean reverse) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction current = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            return state.setValue(BlockStateProperties.HORIZONTAL_FACING,
                    reverse ? current.getCounterClockWise() : current.getClockWise());
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            Direction[] dirs = Direction.values();
            int idx = state.getValue(BlockStateProperties.FACING).ordinal();
            int next = (idx + (reverse ? dirs.length - 1 : 1)) % dirs.length;
            return state.setValue(BlockStateProperties.FACING, dirs[next]);
        }
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            Direction.Axis[] axes = Direction.Axis.values();
            int idx = state.getValue(BlockStateProperties.AXIS).ordinal();
            int next = (idx + (reverse ? axes.length - 1 : 1)) % axes.length;
            return state.setValue(BlockStateProperties.AXIS, axes[next]);
        }
        return null;
    }

    private static Component rotationLabel(BlockState state) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return Component.translatable("gui.quantumindustries.connection.face."
                    + state.getValue(BlockStateProperties.HORIZONTAL_FACING).getName());
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return Component.translatable("gui.quantumindustries.connection.face."
                    + state.getValue(BlockStateProperties.FACING).getName());
        }
        return Component.literal(state.getValue(BlockStateProperties.AXIS).getName().toUpperCase(java.util.Locale.ROOT));
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.quantumindustries.configuration_wrench.use"));
        tooltip.accept(Component.translatable("tooltip.quantumindustries.configuration_wrench.reset"));
        tooltip.accept(Component.translatable("tooltip.quantumindustries.configuration_wrench.rotate"));
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
