package br.com.quantumindustries.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import static br.com.quantumindustries.client.ResearchTheme.*;

final class ResearchTreeView {
    static final int NODE_Y = 163;
    static final int NODE_WIDTH = 151;
    static final int NODE_HEIGHT = 35;

    enum State {
        HIDDEN("Oculta", LINE), WAITING("Aguardando materiais", GOLD),
        READY("Disponível", CYAN), DONE("Concluída", GREEN);

        final String label;
        final int color;
        State(String label, int color) { this.label = label; this.color = color; }
    }

    static int nodeX(int id) { return 179 + id * 207; }

    static int hit(double x, double y) {
        if (y < NODE_Y || y >= NODE_Y + NODE_HEIGHT) return -1;
        for (int id = 0; id < 2; id++)
            if (x >= nodeX(id) && x < nodeX(id) + NODE_WIDTH) return id;
        return -1;
    }

    static void draw(GuiGraphicsExtractor g, Font font, State[] states, int selected, int hovered,
                     String[] names, ItemStack[] icons) {
        for (int y = 144; y < 317; y += 12)
            for (int x = 152 + (y / 12 % 2) * 6; x < 552; x += 12)
                g.fill(x, y, x + 3, y + 3, 0xFF0D1F2C);

        int edge = states[0] == State.DONE ? GREEN : LINE;
        g.fill(330, 180, 386, 182, edge);
        g.fill(352, 177, 359, 185, edge);

        for (int id = 0; id < 2; id++) {
            int x = nodeX(id), y = NODE_Y;
            State state = states[id];
            boolean active = state != State.HIDDEN && (selected == id || hovered == id);
            if (active) frame(g, x - 2, y - 2, NODE_WIDTH + 4, NODE_HEIGHT + 4,
                    0xFF176178, DARK, DARK);
            frame(g, x, y, NODE_WIDTH, NODE_HEIGHT, active ? CYAN : state.color,
                    state == State.HIDDEN ? 0xFF060A10 : PANEL, DARK);
            if (state == State.HIDDEN) {
                lock(g, x + 70, y + 11);
            } else {
                g.fakeItem(icons[id], x + 9, y + 9);
                g.text(font, names[id], x + 34, y + 7, TEXT, false);
                g.text(font, state.label, x + 34, y + 20, state.color, false);
            }
        }
    }
}
