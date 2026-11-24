package br.com.quantumindustries.inspection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import br.com.quantumindustries.mechanical.StopReason;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Server-authored scientific snapshot for a single inspected block. */
public record InspectionSnapshot(
        BlockPos target,
        Kind kind,
        Direction.Axis axle,
        int capacity,
        int load,
        int spinDirection,
        boolean powered,
        boolean limited,
        int nodeCount,
        int localCapacity,
        int localLoad,
        boolean hasTorque,
        int torqueTenths,
        List<BlockPos> networkNodes,
        double angularVelocity,
        double angularAcceleration,
        double rpm,
        double driveTorque,
        double loadTorque,
        double netTorque,
        double equivalentInertia,
        double localInertia,
        double mechanicalPower,
        double effectiveRatio,
        StopReason stopReason) {

    public enum Kind {
        NONE,
        MECHANICAL,
        ENERGY,
        FLUID,
        PNEUMATIC
    }

    public static final int MAX_SYNCED_NODES = 64;

    public InspectionSnapshot(BlockPos target, Kind kind, Direction.Axis axle, int capacity, int load,
            int spinDirection, boolean powered, boolean limited, int nodeCount, int localCapacity,
            int localLoad, boolean hasTorque, int torqueTenths, List<BlockPos> networkNodes) {
        this(target, kind, axle, capacity, load, spinDirection, powered, limited, nodeCount,
                localCapacity, localLoad, hasTorque, torqueTenths, networkNodes,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0,
                powered ? StopReason.NONE : StopReason.NO_SOURCE);
    }

    public InspectionSnapshot {
        target = Objects.requireNonNull(target, "target").immutable();
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(axle, "axle");
        Objects.requireNonNull(stopReason, "stopReason");
        if (!Double.isFinite(angularVelocity) || !Double.isFinite(angularAcceleration)
                || !Double.isFinite(rpm) || !Double.isFinite(driveTorque)
                || !Double.isFinite(loadTorque) || !Double.isFinite(netTorque)
                || !Double.isFinite(equivalentInertia) || !Double.isFinite(localInertia)
                || !Double.isFinite(mechanicalPower) || !Double.isFinite(effectiveRatio)) {
            throw new IllegalArgumentException("Non-finite inspection telemetry");
        }
        networkNodes = networkNodes.stream().map(BlockPos::immutable).toList();
        if (networkNodes.size() > MAX_SYNCED_NODES) {
            throw new IllegalArgumentException("Too many inspection nodes");
        }
        if (kind == Kind.MECHANICAL) {
            spinDirection = spinDirection < 0 ? -1 : 1;
        }
    }

    public static InspectionSnapshot empty(BlockPos target) {
        return new InspectionSnapshot(target.immutable(), Kind.NONE, Direction.Axis.Y,
                0, 0, 1, false, false, 0, 0, 0, false, 0, List.of());
    }

    /** Primary measured quantity (capacity for mechanical, stored energy/fluid or packet count otherwise). */
    public int measuredValue() {
        return switch (kind) {
            case MECHANICAL -> capacity;
            case ENERGY, FLUID, PNEUMATIC -> localCapacity;
            case NONE -> 0;
        };
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, InspectionSnapshot> STREAM_CODEC = StreamCodec.of(
            (buf, snap) -> {
                buf.writeBlockPos(snap.target());
                buf.writeVarInt(snap.kind().ordinal());
                buf.writeVarInt(snap.axle().ordinal());
                buf.writeVarInt(snap.capacity());
                buf.writeVarInt(snap.load());
                buf.writeByte(snap.spinDirection());
                buf.writeBoolean(snap.powered());
                buf.writeBoolean(snap.limited());
                buf.writeVarInt(snap.nodeCount());
                buf.writeVarInt(snap.localCapacity());
                buf.writeVarInt(snap.localLoad());
                buf.writeBoolean(snap.hasTorque());
                buf.writeVarInt(snap.torqueTenths());
                int count = snap.networkNodes().size();
                buf.writeVarInt(count);
                for (int i = 0; i < count; i++) {
                    buf.writeBlockPos(snap.networkNodes().get(i));
                }
                buf.writeDouble(snap.angularVelocity());
                buf.writeDouble(snap.angularAcceleration());
                buf.writeDouble(snap.rpm());
                buf.writeDouble(snap.driveTorque());
                buf.writeDouble(snap.loadTorque());
                buf.writeDouble(snap.netTorque());
                buf.writeDouble(snap.equivalentInertia());
                buf.writeDouble(snap.localInertia());
                buf.writeDouble(snap.mechanicalPower());
                buf.writeDouble(snap.effectiveRatio());
                buf.writeVarInt(snap.stopReason().ordinal());
            },
            buf -> {
                BlockPos target = buf.readBlockPos();
                int kindOrd = buf.readVarInt();
                Kind[] kinds = Kind.values();
                Kind kind = kindOrd >= 0 && kindOrd < kinds.length ? kinds[kindOrd] : Kind.NONE;
                int axisOrd = buf.readVarInt();
                Direction.Axis[] axes = Direction.Axis.values();
                Direction.Axis axle = axisOrd >= 0 && axisOrd < axes.length ? axes[axisOrd] : Direction.Axis.Y;
                int capacity = buf.readVarInt();
                int load = buf.readVarInt();
                int spinDirection = buf.readByte();
                boolean powered = buf.readBoolean();
                boolean limited = buf.readBoolean();
                int nodeCount = buf.readVarInt();
                int localCapacity = buf.readVarInt();
                int localLoad = buf.readVarInt();
                boolean hasTorque = buf.readBoolean();
                int torqueTenths = buf.readVarInt();
                int count = buf.readVarInt();
                if (count < 0 || count > MAX_SYNCED_NODES) {
                    throw new IllegalArgumentException("Invalid inspection node count");
                }
                List<BlockPos> nodes = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    nodes.add(buf.readBlockPos());
                }
                double angularVelocity = buf.readDouble();
                double angularAcceleration = buf.readDouble();
                double rpm = buf.readDouble();
                double driveTorque = buf.readDouble();
                double loadTorque = buf.readDouble();
                double netTorque = buf.readDouble();
                double equivalentInertia = buf.readDouble();
                double localInertia = buf.readDouble();
                double mechanicalPower = buf.readDouble();
                double effectiveRatio = buf.readDouble();
                int reasonOrdinal = buf.readVarInt();
                StopReason[] reasons = StopReason.values();
                StopReason stopReason = reasonOrdinal >= 0 && reasonOrdinal < reasons.length
                        ? reasons[reasonOrdinal] : StopReason.NO_SOURCE;
                return new InspectionSnapshot(target, kind, axle, capacity, load, spinDirection,
                        powered, limited, nodeCount, localCapacity, localLoad, hasTorque, torqueTenths, nodes,
                        angularVelocity, angularAcceleration, rpm, driveTorque, loadTorque, netTorque,
                        equivalentInertia, localInertia, mechanicalPower, effectiveRatio, stopReason);
            });
}
