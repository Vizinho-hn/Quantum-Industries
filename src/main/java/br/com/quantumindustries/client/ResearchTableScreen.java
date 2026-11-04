package br.com.quantumindustries.client;

import br.com.quantumindustries.menu.ResearchTableMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Research bench interface: left side retains the existing inventory slots,
 * while the right panel switches between the analyzer and the technology tree.
 * Tree nodes are informational; only the server grants discoveries.
 */
public final class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {
    private static final int BG = 0xFF111923;
    private static final int PANEL = 0xFF1B2633;
    private static final int BORDER = 0xFF455B70;
    private static final int CYAN = 0xFF10CCE7;
    private static final int GREEN = 0xFF5BD394;
    private static final int MUTED = 0xFF8DA2B6;

    private boolean treeTab = false;
    private int selected = 0;
    private final Button[] nodes = new Button[8];

    private static final String[] TITLES = {
        "Condutividade", "Isolamento", "Engenharia",
        "Eletronica", "Geracao", "Armazenamento",
        "Processamento", "Trituracao"
    };
    private static final String[] DETAILS = {
        "Libera fio de cobre",
        "Libera cabo de cobre (futuro)",
        "Libera estrutura basica (futuro)",
        "Libera circuito basico (futuro)",
        "Libera gerador a carvao (futuro)",
        "Libera armazenamento (futuro)",
        "Libera fornalha eletrica (futuro)",
        "Libera triturador (futuro)"
    };

    public ResearchTableScreen(ResearchTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 256, 256);
        this.titleLabelX = 12;
        this.titleLabelY = 32;
        this.inventoryLabelX = 72;
        this.inventoryLabelY = 150;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("ANALISADOR"), b -> showTab(false))
                .bounds(leftPos + 8, topPos + 7, 113, 20).build());
        addRenderableWidget(Button.builder(Component.literal("PESQUISAS"), b -> showTab(true))
                .bounds(leftPos + 126, topPos + 7, 121, 20).build());

        for (int i = 0; i < nodes.length; i++) {
            final int index = i;
            int x = leftPos + 96 + (i % 2) * 76;
            int y = topPos + 47 + (i / 2) * 22;
            nodes[i] = addRenderableWidget(Button.builder(
                    Component.literal(String.format("%02d", i + 1) + " " + shortName(i)),
                    b -> selected = index
            ).bounds(x, y, 73, 19).build());
        }
        updateNodes();
    }

    private String shortName(int id) {
        return switch (id) {
            case 0 -> "Cobre";
            case 1 -> "Isolar";
            case 2 -> "Mecanica";
            case 3 -> "Circuito";
            case 4 -> "Gerador";
            case 5 -> "Bateria";
            case 6 -> "Fornalha";
            default -> "Triturar";
        };
    }

    private void showTab(boolean tree) {
        treeTab = tree;
        updateNodes();
    }

    private void updateNodes() {
        for (Button b : nodes) if (b != null) b.visible = treeTab;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, BG);
        g.fill(leftPos + 5, topPos + 31, leftPos + 251, topPos + 144, PANEL);
        g.fill(leftPos + 5, topPos + 145, leftPos + 251, topPos + 252, PANEL);
        // An accent line marks the active tab.
        int start = treeTab ? 126 : 8;
        g.fill(leftPos + start, topPos + 28, leftPos + start + (treeTab ? 121 : 113), topPos + 30, CYAN);
        if (treeTab) {
            g.fill(leftPos + 91, topPos + 40, leftPos + 92, topPos + 140, BORDER);
            g.fill(leftPos + 95, topPos + 134, leftPos + 250, topPos + 135, BORDER);
        } else {
            g.fill(leftPos + 91, topPos + 40, leftPos + 92, topPos + 140, BORDER);
            g.fill(leftPos + 99, topPos + 88, leftPos + 242, topPos + 91, BORDER);
            int width = menu.getMaxProgress() > 0
                    ? Math.min(143, 143 * menu.getProgress() / menu.getMaxProgress()) : 0;
            g.fill(leftPos + 99, topPos + 88, leftPos + 99 + width, topPos + 91, CYAN);
        }
        // Slot outlines (coordinates come from the existing ResearchTableMenu).
        for (int[] p : new int[][] {{21,72},{21,108},{66,90}}) {
            g.fill(leftPos + p[0] - 1, topPos + p[1] - 1,
                    leftPos + p[0] + 17, topPos + p[1] + 17, BORDER);
            g.fill(leftPos + p[0], topPos + p[1],
                    leftPos + p[0] + 16, topPos + p[1] + 16, BG);
        }
        // Inventory grid background.
        for (int row = 0; row < 4; row++) {
            int y = row == 3 ? 216 : 162 + row * 18;
            for (int col = 0; col < 9; col++) {
                int x = 72 + col * 18;
                g.fill(leftPos + x - 1, topPos + y - 1,
                        leftPos + x + 17, topPos + y + 17, BORDER);
                g.fill(leftPos + x, topPos + y,
                        leftPos + x + 16, topPos + y + 16, BG);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, "QUANTUM INDUSTRIES", 12, 32, CYAN, false);
        g.text(font, "AMOSTRA", 12, 59, MUTED, false);
        g.text(font, "SONDA", 12, 96, MUTED, false);
        g.text(font, "SAIDA", 54, 78, MUTED, false);
        g.text(font, "INVENTARIO", 12, 149, MUTED, false);
        if (treeTab) {
            g.text(font, "ARVORE TECNOLOGICA", 98, 34, CYAN, false);
            boolean unlocked = menu.hasConductivity();
            g.text(font, TITLES[selected], 98, 139, CYAN, false);
            g.text(font, DETAILS[selected], 8, 243, MUTED, false);
            g.text(font, selected == 0 && unlocked ? "CONCLUIDA" : "BLOQUEADA",
                    175, 139, selected == 0 && unlocked ? GREEN : MUTED, false);
        } else {
            g.text(font, "ANALISE DE COBRE", 102, 49, CYAN, false);
            g.text(font, menu.hasConductivity() ? "CONDUTIVIDADE: DESCOBERTA" :
                    "CONDUTIVIDADE: DESCONHECIDA", 102, 65,
                    menu.hasConductivity() ? GREEN : MUTED, false);
            g.text(font, menu.hasConductivity() ? "Saida: 2 fios de cobre" :
                    "Saida: dados de pesquisa", 102, 102, MUTED, false);
            g.text(font, menu.getProgress() + " / " + menu.getMaxProgress(),
                    102, 117, MUTED, false);
        }
    }
}
