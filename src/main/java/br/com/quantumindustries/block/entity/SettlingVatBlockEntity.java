package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.logistics.MachineItemSlot;
import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.fluid.FluidInspectable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
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
public final class SettlingVatBlockEntity extends BlockEntity implements FluidInspectable, MachineStatusInspectable {
    private String automationOperator = "";
    public boolean automationAuthorized() {
        return ProcessingAuthorization.permitted(level, automationOperator, "fluid_hydrostatics");
    }
    public boolean bindAutomation(net.minecraft.world.entity.player.Player player) {
        if (!ProcessingAuthorization.canBind(level, worldPosition, player, "fluid_hydrostatics")) return false;
        if (!automationAuthorized()) {
            automationOperator = player.getUUID().toString();
            changed();
        }
        return automationAuthorized();
    }

    public static final int CAPACITY=8000,RECIPE_WATER=1000,WORK_TICKS=120;
    private final FluidStacksResourceHandler tank=new FluidStacksResourceHandler(1,CAPACITY){
        @Override protected void onContentsChanged(int slot,net.neoforged.neoforge.fluids.FluidStack stack){
            SettlingVatBlockEntity.this.changed();
        }
    };
    private int lapis,sulfur,halite,bottles,progress;
    private ItemStack concentrate=ItemStack.EMPTY,sediment=ItemStack.EMPTY;
    private final ResourceHandler<ItemResource> itemPort = new CombinedResourceHandler<>(
            ingredientSlot(() -> Items.LAPIS_LAZULI, () -> lapis, n -> lapis = n, () -> halite == 0, () -> 64 - sulfur),
            ingredientSlot(() -> ModItems.CHEMICAL_PRODUCTS.get("sulfur").get(),
                    () -> sulfur, n -> sulfur = n, () -> halite == 0, () -> 64 - lapis),
            ingredientSlot(() -> ModItems.CHEMICAL_PRODUCTS.get("halite").get(),
                    () -> halite, n -> halite = n, () -> lapis == 0 && sulfur == 0, () -> 64),
            ingredientSlot(() -> Items.GLASS_BOTTLE, () -> bottles, n -> bottles = n, () -> true, () -> 64),
            new MachineItemSlot(this, () -> concentrate, stack -> concentrate = stack,
                    null, 64, before -> changed()),
            new MachineItemSlot(this, () -> sediment, stack -> sediment = stack,
                    null, 64, before -> changed()));

    private MachineItemSlot ingredientSlot(java.util.function.Supplier<Item> item, java.util.function.IntSupplier count,
            java.util.function.IntConsumer setCount, java.util.function.BooleanSupplier compatible,
            java.util.function.IntSupplier limit) {
        return new MachineItemSlot(this, () -> new ItemStack(item.get(), count.getAsInt()),
                stack -> setCount.accept(stack.getCount()),
                stack -> automationAuthorized() && compatible.getAsBoolean() && stack.is(item.get()),
                limit, before -> changed());
    }

    public ResourceHandler<ItemResource> itemPort() { return itemPort; }

    public SettlingVatBlockEntity(BlockPos pos,BlockState state){super(ModBlocks.SETTLING_VAT_ENTITY.get(),pos,state);}
    public FluidStacksResourceHandler tank(){return tank;}
    @Override public int storedFluid(){return tank.getAmountAsInt(0);}
    @Override public int fluidCapacity(){return CAPACITY;}
    @Override public Component fluidTitle(){return Component.translatable("block.quantumindustries.settling_vat");}
    @Override public Component fluidDetail(){
        return Component.translatable("message.quantumindustries.vat.status",storedFluid(),CAPACITY,lapis+sulfur+halite,bottles,progress);
    }
    @Override public Component statusTitle(){return fluidTitle();}
    @Override public Component statusDetail(){
        if(progress>0)return Component.translatable("gui.quantumindustries.vat.progress",progress*100/WORK_TICKS);
        if(!concentrate.isEmpty()||!sediment.isEmpty()){
            ItemStack ready=!concentrate.isEmpty()?concentrate:sediment;
            return Component.translatable("gui.quantumindustries.press.output_ready",ready.getCount(),ready.getHoverName());
        }
        if((lapis>0||sulfur>0||halite>0||bottles>0||storedFluid()>0)){
            return Component.translatable("gui.quantumindustries.vat.waiting",storedFluid(),RECIPE_WATER,lapis+sulfur+halite,bottles);
        }
        return fluidDetail();
    }
    @Override public State statusState(){
        if(progress>0)return State.WORKING;
        if(!concentrate.isEmpty()||!sediment.isEmpty())return State.READY;
        if(lapis>0||sulfur>0||halite>0||bottles>0||storedFluid()>0)return State.BLOCKED;
        return State.IDLE;
    }
    @Override public float statusProgress(){
        if(progress>0)return Math.min(1.0f,(float)progress/WORK_TICKS);
        return(!concentrate.isEmpty()||!sediment.isEmpty())?1.0f:0.0f;
    }
    public static boolean isReagentItem(ItemStack stack){
        return stack.is(Items.LAPIS_LAZULI)
                ||(ModItems.CHEMICAL_PRODUCTS.containsKey("sulfur")&&stack.is(ModItems.CHEMICAL_PRODUCTS.get("sulfur").get()))
                ||(ModItems.CHEMICAL_PRODUCTS.containsKey("halite")&&stack.is(ModItems.CHEMICAL_PRODUCTS.get("halite").get()));
    }
    public boolean addIngredient(ItemStack stack){
        if (!automationAuthorized()) return false;
        if(!(level instanceof ServerLevel))return false;
        boolean isLapis=stack.is(Items.LAPIS_LAZULI);
        boolean isSulfur=ModItems.CHEMICAL_PRODUCTS.containsKey("sulfur")&&stack.is(ModItems.CHEMICAL_PRODUCTS.get("sulfur").get());
        boolean isHaliteReagent=ModItems.CHEMICAL_PRODUCTS.containsKey("halite")&&stack.is(ModItems.CHEMICAL_PRODUCTS.get("halite").get());
        if(isLapis&&halite==0&&lapis+sulfur<64){lapis++;stack.shrink(1);changed();return true;}
        if(isSulfur&&halite==0&&lapis+sulfur<64){sulfur++;stack.shrink(1);changed();return true;}
        if(isHaliteReagent&&lapis==0&&sulfur==0&&halite<64){halite++;stack.shrink(1);changed();return true;}
        if(stack.is(Items.GLASS_BOTTLE)&&bottles<64){bottles++;stack.shrink(1);changed();return true;}
        return false;
    }
    public ItemStack collect(){
        if(!(level instanceof ServerLevel))return ItemStack.EMPTY;
        if(!concentrate.isEmpty()){ItemStack taken=concentrate.copy();concentrate=ItemStack.EMPTY;changed();return taken;}
        if(!sediment.isEmpty()){ItemStack taken=sediment.copy();sediment=ItemStack.EMPTY;changed();return taken;}
        return ItemStack.EMPTY;
    }
    public ItemStack outputPreview(){
        return !concentrate.isEmpty()?concentrate:sediment;
    }
    public ItemStack extractOutput(int maximum){
        if(!(level instanceof ServerLevel)||maximum<=0)return ItemStack.EMPTY;
        if(!concentrate.isEmpty()){
            ItemStack taken=concentrate.split(Math.min(maximum,concentrate.getCount()));
            if(concentrate.isEmpty())concentrate=ItemStack.EMPTY;
            changed();return taken;
        }
        if(!sediment.isEmpty()){
            ItemStack taken=sediment.split(Math.min(maximum,sediment.getCount()));
            if(sediment.isEmpty())sediment=ItemStack.EMPTY;
            changed();return taken;
        }
        return ItemStack.EMPTY;
    }
    public void tick(){
        if(!(level instanceof ServerLevel server))return;
        if (!automationAuthorized()) return;
        boolean producesPigment = lapis > 0 && halite == 0;
        Item expectedConcentrate=halite>0?ModItems.HYDROCHLORIC_ACID.get():ModItems.GALVANIC_ELECTROLYTE.get();
        boolean ready=(lapis>0||sulfur>0||halite>0)&&bottles>0&&storedFluid()>=RECIPE_WATER
                &&tank.getResource(0).is(Fluids.WATER)
                &&(concentrate.isEmpty()||(concentrate.is(expectedConcentrate)&&concentrate.getCount()<64))
                &&(!producesPigment||sediment.isEmpty()||sediment.getCount()<64);
        if(!ready)return;
        if(++progress<WORK_TICKS){
            if(progress%6==0){
                server.sendParticles(ParticleTypes.BUBBLE_POP,worldPosition.getX()+0.5,worldPosition.getY()+0.82,
                        worldPosition.getZ()+0.5,2,0.18,0.03,0.18,0.01);
            }
            if(progress%5==0)changed();
            return;
        }
        try(var tx=Transaction.openRoot()){
            int taken=tank.extract(FluidResource.of(Fluids.WATER),RECIPE_WATER,tx);
            if(taken!=RECIPE_WATER){progress=0;return;}
            tx.commit();
        }
        if(halite>0)halite--;
        else if(lapis>0)lapis--;
        else sulfur--;
        bottles--;progress=0;
        if(concentrate.isEmpty())concentrate=new ItemStack(expectedConcentrate);
        else concentrate.grow(1);
        if (producesPigment) {
            if (sediment.isEmpty()) sediment = new ItemStack(ModItems.PIGMENT_SEDIMENT.get());
            else sediment.grow(1);
        }
        server.playSound(null,worldPosition,SoundEvents.BREWING_STAND_BREW,SoundSource.BLOCKS,0.4f,1.1f);
        changed();
    }
    private void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);}
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out); out.putString("automation_operator", automationOperator);tank.serialize(out);out.putInt("lapis",lapis);out.putInt("sulfur",sulfur);out.putInt("halite",halite);
        out.putInt("bottles",bottles);out.putInt("progress",progress);
        if(!concentrate.isEmpty())out.store("concentrate",ItemStack.CODEC,concentrate);
        if(!sediment.isEmpty())out.store("sediment",ItemStack.CODEC,sediment);
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in); automationOperator = in.getStringOr("automation_operator", "");tank.deserialize(in);lapis=Math.clamp(in.getIntOr("lapis",0),0,64);
        sulfur=Math.clamp(in.getIntOr("sulfur",0),0,64);
        halite=Math.clamp(in.getIntOr("halite",0),0,64);
        bottles=Math.clamp(in.getIntOr("bottles",0),0,64);
        progress=Math.clamp(in.getIntOr("progress",0),0,WORK_TICKS);
        concentrate=in.read("concentrate",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        sediment=in.read("sediment",ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }
    @Override public void preRemoveSideEffects(BlockPos pos,BlockState state){
        super.preRemoveSideEffects(pos,state);
        if(!(level instanceof ServerLevel server))return;
        if(lapis>0)Block.popResource(server,pos,new ItemStack(Items.LAPIS_LAZULI,lapis));
        if(sulfur>0&&ModItems.CHEMICAL_PRODUCTS.containsKey("sulfur"))
            Block.popResource(server,pos,new ItemStack(ModItems.CHEMICAL_PRODUCTS.get("sulfur").get(),sulfur));
        if(halite>0&&ModItems.CHEMICAL_PRODUCTS.containsKey("halite"))
            Block.popResource(server,pos,new ItemStack(ModItems.CHEMICAL_PRODUCTS.get("halite").get(),halite));
        if(bottles>0)Block.popResource(server,pos,new ItemStack(Items.GLASS_BOTTLE,bottles));
        if(!concentrate.isEmpty())Block.popResource(server,pos,concentrate.copy());
        if(!sediment.isEmpty())Block.popResource(server,pos,sediment.copy());
        lapis=sulfur=halite=bottles=0;concentrate=sediment=ItemStack.EMPTY;
        // Fluid contents are intentionally not materialized as item drops.
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveCustomOnly(r);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
