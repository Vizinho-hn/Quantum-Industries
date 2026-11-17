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
    public static final DeferredItem<BlockItem> QUANTUM_VAULT = ITEMS.registerSimpleBlockItem(ModBlocks.QUANTUM_VAULT);

    public static final DeferredItem<Item> QUANTUM_GUIDEBOOK = ITEMS.registerItem("quantum_guidebook",
            br.com.quantumindustries.item.QuantumGuidebookItem::new, properties -> properties.stacksTo(1));

    // Ferramenta de teste (Sonda)
    public static final DeferredItem<Item> TEST_PROBES = ITEMS.registerItem("test_probes",
            br.com.quantumindustries.item.TestProbesItem::new, properties -> properties.durability(100));

    // Dados de pesquisa (output da bancada)
    public static final DeferredItem<Item> RESEARCH_DATA = ITEMS.registerItem("research_data", br.com.quantumindustries.research.ResearchReportItem::new);
    public static final DeferredItem<Item> COPPER_WIRE = ITEMS.registerItem("copper_wire",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(
                    "§6⚡ Condutor de Cobre Puro",
                    java.util.List.of("§7Condutividade: §e98% IACS", "§8Utilizado em fiações de baixa impedância e bobinagens."),
                    properties,
                    () -> ModItems.COPPER_BUSBAR.get()));

    public static final DeferredItem<Item> MANUAL_HAMMER = ITEMS.registerItem("manual_hammer",
            br.com.quantumindustries.item.ManualHammerItem::new, properties -> properties.durability(128));

    public static final DeferredItem<Item> IRON_SHEET = ITEMS.registerItem("iron_sheet",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(
                    "§f🛡️ Chapa de Ferro Calandrada",
                    java.util.List.of("§7Espessura: §f3.2 mm (Conformação a Frio)", "§8Resistência estrutural para carcaças e blindagens."),
                    properties));

    public static final DeferredItem<Item> COPPER_SHEET = ITEMS.registerItem("copper_sheet",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(
                    "§6🛡️ Chapa de Cobre Laminada",
                    java.util.List.of("§7Propriedade: §6Alta Dissipação Térmica", "§8Placas condutoras para acumuladores e trocadores de calor."),
                    properties));

    public static final DeferredItem<Item> IRON_GEAR = ITEMS.registerItem("iron_gear",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(
                    "§f⚙️ Engrenagem de Transmissão Mecânica",
                    java.util.List.of("§7Módulo Dentado: §f2.5 Reforçado", "§8Transmite torque de rotação com baixo atrito mecânico."),
                    properties));

    public static final DeferredItem<Item> MECHANICAL_SHAFT = ITEMS.registerItem("mechanical_shaft",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(
                    "§f🔩 Eixo de Transmissão Retificado",
                    java.util.List.of("§7Tolerância de Torque: §f600 Nm", "§8Transfere trabalho mecânico entre dínamos e polias."),
                    properties));

    public static final DeferredItem<Item> HAND_CRANK = ITEMS.registerItem("hand_crank",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(
                    "§f🔄 Manivela de Partida Manual",
                    java.util.List.of("§7Acionamento: §fCinético Muscular", "§8Gera torque inicial para máquinas e dínamos em repouso."),
                    properties));

    public static final DeferredItem<Item> INSULATED_COPPER_WIRE = ITEMS.registerItem("insulated_copper_wire",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(
                    "§9🛡️ Condutor de Cobre Isolado",
                    java.util.List.of("§7Isolação: §9Dielétrico Polimérico", "§8Impede fuga de corrente e curto-circuito entre fases."),
                    properties,
                    () -> ModItems.INSULATED_CONDUIT.get()));

    public static final DeferredItem<Item> COPPER_COIL = ITEMS.registerItem("copper_coil",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(
                    "§6🌀 Bobina de Indução Eletromagnética",
                    java.util.List.of("§7Densidade de Espiras: §6Alta Concentração", "§8Induz campos magnéticos para geração e conversão de energia."),
                    properties));

    // Era II - Lead-Acid Accumulator components
    public static final DeferredItem<Item> LEAD_SHEET = ITEMS.registerItem("lead_sheet",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(
                    "§7⬜ Chapa de Chumbo",
                    java.util.List.of("§7Pureza: §f99.9%", "§8Eletrodo negativo para acumuladores chumbo-ácido."),
                    properties));
    public static final DeferredItem<Item> SULFURIC_ACID = ITEMS.registerItem("sulfuric_acid",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(
                    "§6☠️ Ácido Sulfúrico Concentrado",
                    java.util.List.of("§7Densidade: §e1.84 g/cm³", "§8Eletrólito para acumuladores. Manuseie com EPI."),
                    properties));
    public static final DeferredItem<Item> EBONITE_ROD = ITEMS.registerItem("ebonite_rod",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(
                    "§0🟤 Haste de Ebonite (Borracha Dura)",
                    java.util.List.of("§7Isolante elétrico e químico", "§8Separador de placas e vedação de acumuladores."),
                    properties));

    public static final DeferredItem<Item> ELECTROMECHANICAL_FRAME = ITEMS.registerItem("electromechanical_frame",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(
                    "§3🏗️ Chassi Eletromecânico Estrutural",
                    java.util.List.of("§7Liga: §3Aço-Carbono Estrutural", "§8Gabinete modular para montagem de maquinários industriais."),
                    properties));
    public static final DeferredItem<BlockItem> FORMING_BENCH = ITEMS.registerSimpleBlockItem(ModBlocks.FORMING_BENCH);
    public static final DeferredItem<BlockItem> MANUAL_GRAIN_MILL = ITEMS.registerSimpleBlockItem(ModBlocks.MANUAL_GRAIN_MILL);
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
            properties -> new br.com.quantumindustries.item.ScientificInstrumentItem("Morfologia Óptica", "Revela micro-fraturas e cristalografia de ligas metálicas.", properties),
            properties -> properties.stacksTo(1));
    public static final DeferredItem<Item> FLAME_TEST_KIT = ITEMS.registerItem("flame_test_kit",
            properties -> new br.com.quantumindustries.item.ScientificInstrumentItem("Espectro de Chama", "Identifica emissões atômicas características por combustão controlada.", properties),
            properties -> properties.stacksTo(1));
    public static final DeferredItem<Item> MANUAL_BELLOWS = ITEMS.registerItem("manual_bellows",
            properties -> new br.com.quantumindustries.item.ScientificInstrumentItem("Ventilação Forçada", "Aumenta oxigenação e potencial térmico de ensaios pirométricos.", properties),
            properties -> properties.stacksTo(1));
    public static final DeferredItem<Item> ASSAY_FLASK = ITEMS.registerItem("assay_flask",
            properties -> new br.com.quantumindustries.item.ScientificInstrumentItem("Reação Química", "Frasco de borossilicato graduado para dissolução em solventes ácidos.", properties),
            properties -> properties.stacksTo(1));
    public static final DeferredItem<Item> BRASS_WEIGHTS = ITEMS.registerItem("brass_weights",
            properties -> new br.com.quantumindustries.item.ScientificInstrumentItem("Gravimetria", "Pesos padrão de latão usinado para calibração gravimétrica de densidade.", properties),
            properties -> properties.stacksTo(1));

    public static final DeferredItem<Item> ENERGY_INSPECTION_GOGGLES = ITEMS.registerItem(
            "energy_inspection_goggles", br.com.quantumindustries.item.EnergyInspectionGogglesItem::new,
            properties -> properties.stacksTo(1).component(
                    net.minecraft.core.component.DataComponents.EQUIPPABLE,
                    net.minecraft.world.item.equipment.Equippable.builder(net.minecraft.world.entity.EquipmentSlot.HEAD).build()));

    public static final DeferredItem<Item> HEATED_IRON_INGOT = ITEMS.registerItem("heated_iron_ingot",
            properties -> new br.com.quantumindustries.item.HeatedIngotItem("Ferro Carbono", 980, properties),
            properties -> properties.stacksTo(16));
    public static final DeferredItem<Item> HEATED_COPPER_INGOT = ITEMS.registerItem("heated_copper_ingot",
            properties -> new br.com.quantumindustries.item.HeatedIngotItem("Cobre Eletrolítico", 850, properties),
            properties -> properties.stacksTo(16));
    public static final DeferredItem<BlockItem> PNEUMATIC_TUBE = ITEMS.registerSimpleBlockItem(ModBlocks.PNEUMATIC_TUBE);
    public static final DeferredItem<BlockItem> BELLOWS_EJECTOR = ITEMS.registerSimpleBlockItem(ModBlocks.BELLOWS_EJECTOR);
    public static final DeferredItem<BlockItem> GRAVITATIONAL_SORTER = ITEMS.registerSimpleBlockItem(ModBlocks.GRAVITATIONAL_SORTER);
    public static final DeferredItem<BlockItem> FLUID_PIPE = ITEMS.registerSimpleBlockItem(ModBlocks.FLUID_PIPE);
    public static final DeferredItem<BlockItem> GLASS_PIPE = ITEMS.registerSimpleBlockItem(ModBlocks.GLASS_PIPE);
    public static final DeferredItem<BlockItem> PERISTALTIC_PUMP = ITEMS.registerSimpleBlockItem(ModBlocks.PERISTALTIC_PUMP);
    public static final DeferredItem<BlockItem> SETTLING_VAT = ITEMS.registerSimpleBlockItem(ModBlocks.SETTLING_VAT);
    public static final DeferredItem<Item> GALVANIC_ELECTROLYTE = ITEMS.registerItem("galvanic_electrolyte",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(
                    "§a🧪 Eletrólito Galvânico Ativo",
                    java.util.List.of("§7Composição: §aSolução Iônica Condutora", "§8Fluido transportador de carga para células galvânicas."),
                    properties));

    public static final DeferredItem<Item> PIGMENT_SEDIMENT = ITEMS.registerItem("pigment_sediment",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(
                    "§d🧱 Sedimento Mineral Decantado",
                    java.util.List.of("§7Origem: §dDecantação em Tanque", "§8Precursor purificado para sínteses químicas e reagentes."),
                    properties));

    public static final DeferredItem<BlockItem> PIEZO_SWITCH = ITEMS.registerSimpleBlockItem(ModBlocks.PIEZO_SWITCH);
    public static final DeferredItem<BlockItem> LOAD_RELAY = ITEMS.registerSimpleBlockItem(ModBlocks.LOAD_RELAY);

    public static final DeferredItem<BlockItem> BELLOWS_BRAZIER = ITEMS.registerSimpleBlockItem(ModBlocks.BELLOWS_BRAZIER);

    public static final DeferredItem<Item> GOLD_SHEET = ITEMS.registerItem("gold_sheet",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(
                    "§e✨ Chapa de Ouro Nobre",
                    java.util.List.of("§7Propriedade: §eResistência Total à Corrosão", "§8Superfície de contato quântico e instrumentação analítica."),
                    properties));

    public static final DeferredItem<Item> GOLD_WIRE = ITEMS.registerItem("gold_wire",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(
                    "§e✨ Filamento de Ouro Puro",
                    java.util.List.of("§7Propriedade: §eMicro-Condutância Linear", "§8Fiações de altíssima precisão para circuitos quânticos."),
                    properties));
    public static final DeferredItem<BlockItem> WIRE_DRAWING_BENCH = ITEMS.registerSimpleBlockItem(ModBlocks.WIRE_DRAWING_BENCH);

    public static final DeferredItem<Item> EMPIRICAL_FIELD_NOTES =
            ITEMS.registerItem("empirical_field_notes", br.com.quantumindustries.assay.EmpiricalFieldNotesItem::new,
                    properties -> properties.stacksTo(1));

    public static final DeferredItem<Item> PRIMITIVE_MECHANICAL_CORE = ITEMS.registerItem("primitive_mechanical_core",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(
                    "§d⚙️ Núcleo Mecânico Primitivo",
                    java.util.List.of("§7Trem de Engrenagens: §dCinemática Sincronizada", "§8Módulo de automação e controle da Primeira Era Industrial."),
                    properties));

    public static final DeferredItem<BlockItem> BELLOWS_ALLOY_KILN = ITEMS.registerSimpleBlockItem(ModBlocks.BELLOWS_ALLOY_KILN);
    public static final DeferredItem<BlockItem> ELECTRIC_ALLOY_SMELTER = ITEMS.registerSimpleBlockItem(ModBlocks.ELECTRIC_ALLOY_SMELTER);
    public static final DeferredItem<BlockItem> JAW_CRUSHER = ITEMS.registerSimpleBlockItem(ModBlocks.JAW_CRUSHER);

    public static final DeferredItem<Item> HEATED_BRONZE_INGOT = ITEMS.registerItem("heated_bronze_ingot",
            properties -> new br.com.quantumindustries.item.HeatedIngotItem("Bronze (Cu₃Sn)", 760, properties),
            properties -> properties.stacksTo(16));
    public static final DeferredItem<Item> HEATED_BRASS_INGOT = ITEMS.registerItem("heated_brass_ingot",
            properties -> new br.com.quantumindustries.item.HeatedIngotItem("Latão (Cu₃Zn)", 740, properties),
            properties -> properties.stacksTo(16));
    public static final DeferredItem<Item> HEATED_STEEL_INGOT = ITEMS.registerItem("heated_steel_ingot",
            properties -> new br.com.quantumindustries.item.HeatedIngotItem("Aço Carbono (Fe-C)", 1050, properties),
            properties -> properties.stacksTo(16));

    public static final DeferredItem<Item> ELECTRUM_WIRE = ITEMS.registerItem("electrum_wire",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(
                    "§e⚡ Condutor de Electrum (AuAg)",
                    java.util.List.of("§7Condutividade: §e104% IACS (Baixa Perda)", "§8Fio nobre de ouro-prata para instrumentação e malhas de precisão."),
                    properties));

    public static final DeferredItem<Item> CONSTANTAN_WIRE = ITEMS.registerItem("constantan_wire",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(
                    "§c⚡ Condutor de Constantan (CuNi)",
                    java.util.List.of("§7Resistividade Estável: §c0.49 µΩ·m", "§8Elemento resistivo padrão para resistores, shunts e pirômetros."),
                    properties));

    public static final DeferredItem<Item> CONSTANTAN_COIL = ITEMS.registerItem("constantan_coil",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(
                    "§c🌀 Bobina Resistiva de Constantan",
                    java.util.List.of("§7Dissipação Térmica / Carga Elétrica", "§8Bobina não indutiva para relés térmicos e aquecedores elétricos."),
                    properties));

    public static final DeferredItem<Item> HYDROCHLORIC_ACID = ITEMS.registerItem("hydrochloric_acid",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(
                    "§a🧪 Ácido Clorídrico (Muriático)",
                    java.util.List.of("§7Concentração: §a37% HCl", "§8Agente de decapagem ácida e síntese de sais e fertilizantes."),
                    properties));

    public static final DeferredItem<Item> REFRACTORY_BRICK = ITEMS.registerItem("refractory_brick",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(
                    "§6🧱 Tijolo Refratário Aluminoso",
                    java.util.List.of("§7Resistência Térmica: §6Até 1650°C", "§8Isolamento térmico para fornos de alta temperatura e metalurgia."),
                    properties));

    public static final DeferredItem<Item> PHOSPHATE_FERTILIZER = ITEMS.registerItem("phosphate_fertilizer",
            br.com.quantumindustries.item.PhosphateFertilizerItem::new);

    public static final DeferredItem<Item> TIN_SHEET = ITEMS.registerItem("tin_sheet",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(
                    "§b🛡️ Chapa de Estanho Macio",
                    java.util.List.of("§7Espessura: §b1.5 mm", "§8Lata maleável e resistente à corrosão para embalagens e folheados."),
                    properties));

    public static final DeferredItem<Item> ZINC_SHEET = ITEMS.registerItem("zinc_sheet",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(
                    "§3🛡️ Chapa de Zinco Galvanizada",
                    java.util.List.of("§7Proteção Catódica: §3Ânodo de Sacrifício", "§8Revestimento protetor para componentes ferrosos e baterias."),
                    properties));

    public static final DeferredItem<Item> BRONZE_GEAR = ITEMS.registerItem("bronze_gear",
            properties -> new br.com.quantumindustries.item.ComponentMaterialItem(
                    "§6⚙️ Engrenagem de Bronze de Baixo Atrito",
                    java.util.List.of("§7Dureza e Tenacidade: §6Bronze Cu-Sn", "§8Mancais mecânicos e transmissões resistentes a choques pesados."),
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
                    props -> new br.com.quantumindustries.item.ComponentMaterialItem(
                            "§e✨ Pó de " + dust.namePt() + " Beneficiado",
                            java.util.List.of("§7Granulometria: §fMalha 200 (" + dust.formula() + ")", "§8Pó metálico purificado pronto para fundição direta ou mistura de ligas."),
                            props)));
        }

        for (var alloy : br.com.quantumindustries.material.AlloyCatalog.ALLOYS) {
            ALLOY_INGOTS.put(alloy.id(), ITEMS.registerItem(alloy.ingotId(),
                    props -> new br.com.quantumindustries.item.ComponentMaterialItem(
                            "§6🔩 Lingote de " + alloy.namePt(),
                            java.util.List.of("§7Estequiometria: §f" + alloy.formula(), "§8" + alloy.descriptionPt()),
                            props)));
            ALLOY_NUGGETS.put(alloy.id(), ITEMS.registerItem(alloy.nuggetId(),
                    props -> new br.com.quantumindustries.item.ComponentMaterialItem(
                            "§e✨ Pepita de " + alloy.namePt(),
                            java.util.List.of("§7Fração de Liga: §f1/9 (" + alloy.formula() + ")", "§8Porção fracionária de liga metalúrgica."),
                            props)));
            ALLOY_SHEETS.put(alloy.id(), ITEMS.registerItem(alloy.sheetId(),
                    props -> new br.com.quantumindustries.item.ComponentMaterialItem(
                            "§6🛡️ Chapa de " + alloy.namePt(),
                            java.util.List.of("§7Liga Conformada: §f" + alloy.formula(), "§8Chapa laminada para estruturas e instrumentos industriais."),
                            props)));
        }

        for (var mineral : br.com.quantumindustries.material.MineralCatalog.MINERALS) {
            STONE_ORE_ITEMS.put(mineral.id(), ITEMS.registerSimpleBlockItem(ModBlocks.STONE_ORES.get(mineral.id())));
            DEEPSLATE_ORE_ITEMS.put(mineral.id(), ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_ORES.get(mineral.id())));

            if (mineral.category() == br.com.quantumindustries.material.MineralCatalog.Category.METAL) {
                RAW_MATERIALS.put(mineral.productName(), ITEMS.registerItem(mineral.rawItemId(),
                        props -> new br.com.quantumindustries.item.ComponentMaterialItem(
                                "§6⛏️ " + mineral.mineralNamePt() + " Bruta",
                                java.util.List.of("§7Composição: §f" + mineral.formula(), "§8Minério bruto pronto para britagem, flotação ou refino."),
                                props)));

                INGOTS.put(mineral.productName(), ITEMS.registerItem(mineral.ingotItemId(),
                        props -> new br.com.quantumindustries.item.ComponentMaterialItem(
                                "§f🔩 Lingote de " + mineral.productNamePt(),
                                java.util.List.of("§7Pureza Metalúrgica: §f99.2%", "§8Produto de fundição de alto teor para manufatura e ligas."),
                                props)));

                NUGGETS.put(mineral.productName(), ITEMS.registerItem(mineral.nuggetItemId(),
                        props -> new br.com.quantumindustries.item.ComponentMaterialItem(
                                "§7✨ Pepita de " + mineral.productNamePt(),
                                java.util.List.of("§7Fração de Lingote: §f1/9", "§8Unidade fracionária para pesagem e estequiometria fina."),
                                props)));
            } else {
                CHEMICAL_PRODUCTS.put(mineral.id(), ITEMS.registerItem(mineral.productItemId(),
                        props -> new br.com.quantumindustries.item.ComponentMaterialItem(
                                "§b🧪 " + mineral.productNamePt(),
                                java.util.List.of("§7Fórmula: §f" + mineral.formula(), "§8Composto de grau reagente para reações químicas e industriais."),
                                props)));
            }
        }
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        ModBlocks.register(eventBus);
    }
}
