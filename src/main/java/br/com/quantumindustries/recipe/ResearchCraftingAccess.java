package br.com.quantumindustries.recipe;

import br.com.quantumindustries.research.WorldResearchData;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Scope established by the vanilla crafting-menu bridge; automation has no player. */
public final class ResearchCraftingAccess {
    private ResearchCraftingAccess() {}

    private static final ThreadLocal<Player> CRAFTER = new ThreadLocal<>();

    public static void withCrafter(Player player, Runnable action) {
        asCrafter(player, () -> {
            action.run();
            return null;
        });
    }

    public static <T> T asCrafter(Player player, Supplier<T> action) {
        Player previous = CRAFTER.get();
        CRAFTER.set(player);
        try {
            return action.get();
        } finally {
            if (previous == null) CRAFTER.remove();
            else CRAFTER.set(previous);
        }
    }

    static Player crafter(Level level) {
        Player player = CRAFTER.get();
        return player != null && player.level() == level && level instanceof ServerLevel ? player : null;
    }

    static boolean knows(Player player, String research) {
        return player.level() instanceof ServerLevel server && WorldResearchData.get(server).knows(player, research);
    }
}
