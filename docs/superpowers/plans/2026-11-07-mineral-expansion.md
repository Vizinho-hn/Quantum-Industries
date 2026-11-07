# Expansão Mineralógica & Geológica (Quantum Industries) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar o catálogo mineralógico expandido (~18 minerais e metais reais inspirados na mineralogia e no Emendatus Enigmatica) no Quantum Industries com registro orientado a dados, economia estrita de tokens e suporte a stone/deepslate, itens brutos, lingotes, pepitas e pós.

**Architecture:** Padrão orientado a dados (Data-Driven Architecture) via Enum/Catalog em Java. Uma classe `OreDefinition` e enums `MineralMaterial` e `ChemicalMineral` alimentam dinamicamente os registros `ModBlocks` e `ModItems`. Tags unificadas `#c:...` e tabelas de drop parametrizadas por metadados.

**Tech Stack:** Minecraft 26.1.2, NeoForge 26.1.2.125, Java 25, Gradle 9.2.1.

**Spec:** `docs/GDD_QUANTUM_INDUSTRIES.md` e `docs/ROADMAP_QUANTUM.md`.

## Global Constraints
- Nomes de classes e métodos em inglês e convenções Java idiomáticas.
- Sem repetição de código (DRY/YAGNI): uma única classe de bloco para minérios, parametrizada por `UniformInt` de XP e propriedades de resistência.
- Compatibilidade estrita com Common Tags (`#c:ores/*`, `#c:raw_materials/*`, `#c:ingots/*`, `#c:dusts/*`).
- Não quebrar ou modificar o `AssayCatalog` de 10 materiais da Bancada Empírica nesta fase.

## Catálogo de Minerais Planejado (18 Recursos)

### A. Metais Industriais & Nativos (Minério Stone + Deepslate -> Raw -> Ingot + Nugget)
1. **Cassiterita (Tin)**: $SnO_2$ — Estanho.
2. **Esfalerita (Zinc)**: $(Zn, Fe)S$ — Zinco.
3. **Galena (Lead)**: $PbS$ — Chumbo.
4. **Pentlandita (Nickel)**: $(Fe, Ni)_9S_8$ — Níquel.
5. **Acantita (Silver)**: $Ag_2S$ — Prata.
6. **Bauxita (Aluminum)**: $AlO_x(OH)_{3-2x}$ — Alumínio.
7. **Uraninita (Uranium)**: $UO_2$ — Urânio.
8. **Osmirídio (Osmium)**: $(Os, Ir)$ — Ósmio.
9. **Esperrilita (Platinum)**: $PtAs_2$ — Platina.
10. **Wolframita (Tungsten)**: $(Fe, Mn)WO_4$ — Tungstênio.
11. **Rutilo (Titanium)**: $TiO_2$ — Titânio.
12. **Cobaltita (Cobalt)**: $CoAsS$ — Cobalto.

### B. Minerais Não-Metálicos & Reagentes Químicos (Stone + Deepslate -> Dust / Gem / Shard)
13. **Enxofre Nativo (Sulfur)**: $S_8$ — Pó de enxofre (`sulfur_dust`).
14. **Fluorita (Fluorite)**: $CaF_2$ — Cristal de fluorita (`fluorite_gem`).
15. **Cinábrio (Cinnabar)**: $HgS$ — Pó de cinábrio (`cinnabar_dust`).
16. **Apatita (Apatite)**: Fosfato mineral — Mineral de apatita (`apatite_gem`).
17. **Halita (Salt/Halite)**: $NaCl$ — Sal gema (`salt_rock`).
18. **Grafite (Graphite)**: Carbono alotrópico — Pó de grafite (`graphite_dust`).

---

## Tasks

### Task 1: Catálogo Orientado a Dados (Enum & Definições)
**Files:**
- Create: `src/main/java/br/com/quantumindustries/material/OreType.java`
- Create: `src/main/java/br/com/quantumindustries/material/MineralCatalog.java`

- [x] Definir o enum `OreType` com propriedades: `id`, `mineralName`, `formula`, `category` (METAL vs CHEMICAL), `hardness`, `resistance`, `minXp`, `maxXp`.
- [x] Catalogar os 18 minerais em `MineralCatalog` com seus respectivos metadados estequiométricos.

### Task 2: Registro Unificado de Blocos e Itens (Zero Boilerplate)
**Files:**
- Create: `src/main/java/br/com/quantumindustries/block/QuantumOreBlock.java`
- Modify: `src/main/java/br/com/quantumindustries/core/ModBlocks.java`
- Modify: `src/main/java/br/com/quantumindustries/ModItems.java`
- Modify: `src/main/java/br/com/quantumindustries/core/ModCreativeTabs.java`

- [x] Criar classe `QuantumOreBlock` herdando de `Block` ou `DropExperienceBlock` (recebendo `UniformInt` de XP).
- [x] No `ModBlocks`, iterar por `MineralCatalog` e registrar dinamicamente as variantes Stone (`_ore`) e Deepslate (`deepslate_*_ore`).
- [x] No `ModItems`, registrar dinamicamente os `BlockItem` de cada minério e os itens derivados (`raw_*`, `*_ingot`, `*_nugget` ou `*_dust`, `*_gem`).
- [x] Adicionar os blocos e itens à `ModCreativeTabs`.

### Task 3: Tags do Ecossistema (Common Tags #c:...) & Worldgen
**Files:**
- Create: `src/main/resources/data/c/tags/block/ores/` (tags por mineral)
- Create: `src/main/resources/data/c/tags/item/ores/`, `raw_materials/`, `ingots/`, `nuggets/`, `dusts/`, `gems/`
- Create: `src/main/resources/data/quantumindustries/worldgen/configured_feature/`
- Create: `src/main/resources/data/quantumindustries/worldgen/placed_feature/`
- Create: `src/main/resources/data/quantumindustries/neoforge/biome_modifier/add_quantum_ores.json`

- [x] Gerar tags JSON para cada tipo mineral garantindo compatibilidade multiplataforma.
- [x] Configurar geração procedural de minérios no subsolo do Overworld via NeoForge Biome Modifier.

### Task 4: Receitas Básicas de Fundição e Compactação
**Files:**
- Create: `src/main/resources/data/quantumindustries/recipe/smelting/`
- Create: `src/main/resources/data/quantumindustries/recipe/blasting/`
- Create: `src/main/resources/data/quantumindustries/recipe/crafting/`

- [x] Criar receitas de `smelting` e `blasting` (Raw -> Ingot, Ore -> Ingot).
- [x] Criar receitas de compactação 9 nuggets <-> 1 ingot.

### Task 5: i18n & Nomenclatura Científica
**Files:**
- Modify: `src/main/resources/assets/quantumindustries/lang/en_us.json`
- Modify: `src/main/resources/assets/quantumindustries/lang/pt_br.json`

- [x] Adicionar nomes em Português e Inglês com a fórmula química e nome do mineral real.

### Task 6: Validação de Compilação & Contratos
**Files:**
- Execute: `./gradlew build -x test`

- [x] Validar compilação limpa em Java 25.
- [x] Testar carregamento no servidor sem conflitos de registro.
