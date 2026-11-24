package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.mechanical.KineticNetworkManager;
import br.com.quantumindustries.mechanical.KineticSnapshot;
import br.com.quantumindustries.mechanical.RotationalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * BlockEntity do Tambor de Mola Helicoidal (Ep,e = 0.5 * k * x^2).
 */
public final class MainspringDrumBlockEntity extends RotationalBlockEntity {
    public static final double SPRING_CONSTANT_K = 1200.0;
    private static final double WIND_RATE_PER_RAD_S = 0.0025;
    private static final double UNWIND_PER_TICK = 0.00125;
    private static final int MAX_PEAK_TORQUE = 28;
    private static final int WINDING_LOAD = 10;

    private Mode mode = Mode.WINDING;
    private double tension = 0.0;

    public MainspringDrumBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MAINSPRING_DRUM_ENTITY.get(), pos, state);
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
            return "gui.quantumindustries.mainspring_drum.mode." + key;
        }
    }

    public void tickServer() {
        if (!(level instanceof ServerLevel server)) {
            return;
        }

        tickTransmission();
        KineticSnapshot snap = KineticNetworkManager.snapshot(server, worldPosition);
        Mode effective = effectiveMode();

        if (effective == Mode.WINDING) {
            double omega = Math.abs(snap.angularVelocity());
            if (omega > 1e-3 && tension < 1.0) {
                tension = Math.min(1.0, tension + omega * WIND_RATE_PER_RAD_S);
                setChanged();
            }
        } else if (effective == Mode.RELEASING) {
            if (tension > 0.0) {
                tension = Math.max(0.0, tension - UNWIND_PER_TICK);
                setChanged();
            }
        }
    }

    public Mode effectiveMode() {
        if (level != null && level.hasNeighborSignal(worldPosition) && tension > 0.0) {
            return Mode.RELEASING;
        }
        return mode;
    }

    public void cycleMode() {
        mode = mode.next();
        setChanged();
    }

    public double elasticEnergyJoules() {
        return 0.5 * SPRING_CONSTANT_K * tension * tension;
    }

    public Component statusLine() {
        return Component.translatable(
                "gui.quantumindustries.mainspring_drum.status",
                Component.translatable(effectiveMode().translationKey()),
                String.format("%.0f", tension * 100.0),
                String.format("%.0f", elasticEnergyJoules()));
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
        if (effectiveMode() == Mode.RELEASING && tension > 1e-3) {
            return Math.max(4, (int) Math.round(MAX_PEAK_TORQUE * tension));
        }
        return 0;
    }

    @Override
    public int requestedLoad() {
        if (effectiveMode() == Mode.WINDING && tension < 1.0) {
            return WINDING_LOAD;
        }
        return 0;
    }

    @Override
    public double kineticInertia() {
        return 4.5;
    }

    @Override
    public double kineticDrag() {
        return 0.05;
    }

    @Override
    public double staticFrictionTorque() {
        return 0.05;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("spring_mode", mode.name());
        output.putDouble("spring_tension", tension);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        try {
            mode = Mode.valueOf(input.getStringOr("spring_mode", Mode.WINDING.name()));
        } catch (IllegalArgumentException ignored) {
            mode = Mode.WINDING;
        }
        tension = Math.clamp(input.getDoubleOr("spring_tension", 0.0), 0.0, 1.0);
    }
}
