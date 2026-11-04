package br.com.quantumindustries;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

public class ResearchScreen extends AbstractContainerScreen<ResearchMenu> {
    private int tab;
    private Button action;
    private final Button[] tabs = new Button[3];
    private static final int CYAN = 0xFF00D9FF;
    private static final int TEXT = 0xFFE6EFFA;
    private static final int MUTED = 0xFF9CAFC1;
    private static final int GREEN = 0xFF76DB85;

    public ResearchScreen(ResearchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 320, 238);
    }

    @Override
    protected void init() {
        super.init();
        String[] names = {"Analisar", "Pesquisas", "Fabricação"};
        for (int i = 0; i < 3; i++) {
            final int selected = i;
            tabs[i] = addRenderableWidget(Button.builder(Component.literal(names[i]), b -> tab = selected)
                    .bounds(leftPos + 10 + i * 101, topPos + 27, 98, 18).build());
        }
        action = addRenderableWidget(Button.builder(Component.literal("Analisar"), b -> {
            int command = switch (tab) {
                case 0 -> 0;
                case 1 -> 3;
                default -> menu.stage() == 2 ? 2 : 1;
            };
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, command);
        }).bounds(leftPos + 184, topPos + 205, 126, 22).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (int i = 0; i < tabs.length; i++) tabs[i].active = i != tab;
        action.active = menu.remaining() == 0 && (tab == 0 ? menu.stage() == 0 : menu.stage() >= 1);
        action.setMessage(Component.literal(switch (tab) {
            case 0 -> menu.stage() >= 1 ? "Já analisado" : "Analisar amostra";
            case 1 -> "Emitir relatório";
            default -> menu.stage() == 2 ? "Fabricar 4 cabos" : "Desenvolver cabo";
        }));
    }

    private void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF121B24);
        g.outline(x, y, w, h, 0xFF445568);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFF2E4555);
    }

    private void text(GuiGraphicsExtractor g, String value, int x, int y, int color) {
        g.text(font, value, leftPos + x, topPos + y, color, false);
    }

    private void wrap(GuiGraphicsExtractor g, String value, int x, int y, int width, int color) {
        g.textWithWordWrap(font, Component.literal(value), leftPos + x, topPos + y, width, color);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        panel(g, leftPos, topPos, imageWidth, imageHeight);
        g.fill(leftPos + 3, topPos + 3, leftPos + imageWidth - 3, topPos + 5, CYAN);
        text(g, "QUANTUM INDUSTRIES", 12, 11, CYAN);
        text(g, "Mesa de Pesquisa", 198, 11, TEXT);
        panel(g, leftPos + 10, topPos + 51, 166, 89);
        panel(g, leftPos + 184, topPos + 51, 126, 146);
        if (tab == 0) {
            text(g, "01 / ANÁLISE", 18, 60, CYAN);
            panel(g, leftPos + 74, topPos + 77, 35, 30);
            g.item(new ItemStack(Items.COPPER_INGOT), leftPos + 84, topPos + 84);
            text(g, "Amostra de cobre", 43, 113, TEXT);
            text(g, menu.stage() >= 1 ? "[OK] Descoberta salva" : "Inspecione o material", 24, 126, menu.stage() >= 1 ? GREEN : MUTED);
            text(g, "Condutividade", 192, 61, CYAN);
            text(g, "do cobre", 192, 74, CYAN);
            wrap(g, "Consome 1 barra de cobre do inventário.", 192, 94, 109, TEXT);
            wrap(g, "Descobre uma propriedade do material.", 192, 124, 109, MUTED);
            text(g, "Tempo: 5 s", 192, 158, TEXT);
        } else if (tab == 1) {
            text(g, "02 / COMPARTILHAR", 18, 60, CYAN);
            g.item(new ItemStack(Items.COPPER_INGOT), leftPos + 36, topPos + 86);
            g.fill(leftPos + 60, topPos + 94, leftPos + 112, topPos + 96, CYAN);
            g.item(new ItemStack(ModItems.ANALYSIS_REPORT.get()), leftPos + 126, topPos + 86);
            text(g, "Descoberta > Relatório", 24, 114, TEXT);
            text(g, "Entregue a outro jogador", 18, 127, MUTED);
            text(g, "Relatório científico", 192, 61, CYAN);
            wrap(g, "Custo: 1 papel. Requer a análise do cobre.", 192, 81, 109, TEXT);
            wrap(g, "Quem ler aprende a descoberta. O relatório pode ser reutilizado.", 192, 113, 109, MUTED);
            text(g, "Emissão imediata", 192, 158, TEXT);
        } else {
            text(g, "03 / FABRICAÇÃO", 18, 60, CYAN);
            g.item(new ItemStack(Items.COPPER_INGOT), leftPos + 35, topPos + 86);
            g.fill(leftPos + 60, topPos + 94, leftPos + 111, topPos + 96, CYAN);
            g.item(new ItemStack(ModItems.COPPER_CABLE.get()), leftPos + 125, topPos + 86);
            text(g, "1 cobre > 4 cabos", 37, 114, TEXT);
            text(g, menu.stage() >= 2 ? "[OK] Aplicação desenvolvida" : "[X] Desenvolva a aplicação", 16, 127, menu.stage() >= 2 ? GREEN : MUTED);
            text(g, "Cabo de cobre", 192, 61, CYAN);
            wrap(g, menu.stage() >= 2 ? "Receita: 1 barra de cobre produz 4 cabos." :
                    "Desenvolvimento: 1 cobre + descoberta. Duração: 30 s.", 192, 83, 109, TEXT);
            wrap(g, menu.stage() >= 2 ? "Produção liberada para este jogador." :
                    "Ler um relatório ensina a análise. A prática é individual.", 192, 126, 109, MUTED);
        }
        if (menu.remaining() > 0) {
            int total = menu.operation() == 1 ? 100 : 600;
            text(g, "Em andamento: " + ((menu.remaining() + 19) / 20) + " s", 192, 174, CYAN);
            g.fill(leftPos + 192, topPos + 187, leftPos + 302, topPos + 191, 0xFF2B313D);
            g.fill(leftPos + 192, topPos + 187, leftPos + 192 + 110 * (total - menu.remaining()) / total, topPos + 191, CYAN);
        } else {
            text(g, menu.stage() >= 1 ? "Conhecimento salvo" : "Aguardando análise", 192, 178, menu.stage() >= 1 ? GREEN : MUTED);
        }
        text(g, "Inventário", 14, 143, TEXT);
        for (var slot : menu.slots)
            panel(g, leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18);
        super.extractContents(g, mouseX, mouseY, partialTick);
        extractTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int x, int y) {}
}
