package com.jeremykenedy.firetv.aquarium;

import android.content.res.AssetManager;
import android.opengl.GLES20;
import android.opengl.Matrix;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Large visitors use the same measured mouth point for drawing and feeding. */
final class MarineVisitorRenderer {
    private static final int[] TYPES = {0, 1, 5};
    private static final int[] MASKS = {
        AquariumOptions.SHARK, AquariumOptions.WHALE, AquariumOptions.DOLPHIN
    };
    private static final String[] SPECIES = {
        "great-white-shark", "humpback-whale", "bottlenose-dolphin"
    };
    private static final float[] SCALE = {3.9f, 8.2f, 3.6f};
    private static final float[][] LIGHT = {{1, 1, 1}, {1, .9f, .76f}, {.4f, .62f, .88f}};
    private final AssetManager assets;
    private final TexturedMarineModel[] meshes = new TexturedMarineModel[3];
    private final MarineModelShader shader = new MarineModelShader();
    private final float[] model = new float[16];

    MarineVisitorRenderer(AssetManager assets) {
        this.assets = assets;
    }

    void prepare(FishMotion motion, AquariumOptions settings) {
        for (int species = 0; species < meshes.length; species++) {
            if (!settings.hasCreature(MASKS[species])) continue;
            if (meshes[species] == null) {
                float[] mouth = mouth(assets, SPECIES[species]);
                meshes[species] = new TexturedMarineModel(assets, SPECIES[species]);
                motion.setMeshMouth(TYPES[species], mouth[0], mouth[1], mouth[2], SCALE[species]);
            }
            meshes[species].setPainted(settings.look >= 3);
        }
    }

    void draw(
            FishMotion motion,
            AquariumOptions settings,
            float seconds,
            float[] camera,
            float[] water) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glFrontFace(GLES20.GL_CCW);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        for (int species = 0; species < meshes.length; species++) {
            if (!settings.hasCreature(MASKS[species])) continue;
            int count = species == 1 ? 1 : 2;
            for (int i = 0; i < count; i++) {
                int index = 60 + TYPES[species] * 7 + i;
                FishMotion.Pose pose = motion.pose(index);
                Matrix.setIdentityM(model, 0);
                Matrix.translateM(model, 0, pose.x, pose.y, pose.z);
                Matrix.rotateM(model, 0, (float) Math.toDegrees(pose.yaw), 0, 1, 0);
                Matrix.rotateM(model, 0, (float) Math.toDegrees(pose.pitch), 0, 0, 1);
                Matrix.scaleM(model, 0, SCALE[species], SCALE[species], SCALE[species]);
                shader.draw(
                        meshes[species],
                        model,
                        camera,
                        seconds * settings.swimmingSpeed(),
                        index * 2.7f,
                        water,
                        LIGHT[settings.light],
                        settings.look,
                        species == 0 ? 0 : 1);
            }
        }
        GLES20.glDisable(GLES20.GL_CULL_FACE);
    }

    static float[] mouth(AssetManager assets, String name) {
        try (InputStream input = assets.open("models/" + name + "-source.json")) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = input.read(buffer)) != -1) bytes.write(buffer, 0, read);
            return parseMouth(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
        } catch (IOException | JSONException error) {
            throw new IllegalStateException("Unable to load marine mouth " + name, error);
        }
    }

    static float[] parseMouth(String source) throws JSONException {
        JSONArray point = new JSONObject(source).getJSONArray("mouth_local");
        if (point.length() != 3) throw new JSONException("A mouth point needs three coordinates");
        float[] result = new float[3];
        for (int axis = 0; axis < result.length; axis++) {
            result[axis] = (float) point.getDouble(axis);
            if (Float.isNaN(result[axis]) || Float.isInfinite(result[axis]))
                throw new JSONException("Invalid mouth coordinate");
        }
        return result;
    }
}
