package br.com.quantumindustries.block;

import br.com.quantumindustries.block.entity.ElectricFurnaceBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.menu.ElectricFurnaceMenu;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
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

public final class ElectricFurnaceBlock extends BaseEntityBlock {
    public ElectricFurnaceBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(BlockStateProperties.LIT, false));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(ElectricFurnaceBlock::new); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof ElectricFurnaceBlockEntity furnace) {
            furnace.bind(player);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectricFurnaceBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlocks.ELECTRIC_FURNACE_ENTITY.get(),
                (world, pos, current, furnace) -> furnace.tick());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(BlockStateProperties.LIT) || random.nextInt(6) != 0) return;
        Direction face = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        level.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + 0.5 + face.getStepX() * 0.53,
                pos.getY() + 0.48, pos.getZ() + 0.5 + face.getStepZ() * 0.53, 0, 0.015, 0);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof ElectricFurnaceBlockEntity furnace) {
            furnace.bind(player);
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inventory, owner) -> new ElectricFurnaceMenu(id, inventory, pos),
                    Component.translatable(ElectricFurnaceBlockEntity.TITLE_KEY)), pos);
        }
        return InteractionResult.SUCCESS;
    }
}
