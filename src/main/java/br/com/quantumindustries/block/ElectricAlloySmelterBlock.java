package br.com.quantumindustries.block;

import br.com.quantumindustries.block.entity.ElectricAlloySmelterBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;

public final class ElectricAlloySmelterBlock extends BaseEntityBlock {
    public ElectricAlloySmelterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(BlockStateProperties.LIT, false));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(ElectricAlloySmelterBlock::new); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.LIT);
    }

    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValueOrElse(BlockStateProperties.LIT, false)) return;
        if (random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.02, pos.getZ() + 0.5, 0.0, 0.03, 0.0);
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.FLAME,
                    pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.25,
                    pos.getY() + 0.55,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.25,
                    0.0, 0.01, 0.0);
        }
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectricAlloySmelterBlockEntity(pos, state);
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlocks.ELECTRIC_ALLOY_SMELTER_ENTITY.get(),
                (l, p, s, be) -> be.tick());
    }

    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer op)
                || !(server.getBlockEntity(pos) instanceof ElectricAlloySmelterBlockEntity smelter)) {
            return InteractionResult.SUCCESS;
        }
        op.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (id, inventory, owner) -> new br.com.quantumindustries.menu.MachineProcessingMenu(id, inventory, pos, false),
                Component.translatable("block.quantumindustries.electric_alloy_smelter")), pos);
        return InteractionResult.SUCCESS;
    }
}
