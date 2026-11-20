package br.com.quantumindustries.client.inspection;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Physical sample positions and authoritative vectors; the camera only transforms world coordinates. */
@EventBusSubscriber(modid = "quantumindustries", value = Dist.CLIENT)
public final class HydraulicOverlayRenderer {
    private HydraulicOverlayRenderer() {}
    @SubscribeEvent public static void render(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        var game = Minecraft.getInstance();
        if (game.options.hideGui) return;
        var data = ClientHydraulicInspection.active();
        if (data == null) return;
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        var pose = event.getPoseStack(); var buffers = game.renderBuffers().bufferSource();
        var out = buffers.getBuffer(RenderTypes.lines());
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        Vec3 axis = data.axle() == Direction.Axis.X ? new Vec3(1, 0, 0) : new Vec3(0, 0, 1);
        double face = camera.subtract(data.target().getCenter()).dot(axis) < 0 ? -1 : 1;
        for (var sample : data.samples()) {
            Vec3 point = data.target().getCenter().add(sample.rx(), sample.ry(), sample.rz());
            Vec3 flow = new Vec3(sample.fx(), sample.fy(), sample.fz());
            if (flow.lengthSqr() > 1e-8) {
                double length = .25 + .6 * Math.min(1, flow.length());
                arrow(out, pose.last(), point.add(axis.scale(.53 * face)), flow.normalize().scale(length), 96, 180, 240, false);
            }
            if (!sample.effective()) continue;
            Vec3 radius = new Vec3(sample.rx(), sample.ry(), sample.rz());
            Vec3 tangent = axis.cross(radius).normalize().scale(Math.signum(sample.torque()));
            int relation = data.relation(sample.torque());
            int r = relation > 0 ? 115 : relation < 0 ? 225 : 232;
            int g = relation > 0 ? 209 : relation < 0 ? 105 : 186;
            int b = relation > 0 ? 122 : relation < 0 ? 87 : 92;
            // Double barb distinguishes torque from the single-barbed blue flow arrows.
            double length = .24 + .36 * Math.min(1, Math.abs(sample.torque()) / 18);
            arrow(out, pose.last(), point.add(axis.scale(.60 * face)), tangent.scale(length), r, g, b, true);
        }
        pose.popPose(); buffers.endBatch(RenderTypes.lines());
    }
    private static void arrow(VertexConsumer out, PoseStack.Pose pose, Vec3 start, Vec3 vector,
            int r, int g, int b, boolean doubleHead) {
        if (vector.lengthSqr() < 1e-8) return;
        Vec3 dir = vector.normalize(), end = start.add(vector);
        Vec3 side = dir.cross(Math.abs(dir.y) < .9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0)).normalize().scale(.08);
        Vec3 other = dir.cross(side);
        line(out, pose, start, end, r, g, b);
        for (int i = 0; i < (doubleHead ? 2 : 1); i++) {
            Vec3 tip = end.subtract(dir.scale(i * .10)), back = tip.subtract(dir.scale(.15));
            line(out, pose, tip, back.add(side), r, g, b); line(out, pose, tip, back.subtract(side), r, g, b);
            line(out, pose, tip, back.add(other), r, g, b); line(out, pose, tip, back.subtract(other), r, g, b);
        }
    }
    private static void line(VertexConsumer out, PoseStack.Pose pose, Vec3 a, Vec3 b, int r, int g, int blue) {
        MechanicalOverlayRenderer.line(out, pose, (float)a.x, (float)a.y, (float)a.z,
                (float)b.x, (float)b.y, (float)b.z, r, g, blue, 245);
    }
}
