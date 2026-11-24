package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.block.IndustrialConversionBlock;
import br.com.quantumindustries.block.IndustrialConversionBlock.Kind;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.energy.ConversionPower;
import br.com.quantumindustries.energy.GeneratorModel;
import br.com.quantumindustries.energy.TransferBudget;
import br.com.quantumindustries.mechanical.KineticNetworkManager;
import br.com.quantumindustries.mechanical.RotationalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Sources pay for the following tick before advertising QKS torque. Generators are sinks. */
public final class RotaryConversionBlockEntity extends RotationalBlockEntity implements EnergyInspectable, MachineStatusInspectable {
    public static final int MOTOR_COST=24, MOTOR_CAPACITY=18, STEAM_COST=10, STEAM_CAPACITY=24, GENERATOR_LOAD=12, GENERATOR_MAX=8;
    private static final int BUFFER=1200, TRANSFER=64;
    private final SimpleEnergyHandler battery=new SimpleEnergyHandler(BUFFER,TRANSFER,TRANSFER);
    private final TransferBudget budget=new TransferBudget(TRANSFER);
    private String operator="";
    private boolean enabled=true;
    private long paidTick=Long.MIN_VALUE, activePaidTick=Long.MIN_VALUE;
    private int lastGenerated;
    private int excitation, field, generationRemainder, rotorHeat;
    private boolean thermalTrip;
    public RotaryConversionBlockEntity(BlockPos pos, BlockState state) { super(ModBlocks.ROTARY_CONVERSION_ENTITY.get(),pos,state); }
    public Kind kind() { return ((IndustrialConversionBlock)getBlockState().getBlock()).kind(); }
    public boolean isMotor() { return kind()==Kind.MOTOR; }
    private boolean generator() { return kind()==Kind.GENERATOR; }
    public boolean isGenerator() { return generator(); }
    public boolean isEnabled() { return enabled; }
    public int rotorHeat() { return rotorHeat; }
    public boolean isThermalTrip() { return thermalTrip; }
    public int excitation() { return excitation; }
    public int field() { return field; }
    public int lastGenerated() { return lastGenerated; }
    public boolean needsExcitation() { return generator() && enabled && field==0 && excitation<GeneratorModel.FIELD_START && authorized(); }
    private boolean authorized() { return ConversionAuthorization.permitted(level,operator,kind()); }
    public boolean bind(Player player) {
        if (!ConversionAuthorization.canBind(level,worldPosition,player,kind())) return false;
        if (operator.isEmpty()) { operator=player.getUUID().toString(); changed(); }
        return authorized();
    }
    public void toggle(Player player) {
        if (bind(player)) { enabled=!enabled; paidTick=activePaidTick=Long.MIN_VALUE; changed(); }
    }
    public Direction mechanicalFace() { return IndustrialConversionBlock.facing(getBlockState()).getClockWise(); }
    @Override public Direction.Axis axle() { return mechanicalFace().getAxis(); }
    @Override public boolean connectsMechanical(Direction side) { return side==mechanicalFace(); }
    @Override public boolean allowsCrank() { return false; }
    @Override public double kineticInertia() { return kind()==Kind.STEAM_ENGINE ? 4.0 : 2.8; }
    @Override public double kineticDrag() { return .24; }
    @Override public int sourceCapacity() {
        if (level==null || !enabled || !authorized() || (level.getGameTime()!=paidTick && level.getGameTime()!=activePaidTick)) return 0;
        return kind()==Kind.MOTOR ? MOTOR_CAPACITY : kind()==Kind.STEAM_ENGINE ? STEAM_CAPACITY : 0;
    }
    @Override public int requestedLoad() {
        return generator() && enabled && authorized() && !thermalTrip
                ? GeneratorModel.load(field>0 || excitation>=GeneratorModel.FIELD_START,energy(),BUFFER,lastGenerated) : 0;
    }
    @Override public int energy() { return kind()==Kind.STEAM_ENGINE ? 0 : (int)battery.getAmountAsLong(); }
    @Override public int capacity() { return kind()==Kind.STEAM_ENGINE ? 0 : BUFFER; }
    @Override public Component inspectTitle() { return Component.translatable("block.quantumindustries."+kind().id); }
    @Override public Component inspectDetail() { return statusDetail(); }
    @Override public int receiveEnergy(int offered) {
        if (!(level instanceof ServerLevel server) || offered<=0) return 0;
        if (needsExcitation()) {
            int n=Math.min(Math.min(offered,budget.receiveRemaining(server.getGameTime())),GeneratorModel.FIELD_START-excitation);
            excitation+=n;budget.recordReceive(server.getGameTime(),n);if(n>0)setChanged();return n;
        }
        if (!isMotor()) return 0;
        int n;
        try(var tx=Transaction.openRoot()) {
            n=battery.insert(Math.min(offered,budget.receiveRemaining(server.getGameTime())),tx);
            if(n>0)tx.commit();
        }
        if(n>0){budget.recordReceive(server.getGameTime(),n);setChanged();}
        return n;
    }
    @Override public int extractEnergy(int offered) {
        if (!(level instanceof ServerLevel server) || !generator() || offered<=0) return 0;
        int n;
        try(var tx=Transaction.openRoot()) {
            n=battery.extract(Math.min(offered,budget.extractRemaining(server.getGameTime())),tx);
            if(n>0)tx.commit();
        }
        if(n>0){budget.recordExtract(server.getGameTime(),n);setChanged();}
        return n;
    }
    public void tick() {
        if (!(level instanceof ServerLevel server)) return;
        if (paidTick==server.getGameTime()) activePaidTick=paidTick;
        var snap=KineticNetworkManager.snapshot(server,worldPosition);
        applyKineticSnapshot(snap);
        lastGenerated=0;
        if (generator()) {
            tickAlternator(snap.powered(), Math.abs(rpm()));
        } else if (enabled && authorized()) {
            if (isMotor() && energy()>=MOTOR_COST) {
                try(var tx=Transaction.openRoot()){
                    if(battery.extract(MOTOR_COST,tx)==MOTOR_COST){tx.commit();paidTick=server.getGameTime()+1;setChanged();}
                }
            } else if (kind()==Kind.STEAM_ENGINE) {
                Direction inlet=IndustrialConversionBlock.facing(getBlockState()).getOpposite();
                BlockPos source=worldPosition.relative(inlet);
                if (server.hasChunkAt(source) && server.getBlockEntity(source) instanceof SteamBoilerBlockEntity boiler
                        && boiler.takeSteam(STEAM_COST,inlet.getOpposite())==STEAM_COST) {
                    paidTick=server.getGameTime()+1;setChanged();
                }
            }
        }
        if(generator()) ConversionPower.distribute(server,worldPosition,this::energy,this::extractEnergy,budget);
        if(server.getGameTime()%10==0)server.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);
    }
    private void tickAlternator(boolean powered,double speed){
        rotorHeat=GeneratorModel.heat(rotorHeat,speed);
        if(rotorHeat>=GeneratorModel.MAX_HEAT)thermalTrip=true;
        if(thermalTrip&&rotorHeat<=40)thermalTrip=false;
        if(!enabled||!authorized()||!powered||thermalTrip){
            if(field>0)field--;generationRemainder=0;setChanged();return;
        }
        if(field==0&&excitation>=GeneratorModel.FIELD_START&&speed>=12){
            excitation-=GeneratorModel.FIELD_START;field=GeneratorModel.MAX_FIELD;
        }
        if(field==0){generationRemainder=0;setChanged();return;}
        int gross=GeneratorModel.grossRate(speed);
        // Self-excitation is paid first. At low speed the field reserve decays.
        if(gross<GeneratorModel.SCALE||energy()>=BUFFER)field--;
        if(energy()>=BUFFER||gross<=GeneratorModel.SCALE){generationRemainder=0;setChanged();return;}
        int scaled=GeneratorModel.netRate(speed)+generationRemainder;
        int produced=scaled/GeneratorModel.SCALE;
        generationRemainder=scaled%GeneratorModel.SCALE;
        if(produced>0)try(var tx=Transaction.openRoot()){
            lastGenerated=battery.insert(produced,tx);
            if(lastGenerated>0)tx.commit();
        }
        if(lastGenerated<produced||energy()>=BUFFER)generationRemainder=0;
        setChanged();
    }
    @Override public Component statusTitle(){return inspectTitle();}
    @Override public Component statusDetail(){
        if(generator())return Component.translatable("message.quantumindustries.alternator.status",energy(),capacity(),Math.round(rpm()),lastGenerated,field>0?field:excitation,GeneratorModel.FIELD_START,rotorHeat,
                Component.translatable("message.quantumindustries.alternator."+(thermalTrip?"overheat":!enabled?"disabled":field==0&&excitation<GeneratorModel.FIELD_START?"excitation":"ready")));
        return Component.translatable("message.quantumindustries.rotary_conversion.status",energy(),capacity(),Math.round(rpm()),lastGenerated);
    }
    @Override public State statusState(){return rotating()&&isSpinning()?State.WORKING:enabled?State.BLOCKED:State.IDLE;}
    @Override public float statusProgress(){return capacity()>0?(float)energy()/capacity():0;}
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);out.putInt("stored_fe",energy());out.putString("operator",operator);out.putBoolean("enabled",enabled);out.putInt("last_generated",lastGenerated);
        out.putInt("excitation",excitation);out.putInt("field",field);out.putInt("generation_remainder",generationRemainder);out.putInt("rotor_heat",rotorHeat);out.putBoolean("thermal_trip",thermalTrip);
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);battery.set(Math.clamp(in.getIntOr("stored_fe",0),0,BUFFER));operator=in.getStringOr("operator","");
        enabled=in.getBooleanOr("enabled",true);lastGenerated=Math.clamp(in.getIntOr("last_generated",0),0,GENERATOR_MAX);paidTick=activePaidTick=Long.MIN_VALUE;
        excitation=Math.clamp(in.getIntOr("excitation",0),0,GeneratorModel.FIELD_START);
        field=Math.clamp(in.getIntOr("field",0),0,GeneratorModel.MAX_FIELD);
        generationRemainder=Math.clamp(in.getIntOr("generation_remainder",0),0,GeneratorModel.SCALE-1);
        rotorHeat=Math.clamp(in.getIntOr("rotor_heat",0),0,GeneratorModel.MAX_HEAT);
        thermalTrip=in.getBooleanOr("thermal_trip",false);
    }
}
