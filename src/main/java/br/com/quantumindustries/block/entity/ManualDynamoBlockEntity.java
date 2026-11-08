package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.core.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

public final class ManualDynamoBlockEntity extends BlockEntity implements EnergyInspectable, GeoBlockEntity {
    public static final int CAPACITY = 240;
    public static final int PER_TURN = 12;
    public static final int MAX_EXTRACT = 24;
    public static final int TURN_TICKS = 8;
    public static final int TURNING_GRACE_TICKS = 5;

    private final SimpleEnergyHandler battery = new SimpleEnergyHandler(CAPACITY, PER_TURN, MAX_EXTRACT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean installed;
    private boolean refundable;
    private long lastTurn = -(TURN_TICKS + TURNING_GRACE_TICKS);

    public ManualDynamoBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MANUAL_DYNAMO_ENTITY.get(), pos, state);
    }

    @Override public int energy() { return (int) battery.getAmountAsLong(); }
    @Override public int capacity() { return CAPACITY; }
    @Override public Component inspectTitle() {
        return Component.translatable("block.quantumindustries.manual_dynamo");
    }

    public boolean installed() { return installed; }
    public long lastTurn() { return lastTurn; }
    public boolean isTurning() {
        if (level == null) return false;
        return (level.getGameTime() - lastTurn) < (TURN_TICKS + TURNING_GRACE_TICKS);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<ManualDynamoBlockEntity>("crank_controller", 0, test -> {
            if (this.isTurning()) {
                return test.setAndContinue(RawAnimation.begin().thenLoop("cranking"));
            }
            return test.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public boolean install(boolean paid) {
        if (!(level instanceof ServerLevel) || installed) return false;
        installed = true;
        refundable = paid;
        changed();
        return true;
    }

    /** Clears the installed state before the caller returns or drops the item. */
    public boolean detach() {
        if (!(level instanceof ServerLevel) || !installed) return false;
        installed = false;
        boolean paid = refundable;
        refundable = false;
        changed();
        return paid;
    }

    public boolean turn() {
        if (!(level instanceof ServerLevel server) || !installed || energy() >= CAPACITY) return false;
        long now = server.getGameTime();
        if (now - lastTurn < TURN_TICKS) return false;
        try (var transaction = Transaction.openRoot()) {
            if (battery.insert(PER_TURN, transaction) == 0) return false;
            transaction.commit();
        }
        lastTurn = now;
        changed();
        return true;
    }

    public int extractEnergy(int maxExtract) {
        if (!(level instanceof ServerLevel) || maxExtract <= 0 || energy() <= 0) return 0;
        int extracted;
        try (var transaction = Transaction.openRoot()) {
            extracted = battery.extract(Math.min(MAX_EXTRACT, maxExtract), transaction);
            if (extracted > 0) transaction.commit();
        }
        if (extracted > 0) changed();
        return extracted;
    }

    public void tick() {
        if (!(level instanceof ServerLevel server) || energy() <= 0) return;
        br.com.quantumindustries.energy.CableNetwork.transfer(server, worldPosition,
                this::energy, this::extractEnergy, MAX_EXTRACT);
        for (Direction dir : Direction.values()) {
            if (energy() <= 0) break;
            if (server.getBlockEntity(worldPosition.relative(dir)) instanceof EnergyInspectable target
                    && !(target instanceof ManualDynamoBlockEntity)) {
                int available = Math.min(MAX_EXTRACT, energy());
                int accepted = target.receiveEnergy(available);
                if (accepted > 0) extractEnergy(accepted);
            }
        }
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("stored_fe", energy());
        output.putBoolean("crank_installed", installed);
        output.putBoolean("crank_refundable", refundable);
        output.putLong("last_turn", lastTurn);
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        battery.set(Math.clamp(input.getIntOr("stored_fe", 0), 0, CAPACITY));
        installed = input.getBooleanOr("crank_installed", false);
        refundable = installed && input.getBooleanOr("crank_refundable", false);
        lastTurn = input.getLongOr("last_turn", -(TURN_TICKS + TURNING_GRACE_TICKS));
    }

    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server && installed && refundable) {
            installed = false;
            refundable = false;
            Block.popResource(server, pos, new net.minecraft.world.item.ItemStack(
                    br.com.quantumindustries.ModItems.HAND_CRANK.get()));
        }
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
