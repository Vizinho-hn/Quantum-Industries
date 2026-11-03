package br.com.quantumindustries;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(QuantumIndustries.MOD_ID)
public final class QuantumIndustries {
    public static final String MOD_ID = "quantumindustries";

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final DeferredBlock<Block> RESEARCH_TABLE = BLOCKS.registerBlock(
            "research_table",
            ResearchTableBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F)
    );

    public static final DeferredItem<BlockItem> RESEARCH_TABLE_ITEM =
            ITEMS.registerSimpleBlockItem("research_table", RESEARCH_TABLE);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB =
            TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.quantumindustries"))
                    .withTabsBefore(CreativeModeTabs.REDSTONE_BLOCKS)
                    .icon(() -> RESEARCH_TABLE_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> output.accept(RESEARCH_TABLE_ITEM.get()))
                    .build());

    public QuantumIndustries(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        TABS.register(modEventBus);
    }
}
