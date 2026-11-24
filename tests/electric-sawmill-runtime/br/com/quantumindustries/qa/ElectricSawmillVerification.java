package br.com.quantumindustries.qa;

import br.com.quantumindustries.block.entity.*;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.logistics.ItemRouting;
import br.com.quantumindustries.recipe.MechanicalProcesses;
import br.com.quantumindustries.research.WorldResearchData;
import com.mojang.authlib.GameProfile;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = "quantumindustries")
public final class ElectricSawmillVerification {
  static int checks, readTicks, chunkTicks, unloadedAt;
  static boolean awaitingChunk;
  static ElectricSawmillBlockEntity remote;
  static final BlockPos FAR = new BlockPos(2048, 110, 2048);
  static boolean awaitingDrops;
  static ElectricSawmillBlockEntity restartSaw;

  @SubscribeEvent
  public static void after(net.neoforged.neoforge.event.tick.ServerTickEvent.Post e)
      throws Exception {
    if (awaitingChunk) {
      var l = e.getServer().overworld();
      try {
        if (++chunkTicks > 1200) throw new AssertionError("remote chunk unload timeout");
        if (unloadedAt == 0 && !l.hasChunkAt(FAR)) unloadedAt = chunkTicks;
        if (unloadedAt == 0 || chunkTicks - unloadedAt < 80) return;
        check(!l.hasChunkAt(FAR), "remote chunk remains unloaded without forced loads");
        l.setChunkForced(128, 128, true);
        l.getChunkAt(FAR);
        var restored = (ElectricSawmillBlockEntity) l.getBlockEntity(FAR);
        check(
            restored != remote && !restored.isRemoved(),
            "new entity after actual chunk unload/reload");
        check(
            restored.progress() == 5
                && restored.energy() == 0
                && restored.inventory().getItem(0).getCount() == 2,
            "chunk reload preserves reserved progress/input/FE");
        charge(restored, 140);
        for (int i = 0; i < 35; i++) restored.tick();
        check(
            restored.previewOutput().getCount() == 8
                && restored.energy() == 0
                && restored.inventory().getItem(0).getCount() == 1,
            "chunk reload resumes exactly once");
        Files.writeString(
            Path.of("../result-chunk.txt"), "PASS: " + checks + " chunk reload checks\n");
      } catch (Throwable t) {
        Files.writeString(Path.of("../result-chunk.txt"), "FAIL: " + t);
      }
      awaitingChunk = false;
      e.getServer().halt(false);
      return;
    }
    if (!awaitingDrops) return;
    var l = e.getServer().overworld();
    var p = new BlockPos(8, 110, 8);
    try {
      if (++readTicks == 40) {
        for (var old :
            l.getEntitiesOfClass(
                net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(p).inflate(3))) old.discard();
        charge(restartSaw, 160);
        for (int t = 0; t < 30; t++) restartSaw.tick();
        check(
            restartSaw.inventory().getItem(0).isEmpty()
                && restartSaw.previewOutput().getCount() == 8
                && restartSaw.energy() == 40,
            "restart completes once at exact remaining cost");
        l.destroyBlock(p, true);
        check(restartSaw.inventory().getItem(4).isEmpty(), "destruction clears stored output");
        restartSaw.preRemoveSideEffects(p, restartSaw.getBlockState());
      }
      if (readTicks < 60) return;
      System.out.println(
          "SAW_DROPS "
              + l
                  .getEntitiesOfClass(
                      net.minecraft.world.entity.item.ItemEntity.class,
                      new net.minecraft.world.phys.AABB(p).inflate(120))
                  .stream()
                  .map(it -> it.getItem() + " at " + it.position())
                  .toList());
      int count =
          l
              .getEntitiesOfClass(
                  net.minecraft.world.entity.item.ItemEntity.class,
                  new net.minecraft.world.phys.AABB(p).inflate(3))
              .stream()
              .filter(it -> it.getItem().is(Items.OAK_PLANKS))
              .mapToInt(it -> it.getItem().getCount())
              .sum();
      check(count == 8, "breaking returns output exactly once: " + count);
      Files.writeString(
          Path.of("../result-read.txt"), "PASS: " + checks + " restart/drop checks\n");
    } catch (Throwable t) {
      Files.writeString(Path.of("../result-read.txt"), "FAIL: " + t);
    }
    awaitingDrops = false;
    e.getServer().halt(false);
  }

  static void check(boolean b, String m) {
    checks++;
    if (!b) throw new AssertionError(m);
  }

  static void charge(ElectricSawmillBlockEntity s, int n) {
    var l = (ServerLevel) s.getLevel();
    var tag = s.saveWithFullMetadata(l.registryAccess());
    tag.putInt("stored_fe", n);
    s.loadWithComponents(
        net.minecraft.world.level.storage.TagValueInput.create(
            net.minecraft.util.ProblemReporter.DISCARDING, l.registryAccess(), tag));
  }

  @SubscribeEvent
  public static void start(ServerStartedEvent e) throws Exception {
    if (!Boolean.getBoolean("quantumindustries.verifyElectricSawmill")) return;
    try {
      ServerLevel l = e.getServer().overworld();
      BlockPos p = new BlockPos(8, 110, 8);
      l.getChunk(0, 0);
      for (int x = -3; x <= 3; x++)
        for (int z = -3; z <= 3; z++)
          l.setBlock(p.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
      if (System.getProperty("quantumindustries.electricSawPhase", "write").equals("chunk")) {
        l.setChunkForced(128, 128, true);
        l.getChunkAt(FAR);
        l.setBlock(FAR, Blocks.AIR.defaultBlockState(), 3);
        l.setBlock(FAR, ModBlocks.ELECTRIC_SAWMILL.get().defaultBlockState(), 3);
        remote = (ElectricSawmillBlockEntity) l.getBlockEntity(FAR);
        var op = new FakePlayer(l, new GameProfile(UUID.randomUUID(), "SawChunkQA"));
        op.setPos(2048, 110, 2049);
        WorldResearchData.get(l).unlockAll(op);
        check(remote.bind(op), "chunk fixture binds");
        remote.inventory().setItem(0, new ItemStack(Items.OAK_LOG, 2));
        charge(remote, 20);
        for (int i = 0; i < 5; i++) remote.tick();
        l.setChunkForced(128, 128, false);
        awaitingChunk = true;
        return;
      }
      if (System.getProperty("quantumindustries.electricSawPhase", "write").equals("read")) {
        var saved = (ElectricSawmillBlockEntity) l.getBlockEntity(p);
        check(saved != null, "machine survives full restart");
        check(
            saved.progress() == 10 && saved.energy() == 160,
            "partial work and energy survive full restart");
        check(saved.inventory().getItem(0).getCount() == 1, "reserved input survives full restart");
        restartSaw = saved;
        charge(saved, 0);
        l.setChunkForced(0, 0, true);
        awaitingDrops = true;
        return;
      }
      for (var old :
          l.getEntitiesOfClass(
              net.minecraft.world.entity.item.ItemEntity.class,
              new net.minecraft.world.phys.AABB(p).inflate(3))) old.discard();
      l.setBlock(p, ModBlocks.ELECTRIC_SAWMILL.get().defaultBlockState(), 3);
      var s = (ElectricSawmillBlockEntity) l.getBlockEntity(p);
      var user = new FakePlayer(l, new GameProfile(UUID.randomUUID(), "ElectricSawQA"));
      user.setPos(8, 110, 9);
      var craft =
          (br.com.quantumindustries.recipe.FoundationRecipe)
              l.getServer().getRecipeManager().getRecipes().stream()
                  .filter(
                      h ->
                          h.id()
                              .identifier()
                              .toString()
                              .equals("quantumindustries:electric_sawmill"))
                  .findFirst()
                  .orElseThrow()
                  .value();
      check(s.receiveEnergy(32) == 32, "first receive within tick");
      check(s.receiveEnergy(32) == 32, "second receive within tick");
      check(s.receiveEnergy(1) == 0, "combined receive bounded to 64 FE per tick");
      check(!s.bind(user), "research required");
      check(!craft.unlocked(user), "craft locked before research");
      WorldResearchData.get(l).unlockAll(user);
      check(s.bind(user), "authorized binding");
      check(craft.unlocked(user), "craft unlocked after research");
      var preview =
          br.com.quantumindustries.recipe.FoundationRecipe.previews(
                  br.com.quantumindustries.research.ResearchCatalog.indexOf(
                      "electric_wood_processing"))
              .stream()
              .filter(v -> v.output().is(br.com.quantumindustries.ModItems.ELECTRIC_SAWMILL.get()))
              .findFirst()
              .orElseThrow();
      var table = p.north();
      l.setBlock(table, Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
      var crafting =
          new net.minecraft.world.inventory.CraftingMenu(
              8,
              user.getInventory(),
              net.minecraft.world.inventory.ContainerLevelAccess.create(l, table));
      user.containerMenu = crafting;
      int ci = 0;
      for (var ingredient : preview.ingredients()) {
        int x = ci % preview.width(), y = ci / preview.width();
        ci++;
        crafting.getSlot(1 + y * 3 + x).set(ingredient.copy());
      }
      crafting.slotsChanged(crafting.getInputGridSlots().getFirst().container);
      check(
          crafting
              .getSlot(0)
              .getItem()
              .is(br.com.quantumindustries.ModItems.ELECTRIC_SAWMILL.get()),
          "real server crafting result");
      check(crafting.getSlot(0).safeTake(1, 1, user).getCount() == 1, "craft returns one machine");
      check(
          crafting.getInputGridSlots().stream().allMatch(slot -> slot.getItem().isEmpty()),
          "all nine ingredients consumed once");
      user.containerMenu = user.inventoryMenu;

      check(ItemRouting.isEndpoint(l, p), "transport endpoint");
      var types = List.of(Items.OAK_LOG, Items.SPRUCE_LOG, Items.BIRCH_LOG, Items.ACACIA_LOG);
      for (int i = 0; i < 4; i++) s.inventory().setItem(i, new ItemStack(types.get(i), 2));
      charge(s, 1200);
      for (int k = 0; k < 4; k++) {
        int old = s.energy();
        for (int t = 0; t < 40; t++) s.tick();
        check(old - s.energy() == 160, "exact cycle energy");
        check(s.inventory().getItem(k).getCount() == 1, "round robin");
        check(s.inventory().getItem(4 + k).getCount() == 8, "species output");
      }
      var other = new FakePlayer(l, new GameProfile(UUID.randomUUID(), "SecondSawQA"));
      other.setPos(8, 110, 9);
      WorldResearchData.get(l).unlockAll(other);
      var firstMenu =
          new br.com.quantumindustries.menu.ElectricSawmillMenu(1, user.getInventory(), p);
      var secondMenu =
          new br.com.quantumindustries.menu.ElectricSawmillMenu(2, other.getInventory(), p);
      check(
          firstMenu.quickMoveStack(user, 4).getCount() == 8,
          "first viewer shift extracts real output");
      check(
          secondMenu.quickMoveStack(other, 4).isEmpty(),
          "second viewer cannot duplicate extracted output");
      user.getInventory().setItem(0, new ItemStack(Items.OAK_LOG));
      check(!firstMenu.quickMoveStack(user, 35).isEmpty(), "shift inserts compatible wood");
      int before = s.energy();
      for (int i = 4; i < 8; i++) s.inventory().setItem(i, new ItemStack(Items.STONE, 64));
      for (int t = 0; t < 10; t++) s.tick();
      check(s.energy() == before, "blocked output consumes no energy");
      for (int i = 4; i < 8; i++) s.inventory().setItem(i, ItemStack.EMPTY);
      for (int i = 0; i < 4; i++) s.inventory().setItem(i, ItemStack.EMPTY);
      s.inventory().setItem(0, new ItemStack(Items.OAK_LOG, 2));
      for (int t = 0; t < 15; t++) s.tick();
      int progress = s.progress();
      s.inventory().removeItem(0, 1);
      check(s.progress() == 0, "removing active item resets progress");
      // All data-driven mechanical species and stripped variants remain accepted.
      for (var recipe : MechanicalProcesses.all(l))
        for (var item : recipe.value().inputs())
          check(s.canInsert(new ItemStack(item)), "recipe variant accepted");
      check(!s.canInsert(new ItemStack(Items.BAMBOO)), "bamboo is not a log");
      check(!s.canInsert(new ItemStack(Items.DIRT)), "invalid input rejected");
      var tag = s.saveWithFullMetadata(l.registryAccess());
      var restored = new ElectricSawmillBlockEntity(p, l.getBlockState(p));
      restored.setLevel(l);
      restored.loadWithComponents(
          net.minecraft.world.level.storage.TagValueInput.create(
              net.minecraft.util.ProblemReporter.DISCARDING, l.registryAccess(), tag));
      check(
          restored.energy() == s.energy() && restored.inventory().getItem(0).getCount() == 1,
          "save-load preserves energy and inventory");
      var pauseTag = restored.saveWithFullMetadata(l.registryAccess());
      pauseTag.putInt("stored_fe", 6);
      pauseTag.putInt("active", -1);
      pauseTag.putInt("progress", 0);
      restored.loadWithComponents(
          net.minecraft.world.level.storage.TagValueInput.create(
              net.minecraft.util.ProblemReporter.DISCARDING, l.registryAccess(), pauseTag));
      restored.tick();
      int paused = restored.progress();
      restored.tick();
      check(
          restored.progress() == paused && restored.energy() == 2,
          "insufficient energy pauses without partial payment");
      restored.receiveEnergy(2);
      restored.tick();
      check(
          restored.progress() == paused + 1 && restored.energy() == 0,
          "energy restoration resumes paid progress");
      for (int i = 0; i < 36; i++) user.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
      user.getInventory().setItem(0, new ItemStack(Items.OAK_LOG, 63));
      s.inventory().setItem(0, new ItemStack(Items.OAK_LOG, 2));
      charge(s, 200);
      for (int i = 0; i < 5; i++) s.tick();
      check(
          firstMenu.quickMoveStack(user, 0).getCount() == 2
              && s.inventory().getItem(0).getCount() == 1
              && s.progress() == 0,
          "partial shift withdrawal resets reserved work");
      for (int i = 0; i < 8; i++) s.inventory().setItem(i, ItemStack.EMPTY);
      s.inventory().setItem(0, new ItemStack(Items.OAK_LOG));
      charge(s, 200);
      for (int t = 0; t < 10; t++) s.tick();
      check(s.progress() == 10, "restart fixture has partial paid work");
      Files.writeString(Path.of("../result.txt"), "PASS: " + checks + " electric sawmill checks\n");
    } catch (Throwable t) {
      t.printStackTrace();
      Files.writeString(Path.of("../result.txt"), "FAIL: " + t + "\n");
    }
    e.getServer().halt(false);
  }
}
