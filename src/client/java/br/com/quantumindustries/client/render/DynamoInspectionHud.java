package br.com.quantumindustries.client.render;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.block.entity.CopperCableBlockEntity;
import br.com.quantumindustries.block.entity.ElectricAlloySmelterBlockEntity;
import br.com.quantumindustries.block.entity.ElectricFurnaceBlockEntity;
import br.com.quantumindustries.block.entity.EnergyInspectable;
import br.com.quantumindustries.block.entity.MachineStatusInspectable;
import br.com.quantumindustries.block.entity.PneumaticTubeBlockEntity;
import br.com.quantumindustries.block.entity.ThermoelectricGeneratorBlockEntity;
import br.com.quantumindustries.client.inspection.ClientInspectionCache;
import br.com.quantumindustries.energy.qes.ElectricalNetworkState.NetworkStatus;
import br.com.quantumindustries.fluid.FluidInspectable;
import br.com.quantumindustries.inspection.InspectionSnapshot;
import br.com.quantumindustries.logistics.ConfigurableConduit;
import br.com.quantumindustries.logistics.ConnectionTarget;
import br.com.quantumindustries.mechanical.MechanicalParticipant;
import br.com.quantumindustries.thermal.ThermalInspectable;
import br.com.quantumindustries.thermal.ThermalParticipant;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
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
    private static final String QES = "gui.quantumindustries.qes.";
    private static final String THERMAL_HUD = "gui.quantumindustries.thermal_hud.";
    private static final int PANEL_TOP = 7;
    private static final int PANEL_GAP = 6;
    private static final int TELEMETRY_LINE_HEIGHT = 12;
    private static final int THERMAL_PANEL_HEIGHT = 5 * TELEMETRY_LINE_HEIGHT + 12;
    private static final double MILLI_PER_UNIT = 1000.0;
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
            int top = PANEL_TOP;
            boolean shift = player.isShiftKeyDown();
            InspectionSnapshot snap = ClientInspectionCache.forPos(hit.getBlockPos());
            boolean thermal = blockEntity instanceof ThermalInspectable || snap != null && snap.hasThermal();
            if (blockEntity instanceof br.com.quantumindustries.block.entity.MechanicalSawmillBlockEntity saw) {
                int bottom = SawmillInspectionHud.draw(g, minecraft, saw);
                if (shift && snap != null && snap.kind() == InspectionSnapshot.Kind.MECHANICAL) drawMechanical(g, minecraft, snap, bottom + PANEL_GAP, g.guiHeight() - PANEL_TOP, true);
                return;
            }
            if (blockEntity instanceof FluidInspectable fluid) {
                top = drawFluid(g, minecraft, fluid, snap, shift) + PANEL_GAP;
            } else if (blockEntity instanceof EnergyInspectable target) {
                top = drawEnergy(g, minecraft, target, snap, shift) + PANEL_GAP;
            } else if (blockEntity instanceof PneumaticTubeBlockEntity tube) {
                drawPneumatic(g, minecraft, tube, snap, shift);
                top = 54 + PANEL_GAP;
            }
            if (blockEntity instanceof ConfigurableConduit conduit) {
                int connectionTop = blockEntity instanceof CopperCableBlockEntity ? top : Math.max(84, top);
                drawConnection(g, minecraft, conduit, hit, connectionTop);
                top = connectionTop + 21 + PANEL_GAP;
            }
            if (blockEntity instanceof MechanicalParticipant && snap != null
                    && snap.kind() == InspectionSnapshot.Kind.MECHANICAL) {
                int availableBottom = g.guiHeight() - PANEL_TOP;
                if (thermal) {
                    availableBottom -= Math.min(THERMAL_PANEL_HEIGHT + PANEL_GAP, (availableBottom - top) / 2);
                }
                top = drawMechanical(g, minecraft, snap, top, availableBottom, shift) + PANEL_GAP;
            }
            if (thermal) {
                boolean legacy = !(blockEntity instanceof ThermalParticipant
                        || blockEntity instanceof ThermoelectricGeneratorBlockEntity);
                drawThermal(g, minecraft, snap, top, legacy);
                return;
            }
            if (top > PANEL_TOP) return;
        }

        if (blockEntity instanceof MachineStatusInspectable machine) {
            drawCompactStatus(g, minecraft, machine);
        }
    }

    private static void drawConnection(GuiGraphicsExtractor g, Minecraft minecraft,
            ConfigurableConduit conduit, BlockHitResult hit, int top) {
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
        g.fill(7, top, 7 + width, top + 21, 0xFFB37B55);
        g.fill(8, top + 1, 6 + width, top + 20, 0xED201C19);
        g.text(minecraft.font, label, 14, top + 7, color, false);
    }

    private static String trendSymbol() {
        int t = ClientInspectionCache.trend();
        return t > 0 ? "▲" : t < 0 ? "▼" : "=";
    }

    private static int drawEnergy(GuiGraphicsExtractor g, Minecraft minecraft,
            EnergyInspectable target, InspectionSnapshot snap, boolean shift) {
        if (target instanceof CopperCableBlockEntity cable) return drawCable(g, minecraft, cable, snap);
        int energy = snap != null && snap.kind() == InspectionSnapshot.Kind.ENERGY ? snap.localCapacity() : target.energy();
        int max = Math.max(1, snap != null && snap.kind() == InspectionSnapshot.Kind.ENERGY ? snap.localLoad() : target.capacity());
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
        List<Component> qesLines = new ArrayList<>();
        if (hasNet && target.capacity() > 0) {
            qesLines.add(Component.translatable(QES + "nominal_voltage", qesDecimal(snap.voltageMv())));
            qesLines.add(Component.translatable(QES + "machine_state", Component.translatable(
                    QES + (snap.powered() ? "state.energized" : "state.deenergized"))));
        }
        int transferLimit = switch (target) {
            case ElectricFurnaceBlockEntity furnace -> ElectricFurnaceBlockEntity.MAX_RECEIVE;
            case ElectricAlloySmelterBlockEntity smelter -> ElectricAlloySmelterBlockEntity.MAX_RECEIVE;
            case ThermoelectricGeneratorBlockEntity generator -> ThermoelectricGeneratorBlockEntity.TRANSFER;
            default -> 0;
        };
        if (transferLimit > 0) qesLines.add(Component.translatable(QES + "nominal_limit", transferLimit));
        int width = panelWidth(minecraft, qesLines, 168);
        if (netLine != null) width = Math.max(width, minecraft.font.width(netLine) + 16);
        int boxBottom = (detail == null ? 61 : 73) + (netLine == null ? 0 : 12)
                + qesLines.size() * TELEMETRY_LINE_HEIGHT;
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
        int y = barTop + 10 + (netLine == null ? 0 : TELEMETRY_LINE_HEIGHT);
        for (Component line : qesLines) {
            g.text(minecraft.font, line, 14, y, 0xFFF4EEE6, false);
            y += TELEMETRY_LINE_HEIGHT;
        }
        return boxBottom;
    }

    private static int drawCable(GuiGraphicsExtractor g, Minecraft game,
            CopperCableBlockEntity cable, InspectionSnapshot snap) {
        boolean ready = snap != null && snap.kind() == InspectionSnapshot.Kind.ENERGY;
        int flow = ready ? snap.powerFePerTick() : cable.flow();
        int loss = ready ? snap.lossFePerTick() : cable.loss();
        int limit = ready ? snap.localLoad() : cable.capacity();
        NetworkStatus status = ready ? networkStatus(snap.electricalStateOrdinal()) : cable.qesStatus();
        List<Component> lines = new ArrayList<>();
        lines.add(cable.inspectTitle());
        lines.add(Component.translatable("gui.quantumindustries.cable.stored"));
        lines.add(Component.translatable(QES + "flow_and_loss", flow, loss));
        lines.add(Component.translatable(QES + "nominal_limit", limit));
        if (ready && (snap.voltageMv() != 0 || snap.currentMa() != 0 || snap.resistanceMOhm() != 0)) {
            lines.add(Component.translatable(QES + "measured", qesDecimal(snap.voltageMv()),
                    qesDecimal(snap.currentMa()), qesDecimal(snap.resistanceMOhm())));
        }
        lines.add(Component.translatable(QES + "network_state",
                Component.translatable(QES + "state." + status.name().toLowerCase(Locale.ROOT))));
        lines.add(ready ? Component.translatable("gui.quantumindustries.cable.reserve", snap.load(), snap.capacity())
                : Component.translatable("gui.quantumindustries.cable.pending"));
        lines.add(Component.translatable("gui.quantumindustries.cable.scope", ready ? snap.nodeCount() : 0));
        lines.add(Component.translatable("gui.quantumindustries.cable.storage_hint"));
        int width = panelWidth(game, lines, 220);
        int barTop = PANEL_TOP + 6 + lines.size() * TELEMETRY_LINE_HEIGHT;
        int bottom = barTop + 14;
        g.fill(7, PANEL_TOP, 7 + width, bottom, 0xFFB37B55);
        g.fill(8, PANEL_TOP + 1, 6 + width, bottom - 1, 0xE8201C19);
        drawTelemetryLines(g, game, lines, 14, PANEL_TOP + 6, 0xFFF4DCB9);
        int barWidth = width - 14;
        g.fill(14, barTop, 14 + barWidth, barTop + 6, 0xFF45382F);
        if (flow > 0) {
            g.fill(14, barTop, 14 + barWidth * Math.clamp(flow, 0, limit) / Math.max(1, limit),
                    barTop + 6, 0xFFE8BC65);
        }
        return bottom;
    }

    private static NetworkStatus networkStatus(int ordinal) {
        NetworkStatus[] states = NetworkStatus.values();
        return ordinal >= 0 && ordinal < states.length ? states[ordinal] : NetworkStatus.IDLE;
    }

    private static String qesDecimal(int milliValue) {
        return String.format(Locale.ROOT, "%.3f", milliValue / MILLI_PER_UNIT);
    }

    private static void drawThermal(GuiGraphicsExtractor g, Minecraft game, InspectionSnapshot snap, int top,
            boolean legacy) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(THERMAL_HUD + (legacy ? "estimated_title" : "title")));
        if (snap == null || !snap.hasThermal()) {
            lines.add(Component.translatable(THERMAL_HUD + "pending"));
        } else {
            lines.add(Component.translatable(THERMAL_HUD + "temperatures", snap.temperatureC(), snap.ambientC()));
            lines.add(Component.translatable(THERMAL_HUD + "limits", snap.targetTemperatureC(), snap.maxTemperatureC()));
            lines.add(Component.translatable(THERMAL_HUD + "storage", snap.thermalEnergyTu(), snap.heatCapacityTuPerC()));
            lines.add(Component.translatable(THERMAL_HUD + "rates", snap.heatingRateTu(), snap.coolingRateTu(),
                    snap.heatingRateTu() - snap.coolingRateTu()));
        }
        int width = panelWidth(game, lines, 168);
        int height = lines.size() * TELEMETRY_LINE_HEIGHT + 12;
        float scale = panelScale(g, top, g.guiHeight() - PANEL_TOP, width, height);
        if (scale <= 0) return;
        g.pose().pushMatrix();
        try {
            g.pose().translate(7f, (float) top).scale(scale);
            g.fill(0, 0, width, height, 0xFFF09654);
            g.fill(1, 1, width - 1, height - 1, 0xE8201C19);
            drawTelemetryLines(g, game, lines, 7, 6, 0xFFFFCB9C);
        } finally {
            g.pose().popMatrix();
        }
    }

    private static int panelWidth(Minecraft game, List<Component> lines, int minimumWidth) {
        int width = minimumWidth;
        for (Component line : lines) width = Math.max(width, game.font.width(line) + 16);
        return width;
    }

    private static float panelScale(GuiGraphicsExtractor g, int top, int bottom, int width, int height) {
        return Math.min(1.0f, Math.min((bottom - top) / (float) height, (g.guiWidth() - 14f) / width));
    }

    private static void drawTelemetryLines(GuiGraphicsExtractor g, Minecraft game, List<Component> lines,
            int x, int y, int titleColor) {
        for (int i = 0; i < lines.size(); i++) {
            g.text(game.font, lines.get(i), x, y + i * TELEMETRY_LINE_HEIGHT,
                    i == 0 ? titleColor : 0xFFF4EEE6, false);
        }
    }

    private static final double MOTION_EPSILON = 1.0e-6;
    private static final int MECHANICAL_LINE_HEIGHT = 12;

    private static int drawMechanical(GuiGraphicsExtractor g, Minecraft minecraft,
            InspectionSnapshot snap, int top, int availableBottom, boolean shift) {
        boolean moving = Math.abs(snap.angularVelocity()) > MOTION_EPSILON;
        Component state = Component.translatable(snap.powered()
                ? "gui.quantumindustries.mechanical.state.powered"
                : moving ? "gui.quantumindustries.mechanical.state.coasting"
                : "gui.quantumindustries.mechanical.state.stalled");
        Component reason = Component.translatable("gui.quantumindustries.mechanical.stop_reason."
                + snap.stopReason().name().toLowerCase(Locale.ROOT));
        int direction = moving ? (snap.angularVelocity() < 0 ? -1 : 1) : snap.spinDirection();
        Component spin = Component.translatable(direction < 0
                ? "gui.quantumindustries.mechanical.spin.ccw"
                : "gui.quantumindustries.mechanical.spin.cw");
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.quantumindustries.mechanical.title"));
        lines.add(Component.translatable("gui.quantumindustries.mechanical.state_reason", state, reason));
        lines.add(Component.translatable("gui.quantumindustries.mechanical.rpm", decimal(snap.rpm())));
        lines.add(Component.translatable("gui.quantumindustries.mechanical.capacity_load", snap.capacity(), snap.load()));
        lines.add(Component.translatable("gui.quantumindustries.mechanical.axle_spin",
                Component.translatable("gui.quantumindustries.mechanical.axis." + snap.axle().getName()), spin));
        lines.add(Component.translatable("gui.quantumindustries.mechanical.nodes", snap.nodeCount()));
        if (shift) {
            lines.add(Component.translatable("gui.quantumindustries.mechanical.angular_velocity", decimal(snap.angularVelocity())));
            lines.add(Component.translatable("gui.quantumindustries.mechanical.angular_acceleration", decimal(snap.angularAcceleration())));
            lines.add(Component.translatable("gui.quantumindustries.mechanical.torques",
                    decimal(snap.driveTorque()), decimal(snap.loadTorque()), decimal(snap.netTorque())));
            lines.add(Component.translatable("gui.quantumindustries.mechanical.inertias",
                    decimal(snap.equivalentInertia()), decimal(snap.localInertia())));
            lines.add(Component.translatable("gui.quantumindustries.mechanical.power", decimal(snap.mechanicalPower())));
            lines.add(Component.translatable("gui.quantumindustries.mechanical.ratio", decimal(snap.effectiveRatio())));
        }
        var hydraulic = br.com.quantumindustries.client.inspection.ClientHydraulicInspection.active();
        if (shift && hydraulic != null && hydraulic.target().equals(snap.target())) {
            lines.add(Component.translatable("gui.quantumindustries.hydraulic.title"));
            lines.add(Component.translatable("gui.quantumindustries.hydraulic.totals", decimal(hydraulic.positive()), decimal(hydraulic.negative()), decimal(hydraulic.resultant())));
            lines.add(Component.translatable("gui.quantumindustries.hydraulic.motion", decimal(br.com.quantumindustries.mechanical.KineticDynamics.toRpm(hydraulic.omega())), decimal(hydraulic.acceleration())));
            lines.add(Component.translatable("gui.quantumindustries.hydraulic.direction", hydraulic.axle().getName().toUpperCase(Locale.ROOT),
                    Math.abs(hydraulic.resultant()) < 1e-6 ? "0" : hydraulic.resultant() > 0 ? "+" : "-"));
            lines.add(Component.translatable("gui.quantumindustries.hydraulic.flow_legend"));
            lines.add(Component.translatable(Math.abs(hydraulic.omega()) < 1e-6
                    ? "gui.quantumindustries.hydraulic.rest_legend" : "gui.quantumindustries.hydraulic.torque_legend"));
        }
        lines.add(Component.translatable(shift
                ? "gui.quantumindustries.mechanical.diagnostics.on"
                : "gui.quantumindustries.mechanical.diagnostics.hint"));
        int width = 178;
        for (Component line : lines) width = Math.max(width, minecraft.font.width(line) + 20);
        int height = lines.size() * MECHANICAL_LINE_HEIGHT + 12;
        float scale = panelScale(g, top, availableBottom, width, height);
        if (scale <= 0) return top;
        g.pose().pushMatrix();
        try {
            g.pose().translate(7f, (float) top).scale(scale);
            g.fill(0, 0, width, height, 0xFFB37B55);
            g.fill(1, 1, width - 1, height - 1, 0xE8201C19);
            for (int i = 0; i < lines.size(); i++) {
                int color = i == 0 ? 0xFFF4DCB9 : i == 1
                        ? (snap.powered() ? 0xFF83A276 : moving ? 0xFFE4AE62 : 0xFFC16A4B)
                        : 0xFFF4EEE6;
                g.text(minecraft.font, lines.get(i), 7, 6 + i * MECHANICAL_LINE_HEIGHT, color, false);
            }
        } finally {
            g.pose().popMatrix();
        }
        return top + (int) Math.ceil(height * scale);
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static int drawFluid(GuiGraphicsExtractor g, Minecraft minecraft,
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
        return bottom;
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
