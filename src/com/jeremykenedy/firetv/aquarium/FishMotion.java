package com.jeremykenedy.firetv.aquarium;

/** Stable swimming paths: fish leave the visible tank before wrapping around. */
final class FishMotion {
    private FishMotion() {}

    static float phase(int index) {
        return (index * 0.6180339887f) % 1f;
    }

    static float x(int index, float seconds, float speed) {
        float progress = phase(index) * 52f + seconds * speed * (0.6f + phase(index + 17) * 0.5f);
        float wrapped = ((progress % 52f) + 52f) % 52f - 26f;
        return index % 2 == 0 ? wrapped : -wrapped;
    }

    static float y(int index, float seconds) {
        return -4.1f
                + phase(index + 31) * 10.2f
                + (float) Math.sin(seconds * 0.25f + index * 1.7f) * 0.45f;
    }

    static float depth(int index) {
        return -3f + phase(index + 53) * 6f;
    }

    static float scale(int index) {
        return 0.65f + phase(index + 71) * 0.6f;
    }

    static float schoolX(int index, float seconds, float speed) {
        int group = index / 16;
        return x(group, seconds, speed) + (index % 4 - 1.5f) * 1.65f;
    }

    static float schoolY(int index, float seconds) {
        int group = index / 16;
        return -3.4f
                + group * 3.6f
                + (index % 16 / 4 - 1.5f) * .95f
                + (float) Math.sin(seconds * .35f + group) * .4f;
    }

    static float schoolDepth(int index) {
        return -2 + (index / 16) * 1.5f + (index % 3) * .3f;
    }

    static int direction(int index, boolean schooling) {
        return (schooling ? index / 16 : index) % 2 == 0 ? 1 : -1;
    }

    static int species(int index, int selection) {
        return selection == 0 ? index % 6 : selection - 1;
    }
}
