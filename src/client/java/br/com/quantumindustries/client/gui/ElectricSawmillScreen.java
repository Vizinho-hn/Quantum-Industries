package br.com.quantumindustries.client.gui;

import br.com.quantumindustries.menu.ElectricSawmillMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ElectricSawmillScreen extends AbstractContainerScreen<ElectricSawmillMenu> {
  public ElectricSawmillScreen(ElectricSawmillMenu m, Inventory i, Component t) {
    super(m, i, t, 328, 238);
    titleLabelY = inventoryLabelY = -10000;
  }

  private void box(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) {
    g.fill(leftPos + x, topPos + y, leftPos + x + w, topPos + y + h, c);
  }

  private void text(GuiGraphicsExtractor g, Component t, int x, int y, int c) {
    g.text(font, t, leftPos + x, topPos + y, c, false);
  }

  private String fit(String value, int width) {
    return font.width(value) <= width
        ? value
        : font.plainSubstrByWidth(value, width - font.width("...")) + "...";
  }

  @Override
  public void extractContents(GuiGraphicsExtractor g, int mx, int my, float pt) {
    box(g, 0, 0, 328, 238, 0xFF12161C);
    box(g, 1, 1, 326, 236, 0xFF9D6945);
    box(g, 3, 3, 322, 232, 0xFF292E37);
    box(g, 8, 8, 312, 23, 0xFF15191F);
    text(g, title, 18, 15, 0xFFF4DCB9);
    for (int x = 12; x < 328; x += 30) {
      box(g, x, 4, 2, 2, 0xFFB9C2CC);
      box(g, x, 231, 2, 2, 0xFFB9C2CC);
    }
    for (var slot : menu.slots) {
      box(g, slot.x - 1, slot.y - 1, 18, 18, 0xFF8C98A7);
      box(g, slot.x, slot.y, 16, 16, 0xFF171C23);
    }
    box(g, 16, 42, 10, 72, 0xFF10151C);
    int e = menu.energy() * 68 / 1200;
    box(g, 18, 112 - e, 6, e, 0xFF56BBD4);
    text(
        g,
        Component.literal(menu.energy() + " / 1200 FE (" + menu.energy() * 100 / 1200 + "%)"),
        34,
        111,
        0xFFA5D9E4);
    text(
        g,
        Component.translatable(
            "gui.quantumindustries.electric_sawmill.cost", menu.working() ? 4 : 0),
        180,
        111,
        0xFFA5D9E4);
    int active = menu.activeSlot();
    if (active >= 0 && active < 4) {
      var input = menu.getSlot(active).getItem();
      if (!input.isEmpty()) {
        String name = input.getHoverName().getString();
        text(g, Component.literal(fit(name, 100)), 34, 34, 0xFFE0D5C8);
        var recipe =
            br.com.quantumindustries.client.MechanicalRecipeCache.all().stream()
                .filter(r -> r.accepts(input))
                .findFirst()
                .orElse(null);
        if (recipe != null)
          text(
              g,
              Component.literal("8 x " + fit(recipe.result().getHoverName().getString(), 82)),
              211,
              34,
              0xFFE0D5C8);
      }
    }
    text(g, Component.literal("->"), 107, 62, 0xFFE8BC65);
    text(g, Component.literal("->"), 199, 62, 0xFFE8BC65);
    // Pixel blade is an indicator, never an inventory slot.
    box(g, 143, 46, 36, 36, 0xFF65717E);
    box(g, 148, 41, 26, 46, 0xFF8996A4);
    box(g, 138, 51, 46, 26, 0xFF8996A4);
    box(g, 156, 59, 10, 10, 0xFF30251F);
    if (menu.working()) {
      int phase = (int) (System.currentTimeMillis() / 90) % 4;
      box(g, 142 + phase * 8, 54, 3, 20, 0xFFE8BC65);
    }
    box(g, 34, 91, 260, 6, 0xFF11161D);
    box(g, 34, 91, 260 * menu.progress() / 40, 6, 0xFFE8BC65);
    text(g, Component.literal(menu.progress() * 100 / 40 + "%"), 148, 101, 0xFFF4DCB9);
    text(
        g,
        Component.translatable("gui.quantumindustries.electric_sawmill.state." + menu.stateCode()),
        34,
        127,
        0xFFE0D5C8);
    text(
        g,
        Component.translatable(
            "gui.quantumindustries.electric_sawmill.logistics",
            (menu.transportFlags() & 1) != 0 ? "+" : "-",
            (menu.transportFlags() & 2) != 0 ? "+" : "-"),
        34,
        137,
        0xFFA5D9E4);
    text(g, Component.translatable("container.inventory"), 83, 146, 0xFFE0D5C8);
    super.extractContents(g, mx, my, pt);
    extractTooltip(g, mx, my);
  }
}
