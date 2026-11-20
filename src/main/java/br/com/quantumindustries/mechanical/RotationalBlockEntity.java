package br.com.quantumindustries.mechanical;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.research.WorldResearchData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Shared movement state, not FE storage. A crank pulse supplies time, never instant work. */
public abstract class RotationalBlockEntity extends BlockEntity implements MechanicalParticipant {
    public static final int MANUAL_CAPACITY = 6, CRANK_INTERVAL = 10, CRANK_TICKS = 20;
    private boolean crankInstalled;
    private long manualUntil;
    private long lastTurn = Long.MIN_VALUE / 2;
    private boolean driven;
    private double angleRadians, angularVelocity, angularAcceleration;
    private long kineticSyncTime;
    private double gearRatio = 1.0;
    private boolean jammed;
    private StopReason stopReason = StopReason.NO_SOURCE;
    private int spinDirection = 1;
    private float angle;
    private long angleTime;

    protected RotationalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    public abstract Direction.Axis axle();
    public int sourceCapacity() { return level != null && crankInstalled && level.getGameTime() < manualUntil ? MANUAL_CAPACITY : 0; }
    public int sourceDirection() { return 1; }
    public int requestedLoad() { return 0; }
    public boolean allowsCrank() { return true; }
    public boolean installed() { return crankInstalled; }
    public boolean rotating() { return driven; }
    public int spinDirection() { return spinDirection; }
    protected boolean isCutting() { return false; }

    public boolean isSpinning() { return Math.abs(angularVelocity) > 1e-3; }
    public double angleRadians() { return angleRadians; }
    public double angularVelocity() { return angularVelocity; }
    public double angularAcceleration() { return angularAcceleration; }
    public double rpm() { return KineticDynamics.toRpm(angularVelocity); }
    public double gearRatio() { return gearRatio; }
    public boolean jammed() { return jammed; }
    public StopReason stopReason() { return stopReason; }
    public double interpolatedAngleRadians(double time) {
        double dt = Math.clamp((time - kineticSyncTime) * .05, 0.0, .5);
        // Do not extrapolate braking through zero into an uncommanded reversal.
        if (angularVelocity * angularAcceleration < 0) dt = Math.min(dt, -angularVelocity / angularAcceleration);
        return angleRadians + angularVelocity * dt + .5 * angularAcceleration * dt * dt;
    }
    public float rotation(double time) {
        return (float) Math.toDegrees(KineticDynamics.wrap(interpolatedAngleRadians(time)));
    }
    public void applyClientSync(KineticSyncPayload payload) {
        StopReason[] reasons = StopReason.values();
        int ordinal = Byte.toUnsignedInt(payload.stopReasonOrdinal());
        StopReason reason = ordinal < reasons.length ? reasons[ordinal] : StopReason.NO_SOURCE;
        applyClientSync(payload.serverGameTime(), payload.angleRadians(), payload.angularVelocity(),
                payload.angularAcceleration(), payload.gearRatio(), payload.powered(), payload.jammed(), reason);
    }
    public void applyClientSync(long time, double angle, double omega, double alpha, double ratio,
            boolean powered, boolean blocked, StopReason reason) {
        angleRadians = angle; angularVelocity = omega; angularAcceleration = alpha;
        kineticSyncTime = time; gearRatio = ratio; driven = powered; jammed = blocked; stopReason = reason;
        if (Math.abs(omega) > 1e-6) spinDirection = omega < 0 ? -1 : 1;
    }
    @Override public void applyKineticSnapshot(KineticSnapshot snap) {
        boolean transition = driven != snap.powered() || spinDirection != snap.spinDirection();
        double oldAngle = angleRadians, oldOmega = angularVelocity;
        applyClientSync(snap.serverGameTime(), snap.angleRadians(), snap.angularVelocity(),
                snap.angularAcceleration(), snap.gearRatio(), snap.powered(), snap.jammed(), snap.stopReason());
        spinDirection = snap.spinDirection();
        if (transition || oldOmega != 0 && angularVelocity == 0 || snap.serverGameTime() % 20 == 0
                && (Math.abs(oldAngle - angleRadians) > 1e-6 || Math.abs(oldOmega - angularVelocity) > 1e-6)) setChanged();
    }
    @Override public double persistedAngleRadians() { return angleRadians; }
    @Override public double persistedAngularVelocity() { return angularVelocity; }


    public boolean inReach(Player player) {
        return level instanceof ServerLevel && !isRemoved() && player.level() == level
                && player.distanceToSqr(worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5) <= 64;
    }
    public boolean authorized(Player player) {
        return inReach(player) && WorldResearchData.get((ServerLevel) level).knows(player, "mechanical_transmission");
    }
    public boolean installCrank(Player player, ItemStack held) {
        if (!authorized(player) || !allowsCrank() || crankInstalled || !held.is(ModItems.HAND_CRANK.get())) return false;
        held.shrink(1);
        crankInstalled = true;
        changed();
        return true;
    }
    public boolean turn(Player player) {
        if (!authorized(player) || !crankInstalled || level.getGameTime() - lastTurn < CRANK_INTERVAL) return false;
        lastTurn = level.getGameTime();
        manualUntil = lastTurn + CRANK_TICKS;
        changed();
        return true;
    }
    public ItemStack detachCrank(Player player) {
        if (!inReach(player) || !crankInstalled) return ItemStack.EMPTY;
        crankInstalled = false;
        manualUntil = 0;
        setRotating(false);
        changed();
        return new ItemStack(ModItems.HAND_CRANK.get());
    }
    protected void setRotating(boolean value) {
        setMotion(value, spinDirection);
    }

    protected void setMotion(boolean value, int direction) {
        if (level == null) return;
        driven = value;
        spinDirection = direction < 0 ? -1 : 1;
        setChanged();
    }
    protected void changed() {
        setChanged();
        if (level instanceof ServerLevel server) MechanicalNetwork.invalidate(server);
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putBoolean("crank_installed", crankInstalled);
        out.putLong("manual_until", manualUntil);
        out.putLong("last_turn", lastTurn);
        out.putBoolean("rotating", driven);
        out.putDouble("qks_angle", angleRadians);
        out.putDouble("qks_omega", angularVelocity);
        out.putInt("spin_direction", spinDirection);
        out.putFloat("rotation_angle", driven ? 0f : (level == null ? angle : rotation(level.getGameTime())));
        out.putLong("rotation_time", driven ? 0L : (level == null ? angleTime : level.getGameTime()));
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        crankInstalled = in.getBooleanOr("crank_installed", false);
        manualUntil = in.getLongOr("manual_until", 0);
        lastTurn = in.getLongOr("last_turn", Long.MIN_VALUE / 2);
        driven = in.getBooleanOr("rotating", false);
        spinDirection = in.getIntOr("spin_direction", 1) < 0 ? -1 : 1;
        angle = in.getFloatOr("rotation_angle", 0);
        angleTime = in.getLongOr("rotation_time", 0);
        angleRadians = in.getDoubleOr("qks_angle", Math.toRadians(angle));
        angularVelocity = in.getDoubleOr("qks_omega", 0);
        kineticSyncTime = level == null ? angleTime : level.getGameTime();
    }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server) MechanicalNetwork.invalidate(server);
        if (level instanceof ServerLevel server && crankInstalled) {
            crankInstalled = false;
            manualUntil = 0;
            Block.popResource(server, pos, new ItemStack(ModItems.HAND_CRANK.get()));
        }
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
    @Override public void setRemoved() {
        if (level instanceof ServerLevel server) MechanicalNetwork.invalidate(server);
        super.setRemoved();
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
