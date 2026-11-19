package br.com.quantumindustries.mechanical;

/**
 * Pure, deterministic angular dynamics for the future Quantum Kinetic System.
 * Inputs are consistently scaled gameplay torque/inertia units; they are not SI units.
 * The class is independent of Minecraft, world state, and any animation library.
 */
public final class KineticDynamics {
    private static final double EPSILON = 1e-9;
    private static final double FULL_TURN = 2.0 * Math.PI;

    private KineticDynamics() {}

    public record State(double angleRadians, double angularVelocityRadiansPerSecond) {
        public State {
            requireFinite("angleRadians", angleRadians);
            requireFinite("angularVelocityRadiansPerSecond", angularVelocityRadiansPerSecond);
        }
        public static State atRest() { return new State(0.0, 0.0); }
    }

    /**
     * Advances a single constrained rotating component by one fixed simulation step.
     *
     * @param drive signed drive torque: positive/negative around the component's +axis
     * @param load non-negative resisting torque (acts against motion, not a power source)
     * @param inertia positive effective rotational inertia, including coupled loads
     * @param drag non-negative viscous drag coefficient
     * @param maxSpeed positive maximum absolute angular speed (rad/s)
     * @param seconds positive simulation step (normally 0.05 for 20 TPS)
     */
    public static State advance(State previous, double drive, double load,
            double inertia, double drag, double maxSpeed, double seconds) {
        if (previous == null) throw new IllegalArgumentException("previous state is null");
        requireFinite("drive", drive);
        requireNonNegative("load", load);
        requirePositive("inertia", inertia);
        requireNonNegative("drag", drag);
        requirePositive("maxSpeed", maxSpeed);
        requirePositive("seconds", seconds);

        double oldSpeed = previous.angularVelocityRadiansPerSecond();
        double opposingSign = Math.abs(oldSpeed) > EPSILON ? Math.signum(oldSpeed) : Math.signum(drive);
        double resultantTorque = drive - opposingSign * load - drag * oldSpeed;

        // Static load: a weak drive must not start the mechanism moving.
        if (Math.abs(oldSpeed) <= EPSILON && Math.abs(drive) <= load) {
            resultantTorque = 0.0;
        }

        double nextSpeed = Math.clamp(oldSpeed + resultantTorque * seconds / inertia, -maxSpeed, maxSpeed);
        // Load and friction must not reverse a shaft when there is no drive strong enough to reverse it.
        if (oldSpeed * nextSpeed < 0.0 && Math.abs(drive) <= load) nextSpeed = 0.0;
        if (Math.abs(nextSpeed) <= EPSILON) nextSpeed = 0.0;

        // Trapezoidal integration reduces angular-phase error during acceleration/braking.
        double nextAngle = wrap(previous.angleRadians() + 0.5 * (oldSpeed + nextSpeed) * seconds);
        return new State(nextAngle, nextSpeed);
    }

    public record StepResult(State state, double angularAcceleration, double netTorque,
            double mechanicalPower, double kineticEnergy, boolean stalled) {}

    public static StepResult advanceDetailed(State previous, double drive, double load,
            double inertia, double drag, double maxSpeed, double seconds) {
        return advanceDetailed(previous, drive, load, inertia, drag, maxSpeed, seconds, 0.0);
    }

    public static StepResult advanceDetailed(State previous, double drive, double load,
            double inertia, double drag, double maxSpeed, double seconds, double staticFriction) {
        requireNonNegative("staticFriction", staticFriction);
        // Run the common integrator first so every input is validated, even when statically stalled.
        State next = advance(previous, drive, load, inertia, drag, maxSpeed, seconds);
        if (Math.abs(previous.angularVelocityRadiansPerSecond()) <= EPSILON
                && Math.abs(drive) <= load + staticFriction) next = new State(wrap(previous.angleRadians()), 0);
        double omega = next.angularVelocityRadiansPerSecond();
        double alpha = (omega - previous.angularVelocityRadiansPerSecond()) / seconds;
        double torque = alpha * inertia;
        return new StepResult(next, alpha, torque, torque * omega, .5 * inertia * omega * omega, omega == 0);
    }

    public static double reflectInertia(double localInertia, double ratio) {
        requirePositive("localInertia", localInertia); requireRatio(ratio);
        return localInertia * ratio * ratio;
    }
    public static double reflectDrag(double localDrag, double ratio) {
        requireNonNegative("localDrag", localDrag); requireRatio(ratio);
        return localDrag * ratio * ratio;
    }
    public static double reflectDriveTorque(double localDriveTorque, double ratio, double efficiency) {
        requireFinite("localDriveTorque", localDriveTorque); requireRatio(ratio); requireEfficiency(efficiency);
        return localDriveTorque * ratio * efficiency;
    }
    public static double reflectLoadTorque(double localLoadTorque, double ratio, double efficiency) {
        requireNonNegative("localLoadTorque", localLoadTorque); requireRatio(ratio); requireEfficiency(efficiency);
        return localLoadTorque * Math.abs(ratio) / Math.max(1e-6, efficiency);
    }
    public static double mergeAngularVelocity(double totalAngularMomentum, double totalReflectedInertia) {
        requireFinite("totalAngularMomentum", totalAngularMomentum);
        requirePositive("totalReflectedInertia", totalReflectedInertia);
        return totalAngularMomentum / totalReflectedInertia;
    }
    private static void requireRatio(double ratio) {
        requireFinite("ratio", ratio);
        if (ratio == 0) throw new IllegalArgumentException("ratio must be nonzero");
    }
    private static void requireEfficiency(double efficiency) {
        requirePositive("efficiency", efficiency);
        if (efficiency > 1) throw new IllegalArgumentException("efficiency exceeds one");
    }

    public static double toRpm(double radiansPerSecond) {
        requireFinite("radiansPerSecond", radiansPerSecond);
        return radiansPerSecond * 30.0 / Math.PI;
    }

    public static double wrap(double radians) {
        requireFinite("radians", radians);
        double wrapped = radians % FULL_TURN;
        return wrapped < 0.0 ? wrapped + FULL_TURN : wrapped;
    }

    private static void requireFinite(String name, double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
    private static void requireNonNegative(String name, double value) {
        requireFinite(name, value);
        if (value < 0.0) throw new IllegalArgumentException(name + " must be non-negative");
    }
    private static void requirePositive(String name, double value) {
        requireFinite(name, value);
        if (value <= 0.0) throw new IllegalArgumentException(name + " must be positive");
    }
}
