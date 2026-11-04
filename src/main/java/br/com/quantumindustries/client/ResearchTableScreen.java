package br.com.quantumindustries.client;

import br.com.quantumindustries.menu.ResearchTableMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * F0 research-first interface. The tree is intentionally a presentation of
 * server-owned state, never a source of unlocks. Future nodes are anonymized
 * until ResearchManager can safely synchronize discoveries.
 *
 * Existing three machine slots remain visible for backwards compatibility.
 * Their legacy processing behavior will be migrated in a separate feature PR.
 */
public final class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {
    private static final int BG = 0xFF101720;
    private static final int PANEL = 0xFF1A2633;
    private static final int DEEP = 0xFF0B1018;
    private static final int BORDER = 0xFF415B70;
    private static final int CYAN = 0xFF20D6ED;
    private static final int GREEN = 0xFF64D89F;
    private static final int MUTED = 0xFF91A4B7;
    private static final int WHITE = 0xFFE5EEF4;

    private boolean knowledgeTab = false;
    private Button firstNode;

    public ResearchTableScreen(ResearchTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 320, 256);
        titleLabelX = 12;
        titleLabelY = -100;
        inventoryLabelX = 72;
        inventoryLabelY = -100;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("ARVORE DE PESQUISA"), b -> selectTab(false))
                .bounds(leftPos + 10, topPos + 27, 143, 19).build());
        addRenderableWidget(Button.builder(Component.literal("CONHECIMENTO"), b -> selectTab(true))
                .bounds(leftPos + 159, topPos + 27, 151, 19).build());
        firstNode = addRenderableWidget(Button.builder(Component.literal("Condutividade"),
                b -> { /* selection only: never unlock from the client */ })
                .bounds(leftPos + 107, topPos + 75, 95, 19).build());
        firstNode.visible = !knowledgeTab;
    }

    private void selectTab(boolean knowledge) {
        knowledgeTab = knowledge;
        if (firstNode != null) firstNode.visible = !knowledgeTab;
    }

    private void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(leftPos + x, topPos + y, leftPos + x + w, topPos + y + h, BORDER);
        g.fill(leftPos + x + 1, topPos + y + 1, leftPos + x + w - 1, topPos + y + h - 1, PANEL);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, BG);
        g.fill(leftPos + 6, topPos + 5, leftPos + 10, topPos + 23, CYAN);
        g.fill(leftPos + 6, topPos + 5, leftPos + 24, topPos + 8, CYAN);
        g.fill(leftPos + 310, topPos + 5, leftPos + 314, topPos + 23, CYAN);
        g.fill(leftPos + 296, topPos + 5, leftPos + 314, topPos + 8, CYAN);
        panel(g, 9, 50, 302, 94);
        panel(g, 9, 148, 302, 103);
        g.fill(leftPos + (knowledgeTab ? 159 : 10), topPos + 47,
               leftPos + (knowledgeTab ? 310 : 153), topPos + 49, CYAN);

        if (!knowledgeTab) {
            // Structural lines convey progression, not unrevealed names.
            g.fill(leftPos + 201, topPos + 84, leftPos + 217, topPos + 86, BORDER);
            g.fill(leftPos + 217, topPos + 65, leftPos + 219, topPos + 119, BORDER);
            g.fill(leftPos + 219, topPos + 65, leftPos + 226, topPos + 67, BORDER);
            g.fill(leftPos + 219, topPos + 117, leftPos + 226, topPos + 119, BORDER);
            // Fully anonymous future nodes: no names, icons, or tooltip.
            for (int y : new int[] {56, 107}) {
                g.fill(leftPos + 226, topPos + y, leftPos + 300, topPos + y + 19, BORDER);
                g.fill(leftPos + 227, topPos + y + 1, leftPos + 299, topPos + y + 18, DEEP);
            }
        }

        // Original machine inventory slots remain at their original menu coordinates.
        for (int[] pos : new int[][] {{21,72}, {21,108}, {66,90}}) {
            int x = pos[0], y = pos[1];
            g.fill(leftPos + x - 1, topPos + y - 1, leftPos + x + 17, topPos + y + 17, BORDER);
            g.fill(leftPos + x, topPos + y, leftPos + x + 16, topPos + y + 16, DEEP);
        }
        // Original player inventory and hotbar coordinates are unchanged.
        for (int row = 0; row < 4; row++) {
            int y = row == 3 ? 216 : 162 + 18 * row;
            for (int col = 0; col < 9; col++) {
                int x = 72 + 18 * col;
                g.fill(leftPos + x - 1, topPos + y - 1, leftPos + x + 17, topPos + y + 17, BORDER);
                g.fill(leftPos + x, topPos + y, leftPos + x + 16, topPos + y + 16, DEEP);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, "QUANTUM INDUSTRIES  /  RESEARCH BENCH", 17, 11, WHITE, false);
        if (knowledgeTab) {
            g.text(font, "BANCO DE CONHECIMENTO  /  F0", 18, 56, CYAN, false);
            g.text(font, "Condutividade:", 18, 83, MUTED, false);
            g.text(font, menu.hasConductivity() ? "DESCOBERTA" : "DESCONHECIDA",
                    121, 83, menu.hasConductivity() ? GREEN : MUTED, false);
            g.text(font, "Novas propriedades: proxima fase", 18, 112, MUTED, false);
        } else {
            g.text(font, "ERA I  /  FUNDAMENTOS", 18, 56, CYAN, false);
            g.text(font, "AMOSTRA", 15, 63, MUTED, false);
            g.text(font, "SONDA", 15, 98, MUTED, false);
            g.text(font, "SAIDA", 61, 78, MUTED, false);
            g.text(font, "?", 258, 61, MUTED, false);
            g.text(font, "?", 258, 112, MUTED, false);
            g.text(font, menu.hasConductivity() ? "CONCLUIDA" : "A DESCOBRIR",
                    113, 103, menu.hasConductivity() ? GREEN : MUTED, false);
            g.text(font, "Pesquisa inicial: Condutividade", 18, 130, MUTED, false);
        }
        g.text(font, "INVENTARIO", 18, 151, CYAN, false);
        g.text(font, "Pesquisa: " + menu.getProgress() + "/" + menu.getMaxProgress(),
                18, 235, MUTED, false);
        g.text(font, "Q-01", 274, 237, MUTED, false);
    }
}
