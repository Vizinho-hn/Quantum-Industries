package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.energy.ElectricalNetworkTelemetry;
import br.com.quantumindustries.energy.ElectricalNetworkTelemetry.Snapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Passive instrument: no FE buffer, no transfer capability, no chunk tickets. */
public final class ElectricalNetworkAnalyzerBlockEntity extends BlockEntity {
    public static final int HISTORY=60;
    private Snapshot snapshot=Snapshot.EMPTY;
    private final int[][] history=new int[3][HISTORY];
    private int cursor,count;
    public ElectricalNetworkAnalyzerBlockEntity(BlockPos pos,BlockState state){
        super(ModBlocks.ELECTRICAL_NETWORK_ANALYZER_ENTITY.get(),pos,state);
    }
    public void tick(){
        if(!(level instanceof ServerLevel server)||Math.floorMod(server.getGameTime(),20)!=Math.floorMod(worldPosition.asLong(),20))return;
        BlockPos anchor=worldPosition.relative(getBlockState().getValue(BlockStateProperties.FACING).getOpposite());
        snapshot=ElectricalNetworkTelemetry.scan(server,worldPosition,anchor);
        history[0][cursor]=snapshot.injected();
        history[1][cursor]=snapshot.delivered();
        history[2][cursor]=snapshot.losses();
        cursor=(cursor+1)%HISTORY;count=Math.min(HISTORY,count+1);
    }
    public Snapshot snapshot(){return snapshot;}
    public int cursor(){return cursor;}
    public int count(){return count;}
    public int history(int channel,int physicalIndex){return history[channel][physicalIndex];}
}
