package br.com.quantumindustries.gas;

import net.minecraft.resources.Identifier;

/** Gas identity is separate from NeoForge liquid resources. */
public enum IndustrialGas {
    STEAM("steam", 100);

    public final Identifier id;
    public final int temperatureC;

    IndustrialGas(String path, int temperatureC) {
        this.id = Identifier.fromNamespaceAndPath("quantumindustries", path);
        this.temperatureC = temperatureC;
    }

    /** Gameplay pressure, calibrated to 400 kPa at 100 C and a full segment. */
    public int pressureKpa(int amount, int capacity) {
        return (int) Math.round((amount / (double) capacity) * 400.0
                * (temperatureC + 273.15) / 373.15);
    }

    public static IndustrialGas byId(String id) {
        for (var gas : values()) if (gas.id.toString().equals(id)) return gas;
        return null;
    }
}
