package br.com.quantumindustries.thermal;

/** Gameplay material coefficients per unit of contact area or material quantity. */
public enum ThermalMaterial {
    COPPER(240, 4, 0, -60, 850),
    ALUMINUM(200, 9, 0, -60, 550),
    IRON_CAST(85, 5, 0, -60, 1050),
    STEEL(65, 5, 0, -60, 1200),
    CONSTANTAN(35, 4, 0, -60, 750),
    REFRACTORY_CERAMIC(8, 8, 750, -60, 1500),
    LEAD(55, 2, 0, -60, 275),
    EBONITE(2, 14, 900, -40, 80);

    private final int relativeConductivity;
    private final int specificHeatCapacity;
    private final int insulationFactor;
    private final int minTempC;
    private final int maxTempC;

    ThermalMaterial(int relativeConductivity, int specificHeatCapacity, int insulationFactor,
                    int minTempC, int maxTempC) {
        this.relativeConductivity = relativeConductivity;
        this.specificHeatCapacity = specificHeatCapacity;
        this.insulationFactor = insulationFactor;
        this.minTempC = minTempC;
        this.maxTempC = maxTempC;
    }

    public int relativeConductivity() { return relativeConductivity; }
    public int specificHeatCapacity() { return specificHeatCapacity; }
    public int insulationFactor() { return insulationFactor; }
    public int minTempC() { return minTempC; }
    public int maxTempC() { return maxTempC; }
}
