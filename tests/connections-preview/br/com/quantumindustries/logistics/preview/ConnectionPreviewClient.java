package br.com.quantumindustries.logistics.preview;

import br.com.quantumindustries.logistics.ConfigurableConduit;
import br.com.quantumindustries.logistics.ConnectionMode;
import br.com.quantumindustries.logistics.ConnectionTarget;
import java.nio.file.Files;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = "quantumindustries", value = Dist.CLIENT)
public final class ConnectionPreviewClient {
    private static int ticks;

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) throws Exception {
        if (!Boolean.getBoolean("quantumindustries.previewConnections")) return;
        var game = Minecraft.getInstance();
        org.lwjgl.glfw.GLFW.glfwHideWindow(game.getWindow().handle());
        if (game.player == null || ConnectionPreviewSetup.stage == 0) return;
        ticks++;
        if (ticks >= 160 && ticks <= 280 && (ticks - 160) % 40 == 0) {
            int index = (ticks - 160) / 40 + 1;
            var expected = switch (index) { case 1 -> ConnectionMode.PULL; case 2 -> ConnectionMode.PUSH; case 3 -> ConnectionMode.NONE; default -> ConnectionMode.AUTO; };
            var hit = (BlockHitResult) game.player.pick(6, 0, false);
            if (!hit.getBlockPos().equals(ConnectionPreviewSetup.PIPE)
                    || ConnectionTarget.face(hit.getBlockPos(), hit.getLocation(), hit.getDirection()) != Direction.EAST
                    || ((ConfigurableConduit) game.level.getBlockEntity(hit.getBlockPos())).connectionMode(Direction.EAST) != expected) {
                Files.writeString(game.gameDirectory.toPath().resolve("preview-result.txt"), "FAIL: client port/mode selection");
                game.stop();
                return;
            }
            Screenshot.grab(game.gameDirectory, "connection-" + expected.name().toLowerCase() + ".png", game.getMainRenderTarget(), 1, message -> {});
            if (index < 4) ConnectionPreviewSetup.stage++;
            else Files.writeString(game.gameDirectory.toPath().resolve("preview-result.txt"), "PASS: client observed all four synchronized endpoint modes and East hit selection\n");
        }
        if (ticks == 310) game.stop();
    }
}
