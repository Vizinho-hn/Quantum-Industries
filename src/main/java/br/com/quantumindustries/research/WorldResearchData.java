package br.com.quantumindustries.research;

import br.com.quantumindustries.QuantumIndustries;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class WorldResearchData extends SavedData {
    static final Codec<WorldResearchData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("conductivity", false).forGetter(data -> data.conductivity),
            Codec.BOOL.optionalFieldOf("copper_wire", false).forGetter(data -> data.copperWire),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("players", Map.of())
                    .forGetter(data -> data.legacyPlayers),
            Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()).optionalFieldOf("completed_research", Map.of())
                    .forGetter(WorldResearchData::encodeProgress)
    ).apply(instance, WorldResearchData::new));

    public static final SavedDataType<WorldResearchData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(QuantumIndustries.MODID, "research"),
            WorldResearchData::new, CODEC, null);

    private final Map<String, Set<String>> completed = new HashMap<>();
    private final Map<String, Integer> legacyPlayers;
    private final boolean conductivity;
    private final boolean copperWire;

    public WorldResearchData() { this(false, false, Map.of(), Map.of()); }

    private WorldResearchData(boolean conductivity, boolean copperWire, Map<String, Integer> legacy,
                              Map<String, List<String>> progress) {
        this.conductivity = conductivity;
        this.copperWire = copperWire;
        // Retain the original bits, including any unrecognized ones, for compatibility.
        this.legacyPlayers = Map.copyOf(legacy);
        progress.forEach((id, known) -> completed.put(id, new HashSet<>(known)));
        legacy.forEach((id, bits) -> completed.computeIfAbsent(id, ignored -> LegacyResearch.decode(bits)));
        if (!progress.keySet().containsAll(legacy.keySet())) setDirty();
    }

    private Map<String, List<String>> encodeProgress() {
        Map<String, List<String>> result = new HashMap<>();
        completed.forEach((id, known) -> {
            List<String> sorted = new ArrayList<>(known);
            sorted.sort(String::compareTo);
            result.put(id, sorted);
        });
        return result;
    }

    private Set<String> progress(Player player) {
        String uuid = player.getUUID().toString();
        Set<String> known = completed.get(uuid);
        if (known != null) return known;
        if (conductivity && player.level() instanceof ServerLevel level) {
            var owner = level.getServer().getSingleplayerProfile();
            if (owner != null && owner.id().equals(player.getUUID())) {
                known = new HashSet<>();
                known.add("conductivity");
                if (copperWire) known.add("copper_wire");
                completed.put(uuid, known);
                setDirty();
                return known;
            }
        }
        return Set.of();
    }

    public boolean knows(Player player, String key) { return progress(player).contains(key); }

    public boolean knows(Player player, int slot) {
        return ResearchCatalog.valid(slot) && knows(player, ResearchCatalog.STUDIES.get(slot).key());
    }

    public boolean prerequisites(Player player, int slot) {
        return ResearchCatalog.valid(slot) && prerequisites(player, ResearchCatalog.STUDIES.get(slot));
    }

    private boolean prerequisites(Player player, ResearchCatalog.Study study) {
        Set<String> known = progress(player);
        return known.containsAll(study.prerequisites());
    }

    public boolean learn(Player player, int slot) {
        return ResearchCatalog.valid(slot) && learn(player, ResearchCatalog.STUDIES.get(slot).key());
    }

    public boolean learn(Player player, String key) {
        var study = ResearchCatalog.find(key);
        if (study == null || knows(player, key) || !prerequisites(player, study)) return false;
        completed.computeIfAbsent(player.getUUID().toString(), ignored -> new HashSet<>()).add(key);
        setDirty();
        return true;
    }

    public static WorldResearchData get(ServerLevel level) {
        return level.getServer().getDataStorage().computeIfAbsent(TYPE);
    }
}
