package br.com.quantumindustries.client.inspection;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.mechanical.MechanicalParticipant;
import br.com.quantumindustries.mechanical.RotationalBlockEntity;
import br.com.quantumindustries.block.entity.ManualDynamoBlockEntity;
import br.com.quantumindustries.inspection.InspectionSnapshot;
import br.com.quantumindustries.logistics.ConfigurableConduit;
import br.com.quantumindustries.logistics.ConnectionMode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Discrete world-space topology and directional overlay for the Inspection Goggles. */
@EventBusSubscriber(modid = "quantumindustries", value = Dist.CLIENT)
public final class MechanicalOverlayRenderer {
    private static final double MOTION_EPSILON = 1.0e-6;
    private static final int THERMAL_RED = 0xF0;
    private static final int THERMAL_GREEN = 0x96;
    private static final int THERMAL_BLUE = 0x54;

    private MechanicalOverlayRenderer() {}

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null || !player.isShiftKeyDown() || minecraft.screen != null) return;
        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.ENERGY_INSPECTION_GOGGLES.get())) return;

        InspectionSnapshot snap = ClientInspectionCache.active();
        if (snap == null || snap.kind() == InspectionSnapshot.Kind.NONE) return;
        boolean hasThermalEdges = snap.hasThermal() && !snap.thermalEdges().isEmpty();
        if (snap.networkNodes().isEmpty() && !hasThermalEdges) return;
        if (!(player.pick(6, 0, false) instanceof net.minecraft.world.phys.BlockHitResult hit)
                || hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK || !hit.getBlockPos().equals(snap.target())) return;

        Vec3 cam = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderTypes.lines());

        int r, g, b;
        switch (snap.kind()) {
            case MECHANICAL -> {
                if (snap.powered()) { r = 131; g = 188; b = 118; }
                else if (snap.capacity() > 0) { r = 228; g = 174; b = 98; }
                else { r = 193; g = 106; b = 75; }
            }
            case ENERGY -> {
                if (snap.limited()) { r = 214; g = 92; b = 72; }
                else if (snap.powered()) { r = 232; g = 186; b = 92; }
                else { r = 176; g = 148; b = 104; }
            }
            case FLUID -> { r = 106; g = 178; b = 224; }
            case PNEUMATIC -> { r = 224; g = 196; b = 132; }
            case THERMAL -> { r = THERMAL_RED; g = THERMAL_GREEN; b = THERMAL_BLUE; }
            default -> { return; }
        }

        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        PoseStack.Pose matrix = pose.last();

        for (BlockPos node : snap.networkNodes()) {
            boolean isTarget = node.equals(snap.target());
            float pad = isTarget ? 0.02f : 0.04f;
            int alpha = isTarget ? 235 : 150;
            drawBox(consumer, matrix,
                    node.getX() - pad, node.getY() - pad, node.getZ() - pad,
                    node.getX() + 1f + pad, node.getY() + 1f + pad, node.getZ() + 1f + pad,
                    r, g, b, alpha);

            if (snap.kind() == InspectionSnapshot.Kind.MECHANICAL
                    && level.getBlockEntity(node) instanceof MechanicalParticipant participant) {
                // Each node has its own synchronized sign: gears and crossed belts can reverse it.
                double omega;
                int fallback;
                if (node.equals(snap.target())) {
                    omega = snap.angularVelocity();
                    fallback = snap.spinDirection();
                } else if (participant instanceof RotationalBlockEntity rotor) {
                    omega = rotor.angularVelocity();
                    fallback = rotor.spinDirection();
                } else if (participant instanceof ManualDynamoBlockEntity dynamo) {
                    omega = dynamo.angularVelocity();
                    fallback = dynamo.spinDirection();
                } else {
                    continue;
                }
                int direction = Math.abs(omega) > MOTION_EPSILON ? (omega < 0 ? -1 : 1) : fallback;
                drawAxisArrow(consumer, matrix, node, participant.axle(), direction, r, g, b, alpha);
            }
            if (snap.kind() != InspectionSnapshot.Kind.MECHANICAL
                    && level.getBlockEntity(node) instanceof ConfigurableConduit conduit) {
                drawConduitModeArrows(consumer, matrix, node, conduit);
            }
        }

        if (hasThermalEdges) drawThermalArrows(consumer, matrix, snap);

        pose.popPose();
        buffers.endBatch(RenderTypes.lines());
    }

    private static void drawThermalArrows(VertexConsumer out, PoseStack.Pose pose, InspectionSnapshot snap) {
        for (var edge : snap.thermalEdges()) {
            int flow = edge.heatFlowTu();
            if (flow == 0) continue;
            BlockPos from = flow > 0 ? edge.neighbor() : snap.target();
            BlockPos to = flow > 0 ? snap.target() : edge.neighbor();
            Direction.Axis axis = edge.face().getAxis();
            // Lift the line outside opaque blocks without changing the transfer direction.
            Vec3 offset = axis == Direction.Axis.Y ? new Vec3(0.56, 0, 0) : new Vec3(0, 0.56, 0);
            Vec3 start = Vec3.atCenterOf(from).add(offset);
            Vec3 end = Vec3.atCenterOf(to).add(offset);
            drawDirectionalArrow(out, pose, (float) start.x, (float) start.y, (float) start.z,
                    (float) end.x, (float) end.y, (float) end.z, axis,
                    THERMAL_RED, THERMAL_GREEN, THERMAL_BLUE, 240);
        }
    }

    private static void drawConduitModeArrows(VertexConsumer out, PoseStack.Pose pose,
            BlockPos pos, ConfigurableConduit conduit) {
        float cx = pos.getX() + 0.5f;
        float cy = pos.getY() + 0.5f;
        float cz = pos.getZ() + 0.5f;
        for (Direction dir : Direction.values()) {
            ConnectionMode mode = conduit.connectionMode(dir);
            if (mode == ConnectionMode.AUTO) continue;
            float dx = dir.getStepX();
            float dy = dir.getStepY();
            float dz = dir.getStepZ();
            if (mode == ConnectionMode.PULL) {
                // Arrow pointing inward from face toward center (shape + cyan color).
                drawDirectionalArrow(out, pose,
                        cx + dx * 0.48f, cy + dy * 0.48f, cz + dz * 0.48f,
                        cx + dx * 0.16f, cy + dy * 0.16f, cz + dz * 0.16f,
                        dir.getAxis(), 109, 189, 224, 240);
            } else if (mode == ConnectionMode.PUSH) {
                // Arrow pointing outward from center toward face (shape + amber color).
                drawDirectionalArrow(out, pose,
                        cx + dx * 0.16f, cy + dy * 0.16f, cz + dz * 0.16f,
                        cx + dx * 0.48f, cy + dy * 0.48f, cz + dz * 0.48f,
                        dir.getAxis(), 228, 174, 98, 240);
            } else if (mode == ConnectionMode.NONE) {
                // Small X mark at face to indicate disconnected endpoint without relying on color.
                float fx = cx + dx * 0.46f, fy = cy + dy * 0.46f, fz = cz + dz * 0.46f;
                float s = 0.09f;
                if (dir.getAxis() == Direction.Axis.X) {
                    line(out, pose, fx, fy - s, fz - s, fx, fy + s, fz + s, 193, 106, 75, 220);
                    line(out, pose, fx, fy - s, fz + s, fx, fy + s, fz - s, 193, 106, 75, 220);
                } else if (dir.getAxis() == Direction.Axis.Y) {
                    line(out, pose, fx - s, fy, fz - s, fx + s, fy, fz + s, 193, 106, 75, 220);
                    line(out, pose, fx - s, fy, fz + s, fx + s, fy, fz - s, 193, 106, 75, 220);
                } else {
                    line(out, pose, fx - s, fy - s, fz, fx + s, fy + s, fz, 193, 106, 75, 220);
                    line(out, pose, fx - s, fy + s, fz, fx + s, fy - s, fz, 193, 106, 75, 220);
                }
            }
        }
    }

    private static void drawDirectionalArrow(VertexConsumer out, PoseStack.Pose pose,
            float sx, float sy, float sz, float ex, float ey, float ez,
            Direction.Axis axis, int r, int g, int b, int a) {
        line(out, pose, sx, sy, sz, ex, ey, ez, r, g, b, a);
        float vx = ex - sx, vy = ey - sy, vz = ez - sz;
        float len = (float) Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (len < 1e-4f) return;
        vx /= len; vy /= len; vz /= len;
        float head = 0.09f;
        float bx = ex - vx * head, by = ey - vy * head, bz = ez - vz * head;
        float px = axis == Direction.Axis.X ? 0f : head * 0.75f;
        float py = axis == Direction.Axis.Y ? 0f : (axis == Direction.Axis.X ? head * 0.75f : 0f);
        float pz = axis == Direction.Axis.Z ? 0f : (axis == Direction.Axis.Y ? head * 0.75f : 0f);
        line(out, pose, ex, ey, ez, bx + px, by + py, bz + pz, r, g, b, a);
        line(out, pose, ex, ey, ez, bx - px, by - py, bz - pz, r, g, b, a);
    }

    private static void drawAxisArrow(VertexConsumer out, PoseStack.Pose pose, BlockPos pos,
            Direction.Axis axis, int spinDirection, int r, int g, int b, int a) {
        float cx = pos.getX() + 0.5f;
        float cy = pos.getY() + 0.5f;
        float cz = pos.getZ() + 0.5f;
        float dir = spinDirection < 0 ? -1f : 1f;
        float len = 0.62f;

        float dx = axis == Direction.Axis.X ? dir : 0f;
        float dy = axis == Direction.Axis.Y ? dir : 0f;
        float dz = axis == Direction.Axis.Z ? dir : 0f;

        drawDirectionalArrow(out, pose,
                cx - dx * len, cy - dy * len, cz - dz * len,
                cx + dx * len, cy + dy * len, cz + dz * len,
                axis, r, g, b, a);
    }

    private static void drawBox(VertexConsumer out, PoseStack.Pose pose,
            float x0, float y0, float z0, float x1, float y1, float z1,
            int r, int g, int b, int a) {
        line(out, pose, x0, y0, z0, x1, y0, z0, r, g, b, a);
        line(out, pose, x1, y0, z0, x1, y0, z1, r, g, b, a);
        line(out, pose, x1, y0, z1, x0, y0, z1, r, g, b, a);
        line(out, pose, x0, y0, z1, x0, y0, z0, r, g, b, a);

        line(out, pose, x0, y1, z0, x1, y1, z0, r, g, b, a);
        line(out, pose, x1, y1, z0, x1, y1, z1, r, g, b, a);
        line(out, pose, x1, y1, z1, x0, y1, z1, r, g, b, a);
        line(out, pose, x0, y1, z1, x0, y1, z0, r, g, b, a);

        line(out, pose, x0, y0, z0, x0, y1, z0, r, g, b, a);
        line(out, pose, x1, y0, z0, x1, y1, z0, r, g, b, a);
        line(out, pose, x1, y0, z1, x1, y1, z1, r, g, b, a);
        line(out, pose, x0, y0, z1, x0, y1, z1, r, g, b, a);
    }

    static void line(VertexConsumer out, PoseStack.Pose pose,
            float x0, float y0, float z0, float x1, float y1, float z1,
            int r, int g, int b, int a) {
        float nx = x1 - x0, ny = y1 - y0, nz = z1 - z0;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 1e-5f) { nx /= len; ny /= len; nz /= len; }
        else { nx = 0f; ny = 1f; nz = 0f; }
        out.addVertex(pose, x0, y0, z0).setColor(r, g, b, a).setNormal(pose, nx, ny, nz).setLineWidth(2.0f);
        out.addVertex(pose, x1, y1, z1).setColor(r, g, b, a).setNormal(pose, nx, ny, nz).setLineWidth(2.0f);
    }
}
