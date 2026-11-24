package br.com.quantumindustries.inspection;

import br.com.quantumindustries.QuantumIndustries;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client-to-server request for a single inspected block's real server state. */
public record RequestInspectionPayload(BlockPos pos, boolean includeTopology) implements CustomPacketPayload {
    public static final Type<RequestInspectionPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(QuantumIndustries.MODID, "request_inspection"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestInspectionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.pos());
                buf.writeBoolean(payload.includeTopology());
            },
            buf -> new RequestInspectionPayload(buf.readBlockPos(), buf.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
