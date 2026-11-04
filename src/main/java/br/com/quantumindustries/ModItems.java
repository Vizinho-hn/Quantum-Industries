package br.com.quantumindustries;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.bus.api.IEventBus;

public class ModItems {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(QuantumIndustries.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(QuantumIndustries.MODID);
    public static final DeferredBlock<Block> RESEARCH_TABLE = BLOCKS.registerBlock(
            "scientific_analyzer", ResearchTableBlock::new, properties -> properties.strength(2.5F));
    public static final DeferredItem<BlockItem> SCIENTIFIC_ANALYZER = ITEMS.registerSimpleBlockItem(RESEARCH_TABLE);

    public static final DeferredItem<net.minecraft.world.item.Item> COPPER_CABLE =
            ITEMS.registerSimpleItem("copper_cable");
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.MENU, QuantumIndustries.MODID);
    public static final java.util.function.Supplier<net.minecraft.world.inventory.MenuType<ResearchMenu>> RESEARCH_MENU =
            MENUS.register("research_table", () -> new net.minecraft.world.inventory.MenuType<>(
                    ResearchMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredItem<AnalysisReportItem> ANALYSIS_REPORT = ITEMS.registerItem("copper_analysis_report", AnalysisReportItem::new);
    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }
}


