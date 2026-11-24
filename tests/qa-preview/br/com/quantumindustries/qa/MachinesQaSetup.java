package br.com.quantumindustries.qa;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.block.CopperCableBlock;
import br.com.quantumindustries.block.entity.BellowsAlloyKilnBlockEntity;
import br.com.quantumindustries.block.entity.BellowsBrazierBlockEntity;
import br.com.quantumindustries.block.entity.BellowsEjectorBlockEntity;
import br.com.quantumindustries.block.entity.ElectricAlloySmelterBlockEntity;
import br.com.quantumindustries.block.entity.ElectromechanicalPressBlockEntity;
import br.com.quantumindustries.block.entity.JawCrusherBlockEntity;
import br.com.quantumindustries.block.entity.LeadAcidAccumulatorBlockEntity;
import br.com.quantumindustries.block.entity.LoadRelayBlockEntity;
import br.com.quantumindustries.block.entity.ManualDynamoBlockEntity;
import br.com.quantumindustries.block.entity.ManualGrainMillBlockEntity;
import br.com.quantumindustries.block.entity.MechanicalSawmillBlockEntity;
import br.com.quantumindustries.block.entity.MechanicalShaftBlockEntity;
import br.com.quantumindustries.block.entity.PrimitiveWaterWheelBlockEntity;
import br.com.quantumindustries.block.entity.SettlingVatBlockEntity;
import br.com.quantumindustries.block.entity.VoltaicPileBlockEntity;
import br.com.quantumindustries.block.entity.WireDrawingBenchBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.logistics.ItemRouting;
import br.com.quantumindustries.material.AlloyCatalog;
import br.com.quantumindustries.material.CrushingCatalog;
import br.com.quantumindustries.material.FormingMaterials;
import br.com.quantumindustries.material.HeatTreatment;
import br.com.quantumindustries.material.MineralCatalog;
import br.com.quantumindustries.mechanical.MechanicalNetwork;
import br.com.quantumindustries.research.WorldResearchData;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Runtime QA harness: builds an in-world bench of all Quantum Industries machines,
 * drives their actual gameplay interactions and server ticks, and writes qa-report.txt.
 */
@EventBusSubscriber(modid = "quantumindustries")
public final class MachinesQaSetup {
    public static volatile int stage;
    public static volatile boolean done;
    private static int ticks;
    private static int applied;
    private static final List<String> REPORT = new ArrayList<>();

    // Coordinates for key test machines
    private static final BlockPos FORMING_POS = new BlockPos(0, 100, 1);
    private static final BlockPos MILL_POS = new BlockPos(2, 100, 1);
    private static final BlockPos BRAZIER_POS = new BlockPos(4, 100, 1);
    private static final BlockPos KILN_POS = new BlockPos(6, 100, 1);
    private static final BlockPos CRUSHER_POS = new BlockPos(8, 100, 1);
    private static final BlockPos WIRE_POS = new BlockPos(10, 100, 1);
    private static final BlockPos PRESS_POS = new BlockPos(12, 100, 1);
    private static final BlockPos PRESS_BAT_POS = new BlockPos(12, 100, 2);
    private static final BlockPos SMELTER_POS = new BlockPos(14, 100, 1);
    private static final BlockPos SMELTER_BAT_POS = new BlockPos(14, 100, 2);

    private static final BlockPos WHEEL_POS = new BlockPos(0, 100, 5);
    private static final BlockPos SAWMILL_POS = new BlockPos(3, 100, 5);
    private static final BlockPos DYNAMO_POS = new BlockPos(5, 100, 5);
    private static final BlockPos PING_PILE_POS = new BlockPos(14, 100, 5);
    private static final BlockPos PING_ACCUM_POS = new BlockPos(19, 100, 5);
    private static final BlockPos RELAY_ACCUM_POS = new BlockPos(0, 100, 8);
    private static final BlockPos RELAY_POS = new BlockPos(1, 100, 8);

    private static final BlockPos CHEST_IN_POS = new BlockPos(4, 100, 9);
    private static final BlockPos EJECTOR_POS = new BlockPos(5, 100, 9);
    private static final BlockPos TUBE_POS = new BlockPos(6, 100, 9);
    private static final BlockPos SORTER_POS = new BlockPos(7, 100, 9);
    private static final BlockPos CHEST_OUT_POS = new BlockPos(8, 100, 9);
    private static final BlockPos VAT_POS = new BlockPos(10, 100, 9);
    private static final BlockPos VAT_BREAK_POS = new BlockPos(11, 100, 9);
    private static final BlockPos ASSAY_POS = new BlockPos(13, 100, 9);
    private static final BlockPos TIER1_POS = new BlockPos(15, 100, 9);
    private static final BlockPos TIER2_POS = new BlockPos(17, 100, 9);
    private static final BlockPos VAULT_POS = new BlockPos(19, 100, 9);

    private static int smelterStartFe;
    private static int pingStartTotalFe;

    private static BlockHitResult hit(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }

    private static void log(String line) {
        REPORT.add(line);
        System.out.println("[QA] " + line);
    }

    private static BlockState facingNorth(Block block) {
        BlockState state = block.defaultBlockState();
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                : state;
    }

    private static void placeCable(ServerLevel level, BlockPos pos, CopperCableBlock block) {
        level.setBlock(pos, CopperCableBlock.computeState(level, pos, block.defaultBlockState()), Block.UPDATE_ALL);
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!Boolean.getBoolean("quantumindustries.qaPreview")) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = player.level();

        // Let the camera move for client screenshots even after server verification completes
        if (stage != applied) {
            applied = stage;
            if (stage == 2) {
                player.connection.teleport(7.5, 102.3, -2.6, -4f, 18f);
            } else if (stage == 3) {
                player.connection.teleport(10.5, 102.5, 3.0, -10f, 22f);
            } else if (stage == 4) {
                player.connection.teleport(15.5, 102.3, 6.2, -5f, 18f);
            } else if (stage == 5) {
                player.connection.teleport(17.5, 100.3, 0.8, 0f, 22f);
            } else if (stage == 6) {
                player.connection.teleport(4.3, 100.3, 2.3, 42f, 20f);
            }
        }
        if (done) return;

        if (ticks == 0) {
            player.gameMode.changeGameModeForPlayer(GameType.SPECTATOR);
            player.connection.teleport(8.5, 104.5, -5.0, 0f, 26f);
        }
        ticks++;

        if (ticks == 50) {
            buildBenchAndStartTests(level, player);
            stage = 1;
            return;
        }

        // Real-time manual strokes respecting machine cooldowns (6-10 ticks)
        if (ticks == 65 || ticks == 75 || ticks == 85 || ticks == 95) {
            if (level.getBlockEntity(MILL_POS) instanceof ManualGrainMillBlockEntity mill) mill.stroke();
            if (level.getBlockEntity(CRUSHER_POS) instanceof JawCrusherBlockEntity crusher) crusher.manualStroke();
            if (level.getBlockEntity(WIRE_POS) instanceof WireDrawingBenchBlockEntity wire) wire.manualStroke();
        }

        // Complete runtime assertions after machines have had 140 ticks (7 seconds) to process
        if (ticks == 200) {
            verifyDelayedResults(level, player);
        }
    }

    private static void buildBenchAndStartTests(ServerLevel level, ServerPlayer player) {
        REPORT.clear();
        log("=== QUANTUM INDUSTRIES LIVE RUNTIME QA REPORT ===");
        WorldResearchData.get(level).unlockAll(player);

        for (int x = -4; x <= 22; x++) {
            for (int z = -6; z <= 14; z++) {
                level.setBlock(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState(), 3);
                for (int y = 100; y <= 106; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        for (int x = 0; x <= 20; x += 4) {
            for (int z = 0; z <= 10; z += 4) {
                level.setBlock(new BlockPos(x, 104, z), Blocks.LIGHT.defaultBlockState(), 3);
            }
        }

        // Row 1: Processing & Metallurgy
        level.setBlock(FORMING_POS, ModBlocks.FORMING_BENCH.get().defaultBlockState(), 3);
        level.setBlock(MILL_POS, facingNorth(ModBlocks.MANUAL_GRAIN_MILL.get()), 3);
        level.setBlock(BRAZIER_POS.below(), Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
        level.setBlock(BRAZIER_POS, ModBlocks.BELLOWS_BRAZIER.get().defaultBlockState(), 3);
        level.setBlock(KILN_POS, facingNorth(ModBlocks.BELLOWS_ALLOY_KILN.get()), 3);
        level.setBlock(CRUSHER_POS, facingNorth(ModBlocks.JAW_CRUSHER.get()), 3);
        level.setBlock(WIRE_POS, facingNorth(ModBlocks.WIRE_DRAWING_BENCH.get()), 3);
        level.setBlock(PRESS_POS, facingNorth(ModBlocks.ELECTROMECHANICAL_PRESS.get()), 3);
        level.setBlock(PRESS_BAT_POS, ModBlocks.VOLTAIC_PILE.get().defaultBlockState(), 3);
        level.setBlock(SMELTER_POS, facingNorth(ModBlocks.ELECTRIC_ALLOY_SMELTER.get()), 3);
        level.setBlock(SMELTER_BAT_POS, ModBlocks.LEAD_ACID_ACCUMULATOR.get().defaultBlockState(), 3);

        // Row 2: Mechanical, Dynamo, Relay, Battery Ping-Pong
        level.setBlock(WHEEL_POS, ModBlocks.PRIMITIVE_WATER_WHEEL.get().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST), 3);
        level.setBlock(WHEEL_POS.below(), Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 8), 3);
        level.setBlock(WHEEL_POS.below().north(), Blocks.WATER.defaultBlockState(), 3);
        level.setBlock(new BlockPos(1, 100, 5), ModBlocks.MECHANICAL_SHAFT_BLOCK.get().defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.X), 3);
        level.setBlock(new BlockPos(1, 99, 5), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(new BlockPos(2, 100, 5), ModBlocks.MECHANICAL_SHAFT_BLOCK.get().defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.X), 3);
        level.setBlock(SAWMILL_POS, facingNorth(ModBlocks.MECHANICAL_SAWMILL.get()), 3);
        MechanicalNetwork.invalidate(level);

        level.setBlock(DYNAMO_POS, facingNorth(ModBlocks.MANUAL_DYNAMO.get()), 3);
        for (int x = 6; x <= 9; x++) placeCable(level, new BlockPos(x, 100, 5), ModBlocks.COPPER_BUSBAR.get());
        level.setBlock(new BlockPos(10, 100, 5), ModBlocks.VOLTAIC_PILE.get().defaultBlockState(), 3);
        level.setBlock(new BlockPos(11, 100, 5), ModBlocks.LOAD_RELAY.get().defaultBlockState(), 3);
        level.setBlock(new BlockPos(12, 100, 5), ModBlocks.PIEZO_SWITCH.get().defaultBlockState(), 3);

        level.setBlock(PING_PILE_POS, ModBlocks.VOLTAIC_PILE.get().defaultBlockState(), 3);
        for (int x = 15; x <= 18; x++) placeCable(level, new BlockPos(x, 100, 5), ModBlocks.COPPER_BUSBAR.get());
        level.setBlock(PING_ACCUM_POS, ModBlocks.LEAD_ACID_ACCUMULATOR.get().defaultBlockState(), 3);

        // Close-up battery showcase (facing North, linked via CopperBusbar + standalone LeadAcidAccumulator)
        level.setBlock(new BlockPos(16, 100, 3), facingNorth(ModBlocks.VOLTAIC_PILE.get()), 3);
        placeCable(level, new BlockPos(17, 100, 3), ModBlocks.COPPER_BUSBAR.get());
        level.setBlock(new BlockPos(18, 100, 3), facingNorth(ModBlocks.LEAD_ACID_ACCUMULATOR.get()), 3);

        level.setBlock(RELAY_ACCUM_POS, ModBlocks.LEAD_ACID_ACCUMULATOR.get().defaultBlockState(), 3);
        level.setBlock(RELAY_POS, ModBlocks.LOAD_RELAY.get().defaultBlockState(), 3);
        placeCable(level, new BlockPos(2, 100, 8), ModBlocks.INSULATED_CONDUIT.get());
        level.setBlock(new BlockPos(3, 100, 8), ModBlocks.PERISTALTIC_PUMP.get().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST), 3);

        // Row 3: Pneumatic, Fluid/Vat, Research & Vault
        level.setBlock(CHEST_IN_POS, Blocks.CHEST.defaultBlockState(), 3);
        level.setBlock(EJECTOR_POS, ModBlocks.BELLOWS_EJECTOR.get().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST), 3);
        level.setBlock(EJECTOR_POS.south(), ModBlocks.VOLTAIC_PILE.get().defaultBlockState(), 3);
        level.setBlock(TUBE_POS, ModBlocks.PNEUMATIC_TUBE.get().defaultBlockState(), 3);
        level.setBlock(SORTER_POS, ModBlocks.GRAVITATIONAL_SORTER.get().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST), 3);
        level.setBlock(CHEST_OUT_POS, Blocks.CHEST.defaultBlockState(), 3);

        level.setBlock(VAT_POS, ModBlocks.SETTLING_VAT.get().defaultBlockState(), 3);
        level.setBlock(VAT_BREAK_POS, ModBlocks.SETTLING_VAT.get().defaultBlockState(), 3);
        level.setBlock(new BlockPos(10, 100, 10), ModBlocks.GLASS_PIPE.get().defaultBlockState(), 3);
        level.setBlock(new BlockPos(11, 100, 10), ModBlocks.FLUID_PIPE.get().defaultBlockState(), 3);

        level.setBlock(ASSAY_POS, facingNorth(ModBlocks.EMPIRICAL_ASSAY_BENCH.get()), 3);
        level.setBlock(TIER1_POS, facingNorth(ModBlocks.RESEARCH_TABLE_TIER_1.get()), 3);
        level.setBlock(TIER2_POS, facingNorth(ModBlocks.RESEARCH_TABLE.get()), 3);
        level.setBlock(VAULT_POS, facingNorth(ModBlocks.QUANTUM_VAULT.get()), 3);

        runImmediateTests(level, player);
    }

    private static void runImmediateTests(ServerLevel level, ServerPlayer player) {
        // 1. Bellows Brazier (heated by Magma Block underneath, no coal required)
        player.setPos(BRAZIER_POS.getX()+.5, BRAZIER_POS.getY(), BRAZIER_POS.getZ()+1.5);
        var brazierState = level.getBlockState(BRAZIER_POS);
        var gold = new ItemStack(Items.GOLD_INGOT, 1);
        var goldRes = brazierState.useItemOn(gold, level, player, InteractionHand.MAIN_HAND, hit(BRAZIER_POS));
        var coal = new ItemStack(Items.COAL, 2);
        brazierState.useItemOn(coal, level, player, InteractionHand.MAIN_HAND, hit(BRAZIER_POS));
        var iron = new ItemStack(Items.IRON_INGOT, 2);
        brazierState.useItemOn(iron, level, player, InteractionHand.MAIN_HAND, hit(BRAZIER_POS));
        var bellows = new ItemStack(ModItems.MANUAL_BELLOWS.get(), 1);
        brazierState.useItemOn(bellows, level, player, InteractionHand.MAIN_HAND, hit(BRAZIER_POS));
        log("BellowsBrazier: gold_ingot result=" + goldRes + " (count left=" + gold.getCount() + "), "
                + "coal consumed=" + (2 - coal.getCount()) + ", iron consumed=" + (2 - iron.getCount())
                + ", heatBelow=" + BellowsBrazierBlockEntity.heatSourceLevel(level, BRAZIER_POS));

        // 2. Forming Bench (cold vs heated)
        var formingState = level.getBlockState(FORMING_POS);
        var hammer = new ItemStack(ModItems.MANUAL_HAMMER.get());
        player.setItemSlot(EquipmentSlot.MAINHAND, hammer);
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.IRON_INGOT, 1));
        for (int i = 0; i < 3; i++) {
            player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(hammer));
            formingState.useItemOn(hammer, level, player, InteractionHand.MAIN_HAND, hit(FORMING_POS));
        }
        int coldSheets = player.getInventory().countItem(ModItems.IRON_SHEET.get());
        ItemStack hotIron = HeatTreatment.heat(new ItemStack(Items.IRON_INGOT), level);
        player.setItemSlot(EquipmentSlot.OFFHAND, hotIron);
        player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(hammer));
        formingState.useItemOn(hammer, level, player, InteractionHand.MAIN_HAND, hit(FORMING_POS));
        int totalSheets = player.getInventory().countItem(ModItems.IRON_SHEET.get());
        log("FormingBench: cold 3-hit sheets=" + coldSheets + ", hot 1-hit delta=" + (totalSheets - coldSheets));
        player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);

        // 3. Manual Grain Mill
        player.setPos(MILL_POS.getX()+.5, MILL_POS.getY(), MILL_POS.getZ()+1.5);
        var millState = level.getBlockState(MILL_POS);
        var wheat = new ItemStack(Items.WHEAT, 1);
        millState.useItemOn(wheat, level, player, InteractionHand.MAIN_HAND, hit(MILL_POS));
        var millBe = (ManualGrainMillBlockEntity) level.getBlockEntity(MILL_POS);
        for (int i = 0; i < 4; i++) {
            // bypass 6-tick cooldown cleanly by using tick loop or checking first stroke
        }
        log("ManualGrainMill: inserted wheat, left=" + wheat.getCount() + ", status=" + millBe.status().getString());

        // 4. Bellows Alloy Kiln (Bronze: 3 copper + 1 tin, insert takes 1 item per call)
        var kiln = (BellowsAlloyKilnBlockEntity) level.getBlockEntity(KILN_POS);
        player.setPos(KILN_POS.getX()+.5, KILN_POS.getY(), KILN_POS.getZ()+1.5);
        if (!kiln.bindAutomation(player)) throw new AssertionError("QA machine authorization: kiln");
        kiln.insert(new ItemStack(Items.COAL, 1));
        for (int i = 0; i < 3; i++) kiln.insert(new ItemStack(Items.COPPER_INGOT, 1));
        kiln.insert(new ItemStack(ModItems.INGOTS.get("tin").get(), 1));
        kiln.puff();
        log("BellowsAlloyKiln: staged 3 copper + 1 tin + coal + puff");

        // 5. Electric Alloy Smelter (Steel: 1 iron + 2 coal, insertIngredient takes 1 item per call)
        var smelterBat = (LeadAcidAccumulatorBlockEntity) level.getBlockEntity(SMELTER_BAT_POS);
        smelterBat.setStoredEnergy(2000);
        smelterStartFe = smelterBat.energy();
        var smelter = (ElectricAlloySmelterBlockEntity) level.getBlockEntity(SMELTER_POS);
        player.setPos(SMELTER_POS.getX()+.5, SMELTER_POS.getY(), SMELTER_POS.getZ()+1.5);
        if (!smelter.bindAutomation(player)) throw new AssertionError("QA machine authorization: smelter");
        smelter.insertIngredient(new ItemStack(Items.IRON_INGOT, 1));
        for (int i = 0; i < 2; i++) smelter.insertIngredient(new ItemStack(Items.COAL, 1));
        log("ElectricAlloySmelter: staged 1 iron + 2 coal (steel), bat start FE=" + smelterStartFe);

        // 6. Jaw Crusher (Tin via hand crank + testing 7 advanced metals)
        var crusher = (JawCrusherBlockEntity) level.getBlockEntity(CRUSHER_POS);
        player.setPos(CRUSHER_POS.getX()+.5, CRUSHER_POS.getY(), CRUSHER_POS.getZ()+1.5);
        if (!crusher.bindAutomation(player)) throw new AssertionError("QA machine authorization: crusher");
        var rawTin = new ItemStack(ModItems.RAW_MATERIALS.get("tin").get(), 1);
        boolean tinAccepted = crusher.insert(rawTin);
        boolean crank1 = crusher.manualStroke();
        List<String> rejectedAdvanced = new ArrayList<>();
        for (String metal : List.of("aluminum", "uranium", "osmium", "platinum", "tungsten", "titanium", "cobalt")) {
            ItemStack adv = new ItemStack(ModItems.RAW_MATERIALS.get(metal).get(), 1);
            if (!CrushingCatalog.isValidInput(adv)) rejectedAdvanced.add("raw_" + metal);
        }
        log("JawCrusher: raw_tin accepted=" + tinAccepted + ", crank1=" + crank1 + ", rejectedAdvanced=" + rejectedAdvanced);

        // 7. Electromechanical Press
        var pressBat = (VoltaicPileBlockEntity) level.getBlockEntity(PRESS_BAT_POS);
        pressBat.setStoredEnergy(500);
        var press = (ElectromechanicalPressBlockEntity) level.getBlockEntity(PRESS_POS);
        player.setPos(PRESS_POS.getX()+.5, PRESS_POS.getY(), PRESS_POS.getZ()+1.5);
        if (!press.bindAutomation(player)) throw new AssertionError("QA authorization: press");
        boolean pressIn = press.insertIngot(new ItemStack(ModItems.ALLOY_INGOTS.get("bronze").get(), 1));
        log("ElectromechanicalPress: bronze_ingot inserted=" + pressIn + ", bat FE=" + pressBat.energy());

        // 8. Wire Drawing Bench
        var wire = (WireDrawingBenchBlockEntity) level.getBlockEntity(WIRE_POS);
        player.setPos(WIRE_POS.getX()+.5, WIRE_POS.getY(), WIRE_POS.getZ()+1.5);
        if (!wire.bindAutomation(player)) throw new AssertionError("QA authorization: wire");
        boolean wireIn = wire.insert(new ItemStack(ModItems.ALLOY_SHEETS.get("constantan").get(), 1));
        wire.manualStroke();
        log("WireDrawingBench: constantan_sheet inserted=" + wireIn);

        // 9. Mechanical Sawmill (inReach requires player within ~8 blocks during insert)
        player.connection.teleport(3.5, 101.0, 3.0, 0f, 20f);
        var sawmill = (MechanicalSawmillBlockEntity) level.getBlockEntity(SAWMILL_POS);
        boolean sawIn = sawmill.insert(player, new ItemStack(Items.OAK_LOG, 1));
        player.connection.teleport(8.5, 104.5, -5.0, 0f, 26f);
        log("MechanicalSawmill: oak_log inserted=" + sawIn);

        // 10. Manual Dynamo
        var dynamo = (ManualDynamoBlockEntity) level.getBlockEntity(DYNAMO_POS);
        dynamo.install(true);
        boolean dynamoTurn = dynamo.turn();
        log("ManualDynamo: installed crank + 1 turn=" + dynamoTurn + ", energy=" + dynamo.energy());

        // 11. Battery ping-pong setup
        var pingPile = (VoltaicPileBlockEntity) level.getBlockEntity(PING_PILE_POS);
        var pingAccum = (LeadAcidAccumulatorBlockEntity) level.getBlockEntity(PING_ACCUM_POS);
        pingPile.setStoredEnergy(1000);
        pingAccum.setStoredEnergy(0);
        pingStartTotalFe = pingPile.energy() + pingAccum.energy();

        // 12. LoadRelay + LeadAcidAccumulator test
        var relayAccum = (LeadAcidAccumulatorBlockEntity) level.getBlockEntity(RELAY_ACCUM_POS);
        relayAccum.setStoredEnergy(45000); // 90% (>=80% should trigger alarm if supported)
        var relay = (LoadRelayBlockEntity) level.getBlockEntity(RELAY_POS);
        relay.receiveEnergy(48);

        // 13. Pneumatic Ejector + Chest + Tube + Sorter
        if (level.getBlockEntity(CHEST_IN_POS) instanceof ChestBlockEntity chestIn) {
            chestIn.setItem(0, new ItemStack(Items.IRON_INGOT, 4));
        }
        var ejectorBat = (VoltaicPileBlockEntity) level.getBlockEntity(EJECTOR_POS.south());
        ejectorBat.setStoredEnergy(100);
        // Check ItemRouting against JawCrusher with a valid crusher input (raw_tin)
        ItemStack testProbe = new ItemStack(ModItems.RAW_MATERIALS.get("tin").get(), 1);
        boolean routingIntoCrusher = ItemRouting.insert(level, CRUSHER_POS, testProbe, Direction.NORTH);
        log("ItemRouting -> JawCrusher insert supported: " + routingIntoCrusher);

        // 14. Settling Vat (water + sulfur_dust + glass_bottle) + break drop bug
        var vat = (SettlingVatBlockEntity) level.getBlockEntity(VAT_POS);
        player.setPos(VAT_POS.getX()+.5, VAT_POS.getY(), VAT_POS.getZ()+1.5);
        if (!vat.bindAutomation(player)) throw new AssertionError("QA authorization: vat");
        try (var tx = Transaction.openRoot()) {
            vat.tank().insert(FluidResource.of(Fluids.WATER), 1000, tx);
            tx.commit();
        }
        vat.addIngredient(new ItemStack(ModItems.CHEMICAL_PRODUCTS.get("sulfur").get(), 1));
        vat.addIngredient(new ItemStack(Items.GLASS_BOTTLE, 1));

        var vatBreak = (SettlingVatBlockEntity) level.getBlockEntity(VAT_BREAK_POS);
        player.setPos(VAT_BREAK_POS.getX()+.5, VAT_BREAK_POS.getY(), VAT_BREAK_POS.getZ()+1.5);
        if (!vatBreak.bindAutomation(player)) throw new AssertionError("QA authorization: vatBreak");
        vatBreak.addIngredient(new ItemStack(ModItems.CHEMICAL_PRODUCTS.get("sulfur").get(), 1));
        level.destroyBlock(VAT_BREAK_POS, true);
        List<String> vatBreakDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(VAT_BREAK_POS).inflate(2))
                .stream().map(e -> BuiltInRegistries.ITEM.getKey(e.getItem().getItem()) + "x" + e.getItem().getCount())
                .toList();
        log("SettlingVat break after inserting sulfur_dust dropped: " + vatBreakDrops);
    }

    private static void verifyDelayedResults(ServerLevel level, ServerPlayer player) {
        try {
            // Brazier output & HeatedIngotItem cooling check
            var brazier = (BellowsBrazierBlockEntity) level.getBlockEntity(BRAZIER_POS);
            ItemStack brazierOut = brazier.take(false);
            boolean hasInventoryTick = false;
            try {
                ModItems.HEATED_IRON_INGOT.get().getClass().getDeclaredMethod(
                        "inventoryTick", ItemStack.class, ServerLevel.class,
                        net.minecraft.world.entity.Entity.class, EquipmentSlot.class);
                hasInventoryTick = true;
            } catch (NoSuchMethodException ignored) {}
            boolean cooledInSlot = false;
            if (!brazierOut.isEmpty()) {
                var expired = brazierOut.copy();
                var tag = new net.minecraft.nbt.CompoundTag();
                tag.putLong("hot_until", level.getServer().overworld().getGameTime() - 1);
                expired.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                        net.minecraft.world.item.component.CustomData.of(tag));
                player.setItemSlot(EquipmentSlot.OFFHAND, expired);
                expired.inventoryTick(level, player, EquipmentSlot.OFFHAND);
                cooledInSlot = player.getOffhandItem().is(Items.IRON_INGOT);
                player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            }
            log("BellowsBrazier after 150 ticks: output=" + brazierOut + ", hot="
                    + (!brazierOut.isEmpty() && HeatTreatment.isHot(brazierOut, level))
                    + ", HeatedIngotItem.inventoryTick=" + hasInventoryTick
                    + ", cooledWhenExpired=" + cooledInSlot);

            // Alloy Kiln output
            var kiln = (BellowsAlloyKilnBlockEntity) level.getBlockEntity(KILN_POS);
            ItemStack kilnOut = kiln.take(false);
            log("BellowsAlloyKiln after 150 ticks: output=" + kilnOut);

            // Electric Alloy Smelter output & FE consumed
            var smelter = (ElectricAlloySmelterBlockEntity) level.getBlockEntity(SMELTER_POS);
            var smelterBat = (LeadAcidAccumulatorBlockEntity) level.getBlockEntity(SMELTER_BAT_POS);
            ItemStack smelterOut = smelter.extractResult(false);
            int actualFeSpent = smelterStartFe - (smelterBat.energy() + smelter.energy());
            log("ElectricAlloySmelter after 150 ticks: output=" + smelterOut
                    + ", actualFeSpent=" + actualFeSpent + " (AlloyCatalog steel totalFe=160)");

            // Electromechanical Press output
            var press = (ElectromechanicalPressBlockEntity) level.getBlockEntity(PRESS_POS);
            log("ElectromechanicalPress after 150 ticks: output=" + press.extractResult(false));

            // Manual Grain Mill output
            var millBe = (ManualGrainMillBlockEntity) level.getBlockEntity(MILL_POS);
            log("ManualGrainMill after 4 strokes: take=" + millBe.take());

            // Jaw Crusher output
            var crusher = (JawCrusherBlockEntity) level.getBlockEntity(CRUSHER_POS);
            log("JawCrusher after crank strokes: progress=" + crusher.progress() + ", take=" + crusher.take(false));

            // Wire Drawing Bench
            var wire = (WireDrawingBenchBlockEntity) level.getBlockEntity(WIRE_POS);
            log("WireDrawingBench after strokes: take=" + wire.take(false));

            // Mechanical Sawmill & Water Wheel + Shafts synchronization + reversal test
            var wheel = (PrimitiveWaterWheelBlockEntity) level.getBlockEntity(WHEEL_POS);
            var shaft1 = (MechanicalShaftBlockEntity) level.getBlockEntity(new BlockPos(1, 100, 5));
            var shaft2 = (MechanicalShaftBlockEntity) level.getBlockEntity(new BlockPos(2, 100, 5));
            var sawmill = (MechanicalSawmillBlockEntity) level.getBlockEntity(SAWMILL_POS);
            boolean allRotatingNorth = wheel.rotating() && shaft1.rotating() && shaft2.rotating() && sawmill.rotating();
            int dirNorthWheel = wheel.spinDirection();
            boolean dirSyncedNorth = dirNorthWheel == shaft1.spinDirection()
                    && dirNorthWheel == shaft2.spinDirection()
                    && dirNorthWheel == sawmill.spinDirection();

            // Flip water feed from North to South and tick all 4 nodes to verify direction reversal
            level.setBlock(WHEEL_POS.below().north(), Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(WHEEL_POS.below().south(), Blocks.WATER.defaultBlockState(), 3);
            MechanicalNetwork.invalidate(level);
            wheel.tick();
            shaft1.tick();
            shaft2.tick();
            sawmill.tick();
            int dirSouthWheel = wheel.spinDirection();
            boolean dirSyncedSouth = dirSouthWheel == shaft1.spinDirection()
                    && dirSouthWheel == shaft2.spinDirection()
                    && dirSouthWheel == sawmill.spinDirection()
                    && dirSouthWheel != dirNorthWheel;

            log("MechanicalSawmill after 150 ticks: rotating=" + sawmill.rotating()
                    + ", output=" + sawmill.output()
                    + ", allRotating=" + allRotatingNorth
                    + ", northDir=" + dirNorthWheel + " synced=" + dirSyncedNorth
                    + ", southDir=" + dirSouthWheel + " synced=" + dirSyncedSouth);

            // Settling Vat output
            var vat = (SettlingVatBlockEntity) level.getBlockEntity(VAT_POS);
            ItemStack vatOut1 = vat.collect();
            ItemStack vatOut2 = vat.collect();
            log("SettlingVat after 150 ticks: collect1=" + vatOut1 + ", collect2=" + vatOut2);

            // Battery Ping-Pong loss
            var pingPile = (VoltaicPileBlockEntity) level.getBlockEntity(PING_PILE_POS);
            var pingAccum = (LeadAcidAccumulatorBlockEntity) level.getBlockEntity(PING_ACCUM_POS);
            int pingEndTotal = pingPile.energy() + pingAccum.energy();
            log("Battery Ping-Pong (VoltaicPile <-> 4x CopperBusbar <-> LeadAcidAccumulator): start="
                    + pingStartTotalFe + " FE, end=" + pingEndTotal + " FE, lost=" + (pingStartTotalFe - pingEndTotal) + " FE");

            // LoadRelay + LeadAcidAccumulator
            boolean relayAlarmOn = level.getBlockState(RELAY_POS).getValue(BlockStateProperties.POWERED);
            log("LoadRelay adjacent to 90%-charged LeadAcidAccumulator: POWERED alarm=" + relayAlarmOn);

            // Pneumatic Chest Out
            String chestOutStr = "empty";
            if (level.getBlockEntity(CHEST_OUT_POS) instanceof ChestBlockEntity chestOut) {
                chestOutStr = chestOut.getItem(0).toString();
            }
            log("PneumaticTube + Ejector + Sorter -> Output Chest slot 0: " + chestOutStr);

            // Runtime RecipeManager & LootTable checks
            var rm = level.getServer().getRecipeManager();
            boolean hasAccumRecipe = rm.byKey(ResourceKey.create(Registries.RECIPE,
                    Identifier.fromNamespaceAndPath("quantumindustries", "lead_acid_accumulator"))).isPresent();
            boolean hasFlameRecipe = rm.byKey(ResourceKey.create(Registries.RECIPE,
                    Identifier.fromNamespaceAndPath("quantumindustries", "flame_test_kit"))).isPresent();
            log("Runtime RecipeManager: lead_acid_accumulator present=" + hasAccumRecipe
                    + ", flame_test_kit present=" + hasFlameRecipe);

            // Break scientific_analyzer (Tier 2) and scientific_analyzer_tier_1 to verify loot tables
            level.destroyBlock(TIER1_POS, true);
            level.destroyBlock(TIER2_POS, true);
            var t1Drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(TIER1_POS).inflate(0.6))
                    .stream().map(e -> BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).toString()).toList();
            var t2Drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(TIER2_POS).inflate(0.6))
                    .stream().map(e -> BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).toString()).toList();
            log("Break LootTable test: scientific_analyzer_tier_1 drops=" + t1Drops
                    + ", scientific_analyzer (Tier 2) drops=" + t2Drops);

            Path outPath = level.getServer().getServerDirectory().resolve("qa-report.txt");
            Files.write(outPath, REPORT);
            System.out.println("QA_REPORT_WRITTEN " + outPath.toAbsolutePath());
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            done = true;
        }
    }
}
