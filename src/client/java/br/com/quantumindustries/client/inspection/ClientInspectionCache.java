package br.com.quantumindustries.client.inspection;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.block.entity.EnergyInspectable;
import br.com.quantumindustries.block.entity.PneumaticTubeBlockEntity;
import br.com.quantumindustries.fluid.FluidInspectable;
import br.com.quantumindustries.inspection.InspectionNetworking;
import br.com.quantumindustries.inspection.InspectionSnapshot;
import br.com.quantumindustries.inspection.RequestInspectionPayload;
import br.com.quantumindustries.logistics.ConfigurableConduit;
import br.com.quantumindustries.mechanical.MechanicalParticipant;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Client-side cache, real-value trend tracker, and throttled requester for server-authored snapshots. */
@EventBusSubscriber(modid = "quantumindustries", value = Dist.CLIENT)
public final class ClientInspectionCache {
    private static final int REFRESH_INTERVAL_TICKS = 5;
    private static final long STALE_AFTER_TICKS = 30L;

    private static volatile InspectionSnapshot current;
    private static volatile long receivedAtGameTime = Long.MIN_VALUE;
    private static volatile int trend;
    private static ResourceKey<Level> lastDimension;
    private static BlockPos lastRequestedPos;
    private static boolean lastRequestedTopology;
    private static int ticksSinceRequest;

    static {
        InspectionNetworking.setClientSink(ClientInspectionCache::accept);
    }

    private ClientInspectionCache() {}

    public static void accept(InspectionSnapshot snapshot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            clear();
            return;
        }
        InspectionSnapshot prev = current;
        if (prev != null && prev.target().equals(snapshot.target()) && prev.kind() == snapshot.kind()) {
            trend = Integer.compare(snapshot.measuredValue(), prev.measuredValue());
        } else {
            trend = 0;
        }
        current = snapshot;
        receivedAtGameTime = minecraft.level.getGameTime();
    }

    public static void clear() {
        current = null;
        receivedAtGameTime = Long.MIN_VALUE;
        trend = 0;
        lastRequestedPos = null;
        lastRequestedTopology = false;
        ticksSinceRequest = 0;
    }

    /** Real delta sign between consecutive server snapshots: +1 rising, -1 falling, 0 steady. */
    public static int trend() {
        return trend;
    }

    public static InspectionSnapshot forPos(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        InspectionSnapshot snap = current;
        if (snap == null || minecraft.level == null || !snap.target().equals(pos)) return null;
        if (minecraft.level.getGameTime() - receivedAtGameTime > STALE_AFTER_TICKS) return null;
        return snap;
    }

    public static InspectionSnapshot active() {
        Minecraft minecraft = Minecraft.getInstance();
        InspectionSnapshot snap = current;
        if (snap == null || minecraft.level == null) return null;
        if (minecraft.level.getGameTime() - receivedAtGameTime > STALE_AFTER_TICKS) return null;
        return snap;
    }

    private static boolean isInspectable(BlockEntity be) {
        return be instanceof MechanicalParticipant
                || be instanceof EnergyInspectable
                || be instanceof FluidInspectable
                || be instanceof PneumaticTubeBlockEntity
                || be instanceof ConfigurableConduit;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null) {
            lastDimension = null;
            clear();
            return;
        }
        if (lastDimension != level.dimension()) {
            lastDimension = level.dimension();
            clear();
        }
        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.ENERGY_INSPECTION_GOGGLES.get())) {
            clear();
            return;
        }
        if (!(player.pick(6.0, 0.0f, false) instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            clear();
            return;
        }
        BlockPos pos = hit.getBlockPos();
        if (!isInspectable(level.getBlockEntity(pos))) {
            clear();
            return;
        }

        boolean showTopology = player.isShiftKeyDown();
        boolean targetChanged = !pos.equals(lastRequestedPos) || showTopology != lastRequestedTopology;
        if (targetChanged || ++ticksSinceRequest >= REFRESH_INTERVAL_TICKS) {
            lastRequestedPos = pos.immutable();
            lastRequestedTopology = showTopology;
            ticksSinceRequest = 0;
            ClientPacketDistributor.sendToServer(new RequestInspectionPayload(lastRequestedPos, showTopology));
        }
    }
}
