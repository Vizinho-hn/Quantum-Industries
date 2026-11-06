package br.com.quantumindustries.core;

import br.com.quantumindustries.block.ElectromechanicalPressBlock;
import br.com.quantumindustries.block.GalvanicAccumulatorBlock;
import br.com.quantumindustries.block.ResearchTableBlock;
import br.com.quantumindustries.block.entity.ElectromechanicalPressBlockEntity;
import br.com.quantumindustries.block.entity.GalvanicAccumulatorBlockEntity;
import br.com.quantumindustries.block.entity.ResearchTableBlockEntity;
import net.minecraft.world.level.block.Block;
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

    public static final DeferredBlock<ResearchTableBlock> RESEARCH_TABLE_TIER_1 =
            BLOCKS.registerBlock("scientific_analyzer_tier_1", ResearchTableBlock::new,
                    properties -> properties.strength(2.0F).noOcclusion());

    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<ResearchTableBlockEntity>> RESEARCH_TABLE_ENTITY =
            BLOCK_ENTITIES.register("research_table",
                    () -> new BlockEntityType<>(ResearchTableBlockEntity::new, Set.of(RESEARCH_TABLE.get(), RESEARCH_TABLE_TIER_1.get()), false));

    public static final DeferredBlock<Block> FORMING_BENCH = BLOCKS.registerBlock("forming_bench",
            br.com.quantumindustries.block.FormingBenchBlock::new, properties -> properties.strength(2.5F).noOcclusion());
    public static final DeferredBlock<Block> MANUAL_DYNAMO = BLOCKS.registerBlock("manual_dynamo",
            br.com.quantumindustries.block.ManualDynamoBlock::new, properties -> properties.strength(2.5F).noOcclusion());

    public static final DeferredBlock<br.com.quantumindustries.block.CopperCableBlock> COPPER_BUSBAR =
            BLOCKS.registerBlock("copper_busbar",
                    props -> new br.com.quantumindustries.block.CopperCableBlock(props, false),
                    props -> props.strength(0.8F).noOcclusion());
    public static final DeferredBlock<br.com.quantumindustries.block.CopperCableBlock> INSULATED_CONDUIT =
            BLOCKS.registerBlock("insulated_conduit",
                    props -> new br.com.quantumindustries.block.CopperCableBlock(props, true),
                    props -> props.strength(0.8F).noOcclusion());

    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.CopperCableBlockEntity>> COPPER_CABLE_ENTITY =
            BLOCK_ENTITIES.register("copper_cable",
                    () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.CopperCableBlockEntity::new,
                            Set.of(COPPER_BUSBAR.get(), INSULATED_CONDUIT.get()), false));

    public static final DeferredBlock<br.com.quantumindustries.block.BellowsBrazierBlock> BELLOWS_BRAZIER =
            BLOCKS.registerBlock("bellows_brazier", br.com.quantumindustries.block.BellowsBrazierBlock::new,
                    props -> props.strength(2.4F).noOcclusion());
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.BellowsBrazierBlockEntity>> BELLOWS_BRAZIER_ENTITY =
            BLOCK_ENTITIES.register("bellows_brazier",
                    () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.BellowsBrazierBlockEntity::new,
                            Set.of(BELLOWS_BRAZIER.get()), false));

    public static final DeferredBlock<br.com.quantumindustries.block.WireDrawingBenchBlock> WIRE_DRAWING_BENCH =
            BLOCKS.registerBlock("wire_drawing_bench",
                    br.com.quantumindustries.block.WireDrawingBenchBlock::new,
                    props -> props.strength(2.4F).noOcclusion());

    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.WireDrawingBenchBlockEntity>> WIRE_DRAWING_BENCH_ENTITY =
            BLOCK_ENTITIES.register("wire_drawing_bench",
                    () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.WireDrawingBenchBlockEntity::new,
                            Set.of(WIRE_DRAWING_BENCH.get()), false));

    public static final DeferredBlock<GalvanicAccumulatorBlock> GALVANIC_ACCUMULATOR = BLOCKS.registerBlock("galvanic_accumulator",
            GalvanicAccumulatorBlock::new, properties -> properties.strength(2.5F).noOcclusion());

    public static final DeferredBlock<ElectromechanicalPressBlock> ELECTROMECHANICAL_PRESS = BLOCKS.registerBlock("electromechanical_press",
            ElectromechanicalPressBlock::new, properties -> properties.strength(3.0F).noOcclusion());

    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.EmpiricalAssayBenchBlockEntity>> EMPIRICAL_ASSAY_ENTITY =
            BLOCK_ENTITIES.register("empirical_assay_bench",
                    () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.EmpiricalAssayBenchBlockEntity::new, Set.of(EMPIRICAL_ASSAY_BENCH.get()), false));

    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.ManualDynamoBlockEntity>> MANUAL_DYNAMO_ENTITY =
            BLOCK_ENTITIES.register("manual_dynamo",
                    () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.ManualDynamoBlockEntity::new, Set.of(MANUAL_DYNAMO.get()), false));

    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<GalvanicAccumulatorBlockEntity>> GALVANIC_ACCUMULATOR_ENTITY =
            BLOCK_ENTITIES.register("galvanic_accumulator",
                    () -> new BlockEntityType<>(GalvanicAccumulatorBlockEntity::new, Set.of(GALVANIC_ACCUMULATOR.get()), false));

    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectromechanicalPressBlockEntity>> ELECTROMECHANICAL_PRESS_ENTITY =
            BLOCK_ENTITIES.register("electromechanical_press",
                    () -> new BlockEntityType<>(ElectromechanicalPressBlockEntity::new, Set.of(ELECTROMECHANICAL_PRESS.get()), false));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
    }
}
