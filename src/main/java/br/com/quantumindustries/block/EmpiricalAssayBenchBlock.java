package br.com.quantumindustries.block;

import br.com.quantumindustries.menu.EmpiricalAssayBenchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.BlockHitResult;

public class EmpiricalAssayBenchBlock extends Block {
    public EmpiricalAssayBenchBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH).setValue(BlockStateProperties.BED_PART, BedPart.FOOT));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.BED_PART);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing=context.getHorizontalDirection().getOpposite();
        BlockPos other=context.getClickedPos().relative(facing.getClockWise());
        if(!context.getLevel().getWorldBorder().isWithinBounds(other)||!context.getLevel().getBlockState(other).canBeReplaced(context))return null;
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
    }
    private Direction partnerDirection(BlockState state) {
        Direction right=state.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise();
        return state.getValue(BlockStateProperties.BED_PART)==BedPart.FOOT?right:right.getOpposite();
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack) {
        super.setPlacedBy(level,pos,state,placer,stack);
        if(!level.isClientSide())level.setBlock(pos.relative(partnerDirection(state)),state.setValue(BlockStateProperties.BED_PART,BedPart.HEAD),3);
    }
    @Override protected BlockState updateShape(BlockState state,LevelReader level,ScheduledTickAccess ticks,BlockPos pos,Direction direction,BlockPos neighborPos,BlockState neighbor,RandomSource random) {
        if(direction==partnerDirection(state)&&(neighbor.getBlock()!=this||neighbor.getValue(BlockStateProperties.BED_PART)==state.getValue(BlockStateProperties.BED_PART)||neighbor.getValue(BlockStateProperties.HORIZONTAL_FACING)!=state.getValue(BlockStateProperties.HORIZONTAL_FACING)))return Blocks.AIR.defaultBlockState();
        return super.updateShape(state,level,ticks,pos,direction,neighborPos,neighbor,random);
    }
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player) {
        if(!level.isClientSide()){
            BlockPos other=pos.relative(partnerDirection(state));
            if(level.getBlockState(other).is(this))level.destroyBlock(other,!player.isCreative()&&state.getValue(BlockStateProperties.BED_PART)==BedPart.HEAD,player);
        }
        return super.playerWillDestroy(level,pos,state,player);
    }
    private InteractionResult open(Level level, BlockPos pos, Player player) {
        BlockState state=level.getBlockState(pos);
        if(state.getValue(BlockStateProperties.BED_PART)==BedPart.HEAD)pos=pos.relative(partnerDirection(state));
        final BlockPos anchor=pos;
        if (!level.isClientSide()) player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new EmpiricalAssayBenchMenu(id, inv, anchor),
            Component.literal("Bancada de Ensaio Empírico")), pos);
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return open(level, pos, player);
    }
    @Override public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return open(level, pos, player);
    }
}
