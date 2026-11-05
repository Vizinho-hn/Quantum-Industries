package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.assay.*;
import br.com.quantumindustries.assay.AssayCatalog.*;
import br.com.quantumindustries.core.ModBlocks;
import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.*;

/** Server-owned apparatus. Menus observe it and never advance the experiment. */
public final class EmpiricalAssayBenchBlockEntity extends BlockEntity {
    private boolean loading;
    public int activeTestStage=-1, progress, maxProgress=200, burnTime, burnDuration=200;
    private String tester="", researchOwner="";
    private Material researchMaterial;
    private String researcherName="";
    public String researcherName(){return researcherName;}
    private int completedMask;
    private ItemStack sample=ItemStack.EMPTY;
    private final SimpleContainer inventory=new SimpleContainer(4){
        @Override public void setChanged(){super.setChanged();if(!loading)sync();}
    };
    public EmpiricalAssayBenchBlockEntity(BlockPos pos,BlockState state){super(ModBlocks.EMPIRICAL_ASSAY_ENTITY.get(),pos,state);}
    public SimpleContainer inventory(){return inventory;}
    public int toolMask(){
        int mask=0;
        if(inventory.getItem(1).is(br.com.quantumindustries.ModItems.MAGNIFYING_GLASS.get()))mask|=1;
        if(inventory.getItem(2).is(br.com.quantumindustries.ModItems.MANUAL_BELLOWS.get()))mask|=2;
        if(inventory.getItem(3).is(br.com.quantumindustries.ModItems.ASSAY_FLASK.get()))mask|=4;
        return mask;
    }
    public ItemStack sample(){return sample.isEmpty()?inventory.getItem(0):sample;}
    public Material researchMaterial(){return researchMaterial;}
    public boolean ownedBy(Player player){return researchOwner.isEmpty()||researchOwner.equals(player.getUUID().toString());}
    public boolean busy(){return activeTestStage>=0;}
    public Method method(){return busy()?Method.values()[activeTestStage]:null;}
    public int temperature(){return 25+(method()==Method.HEATING?progress*1075/maxProgress:0);}
    public boolean start(Player player,Material material,Method method){
        if(!(level instanceof ServerLevel)||busy()||!ownedBy(player)||(researchMaterial!=null&&researchMaterial!=material))return false;
        var profile=AssayProfile.of(player);
        if(profile.knows(material,method))return false;
        if(researchMaterial==null){
            boolean legacy=profile.reserved()==material;
            if(!legacy && AssayCatalog.material(inventory.getItem(0))!=material)return false;
            if(!legacy&&!profile.reserve(material))return false;
            sample=legacy?material.sample():inventory.removeItem(0,1);
            researchMaterial=material;researchOwner=player.getUUID().toString();researcherName=player.getName().getString();completedMask=profile.mask(material);
        }
        tester=player.getUUID().toString();activeTestStage=method.ordinal();progress=0;
        burnTime=method==Method.HEATING?burnDuration:0;sync();return true;
    }
    private void sync(){setChanged();if(!loading&&level!=null&&!level.isClientSide())level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    /** Model-local apparatus coordinate transformed through the block facing. */
    public net.minecraft.world.phys.Vec3 apparatus(double x,double y,double z){
        var facing=getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        double dx=x/16-.5,dz=z/16-.5;
        double rx=switch(facing){case EAST->-dz;case SOUTH->-dx;case WEST->dz;default->dx;};
        double rz=switch(facing){case EAST->dx;case SOUTH->-dz;case WEST->-dx;default->dz;};
        return new net.minecraft.world.phys.Vec3(worldPosition.getX()+.5+rx,worldPosition.getY()+y/16,worldPosition.getZ()+.5+rz);
    }
    public void tick(){
        if(!(level instanceof ServerLevel server)||!busy())return;
        progress=Math.min(maxProgress,progress+1);if(method()==Method.HEATING)burnTime=Math.max(0,burnDuration-progress);
        if(progress>=30&&progress<185)effects(server);
        if(progress>=maxProgress){
            var delivery=AssayDelivery.of(server);delivery.enqueue(tester,researchMaterial,method());
            var author=server.getServer().getPlayerList().getPlayer(java.util.UUID.fromString(tester));
            if(author!=null)delivery.deliver(author);
            completedMask|=1<<activeTestStage;activeTestStage=-1;burnTime=0;
            if(completedMask==AssayCatalog.COMPLETE_MASK){sample=ItemStack.EMPTY;researchMaterial=null;researchOwner="";}
        }
        sync();
    }
    private void effects(ServerLevel server){
        Method method=method();var p=apparatus(method==Method.MECHANICAL?8:method==Method.INSPECTION?16:25,
            method==Method.HEATING?19:method==Method.REACTION?20:18,method==Method.HEATING?4:method==Method.REACTION?12:8);
        if(method==Method.MECHANICAL&&progress==145){
            sound(server,SoundEvents.ANVIL_STEP,.35f,.6f);
            server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM,sample.getItem()),p.x,p.y,p.z,6,.06,.03,.06,.015);
        }
        if(method==Method.HEATING){
            if(progress%30==0)sound(server,SoundEvents.FURNACE_FIRE_CRACKLE,.3f,1f);
            if(progress%8==0)server.sendParticles(ParticleTypes.SMOKE,p.x,p.y+.15,p.z,1,.03,.04,.03,.008);
        }
        if(method==Method.REACTION&&researchMaterial.reactive()&&progress>=65&&progress%12==0){
            server.sendParticles(ParticleTypes.BUBBLE_POP,p.x,p.y,p.z,2,.05,.04,.05,.01);
            if(progress%36==0)sound(server,SoundEvents.BREWING_STAND_BREW,.18f,1.2f);
        }
        if(method==Method.INSPECTION&&progress==35)sound(server,SoundEvents.SPYGLASS_USE,.25f,.8f);
    }
    private void sound(ServerLevel level,SoundEvent event,float volume,float pitch){level.playSound(null,worldPosition,event,SoundSource.BLOCKS,volume,pitch);}
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);out.putInt("assay_version",2);
        out.putString("method",method()==null?"":method().id());out.putInt("progress",progress);
        out.putInt("maxProgress",maxProgress);out.putInt("burnTime",burnTime);out.putInt("burnDuration",burnDuration);
        out.putString("tester",tester);out.putString("research_owner",researchOwner);out.putString("researcher_name",researcherName);
        out.putString("research_material",researchMaterial==null?"":researchMaterial.id());out.putInt("completed_mask",completedMask);
        if(!sample.isEmpty())out.store("sample",ItemStack.CODEC,sample);
        for(int i=0;i<4;i++)if(!inventory.getItem(i).isEmpty())out.store("input_"+i,ItemStack.CODEC,inventory.getItem(i));
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);loading=true;
        try{
            var method=AssayCatalog.method(in.getStringOr("method",""));
            if(in.getIntOr("assay_version",1)<2){
                // Old ordinals were mechanical/heating/galvanic. Never reinterpret galvanic as chemistry.
                method=switch(in.getIntOr("stage",-1)){case 0->Method.MECHANICAL;case 1->Method.HEATING;default->null;};
            }
            activeTestStage=method==null?-1:method.ordinal();progress=Math.clamp(in.getIntOr("progress",0),0,200);
            burnTime=Math.clamp(in.getIntOr("burnTime",0),0,200);tester=in.getStringOr("tester","");
            researcherName=in.getStringOr("researcher_name","");researchOwner=in.getStringOr("research_owner",tester);researchMaterial=null;
            String material=in.getStringOr("research_material","");
            for(var m:Material.values())if(m.id().equals(material))researchMaterial=m;
            sample=in.read("sample",ItemStack.CODEC).orElse(ItemStack.EMPTY);
            if(researchMaterial==null&&!sample.isEmpty())researchMaterial=AssayCatalog.material(sample);
            completedMask=in.getIntOr("completed_mask",0)&AssayCatalog.COMPLETE_MASK;
            try{if(!tester.isEmpty())java.util.UUID.fromString(tester);}catch(IllegalArgumentException e){tester="";}
            if(tester.isEmpty()||researchMaterial==null)activeTestStage=-1;
            for(int i=0;i<4;i++)inventory.setItem(i,in.read("input_"+i,ItemStack.CODEC).orElse(ItemStack.EMPTY));
        }finally{loading=false;}
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveCustomOnly(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public void preRemoveSideEffects(BlockPos pos,BlockState state){
        super.preRemoveSideEffects(pos,state);if(level==null||level.isClientSide())return;
        for(int i=0;i<4;i++){var stack=inventory.removeItemNoUpdate(i);if(!stack.isEmpty())Block.popResource(level,pos,stack);}
        // Reserved portions have already been consumed. They are never dropped as a second item.
    }
}
