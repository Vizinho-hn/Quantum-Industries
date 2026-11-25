package br.com.quantumindustries.thermal;

import java.math.BigInteger;

/** Fixed-step thermal calculations in whole TU and gameplay degrees Celsius. */
public final class ThermalMath {
    private static final long PER_MILLE = 1000;
    private static final int MAX_INSULATION_PERMIL = 900;
    private static final BigInteger MAX_FLOW = BigInteger.valueOf(Long.MAX_VALUE);

    private ThermalMath() {}

    public static long energyForTemperature(int temperatureC, int referenceTempC, int capacity) {
        requireCapacity(capacity);
        return ((long) temperatureC - referenceTempC) * capacity;
    }

    public static double temperatureExact(long energyTu, int referenceTempC, int capacity) {
        requireCapacity(capacity);
        return referenceTempC + (double) energyTu / capacity;
    }

    /** Positive flow goes from the first body to the second. Both energies use a 0 °C reference. */
    public static long conductionFlow(long firstEnergy, int firstCapacity,
                                      long secondEnergy, int secondCapacity, int conductanceMilli) {
        requireCapacity(firstCapacity);
        requireCapacity(secondCapacity);
        if (conductanceMilli < 0) throw new IllegalArgumentException("Negative contact conductance");
        if (conductanceMilli == 0) return 0;

        long sumCapacity = (long) firstCapacity + secondCapacity;
        long productCapacity = (long) firstCapacity * secondCapacity;
        try {
            long gradient = Math.subtractExact(Math.multiplyExact(firstEnergy, secondCapacity),
                    Math.multiplyExact(secondEnergy, firstCapacity));
            return boundedFlow(gradient, sumCapacity, conductanceMilli,
                    Math.multiplyExact(productCapacity, PER_MILLE));
        } catch (ArithmeticException overflow) {
            BigInteger gradient = BigInteger.valueOf(firstEnergy).multiply(BigInteger.valueOf(secondCapacity))
                    .subtract(BigInteger.valueOf(secondEnergy).multiply(BigInteger.valueOf(firstCapacity)));
            return boundedFlow(gradient, sumCapacity, conductanceMilli,
                    BigInteger.valueOf(productCapacity).multiply(BigInteger.valueOf(PER_MILLE)));
        }
    }

    /** Positive flow leaves the body. Active conductance only applies when cooling. */
    public static long ambientFlow(long energyTu, int capacity, int ambientC, int passiveConductanceMilli,
                                   int insulationPermil, int activeCoolingConductanceMilli) {
        validateAmbient(passiveConductanceMilli, insulationPermil);
        if (activeCoolingConductanceMilli < 0) throw new IllegalArgumentException("Negative active conductance");
        long equilibriumEnergy = energyForTemperature(ambientC, 0, capacity);
        long effectiveConductance = passiveConductanceMilli * (PER_MILLE - insulationPermil);
        if (energyTu > equilibriumEnergy) effectiveConductance += activeCoolingConductanceMilli * PER_MILLE;
        if (effectiveConductance == 0) return 0;

        long denominator = capacity * PER_MILLE * PER_MILLE;
        try {
            return boundedFlow(Math.subtractExact(energyTu, equilibriumEnergy), 1,
                    effectiveConductance, denominator);
        } catch (ArithmeticException overflow) {
            BigInteger difference = BigInteger.valueOf(energyTu).subtract(BigInteger.valueOf(equilibriumEnergy));
            return boundedFlow(difference, 1, effectiveConductance, BigInteger.valueOf(denominator));
        }
    }

    static void validateAmbient(int conductance, int insulation) {
        if (conductance < 0) throw new IllegalArgumentException("Negative passive conductance");
        if (insulation < 0 || insulation > MAX_INSULATION_PERMIL) {
            throw new IllegalArgumentException("Insulation must be in [0, 900] permil");
        }
    }

    private static void requireCapacity(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("Heat capacity must be positive");
    }

    private static long boundedFlow(long gradient, long equilibriumDenominator,
                                    long conductance, long flowDenominator) {
        long limit = Math.absExact(gradient) / equilibriumDenominator;
        if (limit == 0) return 0;
        long numerator = Math.multiplyExact(gradient, conductance);
        long rounded = Math.floorDiv(numerator, flowDenominator);
        long remainder = Math.floorMod(numerator, flowDenominator);
        if (remainder >= flowDenominator - remainder) rounded++;
        return Math.clamp(rounded, -limit, limit);
    }

    // Rare fallback for long-range energy saves and large capacities.
    private static long boundedFlow(BigInteger gradient, long equilibriumDenominator,
                                    long conductance, BigInteger flowDenominator) {
        BigInteger limit = gradient.abs().divide(BigInteger.valueOf(equilibriumDenominator)).min(MAX_FLOW);
        if (limit.signum() == 0) return 0;
        BigInteger numerator = gradient.multiply(BigInteger.valueOf(conductance));
        BigInteger[] quotientAndRemainder = numerator.abs().divideAndRemainder(flowDenominator);
        BigInteger roundedMagnitude = quotientAndRemainder[0];
        int halfComparison = quotientAndRemainder[1].shiftLeft(1).compareTo(flowDenominator);
        if (halfComparison > 0 || (halfComparison == 0 && numerator.signum() > 0)) {
            roundedMagnitude = roundedMagnitude.add(BigInteger.ONE);
        }
        long bounded = roundedMagnitude.min(limit).longValueExact();
        return numerator.signum() < 0 ? -bounded : bounded;
    }
}
