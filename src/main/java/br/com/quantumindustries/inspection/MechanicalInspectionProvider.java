package br.com.quantumindustries.inspection;

import br.com.quantumindustries.block.entity.PrimitiveWaterWheelBlockEntity;
import br.com.quantumindustries.mechanical.MechanicalNetwork;
import br.com.quantumindustries.mechanical.MechanicalParticipant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Exposes real MechanicalNetwork measurements without inventing unsimulated quantities. */
public final class MechanicalInspectionProvider implements InspectionProvider {
    private static final double TOPOLOGY_RADIUS_SQR = 12.0 * 12.0;

    @Override
    public Optional<InspectionSnapshot> inspect(ServerLevel level, BlockPos pos, boolean includeTopology) {
        if (!level.hasChunkAt(pos)) return Optional.empty();
        if (!(level.getBlockEntity(pos) instanceof MechanicalParticipant participant)) return Optional.empty();

        MechanicalNetwork.Result<BlockPos> result = MechanicalNetwork.state(level, pos);
        List<BlockPos> syncedNodes = List.of();
        if (includeTopology && !result.nodes().isEmpty()) {
            syncedNodes = result.nodes().stream()
                    .filter(node -> node.distSqr(pos) <= TOPOLOGY_RADIUS_SQR)
                    .sorted(Comparator.comparingDouble(node -> node.distSqr(pos)))
                    .limit(InspectionSnapshot.MAX_SYNCED_NODES)
                    .map(BlockPos::immutable)
                    .toList();
        }

        boolean hasTorque = participant instanceof PrimitiveWaterWheelBlockEntity;
        int torqueTenths = hasTorque
                ? (int) Math.round(((PrimitiveWaterWheelBlockEntity) participant).hydraulicTorque() * 10.0)
                : 0;

        return Optional.of(new InspectionSnapshot(
                pos.immutable(),
                InspectionSnapshot.Kind.MECHANICAL,
                participant.axle(),
                result.capacity(),
                result.load(),
                result.spinDirection(),
                result.powered(),
                result.limited(),
                result.nodes().size(),
                participant.sourceCapacity(),
                participant.requestedLoad(),
                hasTorque,
                torqueTenths,
                syncedNodes));
    }
}
