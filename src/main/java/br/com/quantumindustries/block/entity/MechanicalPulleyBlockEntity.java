package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.mechanical.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class MechanicalPulleyBlockEntity extends RotationalBlockEntity {
    public static final int MIN_DISTANCE = BeltGeometry.MIN_DISTANCE, MAX_DISTANCE = BeltGeometry.MAX_DISTANCE;
    public static final double BELT_EFFICIENCY = .94;
    private BlockPos partnerPos;
    private boolean crossed;
    public MechanicalPulleyBlockEntity(BlockPos pos, BlockState state) { super(ModBlocks.MECHANICAL_PULLEY_ENTITY.get(), pos, state); }
    @Override public Direction.Axis axle() { return getBlockState().getValue(BlockStateProperties.AXIS); }
    @Override public boolean allowsCrank() { return false; }
    @Override public int sourceCapacity() { return 0; }
    @Override public double kineticInertia() { return 1.2; }
    @Override public double kineticDrag() { return .06; }
    @Override public double staticFrictionTorque() { return .04; }
    public BlockPos partnerPos() { return partnerPos; }
    public boolean crossed() { return crossed; }
    public static boolean validAlignment(BlockPos a, BlockPos b, Direction.Axis axis) {
        return BeltGeometry.validAlignment(a, b, axis);
    }
    public void setPartner(BlockPos pos, boolean crossed) {
        if (!(level instanceof ServerLevel)) throw new IllegalStateException("Server owns belts");
        partnerPos = pos.immutable();
        this.crossed = crossed;
        changed();
    }
    public boolean validPartner(MechanicalPulleyBlockEntity other) {
        return other != this && axle() == other.axle() && validAlignment(worldPosition, other.worldPosition, axle())
                && other.worldPosition.equals(partnerPos) && worldPosition.equals(other.partnerPos) && crossed == other.crossed;
    }
    @Override public List<KineticLink> kineticLinks(ServerLevel level, BlockPos pos) {
        var links = new ArrayList<KineticLink>(super.kineticLinks(level, pos));
        if (partnerPos != null && level.hasChunkAt(partnerPos)
                && level.getBlockEntity(partnerPos) instanceof MechanicalPulleyBlockEntity other && validPartner(other))
            links.add(new KineticLink(partnerPos, crossed ? -1 : 1, BELT_EFFICIENCY));
        return List.copyOf(links);
    }
    public boolean unlink(boolean refund) {
        if (!(level instanceof ServerLevel server) || partnerPos == null) return false;
        // Retain an unloaded endpoint until it can be cleared atomically; never duplicate its refund.
        if (!server.hasChunkAt(partnerPos)) return false;
        BlockPos old = partnerPos;
        partnerPos = null;
        crossed = false;
        changed();
        if (server.getBlockEntity(old) instanceof MechanicalPulleyBlockEntity other && worldPosition.equals(other.partnerPos)) {
            other.partnerPos = null;
            other.crossed = false;
            other.changed();
        }
        if (refund) Block.popResource(server, worldPosition, new ItemStack(ModItems.DRIVE_BELT.get()));
        return true;
    }
    @Override public void tickTransmission() {
        if (level instanceof ServerLevel server && partnerPos != null && server.hasChunkAt(partnerPos)
                && (!(server.getBlockEntity(partnerPos) instanceof MechanicalPulleyBlockEntity other) || !validPartner(other))) unlink(true);
        super.tickTransmission();
    }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        unlink(true);
        super.preRemoveSideEffects(pos, state);
    }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putBoolean("has_partner", partnerPos != null);
        out.putBoolean("belt_crossed", crossed);
        if (partnerPos != null) {
            var partner = out.child("pulley_partner");
            partner.putInt("X", partnerPos.getX()); partner.putInt("Y", partnerPos.getY()); partner.putInt("Z", partnerPos.getZ());
        }
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        var partner = in.childOrEmpty("pulley_partner");
        partnerPos = in.getBooleanOr("has_partner", false) ? new BlockPos(partner.getIntOr("X", 0), partner.getIntOr("Y", 0), partner.getIntOr("Z", 0)) : null;
        crossed = in.getBooleanOr("belt_crossed", false);
    }
}
