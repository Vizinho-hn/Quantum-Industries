package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.logistics.ItemRouting;
import br.com.quantumindustries.mechanical.KineticNetworkManager;
import br.com.quantumindustries.mechanical.KineticLink;
import java.util.ArrayList;
import java.util.List;
import br.com.quantumindustries.mechanical.RotationalBlockEntity;
import br.com.quantumindustries.research.ResearchIds;
import br.com.quantumindustries.research.WorldResearchData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** One occupied belt cell per block. The item is consumed from its previous owner only on acceptance. */
public final class MechanicalConveyorBlockEntity extends RotationalBlockEntity implements MachineStatusInspectable {
    private static final int LOAD = 2;
    private static final double BLOCKS_PER_RADIAN = .12, MAX_STEP = .20;
    private ItemStack packet = ItemStack.EMPTY;
    private String operator = "";
    private double fraction = .5;
    private long acceptedTick = Long.MIN_VALUE;

    public MechanicalConveyorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MECHANICAL_CONVEYOR_ENTITY.get(), pos, state);
    }
    public Direction facing() { return getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING); }
    @Override public Direction.Axis axle() { return facing().getClockWise().getAxis(); }
    @Override public List<KineticLink> kineticLinks(ServerLevel server, BlockPos pos) {
        var links = new ArrayList<KineticLink>(super.kineticLinks(server, pos));
        for (Direction side : List.of(facing(), facing().getOpposite())) {
            BlockPos neighbor = pos.relative(side);
            if (server.hasChunkAt(neighbor)
                    && server.getBlockEntity(neighbor) instanceof MechanicalConveyorBlockEntity belt
                    && belt.facing().getAxis() == facing().getAxis())
                links.add(new KineticLink(neighbor, 1.0, .99));
        }
        return List.copyOf(links);
    }
    @Override public boolean allowsCrank() { return false; }
    @Override public int sourceCapacity() { return 0; }
    @Override public double kineticInertia() { return .7; }
    @Override public double kineticDrag() { return .07; }
    @Override public int requestedLoad() { return !packet.isEmpty() && knows("mechanical_transmission") ? LOAD : 0; }
    private boolean knows(String study) {
        return level instanceof ServerLevel server && !operator.isEmpty()
                && WorldResearchData.get(server).knows(operator, ResearchIds.of(study));
    }
    public ItemStack packet() { return packet.copy(); }
    public double fraction() { return fraction; }
    public boolean bind(Player player) {
        if (!authorized(player)) return false;
        if (operator.isEmpty()) { operator = player.getUUID().toString(); changed(); }
        return true;
    }
    public boolean insertManual(Player player, ItemStack stack) {
        if (!bind(player) || !packet.isEmpty() || stack.isEmpty() || !knows("mechanical_transmission")) return false;
        packet = stack.split(1); fraction = .5; acceptedTick = level.getGameTime(); changed();
        return true;
    }
    public ItemStack take(Player player) {
        if (!inReach(player) || packet.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = packet;
        packet = ItemStack.EMPTY; fraction = .5; changed(); return result;
    }
    /** Called by neighboring belt or pneumatic endpoint, never by a distant route. */
    public boolean accept(ItemStack incoming, Direction from) {
        if (!(level instanceof ServerLevel server) || !knows("mechanical_transmission")
                || incoming.isEmpty() || !packet.isEmpty() || from.getAxis() != facing().getAxis()) return false;
        var snapshot = KineticNetworkManager.snapshot(server, worldPosition);
        // Refuse delivery against an actively running belt.
        if (snapshot.powered() && Math.abs(snapshot.angularVelocity()) > 1e-3) {
            Direction travel = snapshot.angularVelocity() > 0 ? facing() : facing().getOpposite();
            if (from != travel.getOpposite()) return false;
        }
        packet = incoming.split(1);
        fraction = from == facing().getOpposite() ? 0 : 1;
        acceptedTick = server.getGameTime(); changed(); return true;
    }
    private static boolean vanillaInventory(BlockEntity be) {
        return be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity || be instanceof HopperBlockEntity;
    }
    private boolean permitted(BlockEntity be) {
        return vanillaInventory(be) || be instanceof MechanicalSawmillBlockEntity
                || be instanceof JawCrusherBlockEntity && knows("mineral_beneficiation")
                || be instanceof SteamBoilerBlockEntity && knows("fluid_hydrostatics") && knows("thermal_annealing")
                || be instanceof PneumaticTubeBlockEntity && knows("pneumatic_transport");
    }
    public void tick() {
        if (!(level instanceof ServerLevel server)) return;
        var snapshot = KineticNetworkManager.snapshot(server, worldPosition);
        applyKineticSnapshot(snapshot);
        if (!knows("mechanical_transmission") || !snapshot.powered() || Math.abs(snapshot.angularVelocity()) <= 1e-3
                || acceptedTick == server.getGameTime()) return;
        boolean forward = snapshot.angularVelocity() > 0;
        Direction travel = forward ? facing() : facing().getOpposite();
        if (packet.isEmpty()) {
            BlockPos source = worldPosition.relative(travel.getOpposite());
            if (!server.hasChunkAt(source)) return;
            BlockEntity entity = server.getBlockEntity(source);
            // Belts and tubes push; never pull from their in-flight state.
            if (!permitted(entity) || entity instanceof PneumaticTubeBlockEntity) return;
            ItemStack taken = ItemRouting.extract(server, source, 1);
            if (!taken.isEmpty()) {
                packet = taken; fraction = forward ? 0 : 1; acceptedTick = server.getGameTime(); changed();
            }
            return;
        }
        double step = Math.min(MAX_STEP, Math.abs(snapshot.angularVelocity()) * BLOCKS_PER_RADIAN * .05);
        fraction = Math.clamp(fraction + (forward ? step : -step), 0, 1);
        setChanged();
        if (forward ? fraction >= 1 : fraction <= 0) {
            BlockPos destination = worldPosition.relative(travel);
            if (server.hasChunkAt(destination)) {
                BlockEntity target = server.getBlockEntity(destination);
                if (target instanceof MechanicalConveyorBlockEntity next) next.accept(packet, travel.getOpposite());
                else if (permitted(target)) ItemRouting.insert(server, destination, packet, travel.getOpposite());
                if (packet.isEmpty()) { packet = ItemStack.EMPTY; fraction = .5; changed(); return; }
            }
        }
        if (server.getGameTime() % 3 == 0) {
            server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
    @Override public Component statusTitle() { return Component.translatable("block.quantumindustries.mechanical_conveyor"); }
    @Override public Component statusDetail() { return status(); }
    @Override public State statusState() {
        return packet.isEmpty() ? State.IDLE : rotating() && isSpinning() && fraction > 0 && fraction < 1 ? State.WORKING : State.BLOCKED;
    }
    @Override public float statusProgress() { return packet.isEmpty() ? 0 : (float) fraction; }
    public Component status() {
        return Component.translatable("message.quantumindustries.mechanical_conveyor.status",
                packet.getCount(), Math.round(fraction * 100), Math.round(rpm()));
    }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!packet.isEmpty()) out.store("belt_item", ItemStack.CODEC, packet);
        out.putString("belt_operator", operator);
        out.putDouble("belt_fraction", fraction);
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        packet = in.read("belt_item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        operator = in.getStringOr("belt_operator", "");
        double saved = in.getDoubleOr("belt_fraction", .5);
        fraction = Double.isFinite(saved) ? Math.clamp(saved, 0, 1) : .5;
    }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos,state);
        if (level instanceof ServerLevel server && !packet.isEmpty()) {
            ItemStack drop = packet; packet = ItemStack.EMPTY; Block.popResource(server,pos,drop);
        }
    }
}
