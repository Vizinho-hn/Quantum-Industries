package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.mechanical.KineticNetworkManager;
import br.com.quantumindustries.mechanical.KineticSnapshot;
import br.com.quantumindustries.mechanical.RotationalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * BlockEntity do Guincho de Contrapeso Gravitacional (Ep,g = m * g * h).
 */
public final class GravityWinchBlockEntity extends RotationalBlockEntity {
    public static final int MAX_SHAFT_DEPTH = 32;
    public static final double GRAVITY = 9.81;

    private static final double LIFT_SPEED_PER_RAD_S = 0.008;
    private static final double DESCENT_BLOCKS_PER_TICK = 0.015;

    private Mode mode = Mode.WINDING;
    private MassTier massTier = MassTier.NONE;
    private int availableDepth = 0;
    private double currentHeight = 0.0;
    private int depthScanCooldown = 0;

    public GravityWinchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.GRAVITY_WINCH_ENTITY.get(), pos, state);
    }

    public enum Mode {
        WINDING("winding"),
        LOCKED("locked"),
        RELEASING("releasing");

        private final String key;

        Mode(String key) {
            this.key = key;
        }

        public Mode next() {
            return switch (this) {
                case WINDING -> LOCKED;
                case LOCKED -> RELEASING;
                case RELEASING -> WINDING;
            };
        }

        public String translationKey() {
            return "gui.quantumindustries.gravity_winch.mode." + key;
        }
    }

    public enum MassTier {
        NONE("none", 0.0, 0, 0),
        STONE("stone", 10.0, 6, 8),
        IRON("iron", 30.0, 14, 18),
        LEAD("lead", 55.0, 24, 32);

        private final String key;
        private final double massKg;
        private final int windingLoad;
        private final int releaseTorque;

        MassTier(String key, double massKg, int windingLoad, int releaseTorque) {
            this.key = key;
            this.massKg = massKg;
            this.windingLoad = windingLoad;
            this.releaseTorque = releaseTorque;
        }

        public double massKg() {
            return massKg;
        }

        public int windingLoad() {
            return windingLoad;
        }

        public int releaseTorque() {
            return releaseTorque;
        }

        public String translationKey() {
            return "gui.quantumindustries.gravity_winch.mass." + key;
        }

        public static MassTier fromItem(ItemStack stack) {
            if (stack.isEmpty()) {
                return NONE;
            }
            if (stack.is(Items.STONE) || stack.is(Items.COBBLESTONE) || stack.is(Items.SMOOTH_STONE)) {
                return STONE;
            }
            if (stack.is(Items.IRON_BLOCK)) {
                return IRON;
            }
            if (stack.is(ModItems.LEAD_SHEET.get()) || stack.is(ModItems.BRASS_WEIGHTS.get())) {
                return LEAD;
            }
            return NONE;
        }

        public ItemStack dropStack() {
            return switch (this) {
                case STONE -> new ItemStack(Items.COBBLESTONE);
                case IRON -> new ItemStack(Items.IRON_BLOCK);
                case LEAD -> new ItemStack(ModItems.LEAD_SHEET.get());
                case NONE -> ItemStack.EMPTY;
            };
        }
    }

    public void tickServer() {
        if (!(level instanceof ServerLevel server)) {
            return;
        }

        if (--depthScanCooldown <= 0) {
            depthScanCooldown = 20;
            rescanShaftDepth(server);
        }

        tickTransmission();
        KineticSnapshot snap = KineticNetworkManager.snapshot(server, worldPosition);
        Mode effective = effectiveMode();

        if (massTier == MassTier.NONE || availableDepth <= 0) {
            return;
        }

        if (effective == Mode.WINDING) {
            double omega = Math.abs(snap.angularVelocity());
            if (omega > 1e-3 && currentHeight < availableDepth) {
                currentHeight = Math.min(availableDepth, currentHeight + omega * LIFT_SPEED_PER_RAD_S);
                setChanged();
            }
        } else if (effective == Mode.RELEASING) {
            if (currentHeight > 0.0) {
                currentHeight = Math.max(0.0, currentHeight - DESCENT_BLOCKS_PER_TICK);
                setChanged();
            }
        }
    }

    private void rescanShaftDepth(ServerLevel server) {
        int depth = 0;
        for (int dy = 1; dy <= MAX_SHAFT_DEPTH; dy++) {
            if (server.getBlockState(worldPosition.below(dy)).isAir()) {
                depth = dy;
            } else {
                break;
            }
        }
        if (depth != availableDepth) {
            availableDepth = depth;
            if (currentHeight > availableDepth) {
                currentHeight = availableDepth;
            }
            setChanged();
        }
    }

    public Mode effectiveMode() {
        if (level != null && level.hasNeighborSignal(worldPosition) && currentHeight > 0.0 && massTier != MassTier.NONE) {
            return Mode.RELEASING;
        }
        return mode;
    }

    public void installMass(MassTier newTier, ServerPlayer player, ItemStack heldStack) {
        if (newTier == MassTier.NONE || newTier == massTier) {
            return;
        }
        if (massTier != MassTier.NONE && !player.getAbilities().instabuild) {
            ItemStack previous = massTier.dropStack();
            if (!previous.isEmpty()) {
                player.getInventory().placeItemBackInInventory(previous);
            }
        }
        massTier = newTier;
        currentHeight = 0.0;
        if (!player.getAbilities().instabuild) {
            heldStack.shrink(1);
        }
        if (level instanceof ServerLevel server) {
            rescanShaftDepth(server);
        }
        setChanged();
    }

    public void removeMass(ServerPlayer player) {
        if (massTier == MassTier.NONE) {
            return;
        }
        if (!player.getAbilities().instabuild) {
            ItemStack drop = massTier.dropStack();
            if (!drop.isEmpty()) {
                player.getInventory().placeItemBackInInventory(drop);
            }
        }
        massTier = MassTier.NONE;
        currentHeight = 0.0;
        setChanged();
    }

    public void cycleMode() {
        mode = mode.next();
        setChanged();
    }

    public double potentialEnergyJoules() {
        return massTier.massKg() * GRAVITY * currentHeight;
    }

    public Component statusLine() {
        return Component.translatable(
                "gui.quantumindustries.gravity_winch.status",
                Component.translatable(effectiveMode().translationKey()),
                Component.translatable(massTier.translationKey()),
                String.format("%.1f", currentHeight),
                availableDepth,
                String.format("%.0f", potentialEnergyJoules()));
    }

    @Override
    public Direction.Axis axle() {
        return getBlockState().getValue(BlockStateProperties.AXIS);
    }

    @Override
    public boolean allowsCrank() {
        return true;
    }

    @Override
    public int sourceCapacity() {
        if (effectiveMode() == Mode.RELEASING && massTier != MassTier.NONE && currentHeight > 1e-4) {
            return massTier.releaseTorque();
        }
        return 0;
    }

    @Override
    public int requestedLoad() {
        if (effectiveMode() == Mode.WINDING && massTier != MassTier.NONE && currentHeight < availableDepth) {
            return massTier.windingLoad();
        }
        return 0;
    }

    @Override
    public double kineticInertia() {
        return 2.5 + massTier.massKg() * 0.08;
    }

    @Override
    public double kineticDrag() {
        return 0.08;
    }

    @Override
    public double staticFrictionTorque() {
        return 0.06;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("winch_mode", mode.name());
        output.putString("mass_tier", massTier.name());
        output.putInt("available_depth", availableDepth);
        output.putDouble("current_height", currentHeight);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        try {
            mode = Mode.valueOf(input.getStringOr("winch_mode", Mode.WINDING.name()));
        } catch (IllegalArgumentException ignored) {
            mode = Mode.WINDING;
        }
        try {
            massTier = MassTier.valueOf(input.getStringOr("mass_tier", MassTier.NONE.name()));
        } catch (IllegalArgumentException ignored) {
            massTier = MassTier.NONE;
        }
        availableDepth = Math.clamp(input.getIntOr("available_depth", 0), 0, MAX_SHAFT_DEPTH);
        currentHeight = Math.clamp(input.getDoubleOr("current_height", 0.0), 0.0, availableDepth);
    }
}
