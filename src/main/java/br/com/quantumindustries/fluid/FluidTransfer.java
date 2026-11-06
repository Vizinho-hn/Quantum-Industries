package br.com.quantumindustries.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Atomically moves fluid through the NeoForge 26.1 resource-transfer capability. */
public final class FluidTransfer {
    private FluidTransfer() {}
    public static ResourceHandler<FluidResource> handler(ServerLevel level, BlockPos pos, Direction side) {
        return level.hasChunkAt(pos) ? level.getCapability(Capabilities.Fluid.BLOCK, pos, side) : null;
    }
    public static int move(ResourceHandler<FluidResource> source, ResourceHandler<FluidResource> target, int limit) {
        if(source==null||target==null||source==target||limit<=0)return 0;
        for(int slot=0;slot<source.size();slot++){
            FluidResource fluid=source.getResource(slot);
            if(fluid.isEmpty())continue;
            try(var tx=Transaction.openRoot()){

                int space=0;
                for(int i=0;i<target.size();i++){
                    if(!target.isValid(i,fluid))continue;
                    space+=Math.max(0,target.getCapacityAsInt(i,fluid)-target.getAmountAsInt(i));
                    if(space>=limit)break;
                }
                if(space<=0)continue;
                int taken=source.extract(slot,fluid,Math.min(space,limit),tx);
                if(taken<=0)continue;
                int accepted=target.insert(fluid,taken,tx);
                if(accepted!=taken)continue; // rollback BOTH handlers, no phantom fluid
                tx.commit();
                return accepted;
            }
        }
        return 0;
    }
}
