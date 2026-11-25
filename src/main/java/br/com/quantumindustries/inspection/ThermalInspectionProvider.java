package br.com.quantumindustries.inspection;

import br.com.quantumindustries.block.entity.EnergyInspectable;
import br.com.quantumindustries.fluid.FluidInspectable;
import br.com.quantumindustries.mechanical.MechanicalParticipant;
import br.com.quantumindustries.thermal.ThermalInspectable;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/** Provides the primary inspection panel for dedicated thermal machines. */
public final class ThermalInspectionProvider implements InspectionProvider {
    @Override
    public Optional<InspectionSnapshot> inspect(ServerLevel level, BlockPos pos, boolean includeTopology) {
        if (!level.hasChunkAt(pos)) return Optional.empty();
        var entity = level.getBlockEntity(pos);
        if (!(entity instanceof ThermalInspectable thermal) || entity instanceof EnergyInspectable
                || entity instanceof FluidInspectable || entity instanceof MechanicalParticipant) {
            return Optional.empty();
        }
        return Optional.of(new InspectionSnapshot(pos, InspectionSnapshot.Kind.THERMAL, Direction.Axis.Y,
                0, 0, 0, false, false, 1, 0, 0, false, 0, includeTopology ? List.of(pos) : List.of())
                .withThermal(thermal.thermalSnapshot(), includeTopology));
    }
}
