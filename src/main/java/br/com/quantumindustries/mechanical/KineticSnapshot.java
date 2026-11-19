package br.com.quantumindustries.mechanical;

public record KineticSnapshot(long networkId, int topologyVersion, long serverGameTime,
        double angleRadians, double angularVelocity, double angularAcceleration, double driveTorque,
        double loadTorque, double netTorque, double equivalentInertia, double gearRatio,
        int capacity, int load, int nodeCount, int spinDirection, boolean powered,
        boolean limited, boolean jammed, StopReason stopReason) {
    public static final KineticSnapshot EMPTY = new KineticSnapshot(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1,
            0, 0, 0, 1, false, false, false, StopReason.NO_SOURCE);
    public KineticSnapshot {
        spinDirection = Math.abs(angularVelocity) > 1e-6 ? (angularVelocity < 0 ? -1 : 1) : (spinDirection < 0 ? -1 : 1);
    }
    public double rpm() { return KineticDynamics.toRpm(angularVelocity); }
}
