package br.com.quantumindustries.client;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.menu.ResearchTableMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import static br.com.quantumindustries.client.ResearchTheme.*;
import static br.com.quantumindustries.client.ResearchTreeView.State;

public final class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {
    private static final int W = 576, H = 432;
    private static final int ACTION_X = 16, ACTION_Y = 295, ACTION_W = 118, ACTION_H = 24;
    private static final String[] TABS = {"Árvore de Pesquisa", "Base de Conhecimento", "Receitas Desbloqueadas", "Relatório de pesquisa"};
    private static final ResearchDetailsPanel.Entry[] ENTRIES = {
            new ResearchDetailsPanel.Entry("Condutividade", "Estudo dos princípios básicos de condução elétrica.",
                    "Conhecimento de condutividade e pesquisa Fio de Cobre.",
                    new String[]{"3 lingotes de cobre", "1 redstone", "1 papel"}),
            new ResearchDetailsPanel.Entry("Fio de Cobre", "Desbloqueia a fabricação de fios de cobre.",
                    "Receita: 3 lingotes em linha → 6 fios.",
                    new String[]{"Condutividade concluída", "2 lingotes de cobre", "1 papel"})
    };
    private final Inventory inventory;
    private int tab;
    private int selected;
    private int selectedReport = -1;

    public ResearchTableScreen(ResearchTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
        this.inventory = inventory;
    }

    private float scale() { return Math.min((width - 8f) / W, (height - 8f) / H); }
    private float originX() { return (width - W * scale()) / 2; }
    private float originY() { return (height - H * scale()) / 2; }

    private boolean complete(int id) { return id == 0 ? menu.hasConductivity() : menu.hasCopperWire(); }

    private boolean[] requirements(int id) {
        return id == 0 ? new boolean[]{menu.copper() >= 3, menu.redstone() >= 1, menu.paper() >= 1}
                : new boolean[]{menu.hasConductivity(), menu.copper() >= 2, menu.paper() >= 1};
    }

    private State state(int id) {
        if (!menu.isRevealed(id)) return State.HIDDEN;
        if (complete(id)) return State.DONE;
        for (boolean ready : requirements(id)) if (!ready) return State.WAITING;
        return State.READY;
    }

    private void text(GuiGraphicsExtractor g, String value, int x, int y, int color) {
        g.text(font, value, x, y, color, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int x = (int) Math.floor((mouseX - originX()) / scale());
        int y = (int) Math.floor((mouseY - originY()) / scale());
        int hover = tab == 0 ? ResearchTreeView.hit(x, y) : -1;
        State[] states = {state(0), state(1)};
        ItemStack[] icons = {new ItemStack(Items.REDSTONE), new ItemStack(ModItems.COPPER_WIRE.get())};
        g.pose().pushMatrix();
        try {
            g.pose().translate(originX(), originY()).scale(scale());
            g.fill(0, 0, W, H, 0xFF07111C);
            header(g);
            frame(g, 4, 62, 568, 330, LINE, PANEL, DARK);
            frame(g, 9, 67, 132, 260, LINE, PANEL, DARK);
            frame(g, 146, 67, 421, 260, LINE, DARK, DARK);
            sidebar(g, states);
            if (tab == 0) {
                text(g, "ÁRVORE DE PESQUISA  —  Era I: Fundamentos", 155, 77, TEXT);
                eras(g);
                ResearchTreeView.draw(g, font, states, selected, hover,
                        new String[]{ENTRIES[0].name(), ENTRIES[1].name()}, icons);
                ResearchDetailsPanel.draw(g, font, ENTRIES[selected], 241, 211, 316,
                        complete(selected) ? new boolean[]{true, true, true} : requirements(selected));
            } else if (tab == 3) {
                report(g);
            } else {
                journal(g);
            }
            footer(g, states[selected], icons[selected]);
            if (hover >= 0 && states[hover] != State.HIDDEN) {
                ResearchDetailsPanel.tooltip(g, font, ENTRIES[hover], Math.min(x + 10, W - 284),
                        Math.min(y + 12, H - 79));
            }
        } finally {
            g.pose().popMatrix();
        }
    }

    private void header(GuiGraphicsExtractor g) {
        frame(g, 4, 4, 568, 31, LINE, PANEL, DARK);
        g.fill(4, 4, 16, 5, CYAN);
        g.fill(4, 4, 5, 15, CYAN);
        g.fill(14, 16, 21, 23, CYAN);
        g.fill(24, 23, 31, 30, CYAN);
        g.fill(24, 12, 31, 19, GOLD);
        text(g, "QUANTUM INDUSTRIES", 39, 9, MUTED);
        text(g, "Research Bench", 39, 21, TEXT);
        g.text(font, ModItems.SCIENTIFIC_ANALYZER.get().getName(new ItemStack(ModItems.SCIENTIFIC_ANALYZER.get())), 228, 16, TEXT, false);
        for (int i = 0; i < TABS.length; i++) {
            int x = 9 + i * 140;
            frame(g, x, 39, 136, 23, i == tab ? CYAN : LINE, i == tab ? 0xFF145368 : PANEL, DARK);
            text(g, TABS[i], x + 10, 46, i == tab ? TEXT : MUTED);
        }
    }

    private void sidebar(GuiGraphicsExtractor g, State[] states) {
        text(g, "STATUS DA PESQUISA", 17, 77, MUTED);
        g.fill(17, 89, 133, 90, LINE);
        State[] order = {State.DONE, State.READY, State.WAITING, State.HIDDEN};
        String[] labels = {"Concluídas", "Disponíveis", "Aguardando", "Ocultas"};
        for (int i = 0; i < order.length; i++) {
            int count = 0;
            for (State state : states) if (state == order[i]) count++;
            int y = 97 + i * 16;
            g.fill(18, y + 1, 23, y + 6, order[i].color);
            text(g, labels[i], 29, y, TEXT);
            text(g, Integer.toString(count), 120, y, order[i].color);
        }
        g.fill(17, 163, 133, 164, LINE);
        text(g, "PESQUISA SELECIONADA", 17, 173, MUTED);
        text(g, ENTRIES[selected].name(), 17, 192, TEXT);
        text(g, states[selected].label, 17, 207, states[selected].color);
        g.fill(17, 223, 133, 224, LINE);
        text(g, "DESBLOQUEIO", 17, 234, MUTED);
        text(g, selected == 0 ? "Pesquisa Fio de Cobre" : "Receita de Fio de Cobre", 17, 249, TEXT);
        if (tab == 0) {
            sidebarAction(g, complete(selected) ? "Concluída" : "Iniciar pesquisa",
                    states[selected] == State.READY);
        } else if (tab == 3) {
            sidebarAction(g, "Escrever relatório", selectedReport >= 0 && canWrite(selectedReport));
        }
    }

    private void sidebarAction(GuiGraphicsExtractor g, String label, boolean ready) {
        frame(g, ACTION_X, ACTION_Y, ACTION_W, ACTION_H,
                ready ? CYAN : LINE, ready ? 0xFF145368 : PANEL, DARK);
        text(g, label, ACTION_X + 9, ACTION_Y + 8, ready ? TEXT : MUTED);
    }

    private static boolean hitsSidebarAction(double x, double y) {
        return x >= ACTION_X && x < ACTION_X + ACTION_W
                && y >= ACTION_Y && y < ACTION_Y + ACTION_H;
    }

    private void eras(GuiGraphicsExtractor g) {
        String[] eras = {"Fundamentos", "Automação", "Energia", "Processamento", "Quântica"};
        for (int i = 0; i < eras.length; i++) {
            int x = 153 + i * 81;
            frame(g, x, 104, 79, 30, i == 0 ? CYAN : LINE, i == 0 ? 0xFF145368 : PANEL, DARK);
            text(g, "Era " + new String[]{"I", "II", "III", "IV", "V"}[i], x + 6, 109, TEXT);
            text(g, eras[i], x + 6, 121, MUTED);
            if (i > 0) lock(g, x + 59, 106);
        }
    }

    private void report(GuiGraphicsExtractor g) {
        text(g, "RELATÓRIO DE PESQUISA", 155, 77, CYAN);
        text(g, "Guarde 1 tinta preta, 1 pena e 1 papel por relatório.", 155, 94, TEXT);
        text(g, "Os materiais ficam na bancada quando você sai.", 155, 108, MUTED);
        String[] names = {"Tinta preta", "Pena", "Papel"};
        ItemStack[] icons = {new ItemStack(Items.BLACK_DYE), new ItemStack(Items.FEATHER), new ItemStack(Items.PAPER)};
        for (int i = 0; i < names.length; i++) {
            int x = 155 + i * 135;
            frame(g, x, 125, 129, 75, LINE, PANEL, DARK);
            g.fakeItem(icons[i], x + 8, 132);
            text(g, names[i] + " × " + menu.supply(i), x + 29, 136, TEXT);
            reportButton(g, x + 7, 153, 115, "Guardar", true);
            reportButton(g, x + 7, 175, 115, "Retirar", menu.supply(i) > 0);
        }
        text(g, "SELECIONE UMA PESQUISA CONCLUÍDA", 155, 211, MUTED);
        for (int id = 0; id < ENTRIES.length; id++) {
            int x = 155 + id * 204;
            boolean active = selectedReport == id;
            frame(g, x, 225, 193, 19, active ? CYAN : LINE,
                    active ? 0xFF145368 : PANEL, DARK);
            text(g, ENTRIES[id].name(), x + 8, 231, complete(id) ? TEXT : MUTED);
        }
        text(g, "Entregue o relatório a outro jogador.", 155, 265, TEXT);
        text(g, "Botão direito para aprender. Fio de Cobre exige Condutividade.", 155, 279, MUTED);
    }

    private boolean canWrite(int id) {
        return complete(id) && menu.supply(0) > 0 && menu.supply(1) > 0 && menu.supply(2) > 0;
    }

    private void reportButton(GuiGraphicsExtractor g, int x, int y, int w, String label, boolean enabled) {
        frame(g, x, y, w, 19, enabled ? CYAN : LINE, enabled ? 0xFF145368 : PANEL, DARK);
        text(g, label, x + 6, y + 6, enabled ? TEXT : MUTED);
    }

    private boolean clickReport(double x, double y) {
        int action = -1;
        for (int i = 0; i < 3; i++) {
            int start = 162 + i * 135;
            if (x < start || x >= start + 115) continue;
            if (y >= 153 && y < 172) action = ResearchTableMenu.REPORT_DEPOSIT + i;
            if (y >= 175 && y < 194) action = ResearchTableMenu.REPORT_WITHDRAW + i;
        }
        if (y >= 225 && y < 244) {
            int id = x >= 155 && x < 348 ? 0 : x >= 359 && x < 552 ? 1 : -1;
            if (id >= 0 && complete(id)) {
                selectedReport = id;
                selected = id;
                return true;
            }
        }
        if (hitsSidebarAction(x, y)
                && selectedReport >= 0 && canWrite(selectedReport)) {
            action = ResearchTableMenu.REPORT_WRITE + selectedReport;
        }
        if (action < 0) return false;
        var mode = Minecraft.getInstance().gameMode;
        if (mode != null) mode.handleInventoryButtonClick(menu.containerId, action);
        return true;
    }

    private void journal(GuiGraphicsExtractor g) {
        text(g, TABS[tab].toUpperCase(java.util.Locale.ROOT), 155, 77, CYAN);
        if (tab == 2 && menu.hasCopperWire()) {
            frame(g, 155, 100, 401, 46, CYAN, PANEL, DARK);
            for (int i = 0; i < 3; i++) g.fakeItem(new ItemStack(Items.COPPER_INGOT), 168 + i * 20, 112);
            text(g, "→", 231, 116, CYAN);
            g.fakeItem(new ItemStack(ModItems.COPPER_WIRE.get(), 6), 247, 112);
            text(g, "6 fios de cobre", 273, 116, TEXT);
        } else if (tab == 1 && menu.hasConductivity()) {
            text(g, ENTRIES[0].description(), 157, 112, TEXT);
            if (menu.hasCopperWire()) text(g, ENTRIES[1].description(), 157, 133, GREEN);
        } else {
            text(g, tab == 1 ? "Nenhuma pesquisa concluída." : "Nenhuma receita desbloqueada.",
                    157, 112, MUTED);
        }
    }

    private void footer(GuiGraphicsExtractor g, State state, ItemStack icon) {
        frame(g, 9, 332, 558, 55, LINE, PANEL, DARK);
        g.fakeItem(icon, 19, 348);
        text(g, ENTRIES[selected].name(), 43, 345, TEXT);
        text(g, "Era I / Fundamentos", 43, 360, MUTED);
        g.fill(156, 340, 157, 380, LINE);
        g.fill(316, 340, 317, 380, LINE);
        text(g, "REQUISITOS", 165, 340, MUTED);
        boolean[] ready = requirements(selected);
        for (int i = 0; i < ready.length; i++) {
            boolean met = state == State.DONE || ready[i];
            text(g, (met ? "+ " : "- ") + ENTRIES[selected].costs()[i],
                    165, 351 + i * 10, met ? GREEN : GOLD);
        }
        text(g, "PROGRESSO DA PESQUISA", 327, 340, MUTED);
        for (int i = 0; i < 16; i++) {
            int x = 327 + i * 10;
            g.fill(x, 354, x + 8, 362, state == State.DONE ? CYAN : LINE);
            if (state != State.DONE) g.fill(x + 1, 355, x + 7, 361, DARK);
        }
        text(g, state == State.DONE ? "100%" : "0%", 500, 354, TEXT);
        text(g, state.label, 327, 369, state.color);

        frame(g, 4, 396, 568, 32, LINE, PANEL, DARK);
        text(g, "INVENTÁRIO DO OPERADOR", 15, 408, MUTED);
        for (int i = 0; i < 9; i++) {
            int x = 178 + i * 25;
            frame(g, x, 401, 23, 23, LINE, DARK, DARK);
            g.item(inventory.getItem(i), x + 3, 404);
        }
        text(g, "LAB / 04", 512, 408, MUTED);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {}

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double x = (event.x() - originX()) / scale(), y = (event.y() - originY()) / scale();
            for (int i = 0; i < TABS.length; i++) {
                int start = 9 + i * 140;
                if (y >= 39 && y < 62 && x >= start && x < start + 136) {
                    tab = i;
                    return true;
                }
            }
            if (tab == 3 && clickReport(x, y)) return true;
            if (tab == 0) {
                int id = ResearchTreeView.hit(x, y);
                if (id >= 0 && menu.isRevealed(id)) {
                    selected = id;
                    return true;
                }
                if (hitsSidebarAction(x, y) && state(selected) == State.READY) {
                    var gameMode = Minecraft.getInstance().gameMode;
                    if (gameMode != null) gameMode.handleInventoryButtonClick(menu.containerId, selected);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}





