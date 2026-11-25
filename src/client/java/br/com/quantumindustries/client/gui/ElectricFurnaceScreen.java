package br.com.quantumindustries.client.gui;

import static br.com.quantumindustries.block.entity.ElectricFurnaceBlockEntity.CAPACITY;
import static br.com.quantumindustries.block.entity.ElectricFurnaceBlockEntity.GUI_KEY;

import br.com.quantumindustries.block.entity.ElectricFurnaceBlockEntity.OperatingState;
import br.com.quantumindustries.menu.ElectricFurnaceMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ElectricFurnaceScreen extends AbstractContainerScreen<ElectricFurnaceMenu> {
    private static final int WIDTH = 328;
    private static final int HEIGHT = 238;
    private static final int DARK = 0xFF14161A;
    private static final int STEEL = 0xFF353B47;
    private static final int SLATE = 0xFF202229;
    private static final int EDGE = 0xFF7B88A1;
    private static final int COPPER = 0xFFA34322;
    private static final int FIREBRICK = 0xFF5E2919;
    private static final int PORCELAIN = 0xFFF5F0E6;
    private static final int BRASS = 0xFFE0BA67;
    private static final int AMBER = 0xFFF58426;

    public ElectricFurnaceScreen(ElectricFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
        titleLabelY = -10000;
        inventoryLabelY = -10000;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        panel(graphics, 0, 0, WIDTH, HEIGHT, STEEL);
        panel(graphics, 11, 8, 306, 23, PORCELAIN);
        center(graphics, title, WIDTH / 2, 16, DARK);
        panel(graphics, 11, 35, 306, 105, SLATE);
        panel(graphics, 99, 51, 130, 27, FIREBRICK);
        slotFrame(graphics, 111, 55);
        slotFrame(graphics, 213, 55);
        center(graphics, translated("input"), 120, 40, PORCELAIN);
        center(graphics, translated("output"), 222, 40, PORCELAIN);

        int percent = menu.duration() > 0 ? Math.clamp(menu.progress() * 100 / menu.duration(), 0, 100) : 0;
        meter(graphics, 136, 60, 69, 9, percent, 100, BRASS);
        center(graphics, Component.literal(percent + "%"), 170, 80, PORCELAIN);

        int maxTemp = Math.max(1, menu.maxTempC());
        int gaugeHeight = 43;
        panel(graphics, 277, 43, 27, 52, DARK);
        int fill = scaled(menu.temperatureC(), maxTemp, gaugeHeight);
        box(graphics, 284, 90 - fill, 10, fill, AMBER);
        int threshold = scaled(menu.requiredTempC(), maxTemp, gaugeHeight);
        box(graphics, 281, 90 - threshold, 18, 1, PORCELAIN);

        meter(graphics, 22, 53, 70, 9, menu.energy(), CAPACITY, COPPER);
        labels(graphics);

        panel(graphics, 72, 142, 184, 90, SLATE);
        center(graphics, Component.translatable("container.inventory"), WIDTH / 2, 146, PORCELAIN);
        for (int column = 0; column < 9; column++) {
            for (int row = 0; row < 3; row++) {
                slotFrame(graphics, 82 + column * 18, 155 + row * 18);
            }
            slotFrame(graphics, 82 + column * 18, 213);
        }

        super.extractContents(graphics, mouseX, mouseY, partialTick);
        extractTooltip(graphics, mouseX, mouseY);
    }

    private void labels(GuiGraphicsExtractor graphics) {
        center(graphics, Component.literal(menu.energy() + " / " + CAPACITY + " FE"), 57, 67, PORCELAIN);
        center(graphics, translated("temperature", menu.temperatureC(), menu.requiredTempC(), menu.maxTempC()),
                98, 94, PORCELAIN);
        center(graphics, translated("thermal_energy", menu.thermalEnergyTu()), 248, 99, PORCELAIN);
        center(graphics, translated("heat_flow", menu.heatingRateTu(), menu.coolingRateTu()), 248, 112, PORCELAIN);
        OperatingState state = menu.permitted() ? menu.operatingState() : OperatingState.LOCKED;
        center(graphics, translated(menu.isHeating() ? "heater_on" : "heater_off"), 98, 111, PORCELAIN);
        box(graphics, 17, 123, 294, 12, DARK);
        center(graphics, state.label(), WIDTH / 2, 125, BRASS);
    }

    private static Component translated(String key, Object... arguments) {
        return Component.translatable(GUI_KEY + key, arguments);
    }

    private static int scaled(int value, int maximum, int width) {
        return (int) ((long) Math.clamp(value, 0, maximum) * width / maximum);
    }

    private void meter(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                       int value, int maximum, int color) {
        box(graphics, x, y, width, height, DARK);
        box(graphics, x + 1, y + 1, scaled(value, maximum, width - 2), height - 2, color);
    }

    private void panel(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
        box(graphics, x, y, width, height, DARK);
        box(graphics, x + 1, y + 1, width - 2, height - 2, EDGE);
        box(graphics, x + 2, y + 2, width - 4, height - 4, color);
    }

    private void slotFrame(GuiGraphicsExtractor graphics, int x, int y) {
        panel(graphics, x, y, 18, 18, STEEL);
    }

    private void center(GuiGraphicsExtractor graphics, Component text, int x, int y, int color) {
        graphics.text(font, text, leftPos + x - font.width(text) / 2, topPos + y, color, false);
    }

    private void box(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
        graphics.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + height, color);
    }
}
