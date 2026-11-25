package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.thermal.ThermalInspectable;
import br.com.quantumindustries.thermal.ThermalSnapshot;
import br.com.quantumindustries.energy.ThermoelectricModel;
import br.com.quantumindustries.thermal.ThermalNetwork;
import java.util.List;
import br.com.quantumindustries.block.IndustrialConversionBlock;
import br.com.quantumindustries.block.IndustrialConversionBlock.Kind;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.fluid.FluidInspectable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Water capability plus an internal steam buffer, not an alias for water or a FE source. */
public final class SteamBoilerBlockEntity extends BlockEntity implements FluidInspectable, MachineStatusInspectable, ThermalInspectable {
    public static final int WATER_CAPACITY=4000, STEAM_CAPACITY=2000, WATER_PER_TICK=10, BURN_TICKS=160;
    private final FluidStacksResourceHandler tank=new FluidStacksResourceHandler(1,WATER_CAPACITY){
        @Override public boolean isValid(int slot, FluidResource resource){return resource.is(Fluids.WATER)&&super.isValid(slot,resource);}
        @Override protected void onContentsChanged(int slot,net.neoforged.neoforge.fluids.FluidStack stack){setChanged();}
    };
    private String operator="";
    private ItemStack fuel=ItemStack.EMPTY;
    private static final int STEAM_TEMPERATURE_C = 100;
    private static final int STEAM_CAPACITY_TU_PER_C_PER_MB = 1;
    private int steam,burn;
    private boolean working;
    public SteamBoilerBlockEntity(BlockPos pos,BlockState state){super(ModBlocks.STEAM_BOILER_ENTITY.get(),pos,state);}
    public Direction outlet(){return IndustrialConversionBlock.facing(getBlockState());}
    public FluidStacksResourceHandler tank(){return tank;}
    public boolean bind(Player player){
        if(!ConversionAuthorization.canBind(level,worldPosition,player,Kind.BOILER))return false;
        if(operator.isEmpty()){operator=player.getUUID().toString();changed();}
        return ConversionAuthorization.permitted(level,operator,Kind.BOILER);
    }
    public int fuelCount(){return fuel.getCount();}
    public boolean canInsertFuel(ItemStack stack, Direction side){
        return side!=outlet() && canAddFuel(stack);
    }
    private boolean canAddFuel(ItemStack stack){
        return level instanceof ServerLevel && ConversionAuthorization.permitted(level,operator,Kind.BOILER)
                && !stack.isEmpty() && (stack.is(Items.COAL)||stack.is(Items.CHARCOAL))
                && (fuel.isEmpty() || ItemStack.isSameItemSameComponents(fuel,stack)
                    && fuel.getCount()<fuel.getMaxStackSize());
    }
    public boolean addFuel(ItemStack stack){
        if(!canAddFuel(stack))return false;
        if(fuel.isEmpty())fuel=stack.split(1);else{fuel.grow(1);stack.shrink(1);}
        changed();return true;
    }
    public int steam(){return steam;}
    public int takeSteam(int amount,Direction side){
        if(!(level instanceof ServerLevel)||side!=outlet()||amount<=0||steam<amount
                ||!ConversionAuthorization.permitted(level,operator,Kind.BOILER))return 0;
        steam-=amount;setChanged();return amount;
    }
    public void tick(){
        if(!(level instanceof ServerLevel server))return;
        working=false;
        if(ConversionAuthorization.permitted(level,operator,Kind.BOILER)
                && steam<=STEAM_CAPACITY-WATER_PER_TICK && storedFluid()>=WATER_PER_TICK
                && tank.getResource(0).is(Fluids.WATER) && (burn>0||!fuel.isEmpty())){
            try(var tx=Transaction.openRoot()){
                if(tank.extract(FluidResource.of(Fluids.WATER),WATER_PER_TICK,tx)==WATER_PER_TICK){
                    tx.commit();
                    if(burn==0){fuel.shrink(1);if(fuel.isEmpty())fuel=ItemStack.EMPTY;burn=BURN_TICKS;}
                    burn--;steam+=WATER_PER_TICK;working=true;setChanged();
                }
            }
        }
        if(server.getGameTime()%10==0)server.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);
    }
    @Override
    public ThermalSnapshot thermalSnapshot() {
        int ambient = level instanceof ServerLevel server
                ? ThermalNetwork.ambientAt(server, worldPosition) : ThermoelectricModel.AMBIENT;
        // Legacy boiler telemetry follows the steam reservoir and the last production step.
        int temperature = steam > 0 || working ? STEAM_TEMPERATURE_C : ambient;
        int capacity = steam * STEAM_CAPACITY_TU_PER_C_PER_MB;
        long heating = working
                ? (long) WATER_PER_TICK * STEAM_CAPACITY_TU_PER_C_PER_MB * STEAM_TEMPERATURE_C : 0;
        return new ThermalSnapshot(temperature, ambient, (long) temperature * capacity, capacity,
                heating, 0, heating, STEAM_TEMPERATURE_C, STEAM_TEMPERATURE_C, List.of());
    }

    @Override public int storedFluid(){return tank.getAmountAsInt(0);}
    @Override public int fluidCapacity(){return WATER_CAPACITY;}
    @Override public Component fluidTitle(){return Component.translatable("block.quantumindustries.steam_boiler");}
    @Override public Component fluidDetail(){return statusDetail();}
    @Override public Component statusTitle(){return fluidTitle();}
    @Override public Component statusDetail(){return Component.translatable("message.quantumindustries.steam_boiler.status",storedFluid(),WATER_CAPACITY,steam,STEAM_CAPACITY,fuel.getCount(),burn);}
    @Override public State statusState(){return working?State.WORKING:steam>=STEAM_CAPACITY?State.READY:State.BLOCKED;}
    @Override public float statusProgress(){return (float)steam/STEAM_CAPACITY;}
    private void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);}
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);tank.serialize(out);out.putString("operator",operator);out.putInt("steam",steam);out.putInt("burn",burn);out.putBoolean("working",working);
        if(!fuel.isEmpty())out.store("fuel",ItemStack.CODEC,fuel);
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);tank.deserialize(in);operator=in.getStringOr("operator","");steam=Math.clamp(in.getIntOr("steam",0),0,STEAM_CAPACITY);
        burn=Math.clamp(in.getIntOr("burn",0),0,BURN_TICKS);fuel=in.read("fuel",ItemStack.CODEC).orElse(ItemStack.EMPTY);working=in.getBooleanOr("working",false);
    }
    @Override public void preRemoveSideEffects(BlockPos pos,BlockState state){
        super.preRemoveSideEffects(pos,state);if(level instanceof ServerLevel server&&!fuel.isEmpty()){
            var drop=fuel;fuel=ItemStack.EMPTY;Block.popResource(server,pos,drop);
        }
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveCustomOnly(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
