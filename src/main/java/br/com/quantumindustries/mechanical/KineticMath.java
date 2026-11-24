package br.com.quantumindustries.mechanical;

import net.minecraft.core.Direction;

/** Deterministic world-time phase math shared by kinetic renderers and unit tests. */
public final class KineticMath {
    public static final float DEFAULT_DEGREES_PER_TICK = 9.0f;

    private KineticMath() {}

    public static float normalizeDegrees(double degrees) {
        double wrapped = degrees % 360.0;
        return (float) (wrapped < 0.0 ? wrapped + 360.0 : wrapped);
    }

    /** Deterministic angle derived from world time and spin direction (no per-BE drift). */
    public static float deterministicAngle(double worldTime, float stoppedAngle,
            boolean rotating, int spinDirection, float degreesPerTick) {
        if (!rotating) return normalizeDegrees(stoppedAngle);
        int dir = spinDirection < 0 ? -1 : 1;
        return normalizeDegrees(worldTime * degreesPerTick * dir);
    }

    /** Maps block facing to local rotor-Z sign so world-space rotation matches right-hand rule around +axis. */
    public static float localRotorSign(Direction facing) {
        return switch (facing) {
            case NORTH, WEST, DOWN -> 1.0f;
            case SOUTH, EAST, UP -> -1.0f;
        };
    }
}
