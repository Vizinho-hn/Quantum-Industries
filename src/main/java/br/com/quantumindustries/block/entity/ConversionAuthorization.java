package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.block.IndustrialConversionBlock.Kind;
import br.com.quantumindustries.research.ResearchIds;
import br.com.quantumindustries.research.WorldResearchData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

final class ConversionAuthorization {
    private ConversionAuthorization() {}
    static boolean permitted(Level level, String owner, Kind kind) {
        if (!(level instanceof ServerLevel server) || owner.isEmpty()) return false;
        var knowledge=WorldResearchData.get(server);
        return knowledge.knows(owner,ResearchIds.of(kind.research))
                && (!kind.steam() || knowledge.knows(owner,ResearchIds.of("thermal_annealing")));
    }
    static boolean canBind(Level level, BlockPos pos, Player player, Kind kind) {
        return level instanceof ServerLevel server && player.level()==level && player.mayBuild()
                && player.distanceToSqr(pos.getCenter())<=64 && server.mayInteract(player,pos)
                && permitted(level,player.getUUID().toString(),kind);
    }
}
