package br.com.quantumindustries.inspection;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Dispatches server-side inspection requests to registered subsystem providers. */
public final class InspectionRegistry {
    private static final List<InspectionProvider> PROVIDERS = List.of(
            new MechanicalInspectionProvider(),
            new EnergyInspectionProvider(),
            new FluidInspectionProvider(),
            new PneumaticInspectionProvider()
    );

    private InspectionRegistry() {}

    public static InspectionSnapshot inspect(ServerLevel level, BlockPos pos, boolean includeTopology) {
        for (InspectionProvider provider : PROVIDERS) {
            var snapshot = provider.inspect(level, pos, includeTopology);
            if (snapshot.isPresent()) return snapshot.get();
        }
        return InspectionSnapshot.empty(pos);
    }
}
