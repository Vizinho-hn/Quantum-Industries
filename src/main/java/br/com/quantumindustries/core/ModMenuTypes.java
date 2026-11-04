package br.com.quantumindustries.core;

import br.com.quantumindustries.menu.MaterialAnalyzerMenu;
import br.com.quantumindustries.menu.ResearchTableMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.bus.api.IEventBus;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(net.minecraft.core.registries.Registries.MENU, "quantumindustries");
    public static final net.neoforged.neoforge.registries.DeferredHolder<MenuType<?>, MenuType<br.com.quantumindustries.menu.EmpiricalAssayBenchMenu>> EMPIRICAL_ASSAY_MENU = MENU_TYPES.register("empirical_assay_bench", () -> IMenuTypeExtension.create((id, inv, buf) -> new br.com.quantumindustries.menu.EmpiricalAssayBenchMenu(id, inv, buf.readBlockPos())));

    public static final net.neoforged.neoforge.registries.DeferredHolder<MenuType<?>, MenuType<ResearchTableMenu>> RESEARCH_TABLE_MENU =
            MENU_TYPES.register("research_table_menu", () -> IMenuTypeExtension.create(ResearchTableMenu::createMenu));

    public static final net.neoforged.neoforge.registries.DeferredHolder<MenuType<?>, MenuType<MaterialAnalyzerMenu>> MATERIAL_ANALYZER_MENU =
            MENU_TYPES.register("material_analyzer_menu", () -> IMenuTypeExtension.create((windowId, inv, data) -> new MaterialAnalyzerMenu(windowId, inv)));

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}
