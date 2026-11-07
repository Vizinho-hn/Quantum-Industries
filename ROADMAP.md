# Roadmap — Quantum Industries

Este roadmap registra a direção do projeto. Itens são planejados, não promessas de entrega. As fases podem ser ajustadas após protótipos e testes.

As fases de entrega abaixo não são números de era. A divisão tecnológica é:
I — Engenharia Mecânica e Ciência Empírica; II — Revolução Elétrica;
III — Ondas Eletromagnéticas e Tecnologia Moderna; IV — Engenharia Quântica;
V — Inteligência Artificial e Automação Autônoma.

## Fase 0 — Fundação técnica

- [x] Fixar Minecraft 26.1.2, NeoForge 26.1.2.125, Java 25 e Gradle 9.2.1.
- [x] Configurar build, cliente, servidor dedicado e estrutura de recursos.
- [x] Configurar CI para testes de contrato, migração de pesquisas via Gradle (com as dependências do jogo) e compilação.

## Fase 1 — Núcleo de pesquisa

- [x] Implementar a Bancada de Ensaio Empírico com dez materiais, quatro métodos e 40 descobertas individualizadas.
- [x] Implementar Analisadores Científicos Tier 1 e Tier 2, árvore, caderno, receitas e relatórios.
- [x] Dar à Mesa de Pesquisa Tier 1 um modelo artesanal próprio, com caderno e instrumentos manuais, textura pixel art e arquivo Blockbench.
- [x] Persistir conhecimentos por jogador e validar pesquisas, relatórios e crafts no servidor.
- [x] Tornar Observação dos Materiais a entrada da Era I e liberar o Analisador de Materiais.
- [x] Integrar evidências científicas reutilizáveis (inspeção, prensa, aquecimento e reação) às pesquisas, relatórios e HUD.
- [x] Integrar as receitas desbloqueadas ao livro de receitas do Minecraft.
- [x] Adicionar comandos administrativos de desbloqueio e limpeza de pesquisas.
- [ ] Expandir as aplicações dos demais experimentos em máquinas e processos da Era II.\n- [x] Permitir compartilhar quatro observações empíricas com Fichas de Observação escritas no Analisador.

## Fase 2 — Primeira indústria

- [x] Oficina mecânica da Era I: serraria manual, eixos funcionais e roda-d'água (nesta branch; integração por PR).
- [x] Receitas JSON de processamento, sincronização nativa e prévias automáticas no caderno.
- [x] Testes automatizados da cadeia em survival com suprimentos preparados, reinício real, carga compartilhada, inventários e chunks.
- [ ] Moinho de Pedra, Prensa Mecânica de Fuso, Guincho de Mineração e Bomba Mecânica.
- [ ] Transmissões mais avançadas e evolução elétrica das máquinas na Era II.

- [x] Implementar as 13 pesquisas originais da Era I e acrescentar Aproveitamento Hidráulico, preservando os IDs e a migração.
- [x] Liberar o Núcleo Mecânico Primitivo pela pesquisa de Estrutura Eletromecânica.
- [x] Liberar o Dínamo Manual pelo Projeto do Dínamo e manter Indução Eletromagnética na Era II.
- [x] Tratar **eras como agrupamentos**, não bloqueios globais: abas com pesquisas podem ser abertas, com pré-requisitos de cada estudo preservados.
- [x] Desacoplar os ramos de Hidrostática, Pressão Pneumática, Recozimento Térmico e Trefilação de pesquisas elétricas que não são necessárias ao processo.
- [ ] Testar em cliente/servidor o acesso paralelo aos ramos e a persistência dos saves, sem liberar por engano receitas protegidas.
- [ ] Evoluir com **invenções opcionais de rendimento** (Moedor Manual → Farinha → Pão melhorado, moedor elétrico depois), sem torná-las pré-requisito para a próxima era.
- [ ] Adicionar interface contextual para diferenciar **descoberta fundamental**, **aplicação opcional** e **melhoria de rendimento**, sem tornar a árvore visualmente pesada.
- [x] Implementar manivela separada, geração manual, armazenamento de FE e leitura com óculos.
- [ ] Validar a progressão completa com coleta em survival e dois jogadores reais.
- [x] Implementar os primeiros blocos da Era II: Acumulador Galvânico (2.400 FE), Prensa Eletromecânica (60 FE/chapa) e transferência adjacente com leitura nos Óculos de Inspeção.
- [x] Introduzir Barramento de Cobre Nu e Conduíte Isolado para distribuir FE com limites de vazão e perda por caminho.\n- [x] Introduzir Trefiladora com acionamento manual ou FE e fios de cobre/ouro (4 por chapa).\n- [x] Introduzir forja térmica com fole e pré-aquecimento para conformação/prensa.\n- [x] Introduzir transdutor piezoelétrico e relé de carga por limiares de acumulador.\n- [x] Criar tubos pneumáticos com animação de pacotes, ejetor manual/FE e classificador gravitacional.
- [x] Criar dutos hidráulicos nativos NeoForge, bomba peristáltica manual/FE e cuba de decantação 8.000 mB.
- [ ] Criar fluido registrado de eletrólito refinado e integrar como insumo opcional ao Acumulador sem invalidar receitas anteriores.
- [ ] Validar redes energéticas e novas máquinas de processamento em mundo survival e servidor dedicado.

## Fase 3 — Química e materiais
- [ ] Reações e processos químicos.
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
