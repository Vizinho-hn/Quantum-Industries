package br.com.quantumindustries.inspection;

import br.com.quantumindustries.block.CopperCableBlock;
import br.com.quantumindustries.block.entity.CopperCableBlockEntity;
import br.com.quantumindustries.block.entity.EnergyInspectable;
import br.com.quantumindustries.energy.EnergyPorts;
import br.com.quantumindustries.energy.qes.ElectricalMath;
import br.com.quantumindustries.energy.qes.ElectricalNetworkState.NetworkStatus;
import br.com.quantumindustries.logistics.ConfigurableConduit;
import br.com.quantumindustries.thermal.ThermalInspectable;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/** Exposes real electrical storage, cable flow/loss, and loaded cable topology. */
public final class EnergyInspectionProvider implements InspectionProvider {
    private static final int MAX_NODES = 128;
    private static final double TOPOLOGY_RADIUS_SQR = 12.0 * 12.0;

    @Override
    public Optional<InspectionSnapshot> inspect(ServerLevel level, BlockPos pos, boolean includeTopology) {
        if (!level.hasChunkAt(pos)) return Optional.empty();
        var be = level.getBlockEntity(pos);
        if (!(be instanceof EnergyInspectable inspectable)) return Optional.empty();

        Set<BlockPos> visited = collectNetwork(level, pos);
        List<BlockPos> syncedNodes = List.of();
        if (includeTopology && !visited.isEmpty()) {
            syncedNodes = visited.stream()
                    .filter(node -> node.distSqr(pos) <= TOPOLOGY_RADIUS_SQR)
                    .sorted(Comparator.comparingDouble(node -> node.distSqr(pos)))
                    .limit(InspectionSnapshot.MAX_SYNCED_NODES)
                    .map(BlockPos::immutable)
                    .toList();
        }

        int energy = inspectable.energy();
        int cap = inspectable.capacity();
        var cable = be instanceof CopperCableBlockEntity conductor ? conductor : null;
        boolean isCable = cable != null;
        boolean active = energy > 0;
        boolean bottleneck = isCable && cap > 0 && energy >= cap;
        long stored = 0, storageCapacity = 0;
        if (isCable) for (BlockPos node : visited) {
            var endpoint = level.getBlockEntity(node);
            if (endpoint instanceof EnergyInspectable storage && !(endpoint instanceof CopperCableBlockEntity)) {
                stored += Math.max(0, storage.energy()); storageCapacity += Math.max(0, storage.capacity());
            }
        }

        var snapshot = new InspectionSnapshot(
                pos.immutable(),
                InspectionSnapshot.Kind.ENERGY,
                Direction.Axis.Y,
                isCable ? (int)Math.min(Integer.MAX_VALUE, storageCapacity) : cap,
                isCable ? (int)Math.min(Integer.MAX_VALUE, stored) : energy,
                0,
                active,
                bottleneck,
                visited.size(),
                energy,
                cap,
                false,
                0,
                syncedNodes);
        if (cable != null) {
            snapshot = snapshot.withElectrical(cable.voltageMv(), cable.currentMa(), cable.resistanceMOhm(),
                    cable.flow(), cable.loss(), cable.qesStatus().ordinal());
        } else {
            snapshot = snapshot.withElectrical(active ? ElectricalMath.NOMINAL_VOLTAGE_MV : 0, 0, 0, 0, 0,
                    (active ? NetworkStatus.ACTIVE : NetworkStatus.IDLE).ordinal());
        }
        if (be instanceof ThermalInspectable thermal) {
            snapshot = snapshot.withThermal(thermal.thermalSnapshot(), includeTopology);
        }
        return Optional.of(snapshot);
    }

    private static Set<BlockPos> collectNetwork(ServerLevel level, BlockPos start) {
        Set<BlockPos> visited = new HashSet<>();
        var queue = new ArrayDeque<BlockPos>();
        visited.add(start.immutable());
        queue.add(start.immutable());

        while (!queue.isEmpty() && visited.size() < MAX_NODES) {
            BlockPos current = queue.removeFirst();
            boolean currentIsCable = level.getBlockState(current).getBlock() instanceof CopperCableBlock;
            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (!level.hasChunkAt(next) || visited.contains(next)) continue;

                boolean nextIsCable = level.getBlockState(next).getBlock() instanceof CopperCableBlock;
                if (!currentIsCable && !nextIsCable) continue;

                if (currentIsCable && !nextIsCable) {
                    if (!(level.getBlockEntity(next) instanceof EnergyInspectable)) continue;
                    if (!EnergyPorts.connects(level.getBlockState(next), dir.getOpposite())) continue;
                }
                if (!currentIsCable && nextIsCable) {
                    if (!EnergyPorts.connects(level.getBlockState(current), dir)) continue;
                }

                var ownMode = ConfigurableConduit.modeAt(level, current, dir);
                var otherMode = ConfigurableConduit.modeAt(level, next, dir.getOpposite());
                boolean connected = ownMode.allowsInput() && otherMode.allowsOutput()
                        || ownMode.allowsOutput() && otherMode.allowsInput();
                if (!connected) continue;

                if (visited.size() >= MAX_NODES) break;
                if (visited.add(next.immutable()) && nextIsCable) {
                    queue.addLast(next.immutable());
                }
            }
        }
        return visited;
    }
}
