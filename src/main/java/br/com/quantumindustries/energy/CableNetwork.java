package br.com.quantumindustries.energy;

import br.com.quantumindustries.block.CopperCableBlock;
import br.com.quantumindustries.block.entity.CopperCableBlockEntity;
import br.com.quantumindustries.block.entity.EnergyInspectable;
import br.com.quantumindustries.energy.qes.ElectricalMath;
import br.com.quantumindustries.energy.qes.ElectricalNetworkState.NetworkStatus;
import br.com.quantumindustries.energy.qes.ElectricalSolver;
import br.com.quantumindustries.logistics.ConfigurableConduit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;

/** Loaded-only FE bridge. A cable is a passive conductor with no energy buffer. */
public final class CableNetwork {
    private static final int MAX_NODES = 128;
    private static final int BARE_RESISTANCE_MOHM = 3_500;
    private static final int INSULATED_RESISTANCE_MOHM = 200;

    private record Segment(CopperCableBlockEntity cable, int resistanceMOhm, List<BlockPos> outgoing) {}
    private record Topology(List<BlockPos> inputs, Map<BlockPos, Segment> segments,
                            Map<BlockPos, List<BlockPos>> destinations) {}

    private CableNetwork() {}

    public static void transfer(ServerLevel level, BlockPos source, IntSupplier stored,
                                IntUnaryOperator extract, int senderLimit) {
        transfer(level, source, stored, extract, senderLimit, target -> true);
    }

    public static void transfer(ServerLevel level, BlockPos source, IntSupplier stored,
                                IntUnaryOperator extract, int senderLimit,
                                Predicate<EnergyInspectable> acceptsTarget) {
        int remaining = Math.min(senderLimit, stored.getAsInt());
        if (remaining <= 0) return;
        Topology topology = discover(level, source, acceptsTarget);
        if (topology == null || topology.destinations().isEmpty()) return;
        List<BlockPos> destinations = List.copyOf(topology.destinations().keySet());
        long time = level.getGameTime();
        int index = Math.floorMod(time, destinations.size());
        for (int i = 0; i < destinations.size() && remaining > 0; i++) {
            BlockPos destination = destinations.get((index + i) % destinations.size());
            if (!level.hasChunkAt(destination)
                    || !(level.getBlockEntity(destination) instanceof EnergyInspectable target)) continue;
            List<BlockPos> terminals = topology.destinations().get(destination);
            // A full route can make an alternative parallel path usable for the same receiver.
            for (int attempt = 0; attempt < MAX_NODES && remaining > 0; attempt++) {
                int demand = target.capacity() - target.energy();
                if (demand <= 0) break;
                List<Segment> route = routeTo(topology, terminals, time);
                if (route.isEmpty()) break;
                int budget = remaining;
                int resistance = 0;
                for (Segment segment : route) {
                    budget = Math.min(budget, segment.cable().availableThisTick(time));
                    resistance += segment.resistanceMOhm();
                }
                var quote = ElectricalSolver.quoteFeTransfer(budget, demand, resistance);
                int offer = quote.offered();
                if (offer <= 0) break;

                // Server-thread callers guarantee the offered budget can be extracted once.
                int accepted = target.receiveEnergy(offer);
                if (accepted == 0) break;
                var transfer = quote.accepted(accepted);
                int loss = transfer.loss();
                int taken = extract.applyAsInt(accepted + loss);
                if (taken != accepted + loss) {
                    throw new IllegalStateException("Energy source violated extraction contract");
                }
                remaining -= taken;
                recordRoute(route, transfer, time);
                if (accepted < offer || taken < budget) break;
            }
        }
    }

    private static Topology discover(ServerLevel level, BlockPos source, Predicate<EnergyInspectable> acceptsTarget) {
        var inputs = new ArrayList<BlockPos>();
        var segments = new HashMap<BlockPos, Segment>();
        var destinations = new LinkedHashMap<BlockPos, List<BlockPos>>();
        var visited = new HashSet<BlockPos>();
        var queue = new ArrayDeque<BlockPos>();
        var sourceState = level.getBlockState(source);
        for (Direction side : Direction.values()) {
            if (!EnergyPorts.connects(sourceState, side)
                    || !ConfigurableConduit.modeAt(level, source, side).allowsOutput()) continue;
            BlockPos next = source.relative(side);
            if (!level.hasChunkAt(next) || !(level.getBlockEntity(next) instanceof CopperCableBlockEntity)
                    || !ConfigurableConduit.modeAt(level, next, side.getOpposite()).allowsInput()) continue;
            inputs.add(next);
            if (visited.add(next)) queue.addLast(next);
        }
        while (!queue.isEmpty()) {
            BlockPos pos = queue.removeFirst();
            if (!(level.getBlockState(pos).getBlock() instanceof CopperCableBlock cable)
                    || !(level.getBlockEntity(pos) instanceof CopperCableBlockEntity cableEntity)) continue;
            var outgoing = new ArrayList<BlockPos>();
            for (Direction side : Direction.values()) {
                if (!ConfigurableConduit.modeAt(level, pos, side).allowsOutput()) continue;
                BlockPos next = pos.relative(side);
                if (next.equals(source) || !level.hasChunkAt(next)
                        || !ConfigurableConduit.modeAt(level, next, side.getOpposite()).allowsInput()) continue;
                if (level.getBlockEntity(next) instanceof CopperCableBlockEntity) {
                    outgoing.add(next);
                    if (visited.add(next)) {
                        if (visited.size() > MAX_NODES) return null;
                        queue.addLast(next);
                    }
                } else if (level.getBlockEntity(next) instanceof EnergyInspectable target
                        && EnergyPorts.connects(level.getBlockState(next), side.getOpposite())
                        && acceptsTarget.test(target) && target.capacity() > target.energy()) {
                    destinations.computeIfAbsent(next, key -> new ArrayList<>()).add(pos);
                }
            }
            int resistance = cable.insulated() ? INSULATED_RESISTANCE_MOHM : BARE_RESISTANCE_MOHM;
            segments.put(pos, new Segment(cableEntity, resistance, List.copyOf(outgoing)));
        }
        return new Topology(List.copyOf(inputs), segments, destinations);
    }

    private static List<Segment> routeTo(Topology topology, List<BlockPos> terminals, long time) {
        var queue = new ArrayDeque<BlockPos>();
        var previous = new HashMap<BlockPos, BlockPos>();
        for (BlockPos input : topology.inputs()) {
            if (!usable(topology, input, time)) continue;
            queue.addLast(input);
            previous.put(input, null);
        }
        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            if (terminals.contains(current)) {
                var route = new ArrayList<Segment>();
                for (BlockPos pos = current; pos != null; pos = previous.get(pos)) {
                    route.add(topology.segments().get(pos));
                }
                return route.reversed();
            }
            for (BlockPos next : topology.segments().get(current).outgoing()) {
                if (previous.containsKey(next) || !usable(topology, next, time)) continue;
                previous.put(next, current);
                queue.addLast(next);
            }
        }
        return List.of();
    }

    private static boolean usable(Topology topology, BlockPos pos, long time) {
        Segment segment = topology.segments().get(pos);
        return segment != null && !segment.cable().isRemoved() && segment.cable().availableThisTick(time) > 0;
    }

    private static void recordRoute(List<Segment> route, ElectricalSolver.FeTransfer transfer, long time) {
        double current = transfer.currentMa();
        double potential = ElectricalSolver.feSourceTerminalVoltageMv(current);
        int cumulativeResistance = 0;
        int cumulativeLoss = 0;
        for (int i = 0; i < route.size(); i++) {
            Segment segment = route.get(i);
            CopperCableBlockEntity cable = segment.cable();
            boolean first = i == 0;
            boolean last = i == route.size() - 1;
            NetworkStatus status = transfer.extracted() == cable.availableThisTick(time)
                    ? NetworkStatus.CURRENT_LIMITED : transfer.status();
            cable.recordQesTelemetry((int) Math.round(potential), (int) Math.round(current),
                    segment.resistanceMOhm(), status);
            cumulativeResistance += segment.resistanceMOhm();
            int lossThroughSegment = transfer.lossThrough(cumulativeResistance);
            cable.recordTransfer(transfer.extracted(), lossThroughSegment - cumulativeLoss, time,
                    first ? transfer.extracted() : 0, last ? transfer.delivered() : 0);
            cumulativeLoss = lossThroughSegment;
            potential -= ElectricalMath.voltageDropMv(current, segment.resistanceMOhm());
        }
    }
}
