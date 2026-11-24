package br.com.quantumindustries.energy;

import br.com.quantumindustries.block.CopperCableBlock;
import br.com.quantumindustries.block.entity.CopperCableBlockEntity;
import br.com.quantumindustries.block.entity.LeadAcidAccumulatorBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = "quantumindustries")
public final class CableRuntimeVerification {
    private static final BlockPos POS = new BlockPos(8, 100, 8);
    private static boolean ran;

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void place(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof CopperCableBlock) {
            state = CopperCableBlock.computeState(level, pos, state);
        }
        level.setBlock(pos, state, Block.UPDATE_ALL);
    }

    private static void clear(ServerLevel level) {
        for (BlockPos pos : BlockPos.betweenClosed(POS.offset(-2, -4, -2), POS.offset(2, 4, 2))) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static void verifyMasks(ServerLevel level, CopperCableBlock cable) {
        Direction[] directions = Direction.values();
        for (int mask = 0; mask < 64; mask++) {
            clear(level);
            place(level, POS, cable.defaultBlockState());
            for (int i = 0; i < directions.length; i++) {
                if ((mask & (1 << i)) != 0) {
                    place(level, POS.relative(directions[i]), ModBlocks.INSULATED_CONDUIT.get().defaultBlockState());
                }
            }
            BlockState state = level.getBlockState(POS);
            var bounds = state.getShape(level, POS).bounds();
            var collision = state.getCollisionShape(level, POS).bounds();
            check(bounds.equals(collision), "collision and selection agree");
            for (int i = 0; i < directions.length; i++) {
                Direction direction = directions[i];
                boolean expected = (mask & (1 << i)) != 0;
                check(state.getValue(CopperCableBlock.PROPERTY_BY_DIRECTION.get(direction)) == expected,
                        "neighbor update " + direction + " mask=" + mask);
                double edge = switch (direction) {
                    case UP -> bounds.maxY; case DOWN -> bounds.minY;
                    case EAST -> bounds.maxX; case WEST -> bounds.minX;
                    case SOUTH -> bounds.maxZ; case NORTH -> bounds.minZ;
                };
                boolean reachesFace = edge == 0 || edge == 1;
                check(reachesFace == expected, "shape arm " + direction + " mask=" + mask);
            }
        }
        clear(level);
        place(level, POS, cable.defaultBlockState());
        place(level, POS.above().north(), cable.defaultBlockState());
        for (var property : CopperCableBlock.PROPERTY_BY_DIRECTION.values()) {
            check(!level.getBlockState(POS).getValue(property), "diagonal must not connect");
        }
        place(level, POS.above(), cable.defaultBlockState());
        check(level.getBlockState(POS).getValue(CopperCableBlock.UP), "stack connects up");
        check(level.getBlockState(POS.above()).getValue(CopperCableBlock.DOWN), "stack connects down");
        level.setBlock(POS.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        check(!level.getBlockState(POS).getValue(CopperCableBlock.UP), "removal disconnects up");
        // Simulate a saved state with no connection properties populated yet.
        place(level, POS.below(), cable.defaultBlockState());
        level.setBlock(POS, cable.defaultBlockState(), Block.UPDATE_CLIENTS);
        ((CopperCableBlockEntity) level.getBlockEntity(POS)).onLoad();
        check(CopperCableBlock.computeState(level, POS, cable.defaultBlockState()) == level.getBlockState(POS),
                "load rebuilds saved connections");
    }

    private static void verifyEnergy(ServerLevel level, CopperCableBlock cable, Direction direction) {
        clear(level);
        BlockPos targetPos = POS.relative(direction, 3);
        place(level, POS, ModBlocks.LEAD_ACID_ACCUMULATOR.get().defaultBlockState());
        place(level, targetPos, ModBlocks.LEAD_ACID_ACCUMULATOR.get().defaultBlockState());
        place(level, POS.relative(direction), cable.defaultBlockState());
        place(level, POS.relative(direction, 2), cable.defaultBlockState());
        var source = (LeadAcidAccumulatorBlockEntity) level.getBlockEntity(POS);
        var target = (LeadAcidAccumulatorBlockEntity) level.getBlockEntity(targetPos);
        source.setStoredEnergy(100);
        CableNetwork.transfer(level, POS, source::energy, source::extractEnergy, 100);
        int delivered = cable.insulated() ? 64 : 32;
        check(source.energy() == 100 - delivered && target.energy() == delivered,
                "vertical FE conservation and rate " + direction);
        var state = level.getBlockState(POS.relative(direction));
        check(state.getValue(CopperCableBlock.PROPERTY_BY_DIRECTION.get(direction.getOpposite())),
                "machine connection " + direction);
        level.setBlock(POS.relative(direction, 2), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        int before = target.energy();
        CableNetwork.transfer(level, POS, source::energy, source::extractEnergy, 100);
        check(target.energy() == before, "broken vertical network cannot transfer");
    }

    private static void verifyDynamoFaces(ServerLevel level) {
        Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        Direction[] ports = {Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH};
        for (int i = 0; i < facings.length; i++) {
            for (Direction side : Direction.values()) {
                for (int distance : new int[]{1, 2}) {
                    clear(level);
                    place(level, POS, ModBlocks.MANUAL_DYNAMO.get().defaultBlockState().setValue(
                            net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, facings[i]));
                    BlockPos targetPos = POS.relative(side, distance);
                    place(level, targetPos, ModBlocks.LEAD_ACID_ACCUMULATOR.get().defaultBlockState());
                    if (distance == 2) {
                        BlockPos wire = POS.relative(side);
                        place(level, wire, ModBlocks.INSULATED_CONDUIT.get().defaultBlockState());
                        check(level.getBlockState(wire).getValue(CopperCableBlock.PROPERTY_BY_DIRECTION.get(side.getOpposite()))
                                == (side == ports[i]), "dynamo visual port follows rotation");
                    }
                    var dynamo = (br.com.quantumindustries.block.entity.ManualDynamoBlockEntity) level.getBlockEntity(POS);
                    dynamo.install(true);
                    check(dynamo.turn(), "test dynamo generates a paid crank pulse");
                    dynamo.tick();
                    var target = (LeadAcidAccumulatorBlockEntity) level.getBlockEntity(targetPos);
                    check(target.energy() == (side == ports[i] ? 12 : 0), "dynamo FE face " + facings[i] + "/" + side + " distance=" + distance);
                    check(dynamo.energy() + target.energy() == 12, "port rejection preserves FE");
                }
            }
        }
    }

    private static void verifyIndustrialSwitch(ServerLevel level) {
        clear(level);
        var switchBlock = ModBlocks.INDUSTRIAL_CIRCUIT_SWITCH.get();
        var closedProperty = br.com.quantumindustries.block.IndustrialCircuitSwitchBlock.CLOSED;
        var redstoneProperty = br.com.quantumindustries.block.IndustrialCircuitSwitchBlock.REDSTONE_CONTROLLED;
        var facing = net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
        BlockPos sourcePos = POS.north(2), targetPos = POS.south(2), sidePos = POS.east();
        place(level, sourcePos, ModBlocks.LEAD_ACID_ACCUMULATOR.get().defaultBlockState());
        place(level, targetPos, ModBlocks.LEAD_ACID_ACCUMULATOR.get().defaultBlockState());
        place(level, sidePos, ModBlocks.LEAD_ACID_ACCUMULATOR.get().defaultBlockState());
        place(level, POS.north(), ModBlocks.INSULATED_CONDUIT.get().defaultBlockState());
        place(level, POS.south(), ModBlocks.INSULATED_CONDUIT.get().defaultBlockState());
        place(level, POS, switchBlock.defaultBlockState().setValue(facing, Direction.NORTH));
        var source = (LeadAcidAccumulatorBlockEntity) level.getBlockEntity(sourcePos);
        var target = (LeadAcidAccumulatorBlockEntity) level.getBlockEntity(targetPos);
        var side = (LeadAcidAccumulatorBlockEntity) level.getBlockEntity(sidePos);
        source.setStoredEnergy(300);
        CableNetwork.transfer(level, sourcePos, source::energy, source::extractEnergy, 100);
        check(target.energy() == 0 && source.energy() == 300, "open switch isolates circuit");
        level.setBlock(POS, level.getBlockState(POS).setValue(closedProperty, true), Block.UPDATE_ALL);
        CableNetwork.transfer(level, sourcePos, source::energy, source::extractEnergy, 100);
        check(target.energy() == 64 && source.energy() == 236, "closed switch transmits 64 FE");
        check(side.energy() == 0, "switch cannot deliver through a side without terminal");
        level.setBlock(POS, level.getBlockState(POS).setValue(closedProperty, false), Block.UPDATE_ALL);
        level.setBlock(sidePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        for (int z = -2; z <= 2; z++)
            place(level, POS.offset(1, 0, z), ModBlocks.INSULATED_CONDUIT.get().defaultBlockState());
        CableNetwork.transfer(level, sourcePos, source::energy, source::extractEnergy, 100);
        check(target.energy() == 128 && source.energy() == 172, "alternative route survives open switch");
        level.setBlock(POS, level.getBlockState(POS).setValue(redstoneProperty, true), Block.UPDATE_ALL);
        level.setBlock(POS.above(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        br.com.quantumindustries.block.IndustrialCircuitSwitchBlock.refreshRedstone(level, POS, level.getBlockState(POS));
        check(level.getBlockState(POS).getValue(closedProperty), "redstone closes switch");
        level.setBlock(POS.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        br.com.quantumindustries.block.IndustrialCircuitSwitchBlock.refreshRedstone(level, POS, level.getBlockState(POS));
        check(!level.getBlockState(POS).getValue(closedProperty), "redstone removal opens switch");
    }

    private static void verifyAnalyzerTopology(ServerLevel level){
        clear(level);
        var wire=ModBlocks.INSULATED_CONDUIT.get().defaultBlockState();
        var battery=ModBlocks.LEAD_ACID_ACCUMULATOR.get().defaultBlockState();
        place(level,POS,wire);
        place(level,POS.north(),wire);
        place(level,POS.north().east(),wire);
        place(level,POS.east(),battery);
        place(level,POS.south(2),battery);
        place(level,POS.south(),ModBlocks.INDUSTRIAL_CIRCUIT_SWITCH.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH));
        place(level,POS.west().north(2),wire);
        BlockPos analyzer=POS.above();
        place(level,analyzer,ModBlocks.ELECTRICAL_NETWORK_ANALYZER.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,Direction.UP));
        check(level.getBlockState(POS).getValue(CopperCableBlock.UP),"passive probe has visual contact");
        ((LeadAcidAccumulatorBlockEntity)level.getBlockEntity(POS.east())).setStoredEnergy(123);
        ((LeadAcidAccumulatorBlockEntity)level.getBlockEntity(POS.south(2))).setStoredEnergy(456);
        var open=ElectricalNetworkTelemetry.scan(level,analyzer,POS);
        check(open.connected()&&open.cables()==3&&open.endpoints()==1,"open switch and isolated cable excluded");
        check(open.stored()==123&&open.capacity()==50000,"shared endpoint counted once, no analyzer buffer");
        level.setBlock(POS.south(),level.getBlockState(POS.south()).setValue(
                br.com.quantumindustries.block.IndustrialCircuitSwitchBlock.CLOSED,true),Block.UPDATE_ALL);
        var closed=ElectricalNetworkTelemetry.scan(level,analyzer,POS);
        check(closed.cables()==4&&closed.endpoints()==2&&closed.stored()==579,"closed switch expands measured scope");
        check(closed.nodes().size()==6,"map contains only reachable nodes");
        check(closed.busiest().size()==4,"bottlenecks are cable nodes only");
    }

    private static void verifyBatteryBudget(ServerLevel level) {
        clear(level);
        place(level, POS, ModBlocks.VOLTAIC_PILE.get().defaultBlockState());
        var pile = (br.com.quantumindustries.block.entity.VoltaicPileBlockEntity) level.getBlockEntity(POS);
        check(pile.receiveEnergy(30) == 30 && pile.receiveEnergy(30) == 18
                && pile.receiveEnergy(1) == 0, "pile input limit shared across calls");
        pile.setStoredEnergy(100);
        check(pile.extractEnergy(30) == 30 && pile.extractEnergy(30) == 18
                && pile.extractEnergy(1) == 0 && pile.energy() == 52, "pile output limit shared across calls");
        clear(level);
        place(level, POS, ModBlocks.LEAD_ACID_ACCUMULATOR.get().defaultBlockState());
        var accumulator = (LeadAcidAccumulatorBlockEntity) level.getBlockEntity(POS);
        check(accumulator.receiveEnergy(200) == 200 && accumulator.receiveEnergy(100) == 56
                && accumulator.receiveEnergy(1) == 0, "accumulator input limit shared across calls");
        accumulator.setStoredEnergy(500);
        check(accumulator.extractEnergy(200) == 200 && accumulator.extractEnergy(100) == 56
                && accumulator.extractEnergy(1) == 0 && accumulator.energy() == 244, "accumulator output limit shared across calls");
    }

    @SubscribeEvent public static void tick(ServerTickEvent.Post event) throws Exception {
        if (!Boolean.getBoolean("quantumindustries.verifyCables") || ran) return;
        ServerLevel level = event.getServer().overworld();
        if (level.getGameTime() < 10) return;
        ran = true;
        String result;
        try {
            level.setChunkForced(0, 0, true);
            verifyDynamoFaces(level);
            for (var cable : new CopperCableBlock[]{ModBlocks.COPPER_BUSBAR.get(), ModBlocks.INSULATED_CONDUIT.get()}) {
                verifyMasks(level, cable);
                verifyEnergy(level, cable, Direction.UP);
                verifyEnergy(level, cable, Direction.DOWN);
            }
            verifyIndustrialSwitch(level);
            verifyBatteryBudget(level);
            verifyAnalyzerTopology(level);
            result = "PASS: both cables, 128 masks, shapes, neighbor updates, diagonals, reload, vertical FE, switch, battery budgets and analyzer topology\n";
        } catch (Throwable error) {
            error.printStackTrace();
            result = "FAIL: " + error + "\n";
        }
        Files.writeString(Path.of("../result.txt"), result);
        event.getServer().halt(false);
    }
}
