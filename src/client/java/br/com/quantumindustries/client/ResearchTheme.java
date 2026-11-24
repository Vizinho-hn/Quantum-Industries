package br.com.quantumindustries.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

final class ResearchTheme {
    static final int DARK = 0xFF07131F, PANEL = 0xFF102331, LINE = 0xFF547C99;
    static final int CYAN = 0xFF30ECFF, GREEN = 0xFF58EB77, GOLD = 0xFFFFD762;
    static final int TEXT = 0xFFF1F8FF, MUTED = 0xFFADC9DF;

    private final boolean notebook;

    ResearchTheme(boolean notebook) { this.notebook = notebook; }

    int ink(int color) {
        if (!notebook) return color;
        return switch (color) {
            case DARK -> 0xFFD9C49A;
            case PANEL -> 0xFFEADBB9;
            case LINE -> 0xFF957C5A;
            case CYAN -> 0xFF955831;
            case GREEN -> 0xFF365632;
            case GOLD -> 0xFF79521F;
            case TEXT -> 0xFF30271D;
            case MUTED -> 0xFF594635;
            case 0xFF145368 -> 0xFFF7E8C4;
            case 0xFF176178 -> 0xFFBC9268;
            case 0xFF060A10 -> 0xFFB1A58E;
            case 0xFF0D1F2C -> 0xFFCBB68D;
            case 0xFF07111C -> 0xFF65452F;
            default -> color;
        };
    }

    void fill(GuiGraphicsExtractor g, int x, int y, int right, int bottom, int color) {
        g.fill(x, y, right, bottom, ink(color));
    }

    void text(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font, String label,
              int x, int y, int color, boolean shadow) {
        g.text(font, label, x, y, ink(color), shadow);
    }

    void binding(GuiGraphicsExtractor g) {
        for (int y = 80; y < 385; y += 38) {
            g.fill(4, y + 2, 13, y + 7, 0xFF604A36);
            g.fill(2, y, 12, y + 4, 0xFF746D60);
            g.fill(3, y, 11, y + 1, 0xFFD1C6AD);
        }
    }

    void sketches(GuiGraphicsExtractor g) {
        int ink = 0xFFAA8C65;
        // Copper winding and its two leads, drawn in the page margin.
        for (int i = 0; i < 5; i++) {
            int x = 172 + i * 8;
            g.fill(x + 2, 245, x + 7, 246, ink);
            g.fill(x + 2, 258, x + 7, 259, ink);
            g.fill(x + 1, 247, x + 2, 257, ink);
            g.fill(x + 7, 247, x + 8, 257, ink);
        }
        g.fill(162, 251, 174, 252, ink);
        g.fill(211, 251, 225, 252, ink);
        g.fill(169, 270, 216, 271, ink);
        g.fill(177, 275, 207, 276, ink);
    }

    void frame(GuiGraphicsExtractor g, int x, int y, int w, int h, int edge, int top, int bottom) {
        edge = ink(edge);
        top = ink(top);
        bottom = ink(bottom);
        for (int i = 0; i < 4; i++) {
            g.fill(x + 4 - i, y + i, x + w - 4 + i, y + i + 1, edge);
            g.fill(x + 4 - i, y + h - i - 1, x + w - 4 + i, y + h - i, edge);
        }
        g.fill(x, y + 4, x + w, y + h - 4, edge);
        g.fillGradient(x + 1, y + 4, x + w - 1, y + h - 4, top, bottom);
        g.fill(x + 4, y + 1, x + w - 4, y + 4, top);
        g.fill(x + 4, y + h - 4, x + w - 4, y + h - 1, bottom);
        if (notebook && w >= 125 && h >= 30) {
            for (int row = 7; row < h - 5; row += 11) {
                for (int col = 4 + (row * 13 % 17); col < w - 4; col += 19) {
                    g.fill(x + col, y + row, x + col + 1, y + row + 1, 0x19957C5A);
                }
            }
        }
    }

    void lock(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x + 3, y, x + 9, y + 2, ink(MUTED));
        g.fill(x + 2, y + 2, x + 4, y + 6, ink(MUTED));
        g.fill(x + 8, y + 2, x + 10, y + 6, ink(MUTED));
        g.fill(x + 1, y + 5, x + 11, y + 12, ink(MUTED));
        g.fill(x + 5, y + 7, x + 7, y + 10, ink(DARK));
    }
}
