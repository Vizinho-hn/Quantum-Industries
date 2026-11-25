package br.com.quantumindustries.energy.qes;

/** DC model with an implicit ideal return. One equivalent watt is one FE per tick. */
public final class ElectricalMath {
    public static final int NOMINAL_VOLTAGE_MV = 120_000;
    static final double MILLI = 1_000.0;
    private static final double MILLI_SQUARED = MILLI * MILLI;
    private static final double MILLI_CUBED = MILLI_SQUARED * MILLI;
    private static final double BALANCE_TOLERANCE = 1e-8;

    private ElectricalMath() {}

    public static double voltageDropMv(double currentMa, double resistanceMOhm) {
        return currentMa * resistanceMOhm / MILLI;
    }

    public static double currentMa(double voltageDropMv, double resistanceMOhm) {
        return voltageDropMv * MILLI / resistanceMOhm;
    }

    public static double powerFePerTick(double voltageMv, double currentMa) {
        return voltageMv * currentMa / MILLI_SQUARED;
    }

    public static double jouleLoss(double currentMa, double resistanceMOhm) {
        return currentMa * currentMa * resistanceMOhm / MILLI_CUBED;
    }

    public static double seriesResistance(double... resistancesMOhm) {
        double result = 0;
        for (double resistance : resistancesMOhm) {
            requireResistance(resistance);
            result += resistance;
        }
        return result;
    }

    public static double parallelResistance(double... resistancesMOhm) {
        double conductance = 0;
        boolean shorted = false;
        for (double resistance : resistancesMOhm) {
            requireResistance(resistance);
            if (resistance == 0) shorted = true;
            else conductance += 1 / resistance;
        }
        return shorted ? 0 : 1 / conductance;
    }

    public static boolean isBalanced(double extracted, double delivered, double loss) {
        return Double.isFinite(extracted) && Double.isFinite(delivered + loss)
                && extracted >= 0 && delivered >= 0 && loss >= 0
                && delivered + loss - extracted <= BALANCE_TOLERANCE * Math.max(1, extracted);
    }

    static double lowerCurrentForLoad(double powerFe, double openVoltageMv, double resistanceMOhm) {
        double discriminant = openVoltageMv * openVoltageMv - 4 * resistanceMOhm * powerFe * MILLI;
        if (discriminant < 0) return Double.POSITIVE_INFINITY;
        return 2 * powerFe * MILLI_SQUARED / (openVoltageMv + Math.sqrt(discriminant));
    }

    private static void requireResistance(double resistance) {
        if (Double.isNaN(resistance) || resistance < 0) throw new IllegalArgumentException("Invalid resistance");
    }
}
