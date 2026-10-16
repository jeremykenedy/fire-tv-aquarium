package com.jeremykenedy.firetv.aquarium;

/** Persistent swimming state with independent decisions and gradual changes of direction. */
final class FishMotion {
    private final Pose[] swimmers = new Pose[128];
    private float previous = -1;
    private float stepSeconds;
    private boolean flatAppearance;
    private final float[][] meshMouths = new float[7][];

    private int marineArtwork;
    private float screenAspect = 16f / 9;
    // Mouth positions in each atlas cell, relative to its center, with up positive.
    private static final float[][] MARINE_MOUTHS = {
        {.44f, -.04f, .47f, .02f, .46f, .07f},
        {.44f, -.05f, .47f, .18f, .46f, .03f},
        {.44f, -.11f, .47f, .01f, .44f, .04f}
    };

    void setMeshMouth(int type, float x, float y, float z, float scale) {
        meshMouths[type] = new float[] {x * scale, y * scale, z * scale};
    }

    void setMarineArtwork(int artwork, float aspect) {
        marineArtwork = artwork;
        screenAspect = aspect;
    }

    void setFlatAppearance(boolean flat) {
        flatAppearance = flat;
    }

    FishMotion() {
        this(new java.util.Random());
    }

    FishMotion(java.util.Random random) {
        for (int i = 0; i < swimmers.length; i++)
            swimmers[i] = new Pose(new java.util.Random(random.nextLong()));
    }

    Pose pose(int index) {
        return swimmers[index];
    }

    void update(float seconds, float speed, boolean schooling, int count) {
        update(seconds, speed, schooling, count, 0);
    }

    void update(float seconds, float speed, boolean schooling, int count, int creatures) {
        if (previous < 0) previous = seconds;
        float remaining = Math.max(0, Math.min(.25f, seconds - previous));
        previous = seconds;
        while (remaining > .00001f) {
            float step = Math.min(.04f, remaining);
            stepSeconds = seconds - remaining;
            separate(count);
            feed(creatures, count, step);
            advanceSwimmers(step, speed, schooling, count);
            remaining -= step;
        }
    }

    private void advanceSwimmers(float step, float speed, boolean schooling, int count) {
        for (int i = 0; i < swimmers.length; i++) {
            Pose swimmer = swimmers[i];
            Pose leader = schooling && i < count ? swimmers[i / 16 * 16] : null;
            if (leader != null && (!leader.visible || leader.entering)) leader = null;
            swimmer.advance(step, speed * pace(i, swimmer), leader);
        }
    }

    private float pace(int i, Pose swimmer) {
        return i == 67
                ? (swimmer.pursuing ? 1.8f : .6f)
                : i == 60 || i == 61 || i == 95 || i == 96 ? (swimmer.pursuing ? 1.7f : .85f) : 1;
    }

    private void separate(int count) {
        for (int i = 0; i < count; i++) {
            swimmers[i].escapeX = swimmers[i].escapeY = swimmers[i].escapeZ = 0;
            swimmers[i].spacingX = swimmers[i].spacingY = swimmers[i].spacingZ = 0;
        }
        for (int i = 0; i < count; i++) {
            if (!swimmers[i].visible) continue;
            for (int j = i + 1; j < count; j++) {
                if (!swimmers[j].visible) continue;
                Pose a = swimmers[i];
                Pose b = swimmers[j];
                float dx = a.x - b.x;
                float dy = a.y - b.y;
                float dz = a.z - b.z;
                float distance = dx * dx + dy * dy + dz * dz;
                if (distance < 6.25f) {
                    float push = 1 / Math.max(.2f, distance);
                    a.spacingX += dx * push;
                    b.spacingX -= dx * push;
                    a.spacingY += dy * push;
                    b.spacingY -= dy * push;
                    a.spacingZ += dz * push;
                    b.spacingZ -= dz * push;
                }
            }
        }
    }

    private void feed(int creatures, int count, float step) {
        boolean sharkA = hunts(60, step);
        boolean sharkB = hunts(61, step);
        boolean whale = hunts(67, step);
        boolean dolphinA = hunts(95, step);
        boolean dolphinB = hunts(96, step);
        if ((creatures & AquariumOptions.SHARK) != 0) feedSharks(count, sharkA, sharkB);
        if ((creatures & AquariumOptions.WHALE) != 0 && count >= 6 && whale)
            hunt(67, count, flatAppearance ? 8.8f : 8.2f, 1, true);
        if ((creatures & AquariumOptions.DOLPHIN) != 0) feedDolphins(count, dolphinA, dolphinB);
    }

    private void feedSharks(int count, boolean sharkA, boolean sharkB) {
        if (sharkA)
            hunt(60, count, flatAppearance ? 5.7f : 3.9f, flatAppearance ? .8f : .55f, false);
        if (sharkB)
            hunt(61, count, flatAppearance ? 5.7f : 3.9f, flatAppearance ? .8f : .55f, false);
    }

    private void feedDolphins(int count, boolean dolphinA, boolean dolphinB) {
        if (dolphinA) hunt(95, count, flatAppearance ? 3.9f : 3.6f, .35f, false);
        if (dolphinB) hunt(96, count, flatAppearance ? 3.9f : 3.6f, .35f, false);
    }

    private boolean hunts(int index, float step) {
        Pose predator = swimmers[index];
        predator.pursuing = false;
        predator.feedingClock = (predator.feedingClock + step) % 36;
        predator.hunting = predator.feedingClock >= 27;
        return predator.hunting;
    }

    private float[] feedingSchool(int count) {
        float centerX = 0;
        float centerY = 0;
        float centerZ = 0;
        int largest = 5;
        for (int anchor = 0; anchor < count; anchor++) {
            Pose center = swimmers[anchor];
            if (!center.visible) continue;
            int members = 0;
            float sumX = 0;
            float sumY = 0;
            float sumZ = 0;
            for (int member = 0; member < count; member++) {
                Pose fish = swimmers[member];
                if (!fish.visible
                        || Math.abs(fish.x - center.x) >= 4
                        || Math.abs(fish.y - center.y) >= 2.5f
                        || Math.abs(fish.z - center.z) >= 2) continue;
                members++;
                sumX += fish.x;
                sumY += fish.y;
                sumZ += fish.z;
            }
            if (members > largest) {
                largest = members;
                centerX = sumX / members;
                centerY = sumY / members;
                centerZ = sumZ / members;
            }
        }
        return largest < 6 ? null : new float[] {centerX, centerY, centerZ};
    }

    private float[] mouth(int index, float nose, float[] meshMouth) {
        Pose predator = swimmers[index];
        float mouthX =
                predator.x
                        + (float) Math.cos(predator.yaw) * (float) Math.cos(predator.pitch) * nose;
        float mouthZ =
                predator.z
                        - (float) Math.sin(predator.yaw) * (float) Math.cos(predator.pitch) * nose;
        float mouthY = predator.y + (float) Math.sin(predator.pitch) * nose;
        if (flatAppearance) {
            int species = index == 67 ? 1 : index >= 95 ? 2 : 0;
            float size = species == 1 ? .95f : species == 0 ? .62f : .42f;
            float[] mouths = MARINE_MOUTHS[marineArtwork];
            float localX = mouths[species * 2];
            float localY = mouths[species * 2 + 1];
            float c = (float) Math.cos(predator.pitch);
            float s = (float) Math.sin(predator.pitch);
            float forward = (localX * c - localY * s) * size * 22;
            float facing = (float) Math.cos(predator.yaw);
            mouthX = predator.x + Math.copySign(Math.max(.16f, Math.abs(facing)), facing) * forward;
            mouthZ = predator.z - (float) Math.sin(predator.yaw) * forward;
            mouthY = predator.y + (localX * s + localY * c) * size * screenAspect * 12;
        }
        if (meshMouth != null) {
            float c = (float) Math.cos(predator.pitch);
            float s = (float) Math.sin(predator.pitch);
            float x = meshMouth[0] * c - meshMouth[1] * s;
            float y = meshMouth[0] * s + meshMouth[1] * c;
            float cy = (float) Math.cos(predator.yaw);
            float sy = (float) Math.sin(predator.yaw);
            mouthX = predator.x + x * cy + meshMouth[2] * sy;
            mouthY = predator.y + y;
            mouthZ = predator.z - x * sy + meshMouth[2] * cy;
        }
        return new float[] {mouthX, mouthY, mouthZ};
    }

    private Pose nearestPrey(Pose predator, int count, float radius, float[] mouth) {
        Pose nearest = null;
        float best = Float.MAX_VALUE;
        float mouthX = mouth[0];
        float mouthY = mouth[1];
        float mouthZ = mouth[2];
        for (int i = 0; i < count; i++) {
            Pose prey = swimmers[i];
            if (!prey.visible) continue;
            float dx = prey.x - mouthX;
            float dy = prey.y - mouthY;
            float dz = prey.z - mouthZ;
            float distance = dx * dx + dy * dy + dz * dz;
            if (distance < radius * radius) {
                predator.captures++;
                predator.lastCaptureSeconds = stepSeconds;
                predator.lastPrey = i;
                prey.visible = false;
                prey.hiddenFor = prey.between(12, 24);
                continue;
            }
            if (distance < 36) {
                float weight = 1 / Math.max(.5f, distance);
                prey.escapeX += dx * weight;
                prey.escapeY += dy * weight;
                prey.escapeZ += dz * weight;
            }
            if (distance < best) {
                best = distance;
                nearest = prey;
            }
        }
        return nearest;
    }

    private void hunt(int index, int count, float nose, float radius, boolean school) {
        float[] center = school ? feedingSchool(count) : new float[] {0, 0, 0};
        if (center == null) return;
        Pose predator = swimmers[index];
        float[] meshMouth = meshMouths[index == 67 ? 1 : index >= 95 ? 5 : 0];
        float[] mouth = mouth(index, nose, meshMouth);
        float mouthX = mouth[0];
        float mouthY = mouth[1];
        float mouthZ = mouth[2];
        Pose nearest = nearestPrey(predator, count, radius, mouth);
        if (nearest != null) {
            predator.pursuing = true;
            if (school) {
                predator.targetX = center[0];
                predator.targetY = center[1];
                predator.targetZ = center[2];
            } else {
                predator.targetX = nearest.x;
                predator.targetY = nearest.y;
                predator.targetZ = nearest.z;
            }
            if (meshMouth != null) {
                predator.targetX -= mouthX - predator.x;
                predator.targetY -= mouthY - predator.y;
                predator.targetZ -= mouthZ - predator.z;
            }
            predator.decision = 1;
            predator.pause = 0;
            predator.pace = 1.25f;
        }
    }

    static final class Pose {
        float x;
        float y;
        float z;
        float yaw;
        float pitch;
        float vx;
        float vy;
        float vz;
        float lastCaptureSeconds = -1;
        int lastPrey = -1;
        boolean visible = true;
        boolean hunting;
        int captures;
        private boolean entering;
        private boolean pursuing;
        private float feedingClock;
        private float hiddenFor;
        private float escapeX;
        private float escapeY;
        private float escapeZ;
        private float spacingX;
        private float spacingY;
        private float spacingZ;
        private final java.util.Random random;
        private float targetX;
        private float targetY;
        private float targetZ;
        private float decision;
        private float pause;
        private float pace;
        private float velocity;
        private final float cruising;
        private final float turning;

        Pose(java.util.Random random) {
            this.random = random;
            x = between(-16, 16);
            y = between(-3.6f, 5.1f);
            z = between(-2.7f, 2.7f);
            yaw = between(-(float) Math.PI, (float) Math.PI);
            cruising = between(.65f, 1.25f);
            turning = between(.65f, 1.1f);
            velocity = cruising * .6f;
            chooseTarget();
            feedingClock = between(0, 36);
        }

        private float between(float low, float high) {
            return low + random.nextFloat() * (high - low);
        }

        private void chooseTarget() {
            targetX = Math.max(-17, Math.min(17, x + between(-11, 11)));
            targetY = Math.max(-3.8f, Math.min(5.5f, y + between(-5, 5)));
            targetZ = Math.max(-2.7f, Math.min(2.7f, z + between(-4, 4)));
            decision = between(4, 11);
            pace = between(.65f, 1.35f);
            pause = random.nextFloat() < .14f ? between(.8f, 2.8f) : 0;
        }

        private void orient(float step, float dx, float dy, float dz) {
            float desiredYaw = (float) Math.atan2(-dz, dx);
            float turn = desiredYaw - yaw;
            while (turn > Math.PI) turn -= (float) (Math.PI * 2);
            while (turn < -Math.PI) turn += (float) (Math.PI * 2);
            yaw += Math.max(-turning * step, Math.min(turning * step, turn));
            float desiredPitch = (float) Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));
            desiredPitch = Math.max(-.38f, Math.min(.38f, desiredPitch));
            pitch += Math.max(-.3f * step, Math.min(.3f * step, desiredPitch - pitch));
        }

        private void steer(
                float step, float targetDx, float targetDy, float targetDz, Pose leader) {
            float dx = targetDx;
            float dy = targetDy;
            float dz = targetDz;
            if (leader != null && leader != this) {
                float distance = (leader.x - x) * (leader.x - x) + (leader.y - y) * (leader.y - y);
                if (distance > 16) {
                    dx += (leader.x - x) * .8f;
                    dy += (leader.y - y) * .6f;
                    dz += (leader.z - z) * .5f;
                }
            }
            dx += spacingX * 7;
            dy += spacingY * 5;
            dz += spacingZ * 5;
            dx += escapeX * 16;
            dy += escapeY * 10;
            dz += escapeZ * 12;
            // Turn before reaching the glass rather than wrapping or reflecting a trajectory.
            if (!entering && Math.abs(x + vx * 4) > 16.5f) dx = -x * 2;
            if (y + vy * 4 > 5.3f || y + vy * 4 < -3.8f) dy = 1 - y;
            if (Math.abs(z + vz * 4) > 2.6f) dz = -z * 2;
            orient(step, dx, dy, dz);
        }

        private boolean restoreVisibility(float step) {
            if (!visible) {
                hiddenFor -= step;
                if (hiddenFor > 0) return false;
                visible = true;
                x = random.nextBoolean() ? -27 : 27;
                entering = true;
                y = between(-3.6f, 5.1f);
                z = between(-2.7f, 2.7f);
                yaw = x < 0 ? 0 : (float) Math.PI;
                velocity = 0;
                chooseTarget();
            }
            return true;
        }

        private void swimVelocity(float step, float speed) {
            if (pause > 0) pause = Math.max(0, pause - step);
            float desiredSpeed =
                    pause > 0
                            ? .015f
                            : speed
                                    * cruising
                                    * pace
                                    * (Math.abs(escapeX) + Math.abs(escapeZ) > .1f ? 1.6f : 1);
            velocity += (desiredSpeed - velocity) * Math.min(1, step * 1.8f);
            vx = (float) Math.cos(yaw) * (float) Math.cos(pitch) * velocity;
            vy = (float) Math.sin(pitch) * velocity;
            vz = -(float) Math.sin(yaw) * (float) Math.cos(pitch) * velocity;
        }

        private void advance(float step, float speed, Pose leader) {
            if (!restoreVisibility(step)) return;
            decision -= step;
            float dx = targetX - x;
            float dy = targetY - y;
            float dz = targetZ - z;
            if (decision <= 0 || dx * dx + dy * dy + dz * dz < 1.4f) chooseTarget();
            dx = targetX - x;
            dy = targetY - y;
            dz = targetZ - z;
            steer(step, dx, dy, dz, leader);
            swimVelocity(step, speed);
            x = Math.max(entering ? -27 : -19, Math.min(entering ? 27 : 19, x + vx * step));
            if (entering && Math.abs(x) < 18.5f) entering = false;
            y = Math.max(-4.4f, Math.min(6.1f, y + vy * step));
            z = Math.max(-3.2f, Math.min(3.2f, z + vz * step));
        }
    }

    static float phase(int index) {
        return (index * 0.6180339887f) % 1f;
    }

    static float scale(int index) {
        return 0.65f + phase(index + 71) * 0.6f;
    }

    static int species(int index, int selection) {
        return selection == 0 ? index % 6 : selection - 1;
    }
}
