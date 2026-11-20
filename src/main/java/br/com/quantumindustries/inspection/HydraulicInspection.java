package br.com.quantumindustries.inspection;

import br.com.quantumindustries.block.entity.PrimitiveWaterWheelBlockEntity;
import br.com.quantumindustries.block.entity.RimWaterWheelBlockEntity;
import br.com.quantumindustries.mechanical.HydraulicTorque;
import br.com.quantumindustries.mechanical.KineticNetworkManager;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
/** Detailed on-demand telemetry in local QKS drive units, never SI. */
public record HydraulicInspection(Identifier dimension, BlockPos target, long requestId, long serverTick,
        Direction.Axis axle, double omega, double acceleration, List<HydraulicTorque.Contribution> samples) {
    public static final int MAX_SAMPLES = 16;
    public HydraulicInspection {
        target = target.immutable(); samples = List.copyOf(samples);
        if (samples.size() > MAX_SAMPLES || requestId < 0 || !Double.isFinite(omega) || !Double.isFinite(acceleration))
            throw new IllegalArgumentException("Invalid hydraulic telemetry");
        for (var s : samples) {
            if (Math.max(Math.abs(s.rx()), Math.max(Math.abs(s.ry()), Math.abs(s.rz()))) > 2
                    || !Double.isFinite(s.fx()) || !Double.isFinite(s.fy()) || !Double.isFinite(s.fz())
                    || !Double.isFinite(s.torque()) || Math.abs(s.fx()) > 8 || Math.abs(s.fy()) > 8 || Math.abs(s.fz()) > 8
                    || Math.abs(s.torque()) > 1000) throw new IllegalArgumentException("Invalid hydraulic sample");
        }
    }
    public double positive() { return samples.stream().mapToDouble(s -> Math.max(0, s.torque())).sum(); }
    public double negative() { return samples.stream().mapToDouble(s -> Math.min(0, s.torque())).sum(); }
    public double resultant() { return positive() + negative(); }
    public int relation(double torque) { return Math.abs(omega) < 1e-6 || Math.abs(torque) < 1e-9 ? 0 : torque * omega > 0 ? 1 : -1; }
    public static HydraulicInspection capture(ServerLevel level, BlockPos target, long requestId) {
        if (!level.hasChunkAt(target)) return null;
        var be = level.getBlockEntity(target);
        Direction.Axis axle; List<HydraulicTorque.Contribution> samples;
        if (be instanceof PrimitiveWaterWheelBlockEntity wheel) { axle = wheel.axle(); samples = wheel.hydraulicContributions(); }
        else if (be instanceof RimWaterWheelBlockEntity wheel) { axle = wheel.axle(); samples = wheel.hydraulicContributions(); }
        else return null;
        var motion = KineticNetworkManager.snapshot(level, target);
        return new HydraulicInspection(level.dimension().identifier(), target, requestId, level.getGameTime(),
                axle, motion.angularVelocity(), motion.angularAcceleration(), samples);
    }
}
