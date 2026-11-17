package br.com.quantumindustries.research;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.research.ResearchCatalog.Study;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class GuidebookCatalog {

    public record Chapter(String id, String studyKey, int era, String title, String subtitle,
                          Supplier<ItemStack> icon, List<String> paragraphs, String operationSummary) {
        public Identifier identifier() { return ResearchIds.of(id); }
    }

    public static final Chapter INTRO_VAULT = new Chapter(
        "intro_vault",
        null,
        0,
        "O Vault Caído & Primeiros Passos",
        "Registro de Sobrevivência e Inicialização Tecnológica",
        () -> new ItemStack(ModItems.SCIENTIFIC_ANALYZER_TIER_1.get()),
        List.of(
            "Este Códice foi recuperado de dentro do Quantum Vault que caiu próximo ao seu ponto de chegada. Seus módulos avançados de engenharia estão protegidos por criptografia de progresso (????) e só serão decodificados conforme você comprovar domínio sobre cada princípio científico.",
            "Este manual NÃO entrega receitas prontas de bancada — sua função é ensinar como operar cada máquina, gerador, forno metalúrgico e circuito que você descobrir.",
            "Sua primeira missão é criar o Caderno de Pesquisas (Mesa de Pesquisa Nível 1) e a Bancada de Ensaios Empíricos (Analisador de Materiais). Com eles, inspecione materiais básicos (madeira, pedra e cobre) para destravar as primeiras pesquisas e revelar os capítulos ???? deste livro."
        ),
        "1. Construa o Caderno/Mesa de Pesquisa e o Analisador de Materiais. 2. Realize ensaios empíricos nas amostras e conclua pesquisas na Mesa para decodificar automaticamente os capítulos ???? deste Códice."
    );

    public static final List<Chapter> CHAPTERS = List.of(
        INTRO_VAULT,
        new Chapter("material_observation", "material_observation", 1,
            "Analisador de Materiais",
            "Bancada de Ensaios Empíricos (Inspeção, Impacto, Calor e Ácido)",
            () -> new ItemStack(ModItems.EMPIRICAL_ASSAY_BENCH.get()),
            List.of(
                "A Bancada de Ensaios Empíricos permite investigar as propriedades físicas e químicas de 13 materiais fundamentais antes de desbloquear novas tecnologias na Mesa de Pesquisa.",
                "Existem 4 métodos de ensaio: Inspeção Óptica (Lupa de Bancada), Impacto Mecânico (Martelo / Pesos de Latão), Aquecimento (Fole / Kit de Teste de Chama) e Reação Ácida (Frasco de Ensaio).",
                "Cada descoberta empírica pode ser registrada no Caderno de Campo Empírico e satisfaz as evidências exigidas pelas pesquisas da Era I e Era II."
            ),
            "Coloque a amostra mineral na bancada e aplique o instrumento científico adequado (Lupa, Martelo, Fole/Chama ou Frasco) para registrar a evidência."
        ),
        new Chapter("manual_forming", "manual_forming", 1,
            "Bancada de Conformação & Martelo",
            "Deformação Plástica a Frio e Laminação Manual de Chapas",
            () -> new ItemStack(ModItems.FORMING_BENCH.get()),
            List.of(
                "A Bancada de Conformação é a primeira máquina de processamento metalúrgico da oficina. Ela absorve golpes controlados do Martelo de Conformação para transformar lingotes em chapas laminadas.",
                "Metais maleáveis como Cobre, Ferro, Ouro, Estanho e Zinco podem ser conformados diretamente sobre o barramento de impacto da bancada."
            ),
            "Segure o Martelo na mão principal e o lingote metálico na mão secundária (ou posicione na bancada) e aplique 3 golpes sobre a Bancada de Conformação para obter 1 Chapa Metálica."
        ),
        new Chapter("alloy_metallurgy", "alloy_metallurgy", 1,
            "Forno de Ligas a Fole",
            "Pirometalurgia Primitiva — Fusão de Bronze (Cu-Sn) e Latão (Cu-Zn)",
            () -> new ItemStack(ModItems.BELLOWS_ALLOY_KILN.get()),
            List.of(
                "O Forno de Ligas a Fole permite alcançar temperaturas superiores às de uma fornalha comum através da injeção forçada de ar, fundindo dois metais em uma única liga estrutural.",
                "Na Era I, ele é essencial para combinar Cobre + Estanho (gerando Bronze de baixo atrito para engrenagens) e Cobre + Zinco (gerando Latão para instrumentos de precisão)."
            ),
            "Insira o combustível (Carvão) e os dois metais da receita no Forno de Ligas a Fole. Acione o fole lateral para acelerar a tiragem térmica e recolha os lingotes de liga fundidos."
        ),
        new Chapter("mechanical_transmission", "mechanical_transmission", 1,
            "Eixos, Manivela & Serraria Mecânica",
            "Transmissão de Torque Rotacional e Corte Mecanizado de Madeira",
            () -> new ItemStack(ModItems.MECHANICAL_SAWMILL.get()),
            List.of(
                "Os Eixos Mecânicos conectam fontes de torque (como a Manivela Manual ou a Roda-d'Água) às máquinas operatrizes da oficina, como a Serraria Mecânica e o Moinho de Grãos.",
                "A Serraria Mecânica utiliza lâminas rotativas movidas por eixo para desdobrar toras de madeira com rendimento superior ao trabalho manual."
            ),
            "Conecte Eixos Mecânicos na entrada da Serraria Mecânica ou Moinho Manual, acione por Manivela ou Roda-d'Água e insira toras/grãos no topo da máquina."
        ),
        new Chapter("hydraulic_power", "hydraulic_power", 1,
            "Roda-d'Água Primitiva",
            "Geração Contínua de Torque Mecânico por Fluxo Hidráulico",
            () -> new ItemStack(ModItems.PRIMITIVE_WATER_WHEEL.get()),
            List.of(
                "A Roda-d'Água Primitiva converte a energia potencial e cinética de uma queda d'água em rotação mecânica constante, substituindo o esforço manual da manivela.",
                "Quando corretamente posicionada em contato com água corrente, ela alimenta linhas de eixos para manter serrarias, britadores e dínamos operando sem interrupção."
            ),
            "Posicione a Roda-d'Água com água corrente descendo pelas pás laterais e conecte Eixos Mecânicos ao cubo central para distribuir o torque."
        ),
        new Chapter("mineral_beneficiation", "mineral_beneficiation", 1,
            "Britador de Mandíbulas",
            "Cominuição de Minérios Brutos e Duplicação Metalúrgica (2x Pós)",
            () -> new ItemStack(ModItems.JAW_CRUSHER.get()),
            List.of(
                "O Britador de Mandíbulas comprime minérios brutos entre duas placas dentadas de alta dureza, reduzindo cada minério bruto a 2 Pós Minerais purificados.",
                "Ele é uma máquina híbrida: pode ser operado por pulsos mecânicos na Era I ou alimentado continuamente por energia elétrica (FE) na Era II, dobrando sua produção de lingotes."
            ),
            "Insira minério bruto no Britador de Mandíbulas e acione mecânica ou eletricamente (FE) para obter 2x Pó Mineral, que pode ser fundido diretamente em lingotes."
        ),
        new Chapter("copper_wire", "copper_wire", 1,
            "Cabos de Cobre & Condutos Isolados",
            "Distribuição de Energia Elétrica (FE) em Baixa e Média Tensão",
            () -> new ItemStack(ModItems.COPPER_WIRE.get()),
            List.of(
                "Os Fios de Cobre e Fios de Cobre Isolados podem ser instalados diretamente no chão, paredes e tetos, conectando-se automaticamente em 6 direções entre geradores, baterias e máquinas.",
                "O Fio de Cobre nu funciona como barramento básico (com perdas térmicas e risco de choque quando energizado), enquanto o Fio de Cobre Isolado protege o operador e reduz perdas na linha."
            ),
            "Clique com o botão direito segurando o Fio de Cobre ou Fio de Cobre Isolado diretamente no bloco para instalar a rede elétrica entre seus geradores e máquinas."
        ),
        new Chapter("manual_dynamo_design", "manual_dynamo_design", 1,
            "Dínamo Manual Eletromagnético",
            "Conversão de Rotação Mecânica em Energia Elétrica (Forge Energy)",
            () -> new ItemStack(ModItems.MANUAL_DYNAMO.get()),
            List.of(
                "O Dínamo Manual inaugura a transição para a Era Elétrica. Seu rotor magnético gira no interior de bobinas de cobre estacionárias, induzindo corrente elétrica contínua (FE).",
                "Pode ser acionado manualmente ou acoplado a eixos mecânicos e rodas-d'água, enviando energia diretamente para cabos de cobre ou acumuladores adjacentes."
            ),
            "Acione o Dínamo Manual com o botão direito (ou conecte torque mecânico) e ligue Cabos de Cobre na saída para alimentar acumuladores e máquinas elétricas."
        ),
        new Chapter("scientific_analyzer_tier_2", "scientific_analyzer_tier_2", 1,
            "Analisador Científico — Tier 2",
            "Estação Computacional e Desbloqueio da Era II: Elétrica",
            () -> new ItemStack(ModItems.SCIENTIFIC_ANALYZER.get()),
            List.of(
                "O Analisador Científico Tier 2 é o divisor de águas entre a oficina mecânica primitiva e a engenharia eletromagnética moderna.",
                "Equipado com osciloscópio, visor de rede cristalina e barramentos digitais, esta bancada permite transitar entre as Eras e conduzir as pesquisas da Era II (Elétrica).",
                "Construa-o utilizando ardósia profunda, lápis-lazúli, chapas de cobre e o seu caderno para dar o salto definitivo rumo à automação elétrica."
            ),
            "Instale o Analisador Científico Tier 2 na sua oficina e interaja com ele para acessar o novo painel ciano e pesquisar as tecnologias da Era II."
        ),
        new Chapter("galvanic_storage", "galvanic_storage", 2,
            "Pilha Voltaica & Acumulador Chumbo-Ácido",
            "Armazenamento Eletroquímico de Energia (FE) e Estabilização de Rede",
            () -> new ItemStack(ModItems.LEAD_ACID_ACCUMULATOR.get()),
            List.of(
                "A Pilha Voltaica e o Acumulador Chumbo-Ácido estocam energia elétrica na forma de potencial químico entre placas de cobre/zinco ou chumbo submersas em eletrólito ácido.",
                "Eles atuam como pulmão energético da fábrica, absorvendo o excesso gerado pelos dínamos e fornecendo alta corrente sob demanda para prensas e fornos de indução."
            ),
            "Conecte a entrada do Acumulador aos geradores e a saída às máquinas via Cabos de Cobre. Use os Óculos de Inspeção ou Sonda de Teste para monitorar a carga em FE."
        ),
        new Chapter("load_protection", "load_protection", 2,
            "Relé de Carga & Transdutor Piezoelétrico",
            "Automação de Bateria por Limiares e Geração por Impacto Mecânico",
            () -> new ItemStack(ModItems.LOAD_RELAY.get()),
            List.of(
                "O Relé de Carga monitora o nível de energia de acumuladores adjacentes e emite ou corta a passagem de corrente/sinal de Redstone conforme os limites configurados, evitando descargas profundas.",
                "Já o Transdutor Piezoelétrico utiliza cristais de quartzo para converter pisadas, impactos ou pressão mecânica em pulsos elétricos instantâneos."
            ),
            "Posicione o Relé de Carga junto ao banco de baterias para automatizar o acionamento de geradores e linhas de consumo."
        ),
        new Chapter("thermal_annealing", "thermal_annealing", 2,
            "Forja de Recozimento a Fole",
            "Tratamento Térmico de Metais e Lingotes Aquecidos de Alta Ductilidade",
            () -> new ItemStack(ModItems.BELLOWS_BRAZIER.get()),
            List.of(
                "A Forja de Recozimento (Braseiro a Fole) aquece lingotes de ferro, cobre, bronze, latão e aço até sua faixa de recristalização, aliviando tensões internas da rede cristalina.",
                "Lingotes aquecidos apresentam resistência mecânica reduzida à deformação, sendo indispensáveis para processos avançados de conformação e trefilação."
            ),
            "Abasteça o Braseiro com carvão, insira os lingotes metálicos e impulsione ar com o Fole Manual para retirar Lingotes Aquecidos prontos para forjamento."
        ),
        new Chapter("wire_drawing", "wire_drawing", 2,
            "Trefiladora de Fios de Bancada",
            "Estiramento Mecânico por Matriz — 4 Fios por Chapa e Fios Nobres",
            () -> new ItemStack(ModItems.WIRE_DRAWING_BENCH.get()),
            List.of(
                "A Trefiladora de Bancada traciona chapas metálicas através de matrizes calibradas de redução progressiva, produzindo 4 fios condutores por chapa sem perdas de rebarba.",
                "Permite trefilar não apenas cobre com alta eficiência, mas também Ouro, Electrum e Constantan para instrumentação e bobinas térmicas."
            ),
            "Posicione a chapa metálica na Trefiladora de Bancada e acione o mecanismo de tração para estirar 4 fios calibrados por operação."
        ),
        new Chapter("electromechanical_forming", "electromechanical_forming", 2,
            "Prensa Eletromecânica Industrial",
            "Estampagem e Laminação Pesada Movida a Forge Energy (FE)",
            () -> new ItemStack(ModItems.ELECTROMECHANICAL_PRESS.get()),
            List.of(
                "A Prensa Eletromecânica substitui o trabalho braçal na Bancada de Conformação por um atuador linear motorizado alimentado por energia elétrica (FE).",
                "Capaz de laminar chapas de aço, invar, bronze e ferro de maneira totalmente automatizada assim que recebe matéria-prima e carga elétrica suficiente."
            ),
            "Ligue um Cabo de Cobre energizado à Prensa Eletromecânica e alimente lingotes na câmara superior para produzir chapas metálicas automaticamente."
        ),
        new Chapter("electric_alloy_smelting", "electric_alloy_smelting", 2,
            "Fundidora de Ligas Elétrica",
            "Fusão Eletrotérmica por Indução — Aço, Invar, Electrum e Constantan",
            () -> new ItemStack(ModItems.ELECTRIC_ALLOY_SMELTER.get()),
            List.of(
                "A Fundidora de Ligas Elétrica (Alloy Smelter) utiliza bobinas de indução alimentadas por FE para atingir temperaturas que o Forno a Fole não alcança com precisão estequiométrica.",
                "Desbloqueia as ligas de engenharia da Era II: Aço (Ferro + Carvão), Invar (Ferro + Níquel), Electrum (Ouro + Prata) e Constantan (Cobre + Níquel)."
            ),
            "Conecte alimentação elétrica (FE) à Fundidora de Ligas Elétrica e insira os dois componentes metálicos/pós para sintetizar ligas especiais de alta pureza."
        ),
        new Chapter("fluid_hydrostatics", "fluid_hydrostatics", 2,
            "Tubos Hidráulicos, Bomba Peristáltica & Cuba",
            "Hidrostática, Transporte de Fluidos (50 mB/t) e Decantação Química",
            () -> new ItemStack(ModItems.SETTLING_VAT.get()),
            List.of(
                "Os Dutos de Cobre e Visores de Vidro transportam líquidos por vasos comunicantes e pressão, enquanto a Bomba Peristáltica impulsiona fluidos a 50 mB/t vencendo desníveis.",
                "A Cuba de Decantação utiliza repouso gravitacional e reagentes líquidos para separar sedimentos minerais e preparar soluções eletrolíticas."
            ),
            "Interligue tanques e a Cuba de Decantação com Dutos de Fluido e instale a Bomba Peristáltica para transferir água, eletrólitos e reagentes."
        ),
        new Chapter("pneumatic_transport", "pneumatic_transport", 2,
            "Tubos Pneumáticos & Classificador",
            "Logística Automatizada de Itens por Pulso de Ar e Filtragem",
            () -> new ItemStack(ModItems.PNEUMATIC_TUBE.get()),
            List.of(
                "O Ejetor a Fole injeta pulsos de ar comprimido que deslocam itens em alta velocidade pelo interior dos Tubos Pneumáticos entre fornos, britadores e baús.",
                "O Classificador Gravitacional separa automaticamente os itens em trânsito por massa e tipo, organizando linhas de produção sem intervenção manual."
            ),
            "Instale o Ejetor a Fole na saída das máquinas, conecte Tubos Pneumáticos até o destino e use o Classificador Gravitacional para rotear minérios, pós e lingotes."
        ),
        new Chapter("chemical_leaching", "chemical_leaching", 2,
            "Reagentes Ácidos & Superfosfato",
            "Química Mineral — Ácido Clorídrico (HCl) e Fertilizante Fosfatado",
            () -> new ItemStack(ModItems.PHOSPHATE_FERTILIZER.get()),
            List.of(
                "Na Cuba de Decantação, a salmoura de Halita reage para produzir Ácido Clorídrico Diluído (HCl), um poderoso reagente de decapagem e lixiviação mineral.",
                "Combinando Apatita beneficiada e enxofre em meio ácido, você sintetiza o Fertilizante Fosfatado, capaz de acelerar drasticamente o crescimento agrícola em área."
            ),
            "Processe Halita na Cuba de Decantação para obter HCl e utilize o Fertilizante Fosfatado diretamente nas plantações para estimular o crescimento imediato."
        )
    );

    static {
        for (Chapter chapter : CHAPTERS) {
            if (chapter.studyKey() != null) {
                Study study = ResearchCatalog.find(ResearchCatalog.identifier(chapter.studyKey()));
                if (study == null) {
                    throw new IllegalStateException("Study key '" + chapter.studyKey() + "' not found in ResearchCatalog");
                }
            }
        }
    }

    private GuidebookCatalog() {}

    public static Chapter introVault() { return INTRO_VAULT; }
    public static List<Chapter> chapters() { return CHAPTERS; }
}
