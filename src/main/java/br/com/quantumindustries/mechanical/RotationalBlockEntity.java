package br.com.quantumindustries.mechanical;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.research.WorldResearchData;
import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
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
public abstract class RotationalBlockEntity extends BlockEntity implements GeoBlockEntity, MechanicalParticipant {
    public static final int MANUAL_CAPACITY = 6, CRANK_INTERVAL = 10, CRANK_TICKS = 20;
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private boolean crankInstalled;
    private long manualUntil;
    private long lastTurn = Long.MIN_VALUE / 2;
    private boolean rotating;
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
    public boolean rotating() { return rotating; }
    public int spinDirection() { return spinDirection; }
    protected boolean isCutting() { return false; }

    public float rotation(double time) {
        if (!rotating) return ((angle % 360f) + 360f) % 360f;
        double raw = (angle + Math.max(0, time - angleTime) * 9.0 * spinDirection) % 360.0;
        return (float) ((raw + 360.0) % 360.0);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<RotationalBlockEntity>("mechanical_controller", 0, test -> {
            if (!rotating()) return test.setAndContinue(RawAnimation.begin().thenLoop("idle"));
            if (isCutting()) {
                return test.setAndContinue(RawAnimation.begin().thenLoop(spinDirection < 0 ? "cutting_ccw" : "cutting_cw"));
            }
            return test.setAndContinue(RawAnimation.begin().thenLoop(spinDirection < 0 ? "running_ccw" : "running_cw"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

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
        int normalized = direction < 0 ? -1 : 1;
        if (value == rotating && (!value || normalized == spinDirection)) return;
        if (value) {
            // Phase-lock active rotation to world gameTime so connected components stay in sync.
            angle = 0f;
            angleTime = 0L;
        } else {
            angle = rotation(level.getGameTime());
            angleTime = level.getGameTime();
        }
        spinDirection = normalized;
        rotating = value;
        changed();
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
        out.putBoolean("rotating", rotating);
        out.putInt("spin_direction", spinDirection);
        out.putFloat("rotation_angle", rotating ? 0f : (level == null ? angle : rotation(level.getGameTime())));
        out.putLong("rotation_time", rotating ? 0L : (level == null ? angleTime : level.getGameTime()));
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        crankInstalled = in.getBooleanOr("crank_installed", false);
        manualUntil = in.getLongOr("manual_until", 0);
        lastTurn = in.getLongOr("last_turn", Long.MIN_VALUE / 2);
        rotating = in.getBooleanOr("rotating", false);
        spinDirection = in.getIntOr("spin_direction", 1) < 0 ? -1 : 1;
        angle = in.getFloatOr("rotation_angle", 0);
        angleTime = in.getLongOr("rotation_time", 0);
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
