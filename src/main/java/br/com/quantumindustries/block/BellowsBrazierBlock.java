package br.com.quantumindustries.block;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.block.entity.BellowsBrazierBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.research.WorldResearchData;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.phys.BlockHitResult;

public final class BellowsBrazierBlock extends BaseEntityBlock {
    public BellowsBrazierBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(BellowsBrazierBlock::new); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BellowsBrazierBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlocks.BELLOWS_BRAZIER_ENTITY.get(),
                (l,p,s,be) -> be.tick());
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.is(Items.COAL) || stack.is(Items.IRON_INGOT) || stack.is(Items.COPPER_INGOT)
                || stack.is(ModItems.MANUAL_BELLOWS.get()))) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer op)) return InteractionResult.SUCCESS;
        if (!WorldResearchData.get(server).knows(op, "thermal_annealing")) {
            op.sendOverlayMessage(Component.translatable("message.quantumindustries.brazier.research"));
            return InteractionResult.SUCCESS;
        }
        if (server.getBlockEntity(pos) instanceof BellowsBrazierBlockEntity be) {
            if (stack.is(ModItems.MANUAL_BELLOWS.get())) be.puff();
            else if (!be.insert(stack)) op.sendOverlayMessage(Component.translatable("message.quantumindustries.brazier.busy"));
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && server.getBlockEntity(pos) instanceof BellowsBrazierBlockEntity be) {
            ItemStack taken = be.take(player.isShiftKeyDown());
            if (!taken.isEmpty()) player.getInventory().placeItemBackInInventory(taken);
            else if (player instanceof ServerPlayer op) op.sendOverlayMessage(be.status());
        }
        return InteractionResult.SUCCESS;
    }
}
