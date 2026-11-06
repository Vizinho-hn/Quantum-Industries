package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.fluid.FluidInspectable;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** 8-bucket working vat; produces bottled concentrate and solid sediment. */
public final class SettlingVatBlockEntity extends BlockEntity implements FluidInspectable {
    public static final int CAPACITY=8000,RECIPE_WATER=1000,WORK_TICKS=120;
    private final FluidStacksResourceHandler tank=new FluidStacksResourceHandler(1,CAPACITY){
        @Override protected void onContentsChanged(int slot,net.neoforged.neoforge.fluids.FluidStack stack){
            SettlingVatBlockEntity.this.changed();
        }
    };
    private int lapis,bottles,progress;
    private ItemStack concentrate=ItemStack.EMPTY,sediment=ItemStack.EMPTY;
    public SettlingVatBlockEntity(BlockPos pos,BlockState state){super(ModBlocks.SETTLING_VAT_ENTITY.get(),pos,state);}
    public FluidStacksResourceHandler tank(){return tank;}
    @Override public int storedFluid(){return tank.getAmountAsInt(0);}
    @Override public int fluidCapacity(){return CAPACITY;}
    @Override public Component fluidTitle(){return Component.translatable("block.quantumindustries.settling_vat");}
    @Override public Component fluidDetail(){
        return Component.translatable("message.quantumindustries.vat.status",storedFluid(),CAPACITY,lapis,bottles,progress);
    }
    public boolean addIngredient(ItemStack stack){
        if(!(level instanceof ServerLevel))return false;
        if(stack.is(Items.LAPIS_LAZULI)&&lapis<64){lapis++;stack.shrink(1);changed();return true;}
        if(stack.is(Items.GLASS_BOTTLE)&&bottles<64){bottles++;stack.shrink(1);changed();return true;}
        return false;
    }
    public ItemStack collect(){
        if(!(level instanceof ServerLevel))return ItemStack.EMPTY;
        if(!concentrate.isEmpty()){ItemStack taken=concentrate.copy();concentrate=ItemStack.EMPTY;changed();return taken;}
        if(!sediment.isEmpty()){ItemStack taken=sediment.copy();sediment=ItemStack.EMPTY;changed();return taken;}
        return ItemStack.EMPTY;
    }
    public void tick(){
        if(!(level instanceof ServerLevel))return;
        boolean ready=lapis>0&&bottles>0&&storedFluid()>=RECIPE_WATER
                &&tank.getResource(0).is(Fluids.WATER)
                &&(concentrate.isEmpty()||concentrate.getCount()<64)
                &&(sediment.isEmpty()||sediment.getCount()<64);
        if(!ready)return;
        if(++progress<WORK_TICKS){if(progress%20==0)changed();return;}
        try(var tx=Transaction.openRoot()){
            int taken=tank.extract(FluidResource.of(Fluids.WATER),RECIPE_WATER,tx);
            if(taken!=RECIPE_WATER){progress=0;return;}
            tx.commit();
        }
        lapis--;bottles--;progress=0;
        if(concentrate.isEmpty())concentrate=new ItemStack(ModItems.GALVANIC_ELECTROLYTE.get());
        else concentrate.grow(1);
        if(sediment.isEmpty())sediment=new ItemStack(ModItems.PIGMENT_SEDIMENT.get());
        else sediment.grow(1);
        changed();
    }
    private void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);}
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);tank.serialize(out);out.putInt("lapis",lapis);
        out.putInt("bottles",bottles);out.putInt("progress",progress);
        if(!concentrate.isEmpty())out.store("concentrate",ItemStack.CODEC,concentrate);
        if(!sediment.isEmpty())out.store("sediment",ItemStack.CODEC,sediment);
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);tank.deserialize(in);lapis=Math.clamp(in.getIntOr("lapis",0),0,64);
        bottles=Math.clamp(in.getIntOr("bottles",0),0,64);
        progress=Math.clamp(in.getIntOr("progress",0),0,WORK_TICKS);
        concentrate=in.read("concentrate",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        sediment=in.read("sediment",ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }
    @Override public void preRemoveSideEffects(BlockPos pos,BlockState state){
        super.preRemoveSideEffects(pos,state);
        if(!(level instanceof ServerLevel server))return;
        if(lapis>0)Block.popResource(server,pos,new ItemStack(Items.LAPIS_LAZULI,lapis));
        if(bottles>0)Block.popResource(server,pos,new ItemStack(Items.GLASS_BOTTLE,bottles));
        if(!concentrate.isEmpty())Block.popResource(server,pos,concentrate.copy());
        if(!sediment.isEmpty())Block.popResource(server,pos,sediment.copy());
        lapis=bottles=0;concentrate=sediment=ItemStack.EMPTY;
        // Fluid contents are intentionally not materialized as item drops.
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveCustomOnly(r);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
