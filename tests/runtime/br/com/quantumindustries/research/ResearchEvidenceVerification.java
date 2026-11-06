package br.com.quantumindustries.research;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.assay.AssayCatalog.Material;
import br.com.quantumindustries.assay.AssayCatalog.Method;
import br.com.quantumindustries.assay.AssayProfile;
import br.com.quantumindustries.block.entity.ResearchTableBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.menu.ResearchTableMenu;
import br.com.quantumindustries.recipe.ResearchCraftingAccess;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;

public final class ResearchEvidenceVerification {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void run(ServerLevel level) {
        LegacyResearchCheck.main(new String[0]);
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "ScienceQA"));
        var other = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "ScienceGuest"));
        var research = WorldResearchData.get(level);
        for (var study : ResearchCatalog.STUDIES) {
            check(research.canLearn(player, study.id()) == study.key().equals("material_observation"),
                    "only observation is initially researchable");
        }
        var pos = new BlockPos(3, 100, 2);
        player.setPos(3.5, 100, 2.5);
        level.setBlock(pos, ModBlocks.RESEARCH_TABLE_TIER_1.get().defaultBlockState(), 3);
        var entity = (ResearchTableBlockEntity) level.getBlockEntity(pos);
        var menu = new ResearchTableMenu(12, player.getInventory(), entity,
                new SimpleContainerData(ResearchTableMenu.DATA_SIZE));
        player.containerMenu = menu;
        int observation = ResearchCatalog.indexOf("material_observation");
        int malleability = ResearchCatalog.indexOf("material_malleability");
        int conductivity = ResearchCatalog.indexOf("conductivity");
        var analyzerId = ResearchIds.of("empirical_assay_bench");
        var analyzerKey = ResourceKey.create(Registries.RECIPE, analyzerId);
        var recipe = level.getServer().getRecipeManager().byKey(analyzerKey).orElseThrow();
        var grid = CraftingInput.of(3, 3, List.of(
                new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_NUGGET), new ItemStack(Items.IRON_INGOT),
                new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.OAK_PLANKS),
                new ItemStack(Items.OAK_LOG), ItemStack.EMPTY, new ItemStack(Items.OAK_LOG)));
        ResearchCraftingAccess.withCrafter(player, () -> check(
                !((net.minecraft.world.item.crafting.CraftingRecipe) recipe.value()).matches(grid, level),
                "analyzer crafting blocked before observation"));
        check(!player.getRecipeBook().contains(analyzerKey), "analyzer absent from initial recipe book");
        for (var cost : ResearchCatalog.STUDIES.get(observation).costs()) {
            player.getInventory().add(new ItemStack(cost.item(), cost.amount()));
        }
        check(menu.clickMenuButton(player, observation), "starter research uses accessible vanilla costs");
        check(player.getRecipeBook().contains(analyzerKey), "observation awards analyzer recipe");
        ResearchCraftingAccess.withCrafter(player, () -> check(
                ((net.minecraft.world.item.crafting.CraftingRecipe) recipe.value()).matches(grid, level),
                "analyzer crafting allowed after observation"));
        check(!other.getRecipeBook().contains(analyzerKey), "other player does not gain analyzer recipe");

        player.getInventory().add(new ItemStack(Items.COPPER_INGOT));
        player.getInventory().add(new ItemStack(Items.STONE));
        check(!menu.evidenceReady(malleability), "menu reports missing mechanical discovery");
        check(!menu.clickMenuButton(player, malleability), "materials cannot bypass science");
        check(player.getInventory().countItem(Items.COPPER_INGOT) == 1
                && player.getInventory().countItem(Items.STONE) == 1, "failed science leaves costs intact");
        check(!research.learn(player, "material_malleability"), "direct learning also validates evidence");
        var profile = AssayProfile.of(player);
        profile.complete(Material.COPPER, Method.HEATING);
        profile.complete(Material.IRON, Method.MECHANICAL);
        check(!menu.evidenceReady(malleability), "wrong material or method is insufficient");
        profile.complete(Material.COPPER, Method.MECHANICAL);
        check(menu.evidenceReady(malleability) && menu.clickMenuButton(player, malleability),
                "existing discovery satisfies the menu automatically");
        check(profile.knows(Material.COPPER, Method.MECHANICAL), "research never consumes discovery");
        check(!research.canLearn(player, ResearchIds.of("conductivity")), "copper mechanics is not inspection");
        profile.complete(Material.COPPER, Method.INSPECTION);
        check(research.canLearn(player, ResearchIds.of("conductivity")), "copper familiarity unlocks conductivity research");
        check(research.learn(player, "conductivity") && research.learn(player, "copper_wire")
                && research.learn(player, "copper_winding"), "one mechanical discovery is reused by multiple studies");

        check(research.learn(player, "manual_forming"), "manual forming reuses prior science");
        check(research.canLearn(player, ResearchIds.of("metal_sheets"))
                && research.canLearn(player, ResearchIds.of("basic_mechanics")),
                "iron mechanics unlocks sheet and mechanism research");
        check(!research.canLearn(player, ResearchIds.of("basic_magnetism")), "iron mechanics is not iron familiarity");
        var cumulative = new ResearchCatalog.Study("test", "test", "", "", Items.PAPER, 1,
                List.of(), List.of(), List.of(
                        new ResearchEvidence.Assay(Material.COPPER, Method.MECHANICAL),
                        new ResearchEvidence.Assay(Material.IRON, Method.INSPECTION)));
        check(!research.evidenceSatisfied(player, cumulative), "cumulative science requires every discovery");
        profile.complete(Material.IRON, Method.INSPECTION);
        check(research.evidenceSatisfied(player, cumulative), "cumulative science accepts all discoveries");
        check(research.canLearn(player, ResearchIds.of("basic_magnetism")), "iron inspection enables magnetism research");
        var alternative = new ResearchEvidence.AnyOf(List.of(
                new ResearchEvidence.Assay(Material.COPPER, Method.MECHANICAL),
                new ResearchEvidence.Assay(Material.IRON, Method.INSPECTION)));
        check(alternative.satisfied(player) && !alternative.satisfied(other), "alternative evidence remains individual");
        check(!research.canLearn(player, ResearchIds.of("electrical_insulation")), "insulation awaits non-metal inspection");
        profile.complete(Material.QUARTZ, Method.INSPECTION);
        check(research.canLearn(player, ResearchIds.of("electrical_insulation")), "quartz inspection satisfies insulation AnyOf");

        var completed = new JsonObject();
        var ids = new JsonArray();
        ids.add("copper_wire");
        ids.add("material_malleability");
        var players = new JsonObject();
        players.add(other.getUUID().toString(), ids);
        completed.add("completed_research", players);
        var old = WorldResearchData.CODEC.parse(JsonOps.INSTANCE, completed).getOrThrow();
        check(old.knows(other, "copper_wire") && old.knows(other, "material_malleability"),
                "old learned technologies survive without new evidence");
        check(!old.canLearn(other, ResearchIds.of("manual_forming")),
                "future studies still require initial observation in a legacy save");
        var encoded = WorldResearchData.CODEC.encodeStart(JsonOps.INSTANCE, old).getOrThrow();
        var reloaded = WorldResearchData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        check(reloaded.knows(other, "copper_wire"), "old research survives save round-trip");
        var data = player.getPersistentData().getCompound(AssayProfile.KEY).orElseThrow().copy();
        var cloned = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "ScienceReload"));
        var file = java.nio.file.Path.of("..", "science-profile-" + UUID.randomUUID() + ".dat");
        try {
            net.minecraft.nbt.NbtIo.writeCompressed(data, file);
            var stored = net.minecraft.nbt.NbtIo.readCompressed(file, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            cloned.getPersistentData().put(AssayProfile.KEY, stored);
        } catch (java.io.IOException error) {
            throw new AssertionError("Could not reload the experimental profile from disk", error);
        } finally {
            try { java.nio.file.Files.deleteIfExists(file); }
            catch (java.io.IOException error) { throw new AssertionError(error); }
        }
        check(AssayProfile.of(cloned).knows(Material.COPPER, Method.MECHANICAL),
                "discovery keys survive player save round-trip");
        check(!AssayProfile.of(other).knows(Material.COPPER, Method.MECHANICAL),
                "shared table preserves individual discoveries");
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    private ResearchEvidenceVerification() {}
}
