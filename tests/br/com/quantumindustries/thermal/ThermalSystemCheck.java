package br.com.quantumindustries.thermal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

public final class ThermalSystemCheck {
    private static final int ROOM = 20;
    private static final int PASSIVE = 200;
    private static final int ACTIVE = 800;
    private static final int STRONG_CONTACT = 100_000;

    private ThermalSystemCheck() {}

    public static void main(String[] args) throws IOException {
        heatsFromSource();
        coolsAfterShutdownWithInertia();
        differentCapacitiesRespondDifferently();
        heatFlowsFromHotToCold();
        equilibriumNeverOvershoots();
        insulationReducesExchange();
        passiveDissipationAndAmbientHeating();
        activeDissipationIncreasesCooling();
        restoresEnergyFromSerializedNbt();
        conservesEnergyExactly();
        rejectsInvalidParameters();
        snapshotsPreserveBothSidesOfATick();
        System.out.println("QTS: 10 thermal requirements passed");
        benchmark(100);
        benchmark(1000);
    }

    private static ThermalBody body(int temperature, int capacity, int insulation) {
        return new ThermalBody(temperature, capacity, PASSIVE, insulation, -60, 900);
    }

    private static void heatsFromSource() {
        var body = body(ROOM, 10, 0);
        body.beginTick(1);
        body.addHeat(125);
        check(body.temperatureExact() == 32.5 && body.lastHeatingRateTu() == 125,
                "source heat must raise temperature and appear in tick telemetry");
        body.beginTick(1);
        check(body.lastNetFlowTu() == 125, "reentering the same tick must preserve telemetry");
    }

    private static void coolsAfterShutdownWithInertia() {
        var body = body(100, 10, 0);
        body.beginTick(1);
        body.stepAmbient(ROOM, 0);
        check(body.temperatureExact() == 98.4 && body.lastCoolingRateTu() == 16,
                "after shutdown, passive cooling must retain thermal inertia");
        body.beginTick(2);
        check(body.lastNetFlowTu() == 0, "starting a new tick must clear rates");
    }

    private static void differentCapacitiesRespondDifferently() {
        var light = body(ROOM, 4, 0);
        var heavy = body(ROOM, 16, 0);
        light.addHeat(80);
        heavy.addHeat(80);
        check(light.temperatureExact() == 40 && heavy.temperatureExact() == 25,
                "equal source heat must move the lighter body further");
        check(ThermalMath.energyForTemperature(25, ROOM, 16) == 80
                        && ThermalMath.temperatureExact(80, ROOM, 16) == 25,
                "energy and temperature must round-trip against a nonzero reference");
    }

    private static void heatFlowsFromHotToCold() {
        var cold = body(20, 10, 0);
        var hot = body(100, 10, 0);
        check(cold.transferTo(hot, 250) == -20
                        && cold.thermalEnergyTu() == 220 && hot.thermalEnergyTu() == 980,
                "negative flow must move heat from the second body to the first");
        check(cold.lastHeatingRateTu() == 20 && hot.lastCoolingRateTu() == 20,
                "both ends must report the applied pair flow");
    }

    private static void equilibriumNeverOvershoots() {
        var hot = body(100, 5, 0);
        var cold = body(0, 15, 0);
        check(hot.transferTo(cold, STRONG_CONTACT) == 375, "pair flow must stop at equilibrium");
        check(hot.temperatureExact() == 25 && cold.temperatureExact() == 25,
                "unequal capacities must meet at their weighted equilibrium");
        check(hot.transferTo(cold, STRONG_CONTACT) == 0, "equilibrium must remain stable");

        var smallHot = body(1, 2, 0);
        var smallCold = body(0, 3, 0);
        check(smallHot.transferTo(smallCold, STRONG_CONTACT) == 1
                        && smallHot.transferTo(smallCold, STRONG_CONTACT) == 0,
                "fractional equilibrium must floor to whole TU and never reverse the gradient");
        check(ThermalMath.conductionFlow(Long.MAX_VALUE, 1, Long.MAX_VALUE - 2, 1, STRONG_CONTACT) == 1,
                "large reservoirs must preserve small gradients exactly");
        check(ThermalMath.conductionFlow(Long.MAX_VALUE, 2, Long.MAX_VALUE - 2, 2, STRONG_CONTACT) == 1,
                "overflowing cross products must preserve their exact equilibrium bound");
        check(ThermalMath.conductionFlow(2, 4, 0, 4, 1000) == 1
                        && ThermalMath.conductionFlow(0, 4, 2, 4, 1000) == 0,
                "signed half-TU requests must round toward positive infinity");
    }

    private static void insulationReducesExchange() {
        var insulated = body(100, 10, 750);
        insulated.stepAmbient(ROOM, 0);
        check(insulated.lastCoolingRateTu() == 4, "750 permil insulation must leave 25% of passive flow");
    }

    private static void passiveDissipationAndAmbientHeating() {
        var hot = body(100, 10, 0);
        var cold = body(-10, 10, 0);
        hot.stepAmbient(ROOM, 0);
        cold.stepAmbient(ROOM, 0);
        check(hot.lastCoolingRateTu() == 16 && cold.lastHeatingRateTu() == 6,
                "passive exchange must lose heat above ambient and gain heat below ambient");
    }

    private static void activeDissipationIncreasesCooling() {
        var body = body(100, 10, 0);
        body.stepAmbient(ROOM, ACTIVE);
        check(body.lastCoolingRateTu() == 80 && body.lastAmbientC() == ROOM,
                "active conductance must add to passive dissipation");
        body.stepAmbient(ROOM, STRONG_CONTACT);
        check(body.temperatureExact() == ROOM, "strong cooling must stop at ambient");

        var cold = new ThermalBody(-10, 10, STRONG_CONTACT, 0, -60, 900);
        cold.stepAmbient(ROOM, ACTIVE);
        check(cold.temperatureExact() == ROOM, "strong passive warming must stop at ambient");
    }

    private static void restoresEnergyFromSerializedNbt() throws IOException {
        var original = body(-10, 10, 0);
        original.addHeat(3);
        var out = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        original.save(out, "qts_");
        var bytes = new ByteArrayOutputStream();
        try (var data = new DataOutputStream(bytes)) {
            NbtIo.write(out.buildResult(), data);
        }
        CompoundTag saved;
        try (var data = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            saved = NbtIo.read(data);
        }
        var restored = body(90, 10, 0);
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING, RegistryAccess.EMPTY, saved),
                "qts_", ROOM);
        check(restored.thermalEnergyTu() == -97 && restored.temperatureExact() == -9.7
                        && restored.lastNetFlowTu() == 0,
                "NBT round-trip must preserve signed subdegree energy and reset tick rates");
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING, RegistryAccess.EMPTY, saved),
                "other_", ROOM);
        check(restored.thermalEnergyTu() == 200, "missing prefix must use the caller's migration temperature");
    }

    private static void conservesEnergyExactly() {
        var hot = body(102, 7, 0);
        var cold = body(-15, 23, 0);
        long sum = hot.thermalEnergyTu() + cold.thermalEnergyTu();
        for (int tick = 0; tick < 200; tick++) {
            hot.beginTick(tick);
            cold.beginTick(tick);
            long flow = hot.transferTo(cold, 270);
            check(hot.lastNetFlowTu() == -flow && cold.lastNetFlowTu() == flow
                            && hot.thermalEnergyTu() + cold.thermalEnergyTu() == sum,
                    "isolated pair must debit and credit exactly the same whole TU");
        }
        var reservoir = body(3, 10, 0);
        check(reservoir.consumeHeat(60) == 30 && reservoir.thermalEnergyTu() == 0,
                "consumption must report only the available positive TU");
        var extreme = body(0, 2, 0);
        extreme.addHeat(Long.MAX_VALUE);
        var neighbor = body(0, 1, 0);
        neighbor.addHeat(Long.MAX_VALUE - 1);
        check(neighbor.transferTo(extreme, STRONG_CONTACT) == 0,
                "pair flow cannot overflow a destination's energy storage");
    }

    private static void rejectsInvalidParameters() {
        try {
            body(ROOM, 0, 0);
            throw new AssertionError("zero capacity must fail at construction");
        } catch (IllegalArgumentException expected) {
            // Invalid configuration must fail before it can enter the simulation.
        }
    }

    private static void snapshotsPreserveBothSidesOfATick() {
        var body = body(ROOM, 10, 0);
        check(body.beginNeighborStep(1), "the owner must process canonical pairs once");
        var neighbor = new BlockPos.MutableBlockPos(2, 3, 4);
        body.recordEdge(Direction.EAST, neighbor, 5);
        neighbor.set(8, 9, 10);
        check(!body.beginNeighborStep(1), "a repeated tick must not process the same pairs twice");
        var snapshot = body.snapshot(100);
        body.beginTick(2);
        check(body.snapshot(100).edges().isEmpty() && snapshot.edges().size() == 1
                        && snapshot.edges().getFirst().neighbor().equals(new BlockPos(2, 3, 4)),
                "snapshots must retain an immutable copy of edge telemetry");
    }

    private static void benchmark(int count) {
        var hot = new ThermalBody[count];
        var cold = new ThermalBody[count];
        for (int index = 0; index < count; index++) {
            hot[index] = body(100, 10, 0);
            cold[index] = body(20, 25, 0);
        }
        long transferred = 0;
        long start = System.nanoTime();
        for (int index = 0; index < count; index++) {
            transferred += hot[index].transferTo(cold[index], 270);
        }
        long elapsed = System.nanoTime() - start;
        check(transferred == 22L * count, "benchmark must exercise real nonzero pair exchanges");
        System.out.printf(Locale.ROOT, "%d pairs: %.3f ms (%.3f µs/pair)%n",
                count, elapsed / 1_000_000.0, elapsed / 1_000.0 / count);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
