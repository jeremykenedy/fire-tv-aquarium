package com.jeremykenedy.firetv.aquarium;

import android.opengl.GLES20;
import android.opengl.Matrix;

/** Original silhouettes and animated appendages for the optional sea life. */
final class MarineLife {
    private final AquariumShader shader;
    private final AquariumMesh sphere = AquariumMesh.sphere(12, 18);
    private final AquariumMesh small = AquariumMesh.sphere(6, 10);
    private final AquariumMesh arms = AquariumMesh.tentacles();
    private final AquariumMesh dorsal =
            AquariumMesh.fin(new float[] {-.7f, .12f, 0, -.35f, 1.15f, 0, .30f, .16f, 0});
    private final AquariumMesh tail =
            AquariumMesh.fin(
                    new float[] {
                        0, 0, 0, -.80f, .95f, 0, -.55f, 0, 0, 0, 0, 0, -.55f, 0, 0, -.75f, -.62f, 0
                    });
    private final AquariumMesh flukes =
            AquariumMesh.fin(
                    new float[] {
                        0, 0, 0, -.55f, 0, -.85f, -.9f, 0, -.65f,
                        0, 0, 0, -.9f, 0, -.65f, -.3f, 0, 0,
                        0, 0, 0, -.3f, 0, 0, -.9f, 0, .65f,
                        0, 0, 0, -.9f, 0, .65f, -.55f, 0, .85f
                    });
    private final AquariumMesh flippers =
            AquariumMesh.fin(
                    new float[] {
                        .25f, -.12f, .1f, -.65f, -.55f, 1.0f, -.85f, -.2f, .18f,
                        .25f, -.12f, -.1f, -.85f, -.2f, -.18f, -.65f, -.55f, -1.0f
                    });
    private final AquariumMesh wings =
            AquariumMesh.fin(
                    new float[] {
                        .7f, 0, 0, -.35f, .18f, -1.5f, -.85f, 0, 0, .7f, 0, 0, -.85f, 0, 0, -.35f,
                        .18f, 1.5f
                    });
    private final float[] animal = new float[16];
    private final float[] model = new float[16];

    MarineLife(AquariumShader shader) {
        this.shader = shader;
    }

    void draw(AquariumOptions options, float seconds) {
        float speed = options.swimmingSpeed();
        if (options.hasCreature(AquariumOptions.SHARK)) {
            swimming(101, seconds, speed * .7f, 1.4f, -3, 1.55f);
            cetacean(0);
            swimming(103, seconds + 17, speed * .65f, -.8f, -5, 1.1f);
            cetacean(0);
        }
        if (options.hasCreature(AquariumOptions.WHALE)) {
            swimming(108, seconds, speed * .35f, 4.6f, -8, 2.9f);
            cetacean(1);
        }
        if (options.hasCreature(AquariumOptions.DOLPHIN)) {
            for (int i = 0; i < 2; i++) {
                swimming(112, seconds + i * 3, speed, 2.4f + i * .65f, -2f - i, 1.35f);
                cetacean(2);
            }
        }
        if (options.hasCreature(AquariumOptions.OCTOPUS)) {
            base(-7 + (float) Math.sin(seconds * .12) * .7f, -4.8f, 2, 1.15f);
            ball(0, .15f, 0, .48f, .80f, .43f, .88f, .36f, .23f, 0);
            ball(0, -.3f, .05f, .55f, .40f, .48f, .88f, .36f, .23f, 0);
            appendage(arms, 0, 0, 0, 1, 1, 1, .8f, .28f, .19f, 3, 0);
            eyes(.27f, -.22f, .43f, .12f);
        }
        if (options.hasCreature(AquariumOptions.TURTLE)) {
            swimming(118, seconds, speed * .45f, -1.8f, 3, 1.3f);
            ball(0, 0, 0, .92f, .43f, .65f, .20f, .38f, .21f, 6);
            ball(.99f, .06f, 0, .36f, .23f, .25f, .40f, .55f, .27f, 0);
            for (int side = -1; side <= 1; side += 2) {
                ball(.44f, -.14f, side * .62f, .55f, .10f, .35f, .36f, .51f, .25f, 0);
                ball(-.52f, -.10f, side * .5f, .32f, .08f, .28f, .36f, .51f, .25f, 0);
            }
            eyes(1.13f, .12f, .22f, .075f);
        }
        if (options.hasCreature(AquariumOptions.RAY)) {
            swimming(123, seconds, speed * .7f, -2.8f, -1, 1.35f);
            ball(0, 0, 0, .8f, .12f, .5f, .32f, .43f, .51f, 0);
            appendage(wings, 0, 0, 0, 1, 1, 1, .3f, .4f, .48f, 1, 0);
            ball(-1.15f, 0, 0, .8f, .04f, .04f, .27f, .36f, .42f, 0);
            eyes(.42f, .10f, .20f, .07f);
        }
        if (options.hasCreature(AquariumOptions.JELLYFISH)) jellyfish(seconds);
    }

    private void swimming(
            int index, float seconds, float speed, float height, float depth, float scale) {
        base(
                FishMotion.x(index, seconds, speed),
                height + (float) Math.sin(seconds * .30f + index) * .5f,
                depth,
                scale);
        Matrix.rotateM(animal, 0, index % 2 == 0 ? 0 : 180, 0, 1, 0);
        // A slight yaw exposes the body and paired fins instead of a flat side profile.
        Matrix.rotateM(animal, 0, (float) Math.sin(seconds * .18f + index) * 14, 0, 1, 0);
    }

    private void base(float x, float y, float z, float scale) {
        Matrix.setIdentityM(animal, 0);
        Matrix.translateM(animal, 0, x, y, z);
        Matrix.scaleM(animal, 0, scale, scale, scale);
    }

    private void cetacean(int type) {
        float r = type == 1 ? .14f : .34f;
        float g = type == 1 ? .26f : .45f;
        float b = type == 1 ? .36f : .54f;
        float length = type == 1 ? 1.9f : 1.6f;
        float height = type == 1 ? .65f : .43f;
        ball(0, 0, 0, length, height, .43f, r, g, b, 0);
        ball(.18f, -height * .40f, .03f, length * .80f, height * .5f, .42f, .71f, .78f, .78f, 0);
        ball(-1.5f, 0, 0, .62f, .17f, .18f, r, g, b, 0);
        appendage(
                dorsal,
                -.2f,
                height * .55f,
                0,
                type == 1 ? .6f : 1,
                type == 1 ? .6f : 1,
                1,
                r,
                g,
                b,
                1,
                0);
        appendage(flippers, 0, 0, 0, type == 1 ? 1.2f : 1, 1, 1, r, g, b, 1, 0);
        appendage(type == 0 ? tail : flukes, -1.95f, 0, 0, 1, 1, 1, r, g, b, 1, 0);
        if (type == 2) ball(1.65f, -.10f, 0, .40f, .11f, .13f, r, g, b, 0);
        eyes(length * .72f, .08f, .39f, .06f);
    }

    private void ball(
            float x,
            float y,
            float z,
            float sx,
            float sy,
            float sz,
            float r,
            float g,
            float b,
            int material) {
        System.arraycopy(animal, 0, model, 0, 16);
        Matrix.translateM(model, 0, x, y, z);
        Matrix.scaleM(model, 0, sx, sy, sz);
        shader.object(model, r, g, b, 1, material, 0, 0, 0);
        shader.draw(sphere);
    }

    private void appendage(
            AquariumMesh mesh,
            float x,
            float y,
            float z,
            float sx,
            float sy,
            float sz,
            float r,
            float g,
            float b,
            int motion,
            float phase) {
        System.arraycopy(animal, 0, model, 0, 16);
        Matrix.translateM(model, 0, x, y, z);
        Matrix.scaleM(model, 0, sx, sy, sz);
        shader.object(model, r, g, b, 1, 0, 0, motion, phase);
        shader.draw(mesh);
    }

    private void eyes(float x, float y, float z, float radius) {
        for (int side = -1; side <= 1; side += 2) {
            System.arraycopy(animal, 0, model, 0, 16);
            Matrix.translateM(model, 0, x, y, side * z);
            Matrix.scaleM(model, 0, radius, radius, radius * .6f);
            shader.object(model, .88f, .89f, .78f, 1, 0, 0, 0, 0);
            shader.draw(small);
            Matrix.translateM(model, 0, .12f, 0, side * .6f);
            Matrix.scaleM(model, 0, .58f, .65f, .55f);
            shader.object(model, .018f, .028f, .04f, 1, 0, 0, 0, 0);
            shader.draw(small);
        }
    }

    private void jellyfish(float seconds) {
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glDepthMask(false);
        for (int i = 0; i < 5; i++) {
            float y = (seconds * .15f + i * 3.5f) % 18 - 7;
            base(-13 + i * 6 + (float) Math.sin(seconds * .2f + i) * .5f, y, -2 + i * .5f, .7f);
            System.arraycopy(animal, 0, model, 0, 16);
            Matrix.scaleM(model, 0, .7f, .42f + (float) Math.sin(seconds * 1.6f + i) * .06f, .65f);
            shader.object(model, .53f, .77f, .95f, .5f, 7, 0, 0, 0);
            shader.draw(sphere);
            System.arraycopy(animal, 0, model, 0, 16);
            Matrix.scaleM(model, 0, .3f, 1.3f, .3f);
            shader.object(model, .55f, .79f, .96f, .55f, 7, 0, 3, i);
            shader.draw(arms);
        }
        GLES20.glDepthMask(true);
        GLES20.glDisable(GLES20.GL_BLEND);
    }
}
