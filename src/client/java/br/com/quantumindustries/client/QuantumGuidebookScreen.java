package br.com.quantumindustries.client;

import br.com.quantumindustries.client.codex.CodexJeiBridge;
import br.com.quantumindustries.client.codex.CodexMachinePreviewRenderer;
import br.com.quantumindustries.client.codex.CodexMachinePreviewState;
import br.com.quantumindustries.client.codex.CodexTheme;
import br.com.quantumindustries.menu.QuantumGuidebookMenu;
import br.com.quantumindustries.research.GuidebookCatalog;
import br.com.quantumindustries.research.GuidebookCatalog.Chapter;
import br.com.quantumindustries.research.ResearchCatalog;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import static br.com.quantumindustries.client.codex.CodexTheme.*;

/** Era V Quantum Codex tablet interface with real 3D machine previews and server-synced objectives. */
public final class QuantumGuidebookScreen extends AbstractContainerScreen<QuantumGuidebookMenu> {
    // 1344x752 reference scaled cleanly 1:2 to 672x376 virtual pixels, preserving exact aspect ratio.
    private static final int W = 672, H = 376;
    private static final int HEADER_H = 34;
    private static final int FOOTER_Y = 346;

    // Column geometry matching reference proportions (~24% left, ~48% center, ~26% right)
    private static final int LEFT_X = 8, LEFT_Y = 40, LEFT_W = 158, LEFT_H = 300;
    private static final int MID_X = 172, MID_Y = 40, MID_W = 316, MID_H = 300;
    private static final int RIGHT_X = 494, RIGHT_Y = 40, RIGHT_W = 170, RIGHT_H = 300;

    // Preview stage inside center column
    private static final int STAGE_X = MID_X + 6, STAGE_Y = 104, STAGE_W = MID_W - 12, STAGE_H = 154;

    private static final int ROWS_PER_PAGE = 5;
    private static final int READING_LINES_PER_PAGE = 11;
    private static final int READING_LINE_HEIGHT = 11;

    private int eraFilter = 1; // Starts on Era I so Material Analyzer is the primary initial page
    private int listPage = 0;
    private int selectedIndex = 0;
    private int readingPage = 0;
    private int centerTab = 0; // 0 = Overview, 1 = Construction, 2 = How to use, 3 = Experiments

    private float previewYaw = -28.0f;
    private float previewPitch = 26.0f;
    private boolean draggingPreview = false;
    private double lastDragX = 0;
    private double lastDragY = 0;

    private String searchQuery = "";
    private boolean searchFocused = false;
    private boolean showPonderNotice = false;
    private boolean showingIntroTutorial = false;

    public QuantumGuidebookScreen(QuantumGuidebookMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
    }

    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    private float uiScale() {
        return Math.min((width - 8f) / W, (height - 8f) / H);
    }

    private float originX() {
        return (width - W * uiScale()) / 2f;
    }

    private float originY() {
        return (height - H * uiScale()) / 2f;
    }

    private double toUiX(double screenX) {
        return (screenX - originX()) / uiScale();
    }

    private double toUiY(double screenY) {
        return (screenY - originY()) / uiScale();
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private List<Chapter> filteredChapters() {
        String q = searchQuery.trim().toLowerCase(Locale.ROOT);
        List<Chapter> out = new ArrayList<>();
        for (Chapter c : GuidebookCatalog.chapters()) {
            // Keep narrative intro accessible via "Tutorial inicial" or Intro filter without crowding Era I start.
            if (eraFilter >= 0 && c.era() != eraFilter) continue;
            if (eraFilter < 0 && "intro_vault".equals(c.id()) && !showingIntroTutorial && q.isEmpty()) continue;
            if (!q.isEmpty()) {
                boolean available = menu.isPageAvailable(c);
                if (!available) continue;
                String title = tr(c.title()).toLowerCase(Locale.ROOT);
                String sub = tr(c.subtitle()).toLowerCase(Locale.ROOT);
                if (!title.contains(q) && !sub.contains(q) && !c.id().contains(q)) continue;
            }
            out.add(c);
        }
        return out;
    }

    private Chapter currentChapter(List<Chapter> chapters) {
        if (showingIntroTutorial) return GuidebookCatalog.INTRO_VAULT;
        if (chapters.isEmpty()) return null;
        selectedIndex = Math.clamp(selectedIndex, 0, chapters.size() - 1);
        return chapters.get(selectedIndex);
    }

    private void clipped(GuiGraphicsExtractor g, String label, int x, int y, int maxW, int color) {
        if (font.width(label) > maxW) {
            label = font.plainSubstrByWidth(label, Math.max(8, maxW - 7)) + "…";
        }
        g.text(font, label, x, y, color, false);
    }

    private void centeredClipped(GuiGraphicsExtractor g, String label, int cx, int y, int maxW, int color) {
        if (font.width(label) > maxW) {
            label = font.plainSubstrByWidth(label, Math.max(8, maxW - 7)) + "…";
        }
        g.text(font, label, cx - font.width(label) / 2, y, color, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        double mx = toUiX(mouseX);
        double my = toUiY(mouseY);

        List<Chapter> chapters = filteredChapters();
        int pageCount = Math.max(1, (chapters.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        listPage = Math.clamp(listPage, 0, pageCount - 1);
        Chapter current = currentChapter(chapters);

        // 1. Draw the Era V HUD in unified virtual space (672x376) with uniform scaling
        g.pose().pushMatrix();
        try {
            g.pose().translate(originX(), originY()).scale(uiScale());

            drawBackdrop(g, W, H, HEADER_H, FOOTER_Y);
            drawHeader(g, mx, my);
            drawLeftNavigation(g, chapters, pageCount, mx, my);
            drawCenterContent(g, current, mx, my);
            drawRightObjectives(g, current);
            drawFooter(g, mx, my);
        } finally {
            g.pose().popMatrix();
        }

        // 2. Submit real clipped 3D machine viewport in screen-space coordinates when Overview is active
        if (current != null && menu.isPageAvailable(current) && centerTab == 0) {
            submit3dPreview(g, current);
        }
    }

    private void submit3dPreview(GuiGraphicsExtractor g, Chapter chapter) {
        ItemStack stack = chapter.icon().get();
        float s = uiScale();
        int sx0 = Math.round(originX() + (STAGE_X + 6) * s);
        int sy0 = Math.round(originY() + (STAGE_Y + 20) * s);
        int sx1 = Math.round(originX() + (STAGE_X + STAGE_W - 6) * s);
        int sy1 = Math.round(originY() + (STAGE_Y + STAGE_H - 26) * s);
        if (sx1 - sx0 < 24 || sy1 - sy0 < 24) return;

        if (CodexMachinePreviewRenderer.supports3dPreview(stack.getItem())) {
            ScreenRectangle scissor = new ScreenRectangle(sx0, sy0, sx1 - sx0, sy1 - sy0);
            float pipScale = Math.min(sx1 - sx0, sy1 - sy0) * 0.74f;
            g.submitPictureInPictureRenderState(new CodexMachinePreviewState(
                    stack.getItem(), previewYaw, previewPitch,
                    sx0, sy0, sx1, sy1, pipScale, scissor));
        } else {
            // Non-block items (e.g. copper wire, goggles, drive belt) rendered cleanly centered inside scissor
            g.enableScissor(sx0, sy0, sx1, sy1);
            float itemScale = Math.min(sx1 - sx0, sy1 - sy0) / 28.0f;
            float cx = (sx0 + sx1) * 0.5f - 8f * itemScale;
            float cy = (sy0 + sy1) * 0.5f - 8f * itemScale;
            g.pose().pushMatrix();
            g.pose().translate(cx, cy).scale(itemScale);
            g.fakeItem(stack, 0, 0);
            g.pose().popMatrix();
            g.disableScissor();
        }
    }

    private void drawHeader(GuiGraphicsExtractor g, double mx, double my) {
        // Left: Q logo + vertical cyan line + QUANTUM CODEX / QUANTUM INDUSTRIES
        drawLogoQ(g, 10, 6, 22);
        g.fill(37, 7, 38, 27, CYAN_LINE);
        drawScaledBoldText(g, font, tr("gui.quantumindustries.codex.brand_title"), 44, 7, 1.05f, TEXT);
        drawScaledText(g, font, tr("gui.quantumindustries.codex.brand_sub"), 44, 20, 0.78f, CYAN_SOFT);

        // Center: Era banner pill (clickable to cycle eras)
        int pillX = 236, pillY = 5, pillW = 200, pillH = 24;
        drawEraPill(g, pillX, pillY, pillW, pillH);
        String eraBanner = tr(switch (eraFilter) {
            case 0 -> "gui.quantumindustries.guidebook.era_banner.0";
            case 1 -> "gui.quantumindustries.guidebook.era_banner.1";
            case 2 -> "gui.quantumindustries.guidebook.era_banner.2";
            default -> "gui.quantumindustries.guidebook.era_banner.all";
        });
        centeredClipped(g, eraBanner, pillX + pillW / 2, pillY + 8, pillW - 18, TEXT);

        // Right: Search bar + Close [X]
        int searchX = 480, searchY = 6, searchW = 152, searchH = 22;
        boolean searchHover = inside(mx, my, searchX, searchY, searchW, searchH);
        if (searchFocused) {
            drawActiveCard(g, searchX, searchY, searchW, searchH);
        } else {
            drawDarkButton(g, searchX, searchY, searchW, searchH, searchHover);
        }
        drawIcon(g, ICON_SEARCH, searchX + 6, searchY + 5, 12);
        String displaySearch = searchQuery.isEmpty()
                ? tr("gui.quantumindustries.codex.search_placeholder")
                : searchQuery + (searchFocused ? "_" : "");
        clipped(g, displaySearch, searchX + 22, searchY + 7, searchW - 28,
                searchQuery.isEmpty() ? MUTED : TEXT);

        // Close [X] button
        int closeX = 638, closeY = 6, closeW = 24, closeH = 22;
        drawDarkButton(g, closeX, closeY, closeW, closeH, inside(mx, my, closeX, closeY, closeW, closeH));
        g.text(font, "X", closeX + 9, closeY + 7, TEXT, false);
    }

    private void drawLeftNavigation(GuiGraphicsExtractor g, List<Chapter> chapters, int pageCount, double mx, double my) {
        drawPanel(g, LEFT_X, LEFT_Y, LEFT_W, LEFT_H);
        drawIcon(g, ICON_CHEVRON, LEFT_X + 10, LEFT_Y + 11, 11);
        String navHeader = eraFilter == 1
                ? tr("gui.quantumindustries.codex.first_steps")
                : tr("gui.quantumindustries.text.machine_index");
        drawScaledBoldText(g, font, navHeader, LEFT_X + 24, LEFT_Y + 12, 0.92f, TEXT);

        if (chapters.isEmpty()) {
            centeredClipped(g, tr("gui.quantumindustries.codex.no_results"),
                    LEFT_X + LEFT_W / 2, LEFT_Y + 80, LEFT_W - 18, MUTED);
            return;
        }

        int start = listPage * ROWS_PER_PAGE;
        int end = Math.min(start + ROWS_PER_PAGE, chapters.size());
        for (int i = start; i < end; i++) {
            int row = i - start;
            int rx = LEFT_X + 8;
            int ry = LEFT_Y + 30 + row * 44;
            int rw = LEFT_W - 16;
            int rh = 40;
            Chapter ch = chapters.get(i);
            boolean available = menu.isPageAvailable(ch);
            boolean selected = !showingIntroTutorial && i == selectedIndex;
            boolean hovered = inside(mx, my, rx, ry, rw, rh);

            if (selected) {
                drawActiveCard(g, rx, ry, rw, rh);
            } else if (available) {
                drawDarkButton(g, rx, ry, rw, rh, hovered);
            } else {
                drawLockedCard(g, rx, ry, rw, rh);
            }

            if (available) {
                g.fakeItem(ch.icon().get(), rx + 7, ry + 12);
                List<FormattedCharSequence> titleLines = font.split(Component.literal(tr(ch.title())), rw - 34);
                int ty = titleLines.size() > 1 ? ry + 5 : ry + 8;
                g.text(font, titleLines.get(0), rx + 28, ty, TEXT, false);
                if (titleLines.size() > 1) {
                    g.text(font, titleLines.get(1), rx + 28, ty + 9, TEXT, false);
                }
                int badgeY = titleLines.size() > 1 ? ry + 26 : ry + 22;
                drawScaledText(g, font, tr("gui.quantumindustries.guidebook.available"), rx + 28, badgeY, 0.85f, EMERALD);
            } else {
                drawIcon(g, ICON_LOCK, rx + 9, ry + 13, 14);
                g.text(font, "???", rx + 29, ry + 9, TEXT, false);
                drawScaledText(g, font, tr("gui.quantumindustries.guidebook.undiscovered"), rx + 29, ry + 22, 0.82f, MUTED);
            }
        }

        // Compact pagination if multiple pages + discovery hint at bottom of sidebar
        int pagY = LEFT_Y + LEFT_H - 46;
        if (pageCount > 1) {
            drawDarkButton(g, LEFT_X + 10, pagY, 22, 16, listPage > 0 && inside(mx, my, LEFT_X + 10, pagY, 22, 16));
            g.text(font, "<", LEFT_X + 18, pagY + 4, listPage > 0 ? CYAN : MUTED, false);
            centeredClipped(g, (listPage + 1) + " / " + pageCount, LEFT_X + LEFT_W / 2, pagY + 4, 60, MUTED);
            drawDarkButton(g, LEFT_X + LEFT_W - 32, pagY, 22, 16, listPage + 1 < pageCount && inside(mx, my, LEFT_X + LEFT_W - 32, pagY, 22, 16));
            g.text(font, ">", LEFT_X + LEFT_W - 24, pagY + 4, listPage + 1 < pageCount ? CYAN : MUTED, false);
        }

        List<FormattedCharSequence> hintLines = font.split(
                Component.literal(tr("gui.quantumindustries.guidebook.sidebar_hint")), LEFT_W - 20);
        int hy = LEFT_Y + LEFT_H - 24;
        for (int i = 0; i < Math.min(2, hintLines.size()); i++) {
            int lw = font.width(hintLines.get(i));
            g.text(font, hintLines.get(i), LEFT_X + (LEFT_W - lw) / 2, hy + i * 10, MUTED, false);
        }
    }

    private void drawCenterContent(GuiGraphicsExtractor g, Chapter current, double mx, double my) {
        if (current == null) {
            drawPanel(g, MID_X, MID_Y, MID_W, MID_H);
            centeredClipped(g, tr("gui.quantumindustries.codex.no_results"),
                    MID_X + MID_W / 2, MID_Y + MID_H / 2 - 4, MID_W - 24, MUTED);
            return;
        }
        if (!menu.isPageAvailable(current)) {
            drawLockedCenter(g, current);
            return;
        }

        // Bold uppercase title with cyan vertical bar + auto-fit subtitle (no truncation)
        g.fill(MID_X + 6, MID_Y + 4, MID_X + 10, MID_Y + 29, CYAN);
        String upperTitle = tr(current.title()).toUpperCase(Locale.ROOT);
        float titleScale = font.width(upperTitle) * 1.28f > MID_W - 20 ? 1.02f : 1.28f;
        drawScaledBoldText(g, font, upperTitle, MID_X + 16, MID_Y + 4, titleScale, TEXT);
        String sub = tr(current.subtitle());
        int subMax = MID_W - 22;
        float subScale = font.width(sub) > subMax ? Math.max(0.72f, (float) subMax / font.width(sub)) : 0.94f;
        drawScaledText(g, font, sub, MID_X + 16, MID_Y + 20, subScale, SUBTEXT);

        // 4 sub-tabs: exact width so ZERO labels are ever truncated
        String[] tabKeys = {
                "gui.quantumindustries.guidebook.tab.overview",
                "gui.quantumindustries.guidebook.tab.build",
                "gui.quantumindustries.guidebook.tab.operate",
                "gui.quantumindustries.guidebook.tab.experiments"
        };
        int tabGap = 4;
        int tabW = (MID_W - 12 - tabGap * 3) / 4; // 73px each
        int tabY = MID_Y + 36;
        for (int i = 0; i < 4; i++) {
            int tx = MID_X + 6 + i * (tabW + tabGap);
            boolean active = centerTab == i;
            boolean hovered = inside(mx, my, tx, tabY, tabW, 20);
            drawTab(g, tx, tabY, tabW, 20, active, hovered);
            String label = tr(tabKeys[i]);
            int lw = font.width(label);
            float ts = lw > tabW - 6 ? (float) (tabW - 6) / lw : 0.92f;
            int drawW = Math.round(lw * ts);
            drawScaledText(g, font, label, tx + (tabW - drawW) / 2, tabY + 6, ts, active ? TEXT : SUBTEXT);
        }

        switch (centerTab) {
            case 0 -> drawOverviewTab(g, current, mx, my);
            case 1 -> drawConstructionTab(g, current, mx, my);
            case 2 -> drawHowToUseTab(g, current);
            default -> drawExperimentsTab(g, current);
        }
    }

    private void drawOverviewTab(GuiGraphicsExtractor g, Chapter current, double mx, double my) {
        // 3D Preview Stage backdrop
        drawPreviewStage(g, STAGE_X, STAGE_Y, STAGE_W, STAGE_H);

        // Top-center "PRÉVIA 3D" badge
        String badge = tr("gui.quantumindustries.codex.preview_3d");
        int bw = font.width(badge) + 18;
        int bx = STAGE_X + (STAGE_W - bw) / 2;
        drawDarkButton(g, bx, STAGE_Y + 4, bw, 15, false);
        centeredClipped(g, badge, STAGE_X + STAGE_W / 2, STAGE_Y + 8, bw - 8, CYAN);

        // Rotate left (CCW) / right (CW) buttons at top corners of stage
        int rotLY = STAGE_Y + 5;
        int rotLX = STAGE_X + 10;
        int rotRX = STAGE_X + STAGE_W - 32;
        drawDarkButton(g, rotLX, rotLY, 22, 18, inside(mx, my, rotLX, rotLY, 22, 18));
        drawIconFlippedX(g, ICON_ROTATE, rotLX + 5, rotLY + 3, 12);
        drawDarkButton(g, rotRX, rotLY, 22, 18, inside(mx, my, rotRX, rotLY, 22, 18));
        drawIcon(g, ICON_ROTATE, rotRX + 5, rotLY + 3, 12);

        // "▶ Ver funcionamento" button integrated at bottom center of 3D stage
        int playW = 128, playH = 20;
        int playX = STAGE_X + (STAGE_W - playW) / 2;
        int playY = STAGE_Y + STAGE_H - 24;
        boolean playHover = inside(mx, my, playX, playY, playW, playH);
        drawCyanButton(g, playX, playY, playW, playH, playHover);
        drawIcon(g, ICON_PLAY, playX + 10, playY + 5, 10);
        centeredClipped(g, tr("gui.quantumindustries.guidebook.see_operation"),
                playX + playW / 2 + 6, playY + 6, playW - 24, TEXT);

        // Contextual summary text or temporary Ponder unavailability banner
        int descY = STAGE_Y + STAGE_H + 8;
        String summary = showPonderNotice
                ? tr("gui.quantumindustries.codex.ponder_unavailable")
                : firstSentence(tr(current.paragraphs().isEmpty() ? current.subtitle() : current.paragraphs().get(0)));
        List<FormattedCharSequence> descLines = font.split(Component.literal(summary), STAGE_W - 12);
        int descColor = showPonderNotice ? AMBER : SUBTEXT;
        for (int i = 0; i < Math.min(2, descLines.size()); i++) {
            int lw = font.width(descLines.get(i));
            g.text(font, descLines.get(i), STAGE_X + (STAGE_W - lw) / 2, descY + i * 11, descColor, false);
        }

        // Bottom dual CTA buttons: "Ver receita" + "Abrir tutorial"
        int btnY = MID_Y + MID_H - 30;
        int btnW = (STAGE_W - 10) / 2;
        int btnH = 24;
        int leftBtnX = STAGE_X;
        int rightBtnX = STAGE_X + btnW + 10;

        drawCyanButton(g, leftBtnX, btnY, btnW, btnH, inside(mx, my, leftBtnX, btnY, btnW, btnH));
        drawIcon(g, ICON_BOOK, leftBtnX + 14, btnY + 6, 12);
        centeredClipped(g, tr("gui.quantumindustries.codex.see_recipe"), leftBtnX + btnW / 2 + 8, btnY + 8, btnW - 30, TEXT);

        drawCyanButton(g, rightBtnX, btnY, btnW, btnH, inside(mx, my, rightBtnX, btnY, btnW, btnH));
        drawIcon(g, ICON_TUTORIAL, rightBtnX + 12, btnY + 6, 12);
        centeredClipped(g, tr("gui.quantumindustries.codex.open_tutorial"), rightBtnX + btnW / 2 + 8, btnY + 8, btnW - 30, TEXT);
    }

    private void drawConstructionTab(GuiGraphicsExtractor g, Chapter current, double mx, double my) {
        drawPanel(g, STAGE_X, STAGE_Y, STAGE_W, MID_H - 70);

        int y = STAGE_Y + 10;
        // For Material Analyzer, clearly explain Material Observation prerequisite if not yet studied
        if ("material_observation".equals(current.id())) {
            boolean unlocked = menu.isUnlocked(current);
            String note = tr(unlocked
                    ? "gui.quantumindustries.codex.study_ready_note"
                    : "gui.quantumindustries.codex.research_prereq_note");
            int boxH = 32;
            if (unlocked) drawDoneCard(g, STAGE_X + 8, y, STAGE_W - 16, boxH);
            else drawActiveCard(g, STAGE_X + 8, y, STAGE_W - 16, boxH);
            List<FormattedCharSequence> noteLines = font.split(Component.literal(note), STAGE_W - 28);
            for (int i = 0; i < Math.min(2, noteLines.size()); i++) {
                g.text(font, noteLines.get(i), STAGE_X + 14, y + 6 + i * 10, unlocked ? EMERALD : AMBER, false);
            }
            y += boxH + 8;
        }

        List<FormattedCharSequence> lines = chapterLines(current);
        int maxLines = "material_observation".equals(current.id()) ? 9 : READING_LINES_PER_PAGE;
        int pages = Math.max(1, (lines.size() + maxLines - 1) / maxLines);
        readingPage = Math.clamp(readingPage, 0, pages - 1);
        int start = readingPage * maxLines;
        int end = Math.min(lines.size(), start + maxLines);
        for (int i = start; i < end; i++) {
            g.text(font, lines.get(i), STAGE_X + 10, y + (i - start) * READING_LINE_HEIGHT, TEXT, false);
        }

        int botY = MID_Y + MID_H - 30;
        if (pages > 1) {
            drawDarkButton(g, STAGE_X + 8, botY, 28, 20, readingPage > 0 && inside(mx, my, STAGE_X + 8, botY, 28, 20));
            g.text(font, "<", STAGE_X + 19, botY + 6, readingPage > 0 ? CYAN : MUTED, false);
            centeredClipped(g, tr("gui.quantumindustries.guidebook.reading_page", readingPage + 1, pages),
                    STAGE_X + STAGE_W / 2, botY + 6, 120, SUBTEXT);
            drawDarkButton(g, STAGE_X + STAGE_W - 36, botY, 28, 20,
                    readingPage + 1 < pages && inside(mx, my, STAGE_X + STAGE_W - 36, botY, 28, 20));
            g.text(font, ">", STAGE_X + STAGE_W - 25, botY + 6, readingPage + 1 < pages ? CYAN : MUTED, false);
        } else {
            int btnW = 132;
            int bx = STAGE_X + (STAGE_W - btnW) / 2;
            drawCyanButton(g, bx, botY, btnW, 22, inside(mx, my, bx, botY, btnW, 22));
            drawIcon(g, ICON_BOOK, bx + 12, botY + 5, 12);
            centeredClipped(g, tr("gui.quantumindustries.codex.see_recipe"), bx + btnW / 2 + 8, botY + 7, btnW - 28, TEXT);
        }
    }

    private void drawHowToUseTab(GuiGraphicsExtractor g, Chapter current) {
        drawPanel(g, STAGE_X, STAGE_Y, STAGE_W, MID_H - 70);
        drawScaledBoldText(g, font, tr("gui.quantumindustries.text.how_to_operate"), STAGE_X + 12, STAGE_Y + 12, 1.0f, CYAN);
        g.fill(STAGE_X + 12, STAGE_Y + 25, STAGE_X + STAGE_W - 12, STAGE_Y + 26, CYAN_LINE);

        List<FormattedCharSequence> opLines = font.split(Component.literal(tr(current.operationSummary())), STAGE_W - 24);
        int oy = STAGE_Y + 34;
        for (FormattedCharSequence line : opLines) {
            if (oy > STAGE_Y + MID_H - 86) break;
            g.text(font, line, STAGE_X + 12, oy, TEXT, false);
            oy += 12;
        }
    }

    private void drawExperimentsTab(GuiGraphicsExtractor g, Chapter current) {
        drawPanel(g, STAGE_X, STAGE_Y, STAGE_W, MID_H - 70);
        drawScaledBoldText(g, font, tr("gui.quantumindustries.guidebook.tab.experiments").toUpperCase(Locale.ROOT),
                STAGE_X + 12, STAGE_Y + 12, 1.0f, CYAN);
        String discText = tr("gui.quantumindustries.codex.discoveries_count", menu.discoveryCount());
        g.text(font, discText, STAGE_X + STAGE_W - 12 - font.width(discText), STAGE_Y + 12, EMERALD, false);
        g.fill(STAGE_X + 12, STAGE_Y + 25, STAGE_X + STAGE_W - 12, STAGE_Y + 26, CYAN_LINE);

        int sy = STAGE_Y + 34;
        if (current.requiredStudies().isEmpty()) {
            g.text(font, tr("gui.quantumindustries.guidebook.record_decoded"), STAGE_X + 14, sy, EMERALD, false);
            return;
        }
        for (String key : current.requiredStudies()) {
            var study = ResearchCatalog.find(key);
            boolean known = menu.knows(key);
            if (known) drawDoneCard(g, STAGE_X + 10, sy, STAGE_W - 20, 32);
            else drawActiveCard(g, STAGE_X + 10, sy, STAGE_W - 20, 32);
            drawCheckbox(g, known ? CHECK_DONE : CHECK_CURRENT, STAGE_X + 18, sy + 9, 14);
            clipped(g, study != null ? tr(study.name()) : key, STAGE_X + 38, sy + 7, STAGE_W - 60, TEXT);
            clipped(g, tr(known ? "gui.quantumindustries.guidebook.study_done" : "gui.quantumindustries.guidebook.study_pending"),
                    STAGE_X + 38, sy + 19, STAGE_W - 60, known ? EMERALD : AMBER);
            sy += 38;
            if (sy > STAGE_Y + MID_H - 104) break;
        }
    }

    private void drawLockedCenter(GuiGraphicsExtractor g, Chapter chapter) {
        drawPanel(g, MID_X, MID_Y, MID_W, MID_H);
        drawLockedCard(g, MID_X + 10, MID_Y + 10, MID_W - 20, 44);
        drawIcon(g, ICON_LOCK, MID_X + 20, MID_Y + 24, 16);
        drawScaledBoldText(g, font, tr("gui.quantumindustries.text.encrypted_record"), MID_X + 44, MID_Y + 19, 1.0f, AMBER);
        clipped(g, tr("gui.quantumindustries.text.data_locked_by_the_vault_security_protocol"),
                MID_X + 44, MID_Y + 33, MID_W - 60, MUTED);

        String[] scrambled = {
                "???? ???????? ?????? ???? ???????????? ??????",
                "?????? ???? ?????????? ???????? ???? ????????",
                "?????????? ?????? ???? ???????? ?????????? ??",
                "",
                "???? ?????? ???????????? ???? ???????? ??????",
                "???????? ???? ?????? ?????????? ???? ????????"
        };
        int y = MID_Y + 72;
        for (String line : scrambled) {
            if (!line.isEmpty()) clipped(g, line, MID_X + 16, y, MID_W - 32, MUTED);
            y += 14;
        }

        String missingStudy = chapter.requiredStudies().stream()
                .filter(key -> !menu.knows(ResearchCatalog.indexOf(key)))
                .findFirst().orElse(chapter.studyKey());
        var study = ResearchCatalog.find(missingStudy);
        String studyHint = study != null ? tr(study.name()) : "????";

        drawActiveCard(g, MID_X + 10, MID_Y + 178, MID_W - 20, 110);
        drawScaledBoldText(g, font, tr("gui.quantumindustries.text.how_to_unlock_this_chapter"),
                MID_X + 20, MID_Y + 188, 0.95f, AMBER);
        String msg = tr("gui.quantumindustries.text.you_have_not_yet_learned_the_science_behind_this_machine_build_and_use_the_research_table_and_complete_the_corresponding_study_to_decode_its_full_operating_instructions");
        List<FormattedCharSequence> lines = font.split(Component.literal(msg), MID_W - 40);
        int hy = MID_Y + 204;
        for (FormattedCharSequence line : lines) {
            if (hy > MID_Y + 258) break;
            g.text(font, line, MID_X + 20, hy, TEXT, false);
            hy += 11;
        }
        clipped(g, tr("gui.quantumindustries.guidebook.required_study", studyHint,
                (study != null ? study.era() : chapter.era()) == 1 ? "I" : "II"), MID_X + 20, MID_Y + 268, MID_W - 40, CYAN);
    }

    private record ObjectiveStep(String label, boolean done) {}

    private void drawRightObjectives(GuiGraphicsExtractor g, Chapter current) {
        int objH = 212;
        drawPanel(g, RIGHT_X, RIGHT_Y, RIGHT_W, objH);
        drawIcon(g, ICON_TARGET, RIGHT_X + 10, RIGHT_Y + 10, 12);
        drawScaledBoldText(g, font, tr("gui.quantumindustries.guidebook.current_goal"), RIGHT_X + 26, RIGHT_Y + 12, 0.92f, TEXT);
        g.fill(RIGHT_X + 10, RIGHT_Y + 26, RIGHT_X + RIGHT_W - 10, RIGHT_Y + 27, CYAN_LINE);

        List<ObjectiveStep> steps = buildObjectiveSteps(current);
        int currentStepIdx = steps.size();
        for (int i = 0; i < steps.size(); i++) {
            if (!steps.get(i).done()) {
                currentStepIdx = i;
                break;
            }
        }

        int stepNumber = Math.min(steps.size(), currentStepIdx + 1);
        drawScaledText(g, font, tr("gui.quantumindustries.codex.step_label", String.format(Locale.ROOT, "%02d", Math.max(1, stepNumber))),
                RIGHT_X + 10, RIGHT_Y + 32, 0.82f, CYAN_SOFT);

        String headline = (current == null || "material_observation".equals(current.id()) || "intro_vault".equals(current.id()))
                ? tr("gui.quantumindustries.codex.goal_build_analyzer")
                : tr("gui.quantumindustries.codex.goal_master_chapter",
                menu.isPageAvailable(current) ? tr(current.title()) : "???");
        List<FormattedCharSequence> headLines = font.split(Component.literal(headline), RIGHT_W - 22);
        int hy = RIGHT_Y + 44;
        for (int i = 0; i < Math.min(2, headLines.size()); i++) {
            g.text(font, headLines.get(i), RIGHT_X + 10, hy + i * 11, TEXT, false);
        }

        int sy = RIGHT_Y + 72;
        for (int i = 0; i < Math.min(4, steps.size()); i++) {
            ObjectiveStep step = steps.get(i);
            boolean isCurrent = i == currentStepIdx;
            int rx = RIGHT_X + 8;
            int rw = RIGHT_W - 16;
            int rh = 28;
            if (step.done()) {
                drawDoneCard(g, rx, sy, rw, rh);
            } else if (isCurrent) {
                drawActiveCard(g, rx, sy, rw, rh);
            } else {
                drawLockedCard(g, rx, sy, rw, rh);
            }
            int cbState = step.done() ? CHECK_DONE : isCurrent ? CHECK_CURRENT : CHECK_PENDING;
            drawCheckbox(g, cbState, rx + 7, sy + 7, 14);
            int textColor = step.done() ? EMERALD : isCurrent ? TEXT : SUBTEXT;
            clipped(g, step.label(), rx + 26, sy + 10, rw - 32, textColor);
            sy += 32;
        }

        // Bottom-right "COMO AVANÇAR" card
        int advY = RIGHT_Y + objH + 8;
        int advH = RIGHT_H - objH - 8;
        drawPanel(g, RIGHT_X, advY, RIGHT_W, advH);
        drawIcon(g, ICON_LOCK, RIGHT_X + 10, advY + 16, 16);
        g.fill(RIGHT_X + 32, advY + 12, RIGHT_X + 33, advY + advH - 12, CYAN_LINE);
        drawScaledBoldText(g, font, tr("gui.quantumindustries.guidebook.how_to_advance"), RIGHT_X + 40, advY + 13, 0.90f, TEXT);

        String advanceText = buildAdvanceHint(current);
        List<FormattedCharSequence> advLines = font.split(Component.literal(advanceText), RIGHT_W - 48);
        int ay = advY + 26;
        for (int i = 0; i < Math.min(5, advLines.size()); i++) {
            g.text(font, advLines.get(i), RIGHT_X + 40, ay + i * 10, SUBTEXT, false);
        }
    }

    private List<ObjectiveStep> buildObjectiveSteps(Chapter current) {
        if (current == null || "material_observation".equals(current.id()) || "intro_vault".equals(current.id())) {
            boolean studied = menu.knows("material_observation");
            boolean built = menu.hasAssayBench();
            boolean sampled = menu.hasReservedSample() || menu.discoveryCount() > 0;
            boolean recorded = menu.discoveryCount() > 0;
            return List.of(
                    new ObjectiveStep(tr("gui.quantumindustries.codex.step_research_obs"), studied),
                    new ObjectiveStep(tr("gui.quantumindustries.codex.step_build_bench"), built),
                    new ObjectiveStep(tr("gui.quantumindustries.codex.step_insert_sample"), sampled),
                    new ObjectiveStep(tr("gui.quantumindustries.codex.step_register_discovery"), recorded)
            );
        }
        List<ObjectiveStep> list = new ArrayList<>();
        for (String key : current.requiredStudies()) {
            var study = ResearchCatalog.find(key);
            String name = study != null ? tr(study.name()) : key;
            list.add(new ObjectiveStep(tr("gui.quantumindustries.codex.step_study_prefix", name), menu.knows(key)));
        }
        if (list.size() < 4) {
            list.add(new ObjectiveStep(tr("gui.quantumindustries.codex.step_build_machine"),
                    minecraft != null && minecraft.player != null
                            && minecraft.player.getInventory().countItem(current.icon().get().getItem()) > 0));
        }
        return list;
    }

    private String buildAdvanceHint(Chapter current) {
        if (current != null && !menu.isUnlocked(current) && !current.requiredStudies().isEmpty()) {
            String missing = current.requiredStudies().stream()
                    .filter(k -> !menu.knows(ResearchCatalog.indexOf(k)))
                    .findFirst().orElse(current.studyKey());
            var study = ResearchCatalog.find(missing);
            if (study != null) {
                return tr("gui.quantumindustries.codex.locked_advance", tr(study.name()));
            }
        }
        return tr("gui.quantumindustries.guidebook.advance_hint");
    }

    private void drawFooter(GuiGraphicsExtractor g, double mx, double my) {
        int btnY = FOOTER_Y + 5;
        int escW = 92, escH = 20;
        drawDarkButton(g, 10, btnY, escW, escH, inside(mx, my, 10, btnY, escW, escH));
        centeredClipped(g, tr("gui.quantumindustries.guidebook.esc_close"), 10 + escW / 2, btnY + 6, escW - 10, TEXT);

        centeredClipped(g, tr("gui.quantumindustries.guidebook.footer_hint"), W / 2, btnY + 6, 390, SUBTEXT);

        int tutW = 104, tutH = 20;
        int tutX = W - 10 - tutW;
        drawDarkButton(g, tutX, btnY, tutW, tutH,
                showingIntroTutorial || inside(mx, my, tutX, btnY, tutW, tutH));
        centeredClipped(g, tr("gui.quantumindustries.guidebook.initial_tutorial"),
                tutX + tutW / 2, btnY + 6, tutW - 10, showingIntroTutorial ? CYAN : TEXT);
    }

    private List<FormattedCharSequence> chapterLines(Chapter chapter) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (String paragraph : chapter.paragraphs()) {
            lines.addAll(font.split(Component.translatable(paragraph), STAGE_W - 22));
            lines.add(FormattedCharSequence.EMPTY);
        }
        return lines;
    }

    private static String firstSentence(String text) {
        if (text == null) return "";
        int dot = text.indexOf('.');
        return (dot > 12 && dot < 180) ? text.substring(0, dot + 1) : text;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {}

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mx = toUiX(event.x());
            double my = toUiY(event.y());

            // Search focus
            boolean hitSearch = inside(mx, my, 480, 6, 152, 22);
            searchFocused = hitSearch;
            if (hitSearch) return true;

            // Close [X] or [ESC] Close
            if (inside(mx, my, 638, 6, 24, 22) || inside(mx, my, 10, FOOTER_Y + 5, 92, 20)) {
                onClose();
                return true;
            }

            // Era pill cycle (-1 -> 0 -> 1 -> 2 -> -1)
            if (inside(mx, my, 236, 5, 200, 24)) {
                showingIntroTutorial = false;
                eraFilter = eraFilter >= 2 ? -1 : eraFilter + 1;
                selectedIndex = 0;
                listPage = 0;
                readingPage = 0;
                showPonderNotice = false;
                return true;
            }

            // Initial tutorial button (footer right)
            if (inside(mx, my, W - 114, FOOTER_Y + 5, 104, 20)) {
                showingIntroTutorial = !showingIntroTutorial;
                centerTab = 0;
                readingPage = 0;
                showPonderNotice = false;
                return true;
            }

            List<Chapter> chapters = filteredChapters();
            int pageCount = Math.max(1, (chapters.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);

            // Sidebar chapter cards
            int start = listPage * ROWS_PER_PAGE;
            int end = Math.min(start + ROWS_PER_PAGE, chapters.size());
            for (int i = start; i < end; i++) {
                int ry = LEFT_Y + 30 + (i - start) * 44;
                if (inside(mx, my, LEFT_X + 8, ry, LEFT_W - 16, 40)) {
                    showingIntroTutorial = false;
                    if (selectedIndex != i) {
                        readingPage = 0;
                        showPonderNotice = false;
                    }
                    selectedIndex = i;
                    return true;
                }
            }

            // Sidebar pagination
            int pagY = LEFT_Y + LEFT_H - 46;
            if (pageCount > 1) {
                if (inside(mx, my, LEFT_X + 10, pagY, 22, 16) && listPage > 0) {
                    listPage--;
                    return true;
                }
                if (inside(mx, my, LEFT_X + LEFT_W - 32, pagY, 22, 16) && listPage + 1 < pageCount) {
                    listPage++;
                    return true;
                }
            }

            Chapter current = currentChapter(chapters);
            if (current != null && menu.isPageAvailable(current)) {
                // 4 sub-tabs
                int tabGap = 4;
                int tabW = (MID_W - 12 - tabGap * 3) / 4;
                int tabY = MID_Y + 36;
                for (int i = 0; i < 4; i++) {
                    int tx = MID_X + 6 + i * (tabW + tabGap);
                    if (inside(mx, my, tx, tabY, tabW, 20)) {
                        centerTab = i;
                        readingPage = 0;
                        showPonderNotice = false;
                        return true;
                    }
                }

                if (centerTab == 0) {
                    // Rotate buttons
                    int rotLY = STAGE_Y + 5;
                    if (inside(mx, my, STAGE_X + 10, rotLY, 22, 18)) {
                        previewYaw -= 25.0f;
                        return true;
                    }
                    if (inside(mx, my, STAGE_X + STAGE_W - 32, rotLY, 22, 18)) {
                        previewYaw += 25.0f;
                        return true;
                    }
                    // "Ver funcionamento" button
                    int playW = 128;
                    int playX = STAGE_X + (STAGE_W - playW) / 2;
                    int playY = STAGE_Y + STAGE_H - 24;
                    if (inside(mx, my, playX, playY, playW, 20)) {
                        showPonderNotice = !showPonderNotice;
                        return true;
                    }
                    // Drag inside 3D preview viewport
                    if (inside(mx, my, STAGE_X + 6, STAGE_Y + 22, STAGE_W - 12, STAGE_H - 48)) {
                        draggingPreview = true;
                        lastDragX = event.x();
                        lastDragY = event.y();
                        return true;
                    }
                    // Bottom CTA buttons: "Ver receita" & "Abrir tutorial"
                    int btnY = MID_Y + MID_H - 30;
                    int btnW = (STAGE_W - 10) / 2;
                    if (inside(mx, my, STAGE_X, btnY, btnW, 24)) {
                        if (!CodexJeiBridge.showRecipe(current.icon().get())) {
                            centerTab = 1;
                            readingPage = 0;
                        }
                        return true;
                    }
                    if (inside(mx, my, STAGE_X + btnW + 10, btnY, btnW, 24)) {
                        centerTab = 2;
                        showPonderNotice = false;
                        return true;
                    }
                } else if (centerTab == 1) {
                    int botY = MID_Y + MID_H - 30;
                    int maxLines = "material_observation".equals(current.id()) ? 9 : READING_LINES_PER_PAGE;
                    int pages = Math.max(1, (chapterLines(current).size() + maxLines - 1) / maxLines);
                    if (pages > 1) {
                        if (inside(mx, my, STAGE_X + 8, botY, 28, 20) && readingPage > 0) {
                            readingPage--;
                            return true;
                        }
                        if (inside(mx, my, STAGE_X + STAGE_W - 36, botY, 28, 20) && readingPage + 1 < pages) {
                            readingPage++;
                            return true;
                        }
                    } else {
                        int btnW = 132;
                        int bx = STAGE_X + (STAGE_W - btnW) / 2;
                        if (inside(mx, my, bx, botY, btnW, 22)) {
                            CodexJeiBridge.showRecipe(current.icon().get());
                            return true;
                        }
                    }
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingPreview && event.button() == 0) {
            previewYaw += (float) ((event.x() - lastDragX) * 0.9);
            previewPitch = Math.clamp(previewPitch + (float) ((event.y() - lastDragY) * 0.5), 5.0f, 55.0f);
            lastDragX = event.x();
            lastDragY = event.y();
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingPreview && event.button() == 0) {
            draggingPreview = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (searchFocused) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                if (!searchQuery.isEmpty()) {
                    searchQuery = "";
                    listPage = 0;
                    selectedIndex = 0;
                }
                searchFocused = false;
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                listPage = 0;
                selectedIndex = 0;
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
                searchFocused = false;
                return true;
            }
            // Swallow inventory key ('E') while typing in the search box
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchFocused && event.isAllowedChatCharacter() && searchQuery.length() < 28) {
            searchQuery += event.codepointAsString();
            showingIntroTutorial = false;
            listPage = 0;
            selectedIndex = 0;
            return true;
        }
        return super.charTyped(event);
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
