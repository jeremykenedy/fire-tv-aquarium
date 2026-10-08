package com.jeremykenedy.firetv.aquarium;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Native GPU checks use an offscreen surface and never launch an activity. */
public final class AppearanceInstrumentation extends Instrumentation {
    private static final int WIDTH = 3840;
    private static final int HEIGHT = 2160;
    private final StringBuilder report = new StringBuilder();
    private EGLDisplay display;
    private EGLSurface surface;
    private EGLContext context;
    private File output;
    private boolean exportOnly;

    @Override
    public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        exportOnly = arguments != null && "true".equals(arguments.getString("export"));
        start();
    }

    @Override
    public void onStart() {
        Bundle result = new Bundle();
        try {
            output = new File(getTargetContext().getExternalFilesDir(null), "appearance-checks");
            if (!output.isDirectory() && !output.mkdirs())
                throw new IllegalStateException("Output directory");
            if (exportOnly) {
                exportCaptures();
                result.putString("stream", "Captures exported to Download/AquariumChecks\n");
                finish(Activity.RESULT_OK, result);
                return;
            }
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            int[] version = new int[2];
            check(EGL14.eglInitialize(display, version, 0, version, 1), "Initialize device EGL");
            EGLConfig[] configs = new EGLConfig[1];
            int[] count = new int[1];
            int[] attributes = {
                EGL14.EGL_RENDERABLE_TYPE,
                4,
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
            };
            check(
                    EGL14.eglChooseConfig(display, attributes, 0, configs, 0, 1, count, 0)
                            && count[0] > 0,
                    "Choose device GL configuration");
            context =
                    EGL14.eglCreateContext(
                            display,
                            configs[0],
                            EGL14.EGL_NO_CONTEXT,
                            new int[] {EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE},
                            0);
            surface =
                    EGL14.eglCreatePbufferSurface(
                            display,
                            configs[0],
                            new int[] {
                                EGL14.EGL_WIDTH, WIDTH, EGL14.EGL_HEIGHT, HEIGHT, EGL14.EGL_NONE
                            },
                            0);
            check(
                    EGL14.eglMakeCurrent(display, surface, surface, context),
                    "Native 3840x2160 offscreen surface");
            AquariumAppearanceRenderer renderer =
                    new AquariumAppearanceRenderer(
                            getTargetContext().getResources(), options(0, 0, 0, 0, 0));
            renderer.onSurfaceCreated(null, null);
            renderer.onSurfaceChanged(null, WIDTH, HEIGHT);
            ByteBuffer pixels =
                    ByteBuffer.allocateDirect(WIDTH * HEIGHT * 4).order(ByteOrder.nativeOrder());
            for (int look = 0; look < AquariumOptions.LOOKS.length; look++) {
                for (int scene = 0; scene < AquariumOptions.SCENES.length; scene++) {
                    renderer.configure(options(look, scene, 16, 0, 0));
                    renderer.onDrawFrame(null);
                    read(pixels);
                    check(
                            nonEmpty(pixels),
                            AquariumOptions.LOOKS[look] + " / " + AquariumOptions.SCENES[scene]);
                    if (scene == 0) save(pixels, "look-" + look + ".png");
                }
            }
            byte[] baseline = new byte[WIDTH * HEIGHT * 4];
            for (int look : new int[] {0, 2, 5}) {
                renderer.configure(options(look, 2, 0, 0, 0));
                renderer.onDrawFrame(null);
                read(pixels);
                pixels.position(0);
                pixels.get(baseline);
                pixels.position(0);
                for (int species = 1; species <= 6; species++) {
                    renderer.configure(options(look, 2, 16, species, 0));
                    renderer.onDrawFrame(null);
                    read(pixels);
                    check(
                            changed(pixels, baseline) > 1000,
                            AquariumOptions.LOOKS[look] + " fish " + species);
                }
                for (int creature = 0; creature < 7; creature++) {
                    renderer.configure(options(look, 2, 0, 0, 1 << creature));
                    check(
                            visitorAppears(renderer, pixels, baseline),
                            AquariumOptions.LOOKS[look]
                                    + " "
                                    + AquariumOptions.CREATURE_NAMES[creature]);
                }
                renderer.configure(options(look, 2, 16, 0, 127));
                renderer.onDrawFrame(null);
                read(pixels);
                save(pixels, "marine-" + look + ".png");
            }
            AquariumPreferences preferences = new AquariumPreferences(getTargetContext());
            AquariumOptions original = preferences.read();
            try {
                AquariumOptions selected = options(5, 3, 32, 4, 127);
                preferences.save(selected);
                AquariumOptions saved = new AquariumPreferences(getTargetContext()).read();
                check(
                        saved.look == 5
                                && saved.scene == 3
                                && saved.count == 32
                                && saved.species == 4
                                && saved.creatures == 127,
                        "Appearance and independent preferences persist");
            } finally {
                preferences.save(original);
                getTargetContext().getSharedPreferences("aquarium", 0).edit().commit();
            }
            result.putString(
                    "stream",
                    report + "All native appearance checks passed. Captures: " + output + "\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable failure) {
            result.putString("stream", report + "ERROR: " + failure + "\n");
            finish(Activity.RESULT_CANCELED, result);
        } finally {
            if (display != null) {
                EGL14.eglMakeCurrent(
                        display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
                if (surface != null) EGL14.eglDestroySurface(display, surface);
                if (context != null) EGL14.eglDestroyContext(display, context);
                EGL14.eglTerminate(display);
            }
        }
    }

    private static AquariumOptions options(
            int look, int scene, int count, int species, int creatures) {
        return new AquariumOptions(
                0, count, species, scene, 1, 1, 0, 0, false, false, 0, creatures, look);
    }

    private static boolean visitorAppears(
            AquariumAppearanceRenderer renderer, ByteBuffer pixels, byte[] baseline)
            throws Exception {
        // Visitors can swim completely out of frame. Give them time to return
        // rather than requiring visibility at an arbitrary capture instant.
        long deadline = SystemClock.elapsedRealtime() + 60000;
        do {
            renderer.onDrawFrame(null);
            read(pixels);
            if (changed(pixels, baseline) > 1000) return true;
            Thread.sleep(250);
        } while (SystemClock.elapsedRealtime() < deadline);
        return false;
    }

    private void check(boolean success, String name) {
        if (!success) throw new IllegalStateException(name);
        report.append("PASS: ").append(name).append('\n');
    }

    private static void read(ByteBuffer pixels) {
        pixels.position(0);
        GLES20.glFinish();
        GLES20.glReadPixels(0, 0, WIDTH, HEIGHT, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixels);
        if (GLES20.glGetError() != GLES20.GL_NO_ERROR)
            throw new IllegalStateException("GPU readback");
        pixels.position(0);
    }

    private static boolean nonEmpty(ByteBuffer pixels) {
        for (int i = 0; i < pixels.capacity(); i += 4096)
            if ((pixels.get(i) & 255) > 12) return true;
        return false;
    }

    private static int changed(ByteBuffer pixels, byte[] baseline) {
        int different = 0;
        for (int i = 0; i < baseline.length; i += 4) {
            int delta =
                    Math.abs((pixels.get(i) & 255) - (baseline[i] & 255))
                            + Math.abs((pixels.get(i + 1) & 255) - (baseline[i + 1] & 255))
                            + Math.abs((pixels.get(i + 2) & 255) - (baseline[i + 2] & 255));
            if (delta > 15) different++;
        }
        return different;
    }

    private void save(ByteBuffer pixels, String name) throws Exception {
        pixels.position(0);
        Bitmap raw = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        raw.copyPixelsFromBuffer(pixels);
        Matrix flip = new Matrix();
        flip.setScale(1, -1);
        Bitmap upright = Bitmap.createBitmap(raw, 0, 0, WIDTH, HEIGHT, flip, false);
        raw.recycle();
        Bitmap capture = Bitmap.createScaledBitmap(upright, 1920, 1080, true);
        upright.recycle();
        try (FileOutputStream file = new FileOutputStream(new File(output, name))) {
            capture.compress(Bitmap.CompressFormat.PNG, 100, file);
        }
        capture.recycle();
    }

    private void exportCaptures() throws Exception {
        if (Build.VERSION.SDK_INT < 29)
            throw new IllegalStateException("Capture export requires Android API 29 or newer");
        File[] captures = output.listFiles();
        if (captures == null || captures.length == 0)
            throw new IllegalStateException("Run appearance checks before exporting captures");
        for (File capture : captures) {
            if (!capture.getName().endsWith(".png")) continue;
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, capture.getName());
            values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
            values.put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/AquariumChecks");
            Uri destination =
                    getTargetContext()
                            .getContentResolver()
                            .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (destination == null) throw new IllegalStateException("Capture export destination");
            try (FileInputStream input = new FileInputStream(capture);
                    OutputStream target =
                            getTargetContext().getContentResolver().openOutputStream(destination)) {
                if (target == null) throw new IllegalStateException("Capture export stream");
                byte[] buffer = new byte[16384];
                int read;
                while ((read = input.read(buffer)) != -1) target.write(buffer, 0, read);
            }
        }
    }
}
