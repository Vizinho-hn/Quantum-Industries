package br.com.quantumindustries.energy;

import br.com.quantumindustries.block.CopperCableBlock;
import br.com.quantumindustries.block.entity.CopperCableBlockEntity;
import br.com.quantumindustries.block.entity.EnergyInspectable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import java.util.*;
import java.util.function.IntUnaryOperator;
import java.util.function.IntSupplier;

/**
 * Loaded-only BFS distribution. Every delivered packet is backed by extraction
 * from a real FE source. Loss is 1 FE for every 4 bare busbar segments.
 * No cable is an energy storage or a source of power.
 */
public final class CableNetwork {
    private CableNetwork() {}
    private static final int MAX_NODES = 128;

    private record Route(BlockPos pos, List<BlockPos> cables, int bare, int rate) {}

    public static void transfer(ServerLevel level, BlockPos source, IntSupplier stored,
                                IntUnaryOperator extract, int senderLimit) {
        transfer(level, source, stored, extract, senderLimit, target -> true);
    }

    public static void transfer(ServerLevel level, BlockPos source, IntSupplier stored,
                                IntUnaryOperator extract, int senderLimit,
                                java.util.function.Predicate<EnergyInspectable> acceptsTarget) {
        if (stored.getAsInt() <= 0) return;
        Queue<Route> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        for (Direction direction : Direction.values()) {
            BlockPos next = source.relative(direction);
            if (level.hasChunkAt(next) && level.getBlockState(next).getBlock() instanceof CopperCableBlock cable) {
                visited.add(next);
                queue.add(new Route(next, List.of(next), cable.insulated() ? 0 : 1,
                        cable.insulated() ? 64 : 32));
            }
        }
        List<Route> destinations = new ArrayList<>();
        while (!queue.isEmpty() && visited.size() <= MAX_NODES) {
            Route path = queue.remove();
            for (Direction direction : Direction.values()) {
                BlockPos next = path.pos().relative(direction);
                if (next.equals(source) || !level.hasChunkAt(next)) continue;
                if (level.getBlockState(next).getBlock() instanceof CopperCableBlock cable) {
                    if (visited.add(next) && visited.size() <= MAX_NODES) {
                        var steps = new ArrayList<>(path.cables());
                        steps.add(next);
                        queue.add(new Route(next, List.copyOf(steps),
                                path.bare() + (cable.insulated() ? 0 : 1),
                                Math.min(path.rate(), cable.insulated() ? 64 : 32)));
                    }
                } else if (level.getBlockEntity(next) instanceof EnergyInspectable target
                        && acceptsTarget.test(target) && target.capacity() > 0 && target.energy() < target.capacity()
                        && !destinations.stream().anyMatch(route -> route.pos().equals(next))) {
                    destinations.add(new Route(next, path.cables(), path.bare(), path.rate()));
                }
            }
        }
        if (destinations.isEmpty()) return;
        // Rotating priority avoids starving later consumers in a persistent network.
        int index = Math.floorMod(level.getGameTime(), destinations.size());
        int remaining = Math.min(senderLimit, stored.getAsInt());
        for (int i = 0; i < destinations.size() && remaining > 0; i++) {
            Route route = destinations.get((index + i) % destinations.size());
            if (!(level.getBlockEntity(route.pos()) instanceof EnergyInspectable target)) continue;
            int loss = route.bare() / 4;
            int budget = Math.min(remaining, route.rate());
            if (budget <= loss) continue;
            int offer = Math.min(budget - loss, target.capacity() - target.energy());
            if (offer <= 0) continue;
            // Server thread serializes receiving and extracting; the receiver's accepted
            // quantity is the sole basis for source extraction, preserving conservation.
            int accepted = target.receiveEnergy(offer);
            if (accepted <= 0) continue;
            int taken = extract.applyAsInt(accepted + loss);
            if (taken != accepted + loss) {
                throw new IllegalStateException("Energy source violated extraction contract");
            }
            remaining -= taken;
            for (BlockPos pos : route.cables()) {
                if (level.getBlockEntity(pos) instanceof CopperCableBlockEntity be) {
                    // Loss is charged once per route, never once per BE.
                    be.recordTransfer(accepted, pos.equals(route.cables().getLast()) ? loss : 0, level.getGameTime());
                }
            }
        }
    }
}
