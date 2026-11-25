package br.com.quantumindustries.inspection;

import br.com.quantumindustries.thermal.ThermalInspectable;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Dispatches server-side inspection requests to registered subsystem providers. */
public final class InspectionRegistry {
    private static final List<InspectionProvider> PROVIDERS = List.of(
            new MechanicalInspectionProvider(),
            new EnergyInspectionProvider(),
            new FluidInspectionProvider(),
            new PneumaticInspectionProvider(),
            new ThermalInspectionProvider()
    );

    private InspectionRegistry() {}

    public static InspectionSnapshot inspect(ServerLevel level, BlockPos pos, boolean includeTopology) {
        for (InspectionProvider provider : PROVIDERS) {
            var result = provider.inspect(level, pos, includeTopology);
            if (result.isEmpty()) continue;
            var snapshot = result.get();
            if (!snapshot.hasThermal() && level.getBlockEntity(pos) instanceof ThermalInspectable thermal) {
                return snapshot.withThermal(thermal.thermalSnapshot(), includeTopology);
            }
            return snapshot;
        }
        return InspectionSnapshot.empty(pos);
    }
}
