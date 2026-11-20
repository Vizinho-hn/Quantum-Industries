package br.com.quantumindustries.client.kinetic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/** Standalone acceptance checks for the native mesh compiler; no game instance required. */
public final class QksGeometrySelfCheck {
    private static final float EPSILON = 1.0e-4f;
    private static final int FACES_PER_CUBE = 6;
    private static final Map<String, Map<String, Integer>> CUBES = Map.of(
            "mechanical_shaft_block", Map.of("base", 5, "rotor", 9, "crank", 2),
            "primitive_water_wheel", Map.of("base", 8, "rotor", 26),
            "mechanical_sawmill", Map.of("base", 14, "carriage", 2, "rotor", 21, "crank", 2),
            "manual_dynamo", Map.of("base", 48, "rotor", 38, "crank", 3));

    private QksGeometrySelfCheck() {}

    public static void verifyAll() {
        CUBES.forEach((name, counts) -> {
            try (InputStream stream = QksGeometrySelfCheck.class.getResourceAsStream(
                    QksModelCache.CLASSPATH_PREFIX + name + QksModelCache.MODEL_SUFFIX)) {
                require(stream != null, "Missing model " + name);
                QksCompiledModel model = QksGeoModelLoader.load(stream);
                require(model.bones().size() == counts.size(), name + ": bone count");
                counts.forEach((boneName, count) -> {
                    var bone = model.bone(boneName);
                    require(bone != null, name + ": missing " + boneName);
                    require(bone.quads().length == count * FACES_PER_CUBE * QksCompiledModel.FLOATS_PER_QUAD,
                            name + "/" + boneName + ": cube count");
                    verifyQuads(bone.quads());
                });
                require(model.childBoneNames("rotor").contains("crank") == counts.containsKey("crank"),
                        name + ": crank parent");
                if (counts.containsKey("carriage")) {
                    require(model.childBoneNames("base").contains("carriage"), "carriage parent");
                }
                if (name.equals("manual_dynamo")) verifyBraces(model);
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to check " + name, exception);
            }
        });
        for (String name : java.util.List.of("small_cogwheel", "large_cogwheel", "iron_flywheel", "mechanical_pulley", "medium_water_wheel", "large_water_wheel")) {
            try (InputStream stream = QksGeometrySelfCheck.class.getResourceAsStream(QksModelCache.CLASSPATH_PREFIX + name + QksModelCache.MODEL_SUFFIX)) {
                // Job 2B may still be producing these resources during a parallel Java check.
                if (stream == null) continue;
                var model = QksGeoModelLoader.load(stream);
                require(model.bone("rotor") != null, name + ": missing rotor");
                if (!name.endsWith("cogwheel")) require(model.bone("base") != null, name + ": missing base");
                model.bones().values().forEach(bone -> verifyQuads(bone.quads()));
            } catch (IOException exception) { throw new IllegalStateException("Unable to check " + name, exception); }
        }
        verifyRotationAxes();
        verifyRejectedInput();
        verifyCacheAndImmutability();
        verifyVertexEmission();
    }

    private static void verifyQuads(float[] data) {
        int stride = QksCompiledModel.FLOATS_PER_VERTEX;
        for (int q = 0; q < data.length; q += QksCompiledModel.FLOATS_PER_QUAD) {
            for (int v = q; v < q + QksCompiledModel.FLOATS_PER_QUAD; v += stride) {
                for (int i = 0; i < stride; i++) require(Float.isFinite(data[v + i]), "Non-finite vertex");
                require(data[v + 3] >= 0 && data[v + 3] <= 1 && data[v + 4] >= 0 && data[v + 4] <= 1,
                        "UV outside texture");
                float length = (float) Math.sqrt(data[v + 5] * data[v + 5]
                        + data[v + 6] * data[v + 6] + data[v + 7] * data[v + 7]);
                require(Math.abs(length - 1) < EPSILON, "Non-unit normal");
            }
            float ax = data[q + stride] - data[q], ay = data[q + stride + 1] - data[q + 1];
            float az = data[q + stride + 2] - data[q + 2];
            float bx = data[q + 3 * stride] - data[q], by = data[q + 3 * stride + 1] - data[q + 1];
            float bz = data[q + 3 * stride + 2] - data[q + 2];
            require((ay * bz - az * by) * data[q + 5] + (az * bx - ax * bz) * data[q + 6]
                    + (ax * by - ay * bx) * data[q + 7] > 0, "Inward or degenerate quad");
        }
    }

    private static void verifyBraces(QksCompiledModel model) throws IOException {
        try (InputStream stream = QksGeometrySelfCheck.class.getResourceAsStream(
                QksModelCache.CLASSPATH_PREFIX + "manual_dynamo" + QksModelCache.MODEL_SUFFIX)) {
            JsonObject source = JsonParser.parseReader(new java.io.InputStreamReader(
                    java.util.Objects.requireNonNull(stream), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            var cubes = source.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
                    .getAsJsonArray("bones").get(0).getAsJsonObject().getAsJsonArray("cubes");
            float[] mesh = model.bone("base").quads();
            int rotated = 0;
            for (int c = 0; c < cubes.size(); c++) {
                JsonObject cube = cubes.get(c).getAsJsonObject();
                if (!cube.has("rotation")) continue;
                rotated++;
                var pivot = cube.getAsJsonArray("pivot");
                float z = (8 + pivot.get(2).getAsFloat()) / 16;
                float[] center = new float[3];
                int start = c * FACES_PER_CUBE * QksCompiledModel.FLOATS_PER_QUAD;
                for (int v = start; v < start + FACES_PER_CUBE * QksCompiledModel.FLOATS_PER_QUAD;
                        v += QksCompiledModel.FLOATS_PER_VERTEX) {
                    for (int axis = 0; axis < 3; axis++) center[axis] += mesh[v + axis] / 24;
                }
                require(Math.abs(center[0] - 0.5f) < EPSILON && Math.abs(center[1] - 0.5f) < EPSILON,
                        "Brace not centered at pivot");
                // The authored bars straddle the pivot plane with a 0.1 Bedrock-unit offset.
                require(Math.abs(Math.abs(center[2] - z) - 0.1f / 16) < EPSILON, "Brace Z plane");
                float normalX = mesh[start + 2 * QksCompiledModel.FLOATS_PER_QUAD + 5];
                float normalY = mesh[start + 2 * QksCompiledModel.FLOATS_PER_QUAD + 6];
                float rotation = cube.getAsJsonArray("rotation").get(2).getAsFloat();
                require(normalX * normalY * rotation < 0, "Brace rotation sign");
            }
            require(rotated == 4, "Expected four diagonal braces");
        }
    }

    private static void verifyRotationAxes() {
        for (int axis = 0; axis < 3; axis++) {
            String rotation = axis == 0 ? "90,0,0" : axis == 1 ? "0,90,0" : "0,0,90";
            JsonObject input = JsonParser.parseString("""
                    {"format_version":"1.12.0","minecraft:geometry":[{"description":{
                    "texture_width":16,"texture_height":16},"bones":[{"name":"test","cubes":[{
                    "origin":[0,0,0],"size":[2,2,2],"pivot":[0,0,0],"rotation":[%s],
                    "uv":{"north":{"uv":[4,4],"uv_size":[-2,-2]}}}]}]}]}
                    """.formatted(rotation)).getAsJsonObject();
            float[] data = QksGeoModelLoader.load(input).bone("test").quads();
            verifyQuads(data);
            // S Rz Ry Rx S, with S mirroring X: rotation signs are (+X,-Y,-Z).
            float[] expected = axis == 0 ? new float[]{0, 1, 0}
                    : axis == 1 ? new float[]{1, 0, 0} : new float[]{0, 0, -1};
            for (int i = 0; i < 3; i++) require(Math.abs(data[5 + i] - expected[i]) < EPSILON,
                    "Incorrect rotation axis " + axis);
            require(data[3] > data[19] && data[4] > data[12], "Negative UV span lost");
        }
    }

    private static void verifyRejectedInput() {
        try {
            QksGeoModelLoader.load(JsonParser.parseString("{\"format_version\":\"1.10.0\"}").getAsJsonObject());
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new IllegalStateException("Unsupported version accepted");
    }

    private static void verifyCacheAndImmutability() {
        QksCompiledModel model = QksModelCache.get("manual_dynamo");
        require(model == QksModelCache.get("block/manual_dynamo.geo.json"), "Cache did not reuse mesh");
        float[] copy = model.bone("base").quads();
        float original = copy[0];
        copy[0] = Float.NaN;
        require(model.bone("base").quads()[0] == original, "Mutable bone vertex storage");
        QksModelCache.clear();
        require(model != QksModelCache.get("manual_dynamo"), "Cache did not invalidate mesh");
        try {
            QksModelCache.get("missing_qks_acceptance_model");
        } catch (java.io.UncheckedIOException expected) {
            return;
        }
        throw new IllegalStateException("Missing resource accepted");
    }

    private static void verifyVertexEmission() {
        var stack = new com.mojang.blaze3d.vertex.PoseStack();
        stack.translate(2, 3, 4);
        stack.mulPose(new org.joml.Quaternionf().rotationXYZ(0.2f, -0.4f, 0.6f));
        stack.scale(2, 3, 4);
        var bone = QksModelCache.get("manual_dynamo").bone("base");
        float[] vertices = bone.quads();
        var consumer = new EmissionCheck(vertices, stack.last());
        QksCompiledModel.submitBone(bone, stack.last(), consumer, EmissionCheck.LIGHT);
        require(consumer.vertexCount == vertices.length / QksCompiledModel.FLOATS_PER_VERTEX,
                "Emission omitted vertices");
    }

    private static final class EmissionCheck implements com.mojang.blaze3d.vertex.VertexConsumer {
        private static final int LIGHT = 0x00F000F0;
        private final float[] vertices;
        private final com.mojang.blaze3d.vertex.PoseStack.Pose pose;
        private int vertexCount;
        private int offset;

        private EmissionCheck(float[] vertices, com.mojang.blaze3d.vertex.PoseStack.Pose pose) {
            this.vertices = vertices;
            this.pose = pose;
        }

        @Override public EmissionCheck addVertex(float x, float y, float z) {
            offset = vertexCount++ * QksCompiledModel.FLOATS_PER_VERTEX;
            var expected = pose.pose().transformPosition(vertices[offset], vertices[offset + 1],
                    vertices[offset + 2], new org.joml.Vector3f());
            require(Math.abs(expected.x - x) < EPSILON && Math.abs(expected.y - y) < EPSILON
                    && Math.abs(expected.z - z) < EPSILON, "Position transform mismatch");
            return this;
        }
        @Override public EmissionCheck setNormal(float x, float y, float z) {
            var expected = pose.transformNormal(vertices[offset + 5], vertices[offset + 6],
                    vertices[offset + 7], new org.joml.Vector3f());
            require(Math.abs(expected.x - x) < EPSILON && Math.abs(expected.y - y) < EPSILON
                    && Math.abs(expected.z - z) < EPSILON, "Normal transform mismatch");
            return this;
        }
        @Override public EmissionCheck setColor(int color) {
            require(color == 0xFFFFFFFF, "Vertex tint"); return this;
        }
        @Override public EmissionCheck setColor(int r, int g, int b, int a) {
            throw new IllegalStateException("Unexpected unpacked color");
        }
        @Override public EmissionCheck setUv(float u, float v) {
            require(u == vertices[offset + 3] && v == vertices[offset + 4], "UV emission"); return this;
        }
        @Override public EmissionCheck setOverlay(int overlay) {
            require(overlay == net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                    "Overlay emission"); return this;
        }
        @Override public EmissionCheck setLight(int light) {
            require(light == LIGHT, "Light emission"); return this;
        }
        @Override public EmissionCheck setUv1(int u, int v) { return this; }
        @Override public EmissionCheck setUv2(int u, int v) { return this; }
        @Override public EmissionCheck setLineWidth(float width) { return this; }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    public static void main(String[] args) {
        verifyAll();
    }
}
