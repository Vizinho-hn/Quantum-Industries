package br.com.quantumindustries.energy;

/**
 * Lumped thermal model for the copper/constantan generator. Temperatures are
 * effective contact temperatures, not raw flame/lava temperatures. Heat units
 * and the FE conversion factor are gameplay calibrations, not SI measurements.
 */
public final class ThermoelectricModel {
    public static final int AMBIENT = 20;
    public static final int MAX_HOT = 250;
    public static final int MAX_FE = 6;
    public static final int POWER_SCALE = 12000;
    private ThermoelectricModel() {}

    public enum Coolant {
        NONE(AMBIENT, 1, AMBIENT, 0, 0),
        SNOW(0, 60, 0, 6000, 0),
        ICE(0, 100, 0, 12000, 0),
        PACKED_ICE(-15, 100, 0, 18000, 0),
        BLUE_ICE(-30, 100, 0, 24000, 0),
        WATER(0, 200, 100, 24000, 4000),
        FLOWING_WATER(AMBIENT, 1, AMBIENT, 0, 0);

        private final int start, capacity, phaseTemperature, latentHeat, initialHeat;
        Coolant(int start, int capacity, int phaseTemperature, int latentHeat, int initialHeat) {
            this.start = start;
            this.capacity = capacity;
            this.phaseTemperature = phaseTemperature;
            this.latentHeat = latentHeat;
            this.initialHeat = initialHeat;
        }
        public boolean finite() { return this != NONE && this != FLOWING_WATER; }
        public int initialHeat() { return initialHeat; }
        public int minHeat() {
            if (this == WATER) return -12000; // latent heat released during freezing
            return finite() ? (-60 - start) * capacity : 0;
        }
        public boolean frozen(int heat) { return this == WATER && heat <= minHeat(); }
        public int maxHeat() { return (phaseTemperature - start) * capacity + latentHeat; }
        public int temperature(int heat) {
            int sensible = Math.clamp(heat, minHeat(), maxHeat());
            if (this == WATER) sensible = Math.max(0, sensible);
            return Math.min(phaseTemperature, start + Math.floorDiv(sensible, capacity));
        }
        public boolean exhausted(int heat) { return finite() && heat >= maxHeat(); }
        public static Coolant fromId(int id) {
            var values = values();
            return id >= 0 && id < values.length ? values[id] : NONE;
        }
    }

    public static int gradient(int hot, int cold) { return Math.max(0, hot - cold); }

    /** Matched-load Seebeck power is proportional to delta T squared.
     * Numerator is in 1/POWER_SCALE FE/t, allowing fractional production. */
    public static int powerNumerator(int hot, int cold, Coolant coolant) {
        if (coolant == Coolant.NONE || hot <= AMBIENT || hot > MAX_HOT) return 0;
        int delta = gradient(hot, cold);
        // Bound electrical power by an ideal heat-engine efficiency ceiling.
        // Temperatures in the efficiency expression must be absolute.
        int heat = heatFlow(hot, cold, coolant);
        int carnotBound = heat * delta * POWER_SCALE / (hot + 273);
        return Math.min(MAX_FE * POWER_SCALE, Math.min(delta * delta, carnotBound));
    }

    /** Conductive heat transfer into the cold side; also occurs at open circuit. */
    public static int heatFlow(int hot, int cold, Coolant coolant) {
        if (coolant == Coolant.NONE || hot <= AMBIENT) return 0;
        return (gradient(hot, cold) + 9) / 10;
    }
}
