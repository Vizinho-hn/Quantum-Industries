package br.com.quantumindustries.mechanical;

import java.util.Objects;
import java.util.function.Consumer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class KineticNetworking {
    private static volatile Consumer<KineticSyncPayload> clientSink = payload -> {};
    private KineticNetworking() {}
    public static void setClientSink(Consumer<KineticSyncPayload> sink) { clientSink = Objects.requireNonNull(sink); }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(KineticSyncPayload.TYPE, KineticSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> clientSink.accept(payload)));
    }
}
