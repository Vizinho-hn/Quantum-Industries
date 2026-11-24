package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.block.IndustrialConversionBlock.Kind;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.energy.ConversionPower;
import br.com.quantumindustries.energy.TransferBudget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Gameplay Seebeck effect: hot and cold sources on opposite faces; no mechanical or electrical input. */
public final class ThermoelectricGeneratorBlockEntity extends BlockEntity implements EnergyInspectable, MachineStatusInspectable {
    public static final int CAPACITY=480,TRANSFER=32;
    private final SimpleEnergyHandler battery=new SimpleEnergyHandler(CAPACITY,TRANSFER,TRANSFER);
    private final TransferBudget budget=new TransferBudget(TRANSFER);
    private String operator="";
    private int generation;
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
    private static boolean isColdSource(BlockState state) {
        return state.getFluidState().is(FluidTags.WATER) || state.is(Blocks.ICE)
                || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE) || state.is(Blocks.SNOW_BLOCK);
    }

    /** Pure classification for one opposing pair; either direction is valid. */
    public static int opposingPairRate(BlockState a, BlockState b) {
        int heat = Math.max(isColdSource(b) ? BellowsBrazierBlockEntity.classifyHeatSource(a) : 0,
                isColdSource(a) ? BellowsBrazierBlockEntity.classifyHeatSource(b) : 0);
        return heat == 2 ? 3 : heat == 1 ? 1 : 0;
    }

    /** Select the strongest opposing pair, never sum them or load adjacent chunks. */
    public static int generationRate(ServerLevel level, BlockPos pos) {
        int best = 0;
        for (Direction face : new Direction[]{Direction.UP, Direction.EAST, Direction.NORTH}) {
            BlockPos a = pos.relative(face);
            BlockPos b = pos.relative(face.getOpposite());
            if (!level.hasChunkAt(a) || !level.hasChunkAt(b)) continue;
            best = Math.max(best, opposingPairRate(level.getBlockState(a), level.getBlockState(b)));
        }
        return best;
    }

    public void tick(){
        if(!(level instanceof ServerLevel server))return;
        generation=0;
        int rate = generationRate(server, worldPosition);
        if(rate>0&&ConversionAuthorization.permitted(level,operator,Kind.THERMOELECTRIC)){
            try(var tx=Transaction.openRoot()){
                generation=battery.insert(rate,tx);if(generation>0){tx.commit();setChanged();}
            }
        }
        ConversionPower.distribute(server,worldPosition,this::energy,this::extractEnergy,budget);
        if(server.getGameTime()%10==0)server.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);
    }
    @Override public Component statusTitle(){return inspectTitle();}
    @Override public Component statusDetail(){return Component.translatable("message.quantumindustries.thermoelectric_generator.status",energy(),CAPACITY,generation);}
    @Override public State statusState(){return generation>0?State.WORKING:energy()==CAPACITY?State.READY:State.BLOCKED;}
    @Override public float statusProgress(){return (float)energy()/CAPACITY;}
    @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);out.putInt("stored_fe",energy());out.putString("operator",operator);out.putInt("generation",generation);}
    @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);battery.set(Math.clamp(in.getIntOr("stored_fe",0),0,CAPACITY));operator=in.getStringOr("operator","");generation=Math.clamp(in.getIntOr("generation",0),0,3);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveCustomOnly(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
