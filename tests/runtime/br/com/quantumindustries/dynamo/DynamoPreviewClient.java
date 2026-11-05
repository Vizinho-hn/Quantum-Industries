package br.com.quantumindustries.dynamo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = "quantumindustries", value = Dist.CLIENT)
public final class DynamoPreviewClient {
    private static int ticks;

    private static void capture(Minecraft game, String name) {
        Screenshot.grab(game.gameDirectory, "v1-" + name, game.getMainRenderTarget(), 1,
                message -> System.out.println("DYNAMO_SCREENSHOT " + message.getString()));
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("quantumindustries.previewDynamo") || DynamoPreviewSetup.stage == 0) return;
        var game = Minecraft.getInstance();
        if (game.player == null || game.screen != null) return;
        switch (++ticks) {
            case 30 -> capture(game, "01-sem-manivela.png");
            case 40 -> DynamoPreviewSetup.stage = 2;
            case 65 -> capture(game, "02-leitura-12-fe.png");
            case 73 -> DynamoPreviewSetup.stage = 3;
            case 78 -> capture(game, "03-giro.png");
            case 95 -> capture(game, "04-leitura-24-fe.png");
            case 99 -> game.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
            case 104 -> capture(game, "06-oculos-equipados.png");
            case 105 -> {
                game.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                DynamoPreviewSetup.stage = 4;
            }
            case 125 -> capture(game, "05-sem-oculos.png");
            case 132 -> DynamoPreviewSetup.stage = 5;
            case 155 -> capture(game, "07-nucleo-mecanico.png");
            case 175 -> game.stop();
            default -> { }
        }
    }
}
