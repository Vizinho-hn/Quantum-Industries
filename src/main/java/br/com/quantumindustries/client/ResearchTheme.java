package br.com.quantumindustries.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

final class ResearchTheme {
    static final int DARK = 0xFF07131F, PANEL = 0xFF102331, LINE = 0xFF547C99;
    static final int CYAN = 0xFF30ECFF, GREEN = 0xFF58EB77, GOLD = 0xFFFFD762;
    static final int TEXT = 0xFFF1F8FF, MUTED = 0xFFADC9DF;

    private ResearchTheme() {}

    static void frame(GuiGraphicsExtractor g, int x, int y, int w, int h, int edge, int top, int bottom) {
        for (int i = 0; i < 4; i++) {
            g.fill(x + 4 - i, y + i, x + w - 4 + i, y + i + 1, edge);
            g.fill(x + 4 - i, y + h - i - 1, x + w - 4 + i, y + h - i, edge);
        }
        g.fill(x, y + 4, x + w, y + h - 4, edge);
        g.fillGradient(x + 1, y + 4, x + w - 1, y + h - 4, top, bottom);
        g.fill(x + 4, y + 1, x + w - 4, y + 4, top);
        g.fill(x + 4, y + h - 4, x + w - 4, y + h - 1, bottom);
    }

    static void lock(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x + 3, y, x + 9, y + 2, MUTED);
        g.fill(x + 2, y + 2, x + 4, y + 6, MUTED);
        g.fill(x + 8, y + 2, x + 10, y + 6, MUTED);
        g.fill(x + 1, y + 5, x + 11, y + 12, MUTED);
        g.fill(x + 5, y + 7, x + 7, y + 10, DARK);
    }
}
