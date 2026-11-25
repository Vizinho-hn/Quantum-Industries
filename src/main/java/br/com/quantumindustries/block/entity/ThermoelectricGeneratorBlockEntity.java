package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.thermal.ThermalInspectable;
import br.com.quantumindustries.thermal.ThermalSnapshot;
import br.com.quantumindustries.thermal.ThermalSnapshot.ThermalFlowEdge;
import java.util.List;
import br.com.quantumindustries.block.IndustrialConversionBlock.Kind;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.energy.ConversionPower;
import br.com.quantumindustries.energy.TransferBudget;
import br.com.quantumindustries.energy.ThermoelectricModel;
import br.com.quantumindustries.energy.ThermalExchange;
import br.com.quantumindustries.energy.ThermalHistory;
import br.com.quantumindustries.energy.ThermalModules;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import br.com.quantumindustries.energy.ThermoelectricModel.Coolant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

/** Seebeck conversion across opposing faces, retaining the existing front FE output. */
public final class ThermoelectricGeneratorBlockEntity extends BlockEntity implements EnergyInspectable, MachineStatusInspectable, ThermalInspectable {
    public static final int CAPACITY=480,TRANSFER=32;
    private final SimpleEnergyHandler battery=new SimpleEnergyHandler(CAPACITY,TRANSFER,TRANSFER);
    private final TransferBudget budget=new TransferBudget(TRANSFER);
    private String operator="";
    private int generation;
    private int ambient=ThermoelectricModel.AMBIENT, heatInput, heatExchange, fanCost, syncedEfficiency, syncedNet;
    private final ThermalHistory history=new ThermalHistory();
    private boolean loadingModules;
    private final SimpleContainer modules=new SimpleContainer(4){
        @Override public void setChanged(){super.setChanged();if(!loadingModules)ThermoelectricGeneratorBlockEntity.this.setChanged();}
    };
    public SimpleContainer modules(){return modules;}
    public boolean canConfigure(Player player){return ConversionAuthorization.canBind(level,worldPosition,player,Kind.THERMOELECTRIC);}
    public int ambientTemperature(){return ambient;}
    public int hotTemperature(){return hot;}
    public int coldTemperature(){return cold;}
    public int thermalInput(){return heatInput;}
    public int thermalExchange(){return heatExchange;}
    public int fanCost(){return fanCost;}
    public ThermalHistory history(){return history;}
    public int efficiency(){return level!=null&&level.isClientSide()?syncedEfficiency:history.latest(2);}
    public int netPower(){return level!=null&&level.isClientSide()?syncedNet:history.latest(3);}
    public int coolantRemaining(){return coolant.finite()?Math.clamp(100-100*Math.max(0,coolantHeat)/Math.max(1,coolant.maxHeat()),0,100):coolant==Coolant.NONE?0:100;}
    public int conditionCode(){return condition.ordinal();}
    private Coolant coolant=Coolant.NONE;
    private int coolantHeat, powerRemainder;
    private static final Direction[] FACES = Direction.values();
    private final Coolant[] faceCoolants = new Coolant[FACES.length];
    private final int[] faceHeat = new int[FACES.length];
    private Direction coolantFace = Direction.UP;
    private int hot=ThermoelectricModel.AMBIENT, cold=ThermoelectricModel.AMBIENT;
    private Condition condition=Condition.NO_HEAT;
    private enum Condition { NO_HEAT, NO_COOLANT, OVERHEAT, LOCKED, NO_GRADIENT, FULL, GENERATING }
    public ThermoelectricGeneratorBlockEntity(BlockPos pos,BlockState state){super(ModBlocks.THERMOELECTRIC_GENERATOR_ENTITY.get(),pos,state);java.util.Arrays.fill(faceCoolants,Coolant.NONE);}
    public boolean bind(Player player){
        if(!ConversionAuthorization.canBind(level,worldPosition,player,Kind.THERMOELECTRIC))return false;
        if(operator.isEmpty()){operator=player.getUUID().toString();setChanged();}
        return ConversionAuthorization.permitted(level,operator,Kind.THERMOELECTRIC);
    }
    @Override
    public ThermalSnapshot thermalSnapshot() {
        long heating = heatInput + Math.max(0L, -(long) heatExchange);
        long cooling = generation + Math.max(0L, heatExchange);
        // The finite reservoir is on the cold side. Its model includes phase-change energy.
        int capacity = 0;
        if (coolant.finite()) {
            // Sample the sensible range outside the freezing plateau in the coolant model.
            int fromHeat = coolant == Coolant.WATER ? 0 : coolant.minHeat();
            int toHeat = coolant.initialHeat();
            capacity = (toHeat - fromHeat) / (coolant.temperature(toHeat) - coolant.temperature(fromHeat));
        }
        Direction hotFace = coolantFace.getOpposite();
        List<ThermalFlowEdge> edges = heatInput > 0
                ? List.of(new ThermalFlowEdge(hotFace, worldPosition.relative(hotFace), heatInput))
                : List.of();
        return new ThermalSnapshot(cold, ambient, coolantHeat, capacity, heating, cooling, heating - cooling,
                ambient, coolant.temperature(coolant.maxHeat()), edges);
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

    private static int contactTemperature(BlockState below) {
        if(below.getFluidState().is(FluidTags.LAVA))return ThermoelectricModel.LAVA;
        if(below.is(Blocks.FIRE)||below.is(Blocks.SOUL_FIRE))return ThermoelectricModel.FIRE;
        if(below.is(Blocks.MAGMA_BLOCK))return ThermoelectricModel.MAGMA;
        return BellowsBrazierBlockEntity.classifyHeatSource(below)>0?ThermoelectricModel.LIT_APPLIANCE:ThermoelectricModel.AMBIENT;
    }

    public void tick(){
        if(!(level instanceof ServerLevel server))return;
        int previousGeneration=generation, previousHot=hot, previousCold=cold;
        int previousHeat=coolantHeat, previousRemainder=powerRemainder;
        Coolant previousCoolant=coolant;
        Condition previousCondition=condition;
        generation=0;
        heatInput=heatExchange=fanCost=0;
        if(server.getGameTime()%20==0||history.count()==0)
            ambient=ThermalExchange.ambient(server.getBiome(worldPosition).value().getBaseTemperature(),server.dimension().equals(Level.NETHER));
        // Keep each face's accumulated heat separately: selecting another pair
        // must never restore the thermal capacity of a previously heated block.
        Direction previousFace=coolantFace;
        Direction bestFace=coolantFace;
        int bestScore=-1, bestHot=ThermoelectricModel.AMBIENT;
        for(Direction face:FACES){
            int i=face.ordinal();
            var cp=worldPosition.relative(face);
            var hp=worldPosition.relative(face.getOpposite());
            if(!server.hasChunkAt(cp)||!server.hasChunkAt(hp))continue;
            Coolant current=coolantAt(server.getBlockState(cp));
            if(current!=faceCoolants[i]){
                faceCoolants[i]=current;
                faceHeat[i]=current.initialHeat();
                if(face==previousFace)powerRemainder=0;
                setChanged();
            }
            int candidateHot=contactTemperature(server.getBlockState(hp));
            int candidateCold=current.temperature(faceHeat[i]);
            int potential=ThermoelectricModel.powerNumerator(candidateHot,candidateCold,current);
            // Prefer usable gradients; otherwise retain a meaningful diagnostic.
            int score=potential>0?1000+potential:
                    candidateHot>ThermoelectricModel.AMBIENT?(current!=Coolant.NONE?500:100)+candidateHot:
                    current!=Coolant.NONE?1:0;
            if(score>bestScore||(score==bestScore&&face==previousFace)){
                bestScore=score;bestFace=face;bestHot=candidateHot;
            }
        }
        coolantFace=bestFace;
        if(previousFace!=coolantFace)powerRemainder=0;
        var coolantPos=worldPosition.relative(coolantFace);
        int selected=coolantFace.ordinal();
        coolant=bestScore<0?Coolant.NONE:faceCoolants[selected];
        coolantHeat=bestScore<0?0:faceHeat[selected];
        hot=bestHot;
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

        int sampledCold=cold;
        heatInput=authorized&&bestScore>=0?ThermoelectricModel.heatFlow(hot,cold,coolant):0;
        int conductance=0;
        if(authorized&&coolant.finite()){
            for(int i=0;i<4;i++){
                ItemStack module=modules.getItem(i);
                conductance+=ThermalModules.conductance(module);
                if(ThermalModules.fan(module)&&cold>ambient&&heatInput>0&&energy()>0){
                    try(var tx=Transaction.openRoot()){
                        int spent=battery.extract(1,tx);
                        if(spent==1){tx.commit();fanCost++;conductance+=ThermalExchange.FAN_ACTIVE-ThermalExchange.FAN_PASSIVE;}
                    }
                }
            }
            heatExchange=ThermalExchange.exchange(cold,ambient,conductance);
        }
        // Heat passes through an idle/full module too. World heat sources are
        // idealized hot reservoirs; still water and ice have finite heat capacity.
        if(authorized&&bestScore>=0&&coolant.finite()){
            int absorbed=heatInput-generation-heatExchange;
            coolantHeat=Math.clamp(coolantHeat+absorbed,coolant.minHeat(),coolant.maxHeat());
            if(coolant.frozen(coolantHeat)){
                if(server.setBlock(coolantPos,Blocks.ICE.defaultBlockState(),Block.UPDATE_ALL)){
                    coolant=Coolant.ICE;coolantHeat=0;powerRemainder=0;
                }
            }else if(coolant.exhausted(coolantHeat)){
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
            faceCoolants[selected]=coolant;
            faceHeat[selected]=coolantHeat;
            cold=coolant.temperature(coolantHeat);
        }
        history.tick(hot,sampledCold,heatInput,generation,fanCost);
        if(fanCost>0)setChanged();
        ConversionPower.distribute(server,worldPosition,this::energy,this::extractEnergy,budget);
        if(previousGeneration!=generation||generation>0||previousHot!=hot||previousCold!=cold
                ||previousHeat!=coolantHeat||previousRemainder!=powerRemainder
                ||previousCoolant!=coolant||previousCondition!=condition||previousFace!=coolantFace)setChanged();
        if(server.getGameTime()%10==0)server.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);
    }
    @Override public Component statusTitle(){return inspectTitle();}
    @Override public Component statusDetail(){
        return Component.translatable("message.quantumindustries.thermoelectric_generator.status",energy(),CAPACITY,generation,hot,cold,
                ThermoelectricModel.gradient(hot,cold),Component.translatable("message.quantumindustries.thermoelectric_generator."+condition.name().toLowerCase(java.util.Locale.ROOT)))
                .append(Component.translatable("message.quantumindustries.thermal.telemetry",ambient,efficiency()/100.0,netPower()/100.0,heatExchange));
    }
    @Override public State statusState(){return condition==Condition.GENERATING?State.WORKING:condition==Condition.FULL?State.READY:State.BLOCKED;}
    @Override public float statusProgress(){return (float)energy()/CAPACITY;}
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);
        out.putInt("stored_fe",energy());out.putString("operator",operator);out.putInt("generation",generation);
        out.putInt("coolant_type",coolant.ordinal());out.putInt("coolant_heat",coolantHeat);
        out.putInt("power_remainder",powerRemainder);out.putInt("hot_contact",hot);out.putInt("cold_contact",cold);
        out.putInt("thermal_condition",condition.ordinal());
        out.putInt("coolant_face",coolantFace.ordinal());
        out.putInt("ambient",ambient);out.putInt("thermal_input",heatInput);out.putInt("thermal_exchange",heatExchange);out.putInt("fan_cost",fanCost);
        out.putInt("efficiency_bp",efficiency());out.putInt("net_power",netPower());
        for(int i=0;i<4;i++)if(!modules.getItem(i).isEmpty())out.store("thermal_module_"+i,ItemStack.CODEC,modules.getItem(i));
        for(Direction face:FACES){
            int i=face.ordinal();
            out.putInt("face_coolant_"+i,faceCoolants[i].ordinal());
            out.putInt("face_heat_"+i,faceHeat[i]);
        }
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);
        ambient=Math.clamp(in.getIntOr("ambient",20),-60,80);heatInput=in.getIntOr("thermal_input",0);heatExchange=in.getIntOr("thermal_exchange",0);fanCost=in.getIntOr("fan_cost",0);
        syncedEfficiency=Math.clamp(in.getIntOr("efficiency_bp",0),0,10000);syncedNet=in.getIntOr("net_power",0);
        loadingModules=true;
        for(int i=0;i<4;i++){
            ItemStack module=in.read("thermal_module_"+i,ItemStack.CODEC).orElse(ItemStack.EMPTY);
            modules.setItem(i,ThermalModules.accepts(module)?module.copyWithCount(1):ItemStack.EMPTY);
        }
        loadingModules=false;
        battery.set(Math.clamp(in.getIntOr("stored_fe",0),0,CAPACITY));operator=in.getStringOr("operator","");
        generation=Math.clamp(in.getIntOr("generation",0),0,ThermoelectricModel.MAX_FE);
        coolant=Coolant.fromId(in.getIntOr("coolant_type",0));
        coolantHeat=Math.clamp(in.getIntOr("coolant_heat",coolant.initialHeat()),coolant.minHeat(),coolant.maxHeat());
        coolantFace=FACES[Math.clamp(in.getIntOr("coolant_face",Direction.UP.ordinal()),0,FACES.length-1)];
        // Old saves used the top face exclusively; preserve their thermal state.
        for(Direction face:FACES){
            int i=face.ordinal();
            faceCoolants[i]=Coolant.fromId(in.getIntOr("face_coolant_"+i,face==coolantFace?coolant.ordinal():0));
            faceHeat[i]=Math.clamp(in.getIntOr("face_heat_"+i,face==coolantFace?coolantHeat:faceCoolants[i].initialHeat()),faceCoolants[i].minHeat(),faceCoolants[i].maxHeat());
        }
        powerRemainder=Math.clamp(in.getIntOr("power_remainder",0),0,ThermoelectricModel.POWER_SCALE-1);
        hot=Math.clamp(in.getIntOr("hot_contact",ThermoelectricModel.AMBIENT),ThermoelectricModel.AMBIENT,300);
        cold=coolant.temperature(coolantHeat);
        var conditions=Condition.values();
        condition=conditions[Math.clamp(in.getIntOr("thermal_condition",0),0,conditions.length-1)];
    }
    @Override public void preRemoveSideEffects(BlockPos pos,BlockState state){
        super.preRemoveSideEffects(pos,state);
        if(level instanceof ServerLevel server)for(int i=0;i<4;i++)Block.popResource(server,pos,modules.removeItemNoUpdate(i));
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveCustomOnly(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
