package br.com.quantumindustries.block;

import br.com.quantumindustries.block.entity.GravityWinchBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Guincho de Contrapeso Gravitacional (Era I — Ep,g = m * g * h).
 */
public final class GravityWinchBlock extends AxialTransmissionBlock {
    public static final MapCodec<GravityWinchBlock> CODEC = simpleCodec(GravityWinchBlock::new);

    public GravityWinchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GravityWinchBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        GravityWinchBlockEntity.MassTier candidate = GravityWinchBlockEntity.MassTier.fromItem(stack);
        if (candidate == GravityWinchBlockEntity.MassTier.NONE) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (player instanceof ServerPlayer operator
                && level.getBlockEntity(pos) instanceof GravityWinchBlockEntity winch) {
            winch.installMass(candidate, operator, stack);
            operator.sendOverlayMessage(winch.statusLine());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit) {
        if (player instanceof ServerPlayer operator
                && level.getBlockEntity(pos) instanceof GravityWinchBlockEntity winch) {
            if (player.isShiftKeyDown()) {
                winch.removeMass(operator);
            } else {
                winch.cycleMode();
            }
            operator.sendOverlayMessage(winch.statusLine());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(
                        type,
                        ModBlocks.GRAVITY_WINCH_ENTITY.get(),
                        (l, p, s, be) -> be.tickServer());
    }
}
