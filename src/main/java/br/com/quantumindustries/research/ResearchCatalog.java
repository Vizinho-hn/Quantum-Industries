package br.com.quantumindustries.research;

import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class ResearchCatalog {
    public record Cost(Item item, int amount, String label) {}
    public record Study(String key, String name, String description, String reward, Item icon,
                        int era, List<Integer> prerequisites, List<Cost> costs) {}
    // Positions 0 and 1 retain the saved bits and report IDs of previous versions.
    public static final List<Study> STUDIES = List.of(
        new Study("conductivity", "Condutividade", "Estudo da condução elétrica.", "Pesquisa de fabricação de fios", Items.REDSTONE, 1, List.of(), List.of(new Cost(Items.COPPER_INGOT, 3, "lingotes de cobre"), new Cost(Items.REDSTONE, 1, "redstone"), new Cost(Items.PAPER, 1, "papel"))),
        new Study("copper_wire", "Fabricação de Fios", "Técnica de fabricação de fios metálicos.", "Fio de cobre", Items.COPPER_INGOT, 1, List.of(0), List.of(new Cost(Items.COPPER_INGOT, 2, "lingotes de cobre"), new Cost(Items.PAPER, 1, "papel"))),
        new Study("material_observation", "Observação dos Materiais", "Compare madeira, pedra e metal.", "Maleabilidade e Magnetismo", Items.SPYGLASS, 1, List.of(), List.of(new Cost(Items.OAK_PLANKS, 1, "tábua de carvalho"), new Cost(Items.COBBLESTONE, 1, "pedregulho"), new Cost(Items.COPPER_INGOT, 1, "lingote de cobre"))),
        new Study("material_malleability", "Maleabilidade", "Observe a deformação de um metal.", "Pesquisa de Conformação Manual", Items.COPPER_INGOT, 1, List.of(2), List.of(new Cost(Items.COPPER_INGOT, 1, "lingote de cobre"), new Cost(Items.STONE, 1, "pedra"))),
        new Study("manual_forming", "Conformação Manual", "Aprenda a trabalhar metais à mão.", "Martelo e bancada de conformação", Items.IRON_NUGGET, 1, List.of(3), List.of(new Cost(Items.IRON_NUGGET, 3, "pepitas de ferro"), new Cost(Items.STICK, 2, "gravetos"))),
        new Study("metal_sheets", "Chapas Metálicas", "Aprenda a formar lâminas de metal.", "Chapas de ferro e cobre", Items.IRON_INGOT, 1, List.of(4), List.of(new Cost(Items.IRON_INGOT, 1, "lingote de ferro"), new Cost(Items.COPPER_INGOT, 1, "lingote de cobre"))),
        new Study("electrical_insulation", "Isolamento", "Compare materiais de revestimento.", "Fio de cobre isolado", Items.LEATHER, 1, List.of(1), List.of(new Cost(Items.LEATHER, 1, "couro"), new Cost(Items.PAPER, 1, "papel"))),
        new Study("basic_mechanics", "Mecânica Fundamental", "Estude mecanismos de transmissão.", "Engrenagem de ferro", Items.IRON_INGOT, 1, List.of(4), List.of(new Cost(Items.IRON_INGOT, 1, "lingote de ferro"))),
        new Study("mechanical_transmission", "Transmissão Mecânica", "Transmita movimento por eixos.", "Eixo e manivela", Items.STICK, 1, List.of(7), List.of(new Cost(Items.IRON_INGOT, 1, "lingote de ferro"), new Cost(Items.STICK, 1, "graveto"))),
        new Study("basic_magnetism", "Magnetismo", "Observe a indicação de uma bússola.", "Conhecimento de magnetismo", Items.COMPASS, 1, List.of(2), List.of(new Cost(Items.COMPASS, 1, "bússola"))),
        new Study("copper_winding", "Bobinagem", "Enrole um condutor sobre um suporte.", "Bobina de cobre", Items.COPPER_INGOT, 1, List.of(1), List.of(new Cost(Items.STICK, 1, "graveto"), new Cost(Items.PAPER, 1, "papel"))),
        new Study("electromechanical_structure", "Estrutura Eletromecânica", "Projete um suporte para as peças.", "Estrutura eletromecânica", Items.IRON_INGOT, 1, List.of(5, 8, 9, 10), List.of(new Cost(Items.IRON_INGOT, 1, "lingote de ferro"))),
        new Study("manual_dynamo_design", "Projeto do Dínamo", "Conclua o projeto de geração manual.", "Era II e Indução Eletromagnética", Items.BOOK, 1, List.of(6, 11), List.of(new Cost(Items.PAPER, 1, "papel"))),
        new Study("electromagnetic_induction", "Indução Eletromagnética", "Converta movimento em eletricidade.", "Dínamo Manual", Items.REDSTONE, 2, List.of(12), List.of(new Cost(Items.REDSTONE, 1, "redstone"), new Cost(Items.IRON_INGOT, 1, "lingote de ferro")))
    );

    public record Branch(String title, List<Integer> studies, String knowledge, String application) {}

    public static final List<Branch> ERA_ONE_BRANCHES = List.of(
            new Branch("Ciência dos Materiais", List.of(2, 3, 4, 5),
                    "Metais maleáveis podem ser moldados por golpes controlados.",
                    "Use o martelo e a bancada de conformação para produzir chapas."),
            new Branch("Mecânica e Magnetismo", List.of(7, 8, 9),
                    "Engrenagens e eixos transmitem movimento. A bússola revela a orientação magnética.",
                    "Monte transmissões manuais e reúna os fundamentos da estrutura eletromecânica."),
            new Branch("Eletricidade Básica", List.of(0, 1, 6, 10),
                    "O cobre conduz eletricidade. Um revestimento isola o fio e o enrolamento forma uma bobina.",
                    "Prepare fios isolados e bobinas para os primeiros projetos elétricos.")
    );

    public static String sectionTitle(int study) {
        for (Branch branch : ERA_ONE_BRANCHES) {
            if (branch.studies().contains(study)) return branch.title();
        }
        return STUDIES.get(study).era() == 1 ? "Integração Eletromecânica" : "Era II — Automação";
    }

    private ResearchCatalog() {}

    public static boolean valid(int id) { return id >= 0 && id < STUDIES.size(); }
}
