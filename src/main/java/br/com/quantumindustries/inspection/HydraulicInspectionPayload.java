package br.com.quantumindustries.inspection;

import br.com.quantumindustries.mechanical.HydraulicTorque;
import java.util.ArrayList;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
public record HydraulicInspectionPayload(HydraulicInspection data) implements CustomPacketPayload {
    public static final Type<HydraulicInspectionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("quantumindustries", "hydraulic_inspection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HydraulicInspectionPayload> STREAM_CODEC = StreamCodec.of(
        (b, p) -> {
            var d = p.data();
            b.writeIdentifier(d.dimension()); b.writeBlockPos(d.target()); b.writeVarLong(d.requestId()); b.writeVarLong(d.serverTick());
            b.writeByte(d.axle().ordinal()); b.writeDouble(d.omega()); b.writeDouble(d.acceleration()); b.writeVarInt(d.samples().size());
            for (var s : d.samples()) {
                b.writeByte(s.rx()); b.writeByte(s.ry()); b.writeByte(s.rz());
                b.writeDouble(s.fx()); b.writeDouble(s.fy()); b.writeDouble(s.fz()); b.writeDouble(s.torque());
            }
        }, b -> {
            var dim = b.readIdentifier(); var pos = b.readBlockPos(); long id = b.readVarLong(), tick = b.readVarLong();
            int axis = b.readUnsignedByte();
            if (axis >= Direction.Axis.values().length) throw new IllegalArgumentException("Invalid hydraulic axis");
            double omega = b.readDouble(), alpha = b.readDouble(); int count = b.readVarInt();
            if (count < 0 || count > HydraulicInspection.MAX_SAMPLES) throw new IllegalArgumentException("Invalid hydraulic count");
            var samples = new ArrayList<HydraulicTorque.Contribution>(count);
            for (int i = 0; i < count; i++) samples.add(new HydraulicTorque.Contribution(b.readByte(), b.readByte(), b.readByte(),
                    b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble()));
            return new HydraulicInspectionPayload(new HydraulicInspection(dim, pos, id, tick, Direction.Axis.values()[axis], omega, alpha, samples));
        });
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
