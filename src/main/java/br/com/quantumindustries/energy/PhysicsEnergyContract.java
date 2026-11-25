package br.com.quantumindustries.energy;

import java.util.Objects;

/** Shared accounting for conversions. UM remain QKS gameplay units, never SI N·m. */
public final class PhysicsEnergyContract {
    public static final int TU_PER_FE = 1;
    public static final int MOTOR_COST = 24;
    public static final int MOTOR_OUTPUT_UM = 18;
    private static final double GENERATOR_PEAK_RPM = 50;

    public enum EnergyDomain { KINETIC_QKS, ELECTRICAL_QES, THERMAL_QTS }

    /**
     * Amounts use the conversion's accounting scale, with FE and TU at parity.
     * Raw QKS torque cannot be placed on this ledger without a conversion-specific normalization.
     * Pure Joule conversion accounts for all incoming FE as useful TU or rejected heat.
     */
    public record ConversionReceipt(EnergyDomain source, EnergyDomain target, int inputAmount,
                                    int usefulOutput, int thermalLoss, int rejectedHeat) {
        public ConversionReceipt {
            Objects.requireNonNull(source);
            Objects.requireNonNull(target);
            validateConservation(inputAmount, usefulOutput, (long) thermalLoss + rejectedHeat);
            if (thermalLoss < 0 || rejectedHeat < 0) throw new IllegalArgumentException("Negative heat");
        }
    }

    private PhysicsEnergyContract() {}

    /** Rejects negative quantities and creation of energy. An unspent remainder is permitted. */
    public static void validateConservation(int input, int useful, int losses) {
        validateConservation(input, useful, (long) losses);
    }

    private static void validateConservation(int input, int useful, long losses) {
        if (input < 0 || useful < 0 || losses < 0 || useful + losses > input) {
            throw new IllegalArgumentException("Energy conversion is out of balance");
        }
    }

    public static ConversionReceipt jouleHeating(int inputFe) {
        return new ConversionReceipt(EnergyDomain.ELECTRICAL_QES, EnergyDomain.THERMAL_QTS,
                inputFe, inputFe * TU_PER_FE, 0, 0);
    }

    public static boolean isClosedLoopNonAmplifying() {
        // GeneratorModel's net curve peaks on its 50–60 RPM plateau.
        return isClosedLoopNonAmplifying(MOTOR_COST, MOTOR_OUTPUT_UM, MOTOR_OUTPUT_UM,
                GeneratorModel.netRate(GENERATOR_PEAK_RPM) / GeneratorModel.SCALE);
    }

    /** Checks the mechanical handoff and compares the two FE amounts over one paid tick. */
    public static boolean isClosedLoopNonAmplifying(int motorInputFe, int motorOutputUm,
                                                     int generatorInputUm, int generatorMaxOutputFe) {
        return motorInputFe > 0 && motorOutputUm > 0 && generatorInputUm > 0
                && generatorInputUm <= motorOutputUm
                && generatorMaxOutputFe >= 0 && generatorMaxOutputFe <= motorInputFe;
    }
}
