package br.com.quantumindustries.client.render;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.block.entity.RotaryConversionBlockEntity;
import br.com.quantumindustries.energy.GeneratorModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * HUD grafica do Alternador Eletromagnetico com curva RPM x FE/t,
 * zonas de aquecimento/resfriamento e termometro de desarme termico.
 */
public final class AlternatorInspectionHud {
    private static final int COPPER = 0xFFB37B55;
    private static final int BG = 0xEE1B1816;
    private static final int PLOT_BG = 0xFF12100F;
    private static final int PAPER = 0xFFF4DCB9;
    private static final int TEXT = 0xFFF4EEE6;
    private static final int MUTED = 0xFFAAA096;
    private static final int GOLD = 0xFFE8BC65;
    private static final int GREEN = 0xFF83C66D;
    private static final int CYAN = 0xFF63B8E8;
    private static final int ORANGE = 0xFFE68A47;
    private static final int RED = 0xFFE16555;

    private static final double MAX_RPM_AXIS = 90.0;
    private static final int COOLING_RPM_LIMIT = 65;
    private static final int OPTIMAL_MIN_RPM = 50;
    private static final int OPTIMAL_MAX_RPM = 60;
    private static final int CUTIN_RPM = 12;
    private static final int RESET_HEAT = 40;

    private AlternatorInspectionHud() {}

    public static int draw(GuiGraphicsExtractor g, Minecraft game, RotaryConversionBlockEntity alt) {
        double rpm = Math.abs(alt.rpm());
        int heat = alt.rotorHeat();
        boolean trip = alt.isThermalTrip();
        boolean enabled = alt.isEnabled();
        int field = alt.field();
        int excitation = alt.excitation();
        int gen = alt.lastGenerated();
        int energy = alt.energy();
        int cap = Math.max(1, alt.capacity());

        boolean heating = rpm > COOLING_RPM_LIMIT;
        boolean cooling = !heating && heat > 0;
        boolean optimal = enabled && !trip && field > 0 && rpm >= OPTIMAL_MIN_RPM && rpm <= OPTIMAL_MAX_RPM;

        int width = 316;
        int height = 236;
        float scale = Math.min(1f, Math.min((g.guiWidth() - 14f) / width, (g.guiHeight() - 14f) / height));

        g.pose().pushMatrix();
        try {
            g.pose().translate(7, 7).scale(scale);

            // Moldura industrial padrao Quantum Industries
            g.fill(2, 0, width - 2, height, COPPER);
            g.fill(0, 2, width, height - 2, COPPER);
            g.fill(2, 2, width - 2, height - 2, BG);

            // Cabecalho
            g.fakeItem(new ItemStack(ModItems.COPPER_COIL.get()), 10, 8);
            var title = Component.translatable("block.quantumindustries.electromagnetic_generator");
            g.text(game.font, title, 32, 12, PAPER, false);
            drawBadge(g, game, width - 10, 9, trip, enabled, field, excitation, optimal, heating);

            g.fill(8, 30, width - 8, 31, COPPER);

            // ================================================================
            // GRAFICO 1: Curva de Geracao (FE/t) vs RPM + Zonas Termicas
            // ================================================================
            g.text(game.font, Component.translatable("gui.quantumindustries.alternator_hud.curve_title"), 10, 35, GOLD, false);
            g.text(game.font, String.format("%.0f RPM · %d FE/t", rpm, gen), width - 106, 35, optimal ? GREEN : TEXT, false);

            int gx = 12, gy = 48, gw = width - 24, gh = 64;
            g.fill(gx - 1, gy - 1, gx + gw + 1, gy + gh + 1, 0xFF4A3B31);
            g.fill(gx, gy, gx + gw, gy + gh, PLOT_BG);

            // Faixas verticais de regime
            int xCut = gx + (int) Math.round(gw * (CUTIN_RPM / MAX_RPM_AXIS));
            int xOpt0 = gx + (int) Math.round(gw * (OPTIMAL_MIN_RPM / MAX_RPM_AXIS));
            int xOpt1 = gx + (int) Math.round(gw * (OPTIMAL_MAX_RPM / MAX_RPM_AXIS));
            int xHeat = gx + (int) Math.round(gw * (COOLING_RPM_LIMIT / MAX_RPM_AXIS));

            // 0..12 RPM: abaixo do corte
            g.fill(gx, gy, xCut, gy + gh, 0x22FFFFFF);
            // 50..60 RPM: zona otima (8 FE/t, resfriando)
            g.fill(xOpt0, gy, xOpt1, gy + gh, 0x3383C66D);
            // 65..90 RPM: zona de aquecimento (+2 calor/tick)
            g.fill(xHeat, gy, gx + gw, gy + gh, 0x36E16555);

            // Linhas horizontais de grade (4 FE/t e 8 FE/t)
            int yMid = gy + gh / 2;
            g.fill(gx, yMid, gx + gw, yMid + 1, 0x22FFFFFF);
            g.fill(xOpt0, gy, xOpt0 + 1, gy + gh, 0x8883C66D);
            g.fill(xOpt1, gy, xOpt1 + 1, gy + gh, 0x8883C66D);
            g.fill(xHeat, gy, xHeat + 1, gy + gh, 0xAAE16555);

            // Plot da curva real GeneratorModel.netRate(rpm)
            int maxNet = GeneratorModel.netRate(55.0); // 8000 mFE/t = 8 FE/t
            for (int px = 0; px < gw; px++) {
                double sampleRpm = (px / (double) (gw - 1)) * MAX_RPM_AXIS;
                int net = GeneratorModel.netRate(sampleRpm);
                int barH = maxNet > 0 ? (int) Math.round((net / (double) maxNet) * (gh - 6)) : 0;
                int py = gy + gh - 2 - barH;
                int col = sampleRpm > COOLING_RPM_LIMIT ? RED
                        : (sampleRpm >= OPTIMAL_MIN_RPM && sampleRpm <= OPTIMAL_MAX_RPM ? GREEN
                        : (sampleRpm >= CUTIN_RPM ? GOLD : MUTED));
                if (barH > 0) {
                    g.fill(gx + px, py, gx + px + 1, gy + gh - 1, (col & 0x00FFFFFF) | 0x2E000000);
                    g.fill(gx + px, py - 1, gx + px + 1, py + 1, col);
                }
            }

            // Marcador da RPM atual na curva
            double clampedRpm = Math.clamp(rpm, 0.0, MAX_RPM_AXIS);
            int curX = gx + (int) Math.round((clampedRpm / MAX_RPM_AXIS) * (gw - 1));
            int curNet = GeneratorModel.netRate(clampedRpm);
            int curY = gy + gh - 2 - (maxNet > 0 ? (int) Math.round((curNet / (double) maxNet) * (gh - 6)) : 0);
            g.fill(curX, gy, curX + 1, gy + gh, 0xCCFFFFFF);
            g.fill(curX - 2, curY - 2, curX + 3, curY + 3, 0xFFFFFFFF);
            g.fill(curX - 1, curY - 1, curX + 2, curY + 2, heating ? RED : (optimal ? GREEN : GOLD));

            // Legenda do eixo X
            int axisY = gy + gh + 3;
            g.text(game.font, "0", gx, axisY, MUTED, false);
            g.text(game.font, "12", xCut - 5, axisY, MUTED, false);
            g.text(game.font, "50-60", xOpt0 - 2, axisY, GREEN, false);
            g.text(game.font, "65+", xHeat - 3, axisY, RED, false);
            g.text(game.font, "90", gx + gw - 12, axisY, MUTED, false);

            // ================================================================
            // GRAFICO 2: Termometro do Rotor (Aquecimento +2/t vs Resfriamento -1/t)
            // ================================================================
            int ty = axisY + 14;
            g.fill(8, ty - 4, width - 8, ty - 3, 0x66B37B55);

            g.text(game.font, Component.translatable("gui.quantumindustries.alternator_hud.thermal_title"), 10, ty, GOLD, false);
            Component rateComp = heating
                    ? Component.translatable("gui.quantumindustries.alternator_hud.rate.heating")
                    : (cooling
                    ? Component.translatable("gui.quantumindustries.alternator_hud.rate.cooling")
                    : Component.translatable("gui.quantumindustries.alternator_hud.rate.stable"));
            int rateCol = heating ? RED : (cooling ? CYAN : GREEN);
            g.text(game.font, rateComp, width - 10 - game.font.width(rateComp), ty, rateCol, false);

            int bx = 12, by = ty + 13, bw = width - 24, bh = 14;
            g.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, 0xFF4A3B31);
            g.fill(bx, by, bx + bw, by + bh, PLOT_BG);

            int xReset = bx + (int) Math.round(bw * (RESET_HEAT / (double) GeneratorModel.MAX_HEAT));
            int xWarn = bx + (int) Math.round(bw * 0.80);

            // Zonas de fundo do termometro: 0..40 seguro/religa, 40..80 alerta, 80..100 critico
            g.fill(bx, by, xReset, by + bh, 0x2863B8E8);
            g.fill(xReset, by, xWarn, by + bh, 0x22E8BC65);
            g.fill(xWarn, by, bx + bw, by + bh, 0x33E16555);

            // Preenchimento do calor atual
            int fillW = (int) Math.round(bw * (heat / (double) GeneratorModel.MAX_HEAT));
            int heatCol = trip || heat >= 80 ? RED : (heat > RESET_HEAT ? ORANGE : CYAN);
            if (fillW > 0) {
                g.fill(bx, by + 2, bx + fillW, by + bh - 2, heatCol);
            }
            // Marcador de religamento (40)
            g.fill(xReset, by - 1, xReset + 1, by + bh + 1, 0xDD63B8E8);

            // Texto sobre o termometro e legendas
            String heatTxt = heat + " / " + GeneratorModel.MAX_HEAT;
            g.text(game.font, heatTxt, bx + bw / 2 - game.font.width(heatTxt) / 2, by + 3, TEXT, false);

            int tAxisY = by + bh + 3;
            g.text(game.font, "0", bx, tAxisY, CYAN, false);
            g.text(game.font, Component.translatable("gui.quantumindustries.alternator_hud.reset_mark"), xReset - 18, tAxisY, CYAN, false);
            g.text(game.font, Component.translatable("gui.quantumindustries.alternator_hud.trip_mark"), bx + bw - 52, tAxisY, RED, false);

            // ================================================================
            // RODAPE: Excitacao/Campo, Buffer FE e Dica Dinamica
            // ================================================================
            int fy = tAxisY + 14;
            g.fill(8, fy - 4, width - 8, fy - 3, 0x66B37B55);

            Component fieldLine = field > 0
                    ? Component.translatable("gui.quantumindustries.alternator_hud.field_active", field, GeneratorModel.MAX_FIELD)
                    : Component.translatable("gui.quantumindustries.alternator_hud.field_exc", excitation, GeneratorModel.FIELD_START);
            g.text(game.font, fieldLine, 12, fy, field > 0 ? GREEN : GOLD, false);

            String bufTxt = energy + "/" + cap + " FE";
            g.text(game.font, bufTxt, width - 12 - game.font.width(bufTxt), fy, PAPER, false);

            // Barra de buffer FE
            int eby = fy + 12;
            g.fill(12, eby, width - 12, eby + 6, 0xFF3A3028);
            int eFill = (int) Math.round((width - 24) * (energy / (double) cap));
            if (eFill > 0) {
                g.fill(12, eby, 12 + eFill, eby + 6, GOLD);
            }

            // Dica contextual
            Component hint = pickHint(trip, enabled, field, excitation, rpm, heating, optimal);
            int hintCol = trip || heating ? ORANGE : (optimal ? GREEN : 0xFFB7C19B);
            g.text(game.font, hint, 12, eby + 11, hintCol, false);
        } finally {
            g.pose().popMatrix();
        }
        return 7 + Math.round(height * scale);
    }

    private static void drawBadge(GuiGraphicsExtractor g, Minecraft game, int rightX, int y,
            boolean trip, boolean enabled, int field, int excitation, boolean optimal, boolean heating) {
        String key;
        int col;
        if (trip) {
            key = "overheat";
            col = RED;
        } else if (!enabled) {
            key = "disabled";
            col = MUTED;
        } else if (field == 0 && excitation < GeneratorModel.FIELD_START) {
            key = "excitation";
            col = GOLD;
        } else if (heating) {
            key = "heating";
            col = ORANGE;
        } else if (optimal) {
            key = "optimal";
            col = GREEN;
        } else {
            key = "ready";
            col = PAPER;
        }
        var label = Component.translatable("gui.quantumindustries.alternator_hud.badge." + key);
        int bw = game.font.width(label) + 10;
        int x0 = rightX - bw;
        g.fill(x0, y, rightX, y + 15, col);
        g.fill(x0 + 1, y + 1, rightX - 1, y + 14, BG);
        g.text(game.font, label, x0 + 5, y + 4, col, false);
    }

    private static Component pickHint(boolean trip, boolean enabled, int field, int excitation,
            double rpm, boolean heating, boolean optimal) {
        if (trip) return Component.translatable("gui.quantumindustries.alternator_hud.hint.trip");
        if (!enabled) return Component.translatable("gui.quantumindustries.alternator_hud.hint.disabled");
        if (field == 0 && excitation < GeneratorModel.FIELD_START) {
            return Component.translatable("gui.quantumindustries.alternator_hud.hint.excitation", GeneratorModel.FIELD_START);
        }
        if (heating) return Component.translatable("gui.quantumindustries.alternator_hud.hint.heating");
        if (optimal) return Component.translatable("gui.quantumindustries.alternator_hud.hint.optimal");
        if (rpm < CUTIN_RPM) return Component.translatable("gui.quantumindustries.alternator_hud.hint.low_rpm");
        return Component.translatable("gui.quantumindustries.alternator_hud.hint.tune");
    }
}
