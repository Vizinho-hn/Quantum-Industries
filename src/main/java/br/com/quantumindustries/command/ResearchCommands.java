package br.com.quantumindustries.command;

import br.com.quantumindustries.research.WorldResearchData;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class ResearchCommands {
    private ResearchCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("quantum:desbloquear_pesquisas")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> update(context.getSource(), true)));
        event.getDispatcher().register(Commands.literal("quantum:clear_conhecimento")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> update(context.getSource(), false)));
    }

    private static int update(CommandSourceStack source, boolean unlock) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        var research = WorldResearchData.get(player.level());
        if (unlock) research.unlockAll(player);
        else research.clearAll(player);
        String message = unlock ? "commands.quantumindustries.research.unlock"
                : "commands.quantumindustries.research.clear";
        source.sendSuccess(() -> Component.translatable(message, player.getDisplayName()), true);
        return 1;
    }
}
