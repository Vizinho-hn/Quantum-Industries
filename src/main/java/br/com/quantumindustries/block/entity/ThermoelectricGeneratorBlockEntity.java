package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.block.IndustrialConversionBlock.Kind;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.energy.ConversionPower;
import br.com.quantumindustries.energy.TransferBudget;
import br.com.quantumindustries.energy.ThermoelectricModel;
import br.com.quantumindustries.energy.ThermoelectricModel.Coolant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Seebeck conversion: hot bottom, cold top and the existing front FE output. */
public final class ThermoelectricGeneratorBlockEntity extends BlockEntity implements EnergyInspectable, MachineStatusInspectable {
    public static final int CAPACITY=480,TRANSFER=32;
    private final SimpleEnergyHandler battery=new SimpleEnergyHandler(CAPACITY,TRANSFER,TRANSFER);
    private final TransferBudget budget=new TransferBudget(TRANSFER);
    private String operator="";
    private int generation;
    private Coolant coolant=Coolant.NONE;
    private int coolantHeat, powerRemainder;
    private int hot=ThermoelectricModel.AMBIENT, cold=ThermoelectricModel.AMBIENT;
    private Condition condition=Condition.NO_HEAT;
    private enum Condition { NO_HEAT, NO_COOLANT, OVERHEAT, LOCKED, NO_GRADIENT, FULL, GENERATING }
    public ThermoelectricGeneratorBlockEntity(BlockPos pos,BlockState state){super(ModBlocks.THERMOELECTRIC_GENERATOR_ENTITY.get(),pos,state);}
    public boolean bind(Player player){
        if(!ConversionAuthorization.canBind(level,worldPosition,player,Kind.THERMOELECTRIC))return false;
        if(operator.isEmpty()){operator=player.getUUID().toString();setChanged();}
        return ConversionAuthorization.permitted(level,operator,Kind.THERMOELECTRIC);
    }
    @Override public int energy(){return (int)battery.getAmountAsLong();}
    @Override public int capacity(){return CAPACITY;}
    @Override public Component inspectTitle(){return Component.translatable("block.quantumindustries.thermoelectric_generator");}
    @Override public Component inspectDetail(){return statusDetail();}
    @Override public int extractEnergy(int offered){
        if(!(level instanceof ServerLevel server)||offered<=0)return 0;
        int n;try(var tx=Transaction.openRoot()){
            n=battery.extract(Math.min(offered,budget.extractRemaining(server.getGameTime())),tx);if(n>0)tx.commit();
        }
        if(n>0){budget.recordExtract(server.getGameTime(),n);setChanged();}return n;
    }
    private static Coolant coolantAt(BlockState state) {
        if(state.is(Blocks.SNOW_BLOCK))return Coolant.SNOW;
        if(state.is(Blocks.ICE))return Coolant.ICE;
        if(state.is(Blocks.PACKED_ICE))return Coolant.PACKED_ICE;
        if(state.is(Blocks.BLUE_ICE))return Coolant.BLUE_ICE;
        // Do not consume or replace arbitrary waterlogged blocks.
        if(state.is(Blocks.WATER)&&state.getFluidState().is(FluidTags.WATER))
            return state.getFluidState().isSource()?Coolant.WATER:Coolant.FLOWING_WATER;
        return Coolant.NONE;
    }

    private int contactTemperature(ServerLevel server) {
        var below=server.getBlockState(worldPosition.below());
        if(below.getFluidState().is(FluidTags.LAVA))return 300;
        if(below.is(Blocks.FIRE)||below.is(Blocks.SOUL_FIRE))return 220;
        if(below.is(Blocks.MAGMA_BLOCK))return 180;
        return BellowsBrazierBlockEntity.heatSourceLevel(server,worldPosition)>0?140:ThermoelectricModel.AMBIENT;
    }

    public void tick(){
        if(!(level instanceof ServerLevel server))return;
        int previousGeneration=generation, previousHot=hot, previousCold=cold;
        int previousHeat=coolantHeat, previousRemainder=powerRemainder;
        Coolant previousCoolant=coolant;
        Condition previousCondition=condition;
        generation=0;
        var coolantPos=worldPosition.above();
        var current=coolantAt(server.getBlockState(coolantPos));
        if(current!=coolant){
            coolant=current;
            coolantHeat=current.initialHeat();
            powerRemainder=0;
        }
        hot=contactTemperature(server);
        cold=coolant.temperature(coolantHeat);
        int potential=ThermoelectricModel.powerNumerator(hot,cold,coolant);
        boolean authorized=ConversionAuthorization.permitted(level,operator,Kind.THERMOELECTRIC);
        if(!authorized)condition=Condition.LOCKED;
        else if(hot<=ThermoelectricModel.AMBIENT)condition=Condition.NO_HEAT;
        else if(coolant==Coolant.NONE)condition=Condition.NO_COOLANT;
        else if(hot>ThermoelectricModel.MAX_HOT)condition=Condition.OVERHEAT;
        else if(potential==0)condition=Condition.NO_GRADIENT;
        else if(energy()>=CAPACITY)condition=Condition.FULL;
        else condition=Condition.GENERATING;

        if(condition==Condition.GENERATING){
            int scaled=potential+powerRemainder;
            int offered=scaled/ThermoelectricModel.POWER_SCALE;
            powerRemainder=scaled%ThermoelectricModel.POWER_SCALE;
            if(offered>0){
                try(var tx=Transaction.openRoot()){
                    generation=battery.insert(offered,tx);
                    if(generation>0)tx.commit();
                }
                if(generation<offered||energy()>=CAPACITY)powerRemainder=0;
            }
        }else powerRemainder=0;

        // Heat passes through an idle/full module too. World heat sources are
        // idealized hot reservoirs; still water and ice have finite heat capacity.
        if(authorized&&coolant.finite()){
            int absorbed=Math.max(0,ThermoelectricModel.heatFlow(hot,cold,coolant)-generation);
            coolantHeat=Math.min(coolant.maxHeat(),coolantHeat+absorbed);
            if(coolant.exhausted(coolantHeat)){
                // Only dedicated coolant blocks are mutated; snow/ice become
                // water, and boiled water disappears. Nether ice evaporates.
                boolean toWater=coolant!=Coolant.WATER&&!server.dimension().equals(Level.NETHER);
                var replacement=toWater?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState();
                if(server.setBlock(coolantPos,replacement,Block.UPDATE_ALL)){
                    coolant=toWater?Coolant.WATER:Coolant.NONE;
                    coolantHeat=0;
                    powerRemainder=0;
                }
            }
            cold=coolant.temperature(coolantHeat);
        }
        ConversionPower.distribute(server,worldPosition,this::energy,this::extractEnergy,budget);
        if(previousGeneration!=generation||generation>0||previousHot!=hot||previousCold!=cold
                ||previousHeat!=coolantHeat||previousRemainder!=powerRemainder
                ||previousCoolant!=coolant||previousCondition!=condition)setChanged();
        if(server.getGameTime()%10==0)server.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);
    }
    @Override public Component statusTitle(){return inspectTitle();}
    @Override public Component statusDetail(){
        return Component.translatable("message.quantumindustries.thermoelectric_generator.status",energy(),CAPACITY,generation,hot,cold,
                ThermoelectricModel.gradient(hot,cold),Component.translatable("message.quantumindustries.thermoelectric_generator."+condition.name().toLowerCase(java.util.Locale.ROOT)));
    }
    @Override public State statusState(){return condition==Condition.GENERATING?State.WORKING:condition==Condition.FULL?State.READY:State.BLOCKED;}
    @Override public float statusProgress(){return (float)energy()/CAPACITY;}
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);
        out.putInt("stored_fe",energy());out.putString("operator",operator);out.putInt("generation",generation);
        out.putInt("coolant_type",coolant.ordinal());out.putInt("coolant_heat",coolantHeat);
        out.putInt("power_remainder",powerRemainder);out.putInt("hot_contact",hot);out.putInt("cold_contact",cold);
        out.putInt("thermal_condition",condition.ordinal());
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);
        battery.set(Math.clamp(in.getIntOr("stored_fe",0),0,CAPACITY));operator=in.getStringOr("operator","");
        generation=Math.clamp(in.getIntOr("generation",0),0,ThermoelectricModel.MAX_FE);
        coolant=Coolant.fromId(in.getIntOr("coolant_type",0));
        coolantHeat=Math.clamp(in.getIntOr("coolant_heat",coolant.initialHeat()),0,coolant.maxHeat());
        powerRemainder=Math.clamp(in.getIntOr("power_remainder",0),0,ThermoelectricModel.POWER_SCALE-1);
        hot=Math.clamp(in.getIntOr("hot_contact",ThermoelectricModel.AMBIENT),ThermoelectricModel.AMBIENT,300);
        cold=coolant.temperature(coolantHeat);
        var conditions=Condition.values();
        condition=conditions[Math.clamp(in.getIntOr("thermal_condition",0),0,conditions.length-1)];
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveCustomOnly(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
