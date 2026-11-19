package br.com.quantumindustries.client.kinetic;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

/** Load-once meshes; reload and lookups share a lock so stale loads cannot repopulate the cache. */
@EventBusSubscriber(modid = "quantumindustries", value = Dist.CLIENT)
public final class QksModelCache {
    public static final String MODEL_DIRECTORY = "geckolib/models/block/";
    public static final String MODEL_SUFFIX = ".geo.json";
    public static final String CLASSPATH_PREFIX = "/assets/quantumindustries/" + MODEL_DIRECTORY;
    private static final String MOD_ID = "quantumindustries";
    private static final Map<String, QksCompiledModel> MODELS = new HashMap<>();
    private static ResourceManager resourceManager;

    private QksModelCache() {}

    public static synchronized QksCompiledModel get(String name) {
        String key = modelName(name);
        return MODELS.computeIfAbsent(key, QksModelCache::load);
    }

    /** Resource packs are authoritative once a manager is installed; classpath is for standalone use. */
    public static synchronized void reload(ResourceManager manager) {
        resourceManager = java.util.Objects.requireNonNull(manager, "manager");
        MODELS.clear();
    }

    public static synchronized void clear() { MODELS.clear(); }

    @SubscribeEvent
    public static void registerReloadListener(AddClientReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(MOD_ID, "qks_models"),
                (ResourceManagerReloadListener) QksModelCache::reload);
    }

    private static QksCompiledModel load(String name) {
        try (InputStream stream = resourceManager == null
                ? QksModelCache.class.getResourceAsStream(CLASSPATH_PREFIX + name + MODEL_SUFFIX)
                : resourceManager.open(Identifier.fromNamespaceAndPath(MOD_ID, MODEL_DIRECTORY + name + MODEL_SUFFIX))) {
            if (stream == null) throw new IOException("Missing QKS model " + name);
            return QksGeoModelLoader.load(stream);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to load QKS model " + name, exception);
        }
    }

    private static String modelName(String path) {
        java.util.Objects.requireNonNull(path, "path");
        String name = path;
        if (name.startsWith(MODEL_DIRECTORY)) name = name.substring(MODEL_DIRECTORY.length());
        if (name.startsWith("block/")) name = name.substring("block/".length());
        if (name.endsWith(MODEL_SUFFIX)) name = name.substring(0, name.length() - MODEL_SUFFIX.length());
        if (!name.matches("[a-z0-9_]+")) throw new IllegalArgumentException("Invalid QKS model name " + path);
        return name;
    }
}
