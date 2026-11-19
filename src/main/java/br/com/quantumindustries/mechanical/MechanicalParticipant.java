package br.com.quantumindustries.mechanical;

import net.minecraft.core.Direction;

/** Mechanical ports shared by rotating machinery and electrical generators. */
public interface MechanicalParticipant {
    Direction.Axis axle();

    default boolean connectsMechanical(Direction face) { return face.getAxis() == axle(); }
    default int sourceCapacity() { return 0; }
    default int requestedLoad() { return 0; }
    default int sourceDirection() { return 1; }
}
