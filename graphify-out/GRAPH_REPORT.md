# Graph Report - src  (2026-11-07)

## Corpus Check
- Large corpus: 885 files · ~102,592 words. Semantic extraction will be expensive (many Claude tokens). Consider running on a subfolder.

## Summary
- 1614 nodes · 5202 edges · 74 communities (25 shown, 49 thin omitted)
- Extraction: 97% EXTRACTED · 3% INFERRED · 0% AMBIGUOUS · INFERRED: 146 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Research Screen Details
- Copper Cable Energy
- Empirical Assay Blocks
- Assay Rendering Utilities
- Copper Cable Block
- Material Item Components
- Block and Entity Registries
- Block Entity Lifecycle
- Block Interaction Methods
- Mechanical Recipes and Networks
- Bellows Machine Entities
- Mechanical Recipe Cache
- Menu and Screen Registration
- Empirical Material Catalog
- Assay Bench Processing
- Creative Tab Registration
- Research Menu Workflow
- Research and Assay Menus
- Fluid Pipe Storage
- Pneumatic Tube Rendering
- Rotational Mechanical Networks
- Material Processing Rules
- Quantum Ore Generation
- Water Wheel Mechanics
- Empirical Assay Materials
- Foundation Recipe System
- Mechanical Sawmill Processing
- Wire Drawing Energy
- Assay Bench Renderer
- Electromechanical Press Processing
- Assay Screen Controls
- Mechanical Recipe Types
- Mechanical Renderer
- Gravitational Sorter State
- Dynamo Inspection HUD
- Research Table Persistence
- Research IDs and Progress
- Bellows Ejector Energy
- Galvanic Accumulator Energy
- Load Relay Logic
- Peristaltic Pump Energy
- Settling Vat Processing
- Research Catalog Validation
- Utilities and Legacy Migration
- Bellows Brazier Storage
- Assay Method Types
- Manual Dynamo Block
- Assay Data Delivery
- Press Renderer
- Manual Dynamo Renderer
- Assay Catalog and Items
- Bellows Ejector Block
- Electromechanical Press Block
- Gravitational Sorter Block
- Peristaltic Pump Block
- Research Table Block
- Wire Drawing Bench Block
- Research Commands
- Manual Grain Mill Storage
- Research Evidence Models
- Player Research Persistence
- Galvanic Accumulator Block
- Manual Grain Mill Block
- Third-Party Asset Notices
- Bellows Brazier Block
- Settling Vat Block
- Fluid Capability Registration
- Forming Materials
- Third-Party License Terms

## God Nodes (most connected - your core abstractions)
1. `ModBlocks` - 94 edges
2. `Material` - 55 edges
3. `ResearchTableScreen` - 53 edges
4. `EmpiricalAssayBenchMenu` - 38 edges
5. `ResearchTableMenu` - 34 edges
6. `ModItems` - 33 edges
7. `EmpiricalAssayBenchBlockEntity` - 33 edges
8. `ElectromechanicalPressBlockEntity` - 32 edges
9. `MechanicalProcessingRecipe` - 31 edges
10. `WorldResearchData` - 31 edges

## Surprising Connections (you probably didn't know these)
- `MechanicalRecipeCache` --references--> `MechanicalProcessingRecipe`  [EXTRACTED]
  client/java/br/com/quantumindustries/client/MechanicalRecipeCache.java → main/java/br/com/quantumindustries/recipe/MechanicalProcessingRecipe.java
- `AssayPictureState` --references--> `Material`  [EXTRACTED]
  client/java/br/com/quantumindustries/client/assay/AssayPictureState.java → main/java/br/com/quantumindustries/assay/AssayCatalog.java
- `AssayPictureState` --references--> `Method`  [EXTRACTED]
  client/java/br/com/quantumindustries/client/assay/AssayPictureState.java → main/java/br/com/quantumindustries/assay/AssayCatalog.java
- `EmpiricalAssayBenchScreen` --references--> `EmpiricalAssayBenchMenu`  [EXTRACTED]
  client/java/br/com/quantumindustries/client/gui/EmpiricalAssayBenchScreen.java → main/java/br/com/quantumindustries/menu/EmpiricalAssayBenchMenu.java
- `ElectromechanicalPressRenderer` --references--> `ElectromechanicalPressBlockEntity`  [EXTRACTED]
  client/java/br/com/quantumindustries/client/render/ElectromechanicalPressRenderer.java → main/java/br/com/quantumindustries/block/entity/ElectromechanicalPressBlockEntity.java

## Import Cycles
- None detected.

## Communities (74 total, 49 thin omitted)

### Community 0 - "Research Screen Details"
Cohesion: 0.07
Nodes (15): Entry, ResearchDetailsPanel, ResearchTableScreen, ResearchTheme, Grid, Node, ResearchTreeView, State (+7 more)

### Community 1 - "Copper Cable Energy"
Cohesion: 0.05
Nodes (6): CopperCableBlockEntity, EnergyInspectable, ManualDynamoBlockEntity, PiezoSwitchBlockEntity, CableNetwork, Route

### Community 2 - "Empirical Assay Blocks"
Cohesion: 0.06
Nodes (3): EmpiricalAssayBenchBlock, MechanicalSawmillBlock, MechanicalShaftBlock

### Community 3 - "Assay Rendering Utilities"
Cohesion: 0.07
Nodes (7): AssayApparatus, Box, AssayPictureRenderer, AssayPictureState, AssaySample, AssayVfx, AssayMotion

### Community 4 - "Copper Cable Block"
Cohesion: 0.10
Nodes (4): CopperCableBlock, FluidPipeBlock, PiezoSwitchBlock, PneumaticTubeBlock

### Community 5 - "Material Item Components"
Cohesion: 0.07
Nodes (7): ComponentMaterialItem, EnergyInspectionGogglesItem, HeatedIngotItem, ManualHammerItem, ScientificInstrumentItem, TestProbesItem, ResearchReportItem

### Community 9 - "Mechanical Recipes and Networks"
Cohesion: 0.10
Nodes (8): Cache, MechanicalNetwork, Node, Result, CraftingMenuMixin, RecipeBookPlacementMixin, MechanicalProcesses, ResearchCraftingAccess

### Community 11 - "Mechanical Recipe Cache"
Cohesion: 0.08
Nodes (4): MechanicalRecipeCache, AssayEvents, ClientMenus, ModRecipeBook

### Community 12 - "Menu and Screen Registration"
Cohesion: 0.11
Nodes (7): ModMenuTypes, MaterialAnalyzerMenu, ContainerData, SimpleContainer, Slot, Slot, Slot

### Community 13 - "Empirical Material Catalog"
Cohesion: 0.11
Nodes (13): Material, AMETHYST, COAL, COPPER, DIAMOND, EMERALD, GOLD, IRON (+5 more)

### Community 15 - "Creative Tab Registration"
Cohesion: 0.13
Nodes (3): ModCreativeTabs, QuantumIndustries, ModRecipes

### Community 17 - "Research Menu Workflow"
Cohesion: 0.15
Nodes (5): Cost, Debit, ResearchTableMenu, ContainerData, SpendPlan

### Community 20 - "Pneumatic Tube Rendering"
Cohesion: 0.13
Nodes (3): PneumaticTubeRenderer, State, PneumaticTubeBlockEntity

### Community 22 - "Material Processing Rules"
Cohesion: 0.09
Nodes (19): MaterialProcesses, Process, ABRASIVE, FUEL, SAWING, SHEET, STRUCTURE, TRANSMISSION (+11 more)

### Community 23 - "Quantum Ore Generation"
Cohesion: 0.12
Nodes (6): QuantumOreBlock, Category, CHEMICAL, METAL, MineralCatalog, MineralInfo

### Community 25 - "Empirical Assay Materials"
Cohesion: 0.09
Nodes (11): EmpiricalMaterialCatalog, AMETHYST, COAL, COPPER, DIAMOND, EMERALD, GOLD, IRON (+3 more)

### Community 26 - "Foundation Recipe System"
Cohesion: 0.15
Nodes (3): FoundationRecipe, Pattern, Patterns

### Community 30 - "Assay Bench Renderer"
Cohesion: 0.13
Nodes (3): AssayBenchRenderer, EmpiricalAssayRenderer, State

### Community 34 - "Mechanical Renderer"
Cohesion: 0.17
Nodes (3): Box, MechanicalRenderer, State

### Community 44 - "Research Catalog Validation"
Cohesion: 0.21
Nodes (4): Branch, Cost, ResearchCatalog, Study

### Community 47 - "Assay Method Types"
Cohesion: 0.21
Nodes (5): Method, HEATING, INSPECTION, MECHANICAL, REACTION

### Community 61 - "Research Evidence Models"
Cohesion: 0.28
Nodes (3): AnyOf, Assay, ResearchEvidence

### Community 65 - "Third-Party Asset Notices"
Cohesion: 0.25
Nodes (8): gallery/fonts/NOTICE.md, https://github.com/rehan-remade/hollow-crown, Hollow Crown, Notices, Patch Pixel font, rehan-remade, Sprite Fusion open-source pixel art VFX, https://www.spritefusion.com/pixel-art-vfx

### Community 71 - "Forming Materials"
Cohesion: 0.60
Nodes (3): FormingMaterials, Metal, Metals

### Community 73 - "Third-Party License Terms"
Cohesion: 0.67
Nodes (3): Hugo Duprez, MIT License, Patch

## Knowledge Gaps
- **56 isolated node(s):** `INSPECTION`, `HEATING`, `REACTION`, `MECHANICAL`, `COPPER` (+51 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 313 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **49 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ModBlocks` connect `Bellows Machine Entities` to `Copper Cable Energy`, `Empirical Assay Blocks`, `Copper Cable Block`, `Block and Entity Registries`, `Block Entity Lifecycle`, `Menu and Screen Registration`, `Assay Bench Processing`, `Creative Tab Registration`, `Assay VFX Rendering`, `Fluid Pipe Storage`, `Pneumatic Tube Rendering`, `Quantum Ore Generation`, `Water Wheel Mechanics`, `Mechanical Sawmill Processing`, `Wire Drawing Energy`, `Electromechanical Press Processing`, `Gravitational Sorter State`, `Research Table Persistence`, `Bellows Ejector Energy`, `Galvanic Accumulator Energy`, `Load Relay Logic`, `Peristaltic Pump Energy`, `Settling Vat Processing`, `Bellows Brazier Storage`, `Bellows Ejector Block`, `Electromechanical Press Block`, `Gravitational Sorter Block`, `Peristaltic Pump Block`, `Research Table Block`, `Wire Drawing Bench Block`, `Manual Grain Mill Storage`, `Galvanic Accumulator Block`, `Manual Grain Mill Block`, `Bellows Brazier Block`, `Settling Vat Block`, `Fluid Capability Registration`, `Core Effects and Storage`?**
  _High betweenness centrality (0.166) - this node is a cross-community bridge._
- **What connects `INSPECTION`, `HEATING`, `REACTION` to the rest of the system?**
  _56 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Research Screen Details` be split into smaller, more focused modules?**
  _Cohesion score 0.07144456886898096 - nodes in this community are weakly interconnected._
- **Why does `Material` connect `Empirical Material Catalog` to `Assay Screen Controls`, `Assay Rendering Utilities`, `Material Item Components`, `Menu and Screen Registration`, `Utilities and Legacy Migration`, `Assay Bench Processing`, `Assay Method Types`, `Research and Assay Menus`, `Assay Catalog and Items`, `Material Processing Rules`, `Empirical Assay Materials`, `Minecraft API References`, `Research Evidence Models`, `Assay Bench Renderer`?**
  _High betweenness centrality (0.084) - this node is a cross-community bridge._
- **Should `Copper Cable Energy` be split into smaller, more focused modules?**
  _Cohesion score 0.05336538461538461 - nodes in this community are weakly interconnected._
- **Why does `EmpiricalAssayBenchBlockEntity` connect `Assay Bench Processing` to `Core Effects and Storage`, `Bellows Machine Entities`, `Empirical Material Catalog`, `Assay VFX Rendering`, `Research and Assay Menus`, `Assay Catalog and Items`, `Mechanical Sawmill Processing`, `Assay Bench Renderer`?**
  _High betweenness centrality (0.069) - this node is a cross-community bridge._
- **Should `Empirical Assay Blocks` be split into smaller, more focused modules?**
  _Cohesion score 0.05706214689265537 - nodes in this community are weakly interconnected._

## Scope and Caveats

- Source tree: `src` (757 code files and 2 license/notice documents).
- The 126 image assets were excluded from semantic extraction by user choice.
- Semantic worker token usage was not reported by the host tool, so token totals are incomplete.
- Graph health diagnostics found 504 dangling-endpoint edges, 9 self-loops, and 353 directed / 368 undirected same-endpoint collapses; inspect these before treating the graph as complete.
