package br.com.quantumindustries.core;

import br.com.quantumindustries.block.ResearchTableBlock;
import br.com.quantumindustries.block.entity.ResearchTableBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.bus.api.IEventBus;

import java.util.Set;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("quantumindustries");
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, "quantumindustries");
    public static final DeferredBlock<br.com.quantumindustries.block.EmpiricalAssayBenchBlock> EMPIRICAL_ASSAY_BENCH = BLOCKS.registerBlock("empirical_assay_bench", br.com.quantumindustries.block.EmpiricalAssayBenchBlock::new, p -> p.strength(2.5F).noOcclusion());

    public static final DeferredBlock<ResearchTableBlock> RESEARCH_TABLE = BLOCKS.registerBlock("scientific_analyzer",
            ResearchTableBlock::new, properties -> properties.strength(2.5F).requiresCorrectToolForDrops().noOcclusion());

    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<ResearchTableBlockEntity>> RESEARCH_TABLE_ENTITY =
            BLOCK_ENTITIES.register("research_table",
                    () -> new BlockEntityType<>(ResearchTableBlockEntity::new, Set.of(RESEARCH_TABLE.get()), false));

    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.EmpiricalAssayBenchBlockEntity>> EMPIRICAL_ASSAY_ENTITY =
            BLOCK_ENTITIES.register("empirical_assay_bench",
                    () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.EmpiricalAssayBenchBlockEntity::new, Set.of(EMPIRICAL_ASSAY_BENCH.get()), false));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
    }
}
