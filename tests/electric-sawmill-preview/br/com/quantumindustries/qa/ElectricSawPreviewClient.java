package br.com.quantumindustries.qa;

import java.nio.file.*;
import net.minecraft.client.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = "quantumindustries", value = Dist.CLIENT)
public final class ElectricSawPreviewClient {
  static int total, age, stage = 1;
  static boolean hidden, done, captured;

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post e) {
    if (!Boolean.getBoolean("quantumindustries.previewElectricSaw") || done) return;
    var g = Minecraft.getInstance();
    try {
      if (!hidden) {
        org.lwjgl.glfw.GLFW.glfwHideWindow(g.getWindow().handle());
        Files.createDirectories(g.gameDirectory.toPath().resolve("screenshots"));
        hidden = true;
      }
      if (++total > 1400) throw new AssertionError("timeout");
      if (ElectricSawPreview.failure != null) throw new AssertionError(ElectricSawPreview.failure);
      if (g.player == null || !ElectricSawPreview.ready || ElectricSawPreview.applied != stage)
        return;
      if (stage == 7 && age == 0) {
        var m =
            new br.com.quantumindustries.menu.ResearchTableMenu(
                0,
                g.player.getInventory(),
                null,
                new net.minecraft.world.inventory.SimpleContainerData(
                    br.com.quantumindustries.menu.ResearchTableMenu.DATA_SIZE));
        for (int i = 0; i < br.com.quantumindustries.research.ResearchCatalog.STUDIES.size(); i++) {
          m.setData(3 + i * 2, 2);
          m.setData(4 + i * 2, 65535);
        }
        var screen =
            new br.com.quantumindustries.client.ResearchTableScreen(
                m,
                g.player.getInventory(),
                net.minecraft.network.chat.Component.literal("Scientific Analyzer - Tier 2"));
        g.setScreen(screen);
        for (String name : java.util.List.of("era", "selected")) {
          var f = screen.getClass().getDeclaredField(name);
          f.setAccessible(true);
          f.setInt(
              screen,
              name.equals("era")
                  ? 2
                  : br.com.quantumindustries.research.ResearchCatalog.indexOf(
                      "electric_wood_processing"));
        }
      }
      if (stage == 7 && age == 1) {
        var f = g.screen.getClass().getDeclaredField("tree");
        f.setAccessible(true);
        var tree = f.get(g.screen);
        var set = tree.getClass().getDeclaredMethod("setEra", int.class);
        set.setAccessible(true);
        set.invoke(tree, 2);
      }
      if (stage == 8 && age == 0) ElectricSawJeiPreview.show();
      if (stage == 9 && age == 0) {
        var m = new br.com.quantumindustries.menu.QuantumGuidebookMenu(0, g.player.getInventory());
        m.setData(
            br.com.quantumindustries.research.ResearchCatalog.indexOf("electric_wood_processing"),
            1);
        var chapters = br.com.quantumindustries.research.GuidebookCatalog.chapters();
        int index =
            java.util.stream.IntStream.range(0, chapters.size())
                .filter(i -> chapters.get(i).id().equals("electric_sawmill"))
                .findFirst()
                .orElseThrow();
        if (!m.isUnlocked(chapters.get(index))) throw new AssertionError("Codex chapter locked");
        var screen =
            new br.com.quantumindustries.client.QuantumGuidebookScreen(
                m,
                g.player.getInventory(),
                net.minecraft.network.chat.Component.literal("Codex QA"));
        for (String name : java.util.List.of("selectedIndex", "listPage")) {
          var f = screen.getClass().getDeclaredField(name);
          f.setAccessible(true);
          f.setInt(screen, name.equals("listPage") ? index / 8 : index);
        }
        g.setScreen(screen);
      }
      if (stage == 10 && age == 0) {
        g.options.guiScale().set(2);
        org.lwjgl.glfw.GLFW.glfwSetWindowSize(g.getWindow().handle(), 1280, 720);
        g.resizeGui();
      }
      if (stage == 6 && age == 0) {
        g.options.guiScale().set(3);
        g.resizeGui();
      }
      if (stage == 5 && age == 44) {
        var snap =
            br.com.quantumindustries.client.inspection.ClientInspectionCache.forPos(
                ElectricSawPreview.POS);
        if (snap == null) throw new AssertionError("goggles telemetry missing");
      }
      if (++age >= 45 && !captured) {
        if (stage <= 2
            && !((br.com.quantumindustries.block.entity.ElectricSawmillBlockEntity)
                    g.level.getBlockEntity(ElectricSawPreview.POS))
                .working()) return;
        if ((stage >= 2 && stage <= 4 || stage == 6 || stage == 10)
            && !(g.screen instanceof br.com.quantumindustries.client.gui.ElectricSawmillScreen))
          throw new AssertionError("real screen missing");
        captured = true;
        String name = String.format("%02d-electric-sawmill.png", stage);
        Screenshot.takeScreenshot(
            g.getMainRenderTarget(),
            im -> {
              try (im) {
                im.writeToFile(g.gameDirectory.toPath().resolve("screenshots").resolve(name));
              } catch (Exception x) {
                throw new RuntimeException(x);
              }
            });
      }
      if (age >= 60 && captured) {
        if (stage == 2) {
          if (age < 160) return;
        }
        if (stage < 10) {
          stage++;
          age = 0;
          captured = false;
          ElectricSawPreview.stage = stage;
        } else {
          Files.writeString(
              g.gameDirectory.toPath().resolve("result.txt"),
              "PASS: ten native captures; 1600x1000 and 1280x720; hydraulic FE and item-tube"
                  + " pipeline; goggles and GUI scale 3\n");
          done = true;
          g.stop();
        }
      }
    } catch (Throwable t) {
      t.printStackTrace();
      try {
        Files.writeString(g.gameDirectory.toPath().resolve("result.txt"), "FAIL: " + t);
      } catch (Exception ignored) {
      }
      done = true;
      g.stop();
    }
  }
}
