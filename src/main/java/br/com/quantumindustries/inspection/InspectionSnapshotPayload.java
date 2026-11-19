package br.com.quantumindustries.inspection;

import br.com.quantumindustries.QuantumIndustries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-to-client reply delivering the authoritative inspection snapshot. */
public record InspectionSnapshotPayload(InspectionSnapshot snapshot) implements CustomPacketPayload {
    public static final Type<InspectionSnapshotPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(QuantumIndustries.MODID, "inspection_snapshot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InspectionSnapshotPayload> STREAM_CODEC =
            InspectionSnapshot.STREAM_CODEC.map(InspectionSnapshotPayload::new, InspectionSnapshotPayload::snapshot);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
