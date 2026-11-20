package br.com.quantumindustries.qks.preview;

import br.com.quantumindustries.block.entity.MechanicalPulleyBlockEntity;
import br.com.quantumindustries.block.entity.CogwheelBlockEntity;
import br.com.quantumindustries.mechanical.RotationalBlockEntity;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

@EventBusSubscriber(modid = "quantumindustries", value = Dist.CLIENT)
public final class QksPreviewClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<String> SHOTS = List.of("01-qks-cogwheels-flywheel-a.png",
            "02-qks-cogwheels-flywheel-b.png", "03-qks-pulleys-belts.png", "04-qks-large-wheels.png");
    private static final int FIRST_SHOT = 80, ROTATION_GAP = 7, BELT_SHOT = 115, WHEEL_SHOT = 145;
    private static final int BELT_VIEW_TICK = 90, WHEEL_VIEW_TICK = 120;
    private static final int FINISH_TICK = 170, FILE_TIMEOUT = 400, STARTUP_TIMEOUT = 2400;
    private static final double EPSILON = 1e-3;
    private static int ticks, totalTicks;
    private static boolean initialized, finished;
    private static double firstAngle;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean(QksPreviewSetup.PROPERTY) || finished) return;
        var game = Minecraft.getInstance();
        try {
            if (!initialized) {
                Files.createDirectories(game.gameDirectory.toPath().resolve("screenshots"));
                Files.deleteIfExists(result(game));
                for (String shot : SHOTS) Files.deleteIfExists(screenshot(game, shot));
                GLFW.glfwHideWindow(game.getWindow().handle());
                game.options.hideGui = true;
                initialized = true;
            }
            if (++totalTicks > STARTUP_TIMEOUT) throw new AssertionError("Timed out waiting for QksQA fixture/captures");
            if (QksPreviewSetup.failure != null) throw new AssertionError(QksPreviewSetup.failure);
            if (game.player == null || game.level == null || !QksPreviewSetup.ready) return;
            QksPreviewSetup.clock = ++ticks;
            if (ticks == 1) QksPreviewSetup.requestedView = 1;
            if (ticks == FIRST_SHOT) {
                requireView(1);
                verifyClient(game);
                firstAngle = rotor(game, QksPreviewSetup.SMALL).interpolatedAngleRadians(game.level.getGameTime());
                capture(game, SHOTS.get(0));
            }
            if (ticks == FIRST_SHOT + ROTATION_GAP) {
                requireView(1);
                double angle = rotor(game, QksPreviewSetup.SMALL).interpolatedAngleRadians(game.level.getGameTime());
                check(Math.abs(Math.IEEEremainder(angle - firstAngle, 2 * Math.PI)) > EPSILON,
                        "Client gear phase did not advance between the seven-tick captures");
                capture(game, SHOTS.get(1));
            }
            if (ticks == BELT_VIEW_TICK) QksPreviewSetup.requestedView = 2;
            if (ticks == BELT_SHOT) {
                requireView(2);
                verifyClientBelt(game, QksPreviewSetup.OPEN_A, QksPreviewSetup.OPEN_B, false);
                verifyClientBelt(game, QksPreviewSetup.CROSS_A, QksPreviewSetup.CROSS_B, true);
                capture(game, SHOTS.get(2));
            }
            if (ticks == WHEEL_VIEW_TICK) QksPreviewSetup.requestedView = 3;
            if (ticks == WHEEL_SHOT) {
                requireView(3);
                verifyClient(game);
                capture(game, SHOTS.get(3));
            }
            if (ticks < FINISH_TICK) return;
            if (ticks > FILE_TIMEOUT) throw new AssertionError("Timed out waiting for server checks or PNG writes");
            if (QksPreviewSetup.serverReport == null || !screenshotsWritten(game)) return;
            Files.writeString(result(game), "PASS: QKS PR 2 client preview; all fixture block entities present, finite synced motion, "
                    + "gear phase advanced across seven ticks; open/crossed belt partners and signed ratios synced.\n"
                    + QksPreviewSetup.serverReport + "Screenshots (decoded PNGs): " + String.join(", ", SHOTS)
                    + "\nVisual review still required: half-tooth alignment, belt strips, smoothness and rim/frustum clearance.\n");
            LOGGER.info("QKS preview passed: {}", result(game));
            finished = true;
            game.stop();
        } catch (Exception | AssertionError error) {
            finished = true;
            LOGGER.error("QKS preview failed", error);
            try {
                Files.writeString(result(game), "FAIL: " + error + "\n");
            } catch (IOException writeError) {
                LOGGER.error("Could not write QKS preview failure result", writeError);
            }
            game.stop();
        }
    }

    private static void verifyClient(Minecraft game) {
        for (BlockPos pos : QksPreviewSetup.ROTORS) {
            var rotor = rotor(game, pos);
            check(Double.isFinite(rotor.angleRadians()) && Double.isFinite(rotor.angularVelocity())
                    && Double.isFinite(rotor.angularAcceleration()) && Double.isFinite(rotor.gearRatio())
                    && Math.abs(rotor.angularVelocity()) > EPSILON && rotor.rotating() && !rotor.jammed(),
                    "Client missing synchronized motion at " + pos);
        }
        double ratio = rotor(game, QksPreviewSetup.LARGE).gearRatio() / rotor(game, QksPreviewSetup.SMALL).gearRatio();
        double expected = -(double) CogwheelBlockEntity.SMALL_TEETH / CogwheelBlockEntity.LARGE_TEETH;
        check(Math.abs(ratio - expected) < EPSILON, "Client gear ratio was not synchronized");
    }

    private static void verifyClientBelt(Minecraft game, BlockPos a, BlockPos b, boolean crossed) {
        check(rotor(game, a) instanceof MechanicalPulleyBlockEntity && rotor(game, b) instanceof MechanicalPulleyBlockEntity,
                "Client pulley block entity missing");
        var first = (MechanicalPulleyBlockEntity) rotor(game, a);
        var second = (MechanicalPulleyBlockEntity) rotor(game, b);
        check(first.validPartner(second) && first.crossed() == crossed, "Client belt endpoint/crossing not synchronized");
        check(Math.abs(second.gearRatio() / first.gearRatio() - (crossed ? -1 : 1)) < EPSILON,
                "Client belt ratio not synchronized");
    }

    private static RotationalBlockEntity rotor(Minecraft game, BlockPos pos) {
        if (!(game.level.getBlockEntity(pos) instanceof RotationalBlockEntity rotor))
            throw new AssertionError("Missing client rotor at " + pos);
        return rotor;
    }

    private static void capture(Minecraft game, String name) {
        Screenshot.grab(game.gameDirectory, name, game.getMainRenderTarget(), 1,
                message -> LOGGER.info("QKS screenshot {}: {}", name, message.getString()));
    }

    private static boolean screenshotsWritten(Minecraft game) throws IOException {
        for (String name : SHOTS) {
            Path path = screenshot(game, name);
            if (!Files.isRegularFile(path) || Files.size(path) == 0) return false;
            // Screenshot.grab saves asynchronously. A file's existence alone is not completion.
            try {
                var image = ImageIO.read(path.toFile());
                if (image == null) return false;
                check(image.getWidth() > 0 && image.getHeight() > 0, "Empty screenshot " + name);
            } catch (IOException pendingWrite) {
                return false;
            }
        }
        return true;
    }

    private static Path screenshot(Minecraft game, String name) {
        return game.gameDirectory.toPath().resolve("screenshots").resolve(name);
    }

    private static Path result(Minecraft game) {
        return game.gameDirectory.toPath().resolve("preview-result.txt");
    }

    private static void requireView(int view) {
        check(QksPreviewSetup.appliedView == view, "Server camera teleport was not acknowledged");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
