package br.com.quantumindustries.inspection;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Produces a server-verified scientific snapshot for a domain subsystem. */
@FunctionalInterface
public interface InspectionProvider {
    Optional<InspectionSnapshot> inspect(ServerLevel level, BlockPos pos, boolean includeTopology);
}
