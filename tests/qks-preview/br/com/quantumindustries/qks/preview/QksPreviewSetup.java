package br.com.quantumindustries.qks.preview;

import br.com.quantumindustries.block.entity.*;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.mechanical.HydraulicTorque;
import br.com.quantumindustries.mechanical.KineticNetworkManager;
import br.com.quantumindustries.mechanical.MechanicalNetwork;
import br.com.quantumindustries.research.WorldResearchData;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Administrative visual fixture, not survival/progression acceptance. */
@EventBusSubscriber(modid = "quantumindustries")
public final class QksPreviewSetup {
    public static final String PROPERTY = "quantumindustries.previewQks";
    public static final BlockPos WATER = new BlockPos(0, 101, 0);
    public static final BlockPos SMALL = new BlockPos(2, 101, 0);
    public static final BlockPos LARGE = new BlockPos(2, 101, 1);
    public static final BlockPos FLYWHEEL = new BlockPos(3, 101, 1);
    public static final BlockPos SAW = new BlockPos(4, 101, 1);
    public static final BlockPos OPEN_A = new BlockPos(0, 101, 6);
    public static final BlockPos OPEN_B = new BlockPos(0, 101, 10);
    public static final BlockPos CROSS_A = new BlockPos(7, 101, 6);
    public static final BlockPos CROSS_B = new BlockPos(7, 101, 10);
    public static final BlockPos MEDIUM = new BlockPos(0, 102, 16);
    public static final BlockPos BIG = new BlockPos(7, 103, 18);
    public static final List<BlockPos> ROTORS = List.of(WATER, SMALL, LARGE, FLYWHEEL, SAW,
            OPEN_A, OPEN_B, CROSS_A, CROSS_B, OPEN_B.east(), CROSS_B.east(),
            MEDIUM, BIG, MEDIUM.east(), BIG.east());
    public static volatile boolean ready;
    public static volatile int clock, requestedView, appliedView;
    public static volatile String failure, serverReport;
    private static final int BUILD_TICK = 60, VERIFY_TICK = 140;
    private static final double EPSILON = 1e-3;
    private static int ticks;
    private static boolean verified;

    @SubscribeEvent
    public static void tick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!Boolean.getBoolean(PROPERTY) || !(event.getEntity() instanceof ServerPlayer player) || failure != null) return;
        try {
            if (++ticks == BUILD_TICK) build(player);
            if (!ready) return;
            if (requestedView != appliedView) {
                switch (requestedView) {
                    case 1 -> camera(player, 10, 105, -6, 2, 101, .5);
                    case 2 -> camera(player, 16, 106, 1, 3.5, 101, 8);
                    case 3 -> camera(player, 22, 109, 7, 3.5, 103, 17);
                    case 4 -> {
                        player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
                        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,
                                new ItemStack(br.com.quantumindustries.ModItems.ENERGY_INSPECTION_GOGGLES.get()));
                        player.setNoGravity(true);
                        camera(player, 2.5, 100, -3, 2.5, 101.5, .5);
                    }
                    default -> throw new AssertionError("Unknown preview camera");
                }
                appliedView = requestedView;
            }
            if (clock >= VERIFY_TICK && !verified) {
                serverReport = verify(player.level());
                verified = true;
            }
        } catch (Exception | AssertionError error) {
            failure = "Server fixture: " + error;
        }
    }

    private static void build(ServerPlayer player) {
        var level = player.level();
        player.gameMode.changeGameModeForPlayer(GameType.SPECTATOR);
        // Clear enough sky for the full 5x5 rim, channels and all three camera views.
        for (int x = -6; x <= 25; x++) for (int z = -8; z <= 25; z++) {
            for (int y = 98; y <= 112; y++) put(level, new BlockPos(x, y, z), Blocks.AIR);
            put(level, new BlockPos(x, 99, z), Blocks.STONE);
        }
        for (int x = -4; x <= 20; x += 4) for (int z = -4; z <= 24; z += 4)
            put(level, new BlockPos(x, 108, z), Blocks.LIGHT);
        wheel(level, WATER, ModBlocks.PRIMITIVE_WATER_WHEEL.get());
        axial(level, WATER.east(), ModBlocks.MECHANICAL_SHAFT_BLOCK.get());
        axial(level, SMALL, ModBlocks.SMALL_COGWHEEL.get());
        // Parallel X axles; the mesh offset is perpendicular to X, not a bevel gear.
        axial(level, LARGE, ModBlocks.LARGE_COGWHEEL.get());
        axial(level, FLYWHEEL, ModBlocks.IRON_FLYWHEEL.get());
        level.setBlock(SAW, ModBlocks.MECHANICAL_SAWMILL.get().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), Block.UPDATE_ALL);
        channel(level, WATER.below());
        WorldResearchData.get(level).unlockAll(player);
        camera(player, 6, 102, -1, 2, 101, 0);
        var saw = (MechanicalSawmillBlockEntity) level.getBlockEntity(SAW);
        for (int i = 0; i < 16; i++) check(saw.insert(player, new ItemStack(Items.OAK_LOG)), "Sawmill rejected oak log");
        beltLine(level, OPEN_A, OPEN_B, false);
        beltLine(level, CROSS_A, CROSS_B, true);
        wheel(level, MEDIUM, ModBlocks.MEDIUM_WATER_WHEEL.get());
        wheel(level, BIG, ModBlocks.LARGE_WATER_WHEEL.get());
        axial(level, MEDIUM.east(), ModBlocks.MECHANICAL_SHAFT_BLOCK.get());
        axial(level, BIG.east(), ModBlocks.MECHANICAL_SHAFT_BLOCK.get());
        // Bottom rims at y=101, above the platform at y=99; no stone inside either rim.
        channel(level, MEDIUM.below(1));
        channel(level, BIG.below(2));
        MechanicalNetwork.invalidate(level);
        ready = true;
    }

    private static void beltLine(ServerLevel level, BlockPos a, BlockPos b, boolean crossed) {
        wheel(level, a.west(2), ModBlocks.PRIMITIVE_WATER_WHEEL.get());
        channel(level, a.west(2).below());
        axial(level, a.west(), ModBlocks.MECHANICAL_SHAFT_BLOCK.get());
        axial(level, a, ModBlocks.MECHANICAL_PULLEY.get());
        axial(level, b, ModBlocks.MECHANICAL_PULLEY.get());
        axial(level, b.east(), ModBlocks.MECHANICAL_SHAFT_BLOCK.get());
        var first = (MechanicalPulleyBlockEntity) level.getBlockEntity(a);
        var second = (MechanicalPulleyBlockEntity) level.getBlockEntity(b);
        check(MechanicalPulleyBlockEntity.validAlignment(a, b, Direction.Axis.X), "Invalid belt alignment");
        // Same reciprocal server API as DriveBeltItem, including block update and topology invalidation.
        first.setPartner(b, crossed);
        second.setPartner(a, crossed);
        check(first.validPartner(second) && second.validPartner(first), "Non-reciprocal belt");
        for (var endpoint : List.of(first, second)) {
            var restored = new MechanicalPulleyBlockEntity(endpoint.getBlockPos(), endpoint.getBlockState());
            restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(),
                    endpoint.saveWithoutMetadata(level.registryAccess())));
            check(endpoint.partnerPos().equals(restored.partnerPos()) && restored.crossed() == crossed,
                    "Belt save/load lost its endpoint or crossing");
        }
    }

    /** A source upstream, graded flowing cells and an open downstream drop; no injected rotor state. */
    private static void channel(ServerLevel level, BlockPos bottom) {
        final int upstream = 3, downstream = 4;
        for (int dz = -upstream; dz <= downstream; dz++) {
            var cell = bottom.offset(0, 0, dz);
            put(level, cell.below(), Blocks.STONE);
            put(level, cell.west(), Blocks.STONE);
            put(level, cell.east(), Blocks.STONE);
            level.setBlock(cell, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, dz + upstream), Block.UPDATE_ALL);
        }
        put(level, bottom.offset(0, 0, downstream + 1).below(), Blocks.AIR);
    }

    private static String verify(ServerLevel level) {
        for (BlockPos pos : ROTORS) {
            var snapshot = KineticNetworkManager.snapshot(level, pos);
            check(snapshot.powered() && !snapshot.jammed() && Math.abs(snapshot.angularVelocity()) > EPSILON,
                    "Stationary/unpowered server rotor " + pos + ": " + snapshot);
        }
        var small = KineticNetworkManager.snapshot(level, SMALL);
        var large = KineticNetworkManager.snapshot(level, LARGE);
        double meshRatio = -(double) CogwheelBlockEntity.SMALL_TEETH / CogwheelBlockEntity.LARGE_TEETH;
        check(Math.abs(large.angularVelocity() / small.angularVelocity() - meshRatio) < EPSILON, "Incorrect 8:16 gear ratio");
        verifyBelt(level, OPEN_A, OPEN_B, false);
        verifyBelt(level, CROSS_A, CROSS_B, true);
        var saw = (MechanicalSawmillBlockEntity) level.getBlockEntity(SAW);
        check(saw.progress() > 0 || !saw.output().isEmpty(), "Sawmill did not process wood");
        StringBuilder result = new StringBuilder("Server: moving 8:16 mesh ratio=-0.5, flywheel, working sawmill; reciprocal open +1/crossed -1 belts; both endpoint save/load round trips passed.\n");
        for (BlockPos pos : List.of(MEDIUM, BIG)) {
            var wheel = (RimWaterWheelBlockEntity) level.getBlockEntity(pos);
            int radius = wheel.rimRadius();
            int full = radius == 1 ? MediumWaterWheelBlockEntity.CAPACITY : LargeWaterWheelBlockEntity.CAPACITY;
            var one = HydraulicTorque.evaluateRim(Direction.Axis.X,
                    List.of(new HydraulicTorque.Sample(0, -radius, 0, 0, 0, 1)), radius, full);
            check(one.capacity() > 0 && one.capacity() < full, "Single-cell wheel output is not partial");
            check(wheel.sourceCapacity() > one.capacity() && wheel.sourceCapacity() < full,
                    "Live multi-cell channel did not exceed single-cell partial capacity: " + pos);
            result.append("Wheel radius=").append(radius).append(": analytical single-flow-cell capacity=")
                    .append(one.capacity()).append('/').append(full).append(", live multi-cell capacity=")
                    .append(wheel.sourceCapacity()).append(", torque=").append(wheel.hydraulicTorque())
                    .append(", omega=").append(wheel.angularVelocity()).append('\n');
        }
        return result.toString();
    }

    private static void verifyBelt(ServerLevel level, BlockPos a, BlockPos b, boolean crossed) {
        var first = (MechanicalPulleyBlockEntity) level.getBlockEntity(a);
        var second = (MechanicalPulleyBlockEntity) level.getBlockEntity(b);
        check(first.validPartner(second) && first.crossed() == crossed, "Belt link lost");
        double ratio = KineticNetworkManager.snapshot(level, b).angularVelocity()
                / KineticNetworkManager.snapshot(level, a).angularVelocity();
        check(Math.abs(ratio - (crossed ? -1 : 1)) < EPSILON, "Belt has incorrect signed ratio");
    }

    private static void axial(ServerLevel level, BlockPos pos, Block block) {
        level.setBlock(pos, block.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X), Block.UPDATE_ALL);
    }

    private static void wheel(ServerLevel level, BlockPos pos, Block block) {
        level.setBlock(pos, block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST), Block.UPDATE_ALL);
    }

    private static void put(ServerLevel level, BlockPos pos, Block block) {
        level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
    }

    private static void camera(ServerPlayer player, double x, double y, double z, double tx, double ty, double tz) {
        double dx = tx - x, dz = tz - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(Math.atan2(y + player.getEyeHeight() - ty, Math.hypot(dx, dz)));
        player.connection.teleport(x, y, z, yaw, pitch);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
