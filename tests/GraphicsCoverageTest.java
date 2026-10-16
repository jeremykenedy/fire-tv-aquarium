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
        static final java.util.List<float[]> transforms = new java.util.ArrayList<>();
        static final java.util.List<Float> angles = new java.util.ArrayList<>();

        @Resetter
        public static void clear() {
            compile = link = true;
            textureLimit = 2048;
            error = draws = deletedTextures = 0;
            angles.clear();
            transforms.clear();
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
        protected static void glDrawArrays(int mode, int first, int count) {
            assertEquals(GLES20.GL_TRIANGLES, mode);
            assertTrue(count > 0 && count % 3 == 0);
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
