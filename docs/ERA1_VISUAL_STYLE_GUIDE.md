# Quantum Industries — Guia de Padronização Visual e Auditoria da Era I

**Escopo:** Era I (*Oficina Empírica, Metalurgia Artesanal & Mecânica Primitiva*)  
**Plataforma:** Minecraft `26.1.2` • NeoForge `26.1.2.125` • GeckoLib `5.5.2`  
**Status:** Fases A–E concluídas (`mechanical_shaft_block`, `primitive_water_wheel`, `mechanical_sawmill`, `forming_bench`, `manual_grain_mill`, `bellows_alloy_kiln`, `jaw_crusher`, além dos itens 3D `bronze_gear`, `tin_sheet`, `zinc_sheet` e `mechanical_shaft` em `128×128`).

---

## 1. Conceito Central da Era I

A Era I representa o alvorecer da industrialização no Quantum Industries: uma oficina de inventores e artesãos construída antes da eletrificação industrial avançada e da usinagem em aço.

Todo bloco, máquina, instrumento ou componente pertencente à Era I deve transmitir:
- **Construção artesanal e mecanicamente plausível:** estruturas em carvalho escuro envelhecido, reforços em ferro forjado escurecido, buchas e mancais em bronze envelhecido, chapas de cobre martelado, tiras de couro e vidro soprado artesanal.
- **Legibilidade imediata em primeira e terceira pessoa:** silhuetas fortes no grid voxel (`16×16×16`), sem microdetalhes que desaparecem na resolução de pixel art (`16×` / `32×`).
- **Modularidade real na transmissão mecânica:** um bloco de eixo individual (`mechanical_shaft_block`) é uma peça modular de transmissão que se acopla perfeitamente em linha com rodas-d'água e serrarias, não uma máquina pesada independente.

---

## 2. Direção Artística Oficial & Paletas (5 Camadas)

Cada textura em pixel art (`PNG RGBA`, filtragem *nearest-neighbor*) deve empregar **5 camadas de valor** (*Deep / Shadow / Mid / Light / Highlight*), evitando superfícies chapadas, gradientes suaves e ruído aleatório.

### 2.1 Materiais Predominantes e Códigos Hex

| Material | Uso Principal na Era I | Deep | Shadow | Mid | Light | Highlight |
|---|---|---|---|---|---|---|
| **Carvalho Escuro Envelhecido** (`Dark Aged Oak`) | Aduelas de eixos, estruturas de suporte, bancadas, cabos | `#1a0f08` | `#2b1a0f` | `#3d2617` | `#523522` | `#69462f` |
| **Madeira Temperada Clara** (`Seasoned Timber`) | Tampos de serraria, aros externos da roda-d'água, carros deslizantes | `#3a2414` | `#52341e` | `#6b4528` | `#845735` | `#9c6a43` |
| **Ferro Forjado Escurecido** (`Blackened Wrought Iron`) | Cintas/anéis de aperto, mancais, rebites, sapatas e parafusos | `#111214` | `#1c1e22` | `#2b2e33` | `#3e4249` | `#545963` |
| **Ferro Batido Pesado** (`Charcoal Iron Plate`) | Bases, cantoneiras e suportes estruturais de carga | `#0f1012` | `#181a1e` | `#24272c` | `#33373e` | `#454a54` |
| **Ferro Temperado Neutro** (`Tempered Iron`) | Lâmina da serraria, mandíbulas, pontas de corte (sem tom azulado) | `#36383b` | `#4d5054` | `#66696e` | `#85898f` | `#a8acb3` |
| **Bronze Fundido Envelhecido** (`Aged Cast Bronze`) | Buchas de acoplamento, colares de mancal, pesos e válvulas | `#2e2212` | `#4a381d` | `#6b532d` | `#8a6e3e` | `#a68952` |
| **Cobre Artesanal Martelado** (`Hand-Hammered Copper`) | Fios, bobinas, pás/calhas, pontas de manivela | `#542110` | `#7c3419` | `#a84a25` | `#cf6435` | `#ea8554` |
| **Couro Marrom Curtido** (`Tanned Leather`) | Isolamentos, correias, juntas e foles | `#2e190e` | `#4a2917` | `#693c22` | `#87502f` | `#a36842` |
| **Vidro Artesanal** (`Crafted Glass`) | Frascos de ensaio, lentes e visores de instrumentos | `#3b5763` | `#5b808f` | `#85aebd` | `#b2d4e0` | `#dcf0f7` |

### 2.2 O Que Evitar Estritamente na Era I
- Metais azulados ou cromados de aparência futurista (`#4e5866` → `#edf4fc` em peças externas de transmissão).
- Anéis dourados saturados (`#ffe078`) que remetem a ouro ou joalheria em vez de bronze industrial envelhecido.
- Luzes neon, telas emissivas ou partículas tecnológicas.
- Peças flutuantes sem ligação mecânica ao chassi ou mancal.
- Microelementos menores que `0.5` unidade de bloco que viram serrilhado no render do Minecraft.
- Uso de GeckoLib onde modelos JSON vanilla atendem perfeitamente (blocos estáticos sem partes articuladas).

---

## 3. Padrões de Geometria, Proporção e Acoplamento Mecânico

### 3.1 Eixo de Transmissão e Acoplamento Modular
Para que `primitive_water_wheel`, `mechanical_shaft_block` e `mechanical_sawmill` conectem visualmente em qualquer sequência sem frestas nem descontinuidades:
- **Centro axial nominal:** `x = 8.0`, `y = 8.0` (eixo longitudinal ao longo de `Z` no modelo base `NORTH` para roda-d'água e eixo de transmissão; ao longo de `X` no modelo base `NORTH` da serraria, alinhado na mesma cota `y = 8.0`).
- **Espiga de acoplamento (`Axle Tenon`):** secção quadrada em madeira reforçada `4.0 × 4.0` (`[6.0, 6.0]` até `[10.0, 10.0]`), indo até as faces extremas `0.0` e `16.0`, com face de topo exibindo o **soquete quadrado oco** em madeira + ferro forjado.
- **Colar de bronze (`Bronze Bushing`):** anel octogonal/escalonado de transição `5.5 × 5.5` (`[5.25, 5.25]` até `[10.75, 10.75]`) ligando a espiga quadrada ao tambor central.
- **Tambor central do eixo (`Wooden Drum + Wrought Iron Hoops`):** diâmetro visual ~`6.8` unidades (`[4.5, 4.5]` a `[11.5, 11.5]` nas cintas de ferro forjado), ocupando o centro `z = 4.0..12.0`.
- **Pedestal/haste dinâmico no chão (`base`):** pedestal apoiado exatamente em `y = 0.0` (`[4.4, 0.0, 5.2]` a `[11.6, 5.2, 10.8]`) com sapata parafusada, coluna de carvalho escuro e sela de mancal sob o tambor. Em renderização e *hitbox*, o *bone* `"base"` só aparece quando há bloco sólido completo imediatamente abaixo (`MechanicalShaftBlock.isGrounded`); quando o eixo estiver suspenso no ar (ou vertical em `axis=y`), a haste é ocultada para manter a transmissão limpa.

### 3.2 Era I vs. Era II+ (Contraste Deliberado)

| Dimensão | Era I — Oficina Artesanal | Era II+ — Transição Térmica e Industrial (ex. `thermal_annealing` / `bellows_brazier`) |
|---|---|---|
| **Materiais** | Carvalho escuro, madeira temperada, ferro forjado fosco, bronze envelhecido, cobre martelado | Alvenaria refratária, ferro fundido espesso, aço usinado/escovado, tubulações metálicas |
| **Acabamento** | Junções com espiga, rebites escuros e cintas de aperto | Estruturas soldadas/rebitadas contínuas, chapas blindadas e válvulas térmicas |
| **Paleta** | Terrosa e quente (marrons, carvão, bronze opaco, cobre) | Cinza mineral, ferro escuro de alta densidade, aço polido e brasa térmica controlada |
| **Movimento** | Rotação direta baixa/média por água ou manivela | Vapor, calor forçado e transmissão industrial de alta carga |

---

## 4. Auditoria Completa da Era I (Fase A)

Baseada em `ResearchCatalog.java` (todos os **17 estudos com `era == 1`**), `FoundationRecipe.java`, `ModBlocks.java`, `ModItems.java` e nos arquivos em `src/main/resources/assets/quantumindustries/`.

### 4.1 Estudos da Era I (`ResearchCatalog.java`, `era == 1`)

| # | ID do Estudo | Pré-requisito(s) | Desbloqueios Relevantes na Era I |
|---|---|---|---|
| 1 | `material_observation` | `-` | `manual_grain_mill`, `empirical_assay_bench` |
| 2 | `conductivity` | `material_observation` | Base de pesquisa elétrica (pré-requisito de `copper_wire`) |
| 3 | `copper_wire` | `conductivity` | `copper_wire` |
| 4 | `electrical_insulation` | `copper_wire` | `insulated_copper_wire` |
| 5 | `copper_winding` | `copper_wire` | `copper_coil` |
| 6 | `basic_magnetism` | `material_observation` | Pré-requisito de eletromagnetismo |
| 7 | `material_malleability` | `material_observation` | Pré-requisito de conformação manual |
| 8 | `manual_forming` | `material_malleability` | `manual_hammer`, `forming_bench` |
| 9 | `metal_sheets` | `manual_forming` | Desbloqueio de chapas metálicas e requisito para `mechanical_sawmill` |
| 10 | `basic_mechanics` | `manual_forming` | `iron_gear` |
| 11 | `mechanical_transmission` | `basic_mechanics` | `mechanical_shaft`, `hand_crank`, `mechanical_shaft_block`, `mechanical_sawmill` |
| 12 | `hydraulic_power` | `mechanical_transmission` | `primitive_water_wheel` |
| 13 | `mineral_beneficiation` | `mechanical_transmission`, `metal_sheets` | `jaw_crusher` |
| 14 | `alloy_metallurgy` | `metal_sheets` | `bellows_alloy_kiln`, `bronze_gear` |
| 15 | `electromechanical_structure` | `metal_sheets`, `mechanical_transmission`, `basic_magnetism`, `copper_winding` | `electromechanical_frame`, `primitive_mechanical_core` |
| 16 | `manual_dynamo_design` | `electrical_insulation`, `electromechanical_structure` | `manual_dynamo` |
| 17 | `scientific_analyzer_tier_2` | `manual_dynamo_design`, `mineral_beneficiation` | `scientific_analyzer` (transição para Era II) |

> **Achado de Auditoria (Cross-Era):** em `FoundationRecipe.java`, a receita de `bellows_alloy_kiln` (`era == 1`) utiliza `bellows_brazier` como ingrediente, embora o estudo `thermal_annealing` (que referencia `bellows_brazier`) esteja catalogado com `era == 2` em `ResearchCatalog.java`. A padronização visual deve preservar receitas/IDs e tratar visualmente `bellows_alloy_kiln` como forno de liga artesanal da Era I, enquanto respeita essa fronteira de progressão.

### 4.2 Classificação dos Blocos e Máquinas da Era I

| ID do Bloco / Máquina | Tipo de Modelo / Textura Atual | Diagnóstico | Classificação Oficial |
|---|---|---|---|
| `mechanical_shaft_block` | GeckoLib + `mechanical_workshop.png` (`128×128`), 16 cubos (`5` base dinâmica, `9` rotor, `2` crank) | Pedestal de chão exibido apenas sobre bloco sólido (`MechanicalShaftBlock.isGrounded`); tambor em aduelas de carvalho + cintas de ferro forjado + bronze envelhecido | **Concluído (Piloto Fase C/D)** |
| `primitive_water_wheel` | GeckoLib + `mechanical_workshop.png` (`128×128`), 34 cubos (`8` base, `26` rotor) | Alinhado em `y = 8.0` com espigas quadradas `4×4` em carvalho escuro, buchas de bronze e tambor central com cinta de ferro | **Concluído (Fase E — Lote 1)** |
| `mechanical_sawmill` | GeckoLib + `mechanical_workshop.png` (`128×128`), 39 cubos (`16` base/carriage, `21` rotor, `2` crank) | Alinhado em `y = 8.0` com espigas quadradas `4×4`, buchas de bronze, árvore em carvalho e lâmina em ferro temperado neutro | **Concluído (Fase E — Lote 1)** |
| `forming_bench` | JSON Blockbench + `era1_artisan_machines.png` (`128×128`), 16 cubos | Bancada robusta em carvalho e tampo temperado, bigorna forjada completa, bloco de estampa em bronze e trilho frontal de ferramentas | **Concluído (Fase E — Lote 2)** |
| `bellows_alloy_kiln` | JSON Blockbench + `era1_artisan_machines.png` (`128×128`), 18 cubos | Cúpula de tijolo refratário artesanal com duas cintas rebitadas de ferro forjado, fornalha com grelha de carvão em brasa, bico de bronze e fole em couro/carvalho acoplado por algaraviz de cobre | **Concluído (Fase E — Lote 2)** |
| `jaw_crusher` | JSON Blockbench + `era1_artisan_machines.png` (`128×128`), 18 cubos | Chassi em ferro forjado sobre patins de carvalho, mandíbulas corrugadas em ferro temperado, moega em madeira/ferro, bica de cobre, volante raiado a oeste e espiga motriz em `y = 8.0` a leste | **Concluído (Fase E — Lote 2)** |
| `manual_grain_mill` | JSON Blockbench + `manual_grain_mill_atlas.png` (`128×128`), 16 cubos | Corrigidas todas as UVs invertidas/fora da textura; mó rotativa entalhada com sulcos radiais, cinta de ferro, colar/manivela em bronze e bica frontal em cobre martelado | **Concluído (Fase E — Lote 2)** |
| `empirical_assay_bench` | JSON vanilla (`geckolib=False`), 71 cubos (`block/empirical_workshop`) | Geometria rica de bancada empírica; revisar somente contraste/harmonia cromática com a paleta Era I | **Melhorar apenas a textura (Fase E)** |
| `manual_dynamo` | GeckoLib (`dynamo_materials.png`, `16×16`), 86 cubos, `item/manual_dynamo_gui` | Silhueta mecânica forte e animação dedicada de dínamo manual; revisar apenas harmonia metálica se necessário | **Manter como está / polir textura** |
| `scientific_analyzer_tier_1` | JSON vanilla estilo Blockbench (`research_table_tier_1.json`, `block/research_table_tier_1`) | Coerente como bancada de pesquisa inicial da Era I | **Manter como está** |
| `scientific_analyzer` | JSON vanilla (`block/research_table`, `geckolib=False`) | Marco de transição Era I → Era II desbloqueado por `scientific_analyzer_tier_2` | **Manter como está** |

### 4.3 Classificação dos Instrumentos Científicos, Documentação e Componentes Industriais

| Categoria | Itens | Formato Atual | Classificação / Ação Planejada |
|---|---|---|---|
| **Documentação & Pesquisa** | `quantum_guidebook`, `empirical_field_notes`, `research_data` | `2D item/generated` (`32×32` RGBA) | **Concluído (`32×32` — grimório de couro com engrenagem de bronze, caderno de campo e cianótipo com pergaminho lacrado)** |
| **Instrumentos 2D** | `manual_bellows`, `assay_flask` | `2D item/generated` (`32×32` RGBA) | **Concluído (`32×32` — fole em V de carvalho/couro/cobre e frasco artesanal azulado com reagente âmbar)** |
| **Ícone 2D GUI da Bancada** | `empirical_assay_bench` (contexto `gui`) | `2D item/generated` (`32×32` RGBA) | **Concluído (`32×32` RGBA — bancada com balança de bronze, bico de álcool e frasco)** |
| **Instrumentos 3D** | `magnifying_glass`, `test_probes`, `brass_weights`, `flame_test_kit` | `3D block/block` (`era1_materials.png` `128×128`) | **Textura base `era1_materials.png` atualizada para `128×128` RGBA coerente com toda a Era I** |
| **Ferramentas e Eixos 3D** | `manual_hammer`, `mechanical_shaft`, `hand_crank` | `3D` (`era1_materials.png` `128×128`) | **`mechanical_shaft` remodelado para 3D em aduelas + cintas de ferro + bronze + soquete oco (`era1_materials.png` em `128×128`)** |
| **Componentes Eletromecânicos 3D & 2D** | `copper_wire`, `insulated_copper_wire`, `copper_coil`, `electromechanical_frame`, `primitive_mechanical_core` | `3D` + texturas `2D 32×32` | **`copper_wire.png` e `insulated_copper_wire.png` atualizados para `32×32` RGBA; `era1_materials.png` em `128×128`** |
| **Inconsistência Engrenagens** | `iron_gear` (`3D`) vs. `bronze_gear` (`3D` clonado em bronze) | Padronizado `3D` (`era1_materials.png` `128×128`) | **Concluído (`bronze_gear` convertido de 2D para 3D — Fase E Lote 3)** |
| **Inconsistência Chapas** | `iron_sheet`, `copper_sheet` (`3D`) vs. `tin_sheet`, `zinc_sheet` (`3D`) | Padronizado `3D` (`era1_materials.png` `128×128`) | **Concluído (`tin_sheet` e `zinc_sheet` convertidos de 2D para 3D — Fase E Lote 3)** |
| **Derivados de Moagem** | `flour`, `bread_dough`, `rustic_bread` | `2D item/generated` (`32×32` RGBA) | **Concluído (`32×32` — saca de estopa aberta, massa sovada e pão rústico com cortes dourados)** |

---

## 5. Padrão Técnico de Modelagem e Contratos do Projeto

1. **Preservação de Contrato Java/GeckoLib:**
   - Manter IDs de blocos/itens, receitas, pesquisas e nomes de *bones* (`"base"`, `"rotor"`, `"crank"`, além de `"carriage"` na serraria).
   - No `MechanicalRenderer.java`, `"base"` no `mechanical_shaft_block` só renderiza quando `MechanicalShaftBlock.isGrounded` é verdadeiro, `"crank"` tem visibilidade condicional por estado, e `"rotor"` rotaciona em torno do eixo `Z` com pivô `[0, 8, 0]` (`mechanical_shaft_block` e `primitive_water_wheel`).
2. **Atlas Compartilhado da Oficina Mecânica (`textures/block/mechanical_workshop.png`):**
   - Dimensão obrigatória: `128 × 128` px (`4 × 2` quadrantes de `32 × 32` px na metade superior `y = 0..64` para os materiais gerais, e regiões HD dedicadas `y = 64..128` para aduelas, soquetes quadrados `32 × 32`, cintas de ferro forjado com rebites `3×3` e colares de bronze).
   - Toda alteração no PNG exige sincronização da textura embutida em base64 e das dimensões UV nos `.bbmodel`, `.geo.json` e JSONs de bloco/item de `mechanical_shaft_block`, `primitive_water_wheel` e `mechanical_sawmill`.
3. **Contrato Automático (`tests/test_mechanical_resources.py`):**
   - `models/block/<name>.json` deve conter exatamente os cubos `group == "base"` de `mechanical/parts.json`.
   - `models/item/<name>.json` deve conter exatamente os cubos `group != "crank"` de `mechanical/parts.json`.
   - Todos os cubos em `mechanical/parts.json` devem satisfazer `0 <= a < b <= 16`.
   - `design/mechanical/<name>.bbmodel` deve ter exatamente `len(elements) == len(parts[name])` e PNG embutido idêntico byte a byte a `mechanical_workshop.png`.
4. **Validação Obrigatória:**
   - Blockbench MCP: `check_model` = `0` problemas + `screenshot_views`.
   - Testes Python (`unittest`), `./gradlew check` e cliente ao vivo (`tests/qa-preview.init.gradle runClient`) com inspeção de screenshots.
