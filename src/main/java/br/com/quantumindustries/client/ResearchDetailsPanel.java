package br.com.quantumindustries.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import static br.com.quantumindustries.client.ResearchTheme.*;

final class ResearchDetailsPanel {
    record Entry(String name, String description, String reward, String[] costs) {}

    static void draw(GuiGraphicsExtractor g, Font font, Entry entry, int x, int y, int w,
                     boolean[] ready) {
        frame(g, x, y, w, 107, CYAN, PANEL, DARK);
        g.text(font, entry.name(), x + 9, y + 8, CYAN, false);
        g.text(font, entry.description(), x + 9, y + 22, TEXT, false);
        g.fill(x + 9, y + 34, x + w - 9, y + 35, LINE);
        g.text(font, "REQUISITOS", x + 9, y + 40, MUTED, false);
        for (int i = 0; i < entry.costs().length; i++)
            g.text(font, (ready[i] ? "+ " : "- ") + entry.costs()[i],
                    x + 9, y + 52 + 10 * i, ready[i] ? GREEN : GOLD, false);
        g.text(font, "DESBLOQUEIO", x + 9, y + 84, MUTED, false);
        g.text(font, entry.reward(), x + 9, y + 96, GREEN, false);
    }

    static void tooltip(GuiGraphicsExtractor g, Font font, Entry entry, int x, int y) {
        g.nextStratum();
        frame(g, x, y, 280, 75, CYAN, PANEL, DARK);
        g.text(font, entry.name(), x + 8, y + 7, CYAN, false);
        g.text(font, entry.description(), x + 8, y + 19, TEXT, false);
        for (int i = 0; i < entry.costs().length; i++)
            g.text(font, entry.costs()[i], x + 8, y + 31 + i * 10, GOLD, false);
        g.text(font, entry.reward(), x + 8, y + 63, GREEN, false);
    }
}
