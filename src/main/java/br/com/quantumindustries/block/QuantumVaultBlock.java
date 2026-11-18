package br.com.quantumindustries.block;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.block.entity.QuantumVaultBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class QuantumVaultBlock extends BaseEntityBlock {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public QuantumVaultBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(OPEN, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(QuantumVaultBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QuantumVaultBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        if (!(server.getBlockEntity(pos) instanceof QuantumVaultBlockEntity be)) {
            return InteractionResult.SUCCESS;
        }

        // If player holds an item that can be inserted into the Vault drawer:
        if (!player.isShiftKeyDown() && (be.storedCount() == 0 || ItemStack.isSameItemSameComponents(be.storedItem(), stack))) {
            int inserted = be.insert(stack);
            if (inserted > 0) {
                level.setBlock(pos, state.setValue(OPEN, true), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 0.6f, 1.3f);
                if (player instanceof ServerPlayer op) {
                    op.sendOverlayMessage(Component.translatable("message.quantumindustries.vault.stored", be.storedCount(), be.storedItem().getHoverName()));
                }
                return InteractionResult.SUCCESS;
            }
        }

        // Otherwise extract from the Vault drawer:
        return extractFromVault(state, server, pos, player, be);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        if (!(server.getBlockEntity(pos) instanceof QuantumVaultBlockEntity be)) {
            return InteractionResult.SUCCESS;
        }
        return extractFromVault(state, server, pos, player, be);
    }

    private InteractionResult extractFromVault(BlockState state, ServerLevel level, BlockPos pos,
                                               Player player, QuantumVaultBlockEntity be) {
        if (be.storedCount() <= 0) {
            if (player instanceof ServerPlayer op) {
                op.sendOverlayMessage(Component.translatable("message.quantumindustries.vault.empty"));
            }
            return InteractionResult.SUCCESS;
        }

        ItemStack extracted = be.extract(!player.isShiftKeyDown());
        if (!extracted.isEmpty()) {
            boolean wasGuidebook = extracted.is(ModItems.QUANTUM_GUIDEBOOK.get());
            player.getInventory().placeItemBackInInventory(extracted);
            level.setBlock(pos, state.setValue(OPEN, be.storedCount() > 0), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.7f, 1.2f);
            if (player instanceof ServerPlayer op) {
                if (wasGuidebook) {
                    op.sendOverlayMessage(Component.translatable("message.quantumindustries.vault_opened"));
                } else {
                    Component remaining = be.storedCount() > 0
                            ? Component.translatable("message.quantumindustries.vault.remaining", be.storedCount())
                            : Component.translatable("message.quantumindustries.vault.emptied");
                    op.sendOverlayMessage(Component.translatable("message.quantumindustries.vault.withdrawn", extracted.getCount(), extracted.getHoverName(), remaining));
                }
            }
        }
        return InteractionResult.SUCCESS;
    }
}
