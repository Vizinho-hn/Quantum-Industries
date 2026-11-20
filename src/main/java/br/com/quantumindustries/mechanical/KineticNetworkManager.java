package br.com.quantumindustries.mechanical;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** One reference rotor per connected component; no chunk loads or client dependencies. */
public final class KineticNetworkManager {
    public static final int MAX_NODES = 256, MAX_VISITS_PER_TICK = 8192;
    public static final double STEP_SECONDS = .05;
    private static final double CYCLE_TOLERANCE = 1e-4, SPEED_EPSILON = 1e-6;
    public record Node(List<KineticLink> links, int capacity, int load, double inertia, double drag,
            double drive, double resistance, double staticFriction, double maxSpeed, double angle, double omega) {
        public Node { links = List.copyOf(links); }
    }
    public record Coupling(double ratio, double efficiency) {}
    public record Topology(Map<BlockPos, Coupling> members, double inertia, double drag, double drive,
            double resistance, double friction, double maxSpeed, int capacity, int load,
            boolean limited, boolean jammed, double initialAngle, double initialOmega) {
        public Topology { members = Map.copyOf(members); }
    }
    public record Motion(double unwrappedAngle, double omega, double alpha, double netTorque,
            boolean powered, StopReason reason) {}
    private static final class Component {
        Topology topology;
        Motion motion;
        int version;
        long id;
    }
    private static final class LevelRuntime {
        long time = Long.MIN_VALUE;
        int version, visits;
        boolean dirty;
        final Set<BlockPos> active = new TreeSet<>();
        final Map<BlockPos, Component> components = new HashMap<>();
        final Map<BlockPos, KineticSnapshot> snapshots = new HashMap<>();
        final Map<BlockPos, KineticSnapshot> sent = new HashMap<>();
    }
    private static final Map<ServerLevel, LevelRuntime> LEVELS = new WeakHashMap<>();
    private KineticNetworkManager() {}

    /** Pure bounded BFS, also used by the runtime and standalone topology checks. */
    public static Topology evaluate(BlockPos root, Function<BlockPos, Node> nodes, int limit) {
        var members = new LinkedHashMap<BlockPos, Coupling>();
        var queued = new HashMap<BlockPos, Coupling>();
        var pending = new ArrayDeque<BlockPos>();
        pending.add(root); queued.put(root, new Coupling(1, 1));
        double inertia = 0, drag = 0, drive = 0, resistance = 0, friction = 0;
        double max = Double.MAX_VALUE, momentum = 0, angle = 0, largestInertia = -1;
        int capacity = 0, load = 0;
        boolean limited = false, jammed = false;
        while (!pending.isEmpty()) {
            var pos = pending.removeFirst();
            var node = nodes.apply(pos);
            if (node == null) continue;
            if (members.size() >= Math.min(limit, MAX_NODES)) { limited = true; break; }
            var coupling = queued.get(pos);
            members.put(pos, coupling);
            double r = coupling.ratio(), e = coupling.efficiency();
            double reflected = KineticDynamics.reflectInertia(node.inertia(), r);
            inertia += reflected;
            momentum += node.inertia() * r * node.omega();
            if (reflected > largestInertia) { largestInertia = reflected; angle = node.angle() / r; }
            drag += KineticDynamics.reflectDrag(node.drag(), r);
            drive += KineticDynamics.reflectDriveTorque(node.drive(), r, e);
            resistance += KineticDynamics.reflectLoadTorque(node.resistance(), r, e);
            friction += KineticDynamics.reflectLoadTorque(node.staticFriction(), r, e);
            max = Math.min(max, node.maxSpeed() / Math.abs(r));
            capacity = Math.addExact(capacity, node.capacity()); load = Math.addExact(load, node.load());
            for (var link : node.links()) {
                double ratio = r * link.ratio(), efficiency = e * link.efficiency();
                if (!Double.isFinite(ratio) || ratio == 0 || efficiency <= 0) { jammed = true; continue; }
                var known = queued.get(link.target());
                if (known != null) {
                    if (Math.abs(known.ratio() - ratio) > CYCLE_TOLERANCE) jammed = true;
                    continue;
                }
                if (queued.size() >= MAX_VISITS_PER_TICK) { limited = true; continue; }
                queued.put(link.target(), new Coupling(ratio, efficiency)); pending.add(link.target());
            }
        }
        return new Topology(members, inertia, drag, drive, resistance, friction, max, capacity, load,
                limited, jammed, angle,
                inertia > 0 ? KineticDynamics.mergeAngularVelocity(momentum, inertia) : 0);
    }

    private static StopReason classifyStopReason(Topology topology) {
        return topology.jammed() ? StopReason.JAMMED
                : topology.limited() ? StopReason.NETWORK_LIMIT
                : topology.capacity() <= 0 && Math.abs(topology.drive()) <= SPEED_EPSILON ? StopReason.NO_SOURCE
                : topology.capacity() > 0 && Math.abs(topology.drive()) <= CYCLE_TOLERANCE ? StopReason.OPPOSING_SOURCES
                : topology.load() > topology.capacity() ? StopReason.OVERLOADED : StopReason.NONE;
    }

    private static Motion refreshMotion(Topology topology, Motion previous) {
        StopReason reason = classifyStopReason(topology);
        double omega = (topology.inertia() <= 0 || reason == StopReason.JAMMED) ? 0 : previous.omega();
        return new Motion(previous.unwrappedAngle(), omega, previous.alpha(), previous.netTorque(), reason == StopReason.NONE, reason);
    }

    public static Motion advance(Topology topology, Motion previous) {
        StopReason reason = classifyStopReason(topology);
        if (topology.inertia() <= 0 || reason == StopReason.JAMMED)
            return new Motion(previous.unwrappedAngle(), 0, 0, 0, false, reason);
        double load = topology.resistance();
        if (reason == StopReason.OVERLOADED || reason == StopReason.NETWORK_LIMIT)
            load = Math.max(load, Math.abs(topology.drive()) + 2);
        var step = KineticDynamics.advanceDetailed(new KineticDynamics.State(0, previous.omega()),
                topology.drive(), load, topology.inertia(), topology.drag(), topology.maxSpeed(), STEP_SECONDS, topology.friction());
        double omega = step.state().angularVelocityRadiansPerSecond();
        return new Motion(previous.unwrappedAngle() + (previous.omega() + omega) * .5 * STEP_SECONDS,
                omega, step.angularAcceleration(), step.netTorque(), reason == StopReason.NONE, reason);
    }

    private static Node node(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof MechanicalParticipant p)) return null;
        return new Node(p.kineticLinks(level, pos), p.sourceCapacity(), p.requestedLoad(), p.kineticInertia(),
                p.kineticDrag(), p.kineticDriveTorque(), p.kineticLoadTorque(), p.staticFrictionTorque(),
                p.maxAngularSpeed(), p.persistedAngleRadians(), p.persistedAngularVelocity());
    }
    public static KineticSnapshot snapshot(ServerLevel level, BlockPos pos) {
        var runtime = LEVELS.computeIfAbsent(level, ignored -> new LevelRuntime());
        runtime.active.add(pos.immutable());
        if (runtime.dirty || runtime.time != level.getGameTime() || !runtime.snapshots.containsKey(pos)) {
            tick(level, runtime);
        }
        return runtime.snapshots.getOrDefault(pos, KineticSnapshot.EMPTY);
    }
    public static void invalidate(ServerLevel level) {
        // Membership is revalidated cleanly; retain momentum until the replacement topology is known.
        var runtime = LEVELS.computeIfAbsent(level, ignored -> new LevelRuntime());
        runtime.version++;
        runtime.dirty = true;
    }
    public static int topologyVersion(ServerLevel level) {
        var runtime = LEVELS.get(level);
        return runtime == null ? 0 : runtime.version;
    }
    private static void tick(ServerLevel level, LevelRuntime runtime) {
        boolean freshTick = runtime.time != level.getGameTime();
        boolean rebuildAll = freshTick || runtime.dirty;
        runtime.time = level.getGameTime();
        runtime.dirty = false;
        runtime.active.removeIf(pos -> !level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof MechanicalParticipant));
        var previous = new HashMap<>(runtime.components);
        if (rebuildAll) {
            runtime.components.clear(); runtime.snapshots.clear(); runtime.visits = 0;
        }
        for (var root : new TreeSet<>(runtime.active)) {
            if (runtime.snapshots.containsKey(root)) continue;
            int budget = MAX_VISITS_PER_TICK - runtime.visits;
            if (budget <= 0) {
                if (level.getBlockEntity(root) instanceof MechanicalParticipant p) {
                    var step = KineticDynamics.advanceDetailed(new KineticDynamics.State(p.persistedAngleRadians(),
                            p.persistedAngularVelocity()), 0, 2, p.kineticInertia(), p.kineticDrag(), p.maxAngularSpeed(), STEP_SECONDS);
                    var stopped = new KineticSnapshot(root.asLong(), runtime.version, runtime.time,
                            step.state().angleRadians(), step.state().angularVelocityRadiansPerSecond(), step.angularAcceleration(),
                            0, 2, step.netTorque(), p.kineticInertia(), 1, 0, 0, 1, 1, false, true, false, StopReason.NETWORK_LIMIT);
                    runtime.snapshots.put(root, stopped); p.applyKineticSnapshot(stopped);
                }
                continue;
            }
            boolean[] boundary = {false};
            var topology = evaluate(root, pos -> {
                if (runtime.components.containsKey(pos)) { boundary[0] = true; return null; }
                return node(level, pos);
            }, Math.min(MAX_NODES, budget));
            if (boundary[0]) topology = new Topology(topology.members(), topology.inertia(), topology.drag(),
                    topology.drive(), topology.resistance(), topology.friction(), topology.maxSpeed(),
                    topology.capacity(), topology.load(), true, topology.jammed(), topology.initialAngle(), topology.initialOmega());
            runtime.visits += topology.members().size();
            Component old = null;
            BlockPos phaseAnchor = root;
            double dominantInertia = -1;
            for (var pos : new TreeSet<>(topology.members().keySet())) {
                var candidate = previous.get(pos);
                if (candidate == null) continue;
                if (candidate.topology.inertia() > dominantInertia) {
                    dominantInertia = candidate.topology.inertia(); old = candidate; phaseAnchor = pos;
                }
            }
            var component = new Component();
            boolean same = old != null && old.topology.members().equals(topology.members());
            component.id = root.asLong(); component.version = same ? old.version : ++runtime.version;
            component.topology = topology;
            double initialAngle = old == null ? topology.initialAngle()
                    : old.motion.unwrappedAngle() * old.topology.members().get(phaseAnchor).ratio()
                            / topology.members().get(phaseAnchor).ratio();
            var initial = same ? old.motion : new Motion(initialAngle, topology.initialOmega(), 0, 0, false, StopReason.NONE);
            component.motion = freshTick ? advance(topology, initial) : refreshMotion(topology, initial);
            for (var entry : topology.members().entrySet()) {
                var pos = entry.getKey(); double r = entry.getValue().ratio(); var m = component.motion;
                var snap = new KineticSnapshot(component.id, component.version, runtime.time,
                        KineticDynamics.wrap(m.unwrappedAngle() * r), m.omega() * r, m.alpha() * r,
                        topology.drive(), topology.resistance(), m.netTorque(), topology.inertia(), r,
                        topology.capacity(), topology.load(), topology.members().size(),
                        Math.abs(m.omega()) > SPEED_EPSILON ? (m.omega() * r < 0 ? -1 : 1) : (topology.drive() * r < 0 ? -1 : 1),
                        m.powered(), topology.limited(), topology.jammed(), m.reason());
                runtime.components.put(pos, component); runtime.snapshots.put(pos, snap); runtime.active.add(pos);
                if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof MechanicalParticipant p) p.applyKineticSnapshot(snap);
                var sent = runtime.sent.get(pos);
                if (shouldSync(sent, snap)) {
                    PacketDistributor.sendToPlayersTrackingChunk(level, ChunkPos.containing(pos), KineticSyncPayload.from(pos, snap));
                    runtime.sent.put(pos, snap);
                }
            }
        }
        runtime.sent.keySet().retainAll(runtime.active);
    }
    public static boolean shouldSync(KineticSnapshot old, KineticSnapshot next) {
        if (old == null) return next.powered() || next.jammed() || Math.abs(next.angularVelocity()) > SPEED_EPSILON;
        return Math.abs(old.angularVelocity() - next.angularVelocity()) > .05
                || old.angularVelocity() != 0 && next.angularVelocity() == 0
                || Math.abs(old.angularAcceleration() - next.angularAcceleration()) > .12
                || old.powered() != next.powered() || old.jammed() != next.jammed()
                || old.stopReason() != next.stopReason() || old.spinDirection() != next.spinDirection()
                || Math.abs(next.angularVelocity()) > SPEED_EPSILON && next.serverGameTime() - old.serverGameTime() >= 10;
    }
    @SubscribeEvent public static void onTick(ServerTickEvent.Post event) {
        for (var level : event.getServer().getAllLevels()) {
            var runtime = LEVELS.get(level);
            if (runtime != null) tick(level, runtime);
        }
    }
    @SubscribeEvent public static void onWatch(ChunkWatchEvent.Sent event) {
        var level = event.getLevel();
        var chunk = event.getChunk();
        var runtime = LEVELS.computeIfAbsent(level, ignored -> new LevelRuntime());
        for (var entry : chunk.getBlockEntities().entrySet()) {
            if (entry.getValue() instanceof MechanicalParticipant) runtime.active.add(entry.getKey().immutable());
        }
        for (var entry : chunk.getBlockEntities().entrySet()) {
            if (!(entry.getValue() instanceof MechanicalParticipant)) continue;
            var snap = snapshot(level, entry.getKey());
            PacketDistributor.sendToPlayer(event.getPlayer(), KineticSyncPayload.from(entry.getKey(), snap));
        }
    }
    @SubscribeEvent public static void onUnload(LevelEvent.Unload event) { LEVELS.remove(event.getLevel()); }
}
