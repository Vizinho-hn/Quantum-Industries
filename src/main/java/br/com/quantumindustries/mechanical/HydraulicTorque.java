package br.com.quantumindustries.mechanical;

import net.minecraft.core.Direction;

/** Pure signed axial torque physics: tau = (r x F) . axis_hat. */
public final class HydraulicTorque {
    public static final double EPSILON = 0.001;

    public record Sample(int rx, int ry, int rz, double fx, double fy, double fz) {}
    public record Result(boolean active, double torque, int direction) {}

    public record RimResult(boolean active, double torque, int direction, int capacity, double wettedFraction) {}

    /** Normalize each wet cell by its lever arm so a single cell cannot grant full wheel output. */
    public static RimResult evaluateRim(Direction.Axis axis, Iterable<Sample> samples, int radius, int capacity) {
        if (radius < 1 || capacity <= 0) throw new IllegalArgumentException("Invalid wheel size/capacity");
        double signed = 0;
        int wetted = 0;
        for (Sample sample : samples) {
            double contribution = contribution(axis, sample.rx(), sample.ry(), sample.rz(), sample.fx(), sample.fy(), sample.fz());
            if (Math.abs(contribution) <= EPSILON) continue;
            wetted++;
            signed += Math.clamp(contribution / radius, -1, 1);
        }
        int rimCells = 8 * radius;
        double fraction = Math.min(wetted, rimCells) / (double) rimCells;
        double torque = capacity * Math.clamp(signed / rimCells, -1, 1);
        boolean active = Math.abs(torque) > EPSILON;
        return new RimResult(active, torque, torque < 0 ? -1 : 1, active ? (int) Math.floor(capacity * fraction) : 0, fraction);
    }

    /** Effective local QKS drive share after existing normalization and saturation. */
    public record Contribution(int rx, int ry, int rz, double fx, double fy, double fz, double torque) {
        public boolean effective() { return Math.abs(torque) > 1e-9; }
    }
    public static java.util.List<Contribution> contributions(Direction.Axis axis, java.util.List<Sample> samples,
            int radius, int capacity) {
        double total = radius == 0 ? evaluate(axis, samples).torque() : evaluateRim(axis, samples, radius, capacity).torque();
        double scale = radius == 0 ? (Math.abs(total) > EPSILON ? Math.min(capacity, 27.0 / Math.abs(total)) : capacity)
                : capacity / (double)(8 * radius);
        double absolute = samples.stream().mapToDouble(s -> Math.abs(contribution(axis, s.rx(), s.ry(), s.rz(), s.fx(), s.fy(), s.fz()))).sum();
        // Preserve opposing shares at equilibrium; distribute only the primitive dead-band residual.
        double residual = radius == 0 && Math.abs(total) <= EPSILON ? total * scale : 0;
        var result = new java.util.ArrayList<Contribution>(samples.size());
        for (Sample sample : samples) {
            double raw = contribution(axis, sample.rx(), sample.ry(), sample.rz(), sample.fx(), sample.fy(), sample.fz());
            double effective = radius == 0 ? raw * scale - (absolute > 0 ? residual * Math.abs(raw) / absolute : 0) : Math.abs(raw) <= EPSILON ? 0 : Math.clamp(raw / radius, -1, 1) * scale;
            result.add(new Contribution(sample.rx(), sample.ry(), sample.rz(), sample.fx(), sample.fy(), sample.fz(), effective));
        }
        return java.util.List.copyOf(result);
    }

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
