package br.com.quantumindustries.energy;

/** Shared, dimensionless gameplay thermal units; one unit is one FE-equivalent.
 * Positive exchange removes heat from the reservoir; negative exchange heats it.
 * Material values describe complete module geometry, not bulk conductivity. */
public final class ThermalExchange {
    public static final int COPPER = 240, ALUMINUM = 200, FAN_PASSIVE = 80, FAN_ACTIVE = 440;
    private ThermalExchange() {}
    public static int ambient(float biomeTemperature, boolean nether) {
        return nether ? 80 : Math.clamp(Math.round(40 * biomeTemperature - 12), -60, 65);
    }
    public static int exchange(int contact, int ambient, int milliConductance) {
        return Math.round((contact - ambient) * Math.max(0, milliConductance) / 1000f);
    }
    public static int efficiencyBasisPoints(long electrical, long heat) {
        return heat <= 0 ? 0 : (int)Math.clamp(electrical * 10000L / heat, 0L, 10000L);
    }
}
