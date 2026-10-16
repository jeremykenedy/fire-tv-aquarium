package com.jeremykenedy.firetv.aquarium;

import android.app.Activity;
import android.app.Instrumentation;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;

import java.nio.ByteBuffer;

/** Real shader pixels distinguish a closed body from translucent fin membranes. */
public final class MarineMaterialInstrumentation extends Instrumentation {
    @Override
    public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        start();
    }

    @Override
    public void onStart() {
        Bundle result = new Bundle();
        EGLDisplay display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        EGLContext context = EGL14.EGL_NO_CONTEXT;
        EGLSurface surface = EGL14.EGL_NO_SURFACE;
        int status = Activity.RESULT_CANCELED;
        try {
            check(EGL14.eglInitialize(display, new int[2], 0, new int[2], 0), "EGL initialization");
            EGLConfig configuration = configuration(display);
            context =
                    EGL14.eglCreateContext(
                            display,
                            configuration,
                            EGL14.EGL_NO_CONTEXT,
                            new int[] {EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE},
                            0);
            surface =
                    EGL14.eglCreatePbufferSurface(
                            display,
                            configuration,
                            new int[] {
                                EGL14.EGL_WIDTH, 1280, EGL14.EGL_HEIGHT, 720, EGL14.EGL_NONE
                            },
                            0);
            check(EGL14.eglMakeCurrent(display, surface, surface, context), "EGL current context");
            verifyMaterials();
            GLES20.glViewport(0, 0, 1280, 720);
            contact(0, AquariumOptions.SHARK, 3.9f, "great-white-shark");
            contact(1, AquariumOptions.WHALE, 8.2f, "humpback-whale");
            contact(5, AquariumOptions.DOLPHIN, 3.6f, "bottlenose-dolphin");
            result.putString(
                    "stream",
                    "PASS: closed body and transparent/translucent fins rendered actual EGL pixels;"
                            + " measured shark, whale and dolphin feeding removes visible prey"
                            + " pixels\n");
            status = Activity.RESULT_OK;
        } catch (Throwable failure) {
            result.putString("stream", "ERROR: marine material pixels: " + failure + "\n");
        } finally {
            EGL14.eglMakeCurrent(
                    display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            if (surface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, surface);
            if (context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, context);
            EGL14.eglTerminate(display);
        }
        finish(status, result);
    }

    private EGLConfig configuration(EGLDisplay display) {
        EGLConfig[] configurations = new EGLConfig[1];
        int[] count = new int[1];
        check(
                EGL14.eglChooseConfig(
                                display,
                                new int[] {
                                    EGL14.EGL_RENDERABLE_TYPE,
                                    EGL14.EGL_OPENGL_ES2_BIT,
                                    EGL14.EGL_SURFACE_TYPE,
                                    EGL14.EGL_PBUFFER_BIT,
                                    EGL14.EGL_RED_SIZE,
                                    8,
                                    EGL14.EGL_GREEN_SIZE,
                                    8,
                                    EGL14.EGL_BLUE_SIZE,
                                    8,
                                    EGL14.EGL_ALPHA_SIZE,
                                    8,
                                    EGL14.EGL_DEPTH_SIZE,
                                    16,
                                    EGL14.EGL_NONE
                                },
                                0,
                                configurations,
                                0,
                                1,
                                count,
                                0)
                        && count[0] == 1,
                "EGL color configuration");
        return configurations[0];
    }

    private void verifyMaterials() {
        GLES20.glViewport(0, 0, 32, 32);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        MarineModelShader shader = new MarineModelShader();
        int[] body = pixel(shader, "transparent-body");
        int[] membrane = pixel(shader, "transparent-fin");
        int[] root = pixel(shader, "transparent-root");
        int[] coloredRoot = pixel(shader, "translucent-root");
        int[] coloredBody = pixel(shader, "translucent-body");
        int[] coloredFin = pixel(shader, "translucent-fin");
        check(body[2] < 15, "Transparent mouth texel still closes the body");
        check(membrane[2] > 35 && membrane[1] > 20, "Transparent membrane retains water");
        check(
                root[2] > 35 && coloredRoot[0] > 70 && coloredRoot[0] < 160 && coloredRoot[2] > 15,
                "Rigid fin roots retain membrane alpha");
        check(coloredBody[0] > 180 && coloredBody[2] < 15, "Body skin restores opaque albedo");
        check(
                coloredFin[0] > 70 && coloredFin[0] < 160 && coloredFin[2] > 15,
                "Fin alpha still blends over water");
        check(GLES20.glGetError() == GLES20.GL_NO_ERROR, "Shader drawing has no OpenGL errors");
    }

    private int[] pixel(MarineModelShader shader, String name) {
        GLES20.glClearColor(.018f, .12f, .18f, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        float[] identity = new float[16];
        Matrix.setIdentityM(identity, 0);
        shader.draw(
                new TexturedMarineModel(getContext().getAssets(), name),
                identity,
                identity,
                0,
                0,
                new float[] {.018f, .12f, .18f},
                new float[] {1, 1, 1},
                0);
        ByteBuffer pixel = ByteBuffer.allocateDirect(4);
        GLES20.glReadPixels(16, 16, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixel);
        return new int[] {pixel.get(0) & 255, pixel.get(1) & 255, pixel.get(2) & 255};
    }

    private void contact(int type, int mask, float scale, String name) throws java.io.IOException {
        int count = type == 1 ? 6 : 1;
        AquariumOptions options =
                AquariumOptions.defaults()
                        .withChoice(2, count, 0)
                        .withChoice(3, 1, 0)
                        .withChoice(10 + type, 1, 0);
        FishMotion motion = new FishMotion(new java.util.Random(13 + type));
        MarineFishRenderer fish = new MarineFishRenderer(getTargetContext().getAssets());
        MarineVisitorRenderer visitors = new MarineVisitorRenderer(getTargetContext().getAssets());
        visitors.prepare(motion, options);
        int hunterIndex = 60 + type * 7;
        float seconds = prepareHunter(motion, hunterIndex, name);
        FishMotion.Pose hunter = motion.pose(hunterIndex);
        float[] mouth = MarineVisitorRenderer.mouth(getTargetContext().getAssets(), name);
        float mx = mouth[0] * scale;
        float my = mouth[1] * scale;
        float mz = mouth[2] * scale;
        placePrey(motion, count, mx, my, mz);
        float[] camera = new float[16];
        Matrix.orthoM(camera, 0, -12, 14, -7.3125f, 7.3125f, -10, 10);
        ByteBuffer before = contactFrame(fish, visitors, motion, options, camera, seconds);
        saveContact(before, name + "-before.png");
        int visible = orangePreyPixels(before, mx, my);
        check(
                visible > 20,
                name + " prey is visibly outside the head silhouette before contact: " + visible);
        int captured = hunter.captures;
        motion.update(seconds + .001f, .8f, false, count, mask);
        check(
                hunter.captures > captured && !motion.pose(0).visible,
                name + " captures at measured mesh mouth");
        ByteBuffer after = contactFrame(fish, visitors, motion, options, camera, seconds + .001f);
        saveContact(after, name + "-after.png");
        int remaining = orangePreyPixels(after, mx, my);
        check(
                remaining < visible / 10,
                name
                        + " visible prey pixels disappear after capture: "
                        + visible
                        + " -> "
                        + remaining);
        reportContact(name, captured, hunter.captures, visible, remaining);
    }

    private void reportContact(
            String name, int captured, int captures, int visible, int remaining) {
        android.util.Log.i(
                "AquariumContact",
                name
                        + " counter "
                        + captured
                        + " -> "
                        + captures
                        + "; prey pixels "
                        + visible
                        + " -> "
                        + remaining);
        check(
                GLES20.glGetError() == GLES20.GL_NO_ERROR,
                name + " contact rendering has no GL errors");
    }

    private float prepareHunter(FishMotion motion, int hunterIndex, String name) {
        FishMotion.Pose hunter = motion.pose(hunterIndex);
        motion.update(0, .8f, false, 0);
        float seconds = 0;
        while (!motion.pose(hunterIndex).hunting && seconds < 40) {
            seconds += .04f;
            motion.update(seconds, .8f, false, 0);
        }
        check(motion.pose(hunterIndex).hunting, name + " enters feeding phase");
        hunter.x = hunter.y = hunter.z = hunter.yaw = hunter.pitch = 0;
        if (hunterIndex != 67) {
            FishMotion.Pose other = motion.pose(hunterIndex + 1);
            other.x = -90;
            other.y = 90;
            other.z = 8;
        }
        return seconds;
    }

    private void placePrey(FishMotion motion, int count, float mx, float my, float mz) {
        for (int i = 0; i < count; i++) {
            FishMotion.Pose prey = motion.pose(i);
            prey.x = mx + .12f + i * .01f;
            prey.y = my;
            prey.z = mz + .01f;
            prey.yaw = prey.pitch = 0;
            prey.visible = true;
        }
    }

    private ByteBuffer contactFrame(
            MarineFishRenderer fish,
            MarineVisitorRenderer visitors,
            FishMotion motion,
            AquariumOptions options,
            float[] camera,
            float seconds) {
        float[] water = {.018f, .12f, .18f};
        GLES20.glClearColor(water[0], water[1], water[2], 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        fish.draw(motion, options, seconds, camera, water, 1.65f);
        visitors.draw(motion, options, seconds, camera, water);
        ByteBuffer pixels = ByteBuffer.allocateDirect(1280 * 720 * 4);
        GLES20.glReadPixels(0, 0, 1280, 720, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixels);
        return pixels;
    }

    private int orangePreyPixels(ByteBuffer pixels, float mx, float my) {
        int left = (int) ((mx + .35f + 12) / 26 * 1280);
        int right = (int) ((mx + 1.1f + 12) / 26 * 1280);
        int bottom = (int) ((my - .5f + 7.3125f) / 14.625f * 720);
        int top = (int) ((my + .5f + 7.3125f) / 14.625f * 720);
        int orange = 0;
        for (int y = bottom; y < top; y++) {
            for (int x = left; x < right; x++) {
                int at = (y * 1280 + x) * 4;
                int red = pixels.get(at) & 255;
                int green = pixels.get(at + 1) & 255;
                int blue = pixels.get(at + 2) & 255;
                if (red > 80 && red > green * 1.3f && red > blue * 1.8f) orange++;
            }
        }
        return orange;
    }

    private void saveContact(ByteBuffer pixels, String name) throws java.io.IOException {
        java.io.File directory =
                new java.io.File(getTargetContext().getExternalFilesDir(null), "marine-contact");
        if (!directory.isDirectory() && !directory.mkdirs())
            throw new java.io.IOException("Contact capture directory");
        pixels.position(0);
        android.graphics.Bitmap raw =
                android.graphics.Bitmap.createBitmap(
                        1280, 720, android.graphics.Bitmap.Config.ARGB_8888);
        raw.copyPixelsFromBuffer(pixels);
        android.graphics.Matrix flip = new android.graphics.Matrix();
        flip.setScale(1, -1);
        android.graphics.Bitmap upright =
                android.graphics.Bitmap.createBitmap(raw, 0, 0, 1280, 720, flip, false);
        raw.recycle();
        try (java.io.FileOutputStream output =
                new java.io.FileOutputStream(new java.io.File(directory, name))) {
            upright.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output);
        }
        upright.recycle();
    }

    private static void check(boolean success, String message) {
        if (!success) throw new IllegalStateException(message);
    }
}
