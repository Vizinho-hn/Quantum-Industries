package br.com.quantumindustries.inspection;

import java.util.ArrayList;
import java.util.List;
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
        List<BlockPos> networkNodes) {

    public enum Kind {
        NONE,
        MECHANICAL,
        ENERGY,
        FLUID,
        PNEUMATIC
    }

    public static final int MAX_SYNCED_NODES = 64;

    public InspectionSnapshot {
        networkNodes = List.copyOf(networkNodes);
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
                if (snap.kind() == Kind.NONE) return;
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
                if (snap.hasTorque()) buf.writeVarInt(snap.torqueTenths());
                int count = Math.min(snap.networkNodes().size(), MAX_SYNCED_NODES);
                buf.writeVarInt(count);
                for (int i = 0; i < count; i++) {
                    buf.writeBlockPos(snap.networkNodes().get(i));
                }
            },
            buf -> {
                BlockPos target = buf.readBlockPos();
                int kindOrd = buf.readVarInt();
                Kind[] kinds = Kind.values();
                Kind kind = kindOrd >= 0 && kindOrd < kinds.length ? kinds[kindOrd] : Kind.NONE;
                if (kind == Kind.NONE) return empty(target);
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
                int torqueTenths = hasTorque ? buf.readVarInt() : 0;
                int count = Math.clamp(buf.readVarInt(), 0, MAX_SYNCED_NODES);
                List<BlockPos> nodes = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    nodes.add(buf.readBlockPos());
                }
                return new InspectionSnapshot(target, kind, axle, capacity, load, spinDirection,
                        powered, limited, nodeCount, localCapacity, localLoad, hasTorque, torqueTenths, nodes);
            });
}
