package br.com.quantumindustries.core;

import br.com.quantumindustries.block.ElectromechanicalPressBlock;
import br.com.quantumindustries.block.VoltaicPileBlock;
import br.com.quantumindustries.block.LeadAcidAccumulatorBlock;
import br.com.quantumindustries.block.ResearchTableBlock;
import br.com.quantumindustries.block.entity.ElectromechanicalPressBlockEntity;
import br.com.quantumindustries.block.entity.VoltaicPileBlockEntity;
import br.com.quantumindustries.block.entity.LeadAcidAccumulatorBlockEntity;
import br.com.quantumindustries.block.entity.ResearchTableBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.bus.api.IEventBus;

import java.util.Set;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("quantumindustries");
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, "quantumindustries");
    public static final DeferredBlock<br.com.quantumindustries.block.ElectricalNetworkAnalyzerBlock> ELECTRICAL_NETWORK_ANALYZER =
            BLOCKS.registerBlock("electrical_network_analyzer",br.com.quantumindustries.block.ElectricalNetworkAnalyzerBlock::new,props->props.strength(2.5F).noOcclusion());
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>,BlockEntityType<br.com.quantumindustries.block.entity.ElectricalNetworkAnalyzerBlockEntity>> ELECTRICAL_NETWORK_ANALYZER_ENTITY =
            BLOCK_ENTITIES.register("electrical_network_analyzer",()->new BlockEntityType<>(br.com.quantumindustries.block.entity.ElectricalNetworkAnalyzerBlockEntity::new,Set.of(ELECTRICAL_NETWORK_ANALYZER.get()),false));
    public static final DeferredBlock<br.com.quantumindustries.block.IndustrialConversionBlock> ELECTROMAGNETIC_GENERATOR =
            BLOCKS.registerBlock("electromagnetic_generator", props -> new br.com.quantumindustries.block.IndustrialConversionBlock(props, br.com.quantumindustries.block.IndustrialConversionBlock.Kind.GENERATOR), props -> props.strength(3.0F).noOcclusion());
    public static final DeferredBlock<br.com.quantumindustries.block.IndustrialConversionBlock> ELECTRIC_MOTOR =
            BLOCKS.registerBlock("electric_motor", props -> new br.com.quantumindustries.block.IndustrialConversionBlock(props, br.com.quantumindustries.block.IndustrialConversionBlock.Kind.MOTOR), props -> props.strength(3.0F).noOcclusion());
    public static final DeferredBlock<br.com.quantumindustries.block.IndustrialConversionBlock> STEAM_BOILER =
            BLOCKS.registerBlock("steam_boiler", props -> new br.com.quantumindustries.block.IndustrialConversionBlock(props, br.com.quantumindustries.block.IndustrialConversionBlock.Kind.BOILER), props -> props.strength(3.0F).noOcclusion());
    public static final DeferredBlock<br.com.quantumindustries.block.IndustrialConversionBlock> STEAM_ENGINE =
            BLOCKS.registerBlock("steam_engine", props -> new br.com.quantumindustries.block.IndustrialConversionBlock(props, br.com.quantumindustries.block.IndustrialConversionBlock.Kind.STEAM_ENGINE), props -> props.strength(3.0F).noOcclusion());
    public static final DeferredBlock<br.com.quantumindustries.block.IndustrialConversionBlock> THERMOELECTRIC_GENERATOR =
            BLOCKS.registerBlock("thermoelectric_generator", props -> new br.com.quantumindustries.block.IndustrialConversionBlock(props, br.com.quantumindustries.block.IndustrialConversionBlock.Kind.THERMOELECTRIC), props -> props.strength(3.0F).noOcclusion());
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.RotaryConversionBlockEntity>> ROTARY_CONVERSION_ENTITY =
            BLOCK_ENTITIES.register("rotary_conversion", () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.RotaryConversionBlockEntity::new, Set.of(ELECTROMAGNETIC_GENERATOR.get(), ELECTRIC_MOTOR.get(), STEAM_ENGINE.get()), false));
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.SteamBoilerBlockEntity>> STEAM_BOILER_ENTITY =
            BLOCK_ENTITIES.register("steam_boiler", () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.SteamBoilerBlockEntity::new, Set.of(STEAM_BOILER.get()), false));
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.ThermoelectricGeneratorBlockEntity>> THERMOELECTRIC_GENERATOR_ENTITY =
            BLOCK_ENTITIES.register("thermoelectric_generator", () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.ThermoelectricGeneratorBlockEntity::new, Set.of(THERMOELECTRIC_GENERATOR.get()), false));

    public static final DeferredBlock<br.com.quantumindustries.block.MechanicalConveyorBlock> MECHANICAL_CONVEYOR =
            BLOCKS.registerBlock("mechanical_conveyor", br.com.quantumindustries.block.MechanicalConveyorBlock::new,
                    props -> props.strength(1.8F).noOcclusion());
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.MechanicalConveyorBlockEntity>> MECHANICAL_CONVEYOR_ENTITY =
            BLOCK_ENTITIES.register("mechanical_conveyor", () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.MechanicalConveyorBlockEntity::new, Set.of(MECHANICAL_CONVEYOR.get()), false));

    public static final DeferredBlock<br.com.quantumindustries.block.CompactGearboxBlock> COMPACT_GEARBOX =
            BLOCKS.registerBlock("compact_gearbox", br.com.quantumindustries.block.CompactGearboxBlock::new,
                    props -> props.strength(2.2F).noOcclusion());
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.CompactGearboxBlockEntity>> COMPACT_GEARBOX_ENTITY =
            BLOCK_ENTITIES.register("compact_gearbox", () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.CompactGearboxBlockEntity::new, Set.of(COMPACT_GEARBOX.get()), false));

    public static final DeferredBlock<br.com.quantumindustries.block.SmallCogwheelBlock> SMALL_COGWHEEL =
            BLOCKS.registerBlock("small_cogwheel", br.com.quantumindustries.block.SmallCogwheelBlock::new, p -> p.strength(2.5F).noOcclusion());
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.CogwheelBlockEntity>> SMALL_COGWHEEL_ENTITY =
            BLOCK_ENTITIES.register("small_cogwheel", () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.CogwheelBlockEntity::new, Set.of(SMALL_COGWHEEL.get()), false));
    public static final DeferredBlock<br.com.quantumindustries.block.LargeCogwheelBlock> LARGE_COGWHEEL =
            BLOCKS.registerBlock("large_cogwheel", br.com.quantumindustries.block.LargeCogwheelBlock::new, p -> p.strength(2.5F).noOcclusion());
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.CogwheelBlockEntity>> LARGE_COGWHEEL_ENTITY =
            BLOCK_ENTITIES.register("large_cogwheel", () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.CogwheelBlockEntity::new, Set.of(LARGE_COGWHEEL.get()), false));
    public static final DeferredBlock<br.com.quantumindustries.block.IronFlywheelBlock> IRON_FLYWHEEL =
            BLOCKS.registerBlock("iron_flywheel", br.com.quantumindustries.block.IronFlywheelBlock::new, p -> p.strength(2.5F).noOcclusion());
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.IronFlywheelBlockEntity>> IRON_FLYWHEEL_ENTITY =
            BLOCK_ENTITIES.register("iron_flywheel", () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.IronFlywheelBlockEntity::new, Set.of(IRON_FLYWHEEL.get()), false));
    public static final DeferredBlock<br.com.quantumindustries.block.GravityWinchBlock> GRAVITY_WINCH =
            BLOCKS.registerBlock("gravity_winch", br.com.quantumindustries.block.GravityWinchBlock::new, p -> p.strength(2.5F).noOcclusion());
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.GravityWinchBlockEntity>> GRAVITY_WINCH_ENTITY =
            BLOCK_ENTITIES.register("gravity_winch", () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.GravityWinchBlockEntity::new, Set.of(GRAVITY_WINCH.get()), false));
    public static final DeferredBlock<br.com.quantumindustries.block.MainspringDrumBlock> MAINSPRING_DRUM =
            BLOCKS.registerBlock("mainspring_drum", br.com.quantumindustries.block.MainspringDrumBlock::new, p -> p.strength(2.5F).noOcclusion());
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.MainspringDrumBlockEntity>> MAINSPRING_DRUM_ENTITY =
            BLOCK_ENTITIES.register("mainspring_drum", () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.MainspringDrumBlockEntity::new, Set.of(MAINSPRING_DRUM.get()), false));
    public static final DeferredBlock<br.com.quantumindustries.block.MechanicalPulleyBlock> MECHANICAL_PULLEY =
            BLOCKS.registerBlock("mechanical_pulley", br.com.quantumindustries.block.MechanicalPulleyBlock::new, p -> p.strength(2.5F).noOcclusion());
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.MechanicalPulleyBlockEntity>> MECHANICAL_PULLEY_ENTITY =
            BLOCK_ENTITIES.register("mechanical_pulley", () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.MechanicalPulleyBlockEntity::new, Set.of(MECHANICAL_PULLEY.get()), false));
    public static final DeferredBlock<br.com.quantumindustries.block.MediumWaterWheelBlock> MEDIUM_WATER_WHEEL =
            BLOCKS.registerBlock("medium_water_wheel", br.com.quantumindustries.block.MediumWaterWheelBlock::new, p -> p.strength(2.5F).noOcclusion());
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.MediumWaterWheelBlockEntity>> MEDIUM_WATER_WHEEL_ENTITY =
            BLOCK_ENTITIES.register("medium_water_wheel", () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.MediumWaterWheelBlockEntity::new, Set.of(MEDIUM_WATER_WHEEL.get()), false));
    public static final DeferredBlock<br.com.quantumindustries.block.LargeWaterWheelBlock> LARGE_WATER_WHEEL =
            BLOCKS.registerBlock("large_water_wheel", br.com.quantumindustries.block.LargeWaterWheelBlock::new, p -> p.strength(2.5F).noOcclusion());
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.LargeWaterWheelBlockEntity>> LARGE_WATER_WHEEL_ENTITY =
            BLOCK_ENTITIES.register("large_water_wheel", () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.LargeWaterWheelBlockEntity::new, Set.of(LARGE_WATER_WHEEL.get()), false));

    public static final DeferredBlock<br.com.quantumindustries.block.EmpiricalAssayBenchBlock> EMPIRICAL_ASSAY_BENCH = BLOCKS.registerBlock("empirical_assay_bench", br.com.quantumindustries.block.EmpiricalAssayBenchBlock::new, p -> p.strength(2.5F).noOcclusion());

    public static final DeferredBlock<br.com.quantumindustries.block.QuantumVaultBlock> QUANTUM_VAULT =
            BLOCKS.registerBlock("quantum_vault", br.com.quantumindustries.block.QuantumVaultBlock::new,
                    properties -> properties.mapColor(MapColor.COLOR_CYAN).strength(4.0F, 6.0F).sound(SoundType.NETHERITE_BLOCK).lightLevel(s -> 5).noOcclusion());

    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.QuantumVaultBlockEntity>> QUANTUM_VAULT_ENTITY =
            BLOCK_ENTITIES.register("quantum_vault",
                    () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.QuantumVaultBlockEntity::new, Set.of(QUANTUM_VAULT.get()), false));

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

    public static final DeferredBlock<br.com.quantumindustries.block.ManualGrainMillBlock> MANUAL_GRAIN_MILL =
            BLOCKS.registerBlock("manual_grain_mill", br.com.quantumindustries.block.ManualGrainMillBlock::new,
                    properties -> properties.strength(2.5F).noOcclusion());
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.ManualGrainMillBlockEntity>> MANUAL_GRAIN_MILL_ENTITY =
            BLOCK_ENTITIES.register("manual_grain_mill", () -> new BlockEntityType<>(
                    br.com.quantumindustries.block.entity.ManualGrainMillBlockEntity::new,
                    Set.of(MANUAL_GRAIN_MILL.get()), false));

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

    public static final DeferredBlock<br.com.quantumindustries.block.CopperCableBlock> COPPER_BUSBAR =
            BLOCKS.registerBlock("copper_busbar",
                    props -> new br.com.quantumindustries.block.CopperCableBlock(props, false),
                    props -> props.strength(0.8F).noOcclusion());
    public static final DeferredBlock<br.com.quantumindustries.block.CopperCableBlock> INSULATED_CONDUIT =
            BLOCKS.registerBlock("insulated_conduit",
                    props -> new br.com.quantumindustries.block.CopperCableBlock(props, true),
                    props -> props.strength(0.8F).noOcclusion());

    public static final DeferredBlock<br.com.quantumindustries.block.IndustrialCircuitSwitchBlock> INDUSTRIAL_CIRCUIT_SWITCH =
            BLOCKS.registerBlock("industrial_circuit_switch", br.com.quantumindustries.block.IndustrialCircuitSwitchBlock::new,
                    props -> props.strength(2.0F).noOcclusion());

    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.CopperCableBlockEntity>> COPPER_CABLE_ENTITY =
            BLOCK_ENTITIES.register("copper_cable",
                    () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.CopperCableBlockEntity::new,
                            Set.of(COPPER_BUSBAR.get(), INSULATED_CONDUIT.get(), INDUSTRIAL_CIRCUIT_SWITCH.get()), false));

    public static final DeferredBlock<br.com.quantumindustries.block.PneumaticTubeBlock> PNEUMATIC_TUBE =
            BLOCKS.registerBlock("pneumatic_tube",br.com.quantumindustries.block.PneumaticTubeBlock::new,
                    p->p.strength(1.0F).noOcclusion());
    public static final DeferredBlock<br.com.quantumindustries.block.BellowsEjectorBlock> BELLOWS_EJECTOR =
            BLOCKS.registerBlock("bellows_ejector",br.com.quantumindustries.block.BellowsEjectorBlock::new,
                    p->p.strength(2.0F).noOcclusion());
    public static final DeferredBlock<br.com.quantumindustries.block.GravitationalSorterBlock> GRAVITATIONAL_SORTER =
            BLOCKS.registerBlock("gravitational_sorter",br.com.quantumindustries.block.GravitationalSorterBlock::new,
                    p->p.strength(2.0F).noOcclusion());
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.PneumaticTubeBlockEntity>> PNEUMATIC_TUBE_ENTITY =
            BLOCK_ENTITIES.register("pneumatic_tube",()->new BlockEntityType<>(
                    br.com.quantumindustries.block.entity.PneumaticTubeBlockEntity::new,Set.of(PNEUMATIC_TUBE.get()),false));
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.BellowsEjectorBlockEntity>> BELLOWS_EJECTOR_ENTITY =
            BLOCK_ENTITIES.register("bellows_ejector",()->new BlockEntityType<>(
                    br.com.quantumindustries.block.entity.BellowsEjectorBlockEntity::new,Set.of(BELLOWS_EJECTOR.get()),false));
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.GravitationalSorterBlockEntity>> GRAVITATIONAL_SORTER_ENTITY =
            BLOCK_ENTITIES.register("gravitational_sorter",()->new BlockEntityType<>(
                    br.com.quantumindustries.block.entity.GravitationalSorterBlockEntity::new,Set.of(GRAVITATIONAL_SORTER.get()),false));

    public static final DeferredBlock<br.com.quantumindustries.block.FluidPipeBlock> FLUID_PIPE =
            BLOCKS.registerBlock("fluid_pipe",p->new br.com.quantumindustries.block.FluidPipeBlock(p,false),
                    p->p.strength(1.0F).noOcclusion());
    public static final DeferredBlock<br.com.quantumindustries.block.FluidPipeBlock> GLASS_PIPE =
            BLOCKS.registerBlock("glass_pipe",p->new br.com.quantumindustries.block.FluidPipeBlock(p,true),
                    p->p.strength(1.0F).noOcclusion());
    public static final DeferredBlock<br.com.quantumindustries.block.PeristalticPumpBlock> PERISTALTIC_PUMP =
            BLOCKS.registerBlock("peristaltic_pump",br.com.quantumindustries.block.PeristalticPumpBlock::new,
                    p->p.strength(2.5F).noOcclusion());
    public static final DeferredBlock<br.com.quantumindustries.block.SettlingVatBlock> SETTLING_VAT =
            BLOCKS.registerBlock("settling_vat",br.com.quantumindustries.block.SettlingVatBlock::new,
                    p->p.strength(3.0F).noOcclusion());
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.FluidPipeBlockEntity>> FLUID_PIPE_ENTITY =
            BLOCK_ENTITIES.register("fluid_pipe",()->new BlockEntityType<>(
                    br.com.quantumindustries.block.entity.FluidPipeBlockEntity::new,
                    Set.of(FLUID_PIPE.get(),GLASS_PIPE.get()),false));
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.PeristalticPumpBlockEntity>> PERISTALTIC_PUMP_ENTITY =
            BLOCK_ENTITIES.register("peristaltic_pump",()->new BlockEntityType<>(
                    br.com.quantumindustries.block.entity.PeristalticPumpBlockEntity::new,Set.of(PERISTALTIC_PUMP.get()),false));
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.SettlingVatBlockEntity>> SETTLING_VAT_ENTITY =
            BLOCK_ENTITIES.register("settling_vat",()->new BlockEntityType<>(
                    br.com.quantumindustries.block.entity.SettlingVatBlockEntity::new,Set.of(SETTLING_VAT.get()),false));

    public static final DeferredBlock<br.com.quantumindustries.block.PiezoSwitchBlock> PIEZO_SWITCH =
            BLOCKS.registerBlock("piezo_switch", br.com.quantumindustries.block.PiezoSwitchBlock::new,
                    props -> props.strength(2.0F).noOcclusion());
    public static final DeferredBlock<br.com.quantumindustries.block.LoadRelayBlock> LOAD_RELAY =
            BLOCKS.registerBlock("load_relay", br.com.quantumindustries.block.LoadRelayBlock::new,
                    props -> props.strength(2.0F).noOcclusion());
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.PiezoSwitchBlockEntity>> PIEZO_SWITCH_ENTITY =
            BLOCK_ENTITIES.register("piezo_switch",
                    () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.PiezoSwitchBlockEntity::new,
                            Set.of(PIEZO_SWITCH.get()), false));
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.LoadRelayBlockEntity>> LOAD_RELAY_ENTITY =
            BLOCK_ENTITIES.register("load_relay",
                    () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.LoadRelayBlockEntity::new,
                            Set.of(LOAD_RELAY.get()), false));

    public static final DeferredBlock<br.com.quantumindustries.block.BellowsBrazierBlock> BELLOWS_BRAZIER =
            BLOCKS.registerBlock("bellows_brazier", br.com.quantumindustries.block.BellowsBrazierBlock::new,
                    props -> props.strength(2.4F).noOcclusion().lightLevel(state -> state.getValueOrElse(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT, false) ? 10 : 0));
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.BellowsBrazierBlockEntity>> BELLOWS_BRAZIER_ENTITY =
            BLOCK_ENTITIES.register("bellows_brazier",
                    () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.BellowsBrazierBlockEntity::new,
                            Set.of(BELLOWS_BRAZIER.get()), false));

    public static final DeferredBlock<br.com.quantumindustries.block.WireDrawingBenchBlock> WIRE_DRAWING_BENCH =
            BLOCKS.registerBlock("wire_drawing_bench",
                    br.com.quantumindustries.block.WireDrawingBenchBlock::new,
                    props -> props.strength(2.4F).noOcclusion().lightLevel(state -> state.getValueOrElse(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT, false) ? 5 : 0));

    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.WireDrawingBenchBlockEntity>> WIRE_DRAWING_BENCH_ENTITY =
            BLOCK_ENTITIES.register("wire_drawing_bench",
                    () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.WireDrawingBenchBlockEntity::new,
                            Set.of(WIRE_DRAWING_BENCH.get()), false));

    public static final DeferredBlock<VoltaicPileBlock> VOLTAIC_PILE = BLOCKS.registerBlock("voltaic_pile",
            VoltaicPileBlock::new, properties -> properties.strength(2.5F).noOcclusion());
    public static final DeferredBlock<LeadAcidAccumulatorBlock> LEAD_ACID_ACCUMULATOR = BLOCKS.registerBlock("lead_acid_accumulator",
            LeadAcidAccumulatorBlock::new, properties -> properties.strength(3.0F).noOcclusion());

    public static final DeferredBlock<ElectromechanicalPressBlock> ELECTROMECHANICAL_PRESS = BLOCKS.registerBlock("electromechanical_press",
            ElectromechanicalPressBlock::new, properties -> properties.strength(3.0F).noOcclusion());

    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.EmpiricalAssayBenchBlockEntity>> EMPIRICAL_ASSAY_ENTITY =
            BLOCK_ENTITIES.register("empirical_assay_bench",
                    () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.EmpiricalAssayBenchBlockEntity::new, Set.of(EMPIRICAL_ASSAY_BENCH.get()), false));

    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.ManualDynamoBlockEntity>> MANUAL_DYNAMO_ENTITY =
            BLOCK_ENTITIES.register("manual_dynamo",
                    () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.ManualDynamoBlockEntity::new, Set.of(MANUAL_DYNAMO.get()), false));

    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<VoltaicPileBlockEntity>> VOLTAIC_PILE_ENTITY =
            BLOCK_ENTITIES.register("voltaic_pile",
                    () -> new BlockEntityType<>(VoltaicPileBlockEntity::new, Set.of(VOLTAIC_PILE.get()), false));
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<LeadAcidAccumulatorBlockEntity>> LEAD_ACID_ACCUMULATOR_ENTITY =
            BLOCK_ENTITIES.register("lead_acid_accumulator",
                    () -> new BlockEntityType<>(LeadAcidAccumulatorBlockEntity::new, Set.of(LEAD_ACID_ACCUMULATOR.get()), false));

    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectromechanicalPressBlockEntity>> ELECTROMECHANICAL_PRESS_ENTITY =
            BLOCK_ENTITIES.register("electromechanical_press",
                    () -> new BlockEntityType<>(ElectromechanicalPressBlockEntity::new, Set.of(ELECTROMECHANICAL_PRESS.get()), false));

    public static final DeferredBlock<br.com.quantumindustries.block.BellowsAlloyKilnBlock> BELLOWS_ALLOY_KILN =
            BLOCKS.registerBlock("bellows_alloy_kiln", br.com.quantumindustries.block.BellowsAlloyKilnBlock::new,
                    props -> props.strength(2.6F).noOcclusion());
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.BellowsAlloyKilnBlockEntity>> BELLOWS_ALLOY_KILN_ENTITY =
            BLOCK_ENTITIES.register("bellows_alloy_kiln",
                    () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.BellowsAlloyKilnBlockEntity::new,
                            Set.of(BELLOWS_ALLOY_KILN.get()), false));

    public static final DeferredBlock<br.com.quantumindustries.block.ElectricSawmillBlock> ELECTRIC_SAWMILL =
            BLOCKS.registerBlock("electric_sawmill", br.com.quantumindustries.block.ElectricSawmillBlock::new,
                    props -> props.strength(3.0F).noOcclusion().lightLevel(state -> state.getValueOrElse(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT, false) ? 2 : 0));
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.ElectricSawmillBlockEntity>> ELECTRIC_SAWMILL_ENTITY =
            BLOCK_ENTITIES.register("electric_sawmill",
                    () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.ElectricSawmillBlockEntity::new,
                            Set.of(ELECTRIC_SAWMILL.get()), false));

    public static final DeferredBlock<br.com.quantumindustries.block.ElectricAlloySmelterBlock> ELECTRIC_ALLOY_SMELTER =
            BLOCKS.registerBlock("electric_alloy_smelter", br.com.quantumindustries.block.ElectricAlloySmelterBlock::new,
                    props -> props.strength(3.0F).noOcclusion().lightLevel(state -> state.getValueOrElse(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT, false) ? 12 : 0));
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.ElectricAlloySmelterBlockEntity>> ELECTRIC_ALLOY_SMELTER_ENTITY =
            BLOCK_ENTITIES.register("electric_alloy_smelter",
                    () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.ElectricAlloySmelterBlockEntity::new,
                            Set.of(ELECTRIC_ALLOY_SMELTER.get()), false));

    public static final DeferredBlock<br.com.quantumindustries.block.ElectricFurnaceBlock> ELECTRIC_FURNACE =
            BLOCKS.registerBlock("electric_furnace", br.com.quantumindustries.block.ElectricFurnaceBlock::new,
                    props -> props.strength(3.0F).noOcclusion().lightLevel(state -> state.getValueOrElse(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT, false) ? 12 : 0));
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE_ENTITY =
            BLOCK_ENTITIES.register("electric_furnace",
                    () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.ElectricFurnaceBlockEntity::new,
                            Set.of(ELECTRIC_FURNACE.get()), false));

    public static final DeferredBlock<br.com.quantumindustries.block.JawCrusherBlock> JAW_CRUSHER =
            BLOCKS.registerBlock("jaw_crusher", br.com.quantumindustries.block.JawCrusherBlock::new,
                    props -> props.strength(2.8F).noOcclusion());
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<br.com.quantumindustries.block.entity.JawCrusherBlockEntity>> JAW_CRUSHER_ENTITY =
            BLOCK_ENTITIES.register("jaw_crusher",
                    () -> new BlockEntityType<>(br.com.quantumindustries.block.entity.JawCrusherBlockEntity::new,
                            Set.of(JAW_CRUSHER.get()), false));

    public static final java.util.Map<String, DeferredBlock<br.com.quantumindustries.block.QuantumOreBlock>> STONE_ORES = new java.util.LinkedHashMap<>();
    public static final java.util.Map<String, DeferredBlock<br.com.quantumindustries.block.QuantumOreBlock>> DEEPSLATE_ORES = new java.util.LinkedHashMap<>();

    static {
        for (var mineral : br.com.quantumindustries.material.MineralCatalog.MINERALS) {
            STONE_ORES.put(mineral.id(), BLOCKS.registerBlock(mineral.stoneOreId(),
                    props -> br.com.quantumindustries.block.QuantumOreBlock.createStone(mineral, props)));
            DEEPSLATE_ORES.put(mineral.id(), BLOCKS.registerBlock(mineral.deepslateOreId(),
                    props -> br.com.quantumindustries.block.QuantumOreBlock.createDeepslate(mineral, props)));
        }
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
    }
}
