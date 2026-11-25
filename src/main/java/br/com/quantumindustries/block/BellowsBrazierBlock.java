package br.com.quantumindustries.block;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.block.entity.BellowsBrazierBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.research.WorldResearchData;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

public final class BellowsBrazierBlock extends BaseEntityBlock {
    public BellowsBrazierBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.LIT, false));
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state,
            net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof BellowsBrazierBlockEntity machine)
            machine.bindAutomation(player);
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(BellowsBrazierBlock::new); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.LIT);
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BellowsBrazierBlockEntity(pos, state);
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlocks.BELLOWS_BRAZIER_ENTITY.get(),
                (l, p, s, be) -> be.tick());
    }

    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValueOrElse(BlockStateProperties.LIT, false)) return;
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.35;
        double y = pos.getY() + 0.72;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.35;
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.015, 0.0);
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x, y + 0.08, z, 0.0, 0.025, 0.0);
        }
    }

    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer op)) return InteractionResult.SUCCESS;
        if (!WorldResearchData.get(server).knows(op, "thermal_annealing")) {
            op.sendOverlayMessage(Component.translatable("message.quantumindustries.brazier.research"));
            return InteractionResult.SUCCESS;
        }
        if (!(server.getBlockEntity(pos) instanceof BellowsBrazierBlockEntity be)) return InteractionResult.SUCCESS;

        if (!be.bindAutomation(player)) return InteractionResult.SUCCESS;
        if (stack.is(ModItems.MANUAL_BELLOWS.get())) {
            if (!be.puff()) {
                op.sendOverlayMessage(Component.translatable("message.quantumindustries.brazier.need_heat"));
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) {
            op.sendOverlayMessage(Component.translatable("message.quantumindustries.brazier.need_heat"));
            return InteractionResult.SUCCESS;
        }
        if (br.com.quantumindustries.material.HeatTreatment.canHeat(stack)) {
            if (be.insert(stack)) {
                server.playSound(null, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 0.45f, 1.1f);
                op.sendOverlayMessage(be.statusDetail());
            } else {
                op.sendOverlayMessage(Component.translatable("message.quantumindustries.brazier.busy"));
            }
        } else {
            op.sendOverlayMessage(Component.translatable("message.quantumindustries.brazier.busy"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && server.getBlockEntity(pos) instanceof BellowsBrazierBlockEntity be) {
            be.bindAutomation(player);
            ItemStack taken = be.take(player.isShiftKeyDown());
            if (!taken.isEmpty()) {
                player.getInventory().placeItemBackInInventory(taken);
                server.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.35f, 1.1f);
            } else if (player instanceof ServerPlayer op) {
                op.sendOverlayMessage(be.statusDetail());
            }
        }
        return InteractionResult.SUCCESS;
    }
}
