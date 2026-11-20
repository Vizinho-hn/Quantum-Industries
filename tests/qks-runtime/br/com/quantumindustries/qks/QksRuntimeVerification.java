package br.com.quantumindustries.qks;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.block.entity.*;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.inspection.*;
import br.com.quantumindustries.mechanical.*;
import br.com.quantumindustries.research.WorldResearchData;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import io.netty.buffer.Unpooled;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Real dedicated-server blocks and ticks; supplies/research are staged, progression is not under test. */
@EventBusSubscriber(modid = "quantumindustries")
public final class QksRuntimeVerification {
    private static final String ENABLED = "quantumindustries.verifyQks", PHASE = "quantumindustries.qksPhase";
    private static final String OPERATOR = "d18294eb-aef7-4dc8-81cf-32f12cfc3412";
    private static final Path ROOT = Path.of("..");
    private static final double EPS = 1e-6, MAX_STEP = 1.0;
    private static final int SETTLE = 80, REVERSE_AT = 100, REMOVE_AT = 200, SAVE_AT = 380, MAX_TICKS = 500;
    private static final int[] SIZES = {1, 32, 128, 256};
    private static final List<Feed> FEEDS = new ArrayList<>();
    private static final List<BlockPos> FLOWS = new ArrayList<>(), LOADS = new ArrayList<>();
    private static final List<BlockPos> GEARS = new ArrayList<>(), BELTS = new ArrayList<>();
    private static final BlockPos REVERSE = pos(0, 32), BARE = pos(0, 48), FLY = pos(0, 64);
    private static final BlockPos COAST = pos(0, 80), DYNAMO_WHEEL = pos(0, 96), DYNAMO = DYNAMO_WHEEL.east(2);
    private static final BlockPos SPLIT = pos(0, 112), JAM = pos(0, 128);
    private static ServerLevel level;
    private static FakePlayer player;
    private static boolean active, reading, reversed, crossedZero;
    private static int ticks, checks, coastProgress, coastOutput, coastEnergy;
    private static double firstSpeed, bareStartup, flyStartup, previousReverse, coastSpeed, savedReadAngle;
    private static final Properties SAVED = new Properties();
    private record Feed(BlockPos wheel, Direction facing, int mode) {}
    private static BlockPos pos(int x, int z) { return new BlockPos(x + 8, 100, z + 8); }
    private static void check(boolean ok, String label) {
        checks++;
        if (!ok) throw new AssertionError("tick=" + ticks + ": " + label);
    }
    private static void near(double actual, double expected, String label) {
        check(Double.isFinite(actual) && Math.abs(actual - expected) <= EPS, label + ": " + actual + " != " + expected);
    }
    @SubscribeEvent public static void start(ServerStartedEvent event) throws Exception {
        if (!Boolean.getBoolean(ENABLED)) return;
        level = event.getServer().overworld();
        try {
            String phase = System.getProperty(PHASE);
            check("write".equals(phase) || "read".equals(phase), "explicit write/read phase required");
            reading = "read".equals(phase);
            for (int z = 0; z <= 10; z++) for (int x = 0; x <= 17; x++) level.setChunkForced(x, z, true);
            if (reading) readSaved(); else build();
            active = true;
        } catch (Throwable error) { finish(error); }
    }
    private static void put(BlockPos pos, BlockState state) { level.setBlock(pos, state, Block.UPDATE_CLIENTS); }
    private static void axial(BlockPos pos, Block block) {
        put(pos, block.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X));
    }
    private static void wheel(BlockPos pos, Block block, Direction facing) {
        put(pos, block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
    }
    private static void feed(BlockPos pos, Direction facing, int mode) { FEEDS.add(new Feed(pos, facing, mode)); }
    private static void changeFeed(BlockPos pos, int mode) {
        FEEDS.removeIf(f -> f.wheel().equals(pos));
        feed(pos, Direction.EAST, mode);
        refreshWater();
        MechanicalNetwork.invalidate(level);
    }
    /** Four isolated rim contacts: left/right falling, top/bottom horizontal graded channels. */
    private static void water(Feed feed) {
        var p = feed.wheel();
        Direction tangent = feed.facing().getAxis() == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
        // Clear the four contacts first, so reversal/removal cannot leave another source behind.
        for (Direction side : List.of(Direction.UP, Direction.DOWN, tangent, tangent.getOpposite())) {
            var cell = p.relative(side);
            put(cell, Blocks.STONE.defaultBlockState());
            for (int distance = 1; distance <= 2; distance++) {
                var sideCell = cell.relative(tangent, distance);
                if (!sideCell.equals(p)) put(sideCell, Blocks.STONE.defaultBlockState());
                sideCell = cell.relative(tangent, -distance);
                if (!sideCell.equals(p)) put(sideCell, Blocks.STONE.defaultBlockState());
            }
        }
        if (feed.mode() == 0) return;
        if (Math.abs(feed.mode()) == 1) {
            // Lateral falling water gives exactly r x F = +/-1 about X (opposite about Z).
            var contact = p.relative(feed.mode() > 0 ? tangent : tangent.getOpposite());
            // A real source above sustains the falling cell through scheduled fluid ticks.
            // It lies diagonally from the wheel, outside the four sampled contacts.
            put(contact.above(), Blocks.WATER.defaultBlockState());
            put(contact, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 8));
            return;
        }
        boolean top = Math.abs(feed.mode()) == 2;
        var cell = top ? p.above() : p.below();
        int direction = feed.mode() > 0 ? 1 : -1;
        // Non-falling flow remains horizontal: source -> graded cell -> lower level.
        if (!cell.below().equals(p)) put(cell.below(), Blocks.STONE.defaultBlockState());
        put(cell.relative(tangent, -direction), Blocks.WATER.defaultBlockState());
        put(cell, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1));
        put(cell.relative(tangent, direction), Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 2));
    }
    private static void refreshWater() { for (var feed : FEEDS) water(feed); }
    @SubscribeEvent public static void beforeTick(ServerTickEvent.Pre event) throws Exception {
        if (!active) return;
        try { refreshWater(); MechanicalNetwork.invalidate(level); }
        catch (Throwable error) { finish(error); }
    }
    private static void stageSaw(BlockPos pos) {
        put(pos, ModBlocks.MECHANICAL_SAWMILL.get().defaultBlockState());
        var saw = saw(pos);
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        for (int i = 0; i < 16; i++)
            check(saw.insert(player, new ItemStack(Items.OAK_LOG)), "authorized sawmill accepts staged log " + i);
        check(saw.requestedLoad() == 6, "staged sawmill is an authorized real load");
    }
    private static void stageDynamo(BlockPos pos) {
        put(pos, ModBlocks.MANUAL_DYNAMO.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
        var dynamo = dynamo(pos);
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        check(dynamo.configureMechanicalDrive(player), "authorized dynamo accepts mechanical drive");
        check(dynamo.requestedLoad() == ManualDynamoBlockEntity.MECHANICAL_LOAD, "authorized dynamo load");
    }
    private static void build() {
        player = new FakePlayer(level, new GameProfile(UUID.fromString(OPERATOR), "QksVerifier"));
        WorldResearchData.get(level).unlockAll(player);
        for (Direction facing : List.of(Direction.EAST, Direction.NORTH)) {
            for (int mode : new int[]{-1, 1, 2, -3}) {
                var p = pos(32 + FLOWS.size() * 10, 0);
                wheel(p, ModBlocks.PRIMITIVE_WATER_WHEEL.get(), facing); feed(p, facing, mode); FLOWS.add(p);
            }
        }
        for (var p : List.of(REVERSE, BARE, FLY, COAST, DYNAMO_WHEEL, SPLIT)) {
            wheel(p, ModBlocks.PRIMITIVE_WATER_WHEEL.get(), Direction.EAST); feed(p, Direction.EAST, 1);
        }
        axial(BARE.east(), ModBlocks.MECHANICAL_SHAFT_BLOCK.get());
        axial(FLY.east(), ModBlocks.IRON_FLYWHEEL.get());
        axial(COAST.east(), ModBlocks.MECHANICAL_SHAFT_BLOCK.get()); stageSaw(COAST.east(2));
        axial(DYNAMO_WHEEL.east(), ModBlocks.MECHANICAL_SHAFT_BLOCK.get()); stageDynamo(DYNAMO);
        for (int count : new int[]{1, 3, 4}) {
            var p = pos(32, 16 + count * 16); LOADS.add(p);
            wheel(p, ModBlocks.PRIMITIVE_WATER_WHEEL.get(), Direction.EAST); feed(p, Direction.EAST, 1);
            for (int i = 1; i <= count; i++) stageSaw(p.east(i));
        }
        for (boolean largeFirst : new boolean[]{false, true}) {
            var p = pos(64, largeFirst ? 112 : 96); GEARS.add(p);
            wheel(p.west(), ModBlocks.PRIMITIVE_WATER_WHEEL.get(), Direction.EAST);
            feed(p.west(), Direction.EAST, 1);
            axial(p, largeFirst ? ModBlocks.LARGE_COGWHEEL.get() : ModBlocks.SMALL_COGWHEEL.get());
            axial(p.south(), largeFirst ? ModBlocks.SMALL_COGWHEEL.get() : ModBlocks.LARGE_COGWHEEL.get());
        }
        for (boolean crossed : new boolean[]{false, true}) {
            var p = pos(96, crossed ? 64 : 48); BELTS.add(p);
            wheel(p.west(), ModBlocks.PRIMITIVE_WATER_WHEEL.get(), Direction.EAST); feed(p.west(), Direction.EAST, 1);
            belt(p, p.south(4), crossed);
        }
        // Three gears have two meshes. A crossed return belt supplies the contradictory odd inversion.
        wheel(JAM.west(), ModBlocks.PRIMITIVE_WATER_WHEEL.get(), Direction.EAST); feed(JAM.west(), Direction.EAST, 1);
        for (int z = 0; z < 3; z++) axial(JAM.south(z), ModBlocks.SMALL_COGWHEEL.get());
        belt(JAM.east(), JAM.east().south(2), true);
        axial(SPLIT.east(), ModBlocks.MECHANICAL_SHAFT_BLOCK.get());
        axial(SPLIT.east(2), ModBlocks.IRON_FLYWHEEL.get());
        // Equal opposite sources preserve installed capacity while their signed drive sums to zero.
        var opposed = pos(32, 112);
        wheel(opposed, ModBlocks.PRIMITIVE_WATER_WHEEL.get(), Direction.EAST); feed(opposed, Direction.EAST, 1);
        axial(opposed.east(), ModBlocks.MECHANICAL_SHAFT_BLOCK.get());
        wheel(opposed.east(2), ModBlocks.PRIMITIVE_WATER_WHEEL.get(), Direction.EAST); feed(opposed.east(2), Direction.EAST, -1);
        var dry = pos(32, 128); wheel(dry, ModBlocks.PRIMITIVE_WATER_WHEEL.get(), Direction.EAST); feed(dry, Direction.EAST, 0);
        refreshWater();
        verifyRims();
        MechanicalNetwork.invalidate(level);
    }
    private static void belt(BlockPos a, BlockPos b, boolean crossed) {
        axial(a, ModBlocks.MECHANICAL_PULLEY.get()); axial(b, ModBlocks.MECHANICAL_PULLEY.get());
        check(MechanicalPulleyBlockEntity.validAlignment(a, b, Direction.Axis.X), "belt alignment");
        pulley(a).setPartner(b, crossed); pulley(b).setPartner(a, crossed);
    }
    private static void verifyRims() {
        double mediumTorque = 0, mediumInertia = 0;
        int mediumCapacity = 0;
        for (int radius : new int[]{1, 2}) {
            var p = pos(144, radius * 24);
            wheel(p, radius == 1 ? ModBlocks.MEDIUM_WATER_WHEEL.get() : ModBlocks.LARGE_WATER_WHEEL.get(), Direction.EAST);
            var rim = (RimWaterWheelBlockEntity) level.getBlockEntity(p);
            // Falling contacts along the positive-Z edge all exert the same axial torque.
            for (int y = -radius; y <= radius; y++) put(p.south(radius).above(y), Blocks.STONE.defaultBlockState());
            put(p.south(radius), Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 8));
            MechanicalNetwork.invalidate(level);
            int one = rim.sourceCapacity(); double torque = Math.abs(rim.kineticDriveTorque());
            int max = radius == 1 ? MediumWaterWheelBlockEntity.CAPACITY : LargeWaterWheelBlockEntity.CAPACITY;
            check(one > 0 && one < max && torque > 0, "one real water cell gives partial " + radius + " rim output");
            for (int y = -radius; y <= radius; y++)
                put(p.south(radius).above(y), Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 8));
            MechanicalNetwork.invalidate(level);
            check(rim.sourceCapacity() > one && Math.abs(rim.kineticDriveTorque()) > torque, "multi-cell rim increases UM and torque");
            if (radius == 1) {
                mediumTorque = Math.abs(rim.kineticDriveTorque()); mediumInertia = rim.kineticInertia(); mediumCapacity = rim.sourceCapacity();
            } else {
                check(rim.kineticInertia() > mediumInertia && max == 54, "5x5 inertia and 54 UM ceiling");
                // Compare maxima, not unequal wetted fractions of two different rims.
                check(LargeWaterWheelBlockEntity.CAPACITY > MediumWaterWheelBlockEntity.CAPACITY,
                        "larger wheel has higher maximum torque/UM");
                check(mediumTorque > 0 && mediumCapacity > 0, "3x3 comparison used measured output");
            }
        }
    }
    private static KineticSnapshot snap(BlockPos p) { return KineticNetworkManager.snapshot(level, p); }
    private static RotationalBlockEntity rotor(BlockPos p) { return (RotationalBlockEntity) level.getBlockEntity(p); }
    private static MechanicalSawmillBlockEntity saw(BlockPos p) { return (MechanicalSawmillBlockEntity) level.getBlockEntity(p); }
    private static ManualDynamoBlockEntity dynamo(BlockPos p) { return (ManualDynamoBlockEntity) level.getBlockEntity(p); }
    private static MechanicalPulleyBlockEntity pulley(BlockPos p) { return (MechanicalPulleyBlockEntity) level.getBlockEntity(p); }
    private static double speed(BlockPos p) { return Math.abs(snap(p).angularVelocity()); }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void tick(ServerTickEvent.Post event) throws Exception {
        if (!active) return;
        try {
            check(++ticks <= MAX_TICKS, "runtime deadline");
            if (reading) { tickRead(); return; }
            if (ticks == 1) {
                firstSpeed = speed(REVERSE); bareStartup = speed(BARE); flyStartup = speed(FLY);
                check(firstSpeed > 0 && bareStartup > flyStartup && flyStartup > 0, "flywheel reduces startup acceleration");
                check(((MechanicalParticipant) level.getBlockEntity(FLY.east())).sourceCapacity() == 0, "flywheel is never a source");
                telemetry(COAST.east(2));
            }
            if (ticks == 20) {
                check(speed(REVERSE) > firstSpeed, "progressive startup: omega_t20 > omega_t1 > 0");
                verifyFlows(); verifyTransmissions();
            }
            if (ticks == SETTLE) {
                check(speed(LOADS.get(0)) > speed(LOADS.get(1)) && speed(LOADS.get(1)) > 0,
                        "three sawmills settle slower than one");
                for (int i = 1; i <= 4; i++) {
                    var s = saw(LOADS.get(2).east(i));
                    check(s.stopReason() == StopReason.OVERLOADED && !s.rotating() && s.output().isEmpty() && s.progress() == 0,
                            "four sawmills stall and craft zero planks");
                }
                check(dynamo(DYNAMO).energy() > 0, "real dynamo generates FE");
                previousReverse = snap(REVERSE).angularVelocity(); check(previousReverse > 0, "reversal starts positive");
            }
            if (ticks == REVERSE_AT) {
                previousReverse = snap(REVERSE).angularVelocity(); changeFeed(REVERSE, -1);
            }
            if (ticks > REVERSE_AT && !reversed) {
                double now = snap(REVERSE).angularVelocity();
                check(now < previousReverse && Math.abs(now - previousReverse) < MAX_STEP, "flow reversal decelerates continuously");
                if (previousReverse >= 0 && now <= 0) crossedZero = true;
                if (now < -EPS) { check(crossedZero, "reversal crosses zero"); reversed = true; }
                previousReverse = now;
            }
            if (ticks == REMOVE_AT) {
                check(reversed, "reversed feed spins negative");
                check(dynamo(DYNAMO).energy() == ManualDynamoBlockEntity.CAPACITY && dynamo(DYNAMO).requestedLoad() == 0
                        && dynamo(DYNAMO).isTurning(), "240 FE open circuit freewheels without electrical load");
                // Drain through the production API so a full buffer cannot mask illicit coast generation.
                while (dynamo(DYNAMO).energy() > 0) dynamo(DYNAMO).extractEnergy(ManualDynamoBlockEntity.MAX_EXTRACT);
                coastEnergy = dynamo(DYNAMO).energy(); coastProgress = saw(COAST.east(2)).progress();
                coastOutput = saw(COAST.east(2)).output().getCount(); coastSpeed = speed(COAST);
                changeFeed(COAST, 0); changeFeed(BARE, 0); changeFeed(FLY, 0); changeFeed(DYNAMO_WHEEL, 0);
            }
            if (ticks > REMOVE_AT && ticks <= REMOVE_AT + 20) {
                check(!rotor(COAST).rotating() && speed(COAST) <= coastSpeed, "water removal separates coasting from power");
                if (ticks == REMOVE_AT + 1) check(speed(COAST) > 0 && speed(COAST) < coastSpeed, "water removed: smooth residual positive omega");
                check(saw(COAST.east(2)).progress() == coastProgress && saw(COAST.east(2)).output().getCount() == coastOutput,
                        "unpowered coast produces no planks");
                check(dynamo(DYNAMO).energy() == coastEnergy, "unpowered coast produces zero FE");
                coastSpeed = speed(COAST);

            }
            if (ticks >= REMOVE_AT + 80 && ticks <= REMOVE_AT + 100)
                check(speed(FLY) > speed(BARE), "flywheel retains higher speed longer after water removal");
            if (ticks == REMOVE_AT + 25) split();
            if (ticks == REMOVE_AT + 26) merge();
            if (ticks == REMOVE_AT + 27) check(speed(SPLIT.east(2)) > 0, "bridged shafts resume continuous motion");
            if (ticks == SAVE_AT - 20) {
                changeFeed(DYNAMO_WHEEL, 1);
                verifyBreak();
            }
            if (ticks == SAVE_AT) { benchmark(); saveForRestart(); finish(null); }
        } catch (Throwable error) { finish(error); }
    }
    private static void verifyFlows() {
        for (int i = 0; i < FLOWS.size(); i++) {
            var p = FLOWS.get(i); var wheel = (PrimitiveWaterWheelBlockEntity) level.getBlockEntity(p);
            // X: left=-1, right=+1, top=+1, bottom=+1; Z reverses axial sign.
            double expected = (i % 4 == 0 ? -1 : 1) * (i < 4 ? 1 : -1);
            check(Math.signum(wheel.hydraulicTorque()) == expected && Math.abs(wheel.hydraulicTorque()) > 0.25,
                    "live rim torque has expected sign and sufficient magnitude case " + i);
            check(wheel.sourceCapacity() == PrimitiveWaterWheelBlockEntity.CAPACITY, "moving rim grants 18 UM case " + i);
            check(Math.signum(wheel.angularVelocity()) == expected, "opposite lateral flow inverts spin case " + i);
        }
        var opposed = snap(pos(32, 112));
        check(opposed.stopReason() == StopReason.OPPOSING_SOURCES && !opposed.powered() && Math.abs(opposed.angularVelocity()) <= EPS,
                "balanced opposing sources stall cleanly");
        var dry = snap(pos(32, 128));
        check(dry.stopReason() == StopReason.NO_SOURCE && !dry.powered() && dry.angularVelocity() == 0, "blocked dry wheel stays stopped");
    }
    private static void verifyTransmissions() {
        for (int i = 0; i < GEARS.size(); i++) {
            var a = GEARS.get(i); var b = a.south(); double ratio = i == 0 ? -.5 : -2;
            check(speed(a) > 0, "cogwheel drive is live");
            near(snap(b).angularVelocity() / snap(a).angularVelocity(), ratio, "8:16 / 16:8 signed speed ratio");
            var gear = (CogwheelBlockEntity) level.getBlockEntity(a);
            var link = gear.kineticLinks(level, a).stream().filter(l -> l.target().equals(b)).findFirst().orElseThrow();
            near(link.efficiency(), .96, "mesh loss");
            double sourcePower = Math.abs(snap(a).driveTorque() * snap(a).angularVelocity());
            double transmittedTorque = Math.abs(snap(a).driveTorque()) * link.efficiency() / Math.abs(link.ratio());
            check(sourcePower > 0 && transmittedTorque * speed(b) <= sourcePower + EPS, "mesh cannot amplify transmitted power");
        }
        for (int i = 0; i < BELTS.size(); i++) {
            var a = BELTS.get(i); var b = a.south(4);
            check(pulley(a).validPartner(pulley(b)) && speed(a) > 0, "reciprocal powered belt");
            near(snap(b).angularVelocity() / snap(a).angularVelocity(), i == 0 ? 1 : -1, "open/crossed speed ratio");
            var link = pulley(a).kineticLinks(level, a).stream().filter(l -> l.target().equals(b)).findFirst().orElseThrow();
            near(link.efficiency(), .94, "belt efficiency");
        }
        check(snap(JAM).stopReason() == StopReason.JAMMED && snap(JAM).angularVelocity() == 0, "contradictory three-gear return loop jams");
    }
    private static void telemetry(BlockPos p) {
        var s = InspectionRegistry.inspect(level, p, true); var real = snap(p);
        check(s.kind() == InspectionSnapshot.Kind.MECHANICAL, "server inspection provider dispatch");
        near(s.angularVelocity(), real.angularVelocity(), "measured omega");
        near(s.angularAcceleration(), real.angularAcceleration(), "measured alpha");
        near(s.rpm(), real.rpm(), "measured RPM"); near(s.driveTorque(), real.driveTorque(), "measured drive");
        near(s.loadTorque(), real.loadTorque(), "measured load"); near(s.netTorque(), real.netTorque(), "measured net torque");
        near(s.equivalentInertia(), real.equivalentInertia(), "measured equivalent inertia");
        near(s.localInertia(), ((MechanicalParticipant) level.getBlockEntity(p)).kineticInertia(), "measured local inertia");
        near(s.effectiveRatio(), real.gearRatio(), "measured ratio");
        check(Math.abs(s.angularVelocity()) > 0 && Math.abs(s.angularAcceleration()) > 0 && Math.abs(s.rpm()) > 0
                && Math.abs(s.driveTorque()) > 0 && s.loadTorque() > 0 && Math.abs(s.netTorque()) > 0
                && s.equivalentInertia() > 0 && s.localInertia() > 0 && Math.abs(s.mechanicalPower()) > 0
                && s.effectiveRatio() != 0 && s.stopReason() == StopReason.NONE, "all eleven QKS telemetry components carry real values");
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        try {
            InspectionSnapshot.STREAM_CODEC.encode(buf, s);
            check(s.equals(InspectionSnapshot.STREAM_CODEC.decode(buf)) && !buf.isReadable(), "registry-aware telemetry codec roundtrip");
        } finally { buf.release(); }
        var empty = InspectionRegistry.inspect(level, pos(240, 160), true);
        check(empty.kind() == InspectionSnapshot.Kind.NONE, "non-machine inspection returns empty");
    }
    private static void split() {
        double before = speed(SPLIT.east(2));
        put(SPLIT.east(), Blocks.AIR.defaultBlockState()); MechanicalNetwork.invalidate(level);
        check(speed(SPLIT.east(2)) > 0 && Math.abs(speed(SPLIT.east(2)) - before) < MAX_STEP, "split retains flywheel momentum");
    }
    private static void merge() {
        double before = speed(SPLIT.east(2));
        axial(SPLIT.east(), ModBlocks.MECHANICAL_SHAFT_BLOCK.get()); MechanicalNetwork.invalidate(level);
        check(speed(SPLIT.east(2)) > 0 && Math.abs(speed(SPLIT.east(2)) - before) < MAX_STEP, "merge preserves continuous momentum");
    }
    private static void verifyBreak() {
        var a = BELTS.get(0); var b = a.south(4);
        int before = beltDrops(a);
        level.destroyBlock(a, true);
        check(pulley(b).partnerPos() == null && beltDrops(a) == before + 1, "breaking endpoint refunds one belt and clears partner");
        check(!pulley(b).unlink(true) && beltDrops(a) == before + 1, "repeated unlink cannot duplicate belt");
        // Rebuild so both open and crossed links are covered by the full restart.
        belt(a, b, false);
    }
    private static int beltDrops(BlockPos p) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(p).inflate(3)).stream()
                .filter(e -> e.getItem().is(ModItems.DRIVE_BELT.get())).mapToInt(e -> e.getItem().getCount()).sum();
    }
    private static void benchmark() {
        // Each size is a physically connected straight shaft; no precomputed or synthetic node graph.
        for (int count : SIZES) {
            var origin = pos(0, 152);
            for (int i = 0; i < KineticNetworkManager.MAX_NODES; i++)
                put(origin.east(i), Blocks.AIR.defaultBlockState());
            for (int i = 0; i < count; i++) axial(origin.east(i), ModBlocks.MECHANICAL_SHAFT_BLOCK.get());
            long total = 0;
            final int warmup = 3, samples = 10;
            for (int sample = -warmup; sample < samples; sample++) {
                KineticNetworkManager.invalidate(level);
                long start = System.nanoTime(); var snapshot = snap(origin); long elapsed = System.nanoTime() - start;
                check(snapshot.nodeCount() == count && !snapshot.limited(), "benchmark covers exactly " + count + " live connected nodes");
                if (sample >= 0) total += elapsed;
            }
            LogUtils.getLogger().info("QKS runtime benchmark: {} nodes, {} µs (mean rebuild/solve, {} samples)", count, total / (samples * 1000.0), samples);
        }
    }
    private static List<BlockPos> persistentRotors() {
        return List.of(SPLIT, SPLIT.east(2), pos(96, 48), pos(96, 48).south(4), pos(96, 64), pos(96, 64).south(4));
    }
    private static String key(BlockPos p) { return Long.toString(p.asLong()); }
    private static void saveForRestart() throws Exception {
        for (var p : persistentRotors()) {
            var r = rotor(p); check(speed(p) > 0, "saved rotor is moving " + p);
            r.setChanged(); var nbt = r.saveWithoutMetadata(level.registryAccess());
            near(nbt.getDouble("qks_angle").orElseThrow(), r.angleRadians(), "saved qks_angle");
            near(nbt.getDouble("qks_omega").orElseThrow(), r.angularVelocity(), "saved qks_omega");
            SAVED.setProperty(key(p) + ".angle", Double.toString(r.angleRadians()));
            SAVED.setProperty(key(p) + ".omega", Double.toString(r.angularVelocity()));
        }
        SAVED.setProperty("fe", Integer.toString(dynamo(DYNAMO).energy()));
        check(dynamo(DYNAMO).energy() > 0, "save has real generated FE"); dynamo(DYNAMO).setChanged();
        try (var out = Files.newBufferedWriter(ROOT.resolve("restart-ready.txt"))) { SAVED.store(out, "QKS write phase evidence"); }
    }
    private static void readSaved() throws Exception {
        try (var in = Files.newBufferedReader(ROOT.resolve("restart-ready.txt"))) { SAVED.load(in); }
        for (var p : persistentRotors()) {
            level.getChunkAt(p);
            near(rotor(p).angleRadians(), Double.parseDouble(SAVED.getProperty(key(p) + ".angle")), "restart qks_angle " + p);
            near(rotor(p).angularVelocity(), Double.parseDouble(SAVED.getProperty(key(p) + ".omega")), "restart qks_omega " + p);
        }
        near(dynamo(DYNAMO).energy(), Integer.parseInt(SAVED.getProperty("fe")), "restart dynamo buffer");
        for (int i = 0; i < 2; i++) {
            var a = pos(96, i == 0 ? 48 : 64); var b = a.south(4);
            check(pulley(a).validPartner(pulley(b)) && pulley(a).crossed() == (i == 1)
                    && pulley(b).crossed() == (i == 1), "restart preserves reciprocal link and crossing");
        }
        savedReadAngle = rotor(SPLIT).angleRadians();
        // Re-establish deterministic channels only after persisted values were checked, before any ticks.
        for (var p : List.of(SPLIT, DYNAMO_WHEEL, pos(96, 48).west(), pos(96, 64).west())) feed(p, Direction.EAST, 1);
        refreshWater(); MechanicalNetwork.invalidate(level);
    }
    private static void tickRead() throws Exception {
        if (ticks < 10) return;
        check(speed(SPLIT) > 0 && rotor(SPLIT).angleRadians() != savedReadAngle, "fresh server resumes saved motion");
        check(dynamo(DYNAMO).energy() > Integer.parseInt(SAVED.getProperty("fe")), "fresh server resumes FE generation");
        for (var p : List.of(pos(96, 48), pos(96, 64))) check(speed(p) > 0 && speed(p.south(4)) > 0, "reloaded belt resumes transmission");
        Files.writeString(ROOT.resolve("complete.txt"), "PASS: full server restart, rotor phase/omega, belts, FE and resumed motion; assertions=" + checks + "\n");
        finish(null);
    }
    private static void finish(Throwable error) throws Exception {
        active = false;
        try {
            Files.writeString(ROOT.resolve("result.txt"), error == null
                    ? "PASS: " + System.getProperty(PHASE) + " assertions=" + checks + "\n"
                    : "FAIL: " + System.getProperty(PHASE) + " tick=" + ticks + " " + error + "\n");
            if (error != null) LogUtils.getLogger().error("QKS runtime verification failed", error);
        } finally { level.getServer().halt(false); }
    }
}
