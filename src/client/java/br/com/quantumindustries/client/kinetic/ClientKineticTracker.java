package br.com.quantumindustries.client.kinetic;

import br.com.quantumindustries.block.entity.ManualDynamoBlockEntity;
import br.com.quantumindustries.mechanical.KineticNetworking;
import br.com.quantumindustries.mechanical.KineticSyncPayload;
import br.com.quantumindustries.mechanical.RotationalBlockEntity;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Client-thread packet delivery, including the brief chunk/entity deserialization gap. */
@EventBusSubscriber(modid = "quantumindustries", value = Dist.CLIENT)
public final class ClientKineticTracker {
    private static final int MAX_PENDING = 1024;
    private static final int MAX_AGE_TICKS = 100;
    private static final Map<BlockPos, KineticSyncPayload> PENDING = new LinkedHashMap<>();
    private static ClientLevel trackedLevel;

    static { KineticNetworking.setClientSink(ClientKineticTracker::accept); }

    private ClientKineticTracker() {}

    /** Forces sink installation during renderer registration, before joining a world. */
    public static void init() {}

    public static void accept(KineticSyncPayload payload) {
        ClientLevel level = currentLevel();
        if (level == null || stale(level, payload)) return;
        if (applyToEntity(level, payload)) {
            PENDING.remove(payload.pos());
            return;
        }
        if (!PENDING.containsKey(payload.pos()) && PENDING.size() >= MAX_PENDING) {
            var oldest = PENDING.keySet().iterator();
            oldest.next();
            oldest.remove();
        }
        PENDING.put(payload.pos(), payload);
    }

    public static void applyPending(BlockEntity entity) {
        ClientLevel level = currentLevel();
        if (level == null || entity.getLevel() != level) return;
        KineticSyncPayload payload = PENDING.remove(entity.getBlockPos());
        if (payload != null && !stale(level, payload)) apply(entity, payload);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = currentLevel();
        if (level == null) return;
        var entries = PENDING.values().iterator();
        while (entries.hasNext()) {
            KineticSyncPayload payload = entries.next();
            if (stale(level, payload) || applyToEntity(level, payload)) entries.remove();
        }
    }

    private static ClientLevel currentLevel() {
        ClientLevel level = Minecraft.getInstance().level;
        // Identity also catches reconnecting to the same dimension with a new world instance.
        if (level != trackedLevel || level == null) {
            PENDING.clear();
            trackedLevel = level;
        }
        return level;
    }

    private static boolean stale(ClientLevel level, KineticSyncPayload payload) {
        return level.getGameTime() - payload.serverGameTime() > MAX_AGE_TICKS;
    }

    private static boolean applyToEntity(ClientLevel level, KineticSyncPayload payload) {
        if (!level.hasChunkAt(payload.pos())) return false;
        BlockEntity entity = level.getBlockEntity(payload.pos());
        if (entity == null) return false;
        apply(entity, payload);
        return true;
    }

    private static void apply(BlockEntity entity, KineticSyncPayload payload) {
        // Keep the authoritative phase/time pair: both entity types extrapolate at render time.
        if (entity instanceof RotationalBlockEntity rotor) rotor.applyClientSync(payload);
        else if (entity instanceof ManualDynamoBlockEntity dynamo) dynamo.applyClientSync(payload);
    }
}
