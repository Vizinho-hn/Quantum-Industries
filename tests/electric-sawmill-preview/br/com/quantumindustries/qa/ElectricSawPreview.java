package br.com.quantumindustries.qa;

import br.com.quantumindustries.block.entity.ElectricSawmillBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.research.WorldResearchData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = "quantumindustries")
public final class ElectricSawPreview {
  public static final BlockPos POS = new BlockPos(8, 110, 8);
  public static volatile int stage = 1, applied;
  public static volatile boolean ready;
  public static volatile String failure;
  private static int ticks;

  @SubscribeEvent
  public static void feed(net.neoforged.neoforge.event.tick.ServerTickEvent.Pre e) {
    if (!ready || !Boolean.getBoolean("quantumindustries.previewElectricSaw")) return;
    var l = e.getServer().overworld();
    var wheel = POS.west(4);
    l.setBlock(wheel.south().above(), Blocks.WATER.defaultBlockState(), 2);
    l.setBlock(
        wheel.south(),
        Blocks.WATER
            .defaultBlockState()
            .setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, 8),
        2);
    br.com.quantumindustries.mechanical.MechanicalNetwork.invalidate(l);
  }

  @SubscribeEvent
  public static void tick(PlayerTickEvent.Post e) {
    if (!Boolean.getBoolean("quantumindustries.previewElectricSaw")
        || !(e.getEntity() instanceof ServerPlayer p)
        || failure != null) return;
    try {
      var l = p.level();
      if (++ticks < 60) return;
      if (!ready) {
        for (int x = 4; x <= 13; x++)
          for (int y = 109; y <= 113; y++)
            for (int z = 3; z <= 13; z++)
              l.setBlock(
                  new BlockPos(x, y, z),
                  (y == 109 ? Blocks.STONE : Blocks.AIR).defaultBlockState(),
                  3);
        p.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
        p.connection.teleport(11.5, 110, 4.5, 40, 14);
        WorldResearchData.get(l).unlockAll(p);
        l.setBlock(POS, ModBlocks.ELECTRIC_SAWMILL.get().defaultBlockState(), 3);
        if (ticks == 80)
          for (var old :
              l.getEntitiesOfClass(
                  net.minecraft.world.entity.item.ItemEntity.class,
                  new net.minecraft.world.phys.AABB(POS).inflate(20))) old.discard();
        var s = (ElectricSawmillBlockEntity) l.getBlockEntity(POS);
        s.bind(p);
        Item[] woods = {Items.OAK_LOG, Items.SPRUCE_LOG, Items.BIRCH_LOG, Items.ACACIA_LOG};
        for (int i = 0; i < 4; i++) s.inventory().setItem(i, new ItemStack(woods[i], 32));
        var shaft = POS.west(3);
        var wheel = POS.west(4);
        var dynamo = POS.west(2);
        var cable = POS.west();
        l.setBlock(
            wheel,
            ModBlocks.PRIMITIVE_WATER_WHEEL
                .get()
                .defaultBlockState()
                .setValue(
                    net.minecraft.world.level.block.state.properties.BlockStateProperties
                        .HORIZONTAL_FACING,
                    net.minecraft.core.Direction.EAST),
            3);
        l.setBlock(
            shaft,
            ModBlocks.MECHANICAL_SHAFT_BLOCK
                .get()
                .defaultBlockState()
                .setValue(
                    net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS,
                    net.minecraft.core.Direction.Axis.X),
            3);
        l.setBlock(
            dynamo,
            ModBlocks.MANUAL_DYNAMO
                .get()
                .defaultBlockState()
                .setValue(
                    net.minecraft.world.level.block.state.properties.BlockStateProperties
                        .HORIZONTAL_FACING,
                    net.minecraft.core.Direction.SOUTH),
            3);
        if (!((br.com.quantumindustries.block.entity.ManualDynamoBlockEntity)
                l.getBlockEntity(dynamo))
            .configureMechanicalDrive(p)) throw new AssertionError("dynamo configuration");
        l.setBlock(cable, ModBlocks.INSULATED_CONDUIT.get().defaultBlockState(), 3);
        l.setBlock(POS.north(), ModBlocks.PNEUMATIC_TUBE.get().defaultBlockState(), 3);
        l.setBlock(POS.north(2), Blocks.CHEST.defaultBlockState(), 3);
        ((net.minecraft.world.level.block.entity.ChestBlockEntity) l.getBlockEntity(POS.north(2)))
            .setItem(0, new ItemStack(Items.OAK_LOG, 32));
        l.setBlock(POS.south(), ModBlocks.PNEUMATIC_TUBE.get().defaultBlockState(), 3);
        l.setBlock(POS.south(2), Blocks.CHEST.defaultBlockState(), 3);
        for (var pos : java.util.List.of(POS.north(), POS.south())) {
          var tube =
              (br.com.quantumindustries.block.entity.PneumaticTubeBlockEntity)
                  l.getBlockEntity(pos);
          for (var dir : net.minecraft.core.Direction.values())
            tube.connectionModes().set(dir, br.com.quantumindustries.logistics.ConnectionMode.NONE);
          tube.connectionModes()
              .set(
                  net.minecraft.core.Direction.NORTH,
                  br.com.quantumindustries.logistics.ConnectionMode.PULL);
          tube.connectionModes()
              .set(
                  net.minecraft.core.Direction.SOUTH,
                  br.com.quantumindustries.logistics.ConnectionMode.PUSH);
          tube.connectionChanged();
        }
        ready = true;
      }

      if (ticks == 80)
        for (var old :
            l.getEntitiesOfClass(
                net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(POS).inflate(20))) old.discard();
      var s = (ElectricSawmillBlockEntity) l.getBlockEntity(POS);

      if (applied != stage) {
        applied = stage;
        if (stage == 5) {
          p.closeContainer();
          p.setItemSlot(
              net.minecraft.world.entity.EquipmentSlot.HEAD,
              new ItemStack(br.com.quantumindustries.ModItems.ENERGY_INSPECTION_GOGGLES.get()));
          p.connection.teleport(11.2, 110, 8.5, 90, 20);
        }
        if (stage >= 2 && stage <= 4 || stage == 6 || stage == 10) {
          if (stage == 3) {
            var source =
                (net.minecraft.world.level.block.entity.ChestBlockEntity)
                    l.getBlockEntity(POS.north(2));
            var dest =
                (net.minecraft.world.level.block.entity.ChestBlockEntity)
                    l.getBlockEntity(POS.south(2));
            if (source.getItem(0).getCount() >= 32 || dest.getItem(0).isEmpty())
              throw new AssertionError("full generator/tube pipeline did not deliver planks");
            var wire =
                (br.com.quantumindustries.block.entity.CopperCableBlockEntity)
                    l.getBlockEntity(POS.west());
            wire.connectionModes()
                .set(
                    net.minecraft.core.Direction.EAST,
                    br.com.quantumindustries.logistics.ConnectionMode.NONE);
            wire.connectionChanged();
            var tag = s.saveWithFullMetadata(l.registryAccess());
            tag.putInt("stored_fe", 0);
            s.loadWithComponents(
                net.minecraft.world.level.storage.TagValueInput.create(
                    net.minecraft.util.ProblemReporter.DISCARDING, l.registryAccess(), tag));
          }
          if (stage == 4)
            for (int i = 4; i < 8; i++) s.inventory().setItem(i, new ItemStack(Items.STONE, 64));
          p.openMenu(
              new net.minecraft.world.SimpleMenuProvider(
                  (id, inv, who) ->
                      new br.com.quantumindustries.menu.ElectricSawmillMenu(id, inv, POS),
                  net.minecraft.network.chat.Component.translatable(
                      "block.quantumindustries.electric_sawmill")),
              POS);
        }
      }
    } catch (Throwable t) {
      failure = t.toString();
    }
  }
}
