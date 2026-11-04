# Mesa de Pesquisa — Quantum Industries

A Mesa de Pesquisa é o terminal de desenvolvimento de tecnologias. Está
implementada e ocupa um bloco, com modelo 3D próprio. É um componente separado
da [Bancada de Ensaio Empírico](../bancada-de-ensaio/README.md).

## Funcionamento atual

A interface apresenta as pesquisas de condutividade e cabo de cobre. O servidor
confere os recursos e pré-requisitos de cada jogador antes de registrar uma
pesquisa. A receita de cabo de cobre respeita esses desbloqueios.

A mesa também guarda papel, tinta e livros para escrever relatórios científicos
sobre pesquisas conhecidas. O conhecimento e as autorizações pertencem a cada
jogador; compartilhar a mesa não compartilha automaticamente a progressão.

As novas observações dos dez materiais da bancada ainda não são pré-requisitos
automáticos dessas pesquisas. Essa integração permanece em definição.

## Modelo e arquivos

- Fonte editável: [`design/quantum_research_table.bbmodel`](../../../design/quantum_research_table.bbmodel).
- Modelo do jogo: [`research_table.json`](../../../src/main/resources/assets/quantumindustries/models/block/research_table.json).
- Textura: [`research_table.png`](../../../src/main/resources/assets/quantumindustries/textures/block/research_table.png).
- Interface: [`ResearchTableScreen.java`](../../../src/main/java/br/com/quantumindustries/client/ResearchTableScreen.java).

O modelo combina base industrial, madeira, detalhes em azul-ciano, símbolo de
átomo, livros e monitor. A galeria da bancada está em seu [guia próprio](../bancada-de-ensaio/README.md#galeria).
