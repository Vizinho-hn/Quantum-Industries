package br.com.quantumindustries.client.kinetic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Immutable block-space mesh. Bone rotations remain separate for procedural animation. */
public final class QksCompiledModel {
    public static final int FLOATS_PER_VERTEX = 8;
    public static final int VERTICES_PER_QUAD = 4;
    public static final int FLOATS_PER_QUAD = FLOATS_PER_VERTEX * VERTICES_PER_QUAD;

    public record CompiledBone(String name, String parentName, float pivotX, float pivotY, float pivotZ,
            float defaultRotX, float defaultRotY, float defaultRotZ, float[] quads) {
        public CompiledBone {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(quads, "quads");
            if (quads.length % FLOATS_PER_QUAD != 0) throw new IllegalArgumentException("Incomplete quad");
            quads = quads.clone();
        }

        /** Defensive copy for inspection; emission reads the private backing array directly. */
        @Override public float[] quads() { return quads.clone(); }
    }

    private final Map<String, CompiledBone> bones;
    private final Map<String, List<String>> children;
    private final List<String> roots;

    public QksCompiledModel(List<CompiledBone> compiledBones) {
        Map<String, CompiledBone> byName = new LinkedHashMap<>();
        for (CompiledBone bone : compiledBones) {
            if (byName.putIfAbsent(bone.name(), bone) != null) {
                throw new IllegalArgumentException("Duplicate bone " + bone.name());
            }
        }
        Map<String, List<String>> childNames = new LinkedHashMap<>();
        List<String> rootNames = new ArrayList<>();
        for (CompiledBone bone : compiledBones) {
            childNames.put(bone.name(), new ArrayList<>());
        }
        for (CompiledBone bone : compiledBones) {
            if (bone.parentName() == null) {
                rootNames.add(bone.name());
                continue;
            }
            if (!byName.containsKey(bone.parentName())) {
                throw new IllegalArgumentException("Missing parent " + bone.parentName());
            }
            childNames.get(bone.parentName()).add(bone.name());
            String ancestor = bone.parentName();
            for (int depth = 0; ancestor != null; depth++) {
                if (depth >= byName.size() || ancestor.equals(bone.name())) {
                    throw new IllegalArgumentException("Cyclic bone hierarchy at " + bone.name());
                }
                CompiledBone parent = byName.get(ancestor);
                if (parent == null) throw new IllegalArgumentException("Missing parent " + ancestor);
                ancestor = parent.parentName();
            }
        }
        childNames.replaceAll((name, names) -> List.copyOf(names));
        bones = Collections.unmodifiableMap(byName);
        children = Collections.unmodifiableMap(childNames);
        roots = List.copyOf(rootNames);
    }

    public CompiledBone bone(String name) { return bones.get(name); }
    public Map<String, CompiledBone> bones() { return bones; }
    public List<String> rootBoneNames() { return roots; }
    public List<String> childBoneNames(String name) { return children.getOrDefault(name, List.of()); }

    /** The caller applies parent and bone pivot/rotation transforms to pose before submission. */
    public static void submitBone(CompiledBone bone, PoseStack.Pose pose, VertexConsumer out, int packedLight) {
        float[] vertices = bone.quads;
        Matrix4f matrix = pose.pose();
        Matrix3f normals = pose.normal();
        for (int i = 0; i < vertices.length; i += FLOATS_PER_VERTEX) {
            float x = vertices[i], y = vertices[i + 1], z = vertices[i + 2];
            float nx = vertices[i + 5], ny = vertices[i + 6], nz = vertices[i + 7];
            float transformedX = normals.m00() * nx + normals.m10() * ny + normals.m20() * nz;
            float transformedY = normals.m01() * nx + normals.m11() * ny + normals.m21() * nz;
            float transformedZ = normals.m02() * nx + normals.m12() * ny + normals.m22() * nz;
            float inverseLength = 1.0f / (float) Math.sqrt(transformedX * transformedX
                    + transformedY * transformedY + transformedZ * transformedZ);
            // MC 26.1.2's addVertex(pose, ...) and setNormal(pose, ...) each allocate a Vector3f.
            // Equivalent scalar transforms keep this emission path allocation-free.
            out.addVertex(matrix.m00() * x + matrix.m10() * y + matrix.m20() * z + matrix.m30(),
                            matrix.m01() * x + matrix.m11() * y + matrix.m21() * z + matrix.m31(),
                            matrix.m02() * x + matrix.m12() * y + matrix.m22() * z + matrix.m32())
                    .setColor(0xFFFFFFFF).setUv(vertices[i + 3], vertices[i + 4])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight)
                    .setNormal(transformedX * inverseLength, transformedY * inverseLength,
                            transformedZ * inverseLength);
        }
    }
}
