package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.logistics.ItemRouting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Gravity sorter: light resources to the right, dense ingots/sheets forward. */
public final class GravitationalSorterBlockEntity extends BlockEntity {
    private ItemStack packet=ItemStack.EMPTY;
    private boolean reversed;
    private int progress;
    public GravitationalSorterBlockEntity(BlockPos pos,BlockState state){
        super(ModBlocks.GRAVITATIONAL_SORTER_ENTITY.get(),pos,state);
    }
    public boolean accept(ItemStack stack,Direction from){
        if(!(level instanceof ServerLevel)||!packet.isEmpty()||stack.isEmpty())return false;
        packet=stack.copy();stack.setCount(0);progress=0;changed();return true;
    }
    public void toggleSieve(){if(level instanceof ServerLevel){reversed=!reversed;changed();}}
    public Component status(){return Component.translatable("message.quantumindustries.sorter.status",reversed?"B":"A");}
    private boolean light(ItemStack item){
        return item.is(Items.REDSTONE)||item.is(Items.GLOWSTONE_DUST)||item.is(Items.GUNPOWDER)
                ||item.is(Items.LAPIS_LAZULI)||item.is(Items.SUGAR)||item.is(Items.BLAZE_POWDER);
    }
    public void tick(){
        if(!(level instanceof ServerLevel server)||packet.isEmpty()||++progress<10)return;
        Direction forward=getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        boolean lightweight=light(packet);
        Direction output=(lightweight^reversed)?forward.getClockWise():forward;
        if(ItemRouting.insert(server,worldPosition.relative(output),packet,output.getOpposite())){
            if(packet.isEmpty()){packet=ItemStack.EMPTY;progress=0;}
            changed();
        }else progress=10;
    }
    private void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);}
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);if(!packet.isEmpty())out.store("packet",ItemStack.CODEC,packet);
        out.putBoolean("reversed",reversed);out.putInt("progress",progress);
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);packet=in.read("packet",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        reversed=in.getBooleanOr("reversed",false);progress=Math.clamp(in.getIntOr("progress",0),0,10);
    }
    @Override public void preRemoveSideEffects(BlockPos pos,BlockState state){
        super.preRemoveSideEffects(pos,state);
        if(level instanceof ServerLevel server&&!packet.isEmpty()){Block.popResource(server,pos,packet.copy());packet=ItemStack.EMPTY;}
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registry){return saveCustomOnly(registry);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
