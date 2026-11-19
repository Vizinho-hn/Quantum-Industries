# Quantum Industries — Era II Visual Standardization & Style Guide

**Scope:** Era II (*Early Electromechanical, Galvanic, Thermal & Chemical Industrialization*)  
**Platform:** Minecraft `26.1.2` • NeoForge `26.1.2.125` • GeckoLib `5.5.2`  
**Status:** Style Guide & Full Standardization Specification (following Era I commit `a84e1a8`)

---

## 1. Core Aesthetic Concept of Era II

Where **Era I** portrays an empirical artisan workshop built from dark aged oak, hand-forged wrought iron, aged bronze, and leather, **Era II** represents the leap into **early industrial electromechanical, thermal, galvanic, and chemical engineering**:

- **Material Shift:** Structural frames transition from timber joinery to **heavy sand-cast iron and riveted gunmetal steel**; conductors upgrade from bare hand-hammered wires to **brushed industrial copper busbars, machined brass fittings, glazed porcelain insulators, and vulcanized ebonite switchboards**; furnaces and reactors employ **kilned refractory firebrick, constantan resistance windings, lead tank linings, and thick borosilicate chemical glass**.
- **Mechanical & Electrical Plausibility:** Every machine exposes its working principle cleanly within the `16×16×16` Minecraft voxel grid—dies, rams, busbar lugs, solenoid coils, peristaltic pump rollers, chemical sight-glasses, or pneumatic pressure bellows—without floating fragments or decorative sci-fi clutter.
- **Readable Silhouette Contrast:** Players can immediately distinguish an Era I artisan bench/kiln from an Era II cast-iron/porcelain/brass machine at medium distance, while retaining cohesive Minecraft voxel proportions and crisp nearest-neighbor pixel art.

---

## 2. Official Era II Color Palette & Surface Finishes (5-Value Ramps)

All block atlases (`128×128` RGBA) and 2D item sprites (`32×32` RGBA) use strict **5-step value ramps** (*Deep / Shadow / Mid / Light / Highlight*) with selective dark outlines and crisp nearest-neighbor pixel texels.

| Material | Primary Era II Role | Deep | Shadow | Mid | Light | Highlight |
|---|---|---|---|---|---|---|
| **Heavy Cast Iron** | Machine plinths, frames, anvil bolsters, stanchions | `#14161a` | `#22262e` | `#353b47` | `#4d5666` | `#6e7685` |
| **Riveted Gunmetal Steel** | Press columns, furnace shells, pump casings, dies | `#1a1d24` | `#2a2f3a` | `#3e4656` | `#586378` | `#7b88a1` |
| **Brushed Industrial Copper** | Busbar lugs, tuyeres, pipe barrels, coil windings | `#42180b` | `#6e2812` | `#a34322` | `#d4673b` | `#f09654` |
| **Machined Brass** | Valves, bearings, pressure actuators, sorter weights, flanges | `#38280e` | `#5e4419` | `#8c6828` | `#bd9242` | `#e0ba67` |
| **Glazed White Porcelain** | High-voltage bushings, spool bobbins, relay insulators | `#6e685f` | `#968f84` | `#bfb8ad` | `#dfd9ce` | `#f5f0e6` |
| **Refractory Firebrick** | Brazier & electric smelter masonry, kiln linings | `#3d1a10` | `#5e2919` | `#853d27` | `#ab553a` | `#cc7252` |
| **Thermal / Constantan Glow** | Annealing embers, heating coils, energized filaments | `#4a1408` | `#8c240d` | `#cf4a17` | `#f58426` | `#ffd266` |
| **Vulcanized Ebonite & Slate** | Switchboard panels, relay bases, insulated rods | `#121317` | `#202229` | `#323640` | `#474d59` | `#606878` |
| **Acid-Resistant Lead** | Vat liners, accumulator plates, cable shields | `#1b1e26` | `#292d36` | `#3c4252` | `#5c6475` | `#7b8599` |
| **Borosilicate Chemical Glass** | Settling vats, pump rotor windows, acid bottles | `#1a3836` | `#285450` | `#3b7a75` | `#78c2bc` | `#c4f0ec` |
| **Oiled Industrial Oak** | Drawbench & sorter timber beds (darker/heavier than Era I) | `#1a1109` | `#2c1d10` | `#422c18` | `#593d23` | `#704e2e` |

### Strict Era II Visual Exclusions
- **No Era III futuristic chrome/cyan neon:** Save high-gloss white composites, cyan energy plasma, and silicon wafer accents for Era III+.
- **No vanilla `stone_bricks` / `oak_planks` / `iron_block` / `copper_block` placeholders:** Every Era II block uses dedicated `128×128` pixel art textures.
- **No sub-pixel sliver geometry (`< 0.5` unit) or coplanar Z-fighting:** Every model must pass Blockbench `check_model` with `0` issues.
- **Preserve GUI slot constraints:** No direct block/item model rendered in GUI slots may exceed `16.0` units in `X` (`PlacementAndItemTest.test_no_item_renders_a_multi_block_model_in_the_gui`).

---

## 3. Geometry, Orientation & Port Alignment Rules

- **Coordinate Bounds:** Static machine blocks stay within `[0, 16]` on `X/Y/Z` (with `piezo_switch` matching `Block.box(1, 0, 1, 15, 2, 15)` / subtle low-profile actuator plate `y <= 2.8`).
- **Pipe & Pneumatic Tube Coupling Alignment (`[4, 4]..[12, 12]`):**
  - `fluid_pipe`, `peristaltic_pump`, `settling_vat`, `bellows_ejector`, and `gravitational_sorter` place their inlet/outlet flanged collars centered at `(8, 8)` within `[4, 4]..[12, 12]` so they flush-mate cleanly with `pneumatic_tube` and `glass_pipe`.
- **Electrical Busbar Alignment (`[5.25, 5.25]..[10.75, 10.75]`):**
  - `electric_alloy_smelter`, `electromechanical_press`, `load_relay`, `peristaltic_pump`, and `piezo_switch` expose copper terminal lugs/porcelain bushings aligned around the standard conduit axis so `copper_busbar` and `insulated_conduit` connect visually without floating ends.
- **Joint Anti-Z-Fighting:** Overlapping collars, rivets, and dielectric rings are staggered by `0.05–0.2` units so no two cuboids share coplanar overlapping faces.
- **Blockbench Project & Export Parity:** Every remodeled Era II machine has a `.bbmodel` source in `design/mechanical/<name>.bbmodel` plus its Minecraft block model `models/block/<name>.json`.

---

## 4. Complete Era II Audit & Action Inventory

Based on `ResearchCatalog.java` (`era == 2` studies), `ModBlocks.java`, `ModItems.java`, and existing resource inspection:

### 4.1 Era II Studies (`ResearchCatalog.java`, `era == 2`)
1. `electromagnetic_induction` (`era == 2`)
2. `galvanic_storage` -> `voltaic_pile`, `lead_acid_accumulator`
3. `fluid_hydrostatics` -> `fluid_pipe`, `glass_pipe`, `settling_vat`
4. `pneumatic_transport` -> `pneumatic_tube`, `bellows_ejector`, `gravitational_sorter`
5. `peristaltic_mechanics` -> `peristaltic_pump`
6. `piezoelectricity` -> `piezo_switch`
7. `load_protection` -> `load_relay`
8. `thermal_annealing` -> `bellows_brazier`, `heated_*_ingot`
9. `wire_drawing` -> `wire_drawing_bench`, `gold_wire`, `electrum_wire`, `constantan_wire`
10. `electromechanical_forming` -> `electromechanical_press`
11. `electric_alloy_smelting` -> `electric_alloy_smelter`
12. `chemical_leaching` -> `hydrochloric_acid`, `sulfuric_acid`, `phosphate_fertilizer`, `pigment_sediment`

### 4.2 Era II Blocks & Machines Classification

| Block ID | Previous State | Audit Classification | Era II Standardization Action |
|---|---|---|---|
| `bellows_brazier` | 3 vanilla boxes (`stone_bricks`, `iron_block`, `magma`) | **Remodel completely** | 18-cube cast-iron & refractory brick annealing brazier with ember hearth, ingot grate & copper tuyere |
| `wire_drawing_bench` | 3 vanilla boxes (`oak_planks`, `iron_block`, `copper_block`) | **Remodel completely** | 18-cube oiled timber & cast-iron drawbench with hardened die plate, wire payoff spool & tension capstan |
| `electromechanical_press` | 11 vanilla boxes (`anvil`, `iron_block`, `copper_block`, `gold_block`) | **Remodel completely** | 20-cube heavy twin-column cast-iron stamping press with overhead copper solenoid & corrugated steel ram |
| `electric_alloy_smelter` | 3 vanilla boxes (`blast_furnace_*`, `iron_block`, `copper_block`) | **Remodel completely** | 20-cube riveted steel & firebrick electric resistance furnace with glowing constantan coil window & porcelain busbars |
| `load_relay` | 2 vanilla boxes (`iron_block`, `copper_block`) | **Remodel completely** | 16-cube ebonite switchboard overload breaker with twin copper solenoids, brass armature & trip indicator (`powered` variants) |
| `piezo_switch` | 2 flat slabs using low-detail `32×16` `piezo_switch_atlas` | **Remodel completely** | 14-cube low-profile cast-iron, quartz crystal & ribbed brass floor actuator (`y <= 2.8`) |
| `peristaltic_pump` | 3 vanilla boxes (`copper_block`, `iron_block`) | **Remodel completely** | 18-cube cast-iron & brass rotary peristaltic pump with borosilicate hose-rotor window & `[4..12]` pipe flanges |
| `settling_vat` | 6 vanilla boxes (`iron_block`, `glass`) | **Remodel completely** | 18-cube riveted cast-iron & lead-lined decantation tank with thick translucent borosilicate sight-glass & reagent level |
| `bellows_ejector` | 3 vanilla boxes (`iron_block`, `copper_block`) | **Remodel completely** | 16-cube pneumatic impulse chamber with pleated copper-leather bellows dome & `[4..12]` tube collars |
| `gravitational_sorter` | 6 vanilla boxes (`oak_planks`, `iron_bars`) | **Remodel completely** | 18-cube dark industrial oak & cast-iron manifold with sight window, brass counterweight beam & `[4..12]` tube collars |
| `fluid_pipe` | 3 vanilla `copper_block` boxes | **Remodel completely** | 14-cube cylindrical beveled brushed copper pipe with bolted brass end flanges, cast-iron saddle & relief plug |
| `voltaic_pile` | Custom 27-element `32×32` model | **Keep as-is** | Already high-detail stacked brine/copper/zinc cell column |
| `lead_acid_accumulator` | Custom 17-element `32×32` model | **Keep as-is** | Already high-detail lead-plate acid battery jar |
| `copper_busbar` | Custom 6-dir multipart cable (`64×64`) | **Keep as-is** | Reference Era II electrical busbar aesthetic |
| `insulated_conduit` | Custom 6-dir multipart cable (`64×64`) | **Keep as-is** | Reference Era II insulated conduit aesthetic |
| `pneumatic_tube` | Custom 16-element tube (`industrial_tube_copper` + translucent glass) | **Keep as-is** | Reference `[4..12]` pneumatic tube port aesthetic |
| `glass_pipe` | Custom 12-element tube (`industrial_tube_copper` + translucent glass) | **Keep as-is** | Reference `[4..12]` glass fluid pipe aesthetic |
| `scientific_analyzer` | Custom 29-element `128×128` research table | **Keep as-is** | Era I -> Era II research transition table |

### 4.3 Era II 3D & 2D Item Standardization

- **3D Plates (`models/item/*_sheet.json` using `item/era2_materials.png` `128×128`):** Convert `gold_sheet`, `bronze_sheet`, `brass_sheet`, `lead_sheet`, `invar_sheet`, `electrum_sheet`, `constantan_sheet`, and `steel_sheet` to 2-element 3D bevel-edged metal sheets matching Era I's `copper_sheet`/`iron_sheet`/`tin_sheet`/`zinc_sheet`.
- **3D Coils & Wires (`models/item/constantan_coil.json`, `*_wire.json`):** Convert `constantan_coil` to a 24-element 3D wound resistance bobbin on glazed porcelain/brass flanges, and convert `gold_wire`, `electrum_wire`, and `constantan_wire` to 13-element 3D bundled conductor coils matching `copper_wire`.
- **2D `32×32` RGBA Pixel-Art Sprites (`textures/item/*.png`):** Upgrade Era II chemical reagents, industrial intermediates, and heated annealing ingots (`heated_*_ingot`, `hydrochloric_acid`, `sulfuric_acid`, `galvanic_electrolyte`, `phosphate_fertilizer`, `pigment_sediment`, `refractory_brick`, `ebonite_rod`, plus fallback 2D sprites for sheets/wires/coils) to cohesive `32×32` RGBA 5-layer pixel art. Standard cross-mod metallurgy/mineral staples (`*_ingot`, `*_nugget`, `*_dust`, `raw_*`, `apatite*`, `fluorite*`, `salt*`, `sulfur`, `graphite`, `cinnabar`) retain their conventional `16×16` mod-standard sprites.
