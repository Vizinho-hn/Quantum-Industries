# NeoForge 26.1.2 & Java 25 Reference

## Environment & Toolchain

- **JDK:** Java 25 (`java.toolchain.languageVersion = JavaLanguageVersion.of(25)` in `build.gradle`).
  - Linux path: `/usr/lib/jvm/java-25-openjdk-amd64` (set `JAVA_HOME` explicitly when system default `java` is JDK 21).
- **Gradle:** Wrapper `9.2.1`, `net.neoforged.moddev` `2.0.148`.
- **Source sets (`build.gradle`):**
  - Common/server/shared: `src/main/java/br/com/quantumindustries/`
  - Client-only (included in compilation via `sourceSets.main.java.srcDir("src/client/java")`): `src/client/java/br/com/quantumindustries/client/`
  - Tests (`JavaExec` verifiers): `tests/LegacyResearchCheck.java`, `tests/MechanicalFoundationCheck.java`

## Verified Minecraft 26.1.2 / NeoForge 26.1.2.125 API Deltas

Do not apply 1.20.1/1.21.1 habits blindly. Observed directly in `Quantum-Industries`:
- **Identifiers:** `net.minecraft.resources.Identifier` (`Identifier.fromNamespaceAndPath(QuantumIndustries.MODID, "...")`) instead of `ResourceLocation`.
- **BlockEntity persistence:** `saveAdditional(ValueOutput out)` and `loadAdditional(ValueInput in)` using `out.store("key", Codec, val)` / `in.read("key", Codec)` / `in.getIntOr(...)` / `in.getStringOr(...)` (see `EmpiricalAssayBenchBlockEntity`).
- **Block removal side effects:** `preRemoveSideEffects(BlockPos pos, BlockState state)` instead of legacy `onRemove`.
- **Level side check:** `level.isClientSide()` is a method call.
- **CompoundTag getters return `Optional`:** e.g. `data.getCompound(KEY).orElseGet(CompoundTag::new)`, `getBoolean(...).orElse(false)`, `getString(...).orElse("")` (see `AssayProfile`).
- **World SavedData:** `SavedDataType<T>` with a `RecordCodecBuilder` `Codec<T>` (see `WorldResearchData.TYPE`).
- **Client GUI extraction:** `AbstractContainerScreen.extractContents(GuiGraphicsExtractor g, ...)` and `Button.extractContents(...)` / `extractDefaultLabel(...)`, `RenderPipelines.GUI_TEXTURED`, `g.submitPictureInPictureRenderState(...)` (see `EmpiricalAssayBenchScreen`).
- **Recipe serializers:** `new RecipeSerializer<>(CODEC, STREAM_CODEC)` (see `ModRecipes`).
- **Item data components:** `DataComponents.EQUIPPABLE` with `Equippable.builder(EquipmentSlot.HEAD).build()` (see `ModItems.ENERGY_INSPECTION_GOGGLES`).

## Using `mcdev` MCP (Static Decompiled 26.1.2 Source + Callgraph)

Minecraft `26.1.2` is pre-decompiled, indexed (6,352 classes / 525 packages), and callgraphed (394,389 call refs) in `mcdev`.
- Unknown class or signature? Call `mc_search` or `mc_list_classes` first, then `mc_get_class` / `mc_get_method`.
- Checking subclasses/implementors (e.g., `SavedData`, `RecipeSerializer`, `AbstractContainerScreen`)? Call `mc_find_hierarchy`.
- Checking how vanilla calls a hook or screen method? Call `mc_find_refs` instead of guessing execution order.
- Note: `mcdev` indexes vanilla Minecraft `26.1.2` (not NeoForge-patched classes); verify NeoForge extensions (`DeferredRegister`, `IMenuTypeExtension`, capabilities) against existing `Quantum-Industries` usage or Gradle dependency sources.
