package br.com.quantumindustries.thermal;

import br.com.quantumindustries.energy.ThermalExchange;
import br.com.quantumindustries.energy.ThermoelectricModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;

public final class ThermalNetwork {
    private static final Direction[] FACES = Direction.values();
    private static final Direction[] CANONICAL_FACES = {Direction.UP, Direction.SOUTH, Direction.EAST};

    private ThermalNetwork() {}

    public static int ambientAt(ServerLevel level, BlockPos pos) {
        boolean nether = level.dimension().equals(Level.NETHER);
        if (loadedChunk(level, pos) == null) {
            return nether ? ThermalExchange.ambient(0, true) : ThermoelectricModel.AMBIENT;
        }
        int baseline = ThermalExchange.ambient(level.getBiome(pos).value().getBaseTemperature(), nether);
        int excess = 0;
        for (Direction face : FACES) {
            BlockPos adjacent = pos.relative(face);
            LevelChunk chunk = loadedChunk(level, adjacent);
            if (chunk == null) continue;
            excess += contactTemperature(chunk.getBlockState(adjacent), baseline) - baseline;
        }
        return baseline + Math.round((float) excess / FACES.length);
    }

    private static int contactTemperature(BlockState state, int baseline) {
        if (state.getFluidState().is(FluidTags.LAVA)) return ThermoelectricModel.LAVA;
        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) return ThermoelectricModel.FIRE;
        if (state.is(Blocks.MAGMA_BLOCK)) return ThermoelectricModel.MAGMA;
        if (state.getValueOrElse(BlockStateProperties.LIT, false)
                && (state.is(BlockTags.CAMPFIRES) || state.is(Blocks.FURNACE)
                || state.is(Blocks.BLAST_FURNACE) || state.is(Blocks.SMOKER))) {
            return ThermoelectricModel.LIT_APPLIANCE;
        }
        return baseline;
    }

    /** Each ticking participant owns the three pairs on its positive axes. */
    public static void stepNeighbors(ServerLevel level, BlockPos pos, ThermalParticipant self) {
        LevelChunk ownerChunk = loadedChunk(level, pos);
        if (ownerChunk == null) return;
        BlockEntity owner = ownerChunk.getBlockEntities().get(pos);
        if (owner == null || owner != self || owner.isRemoved()) return;
        ThermalBody body = self.thermalBody();
        long tick = level.getGameTime();
        if (!body.beginNeighborStep(tick)) return;
        for (Direction face : CANONICAL_FACES) {
            if (!self.connectsThermal(face)) continue;
            BlockPos adjacent = pos.relative(face);
            LevelChunk neighborChunk = loadedChunk(level, adjacent);
            if (neighborChunk == null) continue;
            BlockEntity entity = neighborChunk.getBlockEntities().get(adjacent);
            Direction opposite = face.getOpposite();
            if (!(entity instanceof ThermalParticipant other) || entity.isRemoved() || !other.connectsThermal(opposite)) {
                continue;
            }
            int conductance = Math.min(self.thermalContactConductanceMilli(face),
                    other.thermalContactConductanceMilli(opposite));
            ThermalBody otherBody = other.thermalBody();
            otherBody.beginTick(tick);
            long flow = body.transferTo(otherBody, conductance);
            if (flow == 0) continue;
            body.recordEdge(face, adjacent, -flow);
            otherBody.recordEdge(opposite, pos, flow);
            ownerChunk.markUnsaved();
            neighborChunk.markUnsaved();
        }
    }

    private static LevelChunk loadedChunk(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getZ()));
    }
}
