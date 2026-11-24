package br.com.quantumindustries.block;

import br.com.quantumindustries.block.entity.MainspringDrumBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Tambor de Mola Helicoidal (Era I — Ep,e = 0.5 * k * x^2).
 */
public final class MainspringDrumBlock extends AxialTransmissionBlock {
    public static final MapCodec<MainspringDrumBlock> CODEC = simpleCodec(MainspringDrumBlock::new);

    public MainspringDrumBlock(Properties properties) {
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
        return new MainspringDrumBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit) {
        if (player instanceof ServerPlayer operator
                && level.getBlockEntity(pos) instanceof MainspringDrumBlockEntity drum) {
            drum.cycleMode();
            operator.sendOverlayMessage(drum.statusLine());
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
                        ModBlocks.MAINSPRING_DRUM_ENTITY.get(),
                        (l, p, s, be) -> be.tickServer());
    }
}
