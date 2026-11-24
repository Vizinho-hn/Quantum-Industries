package br.com.quantumindustries.client.jei;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.energy.ThermoelectricModel;
import br.com.quantumindustries.energy.ThermoelectricModel.Coolant;
import java.util.List;
import java.util.Locale;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

record ThermoelectricCategory(IGuiHelper gui) implements IRecipeCategory<ThermoelectricCategory.Table> {
    enum Table { INSTANCE }

    static final IRecipeType<Table> TYPE = IRecipeType.create(
            Identifier.fromNamespaceAndPath("quantumindustries", "thermoelectric_comparison"), Table.class);

    private record Hot(String name, Item icon, int temperature) {}
    private record Cold(Coolant coolant, Item icon) {
        int temperature() { return coolant.temperature(coolant.initialHeat()); }
        String name() { return coolant.name().toLowerCase(Locale.ROOT); }
    }

    private static final List<Hot> HOT = List.of(
            new Hot("lava", Items.LAVA_BUCKET, ThermoelectricModel.LAVA),
            new Hot("fire", Items.FLINT_AND_STEEL, ThermoelectricModel.FIRE),
            new Hot("magma", Items.MAGMA_BLOCK, ThermoelectricModel.MAGMA),
            new Hot("lit", Items.CAMPFIRE, ThermoelectricModel.LIT_APPLIANCE));
    private static final List<Cold> COLD = List.of(
            new Cold(Coolant.BLUE_ICE, Items.BLUE_ICE), new Cold(Coolant.PACKED_ICE, Items.PACKED_ICE),
            new Cold(Coolant.ICE, Items.ICE), new Cold(Coolant.SNOW, Items.SNOW_BLOCK),
            new Cold(Coolant.WATER, Items.WATER_BUCKET), new Cold(Coolant.FLOWING_WATER, Items.WATER_BUCKET));
    private static final int LEFT = 26, TOP = 44, CELL = 24, ROW = 18;

    private static Component text(String key, Object... args) {
        return Component.translatable("jei.quantumindustries.thermoelectric." + key, args);
    }

    @Override public IRecipeType<Table> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("block.quantumindustries.thermoelectric_generator"); }
    @Override public int getWidth() { return LEFT + COLD.size() * CELL; }
    @Override public int getHeight() { return 157; }
    @Override public IDrawable getIcon() { return gui.createDrawableItemLike(ModItems.THERMOELECTRIC_GENERATOR.get()); }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, Table recipe, IFocusGroup focuses) {
        for (int i = 0; i < COLD.size(); i++) {
            var cold = COLD.get(i);
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, LEFT + i * CELL + 2, 13).add(cold.icon())
                    .addRichTooltipCallback((slot, tooltip) -> tooltip.add(text(cold.name(), cold.temperature())));
        }
        for (int i = 0; i < HOT.size(); i++) {
            var hot = HOT.get(i);
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 3, TOP + i * ROW).add(hot.icon())
                    .addRichTooltipCallback((slot, tooltip) -> tooltip.add(text(hot.name(), hot.temperature())));
        }
    }

    @Override public void createRecipeExtras(IRecipeExtrasBuilder builder, Table recipe, IFocusGroup focuses) {
        builder.addText(text("axes"), getWidth(), 10).setPosition(0, 0);
        for (int col = 0; col < COLD.size(); col++) {
            var cold = COLD.get(col);
            int x = LEFT + col * CELL;
            builder.addText(Component.literal(cold.temperature() + "°"), CELL, 10).setPosition(x, 32);
            for (int row = 0; row < HOT.size(); row++) {
                int scaled = ThermoelectricModel.initialPowerNumerator(HOT.get(row).temperature(), cold.coolant());
                long percent = Math.round(100.0 * scaled / (ThermoelectricModel.MAX_FE * ThermoelectricModel.POWER_SCALE));
                builder.addText(Component.literal(percent + "%"), CELL, 10).setPosition(x, TOP + row * ROW + 4);
            }
        }
        builder.addText(text("limit", ThermoelectricModel.MAX_FE), getWidth(), 10).setPosition(0, 119);
        builder.addText(text("placement"), getWidth(), 10).setPosition(0, 131);
        builder.addText(text("warming"), getWidth(), 20).setPosition(0, 142);
    }

    @Override public void getTooltip(ITooltipBuilder tooltip, Table recipe, IRecipeSlotsView slots, double x, double y) {
        if (x < LEFT || x >= getWidth() || y < TOP || y >= TOP + HOT.size() * ROW) return;
        var hot = HOT.get((int) (y - TOP) / ROW);
        var cold = COLD.get((int) (x - LEFT) / CELL);
        int scaled = ThermoelectricModel.initialPowerNumerator(hot.temperature(), cold.coolant());
        tooltip.add(text(hot.name(), hot.temperature()));
        tooltip.add(text(cold.name(), cold.temperature()));
        tooltip.add(text("output", String.format(Locale.ROOT, "%.2f", (double) scaled / ThermoelectricModel.POWER_SCALE)));
    }
}
