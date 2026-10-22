package com.jeremykenedy.firetv.aquarium;

import android.opengl.GLES20;

import java.nio.ByteBuffer;

final class AquariumBackgroundPixels {
    private AquariumBackgroundPixels() {}

    static void verifyOpaqueBackground() {
        int width = 96;
        int height = 54;
        GLES20.glViewport(0, 0, width, height);
        GLES20.glDisable(GLES20.GL_BLEND);
        ByteBuffer colors = ByteBuffer.allocateDirect(4 * 4 * 4);
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                colors.put((byte) (30 + x * 40))
                        .put((byte) (40 + y * 35))
                        .put((byte) (50 + x * 10 + y * 15))
                        .put((byte) 255);
            }
        }
        colors.position(0);
        int[] texture = texture(4, 4, colors);
        AquariumTextureShader reference = new AquariumTextureShader();
        AquariumTextureShader opaque = new AquariumTextureShader(true);
        OpaqueCase comparison = new OpaqueCase(texture[0], width, height);
        int maximum = 0;
        for (int look = 0; look < 6; look++) {
            for (int light = 0; light < 3; light++) {
                for (int rays = 0; rays < 2; rays++) {
                    maximum =
                            Math.max(
                                    maximum,
                                    comparison.compare(reference, opaque, look, light, rays));
                }
            }
        }
        android.util.Log.i(
                "AquariumOpaque",
                "108 look/light/ray/time cases; maximum channel difference=" + maximum);
        check(
                maximum <= 1,
                "Opaque specialization preserves background pixels within one channel level");
        GLES20.glDeleteTextures(1, texture, 0);
    }

    static void verifySunlight() {
        int width = 320;
        int height = 180;
        GLES20.glViewport(0, 0, width, height);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDisable(GLES20.GL_BLEND);
        ByteBuffer gray = ByteBuffer.allocateDirect(4);
        gray.put(new byte[] {(byte) 128, (byte) 128, (byte) 128, (byte) 255}).position(0);
        int[] texture = texture(1, 1, gray);
        AquariumTextureShader shader = new AquariumTextureShader();
        ByteBuffer pixels = ByteBuffer.allocateDirect(width * height * 4);
        SunlightStatistics statistics = new SunlightStatistics(width, height);
        for (int rays = 0; rays < 2; rays++) {
            AquariumOptions settings = AquariumOptions.defaults().withChoice(9, rays, 0);
            for (float seconds : new float[] {0, 5, 17}) {
                shader.frame(settings, seconds, new float[] {0, 0, 0});
                shader.draw(texture[0], 0, 0, 2, 2, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1);
                GLES20.glReadPixels(
                        0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixels);
                statistics.measure(pixels, rays, seconds);
                if (rays == 1 && seconds == 0) {
                    ByteBuffer first = pixels.duplicate();
                    first.position(0);
                    first.get(statistics.firstLitFrame);
                }
            }
        }
        GLES20.glDeleteTextures(1, texture, 0);
        statistics.verify();
    }

    private static int[] texture(int width, int height, ByteBuffer colors) {
        int[] texture = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES20.GL_RGBA,
                width,
                height,
                0,
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
                colors);
        return texture;
    }

    private static final class OpaqueCase {
        private final int texture;
        private final int width;
        private final int height;
        private final ByteBuffer expected;
        private final ByteBuffer actual;

        OpaqueCase(int texture, int width, int height) {
            this.texture = texture;
            this.width = width;
            this.height = height;
            expected = ByteBuffer.allocateDirect(width * height * 4);
            actual = ByteBuffer.allocateDirect(width * height * 4);
        }

        int compare(
                AquariumTextureShader reference,
                AquariumTextureShader opaque,
                int look,
                int light,
                int rays) {
            int maximum = 0;
            AquariumOptions settings =
                    AquariumOptions.defaults()
                            .withChoice(18, look, 0)
                            .withChoice(7, light, 0)
                            .withChoice(9, rays, 0);
            for (float seconds : new float[] {0, 5, 17}) {
                for (int pass = 0; pass < 2; pass++) {
                    AquariumTextureShader shader = pass == 0 ? reference : opaque;
                    shader.frame(settings, seconds, new float[] {.02f, .1f, .2f});
                    shader.draw(texture, 0, 0, 2, 2, .13f, .07f, .74f, .86f, 0, 0, 0, 0, 0, 1);
                    GLES20.glReadPixels(
                            0,
                            0,
                            width,
                            height,
                            GLES20.GL_RGBA,
                            GLES20.GL_UNSIGNED_BYTE,
                            pass == 0 ? expected : actual);
                }
                for (int i = 0; i < width * height * 4; i++) {
                    int value = actual.get(i) & 255;
                    if (i % 4 == 3) check(value == 255, "Opaque background fills alpha");
                    maximum = Math.max(maximum, Math.abs(value - (expected.get(i) & 255)));
                }
            }
            return maximum;
        }
    }

    private static final class SunlightStatistics {
        private final int width;
        private final int height;
        private float maximumError;
        private int lit;
        private int moving;
        private final double[] absoluteError = new double[2];
        private final double[] squaredError = new double[2];
        private final int[] samples = new int[2];
        private final byte[] firstLitFrame;

        SunlightStatistics(int width, int height) {
            this.width = width;
            this.height = height;
            firstLitFrame = new byte[width * height * 4];
        }

        void measure(ByteBuffer pixels, int rays, float seconds) {
            for (int y = 9; y < height; y += 17) {
                for (int x = 7; x < width; x += 19) {
                    float u = (x + .5f) / width;
                    float v = 1 - (y + .5f) / height;
                    float[] reference = sunlightReference(u, v, rays, seconds);
                    float base = reference[3];
                    for (int channel = 0; channel < 3; channel++) {
                        float expected = reference[channel];
                        float actual = (pixels.get((y * width + x) * 4 + channel) & 255) / 255f;
                        float error = Math.abs(expected - actual);
                        maximumError = Math.max(maximumError, error);
                        absoluteError[rays] += error;
                        squaredError[rays] += error * error;
                        samples[rays]++;
                        int at = (y * width + x) * 4 + channel;
                        if (rays == 1
                                && seconds > 0
                                && Math.abs((pixels.get(at) & 255) - (firstLitFrame[at] & 255))
                                        >= 2) moving++;
                    }
                    if ((pixels.get((y * width + x) * 4 + 1) & 255) / 255f > base + .02f) lit++;
                }
            }
        }

        void verify() {
            android.util.Log.i(
                    "AquariumSunlight",
                    "Reference max error="
                            + maximumError
                            + "; lit samples="
                            + lit
                            + "; moving samples="
                            + moving);
            for (int rays = 0; rays < 2; rays++) {
                double mean = absoluteError[rays] / samples[rays];
                double rms = Math.sqrt(squaredError[rays] / samples[rays]);
                android.util.Log.i(
                        "AquariumSunlight", "rays=" + rays + "; mean=" + mean + "; rms=" + rms);
                check(mean < 1.5 / 255, "Sunlight mean error stays within its image budget");
                check(rms < 2.0 / 255, "Sunlight RMS error stays within its image budget");
            }
            // Five readback levels cover interpolation and 8-bit output quantization.
            check(maximumError < 5f / 255, "Sunlight interpolation preserves the reference image");
            check(moving > 20, "Sunlight pixels change across the three timestamps");
            check(lit > 20, "Animated sunlight remains visible");
            check(GLES20.glGetError() == GLES20.GL_NO_ERROR, "Sunlight pixels have no GL errors");
        }
    }

    private static void check(boolean success, String message) {
        if (!success) throw new IllegalStateException(message);
    }

    private static float[] sunlightReference(float u, float v, int rays, float seconds) {
        float base =
                (128f / 255)
                        * (1 - .13f * 1.5625f * ((u - .5f) * (u - .5f) + (v - .5f) * (v - .5f)));
        float beam =
                rays
                        * (float)
                                Math.pow(
                                        Math.max(0, Math.sin((u + v * .18f) * 17 + seconds * .1f)),
                                        8)
                        * (1 - v);
        float wave =
                (float)
                        (Math.sin(u * 90 + v * 32 + seconds * 1.3f)
                                * Math.sin(u * 35 - v * 67 - seconds));
        float fade = Math.max(0, Math.min(1, (v - .5f) / .5f));
        float caustic =
                rays * (float) Math.pow(Math.max(0, wave), 5) * fade * fade * (3 - 2 * fade);

        return new float[] {
            base + .035f * beam + .045f * caustic,
            base + .06f * beam + .05f * caustic,
            base + .06f * beam + .025f * caustic,
            base
        };
    }
}
