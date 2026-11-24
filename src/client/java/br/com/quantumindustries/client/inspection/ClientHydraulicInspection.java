package br.com.quantumindustries.client.inspection;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.block.entity.PrimitiveWaterWheelBlockEntity;
import br.com.quantumindustries.block.entity.RimWaterWheelBlockEntity;
import br.com.quantumindustries.inspection.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** No client fluid sampling. Replies must belong to the active world/target/request generation. */
@EventBusSubscriber(modid = "quantumindustries", value = Dist.CLIENT)
public final class ClientHydraulicInspection {
    private static Level world;
    private static BlockPos target;
    private static HydraulicInspection current;
    private static long issued, generation, accepted, received;
    private static int interval;
    static { InspectionNetworking.setHydraulicSink(ClientHydraulicInspection::accept); }
    private ClientHydraulicInspection() {}
    public static BlockPos aimedWheel() {
        var game = Minecraft.getInstance();
        if (game.player == null || game.level == null || game.screen != null || (!game.player.isShiftKeyDown() || !game.options.keyShift.isDown())
                || !game.player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.ENERGY_INSPECTION_GOGGLES.get())) return null;
        if (!(game.player.pick(6, 0, false) instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return null;
        var be = game.level.getBlockEntity(hit.getBlockPos());
        return be instanceof PrimitiveWaterWheelBlockEntity || be instanceof RimWaterWheelBlockEntity ? hit.getBlockPos() : null;
    }
    public static void clear() { current = null; target = null; generation = issued + 1; accepted = 0; interval = 0; }
    public static void accept(HydraulicInspection data) {
        var game = Minecraft.getInstance();
        var aim = aimedWheel();
        if (world == null || game.level != world || aim == null || !aim.equals(target) || !aim.equals(data.target())
                || !world.dimension().identifier().equals(data.dimension()) || data.requestId() < generation
                || data.requestId() > issued || data.requestId() <= accepted || issued - data.requestId() > 3
                || current != null && data.serverTick() < current.serverTick()) return;
        current = data; accepted = data.requestId(); received = world.getGameTime();
    }
    public static HydraulicInspection active() {
        var game = Minecraft.getInstance(); var aim = aimedWheel();
        if (game.level != world || aim == null || !aim.equals(target)) { clear(); return null; }
        if (current == null || world.getGameTime() - received > 12) return null;
        return current;
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var game = Minecraft.getInstance(); var aim = aimedWheel();
        if (game.level != world) { clear(); world = game.level; }
        if (aim == null) { clear(); return; }
        boolean changed = !aim.equals(target);
        if (changed) { clear(); target = aim.immutable(); }
        if (changed || ++interval >= 5) {
            interval = 0;
            ClientPacketDistributor.sendToServer(new RequestHydraulicPayload(world.dimension().identifier(), target, ++issued));
        }
    }
}
