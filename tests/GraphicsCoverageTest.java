package com.jeremykenedy.firetv.aquarium;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;
import org.robolectric.shadows.ShadowGLES20;
import org.robolectric.shadows.ShadowSystemClock;

import java.time.Duration;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, shadows = GraphicsCoverageTest.Graphics.class)
public final class GraphicsCoverageTest {
    @Before
    public void resetGraphics() {
        Graphics.clear();
    }

    @Implements(GLES20.class)
    public static class Graphics extends ShadowGLES20 {
        static boolean compile = true;
        static boolean link = true;
        static int textureLimit = 2048;
        static int error;
        static int draws;
        static int deletedTextures;
        static java.nio.FloatBuffer surface;
        static int surfaceVertices;
        static boolean blending;
        static final java.util.List<Boolean> blendedDraws = new java.util.ArrayList<>();
        static final java.util.List<float[]> transforms = new java.util.ArrayList<>();
        static final java.util.List<Float> angles = new java.util.ArrayList<>();

        @Resetter
        public static void clear() {
            compile = link = true;
            textureLimit = 2048;
            error = draws = deletedTextures = 0;
            angles.clear();
            transforms.clear();
            surface = null;
            surfaceVertices = 0;
            blending = false;
            blendedDraws.clear();
        }

        @Implementation
        protected static int glGetUniformLocation(int program, String name) {
            return name.hashCode();
        }

        @Implementation
        protected static void glUniform1f(int location, float value) {
            if (location == "uAngle".hashCode()) angles.add(value);
        }

        @Implementation
        protected static void glUniformMatrix4fv(
                int location, int count, boolean transpose, float[] values, int offset) {
            if (location == "uModel".hashCode())
                transforms.add(java.util.Arrays.copyOfRange(values, offset, offset + 16));
        }

        @Implementation
        protected static void glDrawElements(int mode, int count, int type, int offset) {
            assertEquals(GLES20.GL_TRIANGLES, mode);
            assertEquals(GLES20.GL_UNSIGNED_SHORT, type);
            assertTrue(count > 0 && count % 3 == 0);
            draws++;
        }

        @Implementation
        protected static void glGetShaderiv(int shader, int pname, int[] result, int offset) {
            result[offset] = compile ? 1 : 0;
        }

        @Implementation
        protected static void glGetProgramiv(int program, int pname, int[] result, int offset) {
            result[offset] = link ? 1 : 0;
        }

        @Implementation
        protected static void glGetIntegerv(int name, int[] result, int offset) {
            result[offset] = textureLimit;
        }

        @Implementation
        protected static int glGetError() {
            return error;
        }

        @Implementation
        protected static void glVertexAttribPointer(
                int index,
                int size,
                int type,
                boolean normalized,
                int stride,
                java.nio.Buffer pointer) {
            if (stride == 16 && pointer.position() == 0)
                surface = ((java.nio.FloatBuffer) pointer).duplicate();
        }

        @Implementation
        protected static void glEnable(int capability) {
            if (capability == GLES20.GL_BLEND) blending = true;
        }

        @Implementation
        protected static void glDisable(int capability) {
            if (capability == GLES20.GL_BLEND) blending = false;
        }

        @Implementation
        protected static void glDrawArrays(int mode, int first, int count) {
            assertEquals(GLES20.GL_TRIANGLES, mode);
            assertTrue(count > 0 && count % 3 == 0);
            surfaceVertices = count;
            blendedDraws.add(blending);
            draws++;
        }

        @Implementation
        protected static void glDeleteTextures(int count, int[] textures, int offset) {
            deletedTextures += count;
        }
    }

    @Implements(BitmapFactory.class)
    public static class MissingArtwork extends org.robolectric.shadows.ShadowBitmapFactory {
        @Implementation
        protected static Bitmap decodeResource(
                Resources resources, int id, BitmapFactory.Options options) {
            options.outWidth = 128;
            options.outHeight = 128;
            return null;
        }
    }

    @Test
    public void everyLookSceneLightAndPopulationSubmitsValidDraws() {
        AquariumOptions defaults = AquariumOptions.defaults();
        AquariumAppearanceRenderer renderer =
                new AquariumAppearanceRenderer(
                        RuntimeEnvironment.getApplication().getResources(), defaults);
        renderer.onSurfaceCreated(null, null);
        renderer.onSurfaceChanged(null, 3840, 2160);
        for (int look = 0; look < 6; look++) {
            for (int scene = 0; scene < 5; scene++) {
                for (int lighting = 0; lighting < 3; lighting++) {
                    AquariumOptions options =
                            new AquariumOptions(
                                    0,
                                    48,
                                    0,
                                    scene,
                                    1,
                                    1,
                                    lighting,
                                    0,
                                    lighting != 0,
                                    lighting != 1,
                                    lighting == 2 ? 5 : 2,
                                    127,
                                    look,
                                    0);
                    renderer.configure(options);
                    renderer.onDrawFrame(null);
                }
            }
        }
        assertTrue(Graphics.draws > 1000);
        assertTrue(Graphics.deletedTextures > 0);
        renderer.onSurfaceChanged(null, 1920, 1080);
        renderer.configure(defaults.withChoice(18, 3, 0));
        for (int frame = 0; frame < 360; frame++) {
            ShadowSystemClock.advanceBy(Duration.ofMillis(33));
            renderer.onDrawFrame(null);
        }
        renderer.configure(defaults.withChoice(2, 0, 0));
        for (int frame = 0; frame < 360; frame++) {
            ShadowSystemClock.advanceBy(Duration.ofMillis(33));
            renderer.onDrawFrame(null);
        }
    }

    @Test
    public void consumedFishAreHiddenInEveryAppearance() {
        for (int look = 0; look < 6; look++) {
            AquariumOptions options =
                    AquariumOptions.defaults().withChoice(2, 1, 0).withChoice(18, look, 0);
            FishMotion motion = new FishMotion(new java.util.Random(19));
            AquariumAppearanceRenderer renderer =
                    new AquariumAppearanceRenderer(
                            RuntimeEnvironment.getApplication().getResources(), options, motion);
            renderer.onSurfaceCreated(null, null);
            renderer.onSurfaceChanged(null, 1920, 1080);
            Graphics.draws = 0;
            renderer.onDrawFrame(null);
            int visible = Graphics.draws;
            motion.pose(0).visible = false;
            Graphics.draws = 0;
            renderer.onDrawFrame(null);
            assertEquals(
                    "The consumed fish must not draw in appearance " + look,
                    visible - 1,
                    Graphics.draws);
        }
    }

    @Test
    public void leftFacingFishAndVisitorsPitchTowardTheirPhysicalNose() {
        AquariumOptions options = AquariumOptions.defaults().withChoice(10, 1, 0);
        FishMotion motion = new FishMotion(new java.util.Random(33));
        motion.pose(0).yaw = (float) Math.PI;
        motion.pose(0).pitch = .31f;
        motion.pose(60).yaw = (float) Math.PI;
        motion.pose(60).pitch = .27f;
        AquariumAppearanceRenderer renderer =
                new AquariumAppearanceRenderer(
                        RuntimeEnvironment.getApplication().getResources(), options, motion);
        renderer.onSurfaceCreated(null, null);
        renderer.onSurfaceChanged(null, 1920, 1080);
        renderer.onDrawFrame(null);
        assertTrue(
                "Left-facing fish must pitch up when their pose pitches up",
                Graphics.transforms.stream().anyMatch(matrix -> matrix[0] < 0 && matrix[1] > 0));
        assertTrue(
                "The shark body must pitch toward its measured capture point",
                Graphics.transforms.stream().anyMatch(matrix -> matrix[0] < -3 && matrix[1] > .8f));
    }

    @Test
    public void opaqueBackgroundAvoidsBlendingWhileBubblesRetainTransparency() {
        AquariumAppearanceRenderer renderer =
                new AquariumAppearanceRenderer(
                        RuntimeEnvironment.getApplication().getResources(),
                        AquariumOptions.defaults());
        renderer.onSurfaceCreated(null, null);
        renderer.onSurfaceChanged(null, 3840, 2160);
        renderer.onDrawFrame(null);
        assertEquals(Boolean.FALSE, Graphics.blendedDraws.get(0));
        assertTrue(
                "All bubble draws keep their translucent edges",
                Graphics.blendedDraws.subList(1, Graphics.blendedDraws.size()).stream()
                        .allMatch(Boolean::booleanValue));
    }

    @Test
    public void sunlightGridCoversTheImageWithContinuousTextureCoordinates() {
        AquariumTextureShader shader = new AquariumTextureShader();
        shader.frame(AquariumOptions.defaults(), 1, new float[] {0, 0, 0});
        shader.draw(1, 0, 0, 2, 2, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1);
        java.nio.FloatBuffer vertices = Graphics.surface;
        double area = 0;
        for (int offset = 0; offset < Graphics.surfaceVertices * 4; offset += 12) {
            float ax = vertices.get(offset), ay = vertices.get(offset + 1);
            float bx = vertices.get(offset + 4), by = vertices.get(offset + 5);
            float cx = vertices.get(offset + 8), cy = vertices.get(offset + 9);
            double triangle = Math.abs((bx - ax) * (cy - ay) - (by - ay) * (cx - ax)) / 2;
            assertTrue("Every background triangle has area", triangle > 0);
            assertTrue(
                    "Lighting interpolation has a dense local footprint",
                    Math.max(Math.abs(bx - ax), Math.abs(cx - ax)) < .02f);
            area += triangle;
            for (int vertex = 0; vertex < 3; vertex++) {
                int index = offset + vertex * 4;
                assertEquals(vertices.get(index) + .5f, vertices.get(index + 2), .00001f);
                assertEquals(.5f - vertices.get(index + 1), vertices.get(index + 3), .00001f);
            }
            float centerX = (ax + bx + cx) / 3;
            float centerU =
                    (vertices.get(offset + 2)
                                    + vertices.get(offset + 6)
                                    + vertices.get(offset + 10))
                            / 3;
            assertEquals(
                    "Triangle interpolation keeps the source image aligned",
                    centerX + .5f,
                    centerU,
                    .00001f);
        }
        assertEquals("The grid covers the complete image", 1, area, .00001);
        int backgroundCount = Graphics.surfaceVertices;
        shader.draw(1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 2, 1);
        assertTrue(
                "Small bubbles do not inherit the background tessellation",
                Graphics.surfaceVertices < backgroundCount / 8);
    }

    @Test
    public void shaderCompilationAndLinkFailuresAreReported() {
        Graphics.compile = false;
        assertThrows(IllegalStateException.class, AquariumTextureShader::new);
        Graphics.compile = true;
        Graphics.link = false;
        assertThrows(IllegalStateException.class, AquariumTextureShader::new);
    }

    @Test
    public void textureUploadsFailVisiblyAndRespectTheGpuLimit() {
        Graphics.textureLimit = 64;
        Graphics.error = GLES20.GL_OUT_OF_MEMORY;
        AquariumAppearanceRenderer renderer =
                new AquariumAppearanceRenderer(
                        RuntimeEnvironment.getApplication().getResources(),
                        AquariumOptions.defaults());
        renderer.onSurfaceCreated(null, null);
        renderer.onSurfaceChanged(null, 3840, 2160);
        assertEquals(
                "Artwork upload failed",
                assertThrows(IllegalStateException.class, () -> renderer.onDrawFrame(null))
                        .getMessage());
    }

    @Test
    @Config(shadows = {Graphics.class, MissingArtwork.class})
    public void missingArtworkFailsVisibly() {
        AquariumAppearanceRenderer renderer =
                new AquariumAppearanceRenderer(
                        RuntimeEnvironment.getApplication().getResources(),
                        AquariumOptions.defaults());
        renderer.onSurfaceCreated(null, null);
        renderer.onSurfaceChanged(null, 3840, 2160);
        assertEquals(
                "Missing aquarium artwork",
                assertThrows(IllegalStateException.class, () -> renderer.onDrawFrame(null))
                        .getMessage());
    }
}
