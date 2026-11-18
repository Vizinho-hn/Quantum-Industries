package br.com.quantumindustries.qa;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = "quantumindustries", value = Dist.CLIENT)
public final class MachinesQaClient {
    private static int ticks;

    private static void capture(Minecraft game, String name) {
        Screenshot.grab(game.gameDirectory, name, game.getMainRenderTarget(), 1,
                c -> System.out.println("QA_SCREENSHOT " + name + " -> " + c.getString()));
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("quantumindustries.qaPreview")) return;
        var game = Minecraft.getInstance();
        if (game.player == null || MachinesQaSetup.stage == 0) return;

        ticks++;
        if (ticks == 1) {
            game.options.hideGui = true;
        }
        if (ticks == 40) {
            capture(game, "01-machines-overview.png");
            MachinesQaSetup.stage = 2;
        }
        if (ticks == 70) {
            capture(game, "02-metallurgy-and-crusher.png");
            MachinesQaSetup.stage = 3;
        }
        if (ticks == 100) {
            capture(game, "03-energy-fluid-pneumatic.png");
            MachinesQaSetup.stage = 4;
        }
        if (ticks == 130) {
            capture(game, "04-research-assay-vault.png");
            MachinesQaSetup.stage = 5;
        }
        if (ticks == 155) {
            capture(game, "05-batteries-closeup.png");
            MachinesQaSetup.stage = 6;
        }
        if (ticks == 180) {
            capture(game, "06-mechanical-era1-closeup.png");
        }
        if (ticks >= 200 && MachinesQaSetup.done) {
            System.out.println("QA_PREVIEW_DONE");
            game.options.hideGui = false;
            game.stop();
        }
    }
}
