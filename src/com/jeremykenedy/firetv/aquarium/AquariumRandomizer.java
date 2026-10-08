package com.jeremykenedy.firetv.aquarium;

import java.util.Random;

/** Resolves saved random choices once, without changing preferences or animating the choices. */
final class AquariumRandomizer {
    private AquariumRandomizer() {}

    static AquariumOptions resolve(AquariumOptions saved) {
        return resolve(saved, new Random());
    }

    static AquariumOptions resolve(AquariumOptions saved, Random random) {
        AquariumOptions result = saved.withMask(0);
        for (int control = 1; control < AquariumOptions.CONTROL_COUNT; control++) {
            if (!saved.isRandom(control)) continue;
            int value =
                    control == 1
                            ? 1 + random.nextInt(5)
                            : random.nextInt(saved.choiceCount(control));
            result = result.withChoice(control, value, 0);
        }
        if (saved.isRandom(1)) {
            result = result.withChoice(2, AquariumOptions.POPULATION_COUNTS[result.population], 0);
        }
        if (saved.isRandom(0)) {
            // Each animated look and the original footage have an equal chance.
            int remaining = random.nextInt(Integer.bitCount(saved.versions));
            int versions = saved.versions;
            while (remaining-- > 0) versions &= versions - 1;
            int version = Integer.numberOfTrailingZeros(versions);
            result = result.withChoice(0, version == AquariumOptions.LOOKS.length ? 1 : 0, 0);
            if (result.mode == 0) result = result.withChoice(18, version, 0);
        }
        return result;
    }
}
