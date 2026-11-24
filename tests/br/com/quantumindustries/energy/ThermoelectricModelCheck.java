package br.com.quantumindustries.energy;

import br.com.quantumindustries.energy.ThermoelectricModel.Coolant;

public final class ThermoelectricModelCheck {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        check(ThermoelectricModel.powerNumerator(180, -15, Coolant.PACKED_ICE)
                > ThermoelectricModel.powerNumerator(180, 20, Coolant.WATER), "colder coolant increases output");
        check(ThermoelectricModel.powerNumerator(20, -30, Coolant.BLUE_ICE) == 0, "requires hot source");
        check(ThermoelectricModel.powerNumerator(180, 20, Coolant.NONE) == 0, "requires coolant");
        check(ThermoelectricModel.powerNumerator(140, 140, Coolant.WATER) == 0, "zero gradient");
        check(ThermoelectricModel.powerNumerator(300, -30, Coolant.BLUE_ICE) == 0, "overheat cutoff");
        check(ThermoelectricModel.heatFlow(300, -30, Coolant.BLUE_ICE) > 0, "overheat still transfers heat");
        check(Coolant.WATER.temperature(Coolant.WATER.initialHeat()) == 20, "fresh water starts at 20 C");
        check(Coolant.WATER.temperature(20000) == 100 && !Coolant.WATER.exhausted(20000), "boiling needs latent heat");
        check(Coolant.WATER.exhausted(Coolant.WATER.maxHeat()), "evaporation after latent heat");
        check(Coolant.BLUE_ICE.temperature(3000) == 0 && !Coolant.BLUE_ICE.exhausted(3000), "melting plateau");
        check(Coolant.BLUE_ICE.exhausted(Coolant.BLUE_ICE.maxHeat()), "finite ice melts");
        check(!Coolant.FLOWING_WATER.finite(), "flowing water is renewed coolant");
        for (int hot = 21; hot <= 300; hot++) {
            for (int cold = -30; cold <= 100; cold++) {
                int p = ThermoelectricModel.powerNumerator(hot, cold, Coolant.WATER);
                int q = ThermoelectricModel.heatFlow(hot, cold, Coolant.WATER);
                check(p >= 0 && p <= ThermoelectricModel.MAX_FE * ThermoelectricModel.POWER_SCALE, "power cap");
                check(p <= q * ThermoelectricModel.POWER_SCALE, "electrical energy cannot exceed incoming heat");
            }
        }
        int heat = 0, produced = 0, remainder = 0, ticks = 0;
        while (!Coolant.PACKED_ICE.exhausted(heat) && ticks++ < 10000) {
            int cold = Coolant.PACKED_ICE.temperature(heat);
            int p = ThermoelectricModel.powerNumerator(180, cold, Coolant.PACKED_ICE) + remainder;
            int fe = p / ThermoelectricModel.POWER_SCALE;
            remainder = p % ThermoelectricModel.POWER_SCALE;
            produced += fe;
            heat += Math.max(0, ThermoelectricModel.heatFlow(180, cold, Coolant.PACKED_ICE) - fe);
        }
        check(produced > 0 && Coolant.PACKED_ICE.exhausted(heat), "generation consumes finite ice");
        System.out.println("Thermoelectric model checks passed");
    }
}
