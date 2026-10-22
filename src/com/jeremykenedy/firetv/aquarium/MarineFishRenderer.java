package com.jeremykenedy.firetv.aquarium;

import android.content.res.AssetManager;
import android.opengl.GLES20;
import android.opengl.Matrix;

/** Species geometry and swimming deformation shared by every appearance. */
final class MarineFishRenderer {
    private static final String[] SPECIES = {
        "clownfish", "yellow-tang", "blue-tang", "angelfish", "neon-tetra", "betta"
    };
    private static final float[] LENGTH = {1f, 1.1f, 1.15f, 1f, .65f, .9f};
    private static final float[][] LIGHT = {{1, 1, 1}, {1, .9f, .76f}, {.4f, .62f, .88f}};
    private final TexturedMarineModel[] meshes = new TexturedMarineModel[SPECIES.length];
    private final MarineModelShader shader = new MarineModelShader();
    private final float[] model = new float[16];

    MarineFishRenderer(AssetManager assets) {
        for (int i = 0; i < meshes.length; i++)
            meshes[i] = new TexturedMarineModel(assets, SPECIES[i]);
    }

    void draw(
            FishMotion motion,
            AquariumOptions settings,
            float seconds,
            float[] camera,
            float[] water,
            float framingScale) {
        for (TexturedMarineModel mesh : meshes) mesh.setPainted(settings.look >= 3);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glFrontFace(GLES20.GL_CCW);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        for (int i = 0; i < settings.count; i++) {
            FishMotion.Pose pose = motion.pose(i);
            if (!pose.visible) continue;
            int species = FishMotion.species(settings.schools() ? i / 16 : i, settings.species);
            float size =
                    (settings.schools() ? .65f : FishMotion.scale(i))
                            * settings.fishScale()
                            * framingScale
                            * LENGTH[species];
            Matrix.setIdentityM(model, 0);
            Matrix.translateM(model, 0, pose.x, pose.y, pose.z);
            Matrix.rotateM(model, 0, (float) Math.toDegrees(pose.yaw), 0, 1, 0);
            Matrix.rotateM(model, 0, (float) Math.toDegrees(pose.pitch), 0, 0, 1);
            Matrix.scaleM(model, 0, size, size, size);
            shader.draw(
                    meshes[species],
                    model,
                    camera,
                    seconds * settings.swimmingSpeed(),
                    i * 2.7f,
                    water,
                    LIGHT[settings.light],
                    settings.look);
        }
        GLES20.glDisable(GLES20.GL_CULL_FACE);
    }
}
