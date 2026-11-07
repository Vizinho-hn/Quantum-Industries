package br.com.quantumindustries.material;

import java.util.List;

/**
 * Catálogo canônico de minerais reais e compostos estequiométricos do Quantum Industries.
 * Define a matriz de dados para registro dinâmico de blocos, itens e produtos industriais.
 */
public final class MineralCatalog {

    public enum Category {
        /** Metal nativo ou sulfeto/óxido que produz Raw -> Ingot -> Nugget */
        METAL,
        /** Mineral químico ou não-metálico que produz pó, cristal ou gema direta */
        CHEMICAL
    }

    public record MineralInfo(
            String id,
            String productName,
            String mineralNamePt,
            String productNamePt,
            String formula,
            Category category,
            float hardness,
            float resistance
    ) {
        public String stoneOreId() {
            return id + "_ore";
        }

        public String deepslateOreId() {
            return "deepslate_" + id + "_ore";
        }

        public String rawItemId() {
            return "raw_" + productName;
        }

        public String ingotItemId() {
            return productName + "_ingot";
        }

        public String nuggetItemId() {
            return productName + "_nugget";
        }

        public String productItemId() {
            return switch (id) {
                case "sulfur" -> "sulfur_dust";
                case "fluorite" -> "fluorite_gem";
                case "cinnabar" -> "cinnabar_dust";
                case "apatite" -> "apatite_gem";
                case "halite" -> "salt_rock";
                case "graphite" -> "graphite_dust";
                default -> productName + "_product";
            };
        }
    }

    public static final List<MineralInfo> MINERALS = List.of(
            // --- METAIS INDUSTRIAIS DA ERA MECÂNICA & QUÍMICA ---
            new MineralInfo("cassiterite", "tin", "Cassiterita", "Estanho", "SnO₂", Category.METAL, 3.0F, 3.0F),
            new MineralInfo("sphalerite", "zinc", "Esfalerita", "Zinco", "(Zn,Fe)S", Category.METAL, 3.0F, 3.0F),
            new MineralInfo("galena", "lead", "Galena", "Chumbo", "PbS", Category.METAL, 3.0F, 3.0F),
            new MineralInfo("pentlandite", "nickel", "Pentlandita", "Níquel", "(Fe,Ni)₉S₈", Category.METAL, 3.5F, 3.0F),
            new MineralInfo("silver", "silver", "Acantita", "Prata", "Ag₂S", Category.METAL, 3.0F, 3.0F),
            new MineralInfo("bauxite", "aluminum", "Bauxita", "Alumínio", "AlO(OH)", Category.METAL, 2.8F, 3.0F),

            // --- METAIS ESTRATÉGICOS & REFRATÁRIOS ---
            new MineralInfo("uraninite", "uranium", "Uraninita", "Urânio", "UO₂", Category.METAL, 4.0F, 3.0F),
            new MineralInfo("osmiridium", "osmium", "Osmirídio", "Ósmio", "(Os,Ir)", Category.METAL, 4.5F, 4.0F),
            new MineralInfo("sperrylite", "platinum", "Esperrilita", "Platina", "PtAs₂", Category.METAL, 4.0F, 3.5F),
            new MineralInfo("wolframite", "tungsten", "Wolframita", "Tungstênio", "(Fe,Mn)WO₄", Category.METAL, 4.5F, 4.0F),
            new MineralInfo("rutile", "titanium", "Rutilo", "Titânio", "TiO₂", Category.METAL, 4.0F, 3.5F),
            new MineralInfo("cobaltite", "cobalt", "Cobaltita", "Cobalto", "CoAsS", Category.METAL, 3.5F, 3.0F),

            // --- MINERAIS NÃO-METÁLICOS & REAGENTES QUÍMICOS ---
            new MineralInfo("sulfur", "sulfur", "Enxofre Nativo", "Enxofre", "S₈", Category.CHEMICAL, 2.0F, 2.0F),
            new MineralInfo("fluorite", "fluorite", "Fluorita", "Fluorita", "CaF₂", Category.CHEMICAL, 3.0F, 3.0F),
            new MineralInfo("cinnabar", "cinnabar", "Cinábrio", "Cinábrio", "HgS", Category.CHEMICAL, 2.5F, 2.5F),
            new MineralInfo("apatite", "apatite", "Apatita", "Apatita", "Ca₅(PO₄)₃(F,Cl,OH)", Category.CHEMICAL, 3.0F, 3.0F),
            new MineralInfo("halite", "salt", "Halita", "Sal Gema", "NaCl", Category.CHEMICAL, 2.0F, 2.0F),
            new MineralInfo("graphite", "graphite", "Grafite", "Grafite", "C", Category.CHEMICAL, 2.2F, 2.0F)
    );

    private MineralCatalog() {}

    public static MineralInfo findById(String id) {
        for (var mineral : MINERALS) {
            if (mineral.id().equals(id)) return mineral;
        }
        return null;
    }
}
