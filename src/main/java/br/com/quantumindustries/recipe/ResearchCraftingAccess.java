package br.com.quantumindustries.recipe;

import br.com.quantumindustries.research.WorldResearchData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Scope established by the vanilla crafting-menu bridge; automation has no player. */
public final class ResearchCraftingAccess {
    private ResearchCraftingAccess() {}

    private static final ThreadLocal<Player> CRAFTER = new ThreadLocal<>();

    public static void withCrafter(Player player, Runnable action) {
        Player previous = CRAFTER.get();
        CRAFTER.set(player);
        try {
            action.run();
        } finally {
            if (previous == null) CRAFTER.remove();
            else CRAFTER.set(previous);
        }
    }

    public static boolean allows(Level level, String research) {
        Player player = CRAFTER.get();
        return player != null && player.level() == level && level instanceof ServerLevel server
                && WorldResearchData.get(server).knows(player, research);
    }
}
