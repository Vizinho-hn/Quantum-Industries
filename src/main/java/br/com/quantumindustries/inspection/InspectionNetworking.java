package br.com.quantumindustries.inspection;

import br.com.quantumindustries.ModItems;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Registers on-demand inspection networking without loading client classes on dedicated servers. */
public final class InspectionNetworking {
    private static final double MAX_INSPECT_DISTANCE_SQR = 10.0 * 10.0;
    private static volatile Consumer<InspectionSnapshot> clientSink = snapshot -> {};

    private static volatile Consumer<HydraulicInspection> hydraulicSink = data -> {};
    public static void setHydraulicSink(Consumer<HydraulicInspection> sink) { hydraulicSink = sink; }
    private InspectionNetworking() {}

    public static void setClientSink(Consumer<InspectionSnapshot> sink) {
        clientSink = sink != null ? sink : snapshot -> {};
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2");
        registrar.playToServer(RequestHydraulicPayload.TYPE, RequestHydraulicPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        var data = HydraulicInspectionService.request(player, payload);
                        if (data != null) context.reply(new HydraulicInspectionPayload(data));
                    }
                }));
        registrar.playToClient(HydraulicInspectionPayload.TYPE, HydraulicInspectionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> hydraulicSink.accept(payload.data())));
        registrar.playToServer(RequestInspectionPayload.TYPE, RequestInspectionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer player)) return;
                    if (!player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.ENERGY_INSPECTION_GOGGLES.get())) return;
                    if (!(player.level() instanceof ServerLevel serverLevel)) return;
                    var pos = payload.pos();
                    if (!serverLevel.hasChunkAt(pos)) return;
                    if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > MAX_INSPECT_DISTANCE_SQR) {
                        return;
                    }
                    InspectionSnapshot snapshot = InspectionRegistry.inspect(serverLevel, pos, payload.includeTopology());
                    context.reply(new InspectionSnapshotPayload(snapshot));
                }));
        registrar.playToClient(InspectionSnapshotPayload.TYPE, InspectionSnapshotPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> clientSink.accept(payload.snapshot())));
    }
}
