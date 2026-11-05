package br.com.quantumindustries.research;

import br.com.quantumindustries.assay.AssayCatalog;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Central catalog for the Era 0 notebook stamps and material-specific empirical results. */
public enum EmpiricalMaterialCatalog {
    COPPER(0, AssayCatalog.Material.COPPER, "Cobre", Items.COPPER_INGOT, Items.RAW_COPPER,
            "Dúctil (8.96 g/cm³)", "Chama Verde (1085°C)", "Condutor Galvânico",
            Identifier.fromNamespaceAndPath("quantumindustries", "electrical_conductivity")),
    IRON(1, AssayCatalog.Material.IRON, "Ferro", Items.IRON_INGOT, Items.RAW_IRON,
            "Rígido (7.87 g/cm³)", "Fusão Alta (1538°C)", "Ferromagnético",
            Identifier.fromNamespaceAndPath("quantumindustries", "ferromagnetism")),
    COAL(2, AssayCatalog.Material.COAL, "Carvão", Items.COAL, Items.CHARCOAL,
            "Quebradiço (1.35 g/cm³)", "Combustível (Ignição)", "Semi-Isolante",
            Identifier.fromNamespaceAndPath("quantumindustries", "solid_combustion")),
    GOLD(3, AssayCatalog.Material.GOLD, "Ouro", Items.GOLD_INGOT, Items.RAW_GOLD,
            "Ultra-Maleável (19.3 g/cm³)", "Inerte (1064°C)", "Condutor Nobre",
            Identifier.fromNamespaceAndPath("quantumindustries", "noble_conductivity")),
    REDSTONE(4, AssayCatalog.Material.REDSTONE, "Redstone", Items.REDSTONE, Items.REDSTONE,
            "Granular (Piezoativo)", "Termo-Luminescente", "Pulso Ressonante",
            Identifier.fromNamespaceAndPath("quantumindustries", "redstone_resonance")),
    QUARTZ(5, AssayCatalog.Material.QUARTZ, "Quartzo", Items.QUARTZ, Items.QUARTZ,
            "Duro / Fratura (2.65 g/cm³)", "Refratário (1670°C)", "Dielétrico Piezo",
            Identifier.fromNamespaceAndPath("quantumindustries", "piezoelectricity")),
    LAPIS(6, AssayCatalog.Material.LAPIS, "Lápis-Lazúli", Items.LAPIS_LAZULI, Items.LAPIS_LAZULI,
            "Poroso (2.40 g/cm³)", "Calcinação Azul", "Capacitivo Iônico",
            Identifier.fromNamespaceAndPath("quantumindustries", "ionic_storage")),
    AMETHYST(7, AssayCatalog.Material.AMETHYST, "Ametista", Items.AMETHYST_SHARD, Items.AMETHYST_SHARD,
            "Clivagem Cristalina", "Descoloração (450°C)", "Harmônico Acústico",
            Identifier.fromNamespaceAndPath("quantumindustries", "harmonic_lattice")),
    DIAMOND(8, AssayCatalog.Material.DIAMOND, "Diamante", Items.DIAMOND, Items.DIAMOND,
            "Dureza Máxima (3.51 g/cm³)", "Condutor Térmico Puro", "Ímã de Campo (Fictício)",
            Identifier.fromNamespaceAndPath("quantumindustries", "diamond_structure")),
    EMERALD(9, AssayCatalog.Material.EMERALD, "Esmeralda", Items.EMERALD, Items.EMERALD,
            "Cristalino / Fratura (2.72 g/cm³)", "Cromático Verde", "Ressonante Verdoso",
            Identifier.fromNamespaceAndPath("quantumindustries", "verdant_lattice"));

    private final int index;
    private final AssayCatalog.Material material;
    private final String displayName;
    private final Item primaryItem;
    private final Item rawItem;
    private final String mechanical;
    private final String heating;
    private final String galvanic;
    private final Identifier discoveryId;

    EmpiricalMaterialCatalog(int index, AssayCatalog.Material material, String displayName, Item primaryItem, Item rawItem,
                            String mechanical, String heating, String galvanic, Identifier discoveryId) {
        this.index = index;
        this.material = material;
        this.displayName = displayName;
        this.primaryItem = primaryItem;
        this.rawItem = rawItem;
        this.mechanical = mechanical;
        this.heating = heating;
        this.galvanic = galvanic;
        this.discoveryId = discoveryId;
    }

    public static final Map<AssayCatalog.Material, EmpiricalMaterialCatalog> BY_MATERIAL =
            Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(c -> c.material, Function.identity()));

    public int index() { return index; }
    public AssayCatalog.Material material() { return material; }
    public String displayName() { return displayName; }
    public ItemStack sampleStack() { return new ItemStack(primaryItem); }
    public ItemStack rawStack() { return new ItemStack(rawItem); }
    public String mechanicalStamp() { return mechanical; }
    public String heatingStamp() { return heating; }
    public String galvanicStamp() { return galvanic; }
    public Identifier discoveryId() { return discoveryId; }

    public String stamp(AssayCatalog.Method method) {
        return switch (method) {
            case MECHANICAL -> mechanical;
            case HEATING -> heating;
            case INSPECTION -> "Aspecto observado";
            case REACTION -> "Reação observada";
        };
    }

    public static EmpiricalMaterialCatalog of(AssayCatalog.Material material) {
        return material == null ? null : BY_MATERIAL.get(material);
    }
}
