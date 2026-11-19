package br.com.quantumindustries.inspection;

import br.com.quantumindustries.block.PneumaticTubeBlock;
import br.com.quantumindustries.block.entity.PneumaticTubeBlockEntity;
import br.com.quantumindustries.logistics.ConfigurableConduit;
import br.com.quantumindustries.logistics.ItemRouting;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/** Exposes real in-transit packet state and connected pneumatic tube topology. */
public final class PneumaticInspectionProvider implements InspectionProvider {
    private static final int MAX_NODES = 128;
    private static final double TOPOLOGY_RADIUS_SQR = 12.0 * 12.0;

    @Override
    public Optional<InspectionSnapshot> inspect(ServerLevel level, BlockPos pos, boolean includeTopology) {
        if (!level.hasChunkAt(pos)) return Optional.empty();
        if (!(level.getBlockEntity(pos) instanceof PneumaticTubeBlockEntity tube)) return Optional.empty();

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

        int count = tube.packet().getCount();
        return Optional.of(new InspectionSnapshot(
                pos.immutable(),
                InspectionSnapshot.Kind.PNEUMATIC,
                tube.inlet().getAxis(),
                1,
                count,
                tube.inlet().get3DDataValue(),
                count > 0,
                false,
                visited.size(),
                count,
                0,
                false,
                0,
                syncedNodes));
    }

    private static Set<BlockPos> collectNetwork(ServerLevel level, BlockPos start) {
        Set<BlockPos> visited = new HashSet<>();
        var queue = new ArrayDeque<BlockPos>();
        visited.add(start.immutable());
        queue.add(start.immutable());

        while (!queue.isEmpty() && visited.size() < MAX_NODES) {
            BlockPos current = queue.removeFirst();
            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (!level.hasChunkAt(next) || visited.contains(next)) continue;

                boolean nextIsTube = level.getBlockState(next).getBlock() instanceof PneumaticTubeBlock;
                if (!nextIsTube && !ItemRouting.isEndpoint(level, next)) continue;

                var ownMode = ConfigurableConduit.modeAt(level, current, dir);
                var otherMode = ConfigurableConduit.modeAt(level, next, dir.getOpposite());
                boolean connected = ownMode.allowsInput() && otherMode.allowsOutput()
                        || ownMode.allowsOutput() && otherMode.allowsInput();
                if (!connected) continue;

                if (visited.add(next.immutable()) && nextIsTube) {
                    queue.addLast(next.immutable());
                }
            }
        }
        return visited;
    }
}
