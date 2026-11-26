package br.com.quantumindustries.client.codex;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Dedicated Era V visual theme and nine-slice graphic toolkit for the Quantum Codex tablet. */
public final class CodexTheme {
    private static final String MOD = "quantumindustries";

    public static final int BG_TOP = 0xFF040D16;
    public static final int BG_BOTTOM = 0xFF071624;
    public static final int HEADER_BG = 0xFF06121E;
    public static final int FOOTER_BG = 0xFF05101B;

    public static final int CYAN = 0xFF34ECFF;
    public static final int CYAN_BRIGHT = 0xFF88F8FF;
    public static final int CYAN_SOFT = 0xFF22A6C4;
    public static final int CYAN_LINE = 0xFF1D4F66;
    public static final int CYAN_TRACE = 0xFF0E2C3E;

    public static final int EMERALD = 0xFF2CE88A;
    public static final int AMBER = 0xFFFFD460;
    public static final int TEXT = 0xFFF2FAFF;
    public static final int SUBTEXT = 0xFFA2C2D8;
    public static final int MUTED = 0xFF6F90A8;

    public static final Identifier PANEL_CHAMFERED = tex("panel_chamfered");
    public static final Identifier PANEL_ACTIVE = tex("panel_active");
    public static final Identifier PANEL_LOCKED = tex("panel_locked");
    public static final Identifier PANEL_DONE = tex("panel_done");
    public static final Identifier TAB_ACTIVE = tex("tab_active");
    public static final Identifier TAB_IDLE = tex("tab_idle");
    public static final Identifier TAB_HOVER = tex("tab_hover");
    public static final Identifier BUTTON_CYAN = tex("button_cyan");
    public static final Identifier BUTTON_CYAN_HOVER = tex("button_cyan_hover");
    public static final Identifier BUTTON_DARK = tex("button_dark");
    public static final Identifier BUTTON_DARK_HOVER = tex("button_dark_hover");
    public static final Identifier ERA_PILL = tex("era_pill");
    public static final Identifier PREVIEW_STAGE = tex("preview_stage");
    public static final Identifier LOGO_Q = tex("logo_q");
    public static final Identifier ICONS = tex("icons");
    public static final Identifier CHECKBOXES = tex("checkboxes");

    public static final int ICON_CHEVRON = 0;
    public static final int ICON_LOCK = 1;
    public static final int ICON_TARGET = 2;
    public static final int ICON_SEARCH = 3;
    public static final int ICON_ROTATE = 4;
    public static final int ICON_PLAY = 5;
    public static final int ICON_BOOK = 6;
    public static final int ICON_TUTORIAL = 7;

    public static final int CHECK_PENDING = 0;
    public static final int CHECK_CURRENT = 1;
    public static final int CHECK_DONE = 2;

    private CodexTheme() {}

    private static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(MOD, "textures/gui/codex/" + name + ".png");
    }

    public static void drawBackdrop(GuiGraphicsExtractor g, int w, int h, int headerH, int footerY) {
        g.fillGradient(0, 0, w, h, BG_TOP, BG_BOTTOM);
        // Header and footer plates
        g.fill(0, 0, w, headerH, HEADER_BG);
        g.fill(0, footerY, w, h, FOOTER_BG);
        // Subtle circuit trace accents in header/footer
        g.fill(0, headerH - 1, w, headerH, CYAN_LINE);
        g.fill(w / 2 - 140, headerH - 1, w / 2 + 140, headerH, CYAN_SOFT);
        g.fill(0, footerY, w, footerY + 1, CYAN_LINE);
        g.fill(w / 2 - 120, footerY, w / 2 + 120, footerY + 1, CYAN_SOFT);
        // Decorative circuit lines flanking the era pill
        int midY = headerH / 2;
        g.fill(184, midY, 232, midY + 1, CYAN_TRACE);
        g.fill(432, midY, 476, midY + 1, CYAN_TRACE);
    }

    public static void drawPanel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        drawNineSlice(g, PANEL_CHAMFERED, 48, 48, 10, x, y, w, h);
    }

    public static void drawActiveCard(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        drawNineSlice(g, PANEL_ACTIVE, 48, 48, 12, x - 1, y - 1, w + 2, h + 2);
    }

    public static void drawLockedCard(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        drawNineSlice(g, PANEL_LOCKED, 48, 48, 9, x, y, w, h);
    }

    public static void drawDoneCard(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        drawNineSlice(g, PANEL_DONE, 48, 48, 10, x - 1, y - 1, w + 2, h + 2);
    }

    public static void drawTab(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean active, boolean hovered) {
        Identifier tex = active ? TAB_ACTIVE : hovered ? TAB_HOVER : TAB_IDLE;
        int pad = active ? 1 : 0;
        drawNineSlice(g, tex, 48, 24, 8, x - pad, y - pad, w + pad * 2, h + pad * 2);
    }

    public static void drawCyanButton(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean hovered) {
        drawNineSlice(g, hovered ? BUTTON_CYAN_HOVER : BUTTON_CYAN, 64, 28, 10, x - 1, y - 1, w + 2, h + 2);
    }

    public static void drawDarkButton(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean hovered) {
        drawNineSlice(g, hovered ? BUTTON_DARK_HOVER : BUTTON_DARK, 64, 24, 8, x, y, w, h);
    }

    public static void drawEraPill(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        drawNineSlice(g, ERA_PILL, 128, 28, 12, x - 1, y - 1, w + 2, h + 2);
    }

    public static void drawPreviewStage(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.blit(RenderPipelines.GUI_TEXTURED, PREVIEW_STAGE, x, y, 0f, 0f, w, h, 256, 160, 256, 160);
    }

    public static void drawLogoQ(GuiGraphicsExtractor g, int x, int y, int size) {
        g.blit(RenderPipelines.GUI_TEXTURED, LOGO_Q, x, y, 0f, 0f, size, size, 32, 32, 32, 32);
    }

    public static void drawIcon(GuiGraphicsExtractor g, int iconIndex, int x, int y, int size) {
        g.blit(RenderPipelines.GUI_TEXTURED, ICONS, x, y, iconIndex * 16f, 0f, size, size, 16, 16, 128, 16);
    }

    public static void drawIconFlippedX(GuiGraphicsExtractor g, int iconIndex, int x, int y, int size) {
        g.blit(RenderPipelines.GUI_TEXTURED, ICONS, x, y, (iconIndex + 1) * 16f, 0f, size, size, -16, 16, 128, 16);
    }

    public static void drawCheckbox(GuiGraphicsExtractor g, int state, int x, int y, int size) {
        g.blit(RenderPipelines.GUI_TEXTURED, CHECKBOXES, x, y, state * 16f, 0f, size, size, 16, 16, 48, 16);
    }

    public static void drawScaledText(GuiGraphicsExtractor g, Font font, String text, int x, int y, float scale, int color) {
        if (Math.abs(scale - 1.0f) < 0.01f) {
            g.text(font, text, x, y, color, false);
            return;
        }
        g.pose().pushMatrix();
        g.pose().translate(x, y).scale(scale);
        g.text(font, text, 0, 0, color, false);
        g.pose().popMatrix();
    }

    public static void drawScaledBoldText(GuiGraphicsExtractor g, Font font, String text, int x, int y, float scale, int color) {
        g.pose().pushMatrix();
        g.pose().translate(x, y).scale(scale);
        g.text(font, text, 0, 0, color, false);
        g.text(font, text, 1, 0, color, false);
        g.pose().popMatrix();
    }

    public static void drawNineSlice(GuiGraphicsExtractor g, Identifier texture, int texW, int texH, int border,
                                     int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) return;
        int b = Math.min(border, Math.min(w / 2, h / 2));
        int srcMidW = texW - border * 2;
        int srcMidH = texH - border * 2;
        int dstMidW = w - b * 2;
        int dstMidH = h - b * 2;

        // 4 corners
        blit(g, texture, x, y, 0, 0, b, b, border, border, texW, texH);
        blit(g, texture, x + w - b, y, texW - border, 0, b, b, border, border, texW, texH);
        blit(g, texture, x, y + h - b, 0, texH - border, b, b, border, border, texW, texH);
        blit(g, texture, x + w - b, y + h - b, texW - border, texH - border, b, b, border, border, texW, texH);

        // Top & bottom edges
        if (dstMidW > 0) {
            blit(g, texture, x + b, y, border, 0, dstMidW, b, srcMidW, border, texW, texH);
            blit(g, texture, x + b, y + h - b, border, texH - border, dstMidW, b, srcMidW, border, texW, texH);
        }
        // Left & right edges
        if (dstMidH > 0) {
            blit(g, texture, x, y + b, 0, border, b, dstMidH, border, srcMidH, texW, texH);
            blit(g, texture, x + w - b, y + b, texW - border, border, b, dstMidH, border, srcMidH, texW, texH);
        }
        // Center
        if (dstMidW > 0 && dstMidH > 0) {
            blit(g, texture, x + b, y + b, border, border, dstMidW, dstMidH, srcMidW, srcMidH, texW, texH);
        }
    }

    private static void blit(GuiGraphicsExtractor g, Identifier tex, int x, int y,
                             int u, int v, int w, int h, int rw, int rh, int tw, int th) {
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, (float) u, (float) v, w, h, rw, rh, tw, th);
    }
}
