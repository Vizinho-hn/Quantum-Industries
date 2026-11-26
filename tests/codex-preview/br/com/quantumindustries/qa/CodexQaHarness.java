package br.com.quantumindustries.qa;

import br.com.quantumindustries.client.QuantumGuidebookScreen;
import br.com.quantumindustries.menu.QuantumGuidebookMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class CodexQaHarness {
    private static volatile boolean opened = false;

    @EventBusSubscriber(modid = "quantumindustries")
    public static final class ServerHooks {
        @SubscribeEvent
        public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (!Boolean.getBoolean("quantumindustries.codexPreview")) return;
            if (event.getEntity() instanceof ServerPlayer player && !opened) {
                opened = true;
                player.openMenu(new SimpleMenuProvider(
                        (id, inv, p) -> new QuantumGuidebookMenu(id, inv),
                        Component.translatable("item.quantumindustries.quantum_guidebook")));
            }
        }
    }

    @EventBusSubscriber(modid = "quantumindustries", value = Dist.CLIENT)
    public static final class ClientHooks {
        private static int ticks = 0;
        private static int originalGuiScale = 0;

        private static void grab(Minecraft game, String name) {
            Screenshot.grab(game.gameDirectory, name, game.getMainRenderTarget(), 1,
                    c -> System.out.println("CODEX_QA_SHOT " + name + " -> " + c.getString()));
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            if (!Boolean.getBoolean("quantumindustries.codexPreview")) return;
            Minecraft game = Minecraft.getInstance();
            if (game.player == null || !(game.screen instanceof QuantumGuidebookScreen screen)) return;
            String tag = System.getProperty("quantumindustries.codexTag", "default");

            ticks++;
            if (ticks == 1) {
                originalGuiScale = game.options.guiScale().get();
                game.options.guiScale().set(2);
                game.resizeGui();
            } else if (ticks == 15) {
                grab(game, tag + "-scale2-overview.png");
            } else if (ticks == 25) {
                game.options.guiScale().set(3);
                game.resizeGui();
            } else if (ticks == 40) {
                grab(game, tag + "-scale3-overview.png");
            } else if (ticks == 50) {
                game.options.guiScale().set(originalGuiScale);
                game.resizeGui();
                System.out.println("CODEX_QA_DONE " + tag);
            } else if (ticks == 58) {
                Runtime.getRuntime().halt(0);
            }
        }
    }
}
