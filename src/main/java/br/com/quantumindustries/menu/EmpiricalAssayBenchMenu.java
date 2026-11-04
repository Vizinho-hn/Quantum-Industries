package br.com.quantumindustries.menu;

import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.core.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

/** Work surface inventory is returned on closing; the player's notebook survives sessions. */
public class EmpiricalAssayBenchMenu extends AbstractContainerMenu {
    private final SimpleContainer inputs = new SimpleContainer(4);
    private final ContainerData data = new SimpleContainerData(2);
    private final ContainerLevelAccess access;
    public EmpiricalAssayBenchMenu(int id, Inventory inv, BlockPos pos) {
        super(ModMenuTypes.EMPIRICAL_ASSAY_MENU.get(), id);
        access = ContainerLevelAccess.create(inv.player.level(), pos);
        if (!inv.player.level().isClientSide()) data.set(0, inv.player.getPersistentData().getInt("quantum_empirical_copper").orElse(0));
        int[][] coords={{28,54},{9,106},{30,106},{51,106}};
        for(int i=0;i<4;i++) {
            final int slot=i;
            addSlot(new Slot(inputs,i,coords[i][0],coords[i][1]) {
                @Override public boolean mayPlace(ItemStack stack) { return valid(slot,stack); }
                @Override public int getMaxStackSize() { return 1; }
            });
        }
        for(int r=0;r<3;r++)for(int col=0;col<9;col++) addSlot(new Slot(inv,col+r*9+9,82+col*18,155+r*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,82+col*18,213));
        addDataSlots(data);
    }
    private static boolean valid(int slot, ItemStack s) {
        return switch(slot) {case 0->s.is(Items.COPPER_INGOT);case 1->s.is(Items.GLASS);case 2->s.is(Items.FLINT_AND_STEEL);case 3->s.is(Items.IRON_NUGGET);default->false;};
    }
    public int discoveries(){return data.get(0);}
    public int lastStage(){return data.get(1);}
    public boolean ready(){for(int i=0;i<4;i++)if(!valid(i,inputs.getItem(i)))return false;return true;}
    @Override public boolean clickMenuButton(Player player,int button) {
        if(button!=0||player.level().isClientSide()||!stillValid(player)||!ready())return false;
        int known=discoveries();if(known>=3)return false;
        data.set(0,known+1);data.set(1,known+1);
        player.getPersistentData().putInt("quantum_empirical_copper",known+1);
        broadcastChanges();return true;
    }
    @Override public boolean stillValid(Player player){return stillValid(access,player,ModBlocks.EMPIRICAL_ASSAY_BENCH.get());}
    @Override public void removed(Player player){super.removed(player);if(!player.level().isClientSide())clearContainer(player,inputs);}
    @Override public ItemStack quickMoveStack(Player player,int index) {
        Slot slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack current=slot.getItem(),copy=current.copy();
        if(index<4){if(!moveItemStackTo(current,4,40,true))return ItemStack.EMPTY;}
        else {boolean moved=false;for(int i=0;i<4;i++)if(valid(i,current)&&moveItemStackTo(current,i,i+1,false)){moved=true;break;}if(!moved)return ItemStack.EMPTY;}
        if(current.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        slot.onTake(player,current);return copy;
    }
}
