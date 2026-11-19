package br.com.quantumindustries.logistics;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.block.entity.*;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.fluid.FluidTransfer;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

@EventBusSubscriber(modid = "quantumindustries")
public final class ConnectionVerification {
    private static final BlockPos ITEMS = new BlockPos(4, 100, 4);
    private static final BlockPos FLUID = new BlockPos(4, 100, 8);
    private static final BlockPos ENERGY = new BlockPos(4, 100, 12);
    private static ServerLevel level;
    private static FakePlayer player;
    private static int checks, ticks;
    private static boolean active, restart;

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    @SubscribeEvent public static void start(ServerStartedEvent event) throws Exception {
        if (!Boolean.getBoolean("quantumindustries.verifyConnections")) return;
        level = event.getServer().overworld();
        level.setChunkForced(0, 0, true);
        player = new FakePlayer(level, new GameProfile(UUID.fromString("fe21d04c-127c-4dab-a6d9-c9df7f67e5e8"), "ConnectionsQA"));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CONFIGURATION_WRENCH.get()));
        WorldResearchData.get(level).unlockAll(player);
        restart = Files.exists(Path.of("../restart-ready.txt"));
        try {
            if (restart) {
                check(conduit(ITEMS.east(2)).connectionMode(Direction.EAST) == ConnectionMode.NONE, "item exit mode survives full server restart");
                check(tube(2).packet().getCount() == 1, "item packet survives full server restart");
                check(pipe(2).storedFluid() == 200, "fluid survives full server restart");
                check(conduit(FLUID.east(2)).connectionMode(Direction.EAST) == ConnectionMode.NONE, "fluid exit mode survives full server restart");
                check(conduit(ENERGY.east(2)).connectionMode(Direction.EAST) == ConnectionMode.NONE, "energy exit mode survives full server restart");
            } else {
                setup();
                verifyGravityAndPressure();
                verifyWrenchAndCapabilities();
            }
            active = true;
        } catch (Throwable error) { finish(error); }
    }

    private static ConfigurableConduit conduit(BlockPos pos) { return (ConfigurableConduit) level.getBlockEntity(pos); }
    private static PneumaticTubeBlockEntity tube(int offset) { return (PneumaticTubeBlockEntity) level.getBlockEntity(ITEMS.east(offset)); }
    private static FluidPipeBlockEntity pipe(int offset) { return (FluidPipeBlockEntity) level.getBlockEntity(FLUID.east(offset)); }
    private static ChestBlockEntity chest(int offset) { return (ChestBlockEntity) level.getBlockEntity(ITEMS.east(offset)); }
    private static SettlingVatBlockEntity vat(int offset) { return (SettlingVatBlockEntity) level.getBlockEntity(FLUID.east(offset)); }
    private static EnergyInspectable energy(int offset) { return (EnergyInspectable) level.getBlockEntity(ENERGY.east(offset)); }

    private static void mode(BlockPos pos, Direction side, ConnectionMode mode) {
        var conduit = conduit(pos);
        conduit.connectionModes().set(side, mode);
        conduit.connectionChanged();
    }

    private static void configureLine(BlockPos origin) {
        for (int offset = 1; offset <= 2; offset++) {
            for (Direction side : Direction.values()) mode(origin.east(offset), side, ConnectionMode.NONE);
            mode(origin.east(offset), Direction.WEST, ConnectionMode.PULL);
            mode(origin.east(offset), Direction.EAST, ConnectionMode.PUSH);
        }
    }

    private static void fillVat(int offset, int amount) {
        try (var transaction = Transaction.openRoot()) {
            check(vat(offset).tank().insert(FluidResource.of(Fluids.WATER), amount, transaction) == amount, "stage real water");
            transaction.commit();
        }
    }

    private static void setup() {
        for (BlockPos origin : new BlockPos[]{ITEMS, FLUID, ENERGY}) {
            for (int x = 0; x <= 3; x++) level.setBlock(origin.east(x), Blocks.AIR.defaultBlockState(), 3);
        }
        level.setBlock(ITEMS, Blocks.CHEST.defaultBlockState(), 3);
        level.setBlock(ITEMS.east(3), Blocks.CHEST.defaultBlockState(), 3);
        for (int x = 1; x <= 2; x++) level.setBlock(ITEMS.east(x), ModBlocks.PNEUMATIC_TUBE.get().defaultBlockState(), 3);
        chest(0).setItem(0, new ItemStack(Items.IRON_INGOT, 10));
        level.setBlock(FLUID, ModBlocks.SETTLING_VAT.get().defaultBlockState(), 3);
        level.setBlock(FLUID.east(3), ModBlocks.SETTLING_VAT.get().defaultBlockState(), 3);
        level.setBlock(FLUID.east(), ModBlocks.FLUID_PIPE.get().defaultBlockState(), 3);
        level.setBlock(FLUID.east(2), ModBlocks.GLASS_PIPE.get().defaultBlockState(), 3);
        fillVat(0, 1000);
        level.setBlock(ENERGY, ModBlocks.LEAD_ACID_ACCUMULATOR.get().defaultBlockState(), 3);
        for (int x = 1; x <= 2; x++) level.setBlock(ENERGY.east(x), ModBlocks.INSULATED_CONDUIT.get().defaultBlockState(), 3);
        level.setBlock(ENERGY.east(3), ModBlocks.ELECTROMECHANICAL_PRESS.get().defaultBlockState(), 3);
        check(energy(0).receiveEnergy(128) == 128, "stage real FE");
        configureLine(ITEMS);
        configureLine(FLUID);
        configureLine(ENERGY);
        for (BlockPos origin : new BlockPos[]{ITEMS, FLUID, ENERGY}) mode(origin.east(2), Direction.EAST, ConnectionMode.NONE);
    }

    private static void click(BlockPos pos, Direction side, boolean reset) {
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + 1.5);
        player.setShiftKeyDown(reset);
        Vec3 hit = Vec3.atCenterOf(pos).add(side.getStepX() * .49, side.getStepY() * .49, side.getStepZ() * .49);
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, side, pos, false));
        ModItems.CONFIGURATION_WRENCH.get().useOn(context);
        player.setShiftKeyDown(false);
    }

    private static void verifyWrenchAndCapabilities() {
        BlockPos pos = FLUID.east();
        click(pos, Direction.UP, true);
        check(conduit(pos).connectionMode(Direction.UP) == ConnectionMode.AUTO, "Shift wrench restores automatic");
        click(pos, Direction.UP, false);
        check(conduit(pos).connectionMode(Direction.UP) == ConnectionMode.PULL, "wrench selects the upper endpoint");
        click(pos, Direction.UP, false);
        check(conduit(pos).connectionMode(Direction.UP) == ConnectionMode.PUSH, "wrench cycles pull to push");
        click(pos, Direction.UP, false);
        check(conduit(pos).connectionMode(Direction.UP) == ConnectionMode.NONE, "wrench cycles push to none");
        check(conduit(pos).connectionMode(Direction.WEST) == ConnectionMode.PULL, "configuring one face preserves another");
        var cached = FluidTransfer.handler(level, pos, Direction.UP);
        try (var transaction = Transaction.openRoot()) {
            check(cached.insert(FluidResource.of(Fluids.WATER), 50, transaction) == 0, "disabled fluid side rejects insertion");
        }
        mode(pos, Direction.UP, ConnectionMode.PULL);
        try (var transaction = Transaction.openRoot()) {
            check(cached.insert(FluidResource.of(Fluids.WATER), 50, transaction) == 50, "cached capability observes mode changes");
            check(cached.extract(FluidResource.of(Fluids.WATER), 50, transaction) == 0, "pull side rejects extraction");
        }
        check(pipe(1).storedFluid() == 0, "rolled-back probe creates no fluid");
        mode(pos, Direction.UP, ConnectionMode.NONE);
        var saved = pipe(1).saveWithoutMetadata(level.registryAccess());
        saved.remove("connection_modes");
        var legacy = new FluidPipeBlockEntity(pos, pipe(1).getBlockState());
        legacy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
        for (Direction side : Direction.values()) check(legacy.connectionMode(side) == ConnectionMode.AUTO, "legacy ports default to automatic");
        check(ConnectionTarget.face(pos, Vec3.atCenterOf(pos).add(.48, 0, .15), Direction.NORTH) == Direction.EAST,
                "side-wall hits and goggles identify the same arm");
        var rejected = new ItemStack(Items.IRON_INGOT);
        check(!tube(1).accept(rejected, Direction.EAST) && rejected.getCount() == 1, "push-only item side rejects insertion without consuming the packet");
    }

    private static void verifyGravityAndPressure() {
        var pos = new BlockPos(11, 100, 4);
        level.setBlock(pos, ModBlocks.FLUID_PIPE.get().defaultBlockState(), 3);
        level.setBlock(pos.above(), ModBlocks.GLASS_PIPE.get().defaultBlockState(), 3);
        for (Direction side : Direction.values()) {
            mode(pos, side, ConnectionMode.NONE);
            mode(pos.above(), side, ConnectionMode.NONE);
        }
        mode(pos, Direction.UP, ConnectionMode.PUSH);
        mode(pos.above(), Direction.DOWN, ConnectionMode.PULL);
        var lower = (FluidPipeBlockEntity) level.getBlockEntity(pos);
        var upper = (FluidPipeBlockEntity) level.getBlockEntity(pos.above());
        try (var transaction = Transaction.openRoot()) {
            check(lower.tank().insert(FluidResource.of(Fluids.WATER), 50, transaction) == 50, "stage pressure fixture");
            transaction.commit();
        }
        lower.tick();
        upper.tick();
        check(upper.storedFluid() == 0, "push mode still requires pressure for uphill flow");
        lower.pressurize(20);
        lower.tick();
        check(upper.storedFluid() == 50, "pressurized pipe can push uphill through an input port");
        check(lower.storedFluid() + upper.storedFluid() == 50, "pressure creates no fluid");
    }

    private static int itemTotal() { return chest(0).getItem(0).getCount() + tube(1).packet().getCount() + tube(2).packet().getCount() + chest(3).getItem(0).getCount(); }
    private static int fluidTotal() { return vat(0).storedFluid() + pipe(1).storedFluid() + pipe(2).storedFluid() + vat(3).storedFluid(); }

    @SubscribeEvent public static void tick(ServerTickEvent.Post event) throws Exception {
        if (!active) return;
        try {
            ticks++;
            if (restart) {
                if (ticks == 30) {
                    check(itemTotal() == 11 && chest(3).getItem(0).getCount() == 10, "disabled item endpoint remains blocked after restart");
                    check(fluidTotal() == 1200 && vat(3).storedFluid() == 1000, "disabled fluid endpoint remains blocked after restart");
                    check(energy(0).energy() == 64 && energy(3).energy() == 128, "disabled energy endpoint remains blocked after restart");
                    for (BlockPos origin : new BlockPos[]{ITEMS, FLUID, ENERGY}) {
                        click(origin.east(2), Direction.EAST, false);
                        click(origin.east(2), Direction.EAST, false);
                    }
                }
                if (ticks == 70) {
                    check(chest(3).getItem(0).getCount() == 11 && itemTotal() == 11, "items resume exactly once after wrench reconfiguration");
                    check(vat(3).storedFluid() == 1200 && fluidTotal() == 1200, "fluid resumes without loss or creation");
                    check(energy(3).energy() == 192 && energy(0).energy() == 0, "energy resumes and conserves FE");
                    Files.writeString(Path.of("../complete.txt"), "PASS: connections restart and " + checks + " checks\n");
                    finish(null);
                }
                return;
            }
            check(itemTotal() == (ticks <= 200 ? 10 : 11), "item conservation on every server tick");
            check(fluidTotal() == (ticks <= 200 ? 1000 : 1200), "fluid conservation on every server tick");
            if (ticks == 30) {
                check(chest(3).getItem(0).isEmpty() && vat(3).storedFluid() == 0 && energy(3).energy() == 0, "None blocks all three resource types");
                for (BlockPos origin : new BlockPos[]{ITEMS, FLUID, ENERGY}) {
                    click(origin.east(2), Direction.EAST, false);
                    click(origin.east(2), Direction.EAST, false);
                }
            }
            if (ticks == 200) {
                check(chest(3).getItem(0).getCount() == 10, "item tubes pull from a chest and push into another chest");
                check(vat(3).storedFluid() == 1000, "copper and glass pipes transfer real water");
                check(energy(0).energy() == 0 && energy(3).energy() == 128, "energy input/output modes preserve real FE");
                for (BlockPos origin : new BlockPos[]{ITEMS, FLUID, ENERGY}) mode(origin.east(2), Direction.EAST, ConnectionMode.NONE);
                chest(0).setItem(0, new ItemStack(Items.IRON_INGOT));
                fillVat(0, 200);
                check(energy(0).receiveEnergy(64) == 64, "stage blocked energy for restart");
            }
            if (ticks == 240) {
                check(tube(2).packet().getCount() == 1 && pipe(2).storedFluid() == 200, "blocked endpoints retain contents for restart");
                Files.writeString(Path.of("../restart-ready.txt"), "PASS: " + checks + " checks\n");
                finish(null);
            }
        } catch (Throwable error) { finish(error); }
    }

    private static void finish(Throwable error) throws Exception {
        active = false;
        Files.writeString(Path.of("../result.txt"), error == null ? "PASS: " + checks + " connection checks\n" : "FAIL: " + error);
        if (error != null) error.printStackTrace();
        level.getServer().halt(false);
    }
}
