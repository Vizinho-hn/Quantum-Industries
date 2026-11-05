package br.com.quantumindustries.dynamo;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.block.entity.ManualDynamoBlockEntity;
import br.com.quantumindustries.block.entity.ResearchTableBlockEntity;
import br.com.quantumindustries.menu.ResearchTableMenu;
import br.com.quantumindustries.recipe.FoundationRecipe;
import br.com.quantumindustries.recipe.ModRecipeBook;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import br.com.quantumindustries.recipe.ResearchCraftingAccess;
import br.com.quantumindustries.research.ResearchCatalog;
import br.com.quantumindustries.research.ResearchReportItem;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.research.WorldResearchData;
import com.mojang.authlib.GameProfile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = "quantumindustries")
public final class DynamoRuntimeVerification {
    private static final BlockPos POS = new BlockPos(0, 100, 0);
    private static ServerLevel level;
    private static FakePlayer player;
    private static ManualDynamoBlockEntity dynamo;
    private static long firstTurn;
    private static int turns;

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static BlockHitResult hit() {
        return new BlockHitResult(Vec3.atCenterOf(POS), Direction.UP, POS, false);
    }

    @SubscribeEvent public static void start(ServerStartedEvent event) throws Exception {
        if (!Boolean.getBoolean("quantumindustries.verifyDynamo")) return;
        level = event.getServer().overworld();
        level.setChunkForced(0, 0, true);
        try {
            var block = ModBlocks.MANUAL_DYNAMO.get().defaultBlockState();
            level.setBlock(POS, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(POS, block, 3);
            dynamo = (ManualDynamoBlockEntity) level.getBlockEntity(POS);
            player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "DynamoQA"));
            player.setPos(.5, 100, 1.5);
            check(dynamo.energy() == 0 && !dynamo.turn(), "cannot crank without installation");
            ItemStack crank = new ItemStack(ModItems.HAND_CRANK.get(), 2);
            block.useItemOn(crank, level, player, InteractionHand.MAIN_HAND, hit());
            check(dynamo.installed() && crank.getCount() == 1, "install consumes one crank");
            block.useItemOn(crank, level, player, InteractionHand.MAIN_HAND, hit());
            check(crank.getCount() == 1, "duplicate installation cannot consume a crank");
            block.useWithoutItem(level, player, hit());
            check(dynamo.energy() == 0, "unknown dynamo design prevents generation");
            verifyProgression();
            block.useWithoutItem(level, player, hit());
            check(dynamo.energy() == 12 && !dynamo.turn(), "one turn supplies 12 FE and starts cooldown");
            check(ModItems.ENERGY_INSPECTION_GOGGLES.get().getDefaultInstance()
                    .get(net.minecraft.core.component.DataComponents.EQUIPPABLE).slot() == EquipmentSlot.HEAD,
                    "goggles equip on head");
            firstTurn = level.getGameTime();
            turns = 1;
        } catch (Throwable error) {
            finish(error);
        }
    }

    private static void verifyProgression() {
        int structure = ResearchCatalog.indexOf("electromechanical_structure");
        int design = ResearchCatalog.indexOf("manual_dynamo_design");
        var sheet = ModItems.IRON_SHEET.get();
        var gear = ModItems.IRON_GEAR.get();
        var shaft = ModItems.MECHANICAL_SHAFT.get();
        var coil = ModItems.COPPER_COIL.get();
        var insulated = ModItems.INSULATED_COPPER_WIRE.get();
        var core = ModItems.PRIMITIVE_MECHANICAL_CORE.get();
        var coreGrid = grid(sheet, gear, sheet, Items.IRON_NUGGET, ModItems.ELECTROMECHANICAL_FRAME.get(),
                Items.IRON_NUGGET, null, shaft, null);
        var dynamoGrid = grid(coil, gear, coil, sheet, core, sheet, insulated, shaft, insulated);
        BlockPos bench = POS.east();
        level.setBlock(bench, ModBlocks.RESEARCH_TABLE_TIER_1.get().defaultBlockState(), 3);
        var entity = (ResearchTableBlockEntity) level.getBlockEntity(bench);
        var menu = (ResearchTableMenu) entity.createMenu(1, player.getInventory(), player);
        var research = WorldResearchData.get(level);
        var coilKey = recipeKey(ModItems.COPPER_COIL.get());
        ModRecipeBook.sync(player);
        check(player.getRecipeBook().contains(recipeKey(ModItems.SCIENTIFIC_ANALYZER_TIER_1.get())),
                "starter craft is available in the book on sync");
        check(!player.getRecipeBook().contains(coilKey), "coil is hidden before research");
        check(menu.stillValid(player), "Tier 1 research bench is usable");
        check(!menu.clickMenuButton(player, design), "design rejects missing prerequisites");
        check(!craft(core).matches(coreGrid, level), "automation cannot craft core");
        check(!craft(ModItems.MANUAL_DYNAMO.get()).matches(dynamoGrid, level), "automation cannot craft dynamo");

        int eraOne = 0;
        for (int id = 0; id < ResearchCatalog.STUDIES.size(); id++) {
            var study = ResearchCatalog.STUDIES.get(id);
            if (study.era() != 1) continue;
            check(!menu.clickMenuButton(player, id), "missing costs: " + study.key());
            for (var cost : study.costs()) {
                check(player.getInventory().add(new ItemStack(cost.item(), cost.amount())), "stage materials");
            }
            if (id == structure) verifyCraft(player, coreGrid, core, false);
            if (id == design) verifyCraft(player, dynamoGrid, ModItems.MANUAL_DYNAMO.get(), false);
            check(menu.clickMenuButton(player, id) && research.knows(player, id), "complete " + study.key());
            for (var cost : study.costs()) {
                check(player.getInventory().countItem(cost.item()) == 0, "consumed costs for " + study.key());
            }
            eraOne++;
        }
        check(player.getRecipeBook().contains(coilKey), "research adds the coil to the book");
        check(player.getRecipeBook().contains(recipeKey(core)), "research adds the core to the book");
        check(player.getRecipeBook().contains(recipeKey(ModItems.MANUAL_DYNAMO.get())),
                "research adds the dynamo to the book without induction");
        check(eraOne == 13 && !research.knows(player, "electromagnetic_induction"), "finish Era I before induction");
        verifyCraft(player, coreGrid, core, true);
        verifyCraft(player, dynamoGrid, ModItems.MANUAL_DYNAMO.get(), true);
        var other = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "DynamoGuest"));
        verifyCraft(other, coreGrid, core, false);
        verifyCraft(other, dynamoGrid, ModItems.MANUAL_DYNAMO.get(), false);
        BlockPos craftingTable = POS.south();
        level.setBlock(craftingTable, Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
        for (int id = 0; id < ResearchCatalog.STUDIES.size(); id++) {
            for (var preview : FoundationRecipe.previews(id)) {
                verifyBookPlacement(craftingTable, other, player, preview);
            }
        }
        verifyResearchTransfer(menu, other);
        verifyResearchCommands(craftingTable, other);
        verifyCraftingMenu(craftingTable, coreGrid, core);
        verifyCraftingMenu(craftingTable, dynamoGrid, ModItems.MANUAL_DYNAMO.get());
        check(FoundationRecipe.previews(structure).size() == 2, "structure and core recipe previews");
        check(FoundationRecipe.previews(design).getFirst().output().is(ModItems.MANUAL_DYNAMO.get()), "dynamo recipe preview");
    }

    private static ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> recipeKey(ItemLike output) {
        return ResourceKey.create(Registries.RECIPE, BuiltInRegistries.ITEM.getKey(output.asItem()));
    }

    private static FoundationRecipe craft(ItemLike output) {
        return (FoundationRecipe) level.getServer().getRecipeManager().byKey(recipeKey(output)).orElseThrow().value();
    }

    private static void verifyBookPlacement(BlockPos table, FakePlayer guest, FakePlayer researcher, FoundationRecipe.Preview preview) {
        var recipeManager = level.getServer().getRecipeManager();
        var key = recipeKey(preview.output().getItem());
        var recipe = recipeManager.byKey(key).orElseThrow();
        var displays = new java.util.ArrayList<net.minecraft.world.item.crafting.display.RecipeDisplayEntry>();
        recipeManager.listDisplaysForRecipe(key, displays::add);
        check(displays.size() == 1, "one registered display for " + key.identifier());
        for (FakePlayer crafter : java.util.List.of(guest, researcher)) {
            boolean allowed = crafter == researcher;
            check(crafter.getRecipeBook().contains(key) == allowed, "book ownership is individual");
            for (ItemStack ingredient : preview.ingredients()) {
                if (!ingredient.isEmpty()) check(crafter.getInventory().add(ingredient.copy()), "stage recipe ingredient");
            }
            var menu = new CraftingMenu(3, crafter.getInventory(), ContainerLevelAccess.create(level, table));
            crafter.containerMenu = menu;
            menu.handlePlacement(false, false, recipe, level, crafter.getInventory());
            ItemStack result = menu.getSlot(0).getItem();
            check(allowed ? ItemStack.matches(result, preview.output()) : result.isEmpty(),
                    "recipe-book placement: " + key.identifier());
            check(menu.getInputGridSlots().stream().filter(slot -> slot.hasItem()).count()
                            == (allowed ? preview.ingredients().stream().filter(stack -> !stack.isEmpty()).count() : 0),
                    "only the learned recipe moves ingredients");
            menu.removed(crafter);
            crafter.containerMenu = crafter.inventoryMenu;
            crafter.getInventory().clearContent();
        }
    }

    private static void verifyResearchTransfer(ResearchTableMenu menu, FakePlayer recipient) {
        int wire = ResearchCatalog.indexOf("copper_wire");
        int conductivity = ResearchCatalog.indexOf("conductivity");
        for (int i = 0; i < 3; i++) {
            var material = java.util.List.of(Items.BLACK_DYE, Items.FEATHER, Items.PAPER).get(i);
            check(player.getInventory().add(new ItemStack(material, 2)), "stage report supplies");
            check(menu.clickMenuButton(player, ResearchTableMenu.REPORT_DEPOSIT + i), "store report supplies");
        }
        check(menu.clickMenuButton(player, ResearchTableMenu.REPORT_WRITE + wire), "write wire report");
        check(menu.clickMenuButton(player, ResearchTableMenu.REPORT_WRITE + conductivity), "write conductivity report");
        for (int i = 0; i < 3; i++) check(menu.supply(i) == 0, "one of each supply per report");

        var research = WorldResearchData.get(level);
        ItemStack wireReport = takeReport(wire);
        recipient.setItemInHand(InteractionHand.MAIN_HAND, wireReport);
        wireReport.getItem().use(level, recipient, InteractionHand.MAIN_HAND);
        check(!wireReport.isEmpty() && !research.knows(recipient, wire), "report checks prerequisites");

        ItemStack conductivityReport = takeReport(conductivity);
        recipient.setItemInHand(InteractionHand.MAIN_HAND, conductivityReport);
        conductivityReport.getItem().use(level, recipient, InteractionHand.MAIN_HAND);
        check(conductivityReport.isEmpty() && research.knows(recipient, conductivity), "report teaches its study");

        recipient.setItemInHand(InteractionHand.MAIN_HAND, wireReport);
        wireReport.getItem().use(level, recipient, InteractionHand.MAIN_HAND);
        check(wireReport.isEmpty() && research.knows(recipient, wire)
                && recipient.getRecipeBook().contains(recipeKey(ModItems.COPPER_WIRE.get())), "report unlocks craft in book");
    }

    private static ItemStack takeReport(int study) {
        var inventory = player.getInventory();
        var expected = ResearchReportItem.create(study);
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (ItemStack.isSameItemSameComponents(inventory.getItem(slot), expected)) return inventory.removeItem(slot, 1);
        }
        throw new AssertionError("missing report");
    }

    private static void verifyResearchCommands(BlockPos table, FakePlayer recipient) {
        var research = WorldResearchData.get(level);
        var commands = level.getServer().getCommands();
        var source = recipient.createCommandSourceStack();
        var wire = recipeKey(ModItems.COPPER_WIRE.get());
        var dynamo = recipeKey(ModItems.MANUAL_DYNAMO.get());
        var noPermissions = source.withPermission(PermissionSet.NO_PERMISSIONS);
        var admin = source.withPermission(PermissionSet.ALL_PERMISSIONS);
        commands.performPrefixedCommand(noPermissions, "quantum:desbloquear_pesquisas");
        check(!research.knows(recipient, "manual_dynamo_design"), "unlock requires permission");
        commands.performPrefixedCommand(admin, "quantum:desbloquear_pesquisas");
        check(research.knows(recipient, "manual_dynamo_design") && recipient.getRecipeBook().contains(dynamo),
                "unlock command grants research and recipes");
        commands.performPrefixedCommand(noPermissions, "quantum:clear_conhecimento");
        check(recipient.getRecipeBook().contains(dynamo), "clear requires permission");

        recipient.getInventory().add(new ItemStack(Items.COPPER_INGOT, 3));
        var recipe = level.getServer().getRecipeManager().byKey(wire).orElseThrow();
        var menu = new CraftingMenu(4, recipient.getInventory(), ContainerLevelAccess.create(level, table));
        recipient.containerMenu = menu;
        menu.handlePlacement(false, false, recipe, level, recipient.getInventory());
        check(menu.getSlot(0).getItem().is(ModItems.COPPER_WIRE.get()), "unlocked wire craft is ready");
        commands.performPrefixedCommand(admin, "quantum:clear_conhecimento");
        check(!research.knows(recipient, "copper_wire") && !recipient.getRecipeBook().contains(wire)
                && !recipient.getRecipeBook().contains(dynamo), "clear command removes research and recipes");
        check(menu.getSlot(0).getItem().isEmpty(), "clear invalidates an open crafting result");
        check(menu.getInputGridSlots().stream().filter(slot -> slot.hasItem()).count() == 3, "clear preserves inputs");
        check(research.knows(player, "copper_wire"), "clear affects only the sender");
        menu.removed(recipient);
        recipient.containerMenu = recipient.inventoryMenu;
    }

    private static void verifyCraftingMenu(BlockPos table, CraftingInput grid, ItemLike output) {
        var menu = new CraftingMenu(2, player.getInventory(), ContainerLevelAccess.create(level, table));
        player.containerMenu = menu;
        for (int i = 0; i < grid.size(); i++) {
            menu.getSlot(i + 1).set(grid.getItem(i).copy());
        }
        check(menu.getSlot(0).getItem().is(output.asItem()), "vanilla output: " + output);
        int before = player.getInventory().countItem(output.asItem());
        menu.clicked(0, 0, ContainerInput.QUICK_MOVE, player);
        check(player.getInventory().countItem(output.asItem()) == before + 1, "one crafted item: " + output);
        for (int i = 0; i <= grid.size(); i++) {
            check(menu.getSlot(i).getItem().isEmpty(), "ingredients and output consumed");
        }
        menu.clicked(0, 0, ContainerInput.QUICK_MOVE, player);
        check(player.getInventory().countItem(output.asItem()) == before + 1, "no repeated output");
        player.containerMenu = player.inventoryMenu;
    }

    private static CraftingInput grid(ItemLike... items) {
        return CraftingInput.of(3, 3, Arrays.stream(items)
                .map(item -> item == null ? ItemStack.EMPTY : new ItemStack(item)).toList());
    }

    private static void verifyCraft(FakePlayer crafter, CraftingInput grid, ItemLike output, boolean allowed) {
        ResearchCraftingAccess.withCrafter(crafter, () -> {
            check(craft(output).matches(grid, level) == allowed, "craft access: " + output);
            if (allowed) {
                var result = craft(output).assemble(grid);
                check(result.is(output.asItem()) && result.getCount() == 1, "craft output: " + output);
            }
        });
    }

    @SubscribeEvent public static void tick(ServerTickEvent.Post event) throws Exception {
        if (turns == 0 || level == null) return;
        try {
            long elapsed = level.getGameTime() - firstTurn;
            if (turns < 20) {
                if (elapsed % 10 != 0) {
                    check(dynamo.energy() == turns * 12 && !dynamo.turn(), "no idle generation or early turns");
                    return;
                }
                check(dynamo.turn(), "turn allowed after 10 ticks");
                turns++;
                return;
            }
            if (elapsed < 220) return;
            check(dynamo.energy() == 240 && !dynamo.turn(), "full storage persists at rest and cannot overflow");
            var saved = dynamo.saveWithoutMetadata(level.registryAccess());
            var restored = new ManualDynamoBlockEntity(POS, dynamo.getBlockState());
            restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
            check(restored.energy() == 240 && restored.installed(), "save/load preserves crank and FE");
            player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            player.setShiftKeyDown(true);
            check(player.isShiftKeyDown() && player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty(),
                    "fake player must crouch with both hands empty");
            check(level.getBlockEntity(POS) == dynamo, "test chunk stays loaded");
            dynamo.getBlockState().useWithoutItem(level, player, hit());
            check(!dynamo.installed() && dynamo.energy() == 240 && !dynamo.turn(), "removal preserves charge: installed=" + dynamo.installed() + ", FE=" + dynamo.energy());
            check(player.getInventory().countItem(ModItems.HAND_CRANK.get()) == 1 && !dynamo.detach(),
                    "removal returns exactly one crank");
            check(dynamo.install(true), "can install a new crank after removal");
            int before = droppedCranks();
            check(level.destroyBlock(POS, true), "break dynamo");
            check(droppedCranks() == before + 1, "break returns exactly one paid crank");
            finish(null);
        } catch (Throwable error) {
            finish(error);
        }
    }

    private static int droppedCranks() {
        return level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                        new net.minecraft.world.phys.AABB(POS).inflate(2),
                        item -> item.getItem().is(ModItems.HAND_CRANK.get()))
                .stream().mapToInt(item -> item.getItem().getCount()).sum();
    }

    private static void finish(Throwable error) throws Exception {
        turns = 0;
        try {
            Files.writeString(Path.of("../result.txt"), error == null ? "PASS: 13 Era I studies, recipe book placement, individual craft gates and dynamo server behavior\n" : "FAIL: " + error);
            if (error != null) error.printStackTrace();
        } finally {
            level.getServer().halt(false);
        }
    }
}
