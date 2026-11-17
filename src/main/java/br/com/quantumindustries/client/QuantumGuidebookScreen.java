package br.com.quantumindustries.client;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.menu.QuantumGuidebookMenu;
import br.com.quantumindustries.research.GuidebookCatalog;
import br.com.quantumindustries.research.GuidebookCatalog.Chapter;
import br.com.quantumindustries.research.ResearchCatalog;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import static br.com.quantumindustries.client.ResearchTheme.*;

public final class QuantumGuidebookScreen extends AbstractContainerScreen<QuantumGuidebookMenu> {
    private static final int W = 540, H = 372;
    private static final int ROWS_PER_PAGE = 8;
    private static final String[] FILTER_LABELS = {"Todos", "Início", "Era I", "Era II"};

    private final ResearchTheme theme = new ResearchTheme(false);
    private int eraFilter = -1; // -1 = All, 0 = Intro, 1 = Era I, 2 = Era II
    private int listPage = 0;
    private int selectedIndex = 0;

    public QuantumGuidebookScreen(QuantumGuidebookMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
    }

    private float scale() { return Math.min((width - 8f) / W, (height - 8f) / H); }
    private float originX() { return (width - W * scale()) / 2f; }
    private float originY() { return (height - H * scale()) / 2f; }

    private List<Chapter> filteredChapters() {
        return GuidebookCatalog.chapters().stream()
                .filter(c -> eraFilter < 0 || c.era() == eraFilter)
                .toList();
    }

    private void text(GuiGraphicsExtractor g, String value, int x, int y, int color) {
        theme.text(g, font, value, x, y, color, false);
    }

    private void clipped(GuiGraphicsExtractor g, String label, int x, int y, int max, int color) {
        if (font.width(label) > max) label = font.plainSubstrByWidth(label, max - 8) + "…";
        text(g, label, x, y, color);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        List<Chapter> chapters = filteredChapters();
        if (chapters.isEmpty()) {
            eraFilter = -1;
            chapters = filteredChapters();
        }
        selectedIndex = Math.clamp(selectedIndex, 0, chapters.size() - 1);
        int pageCount = Math.max(1, (chapters.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        listPage = Math.clamp(listPage, 0, pageCount - 1);

        int unlockedCount = 0;
        for (Chapter c : GuidebookCatalog.chapters()) {
            if (menu.isUnlocked(c)) unlockedCount++;
        }

        g.pose().pushMatrix();
        try {
            g.pose().translate(originX(), originY()).scale(scale());
            theme.fill(g, 0, 0, W, H, 0xFF07111C);

            // Header
            theme.frame(g, 4, 4, W - 8, 32, LINE, PANEL, DARK);
            g.fakeItem(new ItemStack(ModItems.QUANTUM_GUIDEBOOK.get()), 12, 12);
            text(g, "CÓDICE QUANTUM INDUSTRIES", 34, 10, CYAN);
            text(g, "Manual Técnico de Máquinas & Operação (Sem Receitas)", 34, 22, MUTED);

            String progressLabel = "Decodificados: " + unlockedCount + " / " + GuidebookCatalog.chapters().size();
            text(g, progressLabel, W - 16 - font.width(progressLabel), 16, GREEN);

            // Filter tabs
            for (int i = 0; i < FILTER_LABELS.length; i++) {
                int targetFilter = i - 1;
                boolean active = eraFilter == targetFilter;
                int tx = 8 + i * 74;
                theme.frame(g, tx, 40, 70, 20, active ? CYAN : LINE, active ? 0xFF145368 : PANEL, DARK);
                text(g, FILTER_LABELS[i], tx + 10, 46, active ? TEXT : MUTED);
            }

            // Left sidebar (Chapter list)
            theme.frame(g, 6, 64, 184, 300, LINE, PANEL, DARK);
            text(g, "ÍNDICE DE MÁQUINAS", 16, 73, MUTED);
            theme.fill(g, 14, 84, 182, 85, LINE);

            int start = listPage * ROWS_PER_PAGE;
            int end = Math.min(start + ROWS_PER_PAGE, chapters.size());
            for (int i = start; i < end; i++) {
                int row = i - start;
                int ry = 90 + row * 29;
                Chapter ch = chapters.get(i);
                boolean unlocked = menu.isUnlocked(ch);
                boolean selected = i == selectedIndex;

                theme.frame(g, 12, ry, 172, 26, selected ? CYAN : LINE, selected ? 0xFF145368 : DARK, DARK);
                if (unlocked) {
                    g.fakeItem(ch.icon().get(), 16, ry + 5);
                    clipped(g, ch.title(), 36, ry + 9, 142, selected ? CYAN : TEXT);
                } else {
                    theme.lock(g, 19, ry + 7);
                    text(g, "????", 36, ry + 9, GOLD);
                    text(g, "[Criptografado]", 74, ry + 9, MUTED);
                }
            }

            // Pagination controls at bottom of sidebar
            theme.frame(g, 14, 334, 32, 20, listPage > 0 ? CYAN : LINE, listPage > 0 ? 0xFF145368 : PANEL, DARK);
            text(g, "<", 27, 340, listPage > 0 ? TEXT : MUTED);
            String pageStr = (listPage + 1) + " / " + pageCount;
            text(g, pageStr, 98 - font.width(pageStr) / 2, 340, MUTED);
            boolean hasNext = listPage + 1 < pageCount;
            theme.frame(g, 150, 334, 32, 20, hasNext ? CYAN : LINE, hasNext ? 0xFF145368 : PANEL, DARK);
            text(g, ">", 163, 340, hasNext ? TEXT : MUTED);

            // Right reading panel
            theme.frame(g, 196, 64, 338, 300, LINE, DARK, DARK);
            Chapter current = chapters.get(selectedIndex);
            if (menu.isUnlocked(current)) {
                renderUnlockedChapter(g, current);
            } else {
                renderLockedChapter(g, current);
            }
        } finally {
            g.pose().popMatrix();
        }
    }

    private void renderUnlockedChapter(GuiGraphicsExtractor g, Chapter chapter) {
        theme.frame(g, 204, 72, 322, 42, LINE, PANEL, DARK);
        g.fakeItem(chapter.icon().get(), 212, 83);
        clipped(g, chapter.title(), 234, 79, 284, CYAN);
        clipped(g, chapter.subtitle(), 234, 92, 284, MUTED);
        String eraTag = chapter.era() == 0 ? "INTRODUÇÃO" : "ERA " + (chapter.era() == 1 ? "I" : "II") + " — DESBLOQUEADO";
        text(g, eraTag, 234, 103, GREEN);

        int y = 124;
        for (String paragraph : chapter.paragraphs()) {
            List<FormattedCharSequence> lines = font.split(Component.literal(paragraph), 310);
            for (FormattedCharSequence line : lines) {
                if (y > 272) break;
                g.text(font, line, 208, y, TEXT, false);
                y += 11;
            }
            y += 6;
        }

        // Practical operation box at bottom
        theme.frame(g, 204, 284, 322, 72, CYAN, PANEL, DARK);
        text(g, "COMO OPERAR NA PRÁTICA", 214, 292, GOLD);
        List<FormattedCharSequence> opLines = font.split(Component.literal(chapter.operationSummary()), 302);
        int oy = 306;
        for (FormattedCharSequence line : opLines) {
            if (oy > 344) break;
            g.text(font, line, 214, oy, TEXT, false);
            oy += 11;
        }
    }

    private void renderLockedChapter(GuiGraphicsExtractor g, Chapter chapter) {
        theme.frame(g, 204, 72, 322, 42, LINE, PANEL, DARK);
        theme.lock(g, 216, 86);
        text(g, "REGISTRO CRIPTOGRAFADO — ????", 236, 81, GOLD);
        text(g, "Dados bloqueados pelo protocolo de segurança do Vault", 236, 95, MUTED);

        String[] scrambled = {
            "???? ???????? ?????? ???? ???????????? ??????",
            "?????? ???? ?????????? ???????? ???? ????????",
            "?????????? ?????? ???? ???????? ?????????? ??",
            "",
            "???? ?????? ???????????? ???? ???????? ??????",
            "???????? ???? ?????? ?????????? ???? ????????",
            "?????? ?????????? ???? ?????? ???????? ??????"
        };
        int y = 130;
        for (String line : scrambled) {
            if (!line.isEmpty()) {
                text(g, line, 212, y, MUTED);
            }
            y += 13;
        }

        var study = ResearchCatalog.find(chapter.studyKey());
        String studyHint = study != null ? study.name() : "????";

        theme.frame(g, 204, 260, 322, 96, GOLD, PANEL, DARK);
        text(g, "COMO DESBLOQUEAR ESTE CAPÍTULO", 214, 270, GOLD);
        String msg = "Você ainda não compreende a ciência por trás desta máquina. Construa e utilize a Mesa de Pesquisa e conclua o estudo correspondente para decodificar as instruções completas de operação.";
        List<FormattedCharSequence> lines = font.split(Component.literal(msg), 302);
        int hy = 285;
        for (FormattedCharSequence line : lines) {
            g.text(font, line, 214, hy, TEXT, false);
            hy += 11;
        }
        clipped(g, "Estudo necessário na Mesa: " + studyHint + " (Era " + (chapter.era() == 1 ? "I" : "II") + ")", 214, 338, 302, CYAN);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {}

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double x = (event.x() - originX()) / scale();
            double y = (event.y() - originY()) / scale();

            // Filter tabs
            if (y >= 40 && y < 60) {
                for (int i = 0; i < FILTER_LABELS.length; i++) {
                    int tx = 8 + i * 74;
                    if (x >= tx && x < tx + 70) {
                        eraFilter = i - 1;
                        selectedIndex = 0;
                        listPage = 0;
                        return true;
                    }
                }
            }

            List<Chapter> chapters = filteredChapters();
            int pageCount = Math.max(1, (chapters.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);

            // Chapter rows
            int start = listPage * ROWS_PER_PAGE;
            int end = Math.min(start + ROWS_PER_PAGE, chapters.size());
            if (x >= 12 && x < 184) {
                for (int i = start; i < end; i++) {
                    int ry = 90 + (i - start) * 29;
                    if (y >= ry && y < ry + 26) {
                        selectedIndex = i;
                        return true;
                    }
                }
            }

            // Pagination buttons
            if (y >= 334 && y < 354) {
                if (x >= 14 && x < 46 && listPage > 0) {
                    listPage--;
                    return true;
                }
                if (x >= 150 && x < 182 && listPage + 1 < pageCount) {
                    listPage++;
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        List<Chapter> chapters = filteredChapters();
        int pageCount = Math.max(1, (chapters.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        if (vertical < 0 && listPage + 1 < pageCount) {
            listPage++;
            return true;
        } else if (vertical > 0 && listPage > 0) {
            listPage--;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }
}
