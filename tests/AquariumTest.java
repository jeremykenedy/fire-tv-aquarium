package com.jeremykenedy.firetv.aquarium;

/** Exercises corrupt settings and long-running swimming paths without Android. */
public final class AquariumTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        AquariumOptions low = new AquariumOptions(-1, -1, -1, -1, -1, -1, -1, -1, false, false);
        AquariumOptions high =
                new AquariumOptions(999, 999, 999, 999, 999, 999, 999, 999, true, true);
        check(
                low.count == 0 && low.species == 0 && low.mode == 0,
                "Negative preferences must be safe");
        check(
                high.count == 60 && high.species == 6 && high.mode == 1 && high.clock == 2,
                "Out-of-range preferences must be safe");
        for (int n = 2; n <= 7; n++) {
            check(AquariumOptions.cycle(0, -1, n) == n - 1, "Left wraps to last option");
            check(AquariumOptions.cycle(n - 1, 1, n) == 0, "Right wraps to first option");
        }
        for (int preset = 1; preset < AquariumOptions.POPULATIONS.length; preset++)
            check(
                    AquariumOptions.POPULATION_COUNTS[preset] > 0
                            && AquariumOptions.POPULATION_COUNTS[preset]
                                    <= AquariumOptions.MAX_FISH,
                    "Preset population bounds");
        AquariumOptions visitors = new AquariumOptions(0, 48, 0, 4, 1, 1, 0, 0, true, true, 5, 127);
        check(visitors.schools() && visitors.scene == 4, "Schools and deep sea settings");
        for (int bit = 1; bit <= 64; bit *= 2)
            check(visitors.hasCreature(bit), "Independent creature toggles");
        for (int group = 0; group < 3; group++) {
            int first = group * 16;
            for (int i = first; i < first + 16; i++) {
                check(
                        FishMotion.direction(i, true) == FishMotion.direction(first, true),
                        "School members share direction");
                for (float t = 0; t < 3600; t += 31.7f) {
                    float offset =
                            FishMotion.schoolX(i, t, .8f) - FishMotion.schoolX(first, t, .8f);
                    check(
                            Math.abs(offset - (i % 4) * 1.65f) < .001f,
                            "School members preserve spacing through wrapping");
                    check(Float.isFinite(FishMotion.schoolY(i, t)), "School paths remain finite");
                }
            }
        }
        int[] populations = new int[6];
        for (int i = 0; i < AquariumOptions.MAX_FISH; i++) {
            populations[FishMotion.species(i, 0)]++;
            for (int choice = 1; choice <= 6; choice++)
                check(
                        FishMotion.species(i, choice) == choice - 1,
                        "Species choice must apply to every fish");
            check(
                    FishMotion.depth(i) >= -3 && FishMotion.depth(i) <= 3,
                    "Fish remain inside tank depth");
            check(FishMotion.scale(i) >= .65f && FishMotion.scale(i) <= 1.25f, "Fish scale bounds");
            for (float speed : new float[] {.45f, .8f, 1.35f}) {
                for (float t = 0; t <= 86400; t += 97.3f) {
                    float x = FishMotion.x(i, t, speed);
                    float next = FishMotion.x(i, t + .03f, speed);
                    check(
                            Float.isFinite(x) && x >= -26 && x <= 26,
                            "Swimming must stay finite after a day");
                    check(
                            FishMotion.y(i, t) >= -4.6f && FishMotion.y(i, t) <= 6.6f,
                            "Vertical swimming bounds");
                    if (Math.abs(next - x) > 1)
                        check(
                                Math.abs(x) > 25 && Math.abs(next) > 25,
                                "Fish may wrap only beyond visible tank edges");
                    else
                        check(
                                (i % 2 == 0 ? next - x : x - next) >= 0,
                                "Fish keep swimming in their assigned direction");
                }
            }
        }
        for (int population : populations)
            check(population == 10, "Mixed mode must distribute all species evenly");
        System.out.println("Aquarium tests passed: " + checks + " checks");
    }
}
