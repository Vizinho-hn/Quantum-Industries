package br.com.quantumindustries.block;

import br.com.quantumindustries.block.entity.ElectricSawmillBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
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

public final class ElectricSawmillBlock extends BaseEntityBlock {
  public ElectricSawmillBlock(Properties properties) {
    super(properties);
    registerDefaultState(
        stateDefinition
            .any()
            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
            .setValue(BlockStateProperties.LIT, false));
  }

  @Override
  protected MapCodec<? extends BaseEntityBlock> codec() {
    return simpleCodec(ElectricSawmillBlock::new);
  }

  @Override
  protected RenderShape getRenderShape(BlockState state) {
    return RenderShape.INVISIBLE;
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.LIT);
  }

  @Override
  public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
    if (!state.getValueOrElse(BlockStateProperties.LIT, false) || random.nextInt(6) != 0) return;
    if (level.getBlockEntity(pos) instanceof ElectricSawmillBlockEntity saw
        && saw.activeSlot() >= 0) {
      var input = saw.inventory().getItem(saw.activeSlot());
      if (!input.isEmpty())
        level.addParticle(
            new net.minecraft.core.particles.ItemParticleOption(
                ParticleTypes.ITEM, input.getItem()),
            pos.getX() + .5,
            pos.getY() + .65,
            pos.getZ() + .5,
            .02,
            .015,
            .01);
    }
  }

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    return defaultBlockState()
        .setValue(
            BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new ElectricSawmillBlockEntity(pos, state);
  }

  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      Level level, BlockState state, BlockEntityType<T> type) {
    return level.isClientSide()
        ? null
        : createTickerHelper(
            type, ModBlocks.ELECTRIC_SAWMILL_ENTITY.get(), (l, p, s, be) -> be.tick());
  }

  @Override
  public void setPlacedBy(
      Level level,
      BlockPos pos,
      BlockState state,
      net.minecraft.world.entity.LivingEntity placer,
      ItemStack stack) {
    super.setPlacedBy(level, pos, state, placer, stack);
    if (placer instanceof Player player
        && level.getBlockEntity(pos) instanceof ElectricSawmillBlockEntity saw) saw.bind(player);
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
    return InteractionResult.TRY_WITH_EMPTY_HAND;
  }

  @Override
  protected InteractionResult useWithoutItem(
      BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    if (!(level instanceof ServerLevel server)
        || !(player instanceof ServerPlayer op)
        || !(server.getBlockEntity(pos) instanceof ElectricSawmillBlockEntity smelter)) {
      return InteractionResult.SUCCESS;
    }
    smelter.bind(player);
    op.openMenu(
        new net.minecraft.world.SimpleMenuProvider(
            (id, inventory, owner) ->
                new br.com.quantumindustries.menu.ElectricSawmillMenu(id, inventory, pos),
            Component.translatable("block.quantumindustries.electric_sawmill")),
        pos);
    return InteractionResult.SUCCESS;
  }
}
