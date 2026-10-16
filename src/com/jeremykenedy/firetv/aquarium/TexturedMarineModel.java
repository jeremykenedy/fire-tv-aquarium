package com.jeremykenedy.firetv.aquarium;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** A compact authored mesh, uploaded once for each OpenGL context. */
final class TexturedMarineModel {
    private final int vertexBuffer;
    private final int indexBuffer;
    private int texture;
    private boolean painted;
    private final AssetManager assets;
    private final String name;
    private final int count;
    private final int stride;
    private final float[] frontalUv = new float[4];
    private final float[] frontalBlend = new float[3];

    TexturedMarineModel(AssetManager assets, String name) {
        this.assets = assets;
        this.name = name;
        try {
            MarineMeshData mesh;
            try (InputStream input = assets.open("models/" + name + ".bin")) {
                mesh = new MarineMeshData(input);
            }
            stride = mesh.stride;
            count = mesh.count;
            texture = loadTexture(false);
            loadFrontalProjection();
            int[] ids = new int[2];
            GLES20.glGenBuffers(2, ids, 0);
            vertexBuffer = ids[0];
            indexBuffer = ids[1];
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBuffer);
            GLES20.glBufferData(
                    GLES20.GL_ARRAY_BUFFER,
                    mesh.vertices.remaining(),
                    mesh.vertices,
                    GLES20.GL_STATIC_DRAW);
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
            GLES20.glBufferData(
                    GLES20.GL_ELEMENT_ARRAY_BUFFER, count * 2, mesh.indices, GLES20.GL_STATIC_DRAW);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        } catch (IOException | JSONException error) {
            throw new IllegalStateException("Unable to load marine model " + name, error);
        }
    }

    void setPainted(boolean next) {
        if (painted == next) return;
        try {
            int replacement = loadTexture(next);
            GLES20.glDeleteTextures(1, new int[] {texture}, 0);
            texture = replacement;
            painted = next;
        } catch (IOException error) {
            throw new IllegalStateException("Unable to load marine skin " + name, error);
        }
    }

    private int loadTexture(boolean illustrated) throws IOException {
        Bitmap bitmap;
        try (InputStream input =
                assets.open("models/" + (illustrated ? "painted-" : "") + name + ".png")) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            bitmap = BitmapFactory.decodeStream(input, null, options);
        }
        if (bitmap == null) throw new IOException("Invalid marine texture");
        int[] ids = new int[1];
        GLES20.glGenTextures(1, ids, 0);
        int texture = ids[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        bitmap.recycle();
        return texture;
    }

    private void loadFrontalProjection() throws JSONException {
        String source;
        try (InputStream input = assets.open("models/" + name + "-source.json")) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = input.read(buffer)) != -1) bytes.write(buffer, 0, read);
            source = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException absentOptionalMetadata) {
            return;
        }
        float[] projection = parseFrontalProjection(source);
        frontalBlend[0] = projection[0];
        System.arraycopy(projection, 1, frontalUv, 0, 4);
        frontalBlend[1] = projection[5];
        frontalBlend[2] = projection[6];
    }

    static float[] parseFrontalProjection(String source) throws JSONException {
        JSONObject metadata = new JSONObject(source);
        float[] projection = new float[7];
        if (!metadata.has("frontal_projection")) return projection;
        JSONObject front = metadata.getJSONObject("frontal_projection");
        JSONArray uv = front.getJSONArray("uv");
        if (uv.length() != 4) throw new JSONException("Frontal skin needs four UV coefficients");
        projection[0] = 1;
        for (int i = 0; i < 4; i++) projection[i + 1] = (float) uv.getDouble(i);
        projection[5] = (float) front.getDouble("blend_start");
        projection[6] = (float) front.getDouble("blend_end");
        for (float value : projection) {
            if (Float.isNaN(value) || Float.isInfinite(value))
                throw new JSONException("Frontal skin coefficients must be finite");
        }
        if (projection[6] <= projection[5]) throw new JSONException("Frontal blend range is empty");
        return projection;
    }

    void draw(
            int position, int normal, int uv, int flex, int finMask, int frontUv, int frontBlend) {
        GLES20.glUniform4fv(frontUv, 1, frontalUv, 0);
        GLES20.glUniform3fv(frontBlend, 1, frontalBlend, 0);
        GLES20.glUniform1f(finMask, stride == 36 ? 1 : 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBuffer);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
        GLES20.glEnableVertexAttribArray(position);
        GLES20.glEnableVertexAttribArray(normal);
        GLES20.glEnableVertexAttribArray(uv);
        GLES20.glVertexAttribPointer(position, 3, GLES20.GL_FLOAT, false, stride, 0);
        GLES20.glVertexAttribPointer(normal, 3, GLES20.GL_FLOAT, false, stride, 12);
        GLES20.glVertexAttribPointer(uv, 2, GLES20.GL_FLOAT, false, stride, 24);
        if (stride == 36) {
            GLES20.glEnableVertexAttribArray(flex);
            GLES20.glVertexAttribPointer(flex, 1, GLES20.GL_FLOAT, false, stride, 32);
        } else {
            GLES20.glDisableVertexAttribArray(flex);
            GLES20.glVertexAttrib1f(flex, 0);
        }
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, count, GLES20.GL_UNSIGNED_SHORT, 0);
        GLES20.glDisableVertexAttribArray(position);
        GLES20.glDisableVertexAttribArray(normal);
        GLES20.glDisableVertexAttribArray(uv);
        GLES20.glDisableVertexAttribArray(flex);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }
}
