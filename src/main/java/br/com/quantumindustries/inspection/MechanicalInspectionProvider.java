package br.com.quantumindustries.inspection;

import br.com.quantumindustries.block.entity.PrimitiveWaterWheelBlockEntity;
import br.com.quantumindustries.block.entity.RimWaterWheelBlockEntity;
import br.com.quantumindustries.mechanical.MechanicalNetwork;
import br.com.quantumindustries.mechanical.KineticDynamics;
import br.com.quantumindustries.mechanical.KineticNetworkManager;
import br.com.quantumindustries.mechanical.KineticSnapshot;
import br.com.quantumindustries.mechanical.MechanicalParticipant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Exposes server-simulated QKS measurements without inventing unsimulated quantities. */
public final class MechanicalInspectionProvider implements InspectionProvider {
    private static final double TORQUE_TENTHS_SCALE = 10.0;
    private static final double TOPOLOGY_RADIUS_SQR = 12.0 * 12.0;

    @Override
    public Optional<InspectionSnapshot> inspect(ServerLevel level, BlockPos pos, boolean includeTopology) {
        if (!level.hasChunkAt(pos)) return Optional.empty();
        if (!(level.getBlockEntity(pos) instanceof MechanicalParticipant participant)) return Optional.empty();

        KineticSnapshot snap = KineticNetworkManager.snapshot(level, pos);
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

        boolean hasTorque = participant instanceof PrimitiveWaterWheelBlockEntity
                || participant instanceof RimWaterWheelBlockEntity;
        double hydraulicTorque = participant instanceof PrimitiveWaterWheelBlockEntity wheel
                ? wheel.hydraulicTorque()
                : participant instanceof RimWaterWheelBlockEntity wheel ? wheel.hydraulicTorque() : 0.0;
        int torqueTenths = (int) Math.round(hydraulicTorque * TORQUE_TENTHS_SCALE);

        return Optional.of(new InspectionSnapshot(
                pos.immutable(),
                InspectionSnapshot.Kind.MECHANICAL,
                participant.axle(),
                snap.capacity(),
                snap.load(),
                snap.spinDirection(),
                snap.powered(),
                snap.limited(),
                snap.nodeCount(),
                participant.sourceCapacity(),
                participant.requestedLoad(),
                hasTorque,
                torqueTenths,
                syncedNodes,
                snap.angularVelocity(), snap.angularAcceleration(),
                KineticDynamics.toRpm(snap.angularVelocity()),
                snap.driveTorque(), snap.loadTorque(), snap.netTorque(),
                snap.equivalentInertia(), participant.kineticInertia(),
                Math.abs(snap.driveTorque() * snap.angularVelocity()),
                snap.gearRatio(), snap.stopReason()));
    }
}
