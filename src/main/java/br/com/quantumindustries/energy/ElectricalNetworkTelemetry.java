package br.com.quantumindustries.energy;

import br.com.quantumindustries.block.CopperCableBlock;
import br.com.quantumindustries.block.entity.CopperCableBlockEntity;
import br.com.quantumindustries.block.entity.EnergyInspectable;
import br.com.quantumindustries.logistics.ConfigurableConduit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import java.util.*;

/** Read-only, physically connected scope; never loads chunks or transfers FE. */
public final class ElectricalNetworkTelemetry {
    public static final int RADIUS=16, MAX_NODES=128, TOP=6;
    private ElectricalNetworkTelemetry(){}
    public record CableReading(BlockPos pos,int peak,int limit,int throughput) {}
    public record Snapshot(boolean connected,boolean partial,int cables,int endpoints,
            long stored,long capacity,int injected,int delivered,int losses,
            int saturated,List<CableReading> busiest) {
        public static final Snapshot EMPTY=new Snapshot(false,false,0,0,0,0,0,0,0,0,List.of());
    }
    public static Snapshot scan(ServerLevel level,BlockPos origin,BlockPos anchor){
        if(!level.hasChunkAt(anchor)||!(level.getBlockEntity(anchor) instanceof CopperCableBlockEntity))return Snapshot.EMPTY;
        Set<BlockPos> cables=new LinkedHashSet<>(),endpoints=new LinkedHashSet<>();
        ArrayDeque<BlockPos> queue=new ArrayDeque<>();
        cables.add(anchor.immutable());queue.add(anchor.immutable());
        boolean partial=false;
        long stored=0,capacity=0;
        int injected=0,delivered=0,losses=0,saturated=0;
        List<CableReading> readings=new ArrayList<>();
        long time=level.getGameTime();
        while(!queue.isEmpty()){
            BlockPos pos=queue.removeFirst();
            if(!(level.getBlockEntity(pos) instanceof CopperCableBlockEntity be))continue;
            var sample=be.completedTelemetry(time);
            injected+=sample.injected();delivered+=sample.delivered();losses+=sample.loss();
            int limit=be.capacity();
            if(limit>0&&sample.peakFlow()>=limit)saturated++;
            readings.add(new CableReading(pos,sample.peakFlow(),limit,sample.throughput()));
            for(Direction side:Direction.values()){
                var own=ConfigurableConduit.modeAt(level,pos,side);
                if(!own.allowsInput()&&!own.allowsOutput())continue;
                BlockPos next=pos.relative(side);
                if(cables.contains(next)||endpoints.contains(next))continue;
                if(!level.hasChunkAt(next)){partial=true;continue;}
                boolean cable=level.getBlockState(next).getBlock() instanceof CopperCableBlock;
                var target=level.getBlockEntity(next);
                if(cable&&!(target instanceof CopperCableBlockEntity)){partial=true;continue;}
                if(!cable&&(!(target instanceof EnergyInspectable)||!EnergyPorts.connects(level.getBlockState(next),side.getOpposite())))continue;
                var other=ConfigurableConduit.modeAt(level,next,side.getOpposite());
                if(!(own.allowsInput()&&other.allowsOutput()||own.allowsOutput()&&other.allowsInput()))continue;
                if(next.distSqr(origin)>RADIUS*RADIUS||cables.size()+endpoints.size()>=MAX_NODES){partial=true;continue;}
                if(cable){
                    cables.add(next.immutable());queue.addLast(next.immutable());
                }else{
                    endpoints.add(next.immutable());
                    EnergyInspectable e=(EnergyInspectable)target;
                    stored+=Math.max(0,e.energy());capacity+=Math.max(0,e.capacity());
                }
            }
        }
        readings.sort(Comparator.<CableReading>comparingDouble(r->r.limit()>0?(double)r.peak()/r.limit():0).reversed()
                .thenComparing(Comparator.comparingInt(CableReading::throughput).reversed())
                .thenComparingLong(r->r.pos().asLong()));
        return new Snapshot(true,partial,cables.size(),endpoints.size(),stored,capacity,injected,delivered,losses,
                saturated,List.copyOf(readings.subList(0,Math.min(TOP,readings.size()))));
    }
}
