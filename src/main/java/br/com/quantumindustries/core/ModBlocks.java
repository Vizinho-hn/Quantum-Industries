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

    public static final DeferredBlock<br.com.quantumindustries.block.MechanicalSawmillBlock> MECHANICAL_SAWMILL =
            BLOCKS.registerBlock("mechanical_sawmill", br.com.quantumindustries.block.MechanicalSawmillBlock::new,
                    properties -> properties.strength(2.5F).noOcclusion());
    public static final DeferredBlock<br.com.quantumindustries.block.MechanicalShaftBlock> MECHANICAL_SHAFT_BLOCK =
            BLOCKS.registerBlock("mechanical_shaft_block", br.com.quantumindustries.block.MechanicalShaftBlock::new,
                    properties -> properties.strength(2.0F).noOcclusion());
    public static final DeferredBlock<br.com.quantumindustries.block.PrimitiveWaterWheelBlock> PRIMITIVE_WATER_WHEEL =
            BLOCKS.registerBlock("primitive_water_wheel", br.com.quantumindustries.block.PrimitiveWaterWheelBlock::new,
                    properties -> properties.strength(2.0F).noOcclusion());
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.PrimitiveWaterWheelBlockEntity>> PRIMITIVE_WATER_WHEEL_ENTITY =
            BLOCK_ENTITIES.register("primitive_water_wheel", () -> new BlockEntityType<>(
                    br.com.quantumindustries.block.entity.PrimitiveWaterWheelBlockEntity::new, Set.of(PRIMITIVE_WATER_WHEEL.get()), false));
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.MechanicalShaftBlockEntity>> MECHANICAL_SHAFT_ENTITY =
            BLOCK_ENTITIES.register("mechanical_shaft_block", () -> new BlockEntityType<>(
                    br.com.quantumindustries.block.entity.MechanicalShaftBlockEntity::new, Set.of(MECHANICAL_SHAFT_BLOCK.get()), false));
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.MechanicalSawmillBlockEntity>> MECHANICAL_SAWMILL_ENTITY =
            BLOCK_ENTITIES.register("mechanical_sawmill", () -> new BlockEntityType<>(
                    br.com.quantumindustries.block.entity.MechanicalSawmillBlockEntity::new, Set.of(MECHANICAL_SAWMILL.get()), false));

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
