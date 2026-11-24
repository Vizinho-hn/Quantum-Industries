package br.com.quantumindustries.energy;

import br.com.quantumindustries.block.entity.EnergyInspectable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;

/** Cable and adjacent paths share exactly the same per-tick extraction allowance. */
public final class ConversionPower {
    private ConversionPower() {}
    public static void distribute(ServerLevel level, BlockPos pos, IntSupplier energy, IntUnaryOperator extract, TransferBudget budget) {
        long now=level.getGameTime();
        CableNetwork.transfer(level,pos,energy,extract,budget.extractRemaining(now));
        for (Direction side:Direction.values()) {
            int offer=Math.min(energy.getAsInt(),budget.extractRemaining(now));
            if (offer<=0) break;
            if (!EnergyPorts.connects(level.getBlockState(pos),side)) continue;
            BlockPos next=pos.relative(side);
            if (!level.hasChunkAt(next) || !EnergyPorts.connects(level.getBlockState(next),side.getOpposite())) continue;
            if (level.getBlockEntity(next) instanceof EnergyInspectable target) {
                int accepted=target.receiveEnergy(offer);
                if (accepted>0 && extract.applyAsInt(accepted)!=accepted)
                    throw new IllegalStateException("Conversion output violated extraction contract");
            }
        }
    }
}
