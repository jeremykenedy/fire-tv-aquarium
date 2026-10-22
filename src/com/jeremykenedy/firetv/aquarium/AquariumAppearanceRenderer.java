package com.jeremykenedy.firetv.aquarium;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
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
    private static final int[] MARINE_ARTWORK = {
        R.drawable.realistic_marine,
        R.drawable.cinematic_marine,
        R.drawable.drawn_marine,
        R.drawable.illustrated_marine
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
    private final FishMotion motion;
    private AquariumTextureShader shader;
    private AquariumTextureShader backgroundShader;
    private int width;
    private int height;
    private int loadedFamily = -1;
    private int loadedScene = -1;
    private int loadedMarine = -1;
    private Texture background;
    private MarineFishRenderer fish;
    private MarineVisitorRenderer visitors;
    private final float[] fishCamera = new float[16];
    private Texture marine;
    private long sampled;
    private int frames;

    AquariumAppearanceRenderer(Resources resources, AquariumOptions options) {
        this(resources, options, new FishMotion());
    }

    AquariumAppearanceRenderer(Resources resources, AquariumOptions options, FishMotion motion) {
        this.resources = resources;
        this.options = new AtomicReference<>(options);
        this.motion = motion;
    }

    void configure(AquariumOptions options) {
        this.options.set(options);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        shader = new AquariumTextureShader();
        backgroundShader = new AquariumTextureShader(true);
        loadedFamily = loadedScene = loadedMarine = -1;
        background = marine = null;
        fish = new MarineFishRenderer(resources.getAssets());
        visitors = new MarineVisitorRenderer(resources.getAssets());
        Log.i("Aquarium4K", "Offline appearance renderer initialized");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.width = width;
        this.height = height;
        Matrix.orthoM(fishCamera, 0, -22, 22, -12, 12, -10, 10);
        GLES20.glViewport(0, 0, width, height);
        Log.i("Aquarium4K", "Appearance surface " + width + "x" + height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        AquariumOptions settings = options.get();
        int family =
                settings.look == 3 || settings.look == 5
                        ? 2
                        : settings.look == 2 || settings.look == 4 ? 1 : 0;
        int marineFamily = settings.look >= 3 ? 3 : family;
        if (marineFamily != loadedMarine) {
            release(marine);
            marine = load(MARINE_ARTWORK[marineFamily]);
            loadedMarine = marineFamily;
        }
        if (family != loadedFamily || settings.scene != loadedScene) {
            release(background);
            background = load(BACKGROUNDS[family][settings.scene]);
            loadedScene = settings.scene;
        }
        loadedFamily = family;
        float seconds = (SystemClock.elapsedRealtime() - started) / 1000f;
        visitors.prepare(motion, settings);
        motion.setFlatAppearance(true);
        motion.setMarineArtwork(family, (float) width / height);
        motion.update(
                seconds,
                settings.swimmingSpeed(),
                settings.schools(),
                settings.count,
                settings.creatures);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        // Background artwork is opaque RGB; blending only adds a full-surface read.
        GLES20.glDisable(GLES20.GL_BLEND);
        // Android decodes and uploads premultiplied RGBA textures.
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glClearColor(0, 0, 0, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        backgroundShader.frame(settings, seconds, WATER[settings.scene]);
        float cropWidth = Math.min(1, ((float) width / height) / background.aspect);
        float cropHeight = Math.min(1, background.aspect / ((float) width / height));
        backgroundShader.draw(
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
        fish.draw(motion, settings, seconds, fishCamera, WATER[settings.scene], 1.65f);
        visitors.draw(motion, settings, seconds, fishCamera, WATER[settings.scene]);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        shader.frame(settings, seconds, WATER[settings.scene]);
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
                                + settings.count
                                + ", shark_captures="
                                + (motion.pose(60).captures + motion.pose(61).captures)
                                + ", whale_captures="
                                + motion.pose(67).captures
                                + ", whale_capture_s="
                                + motion.pose(67).lastCaptureSeconds
                                + ", whale_prey="
                                + motion.pose(67).lastPrey
                                + ", elapsed_s="
                                + seconds
                                + ", dolphin_captures="
                                + (motion.pose(95).captures + motion.pose(96).captures));
            sampled = now;
            frames = 0;
        }
    }

    private void visitors(AquariumOptions settings, float seconds) {
        for (int type = 0; type < 7; type++) {
            if (type == 0 || type == 1 || type == 5 || !settings.hasCreature(1 << type)) continue;
            int count = type == 6 ? 5 : 1;
            for (int i = 0; i < count; i++) {
                int index = 60 + type * 7 + i;
                FishMotion.Pose pose = motion.pose(index);
                float x = pose.x / 22;
                float y = type == 2 ? -.40f + pose.y * .012f : pose.y / 12;
                float size = type == 2 ? .20f : type == 6 ? .11f : .25f;
                float facing = (float) Math.cos(pose.yaw);
                float widthScale = Math.copySign(Math.max(.16f, Math.abs(facing)), facing);
                float angle = pose.pitch;
                if (type == 6) {
                    y = (seconds * .027f + i * .44f) % 2.5f - 1.25f;
                    widthScale = 1;
                    angle = 0;
                }
                sprite(
                        marine,
                        type,
                        4,
                        x,
                        y,
                        size * widthScale,
                        size * ((float) width / height),
                        type == 2 || type == 6 ? 1.8f : .8f,
                        index,
                        angle,
                        Math.max(0, (3 - pose.z) * .012f),
                        type == 6 ? .75f : 1);
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
