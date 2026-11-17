package br.com.quantumindustries.client.renderer;

import br.com.quantumindustries.block.entity.QuantumVaultBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class QuantumVaultRenderer implements BlockEntityRenderer<QuantumVaultBlockEntity, QuantumVaultRenderer.State> {
    private static final Identifier HOLO_TEXTURE = Identifier.fromNamespaceAndPath("quantumindustries", "textures/block/quantum_vault_core.png");
    private final ItemModelResolver items;

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        int count;
        float time;
    }

    public QuantumVaultRenderer(BlockEntityRendererProvider.Context context) {
        this.items = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(QuantumVaultBlockEntity entity, State state, float partial, Vec3 camera,
                                   ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partial, camera, overlay);
        state.count = entity.storedCount();
        state.time = (entity.getLevel() != null ? entity.getLevel().getGameTime() : 0) + partial;
        state.item.clear();
        var stack = entity.storedItem();
        if (!stack.isEmpty()) {
            items.updateForTopItem(state.item, stack, ItemDisplayContext.FIXED, entity.getLevel(), null, 0);
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.item.isEmpty() || state.count <= 0) return;

        // 1. Render glowing cyan hologram projection beam & rings above the Vault top face
        collector.submitCustomGeometry(pose, RenderTypes.entityTranslucentEmissive(HOLO_TEXTURE), (p, v) -> {
            PoseStack stack = new PoseStack();
            stack.last().pose().set(p.pose());
            stack.last().normal().set(p.normal());
            drawHologramBeam(stack, v, state.time);
        });

        // 2. Render floating, rotating 3D item inside the hologram field
        float bob = (float) Math.sin(state.time * 0.08f) * 0.045f;
        float angle = (state.time * 2.4f) % 360.0f;

        pose.pushPose();
        pose.translate(0.5, 1.38 + bob, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        pose.scale(0.55f, 0.55f, 0.55f);
        state.item.submit(pose, collector, 0x00F000F0, 655360, 0);
        pose.popPose();
    }

    private static void drawHologramBeam(PoseStack pose, VertexConsumer out, float time) {
        float pulse = 0.55f + 0.25f * (float) Math.sin(time * 0.12f);
        int alphaRing = (int) (pulse * 190) & 0xFF;
        int colorRing = (alphaRing << 24) | 0x30ECFF;
        int colorBeam = (95 << 24) | 0x30ECFF;

        // Horizontal holographic emitter ring at y = 1.03
        addHorizontalQuad(pose, out, 0.28f, 0.72f, 1.03f, 0.28f, 0.72f, colorRing);

        // Second floating scanline ring that rises and loops between y = 1.06 and y = 1.56
        float scanY = 1.06f + ((time * 0.015f) % 0.50f);
        addHorizontalQuad(pose, out, 0.24f, 0.76f, scanY, 0.24f, 0.76f, colorRing);

        // Crossed vertical translucent holographic light cone
        float[][][] beams = {
            {{0.30f, 1.02f, 0.50f}, {0.70f, 1.02f, 0.50f}, {0.78f, 1.62f, 0.50f}, {0.22f, 1.62f, 0.50f}},
            {{0.50f, 1.02f, 0.30f}, {0.50f, 1.02f, 0.70f}, {0.50f, 1.62f, 0.78f}, {0.50f, 1.62f, 0.22f}}
        };
        for (var quad : beams) {
            for (int i = 0; i < 4; i++) {
                out.addVertex(pose.last(), quad[i][0], quad[i][1], quad[i][2])
                        .setColor(colorBeam)
                        .setUv(i == 0 || i == 3 ? 0f : 1f, i < 2 ? 1f : 0f)
                        .setOverlay(655360)
                        .setLight(0x00F000F0)
                        .setNormal(pose.last(), 0, 1, 0);
            }
        }
    }

    private static void addHorizontalQuad(PoseStack pose, VertexConsumer out,
                                          float x0, float x1, float y, float z0, float z1, int color) {
        float[][] verts = {{x0, y, z0}, {x1, y, z0}, {x1, y, z1}, {x0, y, z1}};
        for (int i = 0; i < 4; i++) {
            out.addVertex(pose.last(), verts[i][0], verts[i][1], verts[i][2])
                    .setColor(color)
                    .setUv(i == 0 || i == 3 ? 0f : 1f, i < 2 ? 0f : 1f)
                    .setOverlay(655360)
                    .setLight(0x00F000F0)
                    .setNormal(pose.last(), 0, 1, 0);
        }
    }

    @Override
    public AABB getRenderBoundingBox(QuantumVaultBlockEntity entity) {
        return new AABB(entity.getBlockPos()).expandTowards(0, 1.0, 0);
    }
}
