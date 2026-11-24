package br.com.quantumindustries.menu;

import br.com.quantumindustries.block.ElectricalNetworkAnalyzerBlock;
import br.com.quantumindustries.block.entity.ElectricalNetworkAnalyzerBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.core.ModMenuTypes;
import br.com.quantumindustries.energy.ElectricalNetworkTelemetry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Read-only telemetry; wide values are split into two 16-bit data slots. */
public final class ElectricalNetworkAnalyzerMenu extends AbstractContainerMenu {
    public static final int HISTORY=ElectricalNetworkAnalyzerBlockEntity.HISTORY;
    public static final int TOP=ElectricalNetworkTelemetry.TOP;
    private static final int HEADER=19, TOP_FIELDS=6, HIST_BASE=HEADER+TOP*TOP_FIELDS,
            MAP_BASE=HIST_BASE+3*HISTORY*2, SIZE=MAP_BASE+ElectricalNetworkTelemetry.MAX_NODES*2;
    private final ContainerLevelAccess access;
    private final BlockPos pos;
    private final ElectricalNetworkAnalyzerBlockEntity analyzer;
    private final ContainerData data;
    public ElectricalNetworkAnalyzerMenu(int id,Inventory inv,BlockPos pos){
        super(ModMenuTypes.ELECTRICAL_NETWORK_ANALYZER_MENU.get(),id);
        this.pos=pos.immutable();
        access=ContainerLevelAccess.create(inv.player.level(),pos);
        var entity=inv.player.level().getBlockEntity(pos);
        analyzer=entity instanceof ElectricalNetworkAnalyzerBlockEntity a&&!inv.player.level().isClientSide()?a:null;
        data=analyzer==null?new SimpleContainerData(SIZE):new ContainerData(){
            @Override public int get(int index){return serverValue(index);}
            @Override public void set(int index,int value){}
            @Override public int getCount(){return SIZE;}
        };
        addDataSlots(data);
    }
    private int wideHeader(int index){
        var s=analyzer.snapshot();
        return switch(index){
            case 4->(int)Math.min(Integer.MAX_VALUE,s.stored());
            case 5->(int)Math.min(Integer.MAX_VALUE,s.capacity());
            case 6->s.injected();
            case 7->s.delivered();
            case 8->s.losses();
            default->0;
        };
    }
    private int serverValue(int i){
        var s=analyzer.snapshot();
        if(i>=4&&i<=8)return (short)wideHeader(i);
        if(i>=14&&i<HEADER)return (short)(wideHeader(i-10)>>>16);
        if(i<HEADER)return switch(i){
            case 0->s.connected()?1:0;case 1->s.partial()?1:0;
            case 2->s.cables();case 3->s.endpoints();case 9->s.saturated();
            case 10->analyzer.count();case 11->analyzer.cursor();
            case 12->s.busiest().size();case 13->s.nodes().size();default->0;
        };
        if(i<HIST_BASE){
            int n=(i-HEADER)/TOP_FIELDS,f=(i-HEADER)%TOP_FIELDS;
            if(n>=s.busiest().size())return 0;
            var c=s.busiest().get(n);
            return switch(f){
                case 0->c.pos().getX()-pos.getX();case 1->c.pos().getY()-pos.getY();case 2->c.pos().getZ()-pos.getZ();
                case 3->c.peak();case 4->c.limit();case 5->c.throughput();default->0;
            };
        }
        if(i<MAP_BASE){
            int n=(i-HIST_BASE)/2;
            int value=analyzer.history(n/HISTORY,n%HISTORY);
            return (short)((i-HIST_BASE)%2==0?value:value>>>16);
        }
        int n=(i-MAP_BASE)/2;
        if(n>=s.nodes().size())return 0;
        var node=s.nodes().get(n);
        if((i-MAP_BASE)%2==0)return (node.pos().getX()-pos.getX()+16)*64+node.pos().getZ()-pos.getZ()+16;
        return node.endpoint()?101:node.utilization();
    }
    private int wide(int lo,int hi){return (data.get(lo)&0xffff)|((data.get(hi)&0xffff)<<16);}
    public int value(int i){return i>=4&&i<=8?wide(i,i+10):data.get(i);}
    public int count(){return Math.clamp(value(10),0,HISTORY);}
    public int history(int channel,int index){
        int physical=Math.floorMod(value(11)-count()+index,HISTORY);
        int offset=HIST_BASE+2*(channel*HISTORY+physical);
        return wide(offset,offset+1);
    }
    public int top(int row,int field){return data.get(HEADER+row*TOP_FIELDS+field);}
    public int mapPosition(int row){return data.get(MAP_BASE+2*row);}
    public int mapStyle(int row){return data.get(MAP_BASE+2*row+1);}
    @Override public boolean stillValid(Player p){
        if(p.level().isClientSide())return true;
        return p.level() instanceof ServerLevel server&&analyzer!=null
                &&server.hasChunkAt(pos)&&server.getBlockEntity(pos)==analyzer
                &&stillValid(access,p,ModBlocks.ELECTRICAL_NETWORK_ANALYZER.get())
                &&ElectricalNetworkAnalyzerBlock.allowed(server,p,pos);
    }
    @Override public ItemStack quickMoveStack(Player p,int index){return ItemStack.EMPTY;}
}
