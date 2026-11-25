# Roadmap — Quantum Industries

Este roadmap registra a direção do projeto. Itens são planejados, não promessas de entrega. As fases podem ser ajustadas após protótipos e testes.

As fases de entrega abaixo não são números de era. A divisão tecnológica é:

- **Era I — Mecânica Clássica e Ciência Empírica (QKS).** Observação, ensaios, oficina e transmissão mecânica. O Dínamo Manual faz a ponte para aplicações elétricas.
- **Era II — Industrialização Científica.** Ramo A de Termometria e Termodinâmica (QTS), ramo B de Eletrostática e Eletrodinâmica (QES) e faixa de Integração Industrial. Os tubos pneumáticos continuam na Era II.
- **Eras futuras.** Era III de Ondas Eletromagnéticas e Tecnologia Moderna, incluindo QWS e QPS. Era IV de Engenharia Quântica e Era V de Inteligência Artificial e Automação Autônoma.

Consulte a [arquitetura dos sistemas físicos](docs/PHYSICS_SYSTEMS_ARCHITECTURE.md).

## Fase 0 — Fundação técnica

- [x] Fixar Minecraft 26.1.2, NeoForge 26.1.2.125, Java 25 e Gradle 9.2.1.
- [x] Configurar build, cliente, servidor dedicado e estrutura de recursos.
- [x] Configurar CI para testes de contrato, migração de pesquisas via Gradle (com as dependências do jogo) e compilação.

## Fase 1 — Núcleo de pesquisa

- [x] Implementar a Bancada de Ensaio Empírico expandida para treze materiais (incluindo Estanho, Zinco e Chumbo), quatro métodos e 52 descobertas individualizadas.
- [x] Implementar Analisadores Científicos Tier 1 e Tier 2, árvore, caderno, receitas e relatórios.
- [x] Localizar códice, caderno, pesquisas, relatórios e descrições dos itens em PT-BR/EN-US, acompanhando o idioma do jogo.
- [x] Dar à Mesa de Pesquisa Tier 1 um modelo artesanal próprio, com caderno e instrumentos manuais, textura pixel art e arquivo Blockbench.
- [x] Persistir conhecimentos por jogador e validar pesquisas, relatórios e crafts no servidor.
- [x] Configurar seis pontas de tubos de itens, fluidos e cabos elétricos com chave (Puxar/Empurrar/Nenhum), leitura nos óculos, modo automático compatível e persistência.
- [x] Exibir Entrada e saída no ciclo da chave, permitindo carregar e descarregar baterias pela mesma ponta.
- [x] Serraria integrada aos tubos, vínculo persistente de pesquisa e painel dos óculos com estoques/progresso/atividade real.
- [ ] Analisador de Rede Elétrica MK1 (Era II): validar topologia local, telemetria e painel em cliente/servidor; MK2 (Era III) acrescentará comunicação sem fio de dados; MK3 (Era IV) controle remoto e limites; Era V otimização por IA.
- [x] Corrigir leitura elétrica dos cabos: fluxo, limite e perdas separados da reserva conectada, sem inventar armazenamento no condutor.
- [x] Inspeção hidráulica 3D sob demanda: fluxo, parcelas reais de torque e diagnóstico das rodas 1×1/3×3/5×5, com capítulo opcional no Códice.
- [x] Alimentação inteligente da Fundidora Elétrica: seleção de materiais aceitos no baú, um metal por entrada e devolução segura de excedentes em trânsito.
- [x] Adicionar interfaces nativas à Fundidora Elétrica e ao Britador, com inventário, slots persistentes, energia, progresso, nome do material e botão de manivela validado no servidor.
- [x] Tornar Observação dos Materiais a entrada da Era I e liberar o Analisador de Materiais.
- [x] Integrar evidências científicas reutilizáveis (inspeção, prensa, aquecimento e reação) às pesquisas, relatórios e HUD.
- [x] Integrar as receitas desbloqueadas ao livro de receitas do Minecraft.
- [x] Adicionar comandos administrativos de desbloqueio e limpeza de pesquisas.
- [ ] Expandir as aplicações dos demais experimentos em máquinas e processos da Era II.\n- [x] Permitir compartilhar quatro observações empíricas com Fichas de Observação escritas no Analisador.

- [ ] Validar em servidor dedicado a autorização persistente do britador/forno/fundidora, o orçamento de cabos com perdas e a análise instrumental Tier 2 (ver `docs/ERA2_STABILIZATION.md`).

## Fase 2 — Primeira indústria

- [ ] Integrar a Fornalha Elétrica da Era II, liberada por Efeito Joule, com transferência de FE para a reserva térmica, consumo de calor por receita e verificação de persistência.

- [x] Oficina mecânica da Era I: serraria manual, eixos funcionais e roda-d'água (nesta branch; integração por PR).
- [x] Receitas JSON de processamento, sincronização nativa e prévias automáticas no caderno.
- [x] Testes automatizados da cadeia em survival com suprimentos preparados, reinício real, carga compartilhada, inventários e chunks.
- [ ] Moinho de Pedra, Prensa Mecânica de Fuso, Guincho de Mineração e Bomba Mecânica.
- [ ] **Era I — ciência do cotidiano (PR de protótipo):** validar Moedor Manual de Grãos, farinha, massa e Pão Rústico em survival; modelo 3D provisório e ícones temporários.
- [ ] Testar processo manual (4 giros), 2 trigos + água → pão, devolução de balde, drops no break, save/reload, som/UI e interação multiplayer.
- [ ] Próxima expansão opcional: fermento/cultura-mãe reutilizável, Pão Fermentado com maior saturação e Moedor Elétrico como upgrade; sem travar novas eras.
- [ ] Refinar visual no Blockbench e criar sprites 32×32 originais antes de declarar asset final.
- [x] Criar núcleo matemático isolado para aceleração angular/inércia e capítulo **opcional** de física rotacional no Códice, sem bloquear pesquisas existentes.
- [x] **QKS PR 1 — Core / Network / Chunk Sync:** integrate component dynamics, shared network solving and server-client chunk synchronization, preserving UM and progression; see `docs/KINETIC_ENGINE_ARCHITECTURE.md`.
- [x] **QKS PR 1 — Native Renderer:** migrate shafts, water wheels, sawmills and dynamos off the GeckoLib API while preserving geometry; client preview and server regression suites completed.
- [x] **QKS PR 2 — Stage D:** `small_cogwheel` (Z=8), `large_cogwheel` (Z=16), `iron_flywheel`, `mechanical_pulley` + `drive_belt`, `medium_water_wheel` (3×3) and `large_water_wheel` (5×5).
- [x] **QKS PR 3 — Stage E (implementação):** telemetria cinética nos Óculos de Inspeção, capítulos opcionais de física no Códice e suíte dedicada `tools/verify-qks.sh` com reinício write/read.
- [x] **Aceitação QKS PR 3:** `bash tools/verify-qks.sh` e suíte dedicada aprovados nas fases write/read, com persistência após reinício e isolamento de classes nas duas JVMs; preview cliente do HUD normal, diagnósticos com Shift e paginação do Códice verificado.
- [x] Serraria Elétrica da Era II: entradas/saídas 2×2, 8 tábuas por tora, FE, tubos, interface, inspeção, pesquisa, Códice e JEI (branch em revisão; ver `docs/ELECTRIC_SAWMILL.md`).
- [ ] Continuar as transmissões avançadas e a evolução elétrica das demais máquinas da Era II.

- [x] Implementar as 13 pesquisas originais da Era I e acrescentar Aproveitamento Hidráulico e Metalurgia de Ligas, preservando os IDs e a migração.
- [x] Liberar o Núcleo Mecânico Primitivo pela pesquisa de Estrutura Eletromecânica.
- [x] Liberar o Dínamo Manual pelo Projeto do Dínamo e manter Indução Eletromagnética na Era II.
- [x] Acionar o mesmo Dínamo por roda-d'água e eixos, com carga de 6 UM, geração de 12 FE/8 ticks e autorização individual persistente.
- [x] Tratar **eras como agrupamentos**, não bloqueios globais: abas com pesquisas podem ser abertas, com pré-requisitos de cada estudo preservados.
- [x] Desacoplar os ramos de Hidrostática, Pressão Pneumática, Recozimento Térmico e Trefilação de pesquisas elétricas que não são necessárias ao processo.
- [ ] Testar em cliente/servidor o acesso paralelo aos ramos e a persistência dos saves, sem liberar por engano receitas protegidas.
- [ ] Evoluir com **invenções opcionais de rendimento** (Moedor Manual → Farinha → Pão melhorado, moedor elétrico depois), sem torná-las pré-requisito para a próxima era.
- [ ] Adicionar interface contextual para diferenciar **descoberta fundamental**, **aplicação opcional** e **melhoria de rendimento**, sem tornar a árvore visualmente pesada.
- [x] Implementar manivela separada, geração manual, armazenamento de FE e leitura com óculos.
- [x] Expansão mineralógica e geológica: 18 minerais reais (36 blocos Stone/Deepslate, brutos, lingotes, pepitas e pós químicos) com geração no Overworld (Worldgen) e Common Tags `#c:*`.
- [x] Implementar Forno de Ligas a Fole (Era I — seis ligas; revisão de progressão em validação) e Fundidora de Ligas Elétrica / Alloy Smelter (Era II — Bronze, Latão, Invar, Electrum, Constantan e Aço), além da cadeia chumbo-ácido para o Acumulador Galvânico.
- [x] Implementar Britador de Mandíbulas (`jaw_crusher` — Manivela ou FE) com duplicação de minérios brutos em pós purificados (1 bruto → 2 pós), moagem mecanizada de trigo (1 trigo → 2 farinhas) e trituração de carvão em grafite.
- [x] Integrar minerais químicos reais: Fluorita como fluxo metalúrgico no Forno a Fole, Sal Gema (Halita) na Cuba de Decantação para produzir Ácido Clorídrico Diluído (abstração de gameplay), Grafite em Tijolos Refratários e Apatita + Enxofre em Fertilizante Fosfatado.
- [x] Expandir Trefiladora (`electrum_wire`, `constantan_wire`, `constantan_coil`) e Recozimento Térmico (`heated_bronze_ingot`, `heated_brass_ingot`, `heated_steel_ingot`).
- [ ] Validar a progressão completa com coleta em survival e dois jogadores reais.
- [ ] Validar as seis ligas manuais, os crafts revisados do forno e britador, e o caminho até a primeira Fundidora Elétrica em servidor real.
- [x] Implementar os primeiros blocos da Era II: Acumulador Galvânico (2.400 FE), Prensa Eletromecânica (60 FE/chapa) e transferência adjacente com leitura nos Óculos de Inspeção.
- [x] Introduzir Barramento de Cobre Nu e Conduíte Isolado para distribuir FE com limites de vazão e perda por caminho; conexões, modelos e hitboxes nas seis direções, incluindo pilhas verticais.\n- [x] Introduzir Trefiladora com acionamento manual ou FE e fios de cobre/ouro (4 por chapa).\n- [x] Introduzir forja térmica com fole e pré-aquecimento para conformação/prensa.\n- [x] Introduzir transdutor piezoelétrico e relé de carga por limiares de acumulador.\n- [x] Criar tubos pneumáticos com animação de pacotes, ejetor manual/FE e classificador gravitacional.
- [x] Criar dutos hidráulicos nativos NeoForge, bomba peristáltica manual/FE e cuba de decantação 8.000 mB (com suporte a Pó de Enxofre, Lápis-Lazúli e Sal Gema).
- [ ] Criar fluido registrado de eletrólito refinado e integrar como insumo líquido ao Acumulador sem invalidar receitas anteriores.
- [ ] Validar redes energéticas e novas máquinas de processamento em mundo survival e servidor dedicado.

## Fase 3 — Química e materiais
- [x] Primeiros reagentes e processos químicos aplicados (Ácido Clorídrico Diluído por receita abstrata de gameplay, Fluxo de Fluorita, Fertilizante Fosfatado e Tijolo Refratário de Grafite).
- [ ] Reações e processos químicos de múltiplas etapas (lixiviação ácida avançada e eletrólise).
- [ ] Separação, purificação e análise de substâncias.
- [ ] Propriedades físicas e químicas com unidades e escalas definidas.
- [ ] Síntese e materiais industriais avançados.

## Fase 4 — Engenharia atômica e nuclear
- [ ] Análise de composição dos materiais.
- [ ] Extração e processamento de elementos.
- [ ] Isótopos e separação isotópica.
- [ ] Fissão e sistemas de segurança.
- [ ] Fusão e tecnologias de plasma.
- [ ] Balanceamento de resíduos, calor e riscos.

## Fase 5 — Computação avançada
- [ ] Redes de controle e automação.
- [ ] Computadores industriais e IA hierárquica.
- [ ] Núcleo de computação quântica.
- [ ] Estabilização, entrelaçamento e aplicações ficcionais.

## Fase 6 — Tecnologias dimensionais
- [ ] Caracterizar recursos e fenômenos por dimensão.
- [ ] Sistemas de coleta atmosférica e energética específicos por dimensão.
- [ ] Reator de energia de End Crystal com cristal preservado.
- [ ] Investigar fenômenos do Nether, End e futuras dimensões.
- [ ] Tecnologias de alto nível e riscos dimensionais.

## Critérios para avançar de fase

Uma fase não deve avançar apenas porque os blocos existem. Os sistemas centrais precisam:
- funcionar em single-player e servidor dedicado;
- validar operações no servidor;
- persistir dados corretamente;
- ter receitas, custos e feedback compreensíveis;
- ser testáveis sem depender de comandos administrativos.


- [ ] Validar Interruptor Elétrico Industrial (`industrial_circuit_switch`) em servidor e cliente: manual, redstone, rotação, circuito aberto/fechado, rota alternativa, limites de transferência e persistência. Validar orçamento agregado de baterias (48/256 FE/t).


- [ ] Validar Caixa de Transmissão Compacta em todas as orientações, transmissão 1:1, perdas, divisão de carga e persistência. Validar também esteira logística: transporte, reversão, bloqueio, conservação e persistência.

- [ ] Validar gerenciamento térmico (Era II): dissipadores, ventilador pago, inventário persistente, refrigeração em biomas frios/quentes, gráficos, renderização e QA cliente/servidor.
- [ ] Evolução térmica: trocadores de calor, circuito líquido e criogenia; calor de fontes finitas e rede térmica compartilhada.
- [ ] Validar conversão da Era II: gerador eletromagnético, motor elétrico, caldeira, motor a vapor e gerador termoelétrico; verificar autorização, conservação, portas, persistência, modelos e uso em survival. Steam inicialmente interno/adjacente; tubulação de vapor registrada é evolução futura.

- [x] Correção local dos registros ausentes das máquinas industriais, esteira, caixa de engrenagens e chave de circuito; build novamente funcional.
