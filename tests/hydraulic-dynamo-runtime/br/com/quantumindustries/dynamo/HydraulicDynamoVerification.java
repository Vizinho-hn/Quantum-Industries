package br.com.quantumindustries.dynamo;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.block.entity.ManualDynamoBlockEntity;
import br.com.quantumindustries.block.entity.MechanicalSawmillBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.mechanical.MechanicalNetwork;
import br.com.quantumindustries.research.WorldResearchData;
import com.mojang.authlib.GameProfile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = "quantumindustries")
public final class HydraulicDynamoVerification {
    private static final BlockPos DYNAMO = new BlockPos(8, 100, 8);
    private static final BlockPos SHAFT = DYNAMO.west();
    private static final BlockPos WHEEL = DYNAMO.west(2);
    private static final UUID OPERATOR = UUID.fromString("d18294eb-aef7-4dc8-81cf-32f12cfc3412");
    private static ServerLevel level;
    private static ManualDynamoBlockEntity dynamo;
    private static FakePlayer player;
    private static int ticks, checks, stage, previousEnergy, serverTicks;
    private static boolean active, waterEnabled = true;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    @SubscribeEvent public static void start(ServerStartedEvent event) throws Exception {
        if (!Boolean.getBoolean("quantumindustries.verifyHydraulicDynamo")) return;
        level = event.getServer().overworld();
        level.setChunkForced(0, 0, true);
        player = new FakePlayer(level, new GameProfile(OPERATOR, "HydraulicDynamoQA"));
        player.setPos(8.5, 100, 9.5);
        try {
            if (Files.exists(Path.of("../restart-ready.txt"))) {
                dynamo = (ManualDynamoBlockEntity) level.getBlockEntity(DYNAMO);
                previousEnergy = Integer.parseInt(Files.readString(Path.of("../restart-ready.txt")));
                check(dynamo.energy() == previousEnergy, "restart preserves stored energy");
                check(dynamo.requestedLoad() == 6, "restart preserves the authorized mechanical operator");
                stage = 10;
            } else {
                WorldResearchData.get(level).clearAll(player);
                level.setBlock(DYNAMO, Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(DYNAMO, ModBlocks.MANUAL_DYNAMO.get().defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH), 3);
                dynamo = (ManualDynamoBlockEntity) level.getBlockEntity(DYNAMO);
                level.setBlock(WHEEL, ModBlocks.PRIMITIVE_WATER_WHEEL.get().defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST), 3);
                shaft(Direction.Axis.X);
                water(true);
                check(!dynamo.configureMechanicalDrive(player), "unknown operator cannot authorize generation");
                check(dynamo.requestedLoad() == 0, "unknown operator adds no load");
            }
            active = true;
        } catch (Throwable error) { finish(error); }
    }

    private static void shaft(Direction.Axis axis) {
        level.setBlock(SHAFT, ModBlocks.MECHANICAL_SHAFT_BLOCK.get().defaultBlockState()
                .setValue(BlockStateProperties.AXIS, axis), 3);
        MechanicalNetwork.invalidate(level);
    }

    // Keep the fixture deterministic under Minecraft fluid ticks, as in QksRuntimeVerification.
    // A falling lateral contact has a nonzero tangential torque; a falling block below
    // the wheel may instead exert radial force or be replaced by a scheduled fluid update.
    private static void water(boolean flowing) {
        waterEnabled = flowing;
        refreshWater();
    }

    private static void refreshWater() {
        for (Direction side : java.util.List.of(Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH)) {
            var cell = WHEEL.relative(side);
            for (int offset = -2; offset <= 2; offset++) {
                var pos = cell.south(offset);
                if (!pos.equals(WHEEL)) level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
            }
        }
        if (waterEnabled) {
            level.setBlock(WHEEL.south().above(), Blocks.WATER.defaultBlockState(), 2);
            level.setBlock(WHEEL.south(), Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 8), 2);
        }
        MechanicalNetwork.invalidate(level);
    }

    @SubscribeEvent public static void beforeTick(ServerTickEvent.Pre event) {
        if (active) refreshWater();
    }

    @SubscribeEvent public static void tick(ServerTickEvent.Post event) throws Exception {
        if (!active) return;
        try {
            if (++serverTicks > 600) throw new AssertionError("Hydraulic fixture timed out");
            if (ticks == 0 && !((br.com.quantumindustries.block.entity.PrimitiveWaterWheelBlockEntity)
                    level.getBlockEntity(WHEEL)).rotating()) return;
            ticks++;
            if (stage == 10) {
                if (ticks < 16) return;
                check(dynamo.energy() > previousEnergy, "automatic generation resumes after a real server restart");
                level.setBlock(DYNAMO.east(), ModBlocks.LEAD_ACID_ACCUMULATOR.get().defaultBlockState(), 3);
                stage = 11;
                return;
            }
            if (stage == 11) {
                if (ticks < 24) return;
                var accumulator = (br.com.quantumindustries.block.entity.EnergyInspectable) level.getBlockEntity(DYNAMO.east());
                check(accumulator.energy() > 0, "shaft-driven energy reaches the accumulator through the opposite electrical port");
                Files.writeString(Path.of("../complete.txt"), "PASS: hydraulic dynamo restart and " + checks + " checks\n");
                finish(null);
                return;
            }
            if (ticks == 16) {
                check(dynamo.energy() == 0, "water cannot bypass research authorization");
                WorldResearchData.get(level).unlockAll(player);
                var hit = new BlockHitResult(Vec3.atCenterOf(DYNAMO), Direction.UP, DYNAMO, false);
                dynamo.getBlockState().useWithoutItem(level, player, hit);
                check(dynamo.requestedLoad() == 6, "normal right-click configures an old dynamo");
                check(dynamo.connectsMechanical(Direction.WEST) && !dynamo.connectsMechanical(Direction.EAST), "only the crank socket accepts the shaft");
            }
            if (ticks == 48) {
                check(dynamo.energy() > 0 && dynamo.energy() <= 48 && dynamo.isTurning(), "authorized hydraulic drive produces bounded speed-dependent FE");
                previousEnergy = dynamo.energy();
            }
            if (ticks == 64) {
                check(dynamo.energy() > previousEnergy && dynamo.energy() <= previousEnergy + 24 && dynamo.isTurning(), "water and shaft generate speed-dependent FE without a crank: FE="
                        + dynamo.energy() + ", network=" + MechanicalNetwork.state(level, DYNAMO)
                        + ", fluid=" + level.getFluidState(WHEEL.south()));
                previousEnergy = dynamo.energy();
                level.setBlock(SHAFT, Blocks.AIR.defaultBlockState(), 3);
            }
            if (ticks == 72) {
                check(dynamo.energy() == previousEnergy && !dynamo.isTurning(), "shaft disconnection stops generation and animation");
                shaft(Direction.Axis.Z);
            }
            if (ticks == 80) {
                check(dynamo.energy() == previousEnergy, "perpendicular shafts cannot transmit power");
                shaft(Direction.Axis.X);
                water(false);
            }
            if (ticks == 88) {
                check(dynamo.energy() == previousEnergy, "dry wheel produces no energy");
                water(true);
                // Four loads on one straight line total 24 UM, exceeding the wheel's 18 UM.
                for (int offset = 3; offset <= 5; offset++) {
                    var pos = DYNAMO.west(offset);
                    level.setBlock(pos, ModBlocks.MECHANICAL_SAWMILL.get().defaultBlockState(), 3);
                    player.setPos(pos.getX() + .5, 100, 9.5);
                    check(((MechanicalSawmillBlockEntity) level.getBlockEntity(pos)).insert(player, new ItemStack(Items.OAK_LOG)), "add an authorized shared load");
                }
                player.setPos(8.5, 100, 9.5);
                MechanicalNetwork.invalidate(level);
            }
            if (ticks == 104) {
                check(dynamo.energy() == previousEnergy, "overloaded network cannot multiply available power");
                for (int offset = 3; offset <= 5; offset++) level.setBlock(DYNAMO.west(offset), Blocks.AIR.defaultBlockState(), 3);
                MechanicalNetwork.invalidate(level);
            }
            if (ticks == 128) {
                check(dynamo.energy() > previousEnergy && dynamo.energy() <= previousEnergy + 36, "generation resumes after overload and kinetic startup");
                previousEnergy = dynamo.energy();
            }
            if (ticks == 144) {
                check(dynamo.energy() > previousEnergy && dynamo.energy() <= previousEnergy + 24, "removing excess load resumes bounded generation");
                WorldResearchData.get(level).clearAll(player);
                previousEnergy = dynamo.energy();
            }
            if (ticks == 160) {
                check(dynamo.energy() == previousEnergy, "revoking the operator's knowledge suspends generation");
                WorldResearchData.get(level).unlockAll(player);
                check(dynamo.install(true), "manual crank can replace shaft operation");
                check(!dynamo.connectsMechanical(Direction.WEST), "installed crank occupies the mechanical socket");
            }
            if (ticks == 176) {
                check(dynamo.energy() == previousEnergy, "installed manual crank cannot generate from water");
                check(dynamo.turn() && !dynamo.turn(), "manual turn generates once and respects cooldown");
                check(dynamo.energy() == previousEnergy + 12, "manual generation still supplies 12 FE");
                check(dynamo.detach() && !dynamo.detach(), "paid crank is returned exactly once");
            }
            if (ticks == 400) {
                check(dynamo.energy() == 240 && dynamo.requestedLoad() == 0 && dynamo.isTurning(), "full buffer cannot overflow and freewheels with zero electrical load");
                var legacy = dynamo.saveWithoutMetadata(level.registryAccess());
                legacy.remove("mechanical_operator");
                legacy.remove("mechanical_ticks");
                legacy.remove("mechanically_driven");
                var restored = new ManualDynamoBlockEntity(DYNAMO, dynamo.getBlockState());
                restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), legacy));
                check(restored.energy() == 240 && !restored.installed(), "legacy saves keep energy without the new optional keys");
                check(dynamo.extractEnergy(24) == 24, "electrical output remains available");
                previousEnergy = dynamo.energy();
                Files.writeString(Path.of("../restart-ready.txt"), Integer.toString(previousEnergy));
                finish(null);
            }
        } catch (Throwable error) { finish(error); }
    }

    private static void finish(Throwable error) throws Exception {
        active = false;
        Files.writeString(Path.of("../result.txt"), error == null ? "PASS: " + checks + " hydraulic dynamo checks\n" : "FAIL: " + error);
        if (error != null) error.printStackTrace();
        level.getServer().halt(false);
    }
}
