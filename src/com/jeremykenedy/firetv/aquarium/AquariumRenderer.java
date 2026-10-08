package com.jeremykenedy.firetv.aquarium;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** Original 3D fish, tank geometry, lighting, and movement rendered locally. */
final class AquariumRenderer implements GLSurfaceView.Renderer {
    private static final float[][] WATER = {{0.035f,0.16f,0.23f}, {0.045f,0.17f,0.17f},
        {0.025f,0.19f,0.31f}, {0.03f,0.14f,0.10f}, {0.008f,0.025f,0.065f}};
    private static final float[][] LIGHT = {{1,1,1}, {1.12f,0.91f,0.70f}, {0.42f,0.62f,0.9f}};
    private static final float[][] COLORS = {{1,0.34f,0.06f}, {1,0.88f,0.1f}, {0.06f,0.35f,0.95f},
        {0.75f,0.83f,0.87f}, {0.04f,0.75f,0.9f}, {0.85f,0.18f,0.32f}};
    private volatile AquariumOptions options;
    private AquariumShader shader;
    private MarineLife marineLife;
    private AquariumMesh body, detail, plane, leaf, coral, rock, tail, dorsal;
    private final float[] camera = new float[16];
    private final float[] projection = new float[16];
    private final float[] view = new float[16];
    private final float[] model = new float[16];
    private final float[] fish = new float[16];
    private final long started = SystemClock.elapsedRealtime();
    private long sampled;
    private int frames;

    AquariumRenderer(AquariumOptions settings) { options = settings; }
    void configure(AquariumOptions settings) { options = settings; }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        shader = new AquariumShader();
        marineLife = new MarineLife(shader);
        body = AquariumMesh.sphere(16, 24);
        detail = AquariumMesh.sphere(8, 12);
        plane = AquariumMesh.plane(); leaf = AquariumMesh.leaf();
        coral = AquariumMesh.coral(); rock = AquariumMesh.rock();
        tail = AquariumMesh.fin(new float[] {-0.82f,0,0, -1.65f,0.58f,0, -1.65f,-0.58f,0});
        dorsal = AquariumMesh.fin(new float[] {-0.7f,0.2f,0, -0.25f,0.95f,0, 0.4f,0.25f,0,
            -0.6f,-0.2f,0, 0.25f,-0.7f,0, 0.45f,-0.1f,0});
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        Log.i("Aquarium4K", "Offline OpenGL aquarium initialized");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        Matrix.perspectiveM(projection, 0, 45f, (float) width / height, 1f, 100f);
        Matrix.setLookAtM(view, 0, 0, 1, 28, 0, 0, 0, 0, 1, 0);
        Matrix.multiplyMM(camera, 0, projection, 0, view, 0);
        Log.i("Aquarium4K", "Animated surface " + width + "x" + height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        AquariumOptions settings = options;
        float seconds = (SystemClock.elapsedRealtime() - started) / 1000f;
        float[] water = WATER[settings.scene];
        GLES20.glClearColor(water[0], water[1], water[2], 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        shader.frame(camera, seconds, water, LIGHT[settings.light], settings.rays);
        environment(settings, seconds);
        for (int i = 0; i < settings.count; i++) drawFish(i, settings, seconds);
        marineLife.draw(settings, seconds);
        if (settings.bubbles) bubbles(seconds);
        frames++;
        if (frames == 180) {
            long now = SystemClock.elapsedRealtime();
            if (sampled > 0) Log.i("Aquarium4K", "Animated fps=" + (180000f / (now - sampled))
                + ", fish=" + settings.count + ", species=" + AquariumOptions.SPECIES[settings.species]);
            sampled = now; frames = 0;
        }
    }

    private void transform(float x, float y, float z, float sx, float sy, float sz) {
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, x, y, z);
        Matrix.scaleM(model, 0, sx, sy, sz);
    }

    private void solid(AquariumMesh mesh, float r, float g, float b, int material) {
        shader.object(model, r, g, b, 1, material, 0, 0, 0); shader.draw(mesh);
    }

    private void environment(AquariumOptions settings, float seconds) {
        transform(0, 4, -14, 40, 30, 1); solid(plane, 0, 0, 0, 4);
        if (settings.scene == 2 || settings.scene == 4) return;
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, 0, -7.3f, 0);
        Matrix.rotateM(model, 0, -90, 1, 0, 0);
        Matrix.scaleM(model, 0, 35, 35, 1);
        solid(plane, 0.64f, 0.61f, 0.44f, 3);
        for (int i = 0; i < 12; i++) {
            float x = -22 + i * 4.0f;
            float height = .7f + FishMotion.phase(i + 5) * 1.6f;
            transform(x + FishMotion.phase(i) * 1.3f, -7.3f + height * 0.5f, -7.2f, 1.7f, height, 1.4f);
            Matrix.rotateM(model, 0, i * 47f, 0, 1, 0);
            solid(rock, 0.30f, 0.38f, 0.37f, 6);
        }
        if (settings.scene == 1) {
            transform(0, 10.8f, -5, 21, .10f, .1f); solid(plane, .16f,.23f,.23f,0);
            transform(-20.5f, 1, -5, .10f, 10, .1f); solid(plane, .16f,.23f,.23f,0);
            transform(20.5f, 1, -5, .10f, 10, .1f); solid(plane, .16f,.23f,.23f,0);
            transform(18,-4.2f,-7, .7f,2.5f,.65f); solid(rock,.14f,.20f,.22f,0);
        }
        for (int i = 0; i < 28; i++) {
            float x = -23 + FishMotion.phase(i + 13) * 46;
            float height = (settings.scene == 3 ? 5.0f : 1.8f) + FishMotion.phase(i + 4) * 3.0f;
            transform(x, -7.1f, -5 - FishMotion.phase(i) * 3, settings.scene == 0 ? 1.8f : 1, height, 1);
            Matrix.rotateM(model, 0, (i % 3 - 1) * 14, 0, 0, 1);
            float[] color = settings.scene != 0 ? new float[] {0.16f,0.48f,0.28f}
                : new float[] {0.66f,0.28f + FishMotion.phase(i) * 0.15f,0.35f};
            shader.object(model, color[0], color[1], color[2], 1, 0, 0, 2, i * 1.2f);
            shader.draw(settings.scene != 0 ? leaf : coral);
        }
    }

    private void drawFish(int index, AquariumOptions settings, float seconds) {
        int species = FishMotion.species(settings.schools() ? index / 16 : index, settings.species);
        float scale = (settings.schools() ? 0.65f : FishMotion.scale(index)) * settings.fishScale();
        Matrix.setIdentityM(fish, 0);
        Matrix.translateM(fish, 0,
            settings.schools() ? FishMotion.schoolX(index, seconds, settings.swimmingSpeed()) : FishMotion.x(index, seconds, settings.swimmingSpeed()),
            settings.schools() ? FishMotion.schoolY(index, seconds) : FishMotion.y(index, seconds),
            settings.schools() ? FishMotion.schoolDepth(index) : FishMotion.depth(index));
        Matrix.rotateM(fish, 0, FishMotion.direction(index, settings.schools()) > 0 ? 0 : 180, 0, 1, 0);
        Matrix.rotateM(fish, 0, (float) Math.sin(seconds * 0.4f + index) * 4, 0, 0, 1);
        Matrix.scaleM(fish, 0, scale, scale, scale);
        float height = species == 1 || species == 2 ? 0.58f : species == 3 ? 0.8f : 0.38f;
        float length = species == 3 ? 0.68f : species == 4 ? 1.15f : 1f;
        System.arraycopy(fish, 0, model, 0, 16);
        Matrix.scaleM(model, 0, length, height, 0.25f);
        shader.object(model, 1, 1, 1, 1, 1, species, 1, index); shader.draw(body);
        fins(species, index);
        for (int side = -1; side <= 1; side += 2) {
            System.arraycopy(fish, 0, model, 0, 16);
            Matrix.translateM(model, 0, length * 0.65f, height * 0.27f, side * 0.20f);
            Matrix.scaleM(model, 0, 0.085f, 0.085f, 0.06f);
            solid(detail, 0.93f, 0.88f, 0.68f, 0);
            System.arraycopy(fish, 0, model, 0, 16);
            Matrix.translateM(model, 0, length * 0.66f, height * 0.27f, side * 0.252f);
            Matrix.scaleM(model, 0, 0.047f, 0.051f, 0.025f);
            solid(detail, 0.025f, 0.035f, 0.05f, 0);
        }
    }

    private void fins(int species, int index) {
        float[] color = COLORS[species];
        System.arraycopy(fish, 0, model, 0, 16);
        if (species == 5) Matrix.scaleM(model, 0, 1.15f, 1.4f, 1);
        shader.object(model, species == 2 ? 1 : color[0], species == 2 ? 0.87f : color[1],
            species == 2 ? 0.12f : color[2], 0.9f, 2, species, 1, index); shader.draw(tail);
        System.arraycopy(fish, 0, model, 0, 16);
        if (species == 3) Matrix.scaleM(model, 0, 0.8f, 1.45f, 1);
        shader.object(model, color[0], color[1], color[2], 0.82f, 2, species, 1, index); shader.draw(dorsal);
    }

    private void bubbles(float seconds) {
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glDepthMask(false);
        for (int i = 0; i < 22; i++) {
            float radius = 0.07f + FishMotion.phase(i + 3) * 0.11f;
            float x = -18 + FishMotion.phase(i + 9) * 36 + (float) Math.sin(seconds * 0.6f + i) * 0.22f;
            float y = (seconds * (0.7f + FishMotion.phase(i)) + i * 0.83f) % 20f - 8f;
            transform(x, y, 4, radius, radius, radius);
            shader.object(model, 0.6f, 0.85f, 0.94f, 0.48f, 5, 0, 0, 0); shader.draw(detail);
        }
        GLES20.glDepthMask(true);
        GLES20.glDisable(GLES20.GL_BLEND);
    }
}
