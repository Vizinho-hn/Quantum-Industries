package br.com.quantumindustries.energy.qes;

/** Relative resistivity at equal geometry and gameplay operating limits. */
public enum ElectricalMaterial {
    COPPER(1.0, 800, 1_000, true),
    SILVER(0.95, 750, 1_100, true),
    GOLD(1.45, 750, 750, true),
    ELECTRUM(1.8, 700, 650, true),
    CONSTANTAN(29.0, 650, 450, false),
    PORCELAIN(Double.POSITIVE_INFINITY, 600, 0, false),
    EBONITE(Double.POSITIVE_INFINITY, 80, 0, false),
    LEATHER(Double.POSITIVE_INFINITY, 120, 0, false);

    private final double relativeResistivity;
    private final int thermalLimitC;
    private final int currentLimitMa;
    private final boolean distributionConductor;

    ElectricalMaterial(double relativeResistivity, int thermalLimitC, int currentLimitMa,
                       boolean distributionConductor) {
        this.relativeResistivity = relativeResistivity;
        this.thermalLimitC = thermalLimitC;
        this.currentLimitMa = currentLimitMa;
        this.distributionConductor = distributionConductor;
    }

    public double relativeResistivity() { return relativeResistivity; }
    public double relativeConductance() { return 1 / relativeResistivity; }
    public int thermalLimitC() { return thermalLimitC; }
    public int currentLimitMa() { return currentLimitMa; }
    public boolean distributionConductor() { return distributionConductor; }
}
