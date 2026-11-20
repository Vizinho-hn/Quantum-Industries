package br.com.quantumindustries.client.kinetic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Compiles the per-face UV Bedrock 1.12.0 format directly, without an animation library. */
public final class QksGeoModelLoader {
    private static final float UNITS_PER_BLOCK = 16.0f;
    private static final float BLOCK_CENTER = 8.0f;
    private static final float RADIANS_PER_DEGREE = (float) (Math.PI / 180.0);
    private static final String FORMAT_VERSION = "1.12.0";
    private static final String[] FACES = {"north", "south", "west", "east", "up", "down"};
    // Corners are (x high, y high, z high). Cross(v1-v0, v3-v0) is the outward normal.
    private static final int[][] CORNERS = {
            {3, 1, 0, 2}, {6, 4, 5, 7}, {7, 5, 1, 3},
            {2, 0, 4, 6}, {7, 3, 2, 6}, {1, 5, 4, 0}};
    private static final float[][] NORMALS = {
            {0, 0, -1}, {0, 0, 1}, {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}};

    private QksGeoModelLoader() {}

    /** The caller owns the stream. */
    public static QksCompiledModel load(InputStream stream) {
        return load(JsonParser.parseReader(new InputStreamReader(
                java.util.Objects.requireNonNull(stream, "stream"), StandardCharsets.UTF_8)).getAsJsonObject());
    }

    public static QksCompiledModel load(JsonObject document) {
        if (!document.has("format_version") || !FORMAT_VERSION.equals(document.get("format_version").getAsString())) {
            throw new IllegalArgumentException("Expected Bedrock format " + FORMAT_VERSION);
        }
        JsonArray geometries = document.getAsJsonArray("minecraft:geometry");
        if (geometries == null || geometries.size() != 1) {
            throw new IllegalArgumentException("Expected exactly one geometry");
        }
        JsonObject geometry = geometries.get(0).getAsJsonObject();
        JsonObject description = geometry.getAsJsonObject("description");
        float width = number(description, "texture_width", 0);
        float height = number(description, "texture_height", 0);
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Texture dimensions must be positive");
        List<QksCompiledModel.CompiledBone> bones = new ArrayList<>();
        for (JsonElement element : geometry.getAsJsonArray("bones")) {
            JsonObject bone = element.getAsJsonObject();
            float[] pivot = vector(bone, "pivot", new float[3]);
            float[] rotation = vector(bone, "rotation", new float[3]);
            List<Float> vertices = new ArrayList<>();
            if (bone.has("cubes")) {
                for (JsonElement cube : bone.getAsJsonArray("cubes")) {
                    compileCube(cube.getAsJsonObject(), width, height, vertices);
                }
            }
            float[] packed = new float[vertices.size()];
            for (int i = 0; i < packed.length; i++) packed[i] = vertices.get(i);
            bones.add(new QksCompiledModel.CompiledBone(bone.get("name").getAsString(),
                    bone.has("parent") ? bone.get("parent").getAsString() : null,
                    (BLOCK_CENTER - pivot[0]) / UNITS_PER_BLOCK, pivot[1] / UNITS_PER_BLOCK,
                    (BLOCK_CENTER + pivot[2]) / UNITS_PER_BLOCK,
                    rotation[0], -rotation[1], -rotation[2], packed));
        }
        return new QksCompiledModel(bones);
    }

    private static void compileCube(JsonObject cube, float width, float height, List<Float> output) {
        float[] origin = vector(cube, "origin", null);
        float[] size = vector(cube, "size", null);
        for (float extent : size) if (extent < 0) throw new IllegalArgumentException("Negative cube size");
        float inflate = number(cube, "inflate", 0);
        float x0 = (BLOCK_CENTER - origin[0] - size[0] - inflate) / UNITS_PER_BLOCK;
        float x1 = (BLOCK_CENTER - origin[0] + inflate) / UNITS_PER_BLOCK;
        float y0 = (origin[1] - inflate) / UNITS_PER_BLOCK;
        float y1 = (origin[1] + size[1] + inflate) / UNITS_PER_BLOCK;
        float z0 = (BLOCK_CENTER + origin[2] - inflate) / UNITS_PER_BLOCK;
        float z1 = (BLOCK_CENTER + origin[2] + size[2] + inflate) / UNITS_PER_BLOCK;
        if (x1 < x0 || y1 < y0 || z1 < z0) throw new IllegalArgumentException("Inflation inverts cube");
        float[] rotation = vector(cube, "rotation", new float[3]);
        float[] pivot = vector(cube, "pivot", new float[3]);
        float px = (BLOCK_CENTER - pivot[0]) / UNITS_PER_BLOCK;
        float py = pivot[1] / UNITS_PER_BLOCK;
        float pz = (BLOCK_CENTER + pivot[2]) / UNITS_PER_BLOCK;
        // Reflection S=diag(-1,1,1) conjugates Bedrock Rz*Ry*Rx into Rz(-z)*Ry(-y)*Rx(x).
        Quaternionf orientation = new Quaternionf().rotationZYX(-rotation[2] * RADIANS_PER_DEGREE,
                -rotation[1] * RADIANS_PER_DEGREE, rotation[0] * RADIANS_PER_DEGREE);
        Matrix4f transform = new Matrix4f().translation(px, py, pz).rotate(orientation).translate(-px, -py, -pz);
        if (!cube.has("uv") || !cube.get("uv").isJsonObject()) {
            throw new IllegalArgumentException("Expected per-face UV object");
        }
        JsonObject faces = cube.getAsJsonObject("uv");
        Vector3f position = new Vector3f();
        Vector3f normal = new Vector3f();
        for (int faceIndex = 0; faceIndex < FACES.length; faceIndex++) {
            if (!faces.has(FACES[faceIndex])) continue;
            JsonObject face = faces.getAsJsonObject(FACES[faceIndex]);
            float[] uv = vector2(face, "uv");
            float[] span = vector2(face, "uv_size");
            if (span[0] == 0 || span[1] == 0) continue;
            if (faceIndex < 2 && (x0 == x1 || y0 == y1)
                    || faceIndex >= 2 && faceIndex < 4 && (y0 == y1 || z0 == z1)
                    || faceIndex >= 4 && (x0 == x1 || z0 == z1)) continue;
            normal.set(NORMALS[faceIndex]);
            orientation.transform(normal).normalize();
            for (int vertex = 0; vertex < QksCompiledModel.VERTICES_PER_QUAD; vertex++) {
                int corner = CORNERS[faceIndex][vertex];
                position.set((corner & 1) == 0 ? x0 : x1, (corner & 2) == 0 ? y0 : y1,
                        (corner & 4) == 0 ? z0 : z1);
                transform.transformPosition(position);
                float u = (uv[0] + (vertex >= 2 ? span[0] : 0)) / width;
                float v = (uv[1] + (vertex == 1 || vertex == 2 ? span[1] : 0)) / height;
                output.add(position.x); output.add(position.y); output.add(position.z);
                output.add(u); output.add(v);
                output.add(normal.x); output.add(normal.y); output.add(normal.z);
            }
        }
    }

    private static float number(JsonObject object, String key, float fallback) {
        float value = object.has(key) ? object.get(key).getAsFloat() : fallback;
        if (!Float.isFinite(value)) throw new IllegalArgumentException("Non-finite " + key);
        return value;
    }

    private static float[] vector(JsonObject object, String key, float[] fallback) {
        if (!object.has(key)) {
            if (fallback == null) throw new IllegalArgumentException("Missing " + key);
            return fallback;
        }
        return array(object, key, 3);
    }

    private static float[] vector2(JsonObject object, String key) { return array(object, key, 2); }

    private static float[] array(JsonObject object, String key, int length) {
        JsonArray array = object.getAsJsonArray(key);
        if (array == null || array.size() != length) throw new IllegalArgumentException("Invalid " + key);
        float[] result = new float[length];
        for (int i = 0; i < length; i++) {
            result[i] = array.get(i).getAsFloat();
            if (!Float.isFinite(result[i])) throw new IllegalArgumentException("Non-finite " + key);
        }
        return result;
    }
}
