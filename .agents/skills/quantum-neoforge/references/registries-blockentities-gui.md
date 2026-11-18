# Registries, BlockEntities, Menus, Capabilities & Networking

## Existing Registry Classes

Always register into the existing central holders and wire them in `QuantumIndustries(IEventBus modEventBus)`:
- **Blocks & BlockEntityTypes:** `core/ModBlocks.java` (`DeferredRegister.Blocks`, `DeferredRegister<BlockEntityType<?>>`)
  - Use `BLOCKS.registerBlock("id", BlockClass::new, props -> ...)` and `BLOCK_ENTITIES.register("id", () -> new BlockEntityType<>(EntityClass::new, Set.of(BLOCK.get()), false))`.
- **Items & BlockItems:** `ModItems.java` (`DeferredRegister.Items`)
  - Block items: `ITEMS.registerSimpleBlockItem(ModBlocks.FOO)`
  - Simple items: `ITEMS.registerSimpleItem("id")`
  - Custom items: `ITEMS.registerItem("id", Constructor::new, props -> ...)`
- **Menus:** `core/ModMenuTypes.java` (`DeferredRegister<MenuType<?>>` via `IMenuTypeExtension.create(...)`)
- **Recipes:** `recipe/ModRecipes.java` (`SERIALIZERS`, `TYPES`)
- **Creative Tabs:** `core/ModCreativeTabs.java`
- **Capabilities:** `fluid/ModFluidCapabilities.java` registered via `modEventBus.addListener(ModFluidCapabilities::register)`

## BlockEntity & Client/Server Authority Rules

1. **Server owns state:** `BlockEntity` logic (`tick()`, inventories, timers, energy/fluid transfer) runs only when `level instanceof ServerLevel` (or `!level.isClientSide()`). Menus observe server state; they never advance machine or research timers.
2. **Sync pattern (`EmpiricalAssayBenchBlockEntity`):**
   - Call `setChanged()` and `level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3)` when synced visual/HUD state changes (guarded by `!loading && level != null && !level.isClientSide()`).
   - Override `getUpdateTag(HolderLookup.Provider)` (`return saveCustomOnly(registries);`) and `getUpdatePacket()` (`ClientboundBlockEntityDataPacket.create(this)`).
   - Drop physical inventory items in `preRemoveSideEffects(...)` on server; never drop virtual/reserved research samples.
3. **Client/server separation:**
   - Screens (`src/client/java/.../client/gui/`), renderers (`client/render/`, `client/renderer/`), and `Minecraft.getInstance()` must stay strictly under `src/client/java` or client-gated code so `runServer` / dedicated servers never hit `NoClassDefFoundError`.
   - Use `CustomPacketPayload` / `AbstractContainerMenu` button clicks (`handleInventoryButtonClick`) validated server-side; never trust client packets as proof of research completion or recipe unlock.
4. **Energy, Fluid & Mechanical Networks:**
   - Reuse existing domain helpers (`energy/CableNetwork.java`, `fluid/FluidTransfer.java`, `fluid/FluidInspectable.java`, `mechanical/MechanicalNetwork.java`, `mechanical/RotationalBlockEntity.java`, `block/entity/EnergyInspectable.java`, `logistics/ItemRouting.java`).
