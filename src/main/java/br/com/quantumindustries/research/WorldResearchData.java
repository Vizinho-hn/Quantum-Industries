package br.com.quantumindustries.research;

import br.com.quantumindustries.QuantumIndustries;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Small world-shared research profile. Legacy field "conductivity" and SavedData ID
 * are deliberately kept so pre-F0 worlds load without losing prior discoveries.
 */
public final class WorldResearchData extends SavedData {
    public static final SavedDataType<WorldResearchData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(QuantumIndustries.MODID, "research"),
            WorldResearchData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.BOOL.fieldOf("conductivity").forGetter(WorldResearchData::hasConductivity),
                    Codec.BOOL.optionalFieldOf("copper_wire", false).forGetter(WorldResearchData::hasCopperWire),
                    Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("players", Map.of())
                            .forGetter(data -> data.players)
            ).apply(instance, WorldResearchData::new)),
            null
    );

    private final Map<String, Integer> players;
    private boolean conductivity;
    private boolean copperWire;

    public WorldResearchData() { this(false, false, Map.of()); }

    private WorldResearchData(boolean conductivity, boolean copperWire, Map<String, Integer> players) {
        this.players = new HashMap<>(players);
        this.conductivity = conductivity;
        this.copperWire = copperWire;
    }

    public boolean hasConductivity() { return conductivity; }

    public boolean hasCopperWire() { return copperWire && conductivity; }

    public boolean unlockConductivity() {
        if (conductivity) return false;
        conductivity = true;
        setDirty();
        return true;
    }

    public boolean unlockCopperWire() {
        if (!conductivity || copperWire) return false;
        copperWire = true;
        setDirty();
        return true;
    }

    private int progress(Player player) {
        Integer saved = players.get(player.getUUID().toString());
        if (saved != null) return saved;
        if (player.level() instanceof ServerLevel level) {
            var owner = level.getServer().getSingleplayerProfile();
            if (owner != null && owner.id().equals(player.getUUID())) {
                return hasCopperWire() ? 3 : hasConductivity() ? 1 : 0;
            }
        }
        return 0;
    }

    public boolean knows(Player player, int id) {
        return id >= 0 && id < 2 && (progress(player) & (1 << id)) != 0;
    }

    public boolean learn(Player player, int id) {
        if (id < 0 || id > 1 || knows(player, id) || (id == 1 && !knows(player, 0))) return false;
        players.put(player.getUUID().toString(), progress(player) | (1 << id));
        setDirty();
        return true;
    }

    public static WorldResearchData get(ServerLevel level) {
        return level.getServer().getDataStorage().computeIfAbsent(TYPE);
    }
}

