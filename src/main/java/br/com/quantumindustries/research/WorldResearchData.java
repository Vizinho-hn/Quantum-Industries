package br.com.quantumindustries.research;

import br.com.quantumindustries.QuantumIndustries;
import br.com.quantumindustries.recipe.ModRecipeBook;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private final Map<String, ResearchProgress> completed = new HashMap<>();
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
        boolean[] needsRewrite = {false};
        progress.forEach((uuid, known) -> {
            ResearchProgress decoded = ResearchProgress.decode(known);
            if (!decoded.encode().equals(known)) needsRewrite[0] = true;
            completed.put(uuid, decoded);
        });
        legacy.forEach((uuid, bits) -> {
            ResearchProgress decoded = completed.computeIfAbsent(uuid, ignored -> new ResearchProgress());
            if (bits != 0 && !progress.containsKey(uuid)) needsRewrite[0] = true;
            for (String key : LegacyResearch.decode(bits)) {
                if (decoded.addLegacy(key)) needsRewrite[0] = true;
            }
        });
        if (needsRewrite[0]) setDirty();
    }

    private Map<String, List<String>> encodeProgress() {
        Map<String, List<String>> result = new HashMap<>();
        completed.forEach((uuid, known) -> result.put(uuid, known.encode()));
        return result;
    }

    private ResearchProgress progress(Player player) {
        String uuid = player.getUUID().toString();
        ResearchProgress known = completed.get(uuid);
        if (known != null) return known;
        if (conductivity && player.level() instanceof ServerLevel level) {
            var owner = level.getServer().getSingleplayerProfile();
            if (owner != null && owner.id().equals(player.getUUID())) {
                known = new ResearchProgress();
                known.addLegacy("conductivity");
                if (copperWire) known.addLegacy("copper_wire");
                completed.put(uuid, known);
                setDirty();
                return known;
            }
        }
        return new ResearchProgress();
    }

    public boolean knows(Player player, String key) {
        return knows(player, ResearchIds.parse(key));
    }

    public boolean knows(Player player, Identifier id) { return progress(player).knows(id); }

    boolean knows(String playerUuid, Identifier id) {
        ResearchProgress known = completed.get(playerUuid);
        return known != null && known.knows(id);
    }

    public boolean knows(Player player, int slot) {
        return ResearchCatalog.valid(slot) && knows(player, ResearchCatalog.STUDIES.get(slot).key());
    }

    public boolean prerequisites(Player player, int slot) {
        return ResearchCatalog.valid(slot) && prerequisites(player, ResearchCatalog.STUDIES.get(slot));
    }

    private boolean prerequisites(Player player, ResearchCatalog.Study study) {
        ResearchProgress known = progress(player);
        return study.prerequisites().stream()
                .map(ResearchIds::parse)
                .allMatch(known::knows);
    }

    public boolean learn(Player player, int slot) {
        return ResearchCatalog.valid(slot) && learn(player, ResearchCatalog.STUDIES.get(slot).key());
    }

    public boolean learn(Player player, String key) {
        return learn(player, ResearchCatalog.identifier(key));
    }

    public boolean learn(Player player, Identifier id) {
        if (!canLearn(player, id)) return false;
        if (!completed.computeIfAbsent(player.getUUID().toString(), ignored -> new ResearchProgress()).learn(id)) {
            return false;
        }
        setDirty();
        ModRecipeBook.sync(player);
        return true;
    }

    public void unlockAll(Player player) {
        ResearchProgress known = progress(player);
        ResearchCatalog.STUDIES.forEach(study -> known.learn(ResearchCatalog.identifier(study.key())));
        completed.put(player.getUUID().toString(), known);
        setDirty();
        ModRecipeBook.sync(player);
    }

    public void clearAll(Player player) {
        completed.put(player.getUUID().toString(), new ResearchProgress());
        setDirty();
        ModRecipeBook.sync(player);
    }

    public boolean canLearn(Player player, Identifier id) {
        var study = ResearchCatalog.find(id);
        return study != null && !knows(player, id) && prerequisites(player, study)
                && evidenceSatisfied(player, study);
    }

    public boolean evidenceSatisfied(Player player, ResearchCatalog.Study study) {
        return study.evidence().stream().allMatch(evidence -> evidence.satisfied(player));
    }

    public static WorldResearchData get(ServerLevel level) {
        return level.getServer().getDataStorage().computeIfAbsent(TYPE);
    }
}
