package br.com.quantumindustries.inspection;

import br.com.quantumindustries.ModItems;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
/** Server-thread validation and independent per-player throttling. */
public final class HydraulicInspectionService {
    private static final Map<ServerPlayer, Integer> REQUEST_TICKS = new WeakHashMap<>();
    private HydraulicInspectionService() {}
    public static HydraulicInspection request(ServerPlayer player, RequestHydraulicPayload request) {
        int now = player.tickCount; Integer last = REQUEST_TICKS.get(player);
        if (last != null && now - last >= 0 && now - last < 5) return null;
        REQUEST_TICKS.put(player, now);
        var level = player.level(); var pos = request.target();
        if (request.requestId() < 0 || !level.dimension().identifier().equals(request.dimension())
                || !player.isShiftKeyDown() || !player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.ENERGY_INSPECTION_GOGGLES.get())
                || !level.hasChunkAt(pos) || player.distanceToSqr(pos.getCenter()) > 100) return null;
        var eye = player.getEyePosition(1); var end = eye.add(player.getViewVector(1).scale(10));
        int minX = Math.floorDiv((int)Math.floor(Math.min(eye.x, end.x)), 16);
        int maxX = Math.floorDiv((int)Math.floor(Math.max(eye.x, end.x)), 16);
        int minZ = Math.floorDiv((int)Math.floor(Math.min(eye.z, end.z)), 16);
        int maxZ = Math.floorDiv((int)Math.floor(Math.max(eye.z, end.z)), 16);
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++)
            if (!level.hasChunkAt(new net.minecraft.core.BlockPos(x * 16, pos.getY(), z * 16))) return null;
        if (!(player.pick(10, 1, false) instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(pos)) return null;
        return HydraulicInspection.capture(level, pos, request.requestId());
    }
}
