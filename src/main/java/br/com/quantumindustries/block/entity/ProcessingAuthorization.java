package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.research.ResearchIds;
import br.com.quantumindustries.research.WorldResearchData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Shared processing authorization. Legacy inventories remain recoverable until a qualified player binds. */
final class ProcessingAuthorization {
    private ProcessingAuthorization() {}
    static boolean permitted(Level level, String operator, String research) {
        return level instanceof ServerLevel server && !operator.isEmpty()
                && WorldResearchData.get(server).knows(operator, ResearchIds.of(research));
    }
    static boolean canBind(Level level, BlockPos pos, Player player, String research) {
        return level instanceof ServerLevel server && player.level() == level && player.mayBuild()
                && player.distanceToSqr(pos.getCenter()) <= 64 && server.mayInteract(player, pos)
                && WorldResearchData.get(server).knows(player, research);
    }
}
