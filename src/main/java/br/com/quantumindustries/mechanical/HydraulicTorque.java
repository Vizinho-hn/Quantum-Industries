package br.com.quantumindustries.mechanical;

import net.minecraft.core.Direction;

/** Pure signed axial torque physics: tau = (r x F) . axis_hat. */
public final class HydraulicTorque {
    public static final double EPSILON = 0.001;

    public record Sample(int rx, int ry, int rz, double fx, double fy, double fz) {}
    public record Result(boolean active, double torque, int direction) {}

    private HydraulicTorque() {}

    /** Signed scalar torque contribution around the positive world axis (+X, +Y, or +Z). */
    public static double contribution(Direction.Axis axis, int rx, int ry, int rz, double fx, double fy, double fz) {
        return switch (axis) {
            case X -> ry * fz - rz * fy;
            case Y -> rz * fx - rx * fz;
            case Z -> rx * fy - ry * fx;
        };
    }

    public static Result evaluate(Direction.Axis axis, Iterable<Sample> samples) {
        double total = 0.0;
        for (Sample s : samples) {
            total += contribution(axis, s.rx(), s.ry(), s.rz(), s.fx(), s.fy(), s.fz());
        }
        boolean active = Math.abs(total) > EPSILON;
        int direction = total < -EPSILON ? -1 : 1;
        return new Result(active, total, direction);
    }
}
