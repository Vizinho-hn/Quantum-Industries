package br.com.quantumindustries.mechanical;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/** Server-only bounded topology evaluation. Capacity is shared once per connected component. */
public final class MechanicalNetwork {
    public static final int MAX_NODES = 256, MAX_VISITS_PER_TICK = 8192;
    public record Node<N>(List<N> neighbors, int capacity, int load, int direction) {
        public Node {
            neighbors = List.copyOf(neighbors);
            if (capacity < 0 || load < 0) throw new IllegalArgumentException("Negative mechanical rating");
            direction = direction < 0 ? -1 : 1;
        }
        public Node(List<N> neighbors, int capacity, int load) { this(neighbors, capacity, load, 1); }
    }
    public record Result<N>(Set<N> nodes, int capacity, int load, boolean limited, int spinDirection) {
        public Result {
            nodes = Set.copyOf(nodes);
            spinDirection = spinDirection < 0 ? -1 : 1;
        }
        public Result(Set<N> nodes, int capacity, int load, boolean limited) { this(nodes, capacity, load, limited, 1); }
        public boolean powered() { return !limited && capacity > 0 && capacity >= load; }
    }
    private static final class Cache {
        long time = Long.MIN_VALUE;
        int visits;
        final Map<BlockPos, Result<BlockPos>> components = new HashMap<>();
    }
    private static final Map<ServerLevel, Cache> CACHES = new WeakHashMap<>();
    private MechanicalNetwork() {}

    public static <N> Result<N> evaluate(N start, Function<N, Node<N>> topology, int limit) {
        Set<N> visited = new HashSet<>(), queued = new HashSet<>();
        var pending = new ArrayDeque<N>();
        pending.add(start); queued.add(start);
        int capacity = 0, load = 0, signedTorque = 0;
        while (!pending.isEmpty()) {
            N at = pending.removeFirst();
            Node<N> node = topology.apply(at);
            if (node == null) continue;
            if (visited.size() >= limit) return new Result<>(visited, capacity, load, true, signedTorque < 0 ? -1 : 1);
            visited.add(at);
            capacity = Math.addExact(capacity, node.capacity());
            load = Math.addExact(load, node.load());
            if (node.capacity() > 0) signedTorque += node.capacity() * node.direction();
            for (N next : node.neighbors()) if (queued.add(next)) pending.addLast(next);
        }
        return new Result<>(visited, capacity, load, false, signedTorque < 0 ? -1 : 1);
    }

    public static Result<BlockPos> state(ServerLevel level, BlockPos start) {
        Cache cache = CACHES.computeIfAbsent(level, ignored -> new Cache());
        if (cache.time != level.getGameTime()) {
            cache.time = level.getGameTime(); cache.visits = 0; cache.components.clear();
        }
        var known = cache.components.get(start);
        if (known != null) return known;
        if (cache.visits >= MAX_VISITS_PER_TICK) return new Result<>(Set.of(), 0, 0, true);
        var result = evaluate(start, pos -> node(level, pos), Math.min(MAX_NODES, MAX_VISITS_PER_TICK - cache.visits));
        cache.visits += result.nodes().size();
        for (var pos : result.nodes()) cache.components.put(pos, result);
        return result;
    }
    private static Node<BlockPos> node(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof MechanicalParticipant current)) return null;
        var links = new java.util.ArrayList<BlockPos>(2);
        for (Direction direction : Direction.values()) {
            if (!current.connectsMechanical(direction)) continue;
            var next = pos.relative(direction);
            if (level.hasChunkAt(next) && level.getBlockEntity(next) instanceof MechanicalParticipant other
                    && other.connectsMechanical(direction.getOpposite())) links.add(next.immutable());
        }
        return new Node<>(links, current.sourceCapacity(), current.requestedLoad(), current.sourceDirection());
    }
    public static void invalidate(ServerLevel level) {
        Cache cache = CACHES.get(level);
        if (cache != null) cache.components.clear();
    }
}
