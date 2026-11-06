package br.com.quantumindustries;

import br.com.quantumindustries.core.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.bus.api.IEventBus;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(QuantumIndustries.MODID);

    public static final DeferredBlock<br.com.quantumindustries.block.ResearchTableBlock> RESEARCH_TABLE = ModBlocks.RESEARCH_TABLE;

    public static final DeferredItem<BlockItem> SCIENTIFIC_ANALYZER_TIER_1 =
            ITEMS.registerSimpleBlockItem(ModBlocks.RESEARCH_TABLE_TIER_1);
    public static final DeferredItem<BlockItem> SCIENTIFIC_ANALYZER = ITEMS.registerSimpleBlockItem(RESEARCH_TABLE);
    public static final DeferredItem<BlockItem> EMPIRICAL_ASSAY_BENCH = ITEMS.registerSimpleBlockItem(ModBlocks.EMPIRICAL_ASSAY_BENCH);

    // Ferramenta de teste (Sonda)
    public static final DeferredItem<Item> TEST_PROBES = ITEMS.registerItem("test_probes", Item::new, properties -> properties.durability(100));

    // Dados de pesquisa (output da bancada)
    public static final DeferredItem<Item> RESEARCH_DATA = ITEMS.registerItem("research_data", br.com.quantumindustries.research.ResearchReportItem::new);
    public static final DeferredItem<Item> COPPER_WIRE = ITEMS.registerSimpleItem("copper_wire");

    public static final DeferredItem<Item> MANUAL_HAMMER = ITEMS.registerItem("manual_hammer", Item::new, properties -> properties.durability(96));
    public static final DeferredItem<Item> IRON_SHEET = ITEMS.registerSimpleItem("iron_sheet");
    public static final DeferredItem<Item> COPPER_SHEET = ITEMS.registerSimpleItem("copper_sheet");
    public static final DeferredItem<Item> IRON_GEAR = ITEMS.registerSimpleItem("iron_gear");
    public static final DeferredItem<Item> MECHANICAL_SHAFT = ITEMS.registerSimpleItem("mechanical_shaft");
    public static final DeferredItem<Item> HAND_CRANK = ITEMS.registerSimpleItem("hand_crank");
    public static final DeferredItem<Item> INSULATED_COPPER_WIRE = ITEMS.registerSimpleItem("insulated_copper_wire");
    public static final DeferredItem<Item> COPPER_COIL = ITEMS.registerSimpleItem("copper_coil");
    public static final DeferredItem<Item> ELECTROMECHANICAL_FRAME = ITEMS.registerSimpleItem("electromechanical_frame");
    public static final DeferredItem<BlockItem> FORMING_BENCH = ITEMS.registerSimpleBlockItem(ModBlocks.FORMING_BENCH);
    public static final DeferredItem<BlockItem> MANUAL_DYNAMO = ITEMS.registerSimpleBlockItem(ModBlocks.MANUAL_DYNAMO);
    public static final DeferredItem<BlockItem> COPPER_BUSBAR = ITEMS.registerSimpleBlockItem(ModBlocks.COPPER_BUSBAR);
    public static final DeferredItem<BlockItem> INSULATED_CONDUIT = ITEMS.registerSimpleBlockItem(ModBlocks.INSULATED_CONDUIT);
    public static final DeferredItem<BlockItem> GALVANIC_ACCUMULATOR = ITEMS.registerSimpleBlockItem(ModBlocks.GALVANIC_ACCUMULATOR);
    public static final DeferredItem<BlockItem> ELECTROMECHANICAL_PRESS = ITEMS.registerSimpleBlockItem(ModBlocks.ELECTROMECHANICAL_PRESS);

    public static final DeferredItem<Item> MAGNIFYING_GLASS = ITEMS.registerItem("magnifying_glass", Item::new, properties -> properties.stacksTo(1));
    public static final DeferredItem<Item> FLAME_TEST_KIT = ITEMS.registerItem("flame_test_kit", Item::new, properties -> properties.stacksTo(1));
    public static final DeferredItem<Item> MANUAL_BELLOWS = ITEMS.registerItem("manual_bellows", Item::new, properties -> properties.stacksTo(1));
    public static final DeferredItem<Item> ASSAY_FLASK = ITEMS.registerItem("assay_flask", Item::new, properties -> properties.stacksTo(1));
    public static final DeferredItem<Item> BRASS_WEIGHTS = ITEMS.registerItem("brass_weights", Item::new, properties -> properties.stacksTo(1));

    public static final DeferredItem<Item> ENERGY_INSPECTION_GOGGLES = ITEMS.registerItem(
            "energy_inspection_goggles", Item::new, properties -> properties.stacksTo(1).component(
                    net.minecraft.core.component.DataComponents.EQUIPPABLE,
                    net.minecraft.world.item.equipment.Equippable.builder(net.minecraft.world.entity.EquipmentSlot.HEAD).build()));

    public static final DeferredItem<Item> HEATED_IRON_INGOT = ITEMS.registerSimpleItem("heated_iron_ingot");
    public static final DeferredItem<Item> HEATED_COPPER_INGOT = ITEMS.registerSimpleItem("heated_copper_ingot");
    public static final DeferredItem<BlockItem> PNEUMATIC_TUBE = ITEMS.registerSimpleBlockItem(ModBlocks.PNEUMATIC_TUBE);
    public static final DeferredItem<BlockItem> BELLOWS_EJECTOR = ITEMS.registerSimpleBlockItem(ModBlocks.BELLOWS_EJECTOR);
    public static final DeferredItem<BlockItem> GRAVITATIONAL_SORTER = ITEMS.registerSimpleBlockItem(ModBlocks.GRAVITATIONAL_SORTER);
    public static final DeferredItem<BlockItem> FLUID_PIPE = ITEMS.registerSimpleBlockItem(ModBlocks.FLUID_PIPE);
    public static final DeferredItem<BlockItem> GLASS_PIPE = ITEMS.registerSimpleBlockItem(ModBlocks.GLASS_PIPE);
    public static final DeferredItem<BlockItem> PERISTALTIC_PUMP = ITEMS.registerSimpleBlockItem(ModBlocks.PERISTALTIC_PUMP);
    public static final DeferredItem<BlockItem> SETTLING_VAT = ITEMS.registerSimpleBlockItem(ModBlocks.SETTLING_VAT);
    public static final DeferredItem<Item> GALVANIC_ELECTROLYTE = ITEMS.registerSimpleItem("galvanic_electrolyte");
    public static final DeferredItem<Item> PIGMENT_SEDIMENT = ITEMS.registerSimpleItem("pigment_sediment");

    public static final DeferredItem<BlockItem> PIEZO_SWITCH = ITEMS.registerSimpleBlockItem(ModBlocks.PIEZO_SWITCH);
    public static final DeferredItem<BlockItem> LOAD_RELAY = ITEMS.registerSimpleBlockItem(ModBlocks.LOAD_RELAY);

    public static final DeferredItem<BlockItem> BELLOWS_BRAZIER = ITEMS.registerSimpleBlockItem(ModBlocks.BELLOWS_BRAZIER);

    public static final DeferredItem<Item> GOLD_SHEET = ITEMS.registerSimpleItem("gold_sheet");
    public static final DeferredItem<Item> GOLD_WIRE = ITEMS.registerSimpleItem("gold_wire");
    public static final DeferredItem<BlockItem> WIRE_DRAWING_BENCH = ITEMS.registerSimpleBlockItem(ModBlocks.WIRE_DRAWING_BENCH);

    public static final DeferredItem<Item> EMPIRICAL_FIELD_NOTES =
            ITEMS.registerItem("empirical_field_notes", br.com.quantumindustries.assay.EmpiricalFieldNotesItem::new,
                    properties -> properties.stacksTo(1));

    public static final DeferredItem<Item> PRIMITIVE_MECHANICAL_CORE =
            ITEMS.registerSimpleItem("primitive_mechanical_core");

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        ModBlocks.register(eventBus);
    }
}
