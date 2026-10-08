package com.jeremykenedy.firetv.aquarium;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.SystemClock;
import android.util.Log;

import java.util.concurrent.atomic.AtomicReference;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** Six selectable appearances share the same saved population and swimming paths. */
final class AquariumAppearanceRenderer implements GLSurfaceView.Renderer {
    private static final int[][] BACKGROUNDS = {
        {
            R.drawable.realistic_reef,
            R.drawable.realistic_tank,
            R.drawable.realistic_ocean,
            R.drawable.realistic_kelp,
            R.drawable.realistic_deep
        },
        {
            R.drawable.cinematic_reef,
            R.drawable.cinematic_tank,
            R.drawable.cinematic_ocean,
            R.drawable.cinematic_kelp,
            R.drawable.cinematic_deep
        },
        {
            R.drawable.drawn_reef,
            R.drawable.drawn_tank,
            R.drawable.drawn_ocean,
            R.drawable.drawn_kelp,
            R.drawable.drawn_deep
        }
    };
    private static final int[] FISH = {
        R.drawable.realistic_fish, R.drawable.cinematic_fish, R.drawable.drawn_fish
    };
    private static final int[] MARINE = {
        R.drawable.realistic_marine, R.drawable.cinematic_marine, R.drawable.drawn_marine
    };
    private static final float[][] WATER = {
        {0.015f, 0.14f, 0.24f},
        {0.018f, 0.15f, 0.17f},
        {0.025f, 0.22f, 0.35f},
        {0.025f, 0.12f, 0.1f},
        {0.006f, 0.018f, 0.04f}
    };
    private final Resources resources;
    private final long started = SystemClock.elapsedRealtime();
    private final AtomicReference<AquariumOptions> options;
    private AquariumTextureShader shader;
    private AquariumRenderer cartoon;
    private EGLConfig eglConfig;
    private int width;
    private int height;
    private int loadedFamily = -1;
    private int loadedScene = -1;
    private Texture background;
    private Texture fish;
    private Texture marine;
    private long sampled;
    private int frames;

    AquariumAppearanceRenderer(Resources resources, AquariumOptions options) {
        this.resources = resources;
        this.options = new AtomicReference<>(options);
    }

    void configure(AquariumOptions options) {
        this.options.set(options);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        eglConfig = config;
        shader = new AquariumTextureShader();
        cartoon = null;
        loadedFamily = loadedScene = -1;
        background = fish = marine = null;
        Log.i("Aquarium4K", "Offline appearance renderer initialized");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.width = width;
        this.height = height;
        GLES20.glViewport(0, 0, width, height);
        if (cartoon != null) cartoon.onSurfaceChanged(gl, width, height);
        Log.i("Aquarium4K", "Appearance surface " + width + "x" + height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        AquariumOptions settings = options.get();
        if (settings.look == 3) {
            if (cartoon == null) {
                cartoon = new AquariumRenderer(settings);
                cartoon.onSurfaceCreated(gl, eglConfig);
                cartoon.onSurfaceChanged(gl, width, height);
            }
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glDisable(GLES20.GL_BLEND);
            cartoon.configure(settings);
            cartoon.onDrawFrame(gl);
            return;
        }
        int family = settings.look == 5 ? 2 : settings.look == 2 || settings.look == 4 ? 1 : 0;
        if (family != loadedFamily) {
            release(fish);
            release(marine);
            fish = load(FISH[family]);
            marine = load(MARINE[family]);
        }
        if (family != loadedFamily || settings.scene != loadedScene) {
            release(background);
            background = load(BACKGROUNDS[family][settings.scene]);
            loadedScene = settings.scene;
        }
        loadedFamily = family;
        float seconds = (SystemClock.elapsedRealtime() - started) / 1000f;
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        // Android decodes and uploads premultiplied RGBA textures.
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glClearColor(0, 0, 0, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        shader.frame(settings, seconds, WATER[settings.scene]);
        float cropWidth = Math.min(1, ((float) width / height) / background.aspect);
        float cropHeight = Math.min(1, background.aspect / ((float) width / height));
        shader.draw(
                background.id,
                0,
                0,
                2,
                2,
                (1 - cropWidth) / 2,
                (1 - cropHeight) / 2,
                cropWidth,
                cropHeight,
                0,
                0,
                0,
                0,
                0,
                1);
        // Back-to-front layers retain translucent fin edges without a depth buffer.
        for (int layer = 0; layer < 6; layer++)
            for (int i = 0; i < settings.count; i++) {
                float z = settings.schools() ? FishMotion.schoolDepth(i) : FishMotion.depth(i);
                if (Math.min(5, Math.max(0, (int) (z + 3))) == layer)
                    drawFish(i, settings, seconds, z);
            }
        visitors(settings, seconds);
        if (settings.bubbles) bubbles(seconds);
        if (++frames == 180) {
            long now = SystemClock.elapsedRealtime();
            if (sampled != 0)
                Log.i(
                        "Aquarium4K",
                        "Appearance fps="
                                + (180000f / (now - sampled))
                                + ", look="
                                + AquariumOptions.LOOKS[settings.look]
                                + ", fish="
                                + settings.count);
            sampled = now;
            frames = 0;
        }
    }

    private void drawFish(int index, AquariumOptions settings, float seconds, float depth) {
        int species = FishMotion.species(settings.schools() ? index / 16 : index, settings.species);
        float x =
                settings.schools()
                        ? FishMotion.schoolX(index, seconds, settings.swimmingSpeed())
                        : FishMotion.x(index, seconds, settings.swimmingSpeed());
        float y =
                settings.schools()
                        ? FishMotion.schoolY(index, seconds)
                        : FishMotion.y(index, seconds);
        float scale = (settings.schools() ? 0.64f : FishMotion.scale(index)) * settings.fishScale();
        scale *= 0.86f + (depth + 3) * 0.045f;
        float size = 0.18f * scale;
        float rotation = (float) Math.sin(seconds * 0.4f + index) * 0.025f;
        sprite(
                fish,
                species,
                3,
                x / 22,
                y / 12,
                size * FishMotion.direction(index, settings.schools()),
                size * ((float) width / height),
                1,
                index,
                rotation,
                Math.max(0, (3 - depth) * 0.012f),
                1);
    }

    private void visitors(AquariumOptions settings, float seconds) {
        float pace = settings.swimmingSpeed();
        for (int type = 0; type < 7; type++) {
            if (!settings.hasCreature(1 << type)) continue;
            int count = type == 0 || type == 5 ? 2 : type == 6 ? 5 : 1;
            for (int i = 0; i < count; i++) {
                int index = 60 + type * 7 + i;
                float x = FishMotion.x(index, seconds, pace * (type == 1 ? 0.55f : 0.8f)) / 22;
                float y =
                        (type == 2
                                ? -0.32f
                                : type == 1 ? 0.35f : -0.3f + FishMotion.phase(index + 3) * 0.8f);
                y += (float) Math.sin(seconds * 0.2f + index) * 0.035f;
                float size =
                        type == 1
                                ? 0.64f
                                : type == 0
                                        ? 0.40f
                                        : type == 5
                                                ? 0.33f
                                                : type == 2 ? 0.20f : type == 6 ? 0.11f : 0.25f;
                int direction = type == 2 || type == 6 ? 1 : FishMotion.direction(index, false);
                if (type == 6) {
                    x =
                            -0.8f
                                    + FishMotion.phase(index) * 1.6f
                                    + (float) Math.sin(seconds * .2f + i) * .025f;
                    y = (seconds * .027f + i * .44f) % 2.5f - 1.25f;
                }
                sprite(
                        marine,
                        type,
                        4,
                        x,
                        y,
                        size * direction,
                        size * ((float) width / height),
                        type == 2 || type == 6 ? 1.8f : 0.8f,
                        index,
                        0,
                        type == 1 ? 0.17f : 0.06f,
                        type == 6 ? 0.75f : 1);
            }
        }
    }

    private void sprite(
            Texture atlas,
            int index,
            int rows,
            float x,
            float y,
            float sizeX,
            float sizeY,
            float movement,
            float phase,
            float rotation,
            float fog,
            float opacity) {
        float inset = 0.004f;
        shader.draw(
                atlas.id,
                x,
                y,
                sizeX,
                sizeY,
                index % 2 * .5f + inset,
                index / 2 / (float) rows + inset,
                .5f - inset * 2,
                1f / rows - inset * 2,
                movement,
                phase,
                rotation,
                fog,
                1,
                opacity);
    }

    private void bubbles(float seconds) {
        for (int i = 0; i < 22; i++) {
            float size = 0.005f + FishMotion.phase(i + 3) * 0.007f;
            float x =
                    -0.94f
                            + FishMotion.phase(i + 9) * 1.88f
                            + (float) Math.sin(seconds * .6f + i) * .006f;
            float y = (seconds * (.06f + FishMotion.phase(i) * .05f) + i * .173f) % 2.4f - 1.2f;
            shader.draw(
                    background.id,
                    x,
                    y,
                    size,
                    size * ((float) width / height),
                    0,
                    0,
                    1,
                    1,
                    0,
                    0,
                    0,
                    0,
                    2,
                    1);
        }
    }

    private Texture load(int resource) {
        BitmapFactory.Options decode = new BitmapFactory.Options();
        decode.inScaled = false;
        decode.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(resources, resource, decode);
        float aspect = (float) decode.outWidth / decode.outHeight;
        int[] limit = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, limit, 0);
        decode.inSampleSize = 1;
        while (decode.outWidth / decode.inSampleSize > limit[0]
                || decode.outHeight / decode.inSampleSize > limit[0]) decode.inSampleSize *= 2;
        decode.inJustDecodeBounds = false;
        decode.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeResource(resources, resource, decode);
        if (bitmap == null) throw new IllegalStateException("Missing aquarium artwork");
        int[] id = new int[1];
        GLES20.glGenTextures(1, id, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id[0]);
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle();
        if (GLES20.glGetError() != GLES20.GL_NO_ERROR)
            throw new IllegalStateException("Artwork upload failed");
        return new Texture(id[0], aspect);
    }

    private static void release(Texture texture) {
        if (texture != null) GLES20.glDeleteTextures(1, new int[] {texture.id}, 0);
    }

    private static final class Texture {
        final int id;
        final float aspect;

        Texture(int id, float aspect) {
            this.id = id;
            this.aspect = aspect;
        }
    }
}
