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

    private static void verifySwimmingStep(
            FishMotion.Pose fish, float previousX, float previousYaw) {
        check(Float.isFinite(fish.x + fish.y + fish.z), "Long-running swimming stays finite");
        check(
                Math.abs(fish.x) <= 19
                        && fish.y >= -4.4f
                        && fish.y <= 6.1f
                        && Math.abs(fish.z) <= 3.2f,
                "Every swimmer stays inside the tank");
        check(Math.abs(fish.x - previousX) < .12f, "Fish never wrap or teleport across the screen");
        check(Math.abs(fish.yaw - previousYaw) <= .056f, "Turns stay gradual at each frame");
    }

    private static int observedPause(FishMotion.Pose fish, int frame) {
        return frame > 100 && fish.vx * fish.vx + fish.vy * fish.vy + fish.vz * fish.vz < .015f
                ? 1
                : 0;
    }

    private static void wanderingMotion() {
        FishMotion motion = new FishMotion(new java.util.Random(84));
        motion.update(0, .8f, false, 60);
        float[] previousX = new float[60];
        float[] previousY = new float[60];
        float[] previousYaw = new float[60];
        boolean[] reversed = new boolean[60];
        boolean[] rose = new boolean[60];
        boolean[] sank = new boolean[60];
        float[] initialDirection = new float[60];
        int pauses = 0;
        for (int i = 0; i < 60; i++) {
            FishMotion.Pose fish = motion.pose(i);
            previousX[i] = fish.x;
            previousY[i] = fish.y;
            previousYaw[i] = fish.yaw;
            initialDirection[i] = (float) Math.cos(fish.yaw);
        }
        for (int frame = 1; frame <= 6000; frame++) {
            motion.update(frame * .05f, .8f, false, 60);
            for (int i = 0; i < 60; i++) {
                FishMotion.Pose fish = motion.pose(i);
                verifySwimmingStep(fish, previousX[i], previousYaw[i]);
                if (Math.cos(fish.yaw) * (initialDirection[i] < 0 ? -1 : 1) < -.35f)
                    reversed[i] = true;
                if (fish.y - previousY[i] > .001f) rose[i] = true;
                if (previousY[i] - fish.y > .001f) sank[i] = true;
                pauses += observedPause(fish, frame);
                previousX[i] = fish.x;
                previousY[i] = fish.y;
                previousYaw[i] = fish.yaw;
            }
        }
        for (int i = 0; i < 60; i++) {
            check(
                    reversed[i],
                    "Every fish chooses new horizontal directions rather than following a fixed"
                            + " rail");
            check(rose[i] && sank[i], "Fish wander both up and down independently");
        }
        check(pauses > 50, "Independent fish occasionally slow to a near pause");
    }

    private static void swimmingSpeedAndSchools() {
        float slow = travelled(.45f);
        float fast = travelled(1.35f);
        check(fast > slow * 2, "Saved swimming speed changes actual distance travelled");
        FishMotion school = new FishMotion(new java.util.Random(19));
        school.update(0, .8f, true, 32);
        float initialSpacing = school.pose(0).x - school.pose(1).x;
        for (int frame = 1; frame < 2000; frame++) school.update(frame * .05f, .8f, true, 32);
        check(
                Math.abs(school.pose(0).x - school.pose(1).x - initialSpacing) > .5f,
                "School members move independently instead of retaining rigid grid offsets");
        float pausedX = school.pose(0).x;
        school.update(5000, .8f, true, 32);
        check(
                Math.abs(school.pose(0).x - pausedX) < .6f,
                "Resuming after a long pause does not fast-forward the fish");
        school.update(4999, .8f, true, 32);
        check(Float.isFinite(school.pose(0).x), "An earlier clock sample leaves a valid state");
    }

    private static void predatorContactAndReplenishment() {
        for (boolean flat : new boolean[] {false, true}) {
            FishMotion feeding = new FishMotion(new java.util.Random(flat ? 9 : 8));
            feeding.setFlatAppearance(flat);
            float feedingTime = seekHunt(feeding, 60);
            FishMotion.Pose shark = feeding.pose(60);
            FishMotion.Pose prey = feeding.pose(0);
            shark.x = -(flat ? 6.0016f : 3.9f);
            shark.y = flat ? .52907f : 0;
            shark.z = shark.yaw = shark.pitch = 0;
            prey.x = prey.y = prey.z = 0;
            feeding.update(feedingTime + .02f, .8f, false, 1, AquariumOptions.SHARK);
            check(!prey.visible, "A shark consumes prey only at its visible mouth");
            check(
                    shark.captures == 1
                            && shark.lastPrey == 0
                            && Math.abs(shark.lastCaptureSeconds - feedingTime) < .001f,
                    "A visible mouth capture increments only that predator's feeding count");
            verifyPopulationReturns(feeding, prey, feedingTime);
            dolphinContact(flat);
        }
    }

    private static void verifyPopulationReturns(
            FishMotion feeding, FishMotion.Pose prey, float feedingTime) {
        boolean returned = false;
        for (int frame = 2; frame <= 3000; frame++) {
            feeding.update(feedingTime + frame * .02f, .8f, true, 16, 0);
            if (!returned && prey.visible) {
                check(
                        Math.abs(prey.x) > 26,
                        "Consumed fish return outside the visible frame without popping into"
                                + " the tank");
                returned = true;
            }
        }
        check(returned, "The configured population returns gradually after feeding");
        for (int member = 1; member < 16; member++)
            check(
                    Math.abs(feeding.pose(member).x) <= 19,
                    "School members stay inside the tank while their consumed leader returns"
                            + " offscreen");
    }

    private static void dolphinContact(boolean flat) {
        FishMotion dolphin = new FishMotion(new java.util.Random(41));
        dolphin.setFlatAppearance(flat);
        float dolphinTime = seekHunt(dolphin, 95);
        dolphin.pose(95).x = -(flat ? 4.2504f : 3.6f);
        dolphin.pose(95).y = flat ? -.6272f : 0;
        dolphin.pose(95).z = dolphin.pose(95).yaw = dolphin.pose(95).pitch = 0;
        dolphin.pose(0).x = dolphin.pose(0).y = dolphin.pose(0).z = 0;
        dolphin.update(dolphinTime + .02f, .8f, false, 1, AquariumOptions.DOLPHIN);
        check(
                !dolphin.pose(0).visible,
                "Dolphin feeding uses its own mouth geometry in both rendering styles");
    }

    private static void cinematicWhaleContact() {
        FishMotion cinematicWhale = new FishMotion(new java.util.Random(7));
        cinematicWhale.setFlatAppearance(true);
        cinematicWhale.setMarineArtwork(1, 16f / 9);
        float cinemaTime = seekHunt(cinematicWhale, 67);
        FishMotion.Pose cinemaHunter = cinematicWhale.pose(67);
        cinemaHunter.x = cinemaHunter.y = cinemaHunter.z = cinemaHunter.pitch = 0;
        cinemaHunter.yaw = (float) Math.PI;
        for (int i = 0; i < 6; i++) {
            cinematicWhale.pose(i).x = -9.823f;
            cinematicWhale.pose(i).y = 3.648f;
            cinematicWhale.pose(i).z = 0;
        }
        cinematicWhale.update(cinemaTime + .02f, .8f, false, 6, AquariumOptions.WHALE);
        check(
                !cinematicWhale.pose(0).visible,
                "A left-facing cinematic whale feeds at its raised atlas snout, not its belly");
    }

    private static void measuredModelContactAndAim() {
        FishMotion measured = new FishMotion(new java.util.Random(8));
        measured.setMeshMouth(0, 1, .2f, .3f, 4);
        float measuredTime = seekHunt(measured, 60);
        FishMotion.Pose measuredShark = measured.pose(60);
        measuredShark.x = -1.2f;
        measuredShark.y = -2.69282f;
        measuredShark.z = 3.0641f;
        measuredShark.yaw = (float) Math.PI / 2;
        measuredShark.pitch = (float) Math.PI / 6;
        measured.pose(0).x = measured.pose(0).y = measured.pose(0).z = 0;
        measured.update(measuredTime + .02f, .8f, false, 1, AquariumOptions.SHARK);
        check(
                !measured.pose(0).visible,
                "Measured model mouth applies scale, pitch and yaw to all three local coordinates");
        FishMotion aiming = new FishMotion(new java.util.Random(8));
        aiming.setMeshMouth(0, 1, .2f, .3f, 4);
        float aimingTime = seekHunt(aiming, 60);
        FishMotion.Pose aimingShark = aiming.pose(60);
        aimingShark.x = -10;
        aimingShark.y = aimingShark.z = aimingShark.yaw = aimingShark.pitch = 0;
        aiming.pose(0).x = aiming.pose(0).y = aiming.pose(0).z = 0;
        aiming.update(aimingTime + .2f, .8f, false, 1, AquariumOptions.SHARK);
        check(
                aiming.pose(0).visible && aimingShark.pitch < 0 && aimingShark.yaw > 0,
                "Pursuit lowers and turns the model center to align its raised, offset mouth");
    }

    private static void depthAndNonPredators() {
        FishMotion depth = new FishMotion(new java.util.Random(8));
        float depthTime = seekHunt(depth, 60);
        depth.pose(60).x = -3.9f;
        depth.pose(60).y = depth.pose(60).z = depth.pose(60).yaw = depth.pose(60).pitch = 0;
        depth.pose(61).x = -19;
        depth.pose(61).z = -3;
        depth.pose(0).x = depth.pose(0).y = 0;
        depth.pose(0).z = 2;
        depth.update(depthTime + .02f, .8f, false, 1, AquariumOptions.SHARK);
        check(depth.pose(0).visible, "A shark cannot eat a fish at a different tank depth");
        depth.pose(0).z = 0;
        depth.update(depthTime + .04f, .8f, false, 1, AquariumOptions.TURTLE);
        check(depth.pose(0).visible, "Non-predatory creatures never consume fish");
    }

    private static void whaleSchoolContact() {
        FishMotion whale = new FishMotion(new java.util.Random(7));
        float whaleTime = seekHunt(whale, 67);
        FishMotion.Pose hunter = whale.pose(67);
        hunter.x = hunter.y = hunter.z = hunter.yaw = hunter.pitch = 0;
        for (int i = 0; i < 6; i++) {
            whale.pose(i).x = 8.2f;
            whale.pose(i).y = whale.pose(i).z = 0;
        }
        whale.update(whaleTime + .02f, .8f, false, 1, AquariumOptions.WHALE);
        check(whale.pose(0).visible, "A baleen whale does not chase a single isolated fish");
        whale.update(whaleTime + .04f, .8f, false, 6, AquariumOptions.WHALE);
        check(
                !whale.pose(0).visible,
                "A whale can lunge-feed through an actual tight school without the Schools preset");
        whale.update(whaleTime + .08f, .8f, true, 6, AquariumOptions.WHALE);
        check(!whale.pose(0).visible, "A whale ignores an already consumed school");
        check(
                whale.pose(67).captures == 6,
                "A whale records the fish in its single schooling feed");
    }

    private static void mixedPredatorStability() {
        for (boolean flat : new boolean[] {false, true}) {
            FishMotion roaming = new FishMotion(new java.util.Random(31));
            roaming.setFlatAppearance(flat);
            roaming.update(0, .8f, true, 24, 127);
            for (int frame = 1; frame <= 1600; frame++)
                roaming.update(frame * .05f, .8f, true, 24, 127);
            check(
                    Float.isFinite(roaming.pose(67).x),
                    "Mixed predators and schools remain stable together");
            FishMotion scattered = new FishMotion(new java.util.Random(14));
            scattered.setFlatAppearance(flat);
            float scatteredTime = seekHunt(scattered, 67);
            for (int i = 0; i < 6; i++) {
                scattered.pose(i).x = i < 3 ? -14 : 14;
                scattered.pose(i).y = i % 2 == 0 ? -4 : 6;
                scattered.pose(i).z = 0;
            }
            scattered.update(scatteredTime + .02f, .8f, true, 6, AquariumOptions.WHALE);
            check(
                    scattered.pose(0).visible,
                    "A baleen whale does not lunge into a scattered group");
            scattered.update(scatteredTime + .04f, .8f, true, 5, AquariumOptions.WHALE);
            check(scattered.pose(0).visible, "A baleen whale needs a school large enough to feed");
        }
    }

    private static float seekHunt(FishMotion motion, int index) {
        motion.update(0, .8f, false, 0);
        float seconds = 0;
        while (!motion.pose(index).hunting && seconds < 40) {
            seconds += .04f;
            motion.update(seconds, .8f, false, 0);
        }
        check(motion.pose(index).hunting, "Each predator eventually enters its own feeding window");
        return seconds;
    }

    private static void distributedSwimming() {
        for (int seed = 0; seed < 6; seed++) {
            FishMotion motion = new FishMotion(new java.util.Random(seed));
            motion.update(0, .8f, false, 16);
            for (int frame = 1; frame <= 2400; frame++) {
                motion.update(frame * .05f, .8f, false, 16);
                if (frame % 200 != 0) continue;
                float minX = 100;
                float maxX = -100;
                float minY = 100;
                float maxY = -100;
                for (int i = 0; i < 16; i++) {
                    FishMotion.Pose fish = motion.pose(i);
                    minX = Math.min(minX, fish.x);
                    maxX = Math.max(maxX, fish.x);
                    minY = Math.min(minY, fish.y);
                    maxY = Math.max(maxY, fish.y);
                }
                check(
                        maxX - minX > 19,
                        "Ordinary fish stay distributed across the usable tank width");
                check(maxY - minY > 5, "Ordinary fish retain independent vertical distribution");
            }
        }
    }

    private static void independentPredatorWindows() {
        FishMotion predators = new FishMotion(new java.util.Random(32));
        predators.update(0, .8f, false, 0);
        boolean[] rested = new boolean[5];
        boolean[] hunted = new boolean[5];
        int[] slots = {60, 61, 67, 95, 96};
        int independent = 0;
        for (int frame = 1; frame <= 2000; frame++) {
            predators.update(frame * .04f, .8f, false, 0);
            for (int i = 0; i < slots.length; i++) {
                if (predators.pose(slots[i]).hunting) hunted[i] = true;
                else rested[i] = true;
            }
            if (predators.pose(60).hunting != predators.pose(61).hunting) independent++;
        }
        for (int i = 0; i < slots.length; i++)
            check(rested[i] && hunted[i], "Predators alternate feeding and ordinary cruising");
        check(independent > 100, "Predators have independent feeding phases");
        for (int seed = 0; seed < 2; seed++) {
            FishMotion feeding = new FishMotion(new java.util.Random(seed));
            feeding.setFlatAppearance(true);
            int creatures = AquariumOptions.SHARK | AquariumOptions.WHALE | AquariumOptions.DOLPHIN;
            feeding.update(0, 1.35f, false, 60, creatures);
            for (int frame = 1; frame <= 2400; frame++)
                feeding.update(frame * .05f, 1.35f, false, 60, creatures);
            check(
                    feeding.pose(67).captures > 0,
                    "Whales feed on natural shoals without the Schools preset");
            check(
                    feeding.pose(60).captures + feeding.pose(61).captures > 0,
                    "Sharks can pursue and capture independently wandering fish");
            check(
                    feeding.pose(95).captures + feeding.pose(96).captures > 0,
                    "Dolphins can pursue and capture independently wandering fish");
        }
    }

    private static float travelled(float speed) {
        FishMotion motion = new FishMotion(new java.util.Random(23));
        motion.update(0, speed, false, 24);
        float distance = 0;
        for (int frame = 1; frame <= 1200; frame++) {
            float x = motion.pose(0).x;
            float y = motion.pose(0).y;
            float z = motion.pose(0).z;
            motion.update(frame * .05f, speed, false, 24);
            FishMotion.Pose fish = motion.pose(0);
            distance +=
                    (float)
                            Math.sqrt(
                                    (fish.x - x) * (fish.x - x)
                                            + (fish.y - y) * (fish.y - y)
                                            + (fish.z - z) * (fish.z - z));
        }
        return distance;
    }

    public static void main(String[] args) {
        distributedSwimming();
        independentPredatorWindows();
        wanderingMotion();
        swimmingSpeedAndSchools();
        predatorContactAndReplenishment();
        cinematicWhaleContact();
        measuredModelContactAndAim();
        depthAndNonPredators();
        whaleSchoolContact();
        mixedPredatorStability();
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
        int[] populations = new int[6];
        for (int i = 0; i < AquariumOptions.MAX_FISH; i++) {
            populations[FishMotion.species(i, 0)]++;
            for (int choice = 1; choice <= 6; choice++)
                check(
                        FishMotion.species(i, choice) == choice - 1,
                        "Species choice must apply to every fish");
            check(FishMotion.scale(i) >= .65f && FishMotion.scale(i) <= 1.25f, "Fish scale bounds");
        }
        for (int population : populations)
            check(population == 10, "Mixed mode must distribute all species evenly");
        System.out.println("Aquarium tests passed: " + checks + " checks");
    }
}
