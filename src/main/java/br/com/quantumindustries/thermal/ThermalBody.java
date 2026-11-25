package br.com.quantumindustries.thermal;

import java.util.ArrayList;
import java.util.List;

import br.com.quantumindustries.thermal.ThermalSnapshot.ThermalFlowEdge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Server-owned lumped thermal reservoir with a 0 °C energy reference. */
public final class ThermalBody {
    private static final String ENERGY_KEY = "energy_tu";
    private static final long NO_TICK = Long.MIN_VALUE;

    private long thermalEnergyTu;
    private final int heatCapacityTuPerC;
    private final int passiveConductanceMilli;
    private final int insulationPermil;
    private final int minOperatingTempC;
    private final int maxOperatingTempC;

    private long lastHeatingRateTu;
    private long lastCoolingRateTu;
    private long tickStartEnergyTu;
    private int lastAmbientC;
    private long telemetryTick = NO_TICK;
    private long neighborStepTick = NO_TICK;
    private final List<ThermalFlowEdge> edges = new ArrayList<>(6);

    public ThermalBody(int initialTempC, int heatCapacityTuPerC, int passiveConductanceMilli,
                       int insulationPermil, int minOperatingTempC, int maxOperatingTempC) {
        ThermalMath.validateAmbient(passiveConductanceMilli, insulationPermil);
        if (minOperatingTempC > maxOperatingTempC) throw new IllegalArgumentException("Inverted operating range");
        this.thermalEnergyTu = ThermalMath.energyForTemperature(initialTempC, 0, heatCapacityTuPerC);
        this.tickStartEnergyTu = thermalEnergyTu;
        this.heatCapacityTuPerC = heatCapacityTuPerC;
        this.passiveConductanceMilli = passiveConductanceMilli;
        this.insulationPermil = insulationPermil;
        this.minOperatingTempC = minOperatingTempC;
        this.maxOperatingTempC = maxOperatingTempC;
        this.lastAmbientC = initialTempC;
    }

    public int temperatureC() {
        long whole = Math.floorDiv(thermalEnergyTu, heatCapacityTuPerC);
        int remainder = Math.floorMod(thermalEnergyTu, heatCapacityTuPerC);
        if (remainder >= heatCapacityTuPerC - remainder) whole++;
        return Math.clamp(whole, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public double temperatureExact() { return ThermalMath.temperatureExact(thermalEnergyTu, 0, heatCapacityTuPerC); }

    public long thermalEnergyTu() { return thermalEnergyTu; }
    public int heatCapacityTuPerC() { return heatCapacityTuPerC; }
    public int passiveConductanceMilli() { return passiveConductanceMilli; }
    public int insulationPermil() { return insulationPermil; }
    public int minOperatingTempC() { return minOperatingTempC; }
    public int maxOperatingTempC() { return maxOperatingTempC; }
    public long lastHeatingRateTu() { return lastHeatingRateTu; }
    public long lastCoolingRateTu() { return lastCoolingRateTu; }
    public long lastNetFlowTu() {
        long difference = thermalEnergyTu - tickStartEnergyTu;
        if (((thermalEnergyTu ^ tickStartEnergyTu) & (thermalEnergyTu ^ difference)) < 0) {
            return thermalEnergyTu < 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
        return difference;
    }
    public int lastAmbientC() { return lastAmbientC; }

    /** Call before mutations each server tick. A neighbor may already have opened this tick. */
    public void beginTick(long gameTime) {
        if (telemetryTick == gameTime) return;
        telemetryTick = gameTime;
        lastHeatingRateTu = lastCoolingRateTu = 0;
        tickStartEnergyTu = thermalEnergyTu;
        edges.clear();
    }

    boolean beginNeighborStep(long gameTime) {
        if (neighborStepTick == gameTime) return false;
        neighborStepTick = gameTime;
        beginTick(gameTime);
        return true;
    }

    public void addHeat(long tu) {
        requireNonnegativeHeat(tu);
        applyEnergyDelta(tu);
    }

    /** Extracts available positive TU, leaving the body at or above its 0 °C reference. */
    public long consumeHeat(long tu) {
        requireNonnegativeHeat(tu);
        long consumed = Math.min(tu, Math.max(0, thermalEnergyTu));
        applyEnergyDelta(-consumed);
        return consumed;
    }

    /** Returns signed heat lost to the ambient reservoir during this step. */
    public long stepAmbient(int ambientC, int activeCoolingConductanceMilli) {
        long flow = ThermalMath.ambientFlow(thermalEnergyTu, heatCapacityTuPerC, ambientC,
                passiveConductanceMilli, insulationPermil, activeCoolingConductanceMilli);
        applyEnergyDelta(-flow);
        lastAmbientC = ambientC;
        return flow;
    }

    /** Returns signed TU sent to the other body, debiting and crediting the same amount. */
    public long transferTo(ThermalBody other, int conductanceMilli) {
        long requested = ThermalMath.conductionFlow(thermalEnergyTu, heatCapacityTuPerC,
                other.thermalEnergyTu, other.heatCapacityTuPerC, conductanceMilli);
        if (requested == 0) return 0;
        long flow = requested > 0
                ? limitToStorage(requested, thermalEnergyTu, other.thermalEnergyTu)
                : -limitToStorage(-requested, other.thermalEnergyTu, thermalEnergyTu);
        applyEnergyDelta(-flow);
        other.applyEnergyDelta(flow);
        return flow;
    }

    private static long limitToStorage(long magnitude, long source, long destination) {
        long available = source < 0 ? source - Long.MIN_VALUE : Long.MAX_VALUE;
        long room = destination > 0 ? Long.MAX_VALUE - destination : Long.MAX_VALUE;
        return Math.min(magnitude, Math.min(available, room));
    }

    private void applyEnergyDelta(long delta) {
        thermalEnergyTu = Math.addExact(thermalEnergyTu, delta);
        if (delta > 0) lastHeatingRateTu = Math.min(Long.MAX_VALUE - delta, lastHeatingRateTu) + delta;
        if (delta < 0) lastCoolingRateTu = Math.min(Long.MAX_VALUE + delta, lastCoolingRateTu) - delta;
    }

    private static void requireNonnegativeHeat(long tu) {
        if (tu < 0) throw new IllegalArgumentException("Heat amount must be nonnegative");
    }

    void recordEdge(Direction face, BlockPos neighbor, long incomingFlow) {
        int displayFlow = Math.clamp(incomingFlow, Integer.MIN_VALUE, Integer.MAX_VALUE);
        edges.add(new ThermalFlowEdge(face, neighbor, displayFlow));
    }

    public ThermalSnapshot snapshot(int targetTemperatureC) {
        return new ThermalSnapshot(temperatureC(), lastAmbientC, thermalEnergyTu, heatCapacityTuPerC,
                lastHeatingRateTu, lastCoolingRateTu, lastNetFlowTu(), targetTemperatureC, maxOperatingTempC, edges);
    }

    public void save(ValueOutput out, String prefix) {
        out.putLong(prefix + ENERGY_KEY, thermalEnergyTu);
    }

    public void load(ValueInput in, String prefix, int defaultTempC) {
        thermalEnergyTu = in.getLongOr(prefix + ENERGY_KEY,
                ThermalMath.energyForTemperature(defaultTempC, 0, heatCapacityTuPerC));
        telemetryTick = neighborStepTick = NO_TICK;
        lastHeatingRateTu = lastCoolingRateTu = 0;
        tickStartEnergyTu = thermalEnergyTu;
        lastAmbientC = defaultTempC;
        edges.clear();
    }
}
