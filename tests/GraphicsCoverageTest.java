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

        @Resetter
        public static void clear() {
            compile = link = true;
            textureLimit = 2048;
            error = draws = deletedTextures = 0;
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
    public static class MissingArtwork {
        @Implementation
        protected static Bitmap decodeResource(
                Resources resources, int id, BitmapFactory.Options options) {
            options.outWidth = 128;
            options.outHeight = 128;
            return null;
        }
    }

    @Test
    public void meshesContainFiniteTrianglePositionsAndNormals() {
        AquariumMesh[] meshes = {
            AquariumMesh.sphere(8, 12),
            AquariumMesh.plane(),
            AquariumMesh.fin(new float[] {0, 0, 0, 1, 0, 0, 0, 1, 0}),
            AquariumMesh.leaf(),
            AquariumMesh.coral(),
            AquariumMesh.tentacles(),
            AquariumMesh.rock()
        };
        for (AquariumMesh mesh : meshes) {
            assertEquals(mesh.count * 6, mesh.vertices.capacity());
            assertEquals(0, mesh.count % 3);
            for (int index = 0; index < mesh.vertices.capacity(); index++) {
                assertTrue(Float.isFinite(mesh.vertices.get(index)));
            }
        }
        assertEquals(6, meshes[1].count);
        assertEquals(3, meshes[2].count);
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
    public void shaderCompilationAndLinkFailuresAreReported() {
        Graphics.compile = false;
        assertThrows(IllegalStateException.class, AquariumShader::new);
        assertThrows(IllegalStateException.class, AquariumTextureShader::new);
        Graphics.compile = true;
        Graphics.link = false;
        assertThrows(IllegalStateException.class, AquariumShader::new);
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
