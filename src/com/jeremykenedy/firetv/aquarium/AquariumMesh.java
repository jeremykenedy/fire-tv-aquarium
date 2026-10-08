package com.jeremykenedy.firetv.aquarium;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

/** Position and normal data for small, reusable scene meshes. */
final class AquariumMesh {
    final FloatBuffer vertices;
    final int count;

    AquariumMesh(float[] data) {
        vertices =
                ByteBuffer.allocateDirect(data.length * 4)
                        .order(ByteOrder.nativeOrder())
                        .asFloatBuffer();
        vertices.put(data).position(0);
        count = data.length / 6;
    }

    static AquariumMesh sphere(int rows, int columns) {
        float[] data = new float[rows * columns * 6 * 6];
        int offset = 0;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int[] r = {row, row + 1, row, row + 1, row + 1, row};
                int[] c = {column, column, column + 1, column, column + 1, column + 1};
                for (int i = 0; i < 6; i++) {
                    double latitude = Math.PI * r[i] / rows;
                    double longitude = Math.PI * 2 * c[i] / columns;
                    float x = (float) (Math.sin(latitude) * Math.cos(longitude));
                    float y = (float) Math.cos(latitude);
                    float z = (float) (Math.sin(latitude) * Math.sin(longitude));
                    data[offset++] = x;
                    data[offset++] = y;
                    data[offset++] = z;
                    data[offset++] = x;
                    data[offset++] = y;
                    data[offset++] = z;
                }
            }
        }
        return new AquariumMesh(data);
    }

    static AquariumMesh plane() {
        return new AquariumMesh(
                new float[] {
                    -1, -1, 0, 0, 0, 1, 1, -1, 0, 0, 0, 1, -1, 1, 0, 0, 0, 1,
                    -1, 1, 0, 0, 0, 1, 1, -1, 0, 0, 0, 1, 1, 1, 0, 0, 0, 1
                });
    }

    static AquariumMesh fin(float[] points) {
        float[] data = new float[points.length * 2];
        for (int i = 0; i < points.length / 3; i++) {
            System.arraycopy(points, i * 3, data, i * 6, 3);
            data[i * 6 + 5] = 1;
        }
        return new AquariumMesh(data);
    }

    static AquariumMesh leaf() {
        ArrayList<Float> data = new ArrayList<>();
        for (int blade = 0; blade < 5; blade++) {
            for (int segment = 0; segment < 12; segment++) {
                int[] steps = {segment, segment + 1, segment, segment, segment + 1, segment + 1};
                int[] sides = {-1, -1, 1, 1, -1, 1};
                for (int i = 0; i < 6; i++) {
                    float t = steps[i] / 12f;
                    float width = (float) Math.sin(t * Math.PI) * 0.14f;
                    float lean = (blade - 2) * 0.20f;
                    vertex(
                            data,
                            lean * t * t + sides[i] * width,
                            t * (1.2f + (blade % 3) * .22f),
                            (float) Math.sin(t * 3) * .12f,
                            0,
                            0,
                            1);
                }
            }
        }
        return from(data);
    }

    static AquariumMesh coral() {
        ArrayList<Float> data = new ArrayList<>();
        branch(data, 0, 0, 0, 0, .60f, 0, .07f, 3);
        return from(data);
    }

    private static void branch(
            ArrayList<Float> data,
            float x,
            float y,
            float z,
            float dx,
            float dy,
            float dz,
            float radius,
            int depth) {
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float ux = dy / length;
        float uy = -dx / length;
        float vx = -dz * dx / (length * length);
        float vy = -dz * dy / (length * length);
        float vz = (dx * dx + dy * dy) / (length * length);
        for (int side = 0; side < 8; side++) {
            int[] rings = {0, 1, 0, 0, 1, 1};
            int[] corners = {side, side, side + 1, side + 1, side, side + 1};
            for (int i = 0; i < 6; i++) {
                float angle = corners[i] * (float) Math.PI / 4;
                float nx = ux * (float) Math.cos(angle) + vx * (float) Math.sin(angle);
                float ny = uy * (float) Math.cos(angle) + vy * (float) Math.sin(angle);
                float nz = vz * (float) Math.sin(angle);
                float r = radius * (rings[i] == 0 ? 1 : .65f);
                vertex(
                        data,
                        x + dx * rings[i] + nx * r,
                        y + dy * rings[i] + ny * r,
                        z + dz * rings[i] + nz * r,
                        nx,
                        ny,
                        nz);
            }
        }
        if (depth == 0) return;
        branch(
                data,
                x + dx,
                y + dy,
                z + dz,
                dx * .5f - dy * .48f,
                dy * .72f,
                dz * .6f + .09f,
                radius * .68f,
                depth - 1);
        branch(
                data,
                x + dx,
                y + dy,
                z + dz,
                dx * .5f + dy * .48f,
                dy * .72f,
                dz * .6f - .09f,
                radius * .68f,
                depth - 1);
    }

    static AquariumMesh tentacles() {
        ArrayList<Float> data = new ArrayList<>();
        for (int arm = 0; arm < 8; arm++) {
            float angle = arm * (float) Math.PI / 4;
            for (int segment = 0; segment < 16; segment++) {
                int[] steps = {segment, segment + 1, segment, segment, segment + 1, segment + 1};
                int[] sides = {0, 0, 1, 1, 0, 1};
                for (int side = 0; side < 6; side++) {
                    for (int i = 0; i < 6; i++) {
                        float t = steps[i] / 16f;
                        float ring = (side + sides[i]) * (float) Math.PI / 3;
                        float radius = .09f * (1 - t) + .015f;
                        float reach = .2f + t * 1.5f;
                        float curl = (float) Math.sin(t * Math.PI * 1.7) * .16f;
                        float nx = (float) Math.cos(ring) * (float) Math.sin(angle);
                        float nz = (float) Math.cos(ring) * -(float) Math.cos(angle);
                        float ny = (float) Math.sin(ring);
                        vertex(
                                data,
                                (float) Math.cos(angle) * reach + nx * radius,
                                -.4f - t * 1.3f + curl + ny * radius,
                                (float) Math.sin(angle) * reach + nz * radius,
                                nx,
                                ny,
                                nz);
                    }
                }
            }
        }
        return from(data);
    }

    static AquariumMesh rock() {
        AquariumMesh sphere = sphere(8, 12);
        float[] data = new float[sphere.vertices.capacity()];
        sphere.vertices.get(data);
        for (int i = 0; i < data.length; i += 6) {
            float variation =
                    .87f
                            + .13f
                                    * (float)
                                            (Math.sin(data[i] * 9 + data[i + 1] * 5)
                                                    * Math.cos(data[i + 2] * 7 - data[i + 1] * 4));
            data[i] *= variation;
            data[i + 1] *= variation;
            data[i + 2] *= variation;
        }
        return new AquariumMesh(data);
    }

    private static void vertex(
            ArrayList<Float> data, float x, float y, float z, float nx, float ny, float nz) {
        data.add(x);
        data.add(y);
        data.add(z);
        data.add(nx);
        data.add(ny);
        data.add(nz);
    }

    private static AquariumMesh from(ArrayList<Float> values) {
        float[] data = new float[values.size()];
        for (int i = 0; i < data.length; i++) data[i] = values.get(i);
        return new AquariumMesh(data);
    }
}
