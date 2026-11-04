package br.com.quantumindustries;

import br.com.quantumindustries.core.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.bus.api.IEventBus;

public class ModItems {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(QuantumIndustries.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(QuantumIndustries.MODID);

    public static final DeferredBlock<br.com.quantumindustries.block.ResearchTableBlock> RESEARCH_TABLE = ModBlocks.RESEARCH_TABLE;

    public static final DeferredItem<BlockItem> SCIENTIFIC_ANALYZER = ITEMS.registerSimpleBlockItem(RESEARCH_TABLE);

    // Ferramenta de teste (Sonda)
    public static final DeferredItem<Item> TEST_PROBES = ITEMS.registerItem("test_probes", Item::new, properties -> properties.durability(100));

    // Dados de pesquisa (output da bancada)
    public static final DeferredItem<Item> RESEARCH_DATA = ITEMS.registerSimpleItem("research_data");

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        ModBlocks.register(eventBus);
    }
}