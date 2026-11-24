package br.com.quantumindustries.inspection;

import br.com.quantumindustries.block.FluidPipeBlock;
import br.com.quantumindustries.block.PeristalticPumpBlock;
import br.com.quantumindustries.fluid.FluidInspectable;
import br.com.quantumindustries.logistics.ConfigurableConduit;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Exposes real fluid volume, capacity, and connected pipe topology. */
public final class FluidInspectionProvider implements InspectionProvider {
    private static final int MAX_NODES = 128;
    private static final double TOPOLOGY_RADIUS_SQR = 12.0 * 12.0;

    @Override
    public Optional<InspectionSnapshot> inspect(ServerLevel level, BlockPos pos, boolean includeTopology) {
        if (!level.hasChunkAt(pos)) return Optional.empty();
        var be = level.getBlockEntity(pos);
        if (!(be instanceof FluidInspectable inspectable)) return Optional.empty();

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

        int stored = inspectable.storedFluid();
        int cap = inspectable.fluidCapacity();
        return Optional.of(new InspectionSnapshot(
                pos.immutable(),
                InspectionSnapshot.Kind.FLUID,
                Direction.Axis.Y,
                cap,
                stored,
                0,
                stored > 0,
                cap > 0 && stored >= cap,
                visited.size(),
                stored,
                cap,
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
            boolean currentIsPipe = level.getBlockState(current).getBlock() instanceof FluidPipeBlock;
            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (!level.hasChunkAt(next) || visited.contains(next)) continue;

                var nextState = level.getBlockState(next);
                boolean nextIsPipe = nextState.getBlock() instanceof FluidPipeBlock;
                boolean nextIsPump = nextState.getBlock() instanceof PeristalticPumpBlock
                        && dir.getAxis() == nextState.getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis();
                boolean nextIsTank = level.getBlockEntity(next) instanceof FluidInspectable;

                if (!currentIsPipe && !nextIsPipe) continue;
                if (currentIsPipe && !nextIsPipe && !nextIsPump && !nextIsTank) continue;

                var ownMode = ConfigurableConduit.modeAt(level, current, dir);
                var otherMode = ConfigurableConduit.modeAt(level, next, dir.getOpposite());
                boolean connected = ownMode.allowsInput() && otherMode.allowsOutput()
                        || ownMode.allowsOutput() && otherMode.allowsInput();
                if (!connected) continue;

                if (visited.add(next.immutable()) && nextIsPipe) {
                    queue.addLast(next.immutable());
                }
            }
        }
        return visited;
    }
}
