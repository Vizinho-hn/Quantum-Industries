# Research Bench Tier 1 — bancada artesanal

Modelo externo próprio para `scientific_analyzer_tier_1`, com quatro pernas de
madeira, prateleira, tampo reforçado, gaveta visual com alça de cobre, caderno
aberto, balança mecânica, lupa e bandeja de amostras. Não usa componentes
elétricos, luz própria nem efeitos neon. A gaveta não adiciona inventário.

- Geometria: 56 cuboides, dentro de 16 × 15,8 × 16 unidades de modelo.
- Textura original: atlas de 64 × 64 pixels, dividido em materiais de 16 × 16.
- Orientação: acompanha as quatro direções horizontais do bloco existente.
- Item no inventário: usa o mesmo modelo 3D e a mesma textura do bloco.
- Editável no Blockbench: `research_table_tier_1.bbmodel`, com textura embutida,
  UVs por face e grupos de estrutura, ferragens, gaveta, caderno e instrumentos.
- Exportação para o jogo: `assets/quantumindustries/models/block/research_table_tier_1.json`
  e `textures/block/research_table_tier_1.png` em `src/main/resources`.

`research_table_tier_1-views.png` apresenta as vistas isométrica, frontal,
superior e lateral. `research_table_tier_1-comparison.png` compara os recursos
3D da Tier 1 com os recursos atuais da Tier 2. São prévias ortográficas dos
modelos e texturas exportados, sem simular a iluminação do Minecraft.

Esta alteração mantém os registros dos blocos, pesquisas, receitas, HUD de
caderno, armazenamento de relatórios e saves existentes. A Tier 2 continua
usando `research_table.json` e `research_table.png`.
