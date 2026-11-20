package br.com.quantumindustries.item;

import br.com.quantumindustries.block.entity.MechanicalPulleyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;

public final class DriveBeltItem extends Item {
    private static final String X = "selected_pulley_x", Y = "selected_pulley_y", Z = "selected_pulley_z", DIMENSION = "selected_pulley_dimension";
    public DriveBeltItem(Properties properties) { super(properties); }
    @Override public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var player = context.getPlayer();
        if (player == null || !player.mayBuild() || !level.mayInteract(player, pos) || player.distanceToSqr(pos.getCenter()) > 64) return InteractionResult.FAIL;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (!(server.getBlockEntity(pos) instanceof MechanicalPulleyBlockEntity second) || second.partnerPos() != null) return message(context, "invalid_target");
        var stack = context.getItemInHand();
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String dimension = server.dimension().identifier().toString();
        if (!data.contains(X)) {
            data.putInt(X, pos.getX()); data.putInt(Y, pos.getY()); data.putInt(Z, pos.getZ()); data.putString(DIMENSION, dimension);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
            return message(context, "select_first");
        }
        BlockPos firstPos = new BlockPos(data.getIntOr(X, 0), data.getIntOr(Y, 0), data.getIntOr(Z, 0));
        if (!dimension.equals(data.getStringOr(DIMENSION, "")) || !server.hasChunkAt(firstPos)) return message(context, "invalid_target");
        if (firstPos.distManhattan(pos) > MechanicalPulleyBlockEntity.MAX_DISTANCE) return message(context, "too_far");
        if (!(server.getBlockEntity(firstPos) instanceof MechanicalPulleyBlockEntity first) || first.partnerPos() != null
                || !server.mayInteract(player, firstPos) || first.axle() != second.axle()
                || !br.com.quantumindustries.mechanical.BeltGeometry.validAlignment(firstPos, pos, first.axle())) return message(context, "invalid_target");
        boolean crossed = player.isShiftKeyDown();
        first.setPartner(pos, crossed); second.setPartner(firstPos, crossed);
        data.remove(X); data.remove(Y); data.remove(Z); data.remove(DIMENSION);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        stack.shrink(1);
        return message(context, crossed ? "linked_crossed" : "linked_open");
    }
    private static InteractionResult message(UseOnContext context, String key) {
        if (context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player) player.sendOverlayMessage(Component.translatable("message.quantumindustries.belt." + key));
        return InteractionResult.SUCCESS;
    }
}
