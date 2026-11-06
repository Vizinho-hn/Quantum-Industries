package br.com.quantumindustries.client;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.menu.ResearchTableMenu;
import br.com.quantumindustries.research.ResearchCatalog;
import br.com.quantumindustries.recipe.FoundationRecipe;
import br.com.quantumindustries.recipe.MechanicalProcessingRecipe;
import java.util.List;
import java.util.stream.IntStream;
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
    private static final ResearchDetailsPanel.Entry[] ENTRIES = ResearchCatalog.STUDIES.stream()
            .map(study -> new ResearchDetailsPanel.Entry(study.name(), study.description(), study.reward(),
                    study.costs().stream().map(cost -> cost.amount() + " " + cost.label()).toArray(String[]::new)))
            .toArray(ResearchDetailsPanel.Entry[]::new);
    private static final String[] ERAS = {"Fundamentos", "Elétrica", "Ondas / Moderna", "Quântica", "IA / Autônoma"};
    private static final String[] ERA_NUMERALS = {"I", "II", "III", "IV", "V"};
    private final ResearchTreeView tree = new ResearchTreeView();
    private int era = 1;
    private final List<List<FoundationRecipe.Preview>> recipePreviews = IntStream.range(0, ENTRIES.length)
            .mapToObj(FoundationRecipe::previews).toList();
    private final List<ItemStack> sheets = List.of(
            new ItemStack(ModItems.IRON_SHEET.get()), new ItemStack(ModItems.COPPER_SHEET.get()), new ItemStack(ModItems.GOLD_SHEET.get()));
    private int recipePage;
    private int guidePage, guideResearch = -1;
    private int reportPage;
    private boolean canvasPressed;
    private boolean canvasDragged;
    private final Inventory inventory;
    private int tab;
    private int selected = ResearchCatalog.indexOf("material_observation");
    private int selectedReport = -1;

    public ResearchTableScreen(ResearchTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
        this.inventory = inventory;
    }

    private float scale() { return Math.min((width - 8f) / W, (height - 8f) / H); }
    private float originX() { return (width - W * scale()) / 2; }
    private float originY() { return (height - H * scale()) / 2; }

    private boolean complete(int id) { return menu.complete(id); }

    private boolean[] requirements(int id) {
        boolean[] result = new boolean[ENTRIES[id].costs().length];
        for (int i = 0; i < result.length; i++) result[i] = menu.hasCost(id, i);
        return result;
    }

    private State state(int id) {
        if (!menu.isRevealed(id)) return ResearchCatalog.STUDIES.get(id).key().equals("copper_wire") ? State.HIDDEN : State.BLOCKED;
        if (complete(id)) return State.DONE;
        if (!menu.evidenceReady(id)) return State.EVIDENCE;
        for (boolean ready : requirements(id)) if (!ready) return State.WAITING;
        return State.READY;
    }

    private ItemStack icon(int id) {
        var outputs = outputs(id);
        return outputs.isEmpty() ? new ItemStack(ResearchCatalog.STUDIES.get(id).icon()) : outputs.getFirst();
    }

    private boolean isFormingProcess(int id) {
        return ResearchCatalog.STUDIES.get(id).key().equals("metal_sheets");
    }

    private List<ItemStack> outputs(int id) {
        return isFormingProcess(id) ? sheets : crafts(id).stream()
                .map(FoundationRecipe.Preview::output).toList();
    }

    private List<FoundationRecipe.Preview> crafts(int id) {
        return recipePreviews.get(id).stream().filter(preview -> preview.requiredResearch().stream()
                .allMatch(key -> complete(ResearchCatalog.indexOf(key)))).toList();
    }

    private List<MechanicalProcessingRecipe> processes(int id) {
        if (!complete(id)) return List.of();
        String key = ResearchCatalog.STUDIES.get(id).key();
        return MechanicalRecipeCache.all().stream().filter(recipe -> recipe.research().equals(key)).toList();
    }

    private int guideSize() { return crafts(selected).size() + processes(selected).size(); }

    private List<Integer> unlockedRecipes() {
        return IntStream.range(0, ENTRIES.length)
                .filter(id -> complete(id) && (isFormingProcess(id) || !crafts(id).isEmpty() || !processes(id).isEmpty()))
                .boxed().toList();
    }

    private static int pageCount(int size) {
        return (size + 1) / 2;
    }

    private void pagination(GuiGraphicsExtractor g, int page, int count) {
        if (count == 0) return;
        reportButton(g, 420, 71, 28, "<", page > 0);
        reportButton(g, 516, 71, 28, ">", page + 1 < count);
        text(g, (page + 1) + " / " + count, 462, 77, MUTED);
    }

    private List<Integer> recipeRows(List<Integer> recipes) {
        recipePage = Math.min(recipePage, Math.max(0, pageCount(recipes.size()) - 1));
        int first = recipePage * 2;
        return recipes.subList(first, Math.min(first + 2, recipes.size()));
    }

    private ResearchTheme theme = new ResearchTheme(false);
    private boolean notebook;

    private void text(GuiGraphicsExtractor g, String value, int x, int y, int color) {
        theme.text(g, font, value, x, y, color, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (notebook != menu.isTierOne()) {
            notebook = menu.isTierOne();
            theme = new ResearchTheme(notebook);
        }
        int x = (int) Math.floor((mouseX - originX()) / scale());
        int y = (int) Math.floor((mouseY - originY()) / scale());
        int hover = tab == 0 && !canvasDragged ? tree.hit(x, y) : -1;
        List<Integer> recipes = tab == 2 ? unlockedRecipes() : List.of();
        List<Integer> rows = tab == 2 ? recipeRows(recipes) : List.of();
        if (!rows.isEmpty() && !rows.contains(selected)) selected = rows.getFirst();
        State[] states = new State[ENTRIES.length];
        for (int id = 0; id < states.length; id++) states[id] = state(id);
        g.pose().pushMatrix();
        try {
            g.pose().translate(originX(), originY()).scale(scale());
            theme.fill(g,0, 0, W, H, 0xFF07111C);
            header(g);
            theme.frame(g, 4, 62, 568, 330, LINE, PANEL, DARK);
            theme.frame(g, 9, 67, 132, 260, LINE, PANEL, DARK);
            theme.frame(g, 146, 67, 421, 260, LINE, DARK, DARK);
            sidebar(g, states);
            if (tab == 0) {
                text(g, "ÁRVORE DE PESQUISA — Era " + ERA_NUMERALS[era - 1] + ": " + ERAS[era - 1],
                        155, 77, TEXT);
                reportButton(g, 475, 71, 77, "Centralizar", true);
                eras(g);
                tree.draw(theme, g, font, states, selected, hover, this::icon, era);
                selectedDetails(g);
            } else if (tab == 2) {
                recipeJournal(g, rows);
                pagination(g, recipePage, pageCount(recipes.size()));
            } else if (tab == 3) {
                report(g);
                pagination(g, reportPage, pageCount(ENTRIES.length));
            } else {
                text(g, "BASE DE CONHECIMENTO", 155, 77, CYAN);
                knowledgeBase(g);
            }
            footer(g, states[selected], icon(selected));
            if (notebook) theme.binding(g);
        } finally {
            g.pose().popMatrix();
        }
        if (tab == 0 && x >= 153 && x < 559 && y >= 244 && y < 321) {
            var study = ResearchCatalog.STUDIES.get(selected);
            var details = new java.util.ArrayList<Component>();
            details.add(Component.literal(study.name()));
            details.add(Component.literal(study.description()));
            details.add(Component.literal("Conhecimentos anteriores"));
            for (String key : study.prerequisites()) {
                int id = ResearchCatalog.indexOf(key);
                details.add(Component.literal((complete(id) ? "+ " : "- ") + ENTRIES[id].name()));
            }
            details.add(Component.literal(complete(selected) && !menu.evidenceReady(selected)
                    ? "Evidências científicas. Dispensadas neste estudo já concluído." : "Evidências científicas"));
            if (study.evidence().isEmpty()) details.add(Component.literal("Nenhuma evidência adicional."));
            for (int i = 0; i < study.evidence().size(); i++) {
                details.add(Component.literal(menu.hasEvidence(selected, i) ? "+ " : "- ")
                        .append(study.evidence().get(i).description()));
            }
            if (!study.evidence().isEmpty()) details.add(Component.literal("Realize o ensaio no Analisador de Materiais."));
            details.add(Component.literal("Materiais de pesquisa"));
            for (int i = 0; i < study.costs().size(); i++) {
                details.add(Component.literal((complete(selected) || menu.hasCost(selected, i) ? "+ " : "- ")
                        + ENTRIES[selected].costs()[i]));
            }
            g.setComponentTooltipForNextFrame(font, details, mouseX, mouseY);
        }
    }

    private void selectedDetails(GuiGraphicsExtractor g) {
        theme.frame(g, 153, 244, 406, 77, LINE, PANEL, DARK);
        if (selected < 0 || ResearchCatalog.STUDIES.get(selected).era() != era) {
            text(g, "Selecione uma pesquisa da era.", 163, 257, MUTED);
            return;
        }
        var entry = ENTRIES[selected];
        text(g, entry.name(), 163, 252, CYAN);
        if (complete(selected)) drawOutputs(g, outputs(selected), 548, 246);
        clipped(g, entry.description(), 163, 265, 386, TEXT);
        clipped(g, "Desbloqueio: " + entry.reward(), 163, 278, 386, GREEN);
        var study = ResearchCatalog.STUDIES.get(selected);
        var dependencies = study.prerequisites();
        int dependencyY = study.evidence().isEmpty() ? 294 : 288;
        if (!dependencies.isEmpty()) {
            text(g, "Pré-requisitos:", 163, dependencyY, MUTED);
            if (!study.evidence().isEmpty()) {
                String names = dependencies.stream().map(key -> {
                    int id = ResearchCatalog.indexOf(key);
                    return (complete(id) ? "+ " : "- ") + ENTRIES[id].name();
                }).collect(java.util.stream.Collectors.joining(", "));
                boolean ready = dependencies.stream().allMatch(key -> complete(ResearchCatalog.indexOf(key)));
                clipped(g, names, 247, dependencyY, 302, ready ? GREEN : GOLD);
            } else {
                for (int i = 0; i < dependencies.size(); i++) {
                    int id = ResearchCatalog.indexOf(dependencies.get(i));
                    clipped(g, (complete(id) ? "+ " : "- ") + ENTRIES[id].name(),
                            247 + (i % 2) * 149, dependencyY + i / 2 * 11, 144, complete(id) ? GREEN : GOLD);
                }
            }
        }
        if (!study.evidence().isEmpty()) {
            int pending = 0;
            while (pending < study.evidence().size() && menu.hasEvidence(selected, pending)) pending++;
            int evidence = Math.min(pending, study.evidence().size() - 1);
            boolean met = menu.hasEvidence(selected, evidence);
            String evidenceLabel = complete(selected) && !menu.evidenceReady(selected)
                    ? "Pesquisa concluída. Evidência adicional dispensada."
                    : (met ? "+ " : "- ") + study.evidence().get(evidence).description().getString();
            clipped(g, evidenceLabel, 163, 299, 386, met || complete(selected) ? GREEN : GOLD);
            text(g, "Investigue no Analisador de Materiais. Passe o mouse para detalhes.", 163, 310, MUTED);
        }
    }

    private void header(GuiGraphicsExtractor g) {
        theme.frame(g, 4, 4, 568, 31, LINE, PANEL, DARK);
        theme.fill(g,4, 4, 16, 5, CYAN);
        theme.fill(g,4, 4, 5, 15, CYAN);
        theme.fill(g,14, 16, 21, 23, CYAN);
        theme.fill(g,24, 23, 31, 30, CYAN);
        theme.fill(g,24, 12, 31, 19, GOLD);
        text(g, "QUANTUM INDUSTRIES", 39, 9, MUTED);
        text(g, notebook ? "Caderno de pesquisas" : "Research Bench", 39, 21, TEXT);
        var analyzer = notebook ? ModItems.SCIENTIFIC_ANALYZER_TIER_1 : ModItems.SCIENTIFIC_ANALYZER;
        if (notebook) theme.frame(g, 214, 9, 218, 21, LINE, 0xFF145368, PANEL);
        text(g, new ItemStack(analyzer.get()).getHoverName().getString(), 228, 16, TEXT);
        for (int i = 0; i < TABS.length; i++) {
            int x = 9 + i * 140;
            theme.frame(g, x, 39, 136, 23, i == tab ? CYAN : LINE, i == tab ? 0xFF145368 : PANEL, DARK);
            text(g, TABS[i], x + 10, 46, i == tab ? TEXT : MUTED);
        }
    }

    private void sidebar(GuiGraphicsExtractor g, State[] states) {
        text(g, "STATUS DA PESQUISA", 17, 77, MUTED);
        theme.fill(g,17, 89, 133, 90, LINE);
        State[] order = {State.DONE, State.READY, State.WAITING, State.HIDDEN};
        String[] labels = {"Concluídas", "Disponíveis", "Aguardando", "Bloqueadas"};
        for (int i = 0; i < order.length; i++) {
            int count = 0;
            for (State state : states) if (state == order[i] || order[i] == State.HIDDEN && state == State.BLOCKED
                    || order[i] == State.WAITING && state == State.EVIDENCE) count++;
            int y = 97 + i * 16;
            theme.fill(g,18, y + 1, 23, y + 6, order[i].color);
            text(g, labels[i], 29, y, TEXT);
            text(g, Integer.toString(count), 120, y, order[i].color);
        }
        theme.fill(g,17, 163, 133, 164, LINE);
        text(g, "PESQUISA SELECIONADA", 17, 173, MUTED);
        clipped(g, ENTRIES[selected].name(), 17, 192, 115, TEXT);
        clipped(g, states[selected].label, 17, 207, 115, states[selected].color);
        theme.fill(g,17, 223, 133, 224, LINE);
        text(g, "DESBLOQUEIO", 17, 234, MUTED);
        var prerequisites = ResearchCatalog.STUDIES.get(selected).prerequisites();
        String note = states[selected] == State.BLOCKED && !prerequisites.isEmpty()
                ? "Veja dependências abaixo" : ENTRIES[selected].reward();
        clipped(g, note, 17, 249, 115, TEXT);
        if (tab == 0) {
            sidebarAction(g, complete(selected) ? "Concluída" : states[selected] == State.EVIDENCE ? "Falta evidência" : "Iniciar pesquisa",
                    ResearchCatalog.STUDIES.get(selected).era() == era && states[selected] == State.READY);
        } else if (tab == 3) {
            sidebarAction(g, "Escrever relatório", selectedReport >= 0 && canWrite(selectedReport));
        }
    }

    private void clipped(GuiGraphicsExtractor g, String label, int x, int y, int max, int color) {
        if (font.width(label) > max) label = font.plainSubstrByWidth(label, max - 8) + "…";
        text(g, label, x, y, color);
    }

    private void sidebarAction(GuiGraphicsExtractor g, String label, boolean ready) {
        theme.frame(g, ACTION_X, ACTION_Y, ACTION_W, ACTION_H,
                ready ? CYAN : LINE, ready ? 0xFF145368 : PANEL, DARK);
        text(g, label, ACTION_X + 9, ACTION_Y + 8, ready ? TEXT : MUTED);
    }

    private static boolean hitsSidebarAction(double x, double y) {
        return x >= ACTION_X && x < ACTION_X + ACTION_W
                && y >= ACTION_Y && y < ACTION_Y + ACTION_H;
    }

    private void eras(GuiGraphicsExtractor g) {
        for (int i = 0; i < ERAS.length; i++) {
            int x = 153 + i * 81;
            theme.frame(g, x, 104, 79, 30, i + 1 == era ? CYAN : LINE, i + 1 == era ? 0xFF145368 : PANEL, DARK);
            text(g, "Era " + ERA_NUMERALS[i], x + 6, 109, TEXT);
            if (font.width(ERAS[i]) <= 70) {
                text(g, ERAS[i], x + 5, 121, MUTED);
            } else {
                g.pose().pushMatrix();
                g.pose().translate(x + 5, 121).scale(70f / font.width(ERAS[i]));
                text(g, ERAS[i], 0, 0, MUTED);
                g.pose().popMatrix();
            }
            if (i > 1 || i == 1 && !complete(ResearchCatalog.indexOf("manual_dynamo_design"))) theme.lock(g, x + 59, 106);
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
            theme.frame(g, x, 125, 129, 75, LINE, PANEL, DARK);
            g.fakeItem(icons[i], x + 8, 132);
            text(g, names[i] + " × " + menu.supply(i), x + 29, 136, TEXT);
            reportButton(g, x + 7, 153, 115, "Guardar", true);
            reportButton(g, x + 7, 175, 115, "Retirar", menu.supply(i) > 0);
        }
        text(g, "SELECIONE UMA PESQUISA CONCLUÍDA", 155, 211, MUTED);
        for (int id = reportPage * 2; id < Math.min(reportPage * 2 + 2, ENTRIES.length); id++) {
            int x = 155 + (id % 2) * 204;
            boolean active = selectedReport == id;
            theme.frame(g, x, 225, 193, 19, active ? CYAN : LINE,
                    active ? 0xFF145368 : PANEL, DARK);
            text(g, state(id) == State.HIDDEN ? "Pesquisa oculta" : ENTRIES[id].name(), x + 8, 231, complete(id) ? TEXT : MUTED);
        }
        text(g, "Entregue o relatório a outro jogador.", 155, 265, TEXT);
        text(g, "Botão direito para aprender. Fio de Cobre exige Condutividade.", 155, 279, MUTED);
    }

    private boolean canWrite(int id) {
        return complete(id) && menu.supply(0) > 0 && menu.supply(1) > 0 && menu.supply(2) > 0;
    }

    private void reportButton(GuiGraphicsExtractor g, int x, int y, int w, String label, boolean enabled) {
        theme.frame(g, x, y, w, 19, enabled ? CYAN : LINE, enabled ? 0xFF145368 : PANEL, DARK);
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
            int study = reportPage * 2 + id;
            if (id >= 0 && study < ENTRIES.length && complete(study)) {
                selectedReport = study;
                selected = study;
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

    private void recipeGuide(GuiGraphicsExtractor g) {
        if (isFormingProcess(selected)) {
            text(g, "Martelo na mão principal e lingote de ferro, cobre ou ouro na outra.", 165, 250, TEXT);
            text(g, "Dê 3 golpes na bancada para produzir uma chapa.", 165, 266, MUTED);
            return;
        }
        var available = crafts(selected);
        var operations = processes(selected);
        int total = available.size() + operations.size();
        if (guideResearch != selected) { guidePage = 0; guideResearch = selected; }
        guidePage = Math.clamp(guidePage, 0, Math.max(0, pageCount(total) - 1));
        if (total > 2) {
            reportButton(g, 492, 217, 22, "<", guidePage > 0);
            reportButton(g, 531, 217, 22, ">", guidePage + 1 < pageCount(total));
            text(g, "" + (guidePage + 1), 518, 222, MUTED);
        }
        int x = 160;
        for (int i = guidePage * 2; i < Math.min(guidePage * 2 + 2, total); i++) {
            if (i < available.size()) drawCraft(g, x, available.get(i));
            else drawProcess(g, x, operations.get(i - available.size()));
            x += 195;
        }
    }

    private void drawProcess(GuiGraphicsExtractor g, int x, MechanicalProcessingRecipe recipe) {
        theme.frame(g, x, 238, 185, 76, LINE, PANEL, DARK);
        clipped(g, Component.translatable("block.quantumindustries.mechanical_sawmill").getString(), x + 7, 244, 172, TEXT);
        var input = new ItemStack(recipe.inputs().getFirst());
        var result = recipe.result();
        g.fakeItem(input, x + 7, 259);
        clipped(g, input.getHoverName().getString(), x + 28, 263, 100, TEXT);
        text(g, "→", x + 132, 263, MUTED);
        g.fakeItem(result, x + 148, 259);
        text(g, "×" + result.getCount(), x + 166, 263, TEXT);
        clipped(g, result.getCount() + "× " + result.getHoverName().getString(), x + 7, 280, 172, TEXT);
        text(g, Component.translatable("gui.quantumindustries.mechanical.process_rating", recipe.ticks() / 20f, recipe.mechanicalLoad()).getString(), x + 7, 292, MUTED);
        text(g, Component.translatable("gui.quantumindustries.mechanical.process_drive").getString(), x + 7, 303, MUTED);
    }

    private void drawCraft(GuiGraphicsExtractor g, int x, FoundationRecipe.Preview preview) {
        theme.frame(g, x, 238, 185, 76, LINE, PANEL, DARK);
        int rows = preview.ingredients().size() / preview.width();
        int top = rows == 1 ? 1 : 0;
        int left = preview.width() == 1 ? 1 : 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int sx = x + 7 + col * 19, sy = 246 + row * 19;
                theme.frame(g, sx, sy, 18, 18, LINE, DARK, DARK);
                if (row >= top && row < top + rows && col >= left && col < left + preview.width()) {
                    var ingredient = preview.ingredients().get((row - top) * preview.width() + col - left);
                    if (!ingredient.isEmpty()) g.fakeItem(ingredient, sx + 1, sy + 1);
                }
            }
        }
        text(g, "→", x + 70, 270, MUTED);
        theme.frame(g, x + 84, 259, 22, 22, LINE, DARK, DARK);
        g.fakeItem(preview.output(), x + 87, 262);
        text(g, "×" + preview.output().getCount(), x + 112, 268, TEXT);
        var lines = font.split(preview.output().getHoverName(), 106);
        for (int i = 0; i < Math.min(lines.size(), 2); i++) {
            g.text(font, lines.get(i), x + 71, 287 + i * 10, theme.ink(TEXT), false);
        }
    }

    private void knowledgeBase(GuiGraphicsExtractor g) {
        int y = 100;
        for (var branch : ResearchCatalog.ERA_ONE_BRANCHES) {
            if (!branch.studies().stream().allMatch(key -> complete(ResearchCatalog.indexOf(key)))) continue;
            theme.frame(g, 155, y, 401, 63, LINE, PANEL, DARK);
            text(g, branch.title(), 166, y + 8, GREEN);
            knowledgeParagraph(g, branch.knowledge(), y + 21);
            knowledgeParagraph(g, branch.application(), y + 41);
            y += 70;
        }
        if (complete(ResearchCatalog.indexOf("manual_dynamo_design"))) {
            knowledgeParagraph(g, "No dínamo, o giro move um conjunto magnético interno em relação às bobinas.", y - 4);
        }
        if (y == 100) {
            text(g, "Nenhum ramo concluído.", 165, 108, TEXT);
            text(g, "Conclua as pesquisas de um ramo para registrar o que aprendeu.", 165, 125, MUTED);
        }
    }

    private void knowledgeParagraph(GuiGraphicsExtractor g, String value, int y) {
        String firstLine = font.plainSubstrByWidth(value, 374);
        if (firstLine.length() == value.length()) {
            text(g, value, 166, y, TEXT);
            return;
        }
        int split = firstLine.lastIndexOf(' ');
        if (split <= 0) split = firstLine.length();
        text(g, value.substring(0, split), 166, y, TEXT);
        text(g, value.substring(split).stripLeading(), 166, y + 10, TEXT);
    }

    private void drawOutputs(GuiGraphicsExtractor g, List<ItemStack> outputs, int right, int y) {
        int x = right - outputs.size() * 23;
        for (ItemStack output : outputs) {
            theme.frame(g, x, y, 20, 20, LINE, DARK, DARK);
            g.fakeItem(output, x + 2, y + 2);
            x += 23;
        }
    }

    private static int recipeRowY(int row) {
        return 101 + row * 56;
    }

    private void recipeJournal(GuiGraphicsExtractor g, List<Integer> rows) {
        text(g, "RECEITAS DESBLOQUEADAS", 155, 77, CYAN);
        if (rows.isEmpty()) {
            text(g, "Nenhuma receita desbloqueada.", 165, 108, TEXT);
            text(g, "Conclua pesquisas de fabricação para consultar as receitas.", 165, 125, MUTED);
            return;
        }
        for (int row = 0; row < rows.size(); row++) {
            int id = rows.get(row);
            int y = recipeRowY(row);
            theme.frame(g, 155, y, 401, 48, id == selected ? CYAN : LINE, PANEL, DARK);
            text(g, ENTRIES[id].name(), 165, y + 10, id == selected ? CYAN : TEXT);
            clipped(g, ENTRIES[id].reward(), 165, y + 28, 326, MUTED);
            drawOutputs(g, outputs(id), 548, y + 14);
        }
        text(g, Component.translatable("gui.quantumindustries.mechanical.recipe_heading").getString(), 160, 223, MUTED);
        recipeGuide(g);
    }

    private void footer(GuiGraphicsExtractor g, State state, ItemStack icon) {
        theme.frame(g, 9, 332, 558, 55, LINE, PANEL, DARK);
        g.fakeItem(icon, 19, 348);
        clipped(g, ENTRIES[selected].name(), 43, 345, 108, TEXT);
        text(g, ResearchCatalog.STUDIES.get(selected).era() == 2 ? "Era II / Automação" : "Era I / Fundamentos", 43, 360, MUTED);
        theme.fill(g,156, 340, 157, 380, LINE);
        theme.fill(g,316, 340, 317, 380, LINE);
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
            theme.fill(g,x, 354, x + 8, 362, state == State.DONE ? CYAN : LINE);
            if (state != State.DONE) theme.fill(g,x + 1, 355, x + 7, 361, DARK);
        }
        text(g, state == State.DONE ? "100%" : "0%", 500, 354, TEXT);
        text(g, state.label, 327, 369, state.color);

        theme.frame(g, 4, 396, 568, 32, LINE, PANEL, DARK);
        text(g, "INVENTÁRIO DO OPERADOR", 15, 408, MUTED);
        for (int i = 0; i < 9; i++) {
            int x = 178 + i * 25;
            theme.frame(g, x, 401, 23, 23, LINE, DARK, DARK);
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
                    if (i == 2 && tab != 2) {
                        int index = unlockedRecipes().indexOf(selected);
                        if (index >= 0) recipePage = index / 2;
                    }
                    tab = i;
                    return true;
                }
            }
            if ((tab == 2 || tab == 3) && y >= 71 && y < 90 && (x >= 420 && x < 448 || x >= 516 && x < 544)) {
                int step = x < 448 ? -1 : 1;
                if (tab == 2) {
                    List<Integer> recipes = unlockedRecipes();
                    recipePage = Math.max(0, Math.min(pageCount(recipes.size()) - 1, recipePage + step));
                } else {
                    reportPage = Math.max(0, Math.min(pageCount(ENTRIES.length) - 1, reportPage + step));
                }
                return true;
            }
            if (tab == 2 && x >= 155 && x < 556) {
                if (y >= 217 && y < 236 && (x >= 492 && x < 514 || x >= 531 && x < 553)) {
                    guidePage = Math.clamp(guidePage + (x < 514 ? -1 : 1), 0, Math.max(0, pageCount(guideSize()) - 1));
                    return true;
                }
                List<Integer> rows = recipeRows(unlockedRecipes());
                for (int row = 0; row < rows.size(); row++) {
                    int top = recipeRowY(row);
                    if (y >= top && y < top + 48) {
                        selected = rows.get(row);
                        return true;
                    }
                }
            }
            if (tab == 3 && clickReport(x, y)) return true;
            if (tab == 0) {
                if (x >= 475 && x < 552 && y >= 71 && y < 90) {
                    tree.reset();
                    return true;
                }
                if (y >= 104 && y < 134) {
                    for (int index = 0; index < ERAS.length; index++) {
                        int start = 153 + index * 81;
                        if (x < start || x >= start + 79) continue;
                        int next = index + 1;
                        if (next == 1 || next == 2 && complete(ResearchCatalog.indexOf("manual_dynamo_design"))) {
                            era = next;
                            tree.setEra(era);
                            for (int study = 0; study < ENTRIES.length; study++) {
                                if (ResearchCatalog.STUDIES.get(study).era() == era) {
                                    selected = era == 1 ? ResearchCatalog.indexOf("material_observation") : study;
                                    break;
                                }
                            }
                        }
                        return true;
                    }
                }
                if (ResearchTreeView.contains(x, y)) {
                    canvasPressed = true;
                    canvasDragged = false;
                    return true;
                }
                if (hitsSidebarAction(x, y) && ResearchCatalog.STUDIES.get(selected).era() == era && state(selected) == State.READY) {
                    var gameMode = Minecraft.getInstance().gameMode;
                    if (gameMode != null) gameMode.handleInventoryButtonClick(menu.containerId, selected);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (canvasPressed && event.button() == 0) {
            if (dx != 0 || dy != 0) {
                canvasDragged = true;
                tree.pan(dx / scale(), dy / scale());
            }
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (canvasPressed && event.button() == 0) {
            canvasPressed = false;
            if (!canvasDragged) {
                int id = tree.hit((event.x() - originX()) / scale(), (event.y() - originY()) / scale());
                if (id >= 0 && state(id) != State.HIDDEN) selected = id;
            }
            canvasDragged = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        double x = (mouseX - originX()) / scale(), y = (mouseY - originY()) / scale();
        if (tab == 0 && ResearchTreeView.contains(x, y)) {
            tree.zoomAt(x, y, vertical);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }
}
