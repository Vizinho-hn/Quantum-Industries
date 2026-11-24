package br.com.quantumindustries;

import net.minecraft.network.chat.Component;

import br.com.quantumindustries.core.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.bus.api.IEventBus;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(QuantumIndustries.MODID);

    public static final DeferredItem<BlockItem> ELECTROMAGNETIC_GENERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.ELECTROMAGNETIC_GENERATOR);
    public static final DeferredItem<BlockItem> ELECTRIC_MOTOR = ITEMS.registerSimpleBlockItem(ModBlocks.ELECTRIC_MOTOR);
    public static final DeferredItem<BlockItem> STEAM_BOILER = ITEMS.registerSimpleBlockItem(ModBlocks.STEAM_BOILER);
    public static final DeferredItem<BlockItem> STEAM_ENGINE = ITEMS.registerSimpleBlockItem(ModBlocks.STEAM_ENGINE);
    public static final DeferredItem<BlockItem> THERMOELECTRIC_GENERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.THERMOELECTRIC_GENERATOR);
    public static final DeferredItem<BlockItem> MECHANICAL_CONVEYOR = ITEMS.registerSimpleBlockItem(ModBlocks.MECHANICAL_CONVEYOR);
    public static final DeferredItem<BlockItem> COMPACT_GEARBOX = ITEMS.registerSimpleBlockItem(ModBlocks.COMPACT_GEARBOX);
    public static final DeferredItem<BlockItem> INDUSTRIAL_CIRCUIT_SWITCH = ITEMS.registerSimpleBlockItem(ModBlocks.INDUSTRIAL_CIRCUIT_SWITCH);


    public static final DeferredBlock<br.com.quantumindustries.block.ResearchTableBlock> RESEARCH_TABLE = ModBlocks.RESEARCH_TABLE;

    public static final DeferredItem<BlockItem> ELECTROMAGNETIC_GENERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.ELECTROMAGNETIC_GENERATOR);
    public static final DeferredItem<BlockItem> ELECTRIC_MOTOR = ITEMS.registerSimpleBlockItem(ModBlocks.ELECTRIC_MOTOR);
    public static final DeferredItem<BlockItem> STEAM_BOILER = ITEMS.registerSimpleBlockItem(ModBlocks.STEAM_BOILER);
    public static final DeferredItem<BlockItem> STEAM_ENGINE = ITEMS.registerSimpleBlockItem(ModBlocks.STEAM_ENGINE);
    public static final DeferredItem<BlockItem> THERMOELECTRIC_GENERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.THERMOELECTRIC_GENERATOR);
    public static final DeferredItem<BlockItem> MECHANICAL_CONVEYOR = ITEMS.registerSimpleBlockItem(ModBlocks.MECHANICAL_CONVEYOR);
    public static final DeferredItem<BlockItem> COMPACT_GEARBOX = ITEMS.registerSimpleBlockItem(ModBlocks.COMPACT_GEARBOX);
    public static final DeferredItem<BlockItem> INDUSTRIAL_CIRCUIT_SWITCH = ITEMS.registerSimpleBlockItem(ModBlocks.INDUSTRIAL_CIRCUIT_SWITCH);
    public static final DeferredItem<BlockItem> SMALL_COGWHEEL = ITEMS.registerSimpleBlockItem(ModBlocks.SMALL_COGWHEEL);
    public static final DeferredItem<BlockItem> LARGE_COGWHEEL = ITEMS.registerSimpleBlockItem(ModBlocks.LARGE_COGWHEEL);
    public static final DeferredItem<BlockItem> IRON_FLYWHEEL = ITEMS.registerSimpleBlockItem(ModBlocks.IRON_FLYWHEEL);
    public static final DeferredItem<BlockItem> GRAVITY_WINCH = ITEMS.registerSimpleBlockItem(ModBlocks.GRAVITY_WINCH);
    public static final DeferredItem<BlockItem> MAINSPRING_DRUM = ITEMS.registerSimpleBlockItem(ModBlocks.MAINSPRING_DRUM);
    public static final DeferredItem<BlockItem> MECHANICAL_PULLEY = ITEMS.registerSimpleBlockItem(ModBlocks.MECHANICAL_PULLEY);
    public static final DeferredItem<BlockItem> MEDIUM_WATER_WHEEL = ITEMS.registerSimpleBlockItem(ModBlocks.MEDIUM_WATER_WHEEL);
    public static final DeferredItem<BlockItem> LARGE_WATER_WHEEL = ITEMS.registerSimpleBlockItem(ModBlocks.LARGE_WATER_WHEEL);
    public static final DeferredItem<br.com.quantumindustries.item.DriveBeltItem> DRIVE_BELT = ITEMS.registerItem("drive_belt", br.com.quantumindustries.item.DriveBeltItem::new);
    public static final DeferredItem<BlockItem> SCIENTIFIC_ANALYZER_TIER_1 =
            ITEMS.registerSimpleBlockItem(ModBlocks.RESEARCH_TABLE_TIER_1);
    public static final DeferredItem<BlockItem> SCIENTIFIC_ANALYZER = ITEMS.registerSimpleBlockItem(RESEARCH_TABLE);
    public static final DeferredItem<BlockItem> EMPIRICAL_ASSAY_BENCH = ITEMS.registerSimpleBlockItem(ModBlocks.EMPIRICAL_ASSAY_BENCH);
    public static final DeferredItem<BlockItem> QUANTUM_VAULT = ITEMS.registerSimpleBlockItem(ModBlocks.QUANTUM_VAULT);

    public static final DeferredItem<Item> QUANTUM_GUIDEBOOK = ITEMS.registerItem("quantum_guidebook",
            br.com.quantumindustries.item.QuantumGuidebookItem::new, properties -> properties.stacksTo(1));

    // Ferramenta de teste (Sonda)
    public static final DeferredItem<Item> TEST_PROBES = ITEMS.registerItem("test_probes",
            br.com.quantumindustries.item.TestProbesItem::new, properties -> properties.durability(100));
    public static final DeferredItem<Item> CONFIGURATION_WRENCH = ITEMS.registerItem("configuration_wrench",
            br.com.quantumindustries.item.ConfigurationWrenchItem::new, properties -> properties.stacksTo(1));

    // Dados de pesquisa (output da bancada)
    public static final DeferredItem<Item> RESEARCH_DATA = ITEMS.registerItem("research_data", br.com.quantumindustries.research.ResearchReportItem::new);
    public static final DeferredItem<Item> COPPER_WIRE = ITEMS.registerItem("copper_wire",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.copper_wire.title"),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.copper_wire.spec_0"), Component.translatable("tooltip.quantumindustries.copper_wire.spec_1")),
                    properties,
                    () -> ModItems.COPPER_BUSBAR.get()));

    public static final DeferredItem<Item> MANUAL_HAMMER = ITEMS.registerItem("manual_hammer",
            br.com.quantumindustries.item.ManualHammerItem::new, properties -> properties.durability(128));

    public static final DeferredItem<Item> IRON_SHEET = ITEMS.registerItem("iron_sheet",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.iron_sheet.title"),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.iron_sheet.spec_0"), Component.translatable("tooltip.quantumindustries.iron_sheet.spec_1")),
                    properties));

    public static final DeferredItem<Item> COPPER_SHEET = ITEMS.registerItem("copper_sheet",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.copper_sheet.title"),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.copper_sheet.spec_0"), Component.translatable("tooltip.quantumindustries.copper_sheet.spec_1")),
                    properties));

    public static final DeferredItem<Item> IRON_GEAR = ITEMS.registerItem("iron_gear",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.iron_gear.title"),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.iron_gear.spec_0"), Component.translatable("tooltip.quantumindustries.iron_gear.spec_1")),
                    properties));

    public static final DeferredItem<Item> MECHANICAL_SHAFT = ITEMS.registerItem("mechanical_shaft",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.mechanical_shaft.title"),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.mechanical_shaft.spec_0"), Component.translatable("tooltip.quantumindustries.mechanical_shaft.spec_1")),
                    properties));

    public static final DeferredItem<Item> HAND_CRANK = ITEMS.registerItem("hand_crank",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.hand_crank.title"),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.hand_crank.spec_0"), Component.translatable("tooltip.quantumindustries.hand_crank.spec_1")),
                    properties));

    public static final DeferredItem<Item> INSULATED_COPPER_WIRE = ITEMS.registerItem("insulated_copper_wire",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.insulated_copper_wire.title"),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.insulated_copper_wire.spec_0"), Component.translatable("tooltip.quantumindustries.insulated_copper_wire.spec_1")),
                    properties,
                    () -> ModItems.INSULATED_CONDUIT.get()));

    public static final DeferredItem<Item> COPPER_COIL = ITEMS.registerItem("copper_coil",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.copper_coil.title"),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.copper_coil.spec_0"), Component.translatable("tooltip.quantumindustries.copper_coil.spec_1")),
                    properties));

    // Era II - Lead-Acid Accumulator components
    public static final DeferredItem<Item> LEAD_SHEET = ITEMS.registerItem("lead_sheet",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.lead_sheet.title"),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.lead_sheet.spec_0"), Component.translatable("tooltip.quantumindustries.lead_sheet.spec_1")),
                    properties));
    public static final DeferredItem<Item> SULFURIC_ACID = ITEMS.registerItem("sulfuric_acid",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.sulfuric_acid.title"),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.sulfuric_acid.spec_0"), Component.translatable("tooltip.quantumindustries.sulfuric_acid.spec_1")),
                    properties));
    public static final DeferredItem<Item> EBONITE_ROD = ITEMS.registerItem("ebonite_rod",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.ebonite_rod.title"),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.ebonite_rod.spec_0"), Component.translatable("tooltip.quantumindustries.ebonite_rod.spec_1")),
                    properties));

    public static final DeferredItem<Item> ELECTROMECHANICAL_FRAME = ITEMS.registerItem("electromechanical_frame",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.electromechanical_frame.title"),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.electromechanical_frame.spec_0"), Component.translatable("tooltip.quantumindustries.electromechanical_frame.spec_1")),
                    properties));
    public static final DeferredItem<BlockItem> FORMING_BENCH = ITEMS.registerSimpleBlockItem(ModBlocks.FORMING_BENCH);
    public static final DeferredItem<BlockItem> MANUAL_GRAIN_MILL = ITEMS.registerSimpleBlockItem(ModBlocks.MANUAL_GRAIN_MILL);
    public static final DeferredItem<Item> COPPER_HEATSINK = ITEMS.registerSimpleItem("copper_heatsink");
    public static final DeferredItem<Item> ALUMINUM_HEATSINK = ITEMS.registerSimpleItem("aluminum_heatsink");
    public static final DeferredItem<Item> THERMAL_FAN = ITEMS.registerSimpleItem("thermal_fan");
    public static final DeferredItem<Item> PERMANENT_MAGNET = ITEMS.registerSimpleItem("permanent_magnet");
    public static final DeferredItem<Item> LAMINATED_IRON_CORE = ITEMS.registerSimpleItem("laminated_iron_core");
    public static final DeferredItem<Item> EXCITATION_COIL = ITEMS.registerSimpleItem("excitation_coil");
    public static final DeferredItem<Item> ELECTROMAGNETIC_ROTOR = ITEMS.registerSimpleItem("electromagnetic_rotor");
    public static final DeferredItem<Item> GENERATOR_STATOR = ITEMS.registerSimpleItem("generator_stator");
    public static final DeferredItem<Item> PRIMITIVE_RECTIFIER = ITEMS.registerSimpleItem("primitive_rectifier");
    public static final DeferredItem<Item> VOLTAGE_REGULATOR = ITEMS.registerSimpleItem("voltage_regulator");
    public static final DeferredItem<Item> FLOUR = ITEMS.registerSimpleItem("flour");
    public static final DeferredItem<Item> BREAD_DOUGH = ITEMS.registerSimpleItem("bread_dough");
    public static final DeferredItem<Item> RUSTIC_BREAD = ITEMS.registerItem("rustic_bread", Item::new,
            properties -> properties.food(new net.minecraft.world.food.FoodProperties.Builder()
                    .nutrition(5).saturationModifier(0.6F).build()));

    public static final DeferredItem<BlockItem> MECHANICAL_SAWMILL = ITEMS.registerSimpleBlockItem(ModBlocks.MECHANICAL_SAWMILL);
    public static final DeferredItem<BlockItem> MECHANICAL_SHAFT_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.MECHANICAL_SHAFT_BLOCK);
    public static final DeferredItem<BlockItem> PRIMITIVE_WATER_WHEEL = ITEMS.registerSimpleBlockItem(ModBlocks.PRIMITIVE_WATER_WHEEL);
    public static final DeferredItem<BlockItem> MANUAL_DYNAMO = ITEMS.registerSimpleBlockItem(ModBlocks.MANUAL_DYNAMO);
    public static final DeferredItem<BlockItem> COPPER_BUSBAR = ITEMS.registerSimpleBlockItem(ModBlocks.COPPER_BUSBAR);
    public static final DeferredItem<BlockItem> INSULATED_CONDUIT = ITEMS.registerSimpleBlockItem(ModBlocks.INSULATED_CONDUIT);
    public static final DeferredItem<BlockItem> VOLTAIC_PILE = ITEMS.registerSimpleBlockItem(ModBlocks.VOLTAIC_PILE);
    public static final DeferredItem<BlockItem> LEAD_ACID_ACCUMULATOR = ITEMS.registerSimpleBlockItem(ModBlocks.LEAD_ACID_ACCUMULATOR);
    public static final DeferredItem<BlockItem> ELECTROMECHANICAL_PRESS = ITEMS.registerSimpleBlockItem(ModBlocks.ELECTROMECHANICAL_PRESS);

    public static final DeferredItem<Item> MAGNIFYING_GLASS = ITEMS.registerItem("magnifying_glass",
            properties -> new br.com.quantumindustries.item.ScientificInstrumentItem(Component.translatable("tooltip.quantumindustries.magnifying_glass.title"),
                    Component.translatable("tooltip.quantumindustries.magnifying_glass.description"),
                    properties),
            properties -> properties.stacksTo(1));
    public static final DeferredItem<Item> FLAME_TEST_KIT = ITEMS.registerItem("flame_test_kit",
            properties -> new br.com.quantumindustries.item.ScientificInstrumentItem(Component.translatable("tooltip.quantumindustries.flame_test_kit.title"),
                    Component.translatable("tooltip.quantumindustries.flame_test_kit.description"),
                    properties),
            properties -> properties.stacksTo(1));
    public static final DeferredItem<Item> MANUAL_BELLOWS = ITEMS.registerItem("manual_bellows",
            properties -> new br.com.quantumindustries.item.ScientificInstrumentItem(Component.translatable("tooltip.quantumindustries.manual_bellows.title"),
                    Component.translatable("tooltip.quantumindustries.manual_bellows.description"),
                    properties),
            properties -> properties.stacksTo(1));
    public static final DeferredItem<Item> ASSAY_FLASK = ITEMS.registerItem("assay_flask",
            properties -> new br.com.quantumindustries.item.ScientificInstrumentItem(Component.translatable("tooltip.quantumindustries.assay_flask.title"),
                    Component.translatable("tooltip.quantumindustries.assay_flask.description"),
                    properties),
            properties -> properties.stacksTo(1));
    public static final DeferredItem<Item> BRASS_WEIGHTS = ITEMS.registerItem("brass_weights",
            properties -> new br.com.quantumindustries.item.ScientificInstrumentItem(Component.translatable("tooltip.quantumindustries.brass_weights.title"),
                    Component.translatable("tooltip.quantumindustries.brass_weights.description"),
                    properties),
            properties -> properties.stacksTo(1));

    public static final DeferredItem<Item> ENERGY_INSPECTION_GOGGLES = ITEMS.registerItem(
            "energy_inspection_goggles", br.com.quantumindustries.item.EnergyInspectionGogglesItem::new,
            properties -> properties.stacksTo(1).component(
                    net.minecraft.core.component.DataComponents.EQUIPPABLE,
                    net.minecraft.world.item.equipment.Equippable.builder(net.minecraft.world.entity.EquipmentSlot.HEAD).build()));

    public static final DeferredItem<Item> HEATED_IRON_INGOT = ITEMS.registerItem("heated_iron_ingot",
            properties -> new br.com.quantumindustries.item.HeatedIngotItem(Component.translatable("tooltip.quantumindustries.heated_iron_ingot.title"),
                    980,
                    properties),
            properties -> properties.stacksTo(16));
    public static final DeferredItem<Item> HEATED_COPPER_INGOT = ITEMS.registerItem("heated_copper_ingot",
            properties -> new br.com.quantumindustries.item.HeatedIngotItem(Component.translatable("tooltip.quantumindustries.heated_copper_ingot.title"),
                    850,
                    properties),
            properties -> properties.stacksTo(16));
    public static final DeferredItem<BlockItem> PNEUMATIC_TUBE = ITEMS.registerSimpleBlockItem(ModBlocks.PNEUMATIC_TUBE);
    public static final DeferredItem<BlockItem> BELLOWS_EJECTOR = ITEMS.registerSimpleBlockItem(ModBlocks.BELLOWS_EJECTOR);
    public static final DeferredItem<BlockItem> GRAVITATIONAL_SORTER = ITEMS.registerSimpleBlockItem(ModBlocks.GRAVITATIONAL_SORTER);
    public static final DeferredItem<BlockItem> FLUID_PIPE = ITEMS.registerSimpleBlockItem(ModBlocks.FLUID_PIPE);
    public static final DeferredItem<BlockItem> GLASS_PIPE = ITEMS.registerSimpleBlockItem(ModBlocks.GLASS_PIPE);
    public static final DeferredItem<BlockItem> PERISTALTIC_PUMP = ITEMS.registerSimpleBlockItem(ModBlocks.PERISTALTIC_PUMP);
    public static final DeferredItem<BlockItem> SETTLING_VAT = ITEMS.registerSimpleBlockItem(ModBlocks.SETTLING_VAT);
    public static final DeferredItem<Item> GALVANIC_ELECTROLYTE = ITEMS.registerItem("galvanic_electrolyte",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.galvanic_electrolyte.title"),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.galvanic_electrolyte.spec_0"), Component.translatable("tooltip.quantumindustries.galvanic_electrolyte.spec_1")),
                    properties));

    public static final DeferredItem<Item> PIGMENT_SEDIMENT = ITEMS.registerItem("pigment_sediment",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.pigment_sediment.title"),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.pigment_sediment.spec_0"), Component.translatable("tooltip.quantumindustries.pigment_sediment.spec_1")),
                    properties));

    public static final DeferredItem<BlockItem> PIEZO_SWITCH = ITEMS.registerSimpleBlockItem(ModBlocks.PIEZO_SWITCH);
    public static final DeferredItem<BlockItem> LOAD_RELAY = ITEMS.registerSimpleBlockItem(ModBlocks.LOAD_RELAY);

    public static final DeferredItem<BlockItem> BELLOWS_BRAZIER = ITEMS.registerSimpleBlockItem(ModBlocks.BELLOWS_BRAZIER);

    public static final DeferredItem<Item> GOLD_SHEET = ITEMS.registerItem("gold_sheet",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.gold_sheet.title"),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.gold_sheet.spec_0"), Component.translatable("tooltip.quantumindustries.gold_sheet.spec_1")),
                    properties));

    public static final DeferredItem<Item> GOLD_WIRE = ITEMS.registerItem("gold_wire",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.gold_wire.title"),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.gold_wire.spec_0"), Component.translatable("tooltip.quantumindustries.gold_wire.spec_1")),
                    properties));
    public static final DeferredItem<BlockItem> WIRE_DRAWING_BENCH = ITEMS.registerSimpleBlockItem(ModBlocks.WIRE_DRAWING_BENCH);

    public static final DeferredItem<Item> EMPIRICAL_FIELD_NOTES =
            ITEMS.registerItem("empirical_field_notes", br.com.quantumindustries.assay.EmpiricalFieldNotesItem::new,
                    properties -> properties.stacksTo(1));

    public static final DeferredItem<Item> PRIMITIVE_MECHANICAL_CORE = ITEMS.registerItem("primitive_mechanical_core",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.primitive_mechanical_core.title"),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.primitive_mechanical_core.spec_0"), Component.translatable("tooltip.quantumindustries.primitive_mechanical_core.spec_1")),
                    properties));

    public static final DeferredItem<BlockItem> BELLOWS_ALLOY_KILN = ITEMS.registerSimpleBlockItem(ModBlocks.BELLOWS_ALLOY_KILN);
    public static final DeferredItem<BlockItem> ELECTRIC_SAWMILL = ITEMS.registerSimpleBlockItem(ModBlocks.ELECTRIC_SAWMILL);
    public static final DeferredItem<BlockItem> ELECTRIC_ALLOY_SMELTER = ITEMS.registerSimpleBlockItem(ModBlocks.ELECTRIC_ALLOY_SMELTER);
    public static final DeferredItem<BlockItem> JAW_CRUSHER = ITEMS.registerSimpleBlockItem(ModBlocks.JAW_CRUSHER);

    public static final DeferredItem<Item> HEATED_BRONZE_INGOT = ITEMS.registerItem("heated_bronze_ingot",
            properties -> new br.com.quantumindustries.item.HeatedIngotItem(Component.translatable("tooltip.quantumindustries.heated_bronze_ingot.title"),
                    760,
                    properties),
            properties -> properties.stacksTo(16));
    public static final DeferredItem<Item> HEATED_BRASS_INGOT = ITEMS.registerItem("heated_brass_ingot",
            properties -> new br.com.quantumindustries.item.HeatedIngotItem(Component.translatable("tooltip.quantumindustries.heated_brass_ingot.title"),
                    740,
                    properties),
            properties -> properties.stacksTo(16));
    public static final DeferredItem<Item> HEATED_STEEL_INGOT = ITEMS.registerItem("heated_steel_ingot",
            properties -> new br.com.quantumindustries.item.HeatedIngotItem(Component.translatable("tooltip.quantumindustries.heated_steel_ingot.title"),
                    1050,
                    properties),
            properties -> properties.stacksTo(16));

    public static final DeferredItem<Item> ELECTRUM_WIRE = ITEMS.registerItem("electrum_wire",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.electrum_wire.title"),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.electrum_wire.spec_0"), Component.translatable("tooltip.quantumindustries.electrum_wire.spec_1")),
                    properties));

    public static final DeferredItem<Item> CONSTANTAN_WIRE = ITEMS.registerItem("constantan_wire",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.constantan_wire.title"),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.constantan_wire.spec_0"), Component.translatable("tooltip.quantumindustries.constantan_wire.spec_1")),
                    properties));

    public static final DeferredItem<Item> CONSTANTAN_COIL = ITEMS.registerItem("constantan_coil",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.constantan_coil.title"),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.constantan_coil.spec_0"), Component.translatable("tooltip.quantumindustries.constantan_coil.spec_1")),
                    properties));

    public static final DeferredItem<Item> HYDROCHLORIC_ACID = ITEMS.registerItem("hydrochloric_acid",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.hydrochloric_acid.title"),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.hydrochloric_acid.spec_0"), Component.translatable("tooltip.quantumindustries.hydrochloric_acid.spec_1")),
                    properties));

    public static final DeferredItem<Item> REFRACTORY_BRICK = ITEMS.registerItem("refractory_brick",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.refractory_brick.title"),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.refractory_brick.spec_0"), Component.translatable("tooltip.quantumindustries.refractory_brick.spec_1")),
                    properties));

    public static final DeferredItem<Item> PHOSPHATE_FERTILIZER = ITEMS.registerItem("phosphate_fertilizer",
            br.com.quantumindustries.item.PhosphateFertilizerItem::new);

    public static final DeferredItem<Item> TIN_SHEET = ITEMS.registerItem("tin_sheet",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.tin_sheet.title"),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.tin_sheet.spec_0"), Component.translatable("tooltip.quantumindustries.tin_sheet.spec_1")),
                    properties));

    public static final DeferredItem<Item> ZINC_SHEET = ITEMS.registerItem("zinc_sheet",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.zinc_sheet.title"),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.zinc_sheet.spec_0"), Component.translatable("tooltip.quantumindustries.zinc_sheet.spec_1")),
                    properties));

    public static final DeferredItem<Item> BRONZE_GEAR = ITEMS.registerItem("bronze_gear",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.bronze_gear.title"),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.bronze_gear.spec_0"), Component.translatable("tooltip.quantumindustries.bronze_gear.spec_1")),
                    properties));

    public static final java.util.Map<String, DeferredItem<Item>> METAL_DUSTS = new java.util.LinkedHashMap<>();
    public static final java.util.Map<String, DeferredItem<Item>> ALLOY_INGOTS = new java.util.LinkedHashMap<>();
    public static final java.util.Map<String, DeferredItem<Item>> ALLOY_NUGGETS = new java.util.LinkedHashMap<>();
    public static final java.util.Map<String, DeferredItem<Item>> ALLOY_SHEETS = new java.util.LinkedHashMap<>();

    public static final java.util.Map<String, DeferredItem<BlockItem>> STONE_ORE_ITEMS = new java.util.LinkedHashMap<>();
    public static final java.util.Map<String, DeferredItem<BlockItem>> DEEPSLATE_ORE_ITEMS = new java.util.LinkedHashMap<>();
    public static final java.util.Map<String, DeferredItem<Item>> RAW_MATERIALS = new java.util.LinkedHashMap<>();
    public static final java.util.Map<String, DeferredItem<Item>> INGOTS = new java.util.LinkedHashMap<>();
    public static final java.util.Map<String, DeferredItem<Item>> NUGGETS = new java.util.LinkedHashMap<>();
    public static final java.util.Map<String, DeferredItem<Item>> CHEMICAL_PRODUCTS = new java.util.LinkedHashMap<>();

    static {
        for (var dust : br.com.quantumindustries.material.CrushingCatalog.METAL_DUSTS) {
            METAL_DUSTS.put(dust.id(), ITEMS.registerItem(dust.dustId(),
                    props -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.mineral_dust.title", Component.translatable("material.quantumindustries.metal." + dust.id() + ".name")),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.mineral_dust.spec_0", dust.formula()), Component.translatable("tooltip.quantumindustries.mineral_dust.spec_1")),
                    props)));
        }

        for (var alloy : br.com.quantumindustries.material.AlloyCatalog.ALLOYS) {
            ALLOY_INGOTS.put(alloy.id(), ITEMS.registerItem(alloy.ingotId(),
                    props -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.alloy_ingot.title", Component.translatable("material.quantumindustries.alloy." + alloy.id() + ".name")),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.alloy_ingot.spec_0", alloy.formula()), Component.translatable("tooltip.quantumindustries.alloy_ingot.spec_1", Component.translatable("material.quantumindustries.alloy." + alloy.id() + ".description"))),
                    props)));
            ALLOY_NUGGETS.put(alloy.id(), ITEMS.registerItem(alloy.nuggetId(),
                    props -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.alloy_nugget.title", Component.translatable("material.quantumindustries.alloy." + alloy.id() + ".name")),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.alloy_nugget.spec_0", alloy.formula()), Component.translatable("tooltip.quantumindustries.alloy_nugget.spec_1")),
                    props)));
            ALLOY_SHEETS.put(alloy.id(), ITEMS.registerItem(alloy.sheetId(),
                    props -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.alloy_sheet.title", Component.translatable("material.quantumindustries.alloy." + alloy.id() + ".name")),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.alloy_sheet.spec_0", alloy.formula()), Component.translatable("tooltip.quantumindustries.alloy_sheet.spec_1")),
                    props)));
        }

        for (var mineral : br.com.quantumindustries.material.MineralCatalog.MINERALS) {
            STONE_ORE_ITEMS.put(mineral.id(), ITEMS.registerSimpleBlockItem(ModBlocks.STONE_ORES.get(mineral.id())));
            DEEPSLATE_ORE_ITEMS.put(mineral.id(), ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_ORES.get(mineral.id())));

            if (mineral.category() == br.com.quantumindustries.material.MineralCatalog.Category.METAL) {
                RAW_MATERIALS.put(mineral.productName(), ITEMS.registerItem(mineral.rawItemId(),
                        props -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.mineral_raw.title", Component.translatable("material.quantumindustries.mineral." + mineral.id() + ".name")),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.mineral_raw.spec_0", mineral.formula()), Component.translatable("tooltip.quantumindustries.mineral_raw.spec_1")),
                    props)));

                INGOTS.put(mineral.productName(), ITEMS.registerItem(mineral.ingotItemId(),
                        props -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.mineral_ingot.title", Component.translatable("material.quantumindustries.mineral." + mineral.id() + ".product")),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.mineral_ingot.spec_0"), Component.translatable("tooltip.quantumindustries.mineral_ingot.spec_1")),
                    props)));

                NUGGETS.put(mineral.productName(), ITEMS.registerItem(mineral.nuggetItemId(),
                        props -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.mineral_nugget.title", Component.translatable("material.quantumindustries.mineral." + mineral.id() + ".product")),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.mineral_nugget.spec_0"), Component.translatable("tooltip.quantumindustries.mineral_nugget.spec_1")),
                    props)));
            } else {
                CHEMICAL_PRODUCTS.put(mineral.id(), ITEMS.registerItem(mineral.productItemId(),
                        props -> new br.com.quantumindustries.item.ComponentMaterialItem(Component.translatable("tooltip.quantumindustries.mineral_chemical.title", Component.translatable("material.quantumindustries.mineral." + mineral.id() + ".product")),
                    java.util.List.of(Component.translatable("tooltip.quantumindustries.mineral_chemical.spec_0", mineral.formula()), Component.translatable("tooltip.quantumindustries.mineral_chemical.spec_1")),
                    props)));
            }
        }
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        ModBlocks.register(eventBus);
    }
}
