package br.com.quantumindustries.research;

import java.util.List;
import br.com.quantumindustries.assay.AssayCatalog.Material;
import br.com.quantumindustries.assay.AssayCatalog.Method;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class ResearchCatalog {
    public record Cost(Item item, int amount, String label) {}
    public record Study(String key, String name, String description, String reward, Item icon,
                        int era, List<String> prerequisites, List<Cost> costs, List<ResearchEvidence> evidence) {
        public Identifier id() { return ResearchIds.of(key); }
    }
    // Ordering is for display and transient menu messages only.
    public static final List<Study> STUDIES = List.of(
        new Study("conductivity", "Condutividade", "Após inspecionar o cobre, estude a condução elétrica.", "Pesquisa de fabricação de fios", Items.REDSTONE, 1, List.of("material_observation"), List.of(new Cost(Items.COPPER_INGOT, 3, "lingotes de cobre"), new Cost(Items.REDSTONE, 1, "redstone"), new Cost(Items.PAPER, 1, "papel")), List.of(new ResearchEvidence.Assay(Material.COPPER, Method.INSPECTION))),
        new Study("copper_wire", "Fabricação de Fios", "Técnica de fabricação de fios metálicos.", "Fio de cobre", Items.COPPER_INGOT, 1, List.of("conductivity"), List.of(new Cost(Items.COPPER_INGOT, 2, "lingotes de cobre"), new Cost(Items.PAPER, 1, "papel")), List.of(new ResearchEvidence.Assay(Material.COPPER, Method.MECHANICAL))),
        new Study("material_observation", "Observação dos Materiais", "Compare madeira, pedra e metal para iniciar suas investigações.", "Analisador de Materiais", Items.SPYGLASS, 1, List.of(), List.of(new Cost(Items.OAK_PLANKS, 1, "tábua de carvalho"), new Cost(Items.COBBLESTONE, 1, "pedregulho"), new Cost(Items.COPPER_INGOT, 1, "lingote de cobre")), List.of()),
        new Study("material_malleability", "Maleabilidade", "Observe a deformação plástica de um metal sob compressão.", "Pesquisa de Conformação Manual", Items.COPPER_INGOT, 1, List.of("material_observation"), List.of(new Cost(Items.COPPER_INGOT, 1, "lingote de cobre"), new Cost(Items.STONE, 1, "pedra")), List.of(new ResearchEvidence.AnyOf(List.of(new ResearchEvidence.Assay(Material.COPPER, Method.MECHANICAL), new ResearchEvidence.Assay(Material.GOLD, Method.MECHANICAL))))),
        new Study("manual_forming", "Conformação Manual", "Aprenda a trabalhar metais à mão.", "Martelo e bancada de conformação", Items.IRON_NUGGET, 1, List.of("material_malleability"), List.of(new Cost(Items.IRON_NUGGET, 3, "pepitas de ferro"), new Cost(Items.STICK, 2, "gravetos")), List.of()),
        new Study("metal_sheets", "Chapas Metálicas", "Aprenda a formar lâminas de metal.", "Chapas de ferro e cobre", Items.IRON_INGOT, 1, List.of("manual_forming"), List.of(new Cost(Items.IRON_INGOT, 1, "lingote de ferro"), new Cost(Items.COPPER_INGOT, 1, "lingote de cobre")), List.of(new ResearchEvidence.Assay(Material.IRON, Method.MECHANICAL))),
        new Study("electrical_insulation", "Isolamento", "Compare materiais não metálicos e de revestimento.", "Fio de cobre isolado", Items.LEATHER, 1, List.of("copper_wire"), List.of(new Cost(Items.LEATHER, 1, "couro"), new Cost(Items.PAPER, 1, "papel")), List.of(new ResearchEvidence.AnyOf(List.of(new ResearchEvidence.Assay(Material.COAL, Method.INSPECTION), new ResearchEvidence.Assay(Material.QUARTZ, Method.INSPECTION))))),
        new Study("basic_mechanics", "Mecânica Fundamental", "Estude mecanismos de transmissão.", "Engrenagem de ferro", Items.IRON_INGOT, 1, List.of("manual_forming"), List.of(new Cost(Items.IRON_INGOT, 1, "lingote de ferro")), List.of(new ResearchEvidence.Assay(Material.IRON, Method.MECHANICAL))),
        new Study("mechanical_transmission", "Transmissão Mecânica", "Transmita movimento por eixos.", "Eixo e manivela", Items.STICK, 1, List.of("basic_mechanics"), List.of(new Cost(Items.IRON_INGOT, 1, "lingote de ferro"), new Cost(Items.STICK, 1, "graveto")), List.of()),
        new Study("basic_magnetism", "Magnetismo", "Após inspecionar o ferro, investigue magnetismo com uma bússola.", "Conhecimento de magnetismo", Items.COMPASS, 1, List.of("material_observation"), List.of(new Cost(Items.COMPASS, 1, "bússola")), List.of(new ResearchEvidence.Assay(Material.IRON, Method.INSPECTION))),
        new Study("copper_winding", "Bobinagem", "Enrole um condutor sobre um suporte.", "Bobina de cobre", Items.COPPER_INGOT, 1, List.of("copper_wire"), List.of(new Cost(Items.STICK, 1, "graveto"), new Cost(Items.PAPER, 1, "papel")), List.of(new ResearchEvidence.Assay(Material.COPPER, Method.MECHANICAL))),
        new Study("electromechanical_structure", "Estrutura Eletromecânica", "Integre suportes, eixos e engrenagens após verificar a resposta térmica.", "Estrutura Eletromecânica e Núcleo Mecânico Primitivo", Items.IRON_INGOT, 1, List.of("metal_sheets", "mechanical_transmission", "basic_magnetism", "copper_winding"), List.of(new Cost(Items.IRON_INGOT, 1, "lingote de ferro")), List.of(new ResearchEvidence.AnyOf(List.of(new ResearchEvidence.Assay(Material.IRON, Method.HEATING), new ResearchEvidence.Assay(Material.COPPER, Method.HEATING))))),
        new Study("manual_dynamo_design", "Projeto do Dínamo", "Conclua a Era I e aplique movimento manual à geração.", "Dínamo Manual e acesso à Era II", Items.BOOK, 1, List.of("electrical_insulation", "electromechanical_structure"), List.of(new Cost(Items.PAPER, 1, "papel")), List.of()),
        new Study("electromagnetic_induction", "Indução Eletromagnética", "Estude a conversão de movimento em eletricidade e reatividade condutora.", "Fundamentos para futuras aplicações elétricas", Items.REDSTONE, 2, List.of("manual_dynamo_design"), List.of(new Cost(Items.REDSTONE, 1, "redstone"), new Cost(Items.IRON_INGOT, 1, "lingote de ferro")), List.of(new ResearchEvidence.Assay(Material.REDSTONE, Method.REACTION), new ResearchEvidence.AnyOf(List.of(new ResearchEvidence.Assay(Material.COPPER, Method.REACTION), new ResearchEvidence.Assay(Material.LAPIS, Method.REACTION)))))
    );

    public record Branch(String title, List<String> studies, String knowledge, String application) {}

    public static final List<Branch> ERA_ONE_BRANCHES = List.of(
            new Branch("Ciência dos Materiais", List.of("material_observation", "material_malleability", "manual_forming", "metal_sheets"),
                    "Metais maleáveis podem ser moldados por golpes controlados.",
                    "Use o martelo e a bancada de conformação para produzir chapas."),
            new Branch("Mecânica e Magnetismo", List.of("basic_mechanics", "mechanical_transmission", "basic_magnetism"),
                    "Engrenagens e eixos transmitem movimento. A bússola revela a orientação magnética.",
                    "Monte transmissões para a estrutura e o núcleo mecânico."),
            new Branch("Eletricidade Básica", List.of("conductivity", "copper_wire", "electrical_insulation", "copper_winding"),
                    "O cobre conduz eletricidade. Um revestimento isola o fio e o enrolamento forma uma bobina.",
                    "Prepare fios isolados e bobinas para os primeiros projetos elétricos.")
    );

    public static String sectionTitle(int study) {
        for (Branch branch : ERA_ONE_BRANCHES) {
            if (branch.studies().contains(STUDIES.get(study).key())) return branch.title();
        }
        return STUDIES.get(study).era() == 1 ? "Integração Eletromecânica" : "Era II — Automação";
    }

    private static final java.util.Map<String, Study> BY_KEY = STUDIES.stream().collect(
            java.util.stream.Collectors.toUnmodifiableMap(Study::key, study -> study));
    private static final java.util.Map<Identifier, Study> BY_ID = STUDIES.stream().collect(
            java.util.stream.Collectors.toUnmodifiableMap(Study::id, study -> study));

    static {
        var visited = new java.util.HashSet<String>();
        var visiting = new java.util.HashSet<String>();
        for (String key : BY_KEY.keySet()) validate(key, visited, visiting);
    }

    private static void validate(String key, java.util.Set<String> visited, java.util.Set<String> visiting) {
        if (visited.contains(key)) return;
        Study study = BY_KEY.get(key);
        if (study == null) throw new IllegalStateException("Unknown research prerequisite: " + key);
        if (study.evidence().size() > 16) throw new IllegalStateException("Too many evidence requirements: " + key);
        if (!visiting.add(key)) throw new IllegalStateException("Cyclic research prerequisite: " + key);
        for (String dependency : study.prerequisites()) validate(dependency, visited, visiting);
        visiting.remove(key);
        visited.add(key);
    }

    public static Identifier identifier(String value) {
        return ResearchIds.parse(value);
    }

    public static Study find(Identifier id) { return id == null ? null : BY_ID.get(id); }

    public static Study find(String key) { return find(identifier(key)); }

    public static int indexOf(String key) { return STUDIES.indexOf(find(key)); }

    private ResearchCatalog() {}

    public static boolean valid(int id) { return id >= 0 && id < STUDIES.size(); }
}
