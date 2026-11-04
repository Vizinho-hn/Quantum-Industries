package br.com.quantumindustries.menu;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.assay.AssayCatalog;
import br.com.quantumindustries.assay.AssayCatalog.Material;
import br.com.quantumindustries.assay.AssayCatalog.Method;
import br.com.quantumindustries.assay.AssayProfile;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.core.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** The bench owns physical inputs; the player owns discoveries and reserve. */
public class EmpiricalAssayBenchMenu extends AbstractContainerMenu {
    private static final int VIEW=0, RESERVED=1, SELECTED=2, PROGRESS=3, RUNNING=4, MASKS=5;
    private final SimpleContainer inputs;
    private final ContainerData data=new SimpleContainerData(MASKS+AssayCatalog.MATERIAL_COUNT);
    private final ContainerLevelAccess access;
    private final Player owner;
    private final AssayProfile profile;
    private boolean closed;
    private Material testingMaterial;
    private Method testingMethod;

    public EmpiricalAssayBenchMenu(int id, Inventory inv, BlockPos pos) {
        super(ModMenuTypes.EMPIRICAL_ASSAY_MENU.get(),id);
        owner=inv.player;
        access=ContainerLevelAccess.create(owner.level(),pos);
        var entity=owner.level().getBlockEntity(pos);
        // Worlds created before benches had block entities are upgraded on first use.
        if(entity==null && !owner.level().isClientSide() && owner.level().getBlockState(pos).is(ModBlocks.EMPIRICAL_ASSAY_BENCH.get())) {
            entity=new br.com.quantumindustries.block.entity.EmpiricalAssayBenchBlockEntity(pos,owner.level().getBlockState(pos));
            owner.level().setBlockEntity(entity);
        }
        inputs=entity instanceof br.com.quantumindustries.block.entity.EmpiricalAssayBenchBlockEntity bench
            ? bench.inventory() : new SimpleContainer(4);
        profile=owner.level().isClientSide()?null:AssayProfile.of(owner);
        int[][] coords={{28,54},{9,106},{30,106},{51,106}};
        for(int i=0;i<4;i++) {
            final int index=i;
            addSlot(new Slot(inputs,i,coords[i][0],coords[i][1]) {
                @Override public boolean mayPlace(ItemStack stack){return valid(index,stack);}
                @Override public int getMaxStackSize(){return 1;}
            });
        }
        for(int r=0;r<3;r++)for(int col=0;col<9;col++)addSlot(new Slot(inv,col+r*9+9,82+col*18,155+r*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,82+col*18,213));
        if(profile!=null){syncProfile();if(profile.reserved()!=null)data.set(VIEW,profile.reserved().ordinal());}
        addDataSlots(data);
    }
    private static boolean valid(int slot,ItemStack s) {
        return switch(slot){
            case 0 -> AssayCatalog.material(s)!=null;
            case 1 -> s.is(ModItems.MAGNIFYING_GLASS.get());
            case 2 -> (s.is(net.minecraft.world.item.Items.FLINT_AND_STEEL)||s.is(ModItems.FLAME_TEST_KIT.get()));
            case 3 -> s.is(ModItems.BRASS_WEIGHTS.get());
            default -> false;
        };
    }
    private void syncProfile(){
        Material reserve=profile.reserved();data.set(RESERVED,reserve==null?0:reserve.ordinal()+1);
        for(var m:Material.values())data.set(MASKS+m.ordinal(),profile.mask(m));
    }
    public Material viewedMaterial(){return Material.values()[Math.clamp(data.get(VIEW),0,AssayCatalog.MATERIAL_COUNT-1)];}
    public Material reservedMaterial(){int index=data.get(RESERVED)-1;return index>=0&&index<AssayCatalog.MATERIAL_COUNT?Material.values()[index]:null;}
    public Method selectedMethod(){return Method.values()[Math.clamp(data.get(SELECTED),0,2)];}
    public boolean knows(Material material,Method method){return material!=null&&(data.get(MASKS+material.ordinal())&(1<<method.ordinal()))!=0;}
    public int discoveries(){return Integer.bitCount(data.get(MASKS+viewedMaterial().ordinal()));}
    public int totalDiscoveries(){int total=0;for(var m:Material.values())total+=Integer.bitCount(data.get(MASKS+m.ordinal()));return total;}
    public int progressTicks(){return data.get(PROGRESS);}
    public boolean running(){return data.get(RUNNING)!=0;}
    public int missingTools(){return missingTools(selectedMethod());}
    private int missingTools(Method method){
        int missing=valid(1,inputs.getItem(1))?0:1;
        if(method==Method.HEATING&&!valid(2,inputs.getItem(2)))missing|=2;
        if(method==Method.MECHANICAL&&!valid(3,inputs.getItem(3)))missing|=4;
        return missing;
    }
    public boolean ready(){
        Material material=reservedMaterial();if(material==null)material=AssayCatalog.material(inputs.getItem(0));
        return !closed&&!running()&&missingTools()==0&&material!=null&&!knows(material,selectedMethod());
    }
    @Override public boolean clickMenuButton(Player player,int button){
        if(profile==null||closed||player!=owner||player.containerMenu!=this||!player.isAlive()||!stillValid(player))return false;
        syncProfile();
        if(button>=10&&button<10+AssayCatalog.MATERIAL_COUNT){data.set(VIEW,button-10);broadcastChanges();return true;}
        if(button>=1&&button<=3){if(running())return false;data.set(SELECTED,button-1);broadcastChanges();return true;}
        if(button!=0||!ready())return false;
        Material material=profile.reserved();
        if(material==null){
            material=AssayCatalog.material(inputs.getItem(0));
            if(material==null||!profile.reserve(material))return false;
            inputs.removeItem(0,1);
        }
        testingMaterial=material;testingMethod=selectedMethod();
        data.set(VIEW,material.ordinal());data.set(PROGRESS,0);data.set(RUNNING,1);
        syncProfile();broadcastChanges();return true;
    }
    private void cancelTimer(){testingMaterial=null;testingMethod=null;data.set(PROGRESS,0);data.set(RUNNING,0);}
    public void serverTick(Player player){
        if(profile==null||closed)return;
        syncProfile();
        if(running()){
            if(player!=owner||player.containerMenu!=this||!player.isAlive()||!stillValid(player)
                ||testingMaterial!=profile.reserved()||testingMethod!=selectedMethod()||missingTools(testingMethod)!=0||profile.knows(testingMaterial,testingMethod)){
                cancelTimer();
            }else{
                data.set(PROGRESS,data.get(PROGRESS)+1);
                if(data.get(PROGRESS)>=AssayCatalog.TEST_TICKS){
                    profile.complete(testingMaterial,testingMethod);
                    cancelTimer();syncProfile();
                }
            }
        }
        broadcastChanges();
    }
    @Override public boolean stillValid(Player player){return player==owner&&!closed&&stillValid(access,player,ModBlocks.EMPIRICAL_ASSAY_BENCH.get());}
    @Override public void removed(Player player){
        super.removed(player);
        if(player==owner&&!closed){cancelTimer();closed=true;}
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(player!=owner||closed||index<0||index>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack current=slot.getItem(),copy=current.copy();
        if(index<4){if(!moveItemStackTo(current,4,40,true))return ItemStack.EMPTY;}
        else{boolean moved=false;for(int i=0;i<4;i++)if(valid(i,current)&&moveItemStackTo(current,i,i+1,false)){moved=true;break;}if(!moved)return ItemStack.EMPTY;}
        if(current.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,current);return copy;
    }
}
