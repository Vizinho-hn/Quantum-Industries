package br.com.quantumindustries.client.render;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.block.entity.EnergyInspectable;
import br.com.quantumindustries.block.entity.MachineStatusInspectable;
import br.com.quantumindustries.block.entity.PneumaticTubeBlockEntity;
import br.com.quantumindustries.client.inspection.ClientInspectionCache;
import br.com.quantumindustries.fluid.FluidInspectable;
import br.com.quantumindustries.inspection.InspectionSnapshot;
import br.com.quantumindustries.logistics.ConfigurableConduit;
import br.com.quantumindustries.logistics.ConnectionTarget;
import br.com.quantumindustries.mechanical.MechanicalParticipant;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid = "quantumindustries", value = Dist.CLIENT)
public final class DynamoInspectionHud {
    private DynamoInspectionHud() {}

    @SubscribeEvent public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Identifier.fromNamespaceAndPath("quantumindustries", "energy_inspection"),
                (graphics, delta) -> draw(graphics, delta.getGameTimeDeltaPartialTick(false)));
    }

    private static void draw(GuiGraphicsExtractor g, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null || minecraft.options.hideGui) return;
        if (!(player.pick(6.0, partialTick, false) instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) return;

        var blockEntity = minecraft.level.getBlockEntity(hit.getBlockPos());
        boolean hasGoggles = player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.ENERGY_INSPECTION_GOGGLES.get());

        if (hasGoggles) {
            boolean drewTopPanel = false;
            boolean shift = player.isShiftKeyDown();
            InspectionSnapshot snap = ClientInspectionCache.forPos(hit.getBlockPos());
            if (blockEntity instanceof ConfigurableConduit conduit) drawConnection(g, minecraft, conduit, hit);
            if (blockEntity instanceof FluidInspectable fluid) {
                drawFluid(g, minecraft, fluid, snap, shift);
                drewTopPanel = true;
            } else if (blockEntity instanceof EnergyInspectable target) {
                drawEnergy(g, minecraft, target, snap, shift);
                drewTopPanel = true;
            } else if (blockEntity instanceof PneumaticTubeBlockEntity tube) {
                drawPneumatic(g, minecraft, tube, snap, shift);
                drewTopPanel = true;
            }
            if (blockEntity instanceof MechanicalParticipant) {
                if (snap != null && snap.kind() == InspectionSnapshot.Kind.MECHANICAL) {
                    drawMechanical(g, minecraft, snap, drewTopPanel ? 108 : 7, shift);
                    return;
                }
            }
            if (drewTopPanel) return;
        }

        if (blockEntity instanceof MachineStatusInspectable machine) {
            drawCompactStatus(g, minecraft, machine);
        }
    }

    private static void drawConnection(GuiGraphicsExtractor g, Minecraft minecraft,
            ConfigurableConduit conduit, BlockHitResult hit) {
        var side = ConnectionTarget.face(hit.getBlockPos(), hit.getLocation(), hit.getDirection());
        var mode = conduit.connectionMode(side);
        int color = switch (mode) {
            case PULL -> 0xFF6DBDE0;
            case PUSH -> 0xFFE4AE62;
            case NONE -> 0xFFB4A99A;
            case AUTO -> 0xFF83A276;
        };
        var label = Component.translatable("gui.quantumindustries.connection.changed",
                Component.translatable("gui.quantumindustries.connection.face." + side.getName()), mode.label());
        int width = Math.max(168, minecraft.font.width(label) + 16);
        g.fill(7, 84, 7 + width, 105, 0xFFB37B55);
        g.fill(8, 85, 6 + width, 104, 0xED201C19);
        g.text(minecraft.font, label, 14, 91, color, false);
    }

    private static String trendSymbol() {
        int t = ClientInspectionCache.trend();
        return t > 0 ? "▲" : t < 0 ? "▼" : "=";
    }

    private static void drawEnergy(GuiGraphicsExtractor g, Minecraft minecraft,
            EnergyInspectable target, InspectionSnapshot snap, boolean shift) {
        int energy = target.energy(), max = Math.max(1, target.capacity());
        int percent = energy * 100 / max;
        Component detail = target.inspectDetail();
        boolean hasNet = snap != null && snap.kind() == InspectionSnapshot.Kind.ENERGY && snap.nodeCount() > 0;
        Component netLine = hasNet
                ? Component.translatable("gui.quantumindustries.inspection.network_trend",
                        trendSymbol(), snap.nodeCount(),
                        Component.translatable(shift
                                ? "gui.quantumindustries.mechanical.overlay.on"
                                : "gui.quantumindustries.mechanical.overlay.hint"))
                : null;
        int width = 168;
        if (netLine != null) width = Math.max(width, minecraft.font.width(netLine) + 16);
        int boxBottom = (detail == null ? 61 : 73) + (netLine == null ? 0 : 12);
        g.fill(7, 7, 7 + width, boxBottom, 0xFFB37B55);
        g.fill(8, 8, 6 + width, boxBottom - 1, 0xE8201C19);
        g.text(minecraft.font, target.inspectTitle(), 14, 13, 0xFFF4DCB9, false);
        g.text(minecraft.font, Component.translatable("gui.quantumindustries.dynamo.energy", energy, max), 14, 25, 0xFFF4EEE6, false);
        g.text(minecraft.font, Component.translatable("gui.quantumindustries.dynamo.charge", percent), 14, 36, 0xFFF4EEE6, false);
        if (detail != null) {
            g.text(minecraft.font, detail, 14, 47, 0xFFAEC6AD, false);
        }
        int barTop = detail == null ? 48 : 60;
        int color = percent <= 20 ? 0xFFC16A4B : percent <= 70 ? 0xFFCEA457 : 0xFF83A276;
        g.fill(14, barTop, 166, barTop + 6, 0xFF45382F);
        if (energy > 0) g.fill(14, barTop, 14 + 152 * energy / max, barTop + 6, color);
        if (netLine != null) {
            g.text(minecraft.font, netLine, 14, barTop + 10, 0xFFB4A99A, false);
        }
    }

    private static void drawMechanical(GuiGraphicsExtractor g, Minecraft minecraft,
            InspectionSnapshot snap, int top, boolean topologyActive) {
        Component stateLabel;
        int stateColor;
        if (snap.limited()) {
            stateLabel = Component.translatable("gui.quantumindustries.mechanical.state.limited");
            stateColor = 0xFFC16A4B;
        } else if (snap.powered()) {
            stateLabel = Component.translatable("gui.quantumindustries.mechanical.state.active");
            stateColor = 0xFF83A276;
        } else if (snap.capacity() > 0) {
            stateLabel = Component.translatable("gui.quantumindustries.mechanical.state.overloaded");
            stateColor = 0xFFE4AE62;
        } else {
            stateLabel = Component.translatable("gui.quantumindustries.mechanical.state.unpowered");
            stateColor = 0xFFC16A4B;
        }

        Component title = Component.translatable("gui.quantumindustries.mechanical.title");
        Component capLoad = Component.translatable("gui.quantumindustries.mechanical.capacity_load",
                snap.capacity(), snap.load());
        Component spin = Component.translatable(snap.spinDirection() < 0
                ? "gui.quantumindustries.mechanical.spin.ccw"
                : "gui.quantumindustries.mechanical.spin.cw");
        Component axleSpin = Component.translatable("gui.quantumindustries.mechanical.axle_spin",
                snap.axle().getName().toUpperCase(Locale.ROOT), spin);
        Component nodes = Component.translatable("gui.quantumindustries.mechanical.nodes", snap.nodeCount());
        Component torqueLine = snap.hasTorque()
                ? Component.translatable("gui.quantumindustries.mechanical.hydraulic_torque",
                        String.format(Locale.ROOT, "%+.1f", snap.torqueTenths() / 10.0))
                : null;
        Component hint = Component.translatable(topologyActive
                ? "gui.quantumindustries.mechanical.overlay.on"
                : "gui.quantumindustries.mechanical.overlay.hint");

        int width = 178;
        for (Component line : new Component[]{title, stateLabel, capLoad, axleSpin, nodes, torqueLine, hint}) {
            if (line != null) width = Math.max(width, minecraft.font.width(line) + 20);
        }
        int bottom = top + (torqueLine == null ? 77 : 89);
        g.fill(7, top, 7 + width, bottom, 0xFFB37B55);
        g.fill(8, top + 1, 6 + width, bottom - 1, 0xE8201C19);
        g.text(minecraft.font, title, 14, top + 6, 0xFFF4DCB9, false);
        g.text(minecraft.font, stateLabel, 14, top + 18, stateColor, false);
        g.text(minecraft.font, capLoad, 14, top + 30, 0xFFF4EEE6, false);
        g.text(minecraft.font, axleSpin, 14, top + 42, 0xFFF4EEE6, false);
        g.text(minecraft.font, nodes, 14, top + 54, 0xFFAEC6AD, false);
        if (torqueLine != null) {
            g.text(minecraft.font, torqueLine, 14, top + 65, 0xFF6DBDE0, false);
            g.text(minecraft.font, hint, 14, top + 77, 0xFFB4A99A, false);
        } else {
            g.text(minecraft.font, hint, 14, top + 65, 0xFFB4A99A, false);
        }
    }

    private static void drawFluid(GuiGraphicsExtractor g, Minecraft minecraft,
            FluidInspectable fluid, InspectionSnapshot snap, boolean shift) {
        int amount = fluid.storedFluid(), capacity = Math.max(1, fluid.fluidCapacity());
        boolean hasNet = snap != null && snap.kind() == InspectionSnapshot.Kind.FLUID && snap.nodeCount() > 0;
        Component netLine = hasNet
                ? Component.translatable("gui.quantumindustries.inspection.network_trend",
                        trendSymbol(), snap.nodeCount(),
                        Component.translatable(shift
                                ? "gui.quantumindustries.mechanical.overlay.on"
                                : "gui.quantumindustries.mechanical.overlay.hint"))
                : null;
        int width = 168;
        if (netLine != null) width = Math.max(width, minecraft.font.width(netLine) + 16);
        int bottom = netLine == null ? 75 : 87;
        g.fill(7, 7, 7 + width, bottom, 0xFFB37B55);
        g.fill(8, 8, 6 + width, bottom - 1, 0xE8201C19);
        g.text(minecraft.font, fluid.fluidTitle(), 14, 13, 0xFFF4DCB9, false);
        g.text(minecraft.font, Component.translatable("gui.quantumindustries.fluid.amount", amount, capacity),
                14, 27, 0xFFF4EEE6, false);
        g.text(minecraft.font, fluid.fluidDetail(), 14, 42, 0xFFAEC6AD, false);
        g.fill(14, 60, 166, 66, 0xFF45382F);
        if (amount > 0) g.fill(14, 60, 14 + 152 * amount / capacity, 66, 0xFF6AA6D9);
        if (netLine != null) {
            g.text(minecraft.font, netLine, 14, 71, 0xFFB4A99A, false);
        }
    }

    private static void drawPneumatic(GuiGraphicsExtractor g, Minecraft minecraft,
            PneumaticTubeBlockEntity tube, InspectionSnapshot snap, boolean shift) {
        Component title = Component.translatable("block.quantumindustries.pneumatic_tube");
        Component packetLine = tube.packet().isEmpty()
                ? Component.translatable("gui.quantumindustries.pneumatic.empty")
                : Component.translatable("gui.quantumindustries.pneumatic.packet",
                        tube.packet().getCount(), tube.packet().getHoverName());
        int nodes = snap != null && snap.kind() == InspectionSnapshot.Kind.PNEUMATIC ? snap.nodeCount() : 1;
        Component netLine = Component.translatable("gui.quantumindustries.inspection.network_trend",
                tube.packet().isEmpty() ? "=" : "→", nodes,
                Component.translatable(shift
                        ? "gui.quantumindustries.mechanical.overlay.on"
                        : "gui.quantumindustries.mechanical.overlay.hint"));
        int width = Math.max(168, Math.max(minecraft.font.width(packetLine), minecraft.font.width(netLine)) + 16);
        g.fill(7, 7, 7 + width, 54, 0xFFB37B55);
        g.fill(8, 8, 6 + width, 53, 0xE8201C19);
        g.text(minecraft.font, title, 14, 13, 0xFFF4DCB9, false);
        g.text(minecraft.font, packetLine, 14, 26, tube.packet().isEmpty() ? 0xFFAEC6AD : 0xFFF4EEE6, false);
        g.text(minecraft.font, netLine, 14, 39, 0xFFB4A99A, false);
    }

    private static void drawCompactStatus(GuiGraphicsExtractor g, Minecraft minecraft, MachineStatusInspectable machine) {
        Component title = machine.statusTitle();
        Component detail = machine.statusDetail();
        MachineStatusInspectable.State state = machine.statusState();

        int titleW = minecraft.font.width(title);
        int detailW = minecraft.font.width(detail);
        int innerW = Math.max(118, Math.max(titleW, detailW) + 12);
        int cx = g.guiWidth() / 2;
        int top = g.guiHeight() / 2 + 16;
        int left = cx - innerW / 2;
        int right = left + innerW;
        int bottom = top + 31;

        int borderColor = switch (state) {
            case WORKING -> 0xFFE59B3A;
            case READY   -> 0xFF72C466;
            case BLOCKED -> 0xFFD9644A;
            case IDLE    -> 0xFF8A7968;
        };
        int detailColor = switch (state) {
            case WORKING -> 0xFFFFD885;
            case READY   -> 0xFFA6F29B;
            case BLOCKED -> 0xFFFFA58A;
            case IDLE    -> 0xFFC7BFB6;
        };

        g.fill(left - 1, top - 1, right + 1, bottom + 1, borderColor);
        g.fill(left, top, right, bottom, 0xDC1B1815);
        g.text(minecraft.font, title, left + 6, top + 4, 0xFFF4DCB9, false);
        g.text(minecraft.font, detail, left + 6, top + 14, detailColor, false);

        int barLeft = left + 6;
        int barRight = right - 6;
        int barTop = top + 25;
        g.fill(barLeft, barTop, barRight, barTop + 3, 0xFF3A3028);
        float progress = Math.clamp(machine.statusProgress(), 0.0f, 1.0f);
        if (state == MachineStatusInspectable.State.BLOCKED) {
            g.fill(barLeft, barTop, barLeft + Math.max(8, (barRight - barLeft) / 4), barTop + 3, 0xFFD9644A);
        } else if (progress > 0.0f) {
            int fillW = Math.max(2, Math.round((barRight - barLeft) * progress));
            g.fill(barLeft, barTop, barLeft + fillW, barTop + 3, borderColor);
        }
    }
}
