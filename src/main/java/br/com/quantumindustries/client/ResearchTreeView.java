package br.com.quantumindustries.client;

import br.com.quantumindustries.research.ResearchCatalog;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import static br.com.quantumindustries.client.ResearchTheme.*;

final class ResearchTreeView {
    static final int LEFT = 153, TOP = 139, RIGHT = 559, BOTTOM = 239;
    private static final int NODE_W = 120, NODE_H = 32;
    private static final Map<String, Grid> FOUNDATIONS = Map.ofEntries(
            Map.entry("material_observation", new Grid(0, 20)),
            Map.entry("material_malleability", new Grid(1, 20)),
            Map.entry("manual_forming", new Grid(2, 20)),
            Map.entry("metal_sheets", new Grid(3, 20)),
            Map.entry("basic_magnetism", new Grid(1, 76)),
            Map.entry("basic_mechanics", new Grid(3, 76)),
            Map.entry("mechanical_transmission", new Grid(4, 76)),
            Map.entry("conductivity", new Grid(0, 135)),
            Map.entry("copper_wire", new Grid(1, 135)),
            Map.entry("copper_winding", new Grid(3, 128)),
            Map.entry("electrical_insulation", new Grid(3, 168)),
            Map.entry("electromechanical_structure", new Grid(5, 76)),
            Map.entry("manual_dynamo_design", new Grid(6, 116)),
            Map.entry("electromagnetic_induction", new Grid(0, 76)),
            Map.entry("galvanic_storage", new Grid(1, 36)),
            Map.entry("electromechanical_forming", new Grid(1, 116)),
            Map.entry("wire_drawing", new Grid(2, 152)),
            Map.entry("thermal_annealing", new Grid(2, 24)),
            Map.entry("piezoelectricity", new Grid(2, 90)),
            Map.entry("fluid_hydrostatics", new Grid(4, 28)),
            Map.entry("pneumatic_transport", new Grid(4, 133)),
            Map.entry("peristaltic_mechanics", new Grid(5, 28)),
            Map.entry("load_protection", new Grid(3, 60))
    );

    private record Grid(int column, int y) {}
    private record Node(int id, int x, int y) {}

    enum State {
        HIDDEN("Oculta", LINE), BLOCKED("Bloqueada", LINE), WAITING("Faltam materiais", GOLD),
        READY("Disponível", CYAN), EVIDENCE("Falta evidência", GOLD), DONE("Concluída", GREEN);

        final String label;
        final int color;
        State(String label, int color) { this.label = label; this.color = color; }
    }

    private List<Node> nodes = List.of();
    private float zoom, panX, panY;
    private int worldW, worldH;

    ResearchTreeView() { setEra(1); }

    void setEra(int era) {
        List<Node> layout = new ArrayList<>();
        for (int id = 0; id < ResearchCatalog.STUDIES.size(); id++) {
            var study = ResearchCatalog.STUDIES.get(id);
            if (study.era() != era) continue;
            Grid grid = FOUNDATIONS.get(study.key());
            if (grid == null) grid = new Grid(layout.size() % 4, 20 + layout.size() / 4 * 48);
            layout.add(new Node(id, 8 + grid.column() * 144, grid.y()));
        }
        nodes = List.copyOf(layout);
        worldW = nodes.stream().mapToInt(node -> node.x + NODE_W + 8).max().orElse(1);
        worldH = nodes.stream().mapToInt(node -> node.y + NODE_H + 8).max().orElse(1);
        reset();
    }

    void reset() {
        zoom = Math.min(1f, Math.min((RIGHT - LEFT - 8f) / worldW, (BOTTOM - TOP - 8f) / worldH));
        panX = (RIGHT - LEFT - worldW * zoom) / 2;
        panY = (BOTTOM - TOP - worldH * zoom) / 2;
    }

    static boolean contains(double x, double y) {
        return x >= LEFT && x < RIGHT && y >= TOP && y < BOTTOM;
    }

    void pan(double dx, double dy) {
        panX += (float) dx;
        panY += (float) dy;
        constrain();
    }

    void zoomAt(double x, double y, double delta) {
        float min = Math.min(0.38f, zoom);
        float next = Math.max(min, Math.min(1.5f, (float) (zoom * Math.pow(1.15, delta))));
        float mouseX = (float) x - LEFT, mouseY = (float) y - TOP;
        panX = mouseX - (mouseX - panX) * next / zoom;
        panY = mouseY - (mouseY - panY) * next / zoom;
        zoom = next;
        constrain();
    }

    private void constrain() {
        panX = Math.max(24 - worldW * zoom, Math.min(RIGHT - LEFT - 24, panX));
        panY = Math.max(24 - worldH * zoom, Math.min(BOTTOM - TOP - 24, panY));
    }

    int hit(double x, double y) {
        if (!contains(x, y)) return -1;
        double wx = (x - LEFT - panX) / zoom, wy = (y - TOP - panY) / zoom;
        for (Node node : nodes) {
            if (wx >= node.x && wx < node.x + NODE_W && wy >= node.y && wy < node.y + NODE_H) return node.id;
        }
        return -1;
    }

    void draw(ResearchTheme theme, GuiGraphicsExtractor g, Font font, State[] states, int selected,
              int hovered, IntFunction<ItemStack> icon, int era) {
        theme.frame(g, LEFT - 1, TOP - 1, RIGHT - LEFT + 2, BOTTOM - TOP + 2, LINE, DARK, DARK);
        if (nodes.isEmpty()) {
            theme.text(g, font, "Nenhuma pesquisa nesta era.", LEFT + 10, TOP + 12, MUTED, false);
            return;
        }
        g.enableScissor(LEFT, TOP, RIGHT, BOTTOM);
        g.pose().pushMatrix();
        try {
            g.pose().translate(LEFT + panX, TOP + panY).scale(zoom);
            if (era == 1) {
                int[] rows = {7, 62, 115};
                for (int i = 0; i < rows.length; i++) {
                    theme.text(g, font, ResearchCatalog.ERA_ONE_BRANCHES.get(i).title(), 8, rows[i], MUTED, false);
                }
            }
            for (Node node : nodes) {
                for (String dependency : ResearchCatalog.STUDIES.get(node.id).prerequisites()) {
                    for (Node source : nodes) {
                        if (ResearchCatalog.STUDIES.get(source.id).key().equals(dependency)) edge(theme, g, source, node, states[source.id] == State.DONE);
                    }
                }
            }
            for (Node node : nodes) {
                State state = states[node.id];
                boolean active = state != State.HIDDEN && (selected == node.id || hovered == node.id);
                theme.frame(g, node.x, node.y, NODE_W, NODE_H, active ? CYAN : state.color,
                        state == State.HIDDEN ? 0xFF060A10 : PANEL, DARK);
                if (state == State.HIDDEN) {
                    theme.lock(g, node.x + 54, node.y + 10);
                    continue;
                }
                if (state == State.BLOCKED) {
                    theme.lock(g, node.x + 7, node.y + 10);
                } else {
                    g.fakeItem(icon.apply(node.id), node.x + 5, node.y + 8);
                }
                String name = ResearchCatalog.STUDIES.get(node.id).name();
                theme.text(g, font, font.plainSubstrByWidth(name, 92), node.x + 24, node.y + 6, TEXT, false);
                theme.text(g, font, state.label, node.x + 24, node.y + 19, state.color, false);
            }
        } finally {
            g.pose().popMatrix();
            g.disableScissor();
        }
    }

    private static void edge(ResearchTheme theme, GuiGraphicsExtractor g, Node from, Node to, boolean known) {
        int a = from.x + NODE_W, b = to.x, mid = (a + b) / 2;
        if (to.x <= from.x) {
            a = from.x;
            mid = Math.min(from.x, to.x) - 4;
        }
        int y1 = from.y + NODE_H / 2, y2 = to.y + NODE_H / 2;
        int color = known ? GREEN : LINE;
        theme.fill(g, a, y1, mid + 2, y1 + 2, color);
        theme.fill(g, mid, Math.min(y1, y2), mid + 2, Math.max(y1, y2) + 2, color);
        theme.fill(g, mid, y2, b, y2 + 2, color);
    }
}
