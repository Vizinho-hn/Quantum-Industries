package br.com.quantumindustries.client;

import br.com.quantumindustries.menu.ResearchTableMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Research-only interface. Two real research nodes, zero inventory slots.
 * No unlocks are calculated or granted on the client.
 *
 * Layout uses Minecraft's GUI coordinates (not full resolution pixels).
 */
public final class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {
    private static final int BG = 0xFF0D141F;
    private static final int PANEL = 0xFF182431;
    private static final int DARK = 0xFF101923;
    private static final int BORDER = 0xFF455B70;
    private static final int CYAN = 0xFF17CFE8;
    private static final int GREEN = 0xFF65D998;
    private static final int MUTED = 0xFF92A9BC;
    private static final int WHITE = 0xFFF0F6FC;
    private static final int GOLD = 0xFFF2BD52;

    private int tab = 0;
    private int selected = ResearchTableMenu.RESEARCH_CONDUCTIVITY;

    public ResearchTableScreen(ResearchTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 410, 252);
        titleLabelX = 8;
        titleLabelY = -100;
        inventoryLabelX = 8;
        inventoryLabelY = -100;
    }

    private void frame(GuiGraphicsExtractor g, int x, int y, int w, int h, int border) {
        g.fill(leftPos + x, topPos + y, leftPos + x + w, topPos + y + h, border);
        g.fill(leftPos + x + 1, topPos + y + 1, leftPos + x + w - 1, topPos + y + h - 1, PANEL);
    }

    private boolean within(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + w
                && mouseY >= topPos + y && mouseY < topPos + y + h;
    }

    private boolean researched(int id) {
        return id == ResearchTableMenu.RESEARCH_CONDUCTIVITY
                ? menu.hasConductivity() : menu.hasCopperWire();
    }

    private boolean revealed(int id) {
        return menu.isRevealed(id);
    }

    private int hoverNode(int mx, int my) {
        if (within(mx, my, 148, 112, 106, 27)) return 0;
        if (revealed(1) && within(mx, my, 285, 112, 106, 27)) return 1;
        return -1;
    }

    private void node(GuiGraphicsExtractor g, int id, int x, int y, int mx, int my) {
        boolean hidden = !revealed(id);
        boolean complete = researched(id);
        boolean hovered = hoverNode(mx, my) == id;
        int accent = hidden ? BORDER : complete ? GREEN : CYAN;
        if (hovered || (!hidden && selected == id)) {
            g.fill(leftPos + x - 2, topPos + y - 2, leftPos + x + 108, topPos + y + 29, accent);
        }
        frame(g, x, y, 106, 27, accent);
        g.fill(leftPos + x + 2, topPos + y + 2, leftPos + x + 104, topPos + y + 25,
                hidden ? 0xFF060A11 : complete ? 0xFF18382C : DARK);
        if (!hidden) {
            // Accent chip hints at a future icon renderer without requiring textures.
            g.fill(leftPos + x + 5, topPos + y + 7, leftPos + x + 13, topPos + y + 18,
                    complete ? GREEN : GOLD);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, BG);
        // Technical corners and title bar.
        g.fill(leftPos + 5, topPos + 5, leftPos + 23, topPos + 7, CYAN);
        g.fill(leftPos + 5, topPos + 5, leftPos + 7, topPos + 19, CYAN);
        g.fill(leftPos + 387, topPos + 5, leftPos + 405, topPos + 7, CYAN);
        g.fill(leftPos + 403, topPos + 5, leftPos + 405, topPos + 19, CYAN);
        g.fill(leftPos + 8, topPos + 34, leftPos + 402, topPos + 35, BORDER);
        frame(g, 9, 41, 127, 23, tab == 0 ? CYAN : BORDER);
        frame(g, 141, 41, 126, 23, tab == 1 ? CYAN : BORDER);
        frame(g, 272, 41, 129, 23, tab == 2 ? CYAN : BORDER);

        frame(g, 9, 70, 121, 144, BORDER);
        frame(g, 135, 70, 266, 144, BORDER);
        frame(g, 9, 219, 392, 25, BORDER);

        if (tab == 0) {
            g.fill(leftPos + 145, topPos + 91, leftPos + 390, topPos + 92, BORDER);
            // One real dependency edge: conductivity -> wire.
            g.fill(leftPos + 254, topPos + 124, leftPos + 285, topPos + 126,
                    menu.hasConductivity() ? GREEN : BORDER);
            node(g, 0, 148, 112, mouseX, mouseY);
            node(g, 1, 285, 112, mouseX, mouseY);
            g.fill(leftPos + 144, topPos + 151, leftPos + 391, topPos + 152, BORDER);
            // Research action button is custom-drawn; server validates every click.
            boolean canResearch = revealed(selected) && !researched(selected);
            frame(g, 19, 185, 101, 21, canResearch ? CYAN : BORDER);
            g.fill(leftPos + 21, topPos + 187, leftPos + 118, topPos + 204,
                    canResearch ? 0xFF103949 : DARK);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, "QUANTUM INDUSTRIES  /  RESEARCH BENCH", 16, 16, WHITE, false);
        g.text(font, "ARVORE", 43, 49, tab == 0 ? CYAN : MUTED, false);
        g.text(font, "CONHECIMENTO", 163, 49, tab == 1 ? CYAN : MUTED, false);
        g.text(font, "RECEITAS", 311, 49, tab == 2 ? CYAN : MUTED, false);

        if (tab != 0) {
            g.text(font, tab == 1 ? "BASE DE CONHECIMENTO" : "RECEITAS DESBLOQUEADAS", 148, 82, CYAN, false);
            g.text(font, "Condutividade:", 150, 109, MUTED, false);
            g.text(font, menu.hasConductivity() ? "Concluida" : "Nao pesquisada", 251, 109,
                    menu.hasConductivity() ? GREEN : MUTED, false);
            g.text(font, "Fio de cobre:", 150, 129, MUTED, false);
            g.text(font, menu.hasCopperWire() ? "Liberado" : "Bloqueado", 251, 129,
                    menu.hasCopperWire() ? GREEN : MUTED, false);
            g.text(font, "A arvore completa sera adicionada depois.", 148, 176, MUTED, false);
        } else {
            g.text(font, "STATUS", 20, 81, CYAN, false);
            g.text(font, "Concluidas: " + ((menu.hasConductivity() ? 1 : 0) + (menu.hasCopperWire() ? 1 : 0)),
                    20, 99, GREEN, false);
            g.text(font, "Disponiveis: " + (!menu.hasConductivity() || !menu.hasCopperWire() ? 1 : 0),
                    20, 114, MUTED, false);
            g.text(font, "Ocultas: " + (menu.hasConductivity() ? 0 : 1),
                    20, 129, MUTED, false);
            g.text(font, "PESQUISA ATUAL", 20, 155, CYAN, false);
            g.text(font, selected == 0 ? "Condutividade" : "Fio de cobre", 20, 169, WHITE, false);
            g.text(font, !revealed(selected) ? "OCULTA" : researched(selected) ? "CONCLUIDA" : "DISPONIVEL",
                    20, 176, researched(selected) ? GREEN : GOLD, false);
            g.text(font, researched(selected) ? "CONCLUIDA" : "PESQUISAR", 37, 193,
                    researched(selected) ? MUTED : WHITE, false);

            g.text(font, "ERA I  /  FUNDAMENTOS", 148, 81, CYAN, false);
            g.text(font, "Condutividade", 166, 122, WHITE, false);
            g.text(font, menu.hasConductivity() ? "Fio de cobre" : "?", 306, 122, menu.hasConductivity() ? WHITE : MUTED, false);

            int focused = hoverNode(mouseX, mouseY);
            if (focused < 0) focused = selected;
            if (revealed(focused)) {
                g.text(font, focused == 0 ? "CONDUTIVIDADE" : "FIO DE COBRE", 148, 158, CYAN, false);
                g.text(font, focused == 0 ? "Estudar principios de conducao." : "Fabricar fios a partir de cobre.",
                        148, 170, WHITE, false);
                g.text(font, focused == 0 ? "Custo: 3 cobre, 1 redstone, 1 papel" : "Requer condutividade concluida.",
                        148, 184, MUTED, false);
                g.text(font, focused == 0 ? "Desbloqueia: pesquisa de fios" : "Custo: 2 cobre, 1 papel",
                        148, 199, GREEN, false);
            }
        }
        g.text(font, "PESQUISAS: " + ((menu.hasConductivity() ? 1 : 0) + (menu.hasCopperWire() ? 1 : 0))
                + " / 2", 19, 227, CYAN, false);
        g.text(font, "TECNOLOGIA: ERA I", 284, 227, MUTED, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mx = event.x(), my = event.y();
            if (within(mx, my, 9, 41, 127, 23)) { tab = 0; return true; }
            if (within(mx, my, 141, 41, 126, 23)) { tab = 1; return true; }
            if (within(mx, my, 272, 41, 129, 23)) { tab = 2; return true; }
            if (tab == 0) {
                int node = hoverNode((int) mx, (int) my);
                if (node >= 0) { selected = node; return true; }
                if (within(mx, my, 19, 185, 101, 21) && revealed(selected) && !researched(selected)) {
                    if (Minecraft.getInstance().gameMode != null) {
                        Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, selected);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
