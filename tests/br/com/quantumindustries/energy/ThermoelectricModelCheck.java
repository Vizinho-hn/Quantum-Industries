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
        check(ThermalExchange.exchange(-30,25,ThermalExchange.COPPER)<0,"warm air heats ice");
        check(ThermalExchange.exchange(0,-40,ThermalExchange.COPPER)>0,"cold air removes heat");
        check(ThermalExchange.exchange(20,20,ThermalExchange.COPPER)==0,"equilibrium has no exchange");
        check(ThermalExchange.efficiencyBasisPoints(3,20)==1500,"gross energy ratio");
        check(ThermalExchange.efficiencyBasisPoints(0,0)==0,"no heat no efficiency");
        check(Coolant.BLUE_ICE.temperature(-1000)==-40,"subzero cooling extends sensible heat");
        check(Coolant.WATER.temperature(-6000)==0&&!Coolant.WATER.frozen(-6000),"freezing plateau");
        check(Coolant.WATER.frozen(Coolant.WATER.minHeat()),"water freezes after releasing latent heat");
        check(ThermalExchange.ambient(-0.7f,false)<0&&ThermalExchange.ambient(2f,false)>20,"biome temperatures differ");
        int frozenHeat=0;
        for(int t=0;t<20000;t++){
            int temperature=Coolant.BLUE_ICE.temperature(frozenHeat);
            int incoming=ThermoelectricModel.heatFlow(180,temperature,Coolant.BLUE_ICE);
            int outgoing=ThermalExchange.exchange(temperature,-40,4*ThermalExchange.COPPER);
            frozenHeat=Math.clamp(frozenHeat+incoming-outgoing,Coolant.BLUE_ICE.minHeat(),Coolant.BLUE_ICE.maxHeat());
        }
        check(Coolant.BLUE_ICE.temperature(frozenHeat)<0&&!Coolant.BLUE_ICE.exhausted(frozenHeat),"cold biome can preserve blue ice even without electrical extraction");
        ThermalHistory history=new ThermalHistory();
        for(int second=0;second<65;second++){
            for(int tick=0;tick<20;tick++)history.tick(180,-10,20,3,1);
        }
        check(history.count()==60&&history.cursor()==5,"history bounded to sixty samples");
        check(history.latest(0)==180&&history.latest(1)==-10,"temperature history");
        check(history.latest(2)==1500&&history.latest(3)==200,"efficiency and fan net power");
        System.out.println("Thermoelectric model checks passed");
    }
}
