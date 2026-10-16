package com.jeremykenedy.firetv.aquarium;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** The versioned offline mesh payload used by the marine renderer. */
final class MarineMeshData {
    final ByteBuffer vertices;
    final ByteBuffer indices;
    final int stride;
    final int count;

    MarineMeshData(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = input.read(chunk)) != -1) output.write(chunk, 0, read);
        byte[] bytes = output.toByteArray();
        if (bytes.length < 16) throw new IOException("Marine mesh header is incomplete");
        ByteBuffer data = ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.LITTLE_ENDIAN);
        data.put(bytes).rewind();
        if (data.getInt() != 0x48534946) throw new IOException("Unsupported marine mesh format");
        int version = data.getInt();
        if (version != 1 && version != 2) throw new IOException("Unsupported marine mesh version");
        stride = version == 2 ? 36 : 32;
        int vertexCount = data.getInt();
        count = data.getInt();
        validateDimensions(vertexCount, count, stride, bytes.length);
        data.position(16);
        vertices = data.slice().order(ByteOrder.LITTLE_ENDIAN);
        vertices.limit(vertexCount * stride);
        data.position(16 + vertexCount * stride);
        indices = data.slice().order(ByteOrder.LITTLE_ENDIAN);
        indices.limit(count * 2);
    }

    private static void validateDimensions(int vertexCount, int count, int stride, int length)
            throws IOException {
        if (vertexCount < 3
                || vertexCount > 65535
                || count < 3
                || count % 3 != 0
                || length != 16L + vertexCount * (long) stride + count * 2L)
            throw new IOException("Invalid marine mesh dimensions");
    }
}
