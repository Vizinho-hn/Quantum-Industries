package br.com.quantumindustries.client.kinetic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

/** Two tangent strips, captured in world-axis coordinates before model orientation. */
final class QksBeltRenderer {
    private static final double RADIUS = .375, HALF_WIDTH = .09;
    private static final int LEATHER_COLOR = 0xFF38281C;
    private QksBeltRenderer() {}
    static void submit(QksRenderState state, PoseStack pose, SubmitNodeCollector collector) {
        Vec3 axis = switch (state.axle) {
            case X -> new Vec3(1, 0, 0); case Y -> new Vec3(0, 1, 0); case Z -> new Vec3(0, 0, 1);
        };
        Vec3 along = state.beltEnd.normalize();
        Vec3 side = axis.cross(along).normalize();
        double length = state.beltEnd.length();
        // Internal tangents meet in an X for a crossed belt; external tangents stay parallel.
        double slope = state.beltCrossed ? 2 * RADIUS / length : 0;
        Vec3 tangentOffset = side.scale(Math.sqrt(1 - slope * slope)).add(along.scale(slope)).scale(RADIUS);
        Vec3 width = axis.scale(HALF_WIDTH);
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(state.texture), (transform, consumer) -> {
            for (int sign = -1; sign <= 1; sign += 2) {
                Vec3 start = new Vec3(.5, .5, .5).add(tangentOffset.scale(sign));
                Vec3 end = new Vec3(.5, .5, .5).add(state.beltEnd).add(tangentOffset.scale(state.beltCrossed ? -sign : sign));
                Vec3 normal = end.subtract(start).cross(axis).normalize();
                Vec3 startLeft = start.subtract(width), endLeft = end.subtract(width);
                Vec3 endRight = end.add(width), startRight = start.add(width);
                vertex(consumer, transform, startLeft, normal, 0, 0, state.lightCoords);
                vertex(consumer, transform, endLeft, normal, 1, 0, state.lightCoords);
                vertex(consumer, transform, endRight, normal, 1, 1, state.lightCoords);
                vertex(consumer, transform, startRight, normal, 0, 1, state.lightCoords);
                Vec3 backNormal = normal.scale(-1);
                vertex(consumer, transform, startRight, backNormal, 0, 1, state.lightCoords);
                vertex(consumer, transform, endRight, backNormal, 1, 1, state.lightCoords);
                vertex(consumer, transform, endLeft, backNormal, 1, 0, state.lightCoords);
                vertex(consumer, transform, startLeft, backNormal, 0, 0, state.lightCoords);
            }
        });
    }
    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 pos, Vec3 normal, float u, float v, int light) {
        consumer.addVertex(pose, (float) pos.x, (float) pos.y, (float) pos.z).setColor(LEATHER_COLOR)
                .setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }
}
