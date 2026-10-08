package com.jeremykenedy.firetv.aquarium;

/** Exercises corrupt settings and long-running swimming paths without Android. */
public final class AquariumTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void randomChoices() {
        AquariumOptions defaults = AquariumOptions.defaults();
        java.util.Random random = new java.util.Random(12345);
        for (int control = 0; control < 20; control++) {
            AquariumOptions selected = defaults;
            for (int i = 0; i <= defaults.choiceCount(control); i++) {
                check(!selected.isRandom(control), "Fixed choices remain fixed");
                selected = selected.adjust(control, 1);
                if (selected.isRandom(control)) break;
            }
            check(selected.isRandom(control), "Every setting can select Random");
            check(!selected.adjust(control, -1).isRandom(control), "Left exits Random");
            check(!selected.adjust(control, 1).isRandom(control), "Right exits Random");
            for (int sample = 0; sample < 100; sample++) {
                AquariumOptions resolved = AquariumRandomizer.resolve(selected, random);
                check(resolved.randomMask == 0, "Rendering receives concrete choices");
                for (int other = 0; other < 20; other++) {
                    check(
                            resolved.choice(other) >= 0
                                    && resolved.choice(other) < resolved.choiceCount(other),
                            "Resolved choice within bounds");
                    if (other != control
                            && !(control == 0 && other == 18)
                            && !(control == 1 && other == 2))
                        check(
                                resolved.choice(other) == selected.choice(other),
                                "Fixed choices preserved");
                }
                check(selected.isRandom(control), "Resolution never replaces saved Random choice");
            }
        }
        AquariumOptions all = defaults.withMask((1 << 20) - 1);
        boolean[] seen = new boolean[7];
        for (int sample = 0; sample < 1000; sample++) {
            AquariumOptions resolved = AquariumRandomizer.resolve(all, random);
            seen[resolved.mode == 1 ? 6 : resolved.look] = true;
            check(
                    resolved.count == AquariumOptions.POPULATION_COUNTS[resolved.population],
                    "Random presets match counts");
            check(
                    resolved.population > 0 && resolved.population < 6,
                    "Random population chooses a preset");
        }
        for (boolean version : seen) check(version, "All seven versions reachable");
        AquariumOptions pool = all;
        for (int version = 0; version < 7; version++) {
            if (version != 2 && version != 6) pool = pool.toggleVersion(version);
        }
        for (int sample = 0; sample < 100; sample++) {
            AquariumOptions resolved = AquariumRandomizer.resolve(pool, random);
            check(
                    resolved.mode == 1 || resolved.look == 2,
                    "Excluded shuffle versions never selected");
        }
        pool = pool.toggleVersion(2);
        check(pool.toggleVersion(6).versions == 64, "At least one shuffle version remains enabled");
        check(
                all.adjust(2, 1).population == 0 && !all.adjust(2, 1).isRandom(1),
                "Exact fish count takes priority over population");
        check(
                all.adjust(1, -1).count == 48 && !all.adjust(1, -1).isRandom(2),
                "Population takes priority over exact fish count");
        AquariumOptions fixed = AquariumRandomizer.resolve(defaults, random);
        for (int control = 0; control < 20; control++)
            check(
                    fixed.choice(control) == defaults.choice(control),
                    "Default settings never randomized");
    }

    public static void main(String[] args) {
        randomChoices();
        check(AquariumOptions.defaults().look == 0, "Realistic is the default appearance");
        check(AquariumOptions.defaults().time == 0, "Day is the default brightness");
        for (int time = -1; time <= 2; time++) {
            for (int mode = 0; mode < 2; mode++) {
                AquariumOptions brightness =
                        new AquariumOptions(mode, 16, 0, 0, 1, 1, 0, 0, true, true, 2, 0, 5, time);
                check(brightness.time == Math.max(0, Math.min(1, time)), "Day/night bounds");
                check(
                        brightness.nightShade() == (time <= 0 ? 0 : 0x99000000),
                        "Night dims both animated and original footage modes");
                check(
                        brightness.look == 5 && brightness.mode == mode,
                        "Brightness preserves the selected appearance and playback mode");
            }
        }
        check(AquariumOptions.LOOKS.length == 6, "All six requested appearances are available");
        for (int look = -1; look <= 6; look++) {
            AquariumOptions appearance =
                    new AquariumOptions(0, 32, 3, 2, 2, 2, 1, 2, false, true, 3, 127, look);
            check(appearance.look == Math.max(0, Math.min(5, look)), "Appearance bounds");
            check(
                    appearance.count == 32
                            && appearance.species == 3
                            && appearance.scene == 2
                            && appearance.creatures == 127
                            && appearance.rays,
                    "Changing appearance retains independent aquarium options");
        }
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
