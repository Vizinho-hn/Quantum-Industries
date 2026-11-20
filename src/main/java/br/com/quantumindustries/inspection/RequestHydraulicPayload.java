package br.com.quantumindustries.inspection;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
public record RequestHydraulicPayload(Identifier dimension, BlockPos target, long requestId) implements CustomPacketPayload {
    public static final Type<RequestHydraulicPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("quantumindustries", "request_hydraulic"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestHydraulicPayload> STREAM_CODEC = StreamCodec.of(
        (b, p) -> { b.writeIdentifier(p.dimension()); b.writeBlockPos(p.target()); b.writeVarLong(p.requestId()); },
        b -> new RequestHydraulicPayload(b.readIdentifier(), b.readBlockPos(), b.readVarLong()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
