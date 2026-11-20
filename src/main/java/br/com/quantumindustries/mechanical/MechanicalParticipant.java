package br.com.quantumindustries.mechanical;

import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import java.util.List;
import java.util.ArrayList;

/** Mechanical ports shared by rotating machinery and electrical generators. */
public interface MechanicalParticipant {
    Direction.Axis axle();

    default boolean connectsMechanical(Direction face) { return face.getAxis() == axle(); }
    default int sourceCapacity() { return 0; }
    default int requestedLoad() { return 0; }
    default int sourceDirection() { return 1; }
    default double kineticInertia() { return 0.8; }
    default double kineticDrag() { return 0.12; }
    /** Torque/friction helpers describe dynamics; callers gate power delivery via StopReason and overload checks. */
    default double staticFrictionTorque() { return 0.08; }
    default double kineticDriveTorque() { return sourceCapacity() * (sourceDirection() < 0 ? -1.0 : 1.0); }
    default double kineticLoadTorque() { return requestedLoad() * 0.75; }
    default double maxAngularSpeed() { return 12.0; }
    default List<KineticLink> kineticLinks(ServerLevel level, BlockPos pos) {
        var links = new ArrayList<KineticLink>();
        for (Direction dir : Direction.values()) {
            if (!connectsMechanical(dir)) continue;
            var next = pos.relative(dir);
            if (level.hasChunkAt(next) && level.getBlockEntity(next) instanceof MechanicalParticipant other
                    && other.connectsMechanical(dir.getOpposite())) links.add(new KineticLink(next, 1, 1));
        }
        return List.copyOf(links);
    }
    default void applyKineticSnapshot(KineticSnapshot snapshot) {}
    default double persistedAngleRadians() { return 0.0; }
    default double persistedAngularVelocity() { return 0.0; }
}
