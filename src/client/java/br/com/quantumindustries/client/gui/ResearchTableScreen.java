package br.com.quantumindustries.client.gui;

import br.com.quantumindustries.menu.ResearchTableMenu;
import net.neoforged.neoforge.client.extensions.IClientExtendedEntityInformation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("quantumindustries", "textures/gui/research_table_gui.png");

    private float animationTicks = 0.0f;

    // Vértices da célula unitária cúbica centrada nas faces (CFC) do cobre
    private final List<float[]> latticeNodes = new ArrayList<>();

    public ResearchTableScreen(ResearchTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 256, 224);
        initLattice();
    }

    private void initLattice() {
        // 8 Vértices do Cubo [-1, 1]
        for (int x = -1; x <= 1; x += 2) {
            for (int y = -1; y <= 1; y += 2) {
                for (int z = -1; z <= 1; z += 2) {
                    latticeNodes.add(new float[]{x, y, z, 0});
                }
            }
        }
        // 6 Centros das Faces (CFC)
        latticeNodes.add(new float[]{ 0,  0,  1, 1});
        latticeNodes.add(new float[]{ 0,  0, -1, 1});
        latticeNodes.add(new float[]{ 0,  1,  0, 1});
        latticeNodes.add(new float[]{ 0, -1,  0, 1});
        latticeNodes.add(new float[]{ 1,  0,  0, 1});
        latticeNodes.add(new float[]{-1,  0,  0, 1});
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = (this.width - this.imageWidth) / 2;
        int top = (this.height - this.imageHeight) / 2;

        // Fundo principal da interface
        guiGraphics.blit(TEXTURE, left, top, 0, 0, this.imageWidth, this.imageHeight);

        // Se estiver ativa a análise, incrementa os ticks visuais
        if (this.menu.isActive() || this.menu.hasToolAndSample()) {
            animationTicks += partialTick * 0.05f;
        }

        int monitorCenterX = left + 115;
        int monitorCenterY = top + 52;

        // --- 1. RENDERIZAÇÃO DA REDE ATÔMICA 3D ---
        render3DLattice(guiGraphics, monitorCenterX, monitorCenterY);

        // --- 2. RENDERIZAÇÃO DA FORMA DE ONDA DO OSCILOSCÓPIO ---
        renderWaveform(guiGraphics, left + 78, top + 92, 74, 18);

        // --- 3. BARRA E DADOS DE TELEMETRIA LATERAL ---
        renderTelemetryData(guiGraphics, left, top);
    }

    private void render3DLattice(GuiGraphics graphics, int cx, int cy) {
        float angleY = animationTicks * 0.8f;
        float angleX = 0.35f;
        float scale = 22.0f;

        float cosY = Mth.cos(angleY);
        float sinY = Mth.sin(angleY);
        float cosX = Mth.cos(angleX);
        float sinX = Mth.sin(angleX);

        for (float[] node : latticeNodes) {
            float x1 = node[0] * cosY - node[2] * sinY;
            float z1 = node[0] * sinY + node[2] * cosY;
            float y1 = node[1];

            float y2 = y1 * cosX - z1 * sinX;
            float z2 = y1 * sinX + z1 * cosX;

            int px = cx + (int) (x1 * scale);
            int py = cy + (int) (y2 * scale);

            int color = (node[3] == 0) ? 0xFFE07A48 : 0xFF00E5FF;
            int size = (z2 > 0) ? 3 : 2;
            graphics.fill(px - size / 2, py - size / 2, px + size / 2 + 1, py + size / 2 + 1, color);
        }
    }

    private void renderWaveform(GuiGraphics graphics, int x, int y, int width, int height) {
        int centerY = y + height / 2;
        float freq = 0.25f;
        float speed = animationTicks * 4.0f;
        float amplitude = this.menu.isActive() ? 6.0f : 1.5f;

        int lastPy = centerY;
        for (int i = 0; i < width; i++) {
            float sinVal = Mth.sin(i * freq + speed);
            int py = centerY + (int) (sinVal * amplitude);
            graphics.fill(x + i, Math.min(lastPy, py), x + i + 1, Math.max(lastPy, py) + 1, 0xFF00FFCC);
            lastPy = py;
        }
    }

    private void renderTelemetryData(GuiGraphics graphics, int left, int top) {
        // Use screen's font
        graphics.drawString(this.font, "§7Resistividade:", left + 168, top + 42, 0xFFFFFFFF, false);
        graphics.drawString(this.font, "§b1.68e-8 Ω·m", left + 168, top + 52, 0xFFFFFFFF, false);
        graphics.drawString(this.font, "§7Condutividade:", left + 168, top + 66, 0xFFFFFFFF, false);
        graphics.drawString(this.font, "§a5.96e7 S/m", left + 168, top + 76, 0xFFFFFFFF, false);

        int progress = this.menu.getProgress();
        int max = Math.max(1, this.menu.getMaxProgress());
        int progressWidth = (progress * 62) / max;

        graphics.drawString(this.font, "Status: " + ((progress * 100) / max) + "%", left + 168, top + 92, 0xFF00E5FF, false);
        graphics.fill(left + 168, top + 102, left + 168 + 62, top + 106, 0xFF1A222D);
        graphics.fill(left + 168, top + 102, left + 168 + progressWidth, top + 106, 0xFF00E5FF);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}