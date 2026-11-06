package br.com.quantumindustries.block;

import br.com.quantumindustries.block.entity.ResearchTableBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class ResearchTableBlock extends BaseEntityBlock {
    public static final Property<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 14, 16);

    public ResearchTableBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(ResearchTableBlock::new);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ResearchTableBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return openResearchMenu(level, pos, player);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Research is handled by the world-level research profile, not a ticking block entity.
        return null;
    }

    @Override
    public InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand == InteractionHand.MAIN_HAND && itemStack.is(net.minecraft.world.item.Items.FEATHER)
                && br.com.quantumindustries.assay.AssayCatalog.material(player.getOffhandItem()) != null) {
            if (level instanceof net.minecraft.server.level.ServerLevel server) {
                var material = br.com.quantumindustries.assay.AssayCatalog.material(player.getOffhandItem());
                var profile = br.com.quantumindustries.assay.AssayProfile.of(player);
                if (profile.mask(material) != br.com.quantumindustries.assay.AssayCatalog.COMPLETE_MASK) {
                    ((net.minecraft.server.level.ServerPlayer) player).sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                            "message.quantumindustries.field_notes.incomplete"));
                    return InteractionResult.SUCCESS;
                }
                int paperSlot = -1;
                for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                    if (player.getInventory().getItem(slot).is(net.minecraft.world.item.Items.PAPER)) {
                        paperSlot = slot;
                        break;
                    }
                }
                if (paperSlot < 0 && !player.getAbilities().instabuild) {
                    ((net.minecraft.server.level.ServerPlayer) player).sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                            "message.quantumindustries.field_notes.paper"));
                    return InteractionResult.SUCCESS;
                }
                if (!player.getAbilities().instabuild) {
                    itemStack.shrink(1);
                    player.getInventory().getItem(paperSlot).shrink(1);
                }
                player.getInventory().placeItemBackInInventory(
                        br.com.quantumindustries.assay.EmpiricalFieldNotesItem.create(material));
                ((net.minecraft.server.level.ServerPlayer) player).sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                        "message.quantumindustries.field_notes.written"));
            }
            return InteractionResult.SUCCESS;
        }
        return openResearchMenu(level, pos, player);
    }

    private InteractionResult openResearchMenu(Level level, BlockPos pos, Player player) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof MenuProvider menuProvider)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            player.openMenu(menuProvider, pos);
        }
        return InteractionResult.SUCCESS;
    }
}