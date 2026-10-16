package com.jeremykenedy.firetv.aquarium;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, shadows = GraphicsCoverageTest.Graphics.class)
public final class MarineModelTest {
    @Before
    public void resetGraphics() {
        GraphicsCoverageTest.Graphics.clear();
    }

    @Test
    public void versionedPayloadsRetainVertexAndIndexBoundaries() throws Exception {
        for (int version : new int[] {1, 2}) {
            MarineMeshData mesh = decode(fixture(version, 3, 3));
            assertEquals(version == 1 ? 32 : 36, mesh.stride);
            assertEquals(3, mesh.count);
            assertEquals(mesh.stride * 3, mesh.vertices.remaining());
            assertEquals(6, mesh.indices.remaining());
            assertTrue(mesh.vertices.isDirect());
        }
    }

    @Test
    public void unprojectedMeshesDoNotRequireOptionalProjectionMetadata() {
        TexturedMarineModel mesh =
                new TexturedMarineModel(
                        RuntimeEnvironment.getApplication().getAssets(), "transparent-body");
        mesh.draw(0, 1, 2, 3, 4, 5, 6);
        assertEquals(1, GraphicsCoverageTest.Graphics.draws);
    }

    @Test
    public void malformedPayloadsFailBeforeGpuUpload() {
        byte[] wrongMagic = fixture(1, 3, 3);
        wrongMagic[0] = 0;
        byte[][] invalid = {
            new byte[0],
            wrongMagic,
            fixture(0, 3, 3),
            fixture(3, 3, 3),
            fixture(1, 2, 3),
            fixture(1, 3, 2),
            fixture(1, 3, 4),
            Arrays.copyOf(fixture(2, 3, 3), 100),
            ByteBuffer.allocate(16)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putInt(0x48534946)
                    .putInt(1)
                    .putInt(65536)
                    .putInt(3)
                    .array()
        };
        for (byte[] bytes : invalid) assertThrows(IOException.class, () -> decode(bytes));
    }

    @Test
    public void bothMeshVersionsSubmitIndexedTriangles() {
        for (String species : new String[] {"barramundi", "clownfish"}) {
            TexturedMarineModel model =
                    new TexturedMarineModel(
                            RuntimeEnvironment.getApplication().getAssets(), species);
            model.draw(0, 1, 2, 3, 4, 5, 6);
        }
        assertEquals(2, GraphicsCoverageTest.Graphics.draws);
        assertThrows(
                IllegalStateException.class,
                () ->
                        new TexturedMarineModel(
                                RuntimeEnvironment.getApplication().getAssets(), "missing-model"));
    }

    @Test
    public void switchingSkinSetsReplacesTheTextureWithoutReplacingGeometry() {
        TexturedMarineModel model =
                new TexturedMarineModel(
                        RuntimeEnvironment.getApplication().getAssets(), "clownfish");
        model.setPainted(false);
        model.setPainted(true);
        model.setPainted(true);
        model.setPainted(false);
        assertEquals(2, GraphicsCoverageTest.Graphics.deletedTextures);
        TexturedMarineModel reference =
                new TexturedMarineModel(
                        RuntimeEnvironment.getApplication().getAssets(), "barramundi");
        assertThrows(IllegalStateException.class, () -> reference.setPainted(true));
    }

    @Test
    public void authoredTailMotionDistinguishesCetaceansFromFish() {
        for (String species :
                new String[] {
                    "clownfish", "great-white-shark", "humpback-whale", "bottlenose-dolphin"
                }) {
            TexturedMarineModel model =
                    new TexturedMarineModel(
                            RuntimeEnvironment.getApplication().getAssets(), species);
            assertEquals(
                    species.equals("humpback-whale") || species.equals("bottlenose-dolphin"),
                    model.hasVerticalTail());
        }
    }

    @Implements(BitmapFactory.class)
    public static class MissingModelTexture extends org.robolectric.shadows.ShadowBitmapFactory {
        @Implementation
        protected static Bitmap decodeStream(
                InputStream input, android.graphics.Rect padding, BitmapFactory.Options options) {
            return null;
        }
    }

    @Test
    @Config(shadows = {GraphicsCoverageTest.Graphics.class, MissingModelTexture.class})
    public void undecodableSkinIsReportedBeforeGpuUpload() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        new TexturedMarineModel(
                                RuntimeEnvironment.getApplication().getAssets(), "clownfish"));
        assertEquals(0, GraphicsCoverageTest.Graphics.draws);
    }

    @Test
    public void shaderFailuresAreReported() {
        GraphicsCoverageTest.Graphics.compile = false;
        assertThrows(IllegalStateException.class, MarineModelShader::new);
        GraphicsCoverageTest.Graphics.compile = true;
        GraphicsCoverageTest.Graphics.link = false;
        assertThrows(IllegalStateException.class, MarineModelShader::new);
    }

    @Test
    public void visitorMouthMetadataRequiresThreeFiniteCoordinates() throws Exception {
        float[] point = MarineVisitorRenderer.parseMouth("{\"mouth_local\":[1,.2,.1]}");
        org.junit.Assert.assertArrayEquals(new float[] {1, .2f, .1f}, point, .00001f);
        for (String invalid :
                new String[] {
                    "{}",
                    "{\"mouth_local\":[1,2]}",
                    "{\"mouth_local\":[1,2,1e300]}",
                    "{\"mouth_local\":[1,2,\"NaN\"]}"
                })
            assertThrows(
                    org.json.JSONException.class, () -> MarineVisitorRenderer.parseMouth(invalid));
        assertThrows(
                IllegalStateException.class,
                () ->
                        MarineVisitorRenderer.mouth(
                                RuntimeEnvironment.getApplication().getAssets(), "missing"));
    }

    @Test
    public void frontalSkinsRequireFiniteProjectionAndAnOrderedBlendRange() throws Exception {
        org.junit.Assert.assertArrayEquals(
                new float[7], TexturedMarineModel.parseFrontalProjection("{}"), 0);
        String valid =
                "{\"frontal_projection\":{\"uv\":[.7,.6,-1.7,.6],\"blend_start\":.1,\"blend_end\":.4}}";
        assertEquals(1, TexturedMarineModel.parseFrontalProjection(valid)[0], 0);
        for (String invalid :
                new String[] {
                    valid.replace(".7,.6,-1.7,.6", ".7,.6"),
                    valid.replace(".7,.6,-1.7,.6", "1e300,.6,-1.7,.6"),
                    valid.replace(".7,.6,-1.7,.6", "\"NaN\",.6,-1.7,.6"),
                    valid.replace("\"blend_end\":.4", "\"blend_end\":.1")
                })
            assertThrows(
                    org.json.JSONException.class,
                    () -> TexturedMarineModel.parseFrontalProjection(invalid));
    }

    private static MarineMeshData decode(byte[] bytes) throws IOException {
        return new MarineMeshData(new ByteArrayInputStream(bytes));
    }

    private static byte[] fixture(int version, int vertices, int indices) {
        int stride = version == 2 ? 36 : 32;
        return ByteBuffer.allocate(16 + vertices * stride + indices * 2)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(0x48534946)
                .putInt(version)
                .putInt(vertices)
                .putInt(indices)
                .array();
    }
}
