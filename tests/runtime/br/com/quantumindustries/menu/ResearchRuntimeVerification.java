package br.com.quantumindustries.menu;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.recipe.FoundationRecipe;
import br.com.quantumindustries.recipe.ResearchCraftingAccess;
import br.com.quantumindustries.research.ResearchIds;
import br.com.quantumindustries.assay.AssayProfile;
import br.com.quantumindustries.assay.AssayCatalog;
import br.com.quantumindustries.research.WorldResearchData;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import java.util.List;
import java.util.UUID;

public final class ResearchRuntimeVerification {
    private static final BlockPos TABLE = new BlockPos(40, 100, 0);
    private static int checks;

    private ResearchRuntimeVerification() {}

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static FakePlayer player(ServerLevel level) {
        FakePlayer player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "ResearchTest"));
        player.setPos(TABLE.getX() + .5, TABLE.getY(), TABLE.getZ() + .5);
        return player;
    }

    private static ResearchTableMenu open(FakePlayer player) {
        var entity = player.level().getBlockEntity(TABLE);
        if (!(entity instanceof br.com.quantumindustries.block.entity.ResearchTableBlockEntity table)) {
            throw new AssertionError("Research table block entity missing");
        }
        ResearchTableMenu menu = new ResearchTableMenu(7, player.getInventory(), table,
                new net.minecraft.world.inventory.SimpleContainerData(ResearchTableMenu.DATA_SIZE));
        player.containerMenu = menu;
        return menu;
    }

    private static int count(FakePlayer player, Item item) {
        return player.getInventory().countItem(item);
    }

    public static int run(ServerLevel level) {
        checks = 0;
        level.setBlock(TABLE, ModBlocks.RESEARCH_TABLE.get().defaultBlockState(), 3);
        var entity = (br.com.quantumindustries.block.entity.ResearchTableBlockEntity) level.getBlockEntity(TABLE);
        entity.reportSupplies().clearContent();
        try {
            verifySpendPlan(level);
            verifyResearchCommands(level);
            verifyCraftingGate(level);
            return checks;
        } finally {
            level.setBlock(TABLE, Blocks.AIR.defaultBlockState(), 3);
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(TABLE).inflate(8))) item.discard();
        }
    }

    private static void verifySpendPlan(ServerLevel level) {
        FakePlayer player = player(level);
        var inventory = player.getInventory();
        inventory.clearContent();
        inventory.setItem(0, new ItemStack(Items.COPPER_INGOT, 2));
        inventory.setItem(1, new ItemStack(Items.COPPER_INGOT, 3));
        ResearchTableMenu.SpendPlan exact = ResearchTableMenu.prepareSpend(inventory,
                new ResearchTableMenu.Cost(Items.COPPER_INGOT, 3),
                new ResearchTableMenu.Cost(Items.COPPER_INGOT, 2));
        check(exact != null && exact.commit(), "duplicate costs aggregate and consume exact total");
        check(count(player, Items.COPPER_INGOT) == 0, "split stacks consumed exactly");

        inventory.setItem(0, new ItemStack(Items.COPPER_INGOT, 3));
        ResearchTableMenu.SpendPlan insufficient = ResearchTableMenu.prepareSpend(inventory,
                new ResearchTableMenu.Cost(Items.COPPER_INGOT, 2),
                new ResearchTableMenu.Cost(Items.COPPER_INGOT, 2));
        check(insufficient == null, "insufficient aggregated duplicate costs rejected");
        check(count(player, Items.COPPER_INGOT) == 3, "failed plan leaves all stacks unchanged");

        check(ResearchTableMenu.prepareSpend(inventory, new ResearchTableMenu.Cost(Items.PAPER, 0)) == null,
                "zero cost rejected");
        check(ResearchTableMenu.prepareSpend(inventory, new ResearchTableMenu.Cost(Items.PAPER, -1)) == null,
                "negative cost rejected");
        check(ResearchTableMenu.prepareSpend(inventory,
                new ResearchTableMenu.Cost(Items.COPPER_INGOT, Integer.MAX_VALUE),
                new ResearchTableMenu.Cost(Items.COPPER_INGOT, Integer.MAX_VALUE)) == null,
                "oversized aggregate rejected without overflow");
        check(count(player, Items.COPPER_INGOT) == 3, "invalid and oversized plans do not consume");
    }

    private static void verifyResearchCommands(ServerLevel level) {
        FakePlayer owner = player(level);
        ResearchTableMenu menu = open(owner);
        var inventory = owner.getInventory();
        inventory.clearContent();

        int studyCount = br.com.quantumindustries.research.ResearchCatalog.STUDIES.size();
        check(!menu.clickMenuButton(owner, -1), "negative command rejected");
        check(!menu.clickMenuButton(owner, Integer.MIN_VALUE), "minimum integer command rejected");
        check(!menu.clickMenuButton(owner, Integer.MAX_VALUE), "large command rejected");
        check(!menu.clickMenuButton(owner, ResearchTableMenu.REPORT_WRITE + studyCount), "command after final report rejected");
        check(!menu.clickMenuButton(owner, ResearchTableMenu.REPORT_DEPOSIT), "empty deposit rejected");
        check(!menu.clickMenuButton(owner, ResearchTableMenu.REPORT_WITHDRAW), "empty withdrawal rejected");
        check(!menu.clickMenuButton(owner, ResearchTableMenu.REPORT_WRITE), "unknown report rejected");
        check(!menu.complete(-1) && !menu.isRevealed(studyCount) && !menu.hasCost(-1, 0),
                "invalid state queries safely reject");

        check(WorldResearchData.get(level).learn(owner, "material_observation"), "initial observation completed");
        AssayProfile.of(owner).complete(AssayCatalog.Material.COPPER, AssayCatalog.Method.INSPECTION);

        inventory.setItem(0, new ItemStack(Items.COPPER_INGOT, 3));
        inventory.setItem(1, new ItemStack(Items.REDSTONE));
        inventory.setItem(2, new ItemStack(Items.PAPER));
        for (int slot = 3; slot < inventory.getContainerSize(); slot++) {
            inventory.setItem(slot, new ItemStack(Items.DIRT, 64));
        }
        check(menu.clickMenuButton(owner, 0), "research succeeds with exact resources in a full inventory");
        check(count(owner, Items.COPPER_INGOT) == 0 && count(owner, Items.REDSTONE) == 0
                && count(owner, Items.PAPER) == 0, "research charges exact costs");
        check(WorldResearchData.get(level).knows(owner, ResearchIds.of("conductivity")),
                "research completion is recorded for owner");
        check(!menu.clickMenuButton(owner, 0), "completed research cannot be purchased twice");

        FakePlayer insufficient = player(level);
        ResearchTableMenu insufficientMenu = open(insufficient);
        check(WorldResearchData.get(level).learn(insufficient, "material_observation"), "insufficient player has observation");
        AssayProfile.of(insufficient).complete(AssayCatalog.Material.COPPER, AssayCatalog.Method.INSPECTION);
        insufficient.getInventory().setItem(0, new ItemStack(Items.COPPER_INGOT, 3));
        insufficient.getInventory().setItem(1, new ItemStack(Items.REDSTONE));
        check(!insufficientMenu.clickMenuButton(insufficient, 0), "missing one resource blocks research");
        check(count(insufficient, Items.COPPER_INGOT) == 3 && count(insufficient, Items.REDSTONE) == 1,
                "insufficient research cost has no partial consumption");

        FakePlayer unauthorized = player(level);
        ResearchTableMenu unauthorizedMenu = open(unauthorized);
        unauthorized.getInventory().setItem(0, new ItemStack(Items.COPPER_INGOT, 2));
        unauthorized.getInventory().setItem(1, new ItemStack(Items.PAPER));
        check(!unauthorizedMenu.clickMenuButton(unauthorized, 1), "missing prerequisite blocks dependent research");
        check(count(unauthorized, Items.COPPER_INGOT) == 2 && count(unauthorized, Items.PAPER) == 1,
                "unauthorized research does not consume resources");
        check(!WorldResearchData.get(level).knows(unauthorized, ResearchIds.of("conductivity")),
                "players retain independent research progress");

        FakePlayer other = player(level);
        check(!menu.clickMenuButton(other, 0), "another player cannot use the owner's menu");
        other.containerMenu = menu;
        check(!menu.clickMenuButton(other, 0), "setting another player's menu reference does not transfer ownership");

        inventory.clearContent();
        inventory.setItem(0, new ItemStack(Items.BLACK_DYE));
        check(menu.clickMenuButton(owner, ResearchTableMenu.REPORT_DEPOSIT), "first deposit command is valid");
        inventory.setItem(0, new ItemStack(Items.PAPER));
        check(menu.clickMenuButton(owner, ResearchTableMenu.REPORT_DEPOSIT + 2), "last deposit command is valid");
        check(menu.supply(0) == 1 && menu.supply(2) == 1, "boundary deposits update the matching supplies");
        check(menu.clickMenuButton(owner, ResearchTableMenu.REPORT_WITHDRAW), "first withdrawal command is valid");
        check(menu.clickMenuButton(owner, ResearchTableMenu.REPORT_WRITE - 1), "last withdrawal command is valid");
        check(count(owner, Items.BLACK_DYE) == 1 && count(owner, Items.PAPER) == 1,
                "boundary withdrawals return supplies to the player");

        inventory.setItem(0, new ItemStack(Items.BLACK_DYE));
        inventory.setItem(1, new ItemStack(Items.FEATHER));
        inventory.setItem(2, new ItemStack(Items.PAPER));
        check(menu.clickMenuButton(owner, ResearchTableMenu.REPORT_DEPOSIT), "deposit report ink");
        check(menu.clickMenuButton(owner, ResearchTableMenu.REPORT_DEPOSIT + 1), "deposit report feather");
        check(menu.clickMenuButton(owner, ResearchTableMenu.REPORT_DEPOSIT + 2), "deposit report paper");

        ResearchTableMenu unauthorizedReport = open(unauthorized);
        check(!unauthorizedReport.clickMenuButton(unauthorized, ResearchTableMenu.REPORT_WRITE),
                "player without research cannot write its report");
        check(unauthorizedReport.supply(0) == 1 && unauthorizedReport.supply(1) == 1
                && unauthorizedReport.supply(2) == 1, "invalid report leaves all supplies untouched");

        owner.containerMenu = menu;
        check(menu.clickMenuButton(owner, ResearchTableMenu.REPORT_WRITE),
                "valid research report can be written");
        check(count(owner, ModItems.RESEARCH_DATA.get()) == 1, "written report reaches player inventory");

        owner.setPos(TABLE.getX() + 20, TABLE.getY(), TABLE.getZ());
        check(!menu.clickMenuButton(owner, ResearchTableMenu.REPORT_DEPOSIT), "distant player command rejected");
        owner.setPos(TABLE.getX() + .5, TABLE.getY(), TABLE.getZ() + .5);
        owner.containerMenu = owner.inventoryMenu;
        check(!menu.clickMenuButton(owner, ResearchTableMenu.REPORT_DEPOSIT), "closed menu command rejected");
        owner.containerMenu = menu;
        level.setBlock(TABLE, Blocks.AIR.defaultBlockState(), 3);
        check(!menu.clickMenuButton(owner, ResearchTableMenu.REPORT_DEPOSIT), "destroyed-table command rejected");
        level.setBlock(TABLE, ModBlocks.RESEARCH_TABLE.get().defaultBlockState(), 3);
    }

    private static void verifyCraftingGate(ServerLevel level) {
        CraftingInput input = CraftingInput.of(3, 1, List.of(
                new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.COPPER_INGOT),
                new ItemStack(Items.COPPER_INGOT)));
        FakePlayer player = player(level);
        WorldResearchData research = WorldResearchData.get(level);
        check(!copperWireRecipe().matches(input, level), "automated crafting without a player is denied");
        check(!withCrafterMatches(player, input, level), "player without research is denied");
        check(research.learn(player, "material_observation"), "observe before conductivity");
        AssayProfile.of(player).complete(AssayCatalog.Material.COPPER, AssayCatalog.Method.INSPECTION);
        AssayProfile.of(player).complete(AssayCatalog.Material.COPPER, AssayCatalog.Method.MECHANICAL);
        check(research.learn(player, ResearchIds.of("conductivity")), "test grants first prerequisite");
        check(research.learn(player, ResearchIds.of("copper_wire")), "test grants second prerequisite");
        check(withCrafterMatches(player, input, level), "authorized manual crafting is allowed");

        FakePlayer other = player(level);
        check(!withCrafterMatches(other, input, level), "another player's progress does not authorize crafting");
        check(!copperWireRecipe().matches(input, level), "crafting context is removed after operation");
        try {
            ResearchCraftingAccess.withCrafter(player, () -> { throw new ExpectedFailure(); });
        } catch (ExpectedFailure expected) {
            checks++;
        }
        check(!copperWireRecipe().matches(input, level), "crafting context is removed after exceptions");
    }

    private static boolean withCrafterMatches(FakePlayer player, CraftingInput input, ServerLevel level) {
        boolean[] matches = {false};
        ResearchCraftingAccess.withCrafter(player, () -> matches[0] = copperWireRecipe().matches(input, level));
        return matches[0];
    }

    private static FoundationRecipe copperWireRecipe() {
        try {
            var constructor = FoundationRecipe.class.getDeclaredConstructor(String.class);
            constructor.setAccessible(true);
            return constructor.newInstance("copper_wire");
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Cannot instantiate copper wire recipe", exception);
        }
    }

    private static final class ExpectedFailure extends RuntimeException {}
}
