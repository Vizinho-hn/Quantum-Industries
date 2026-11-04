package br.com.quantumindustries.research;

import br.com.quantumindustries.QuantumIndustries;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
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
                    Codec.BOOL.optionalFieldOf("copper_wire", false).forGetter(WorldResearchData::hasCopperWire)
            ).apply(instance, WorldResearchData::new)),
            null
    );

    private boolean conductivity;
    private boolean copperWire;

    public WorldResearchData() { this(false, false); }

    private WorldResearchData(boolean conductivity, boolean copperWire) {
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

    public static WorldResearchData get(ServerLevel level) {
        return level.getServer().getDataStorage().computeIfAbsent(TYPE);
    }
}
