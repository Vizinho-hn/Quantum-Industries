# Roadmap — Quantum Industries

Este roadmap registra a direção do projeto. Itens são planejados, não promessas de entrega. As fases podem ser ajustadas após protótipos e testes.

## Fase 0 — Fundação técnica

- [x] Fixar Minecraft 26.1.2, NeoForge 26.1.2.125, Java 25 e Gradle 9.2.1.
- [x] Configurar build, cliente, servidor dedicado e estrutura de recursos.
- [x] Configurar CI para testes de contrato, migração de pesquisas e compilação.

## Fase 1 — Núcleo de pesquisa

- [x] Implementar a Bancada de Ensaio Empírico com dez materiais e três métodos.
- [x] Implementar Analisadores Científicos Tier 1 e Tier 2, árvore, caderno, receitas e relatórios.
- [x] Persistir conhecimentos por jogador e validar pesquisas, relatórios e crafts no servidor.
- [x] Integrar uma observação empírica à pesquisa de Maleabilidade.
- [x] Integrar as receitas desbloqueadas ao livro de receitas do Minecraft.
- [x] Adicionar comandos administrativos de desbloqueio e limpeza de pesquisas.
- [ ] Expandir as aplicações tecnológicas das observações empíricas.

## Fase 2 — Primeira indústria

- [x] Implementar as 13 pesquisas da Era I, com conformação manual, chapas e componentes mecânicos e elétricos.
- [x] Liberar o Núcleo Mecânico Primitivo pela pesquisa de Estrutura Eletromecânica.
- [x] Liberar o Dínamo Manual pelo Projeto do Dínamo e manter Indução Eletromagnética na Era II.
- [x] Implementar manivela separada, geração manual, armazenamento de FE e leitura com óculos.
- [ ] Validar a progressão completa com coleta em survival e dois jogadores reais.
- [ ] Desenvolver a progressão da Era II, incluindo rede de energia e primeiras máquinas consumidoras.

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
