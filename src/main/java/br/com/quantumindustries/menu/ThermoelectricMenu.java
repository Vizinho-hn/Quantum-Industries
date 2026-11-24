package br.com.quantumindustries.menu;

import br.com.quantumindustries.block.entity.ThermoelectricGeneratorBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.core.ModMenuTypes;
import br.com.quantumindustries.energy.ThermalHistory;
import br.com.quantumindustries.energy.ThermalModules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class ThermoelectricMenu extends AbstractContainerMenu {
    public static final int SLOTS=4, HEADER=12;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final ThermoelectricGeneratorBlockEntity machine;
    public ThermoelectricMenu(int id,Inventory inv,BlockPos pos){
        super(ModMenuTypes.THERMOELECTRIC_MENU.get(),id);
        access=ContainerLevelAccess.create(inv.player.level(),pos);
        var entity=inv.player.level().getBlockEntity(pos);
        machine=entity instanceof ThermoelectricGeneratorBlockEntity t&&!inv.player.level().isClientSide()?t:null;
        Container items=machine==null?new SimpleContainer(SLOTS):machine.modules();
        int size=HEADER+ThermalHistory.CHANNELS*ThermalHistory.SIZE;
        data=machine==null?new SimpleContainerData(size):new ContainerData(){
            @Override public int get(int i){
                return switch(i){
                    case 0->machine.energy();case 1->machine.hotTemperature();case 2->machine.coldTemperature();
                    case 3->machine.ambientTemperature();case 4->machine.efficiency();case 5->machine.netPower();
                    case 6->machine.coolantRemaining();case 7->machine.conditionCode();case 8->machine.thermalInput();
                    case 9->machine.thermalExchange();case 10->machine.history().count();case 11->machine.history().cursor();
                    default->machine.history().physical((i-HEADER)/ThermalHistory.SIZE,(i-HEADER)%ThermalHistory.SIZE);
                };
            }
            @Override public void set(int i,int value){}
            @Override public int getCount(){return size;}
        };
        for(int i=0;i<SLOTS;i++)addSlot(new Slot(items,i,14+i*20,124){
            @Override public boolean mayPlace(ItemStack stack){return allowed(inv.player)&&ThermalModules.accepts(stack);}
            @Override public boolean mayPickup(Player player){return allowed(player);}
            @Override public int getMaxStackSize(){return 1;}
        });
        for(int r=0;r<3;r++)for(int c=0;c<9;c++)addSlot(new Slot(inv,9+r*9+c,83+c*18,156+r*18));
        for(int c=0;c<9;c++)addSlot(new Slot(inv,c,83+c*18,214));
        addDataSlots(data);
    }
    private boolean allowed(Player p){return p.level().isClientSide()||machine!=null&&p.level().getBlockEntity(machine.getBlockPos())==machine&&machine.canConfigure(p);}
    public int value(int index){return data.get(index);}
    public int count(){return Math.clamp(data.get(10),0,ThermalHistory.SIZE);}
    public int sample(int channel,int index){
        int physical=Math.floorMod(data.get(11)-count()+index,ThermalHistory.SIZE);
        return data.get(HEADER+channel*ThermalHistory.SIZE+physical);
    }
    @Override public boolean stillValid(Player p){return allowed(p)&&stillValid(access,p,ModBlocks.THERMOELECTRIC_GENERATOR.get());}
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(!allowed(p)||index<0||index>=slots.size()||!slots.get(index).hasItem())return ItemStack.EMPTY;
        Slot slot=slots.get(index);
        ItemStack stack=slot.getItem(),copy=stack.copy();
        if(index<SLOTS){
            if(!moveItemStackTo(stack,SLOTS,slots.size(),true))return ItemStack.EMPTY;
        }else if(!ThermalModules.accepts(stack)||!moveItemStackTo(stack,0,SLOTS,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        slot.onTake(p,stack);return copy;
    }
}
