# Ícone de GUI para itens multi-bloco (Analisador de Materiais)

## Contexto

`quantumindustries:empirical_assay_bench` (Analisador de Materiais) é um bloco de **duas células**
(`empirical_assay_bench_foot` + `empirical_assay_bench_head`). O item correspondente era declarado
com um modelo de bloco 3D direto em `items/empirical_assay_bench.json`, e o slot de inventário tem
16×16. O modelo, com 32 px de largura no eixo X, era reduzido por `display.gui.scale` para caber —
o que cortava e deformava a peça ("a foto do inventário está bugada").

## Decisão

Separar a representação de GUI da representação 3D:

- `items/empirical_assay_bench.json` usa `minecraft:select` com propriedade
  `minecraft:display_context`.
  - caso `gui` → `item/empirical_assay_bench_gui` (ícone 2D plano);
  - `fallback` → `item/empirical_assay_bench` (modelo 3D, usado na mão, no chão e em item frame).
- `models/item/empirical_assay_bench_gui.json` estende `minecraft:item/generated` com
  `layer0: quantumindustries:item/empirical_assay_bench`.
- `textures/item/empirical_assay_bench.png`: pixel art 32×32 com fundo transparente, lendo a
  bancada empírica (corpo de madeira, pernas de ferro, balança de latão e chama de ensaio).

O ícone plano é dimensionado pelo próprio `minecraft:item/generated`, sem depender de
`display.gui`, portanto não há mais escala/offset que possa cortar a peça.

## Alternativas rejeitadas

- **Ajustar apenas `display.gui`** (rotação/translação/escala): a peça tem proporção 2:1; nenhuma
  escala caberia os 32 px de largura no slot sem deixá-la ilegível. Mantém o item como miniatura
  de bloco em vez de leitura de ícone.
- **Render 3D pré-renderizado para 32×32**: como `docs/images/blocks/empirical_assay_bench.png`,
  a fonte disponível é um render com barra de rótulo e fundo opaco; convertê-la produziria ícone
  borrado (o mesmo defeito hoje visível em `textures/item/manual_dynamo.png`). Pixel art é o
  formato nativo dos demais ícones de item do mod.

## Consequências

- Outros itens multi-bloco devem seguir o mesmo padrão: `display_context`/`gui` → ícone plano,
  `fallback` → modelo 3D. O teste `test_no_item_renders_a_multi_block_model_in_the_gui` passa a
  barrar regressões dessa classe em todo `items/*.json`.
- O item precisa manter `models/item/<nome>_gui.json` e `textures/item/<nome>.png` sincronizados
  com o nome do registro.
