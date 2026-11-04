package br.com.quantumindustries.research;

import br.com.quantumindustries.QuantumIndustries;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** One research journal per world, shared across all dimensions and players. */
public final class WorldResearchData extends SavedData {
    public static final SavedDataType<WorldResearchData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(QuantumIndustries.MODID, "research"),
            WorldResearchData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.BOOL.fieldOf("conductivity").forGetter(WorldResearchData::hasConductivity)
            ).apply(instance, WorldResearchData::new))
    );
    private boolean conductivity;
    public WorldResearchData() { this(false); }
    private WorldResearchData(boolean conductivity) { this.conductivity = conductivity; }
    public boolean hasConductivity() { return conductivity; }
    public void unlockConductivity() {
        if (!conductivity) {
            conductivity = true;
            setDirty();
        }
    }
    public static WorldResearchData get(ServerLevel level) {
        return level.getServer().getDataStorage().computeIfAbsent(TYPE);
    }
}
