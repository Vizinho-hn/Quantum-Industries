package br.com.quantumindustries.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record KineticSyncPayload(BlockPos pos, long serverGameTime, double angleRadians,
        double angularVelocity, double angularAcceleration, double gearRatio, boolean powered,
        boolean jammed, byte stopReasonOrdinal) implements CustomPacketPayload {
    public KineticSyncPayload {
        java.util.Objects.requireNonNull(pos, "pos");
        if (!Double.isFinite(angleRadians) || !Double.isFinite(angularVelocity) || !Double.isFinite(angularAcceleration)
                || !Double.isFinite(gearRatio) || gearRatio == 0 || stopReasonOrdinal < 0
                || stopReasonOrdinal >= StopReason.values().length) throw new IllegalArgumentException("Invalid kinetic snapshot");
        pos = pos.immutable();
    }
    public static final Type<KineticSyncPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("quantumindustries", "kinetic_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, KineticSyncPayload> STREAM_CODEC = StreamCodec.of(
            (b, p) -> { b.writeBlockPos(p.pos()); b.writeVarLong(p.serverGameTime());
                b.writeDouble(p.angleRadians()); b.writeDouble(p.angularVelocity()); b.writeDouble(p.angularAcceleration());
                b.writeDouble(p.gearRatio()); b.writeBoolean(p.powered()); b.writeBoolean(p.jammed()); b.writeByte(p.stopReasonOrdinal()); },
            b -> new KineticSyncPayload(b.readBlockPos(), b.readVarLong(), b.readDouble(), b.readDouble(),
                b.readDouble(), b.readDouble(), b.readBoolean(), b.readBoolean(), b.readByte()));
    public static KineticSyncPayload from(BlockPos pos, KineticSnapshot s) {
        return new KineticSyncPayload(pos, s.serverGameTime(), s.angleRadians(), s.angularVelocity(),
                s.angularAcceleration(), s.gearRatio(), s.powered(), s.jammed(), (byte) s.stopReason().ordinal());
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
