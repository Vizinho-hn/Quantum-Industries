package br.com.quantumindustries.client;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.menu.QuantumGuidebookMenu;
import br.com.quantumindustries.research.GuidebookCatalog;
import br.com.quantumindustries.research.GuidebookCatalog.Chapter;
import br.com.quantumindustries.research.ResearchCatalog;
import java.util.List;
import java.util.ArrayList;
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
    private final String[] FILTER_LABELS = {tr("gui.quantumindustries.text.all"), tr("gui.quantumindustries.guidebook.filter.intro"), tr("gui.quantumindustries.text.era_i"), tr("gui.quantumindustries.text.era_ii")};

    private final ResearchTheme theme = new ResearchTheme(false);
    private int eraFilter = -1; // -1 = All, 0 = Intro, 1 = Era I, 2 = Era II
    private int listPage = 0;
    private int selectedIndex = 0;
    private int readingPage = 0;
    private static final int READING_LINES_PER_PAGE = 12;
    private static final int READING_LINE_HEIGHT = 11;

    public QuantumGuidebookScreen(QuantumGuidebookMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
    }

    private static String tr(String key, Object... args) { return Component.translatable(key, args).getString(); }

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
            text(g, tr("gui.quantumindustries.text.quantum_industries_codex"), 34, 10, CYAN);
            text(g, tr("gui.quantumindustries.text.machine_operation_manual_no_recipes"), 34, 22, MUTED);

            String progressLabel = tr("gui.quantumindustries.text.decoded") + unlockedCount + " / " + GuidebookCatalog.chapters().size();
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
            text(g, tr("gui.quantumindustries.text.machine_index"), 16, 73, MUTED);
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
                    clipped(g, tr(ch.title()), 36, ry + 9, 142, selected ? CYAN : TEXT);
                } else {
                    theme.lock(g, 19, ry + 7);
                    text(g, "????", 36, ry + 9, GOLD);
                    text(g, tr("gui.quantumindustries.text.encrypted"), 74, ry + 9, MUTED);
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
        clipped(g, tr(chapter.title()), 234, 79, 284, CYAN);
        clipped(g, tr(chapter.subtitle()), 234, 92, 284, MUTED);
        String eraTag = chapter.era() == 0 ? tr("gui.quantumindustries.text.introduction") : tr("gui.quantumindustries.guidebook.era_unlocked", chapter.era() == 1 ? "I" : "II");
        text(g, eraTag, 234, 103, GREEN);

        List<FormattedCharSequence> lines = chapterLines(chapter);
        int pages = readingPageCount(lines.size());
        readingPage = Math.clamp(readingPage, 0, pages - 1);
        int start = readingPage * READING_LINES_PER_PAGE;
        int end = Math.min(lines.size(), start + READING_LINES_PER_PAGE);
        for (int i = start; i < end; i++) {
            g.text(font, lines.get(i), 208, 124 + (i - start) * READING_LINE_HEIGHT, TEXT, false);
        }
        if (pages > 1) {
            theme.frame(g, 208, 260, 32, 20, readingPage > 0 ? CYAN : LINE, PANEL, DARK);
            text(g, tr("gui.quantumindustries.guidebook.previous_page"), 220, 266, TEXT);
            text(g, tr("gui.quantumindustries.guidebook.reading_page", readingPage + 1, pages), 250, 266, MUTED);
            theme.frame(g, 490, 260, 32, 20, readingPage + 1 < pages ? CYAN : LINE, PANEL, DARK);
            text(g, tr("gui.quantumindustries.guidebook.next_page"), 502, 266, TEXT);
        }

        // Practical operation box at bottom
        theme.frame(g, 204, 284, 322, 72, CYAN, PANEL, DARK);
        text(g, tr("gui.quantumindustries.text.how_to_operate"), 214, 292, GOLD);
        List<FormattedCharSequence> opLines = font.split(Component.literal(tr(chapter.operationSummary())), 302);
        int oy = 306;
        for (FormattedCharSequence line : opLines) {
            if (oy > 344) break;
            g.text(font, line, 214, oy, TEXT, false);
            oy += 11;
        }
    }

    private List<FormattedCharSequence> chapterLines(Chapter chapter) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (String paragraph : chapter.paragraphs()) {
            lines.addAll(font.split(Component.translatable(paragraph), 310));
            lines.add(FormattedCharSequence.EMPTY);
        }
        return lines;
    }

    private static int readingPageCount(int lineCount) {
        return Math.max(1, (lineCount + READING_LINES_PER_PAGE - 1) / READING_LINES_PER_PAGE);
    }

    private void renderLockedChapter(GuiGraphicsExtractor g, Chapter chapter) {
        theme.frame(g, 204, 72, 322, 42, LINE, PANEL, DARK);
        theme.lock(g, 216, 86);
        text(g, tr("gui.quantumindustries.text.encrypted_record"), 236, 81, GOLD);
        text(g, tr("gui.quantumindustries.text.data_locked_by_the_vault_security_protocol"), 236, 95, MUTED);

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

        String missingStudy = chapter.requiredStudies().stream()
                .filter(key -> !menu.knows(ResearchCatalog.indexOf(key)))
                .findFirst().orElse(chapter.studyKey());
        var study = ResearchCatalog.find(missingStudy);
        String studyHint = study != null ? tr(study.name()) : "????";

        theme.frame(g, 204, 260, 322, 96, GOLD, PANEL, DARK);
        text(g, tr("gui.quantumindustries.text.how_to_unlock_this_chapter"), 214, 270, GOLD);
        String msg = tr("gui.quantumindustries.text.you_have_not_yet_learned_the_science_behind_this_machine_build_and_use_the_research_table_and_complete_the_corresponding_study_to_decode_its_full_operating_instructions");
        List<FormattedCharSequence> lines = font.split(Component.literal(msg), 302);
        int hy = 285;
        for (FormattedCharSequence line : lines) {
            g.text(font, line, 214, hy, TEXT, false);
            hy += 11;
        }
        clipped(g, tr("gui.quantumindustries.guidebook.required_study", studyHint, (study != null ? study.era() : chapter.era()) == 1 ? "I" : "II"), 214, 338, 302, CYAN);
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
                        readingPage = 0;
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
                        if (selectedIndex != i) readingPage = 0;
                        selectedIndex = i;
                        return true;
                    }
                }
            }

            Chapter current = chapters.get(selectedIndex);
            if (menu.isUnlocked(current) && y >= 260 && y < 280) {
                int pages = readingPageCount(chapterLines(current).size());
                if (x >= 208 && x < 240 && readingPage > 0) {
                    readingPage--;
                    return true;
                }
                if (x >= 490 && x < 522 && readingPage + 1 < pages) {
                    readingPage++;
                    return true;
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
