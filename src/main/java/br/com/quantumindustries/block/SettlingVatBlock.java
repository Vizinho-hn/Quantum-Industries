package br.com.quantumindustries.block;

import br.com.quantumindustries.block.entity.SettlingVatBlockEntity;
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
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

public final class SettlingVatBlock extends BaseEntityBlock {
    public SettlingVatBlock(Properties properties){super(properties);}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(SettlingVatBlock::new);}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState s){return new SettlingVatBlockEntity(pos,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState s,BlockEntityType<T> type){
        return level.isClientSide()?null:createTickerHelper(type,ModBlocks.SETTLING_VAT_ENTITY.get(),(l,p,st,be)->be.tick());
    }
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,
            Player player,InteractionHand hand,BlockHitResult hit){
        if(!(level instanceof ServerLevel server)||!(player instanceof ServerPlayer op))return InteractionResult.SUCCESS;
        if(!WorldResearchData.get(server).knows(op,"fluid_hydrostatics")){
            op.sendOverlayMessage(Component.translatable("message.quantumindustries.vat.research"));
            return InteractionResult.SUCCESS;
        }
        if(stack.is(Items.LAPIS_LAZULI)||stack.is(Items.GLASS_BOTTLE)){
            if(server.getBlockEntity(pos) instanceof SettlingVatBlockEntity vat)vat.addIngredient(stack);
            return InteractionResult.SUCCESS;
        }
        if(FluidUtil.interactWithFluidHandler(player,hand,level,pos,hit.getDirection(),null))
            return InteractionResult.SUCCESS;
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,
            Player player,BlockHitResult hit){
        if(level instanceof ServerLevel server&&server.getBlockEntity(pos) instanceof SettlingVatBlockEntity vat){
            ItemStack result=vat.collect();
            if(!result.isEmpty())player.getInventory().placeItemBackInInventory(result);
            else if(player instanceof ServerPlayer op)op.sendOverlayMessage(vat.fluidDetail());
        }
        return InteractionResult.SUCCESS;
    }
}
