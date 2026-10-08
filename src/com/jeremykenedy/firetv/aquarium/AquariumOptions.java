package com.jeremykenedy.firetv.aquarium;

/** Validated, immutable settings shared by the renderer and the remote controls. */
final class AquariumOptions {
    static final String[] MODES = {"Animated aquarium", "Original 4K footage"};
    static final String[] TIMES = {"Day", "Night"};
    static final String[] LOOKS = {
        "Realistic",
        "Classic Windows aquarium",
        "Animated 3D",
        "Cartoon",
        "Finding Nemo inspired",
        "Little Mermaid inspired"
    };
    static final String[] SPECIES = {
        "Mixed", "Clownfish", "Yellow tang", "Blue tang", "Angelfish", "Neon tetra", "Betta"
    };
    static final String[] SCENES = {"Reef", "Fish tank", "Ocean", "Kelp forest", "Deep sea"};
    static final String[] POPULATIONS = {
        "Custom", "A few", "A handful", "A lot", "A ton", "Schools"
    };
    static final int[] POPULATION_COUNTS = {0, 6, 16, 32, 60, 48};
    static final String[] CREATURE_NAMES = {
        "Sharks", "Whales", "Octopuses", "Turtles", "Rays", "Dolphins", "Jellyfish"
    };
    static final int SHARK = 1,
            WHALE = 2,
            OCTOPUS = 4,
            TURTLE = 8,
            RAY = 16,
            DOLPHIN = 32,
            JELLYFISH = 64;
    static final String[] SPEEDS = {"Calm", "Gentle", "Lively"};
    static final String[] SIZES = {"Small", "Medium", "Large"};
    static final String[] LIGHTS = {"Daylight", "Warm", "Moonlight"};
    static final String[] CLOCKS = {"Hidden", "12-hour", "24-hour"};
    static final int MAX_FISH = 60;

    final int mode,
            count,
            species,
            scene,
            speed,
            size,
            light,
            clock,
            population,
            creatures,
            look,
            time;
    final boolean bubbles, rays;
    final int randomMask, versions;
    static final int CONTROL_COUNT = 20;

    AquariumOptions(
            int mode,
            int count,
            int species,
            int scene,
            int speed,
            int size,
            int light,
            int clock,
            boolean bubbles,
            boolean rays) {
        this(mode, count, species, scene, speed, size, light, clock, bubbles, rays, 0, 0);
    }

    AquariumOptions(
            int mode,
            int count,
            int species,
            int scene,
            int speed,
            int size,
            int light,
            int clock,
            boolean bubbles,
            boolean rays,
            int population,
            int creatures) {
        this(
                mode,
                count,
                species,
                scene,
                speed,
                size,
                light,
                clock,
                bubbles,
                rays,
                population,
                creatures,
                0);
    }

    AquariumOptions(
            int mode,
            int count,
            int species,
            int scene,
            int speed,
            int size,
            int light,
            int clock,
            boolean bubbles,
            boolean rays,
            int population,
            int creatures,
            int look) {
        this(
                mode,
                count,
                species,
                scene,
                speed,
                size,
                light,
                clock,
                bubbles,
                rays,
                population,
                creatures,
                look,
                0);
    }

    AquariumOptions(
            int mode,
            int count,
            int species,
            int scene,
            int speed,
            int size,
            int light,
            int clock,
            boolean bubbles,
            boolean rays,
            int population,
            int creatures,
            int look,
            int time) {
        this(
                mode,
                count,
                species,
                scene,
                speed,
                size,
                light,
                clock,
                bubbles,
                rays,
                population,
                creatures,
                look,
                time,
                0);
    }

    AquariumOptions(
            int mode,
            int count,
            int species,
            int scene,
            int speed,
            int size,
            int light,
            int clock,
            boolean bubbles,
            boolean rays,
            int population,
            int creatures,
            int look,
            int time,
            int randomMask) {
        this(
                mode,
                count,
                species,
                scene,
                speed,
                size,
                light,
                clock,
                bubbles,
                rays,
                population,
                creatures,
                look,
                time,
                randomMask,
                127);
    }

    AquariumOptions(
            int mode,
            int count,
            int species,
            int scene,
            int speed,
            int size,
            int light,
            int clock,
            boolean bubbles,
            boolean rays,
            int population,
            int creatures,
            int look,
            int time,
            int randomMask,
            int versions) {
        this.versions = (versions & 127) == 0 ? 127 : versions & 127;
        this.randomMask = randomMask & ((1 << CONTROL_COUNT) - 1);
        this.population = bounded(population, 0, POPULATIONS.length - 1);
        this.creatures = creatures & 127;
        this.mode = bounded(mode, 0, MODES.length - 1);
        this.count = bounded(count, 0, MAX_FISH);
        this.species = bounded(species, 0, SPECIES.length - 1);
        this.scene = bounded(scene, 0, SCENES.length - 1);
        this.speed = bounded(speed, 0, SPEEDS.length - 1);
        this.size = bounded(size, 0, SIZES.length - 1);
        this.light = bounded(light, 0, LIGHTS.length - 1);
        this.clock = bounded(clock, 0, CLOCKS.length - 1);
        this.bubbles = bubbles;
        this.rays = rays;
        this.look = bounded(look, 0, LOOKS.length - 1);
        this.time = bounded(time, 0, TIMES.length - 1);
    }

    boolean isRandom(int control) {
        return (randomMask & (1 << control)) != 0;
    }

    int choiceCount(int control) {
        switch (control) {
            case 0:
                return MODES.length;
            case 1:
                return POPULATIONS.length;
            case 2:
                return MAX_FISH + 1;
            case 3:
                return SPECIES.length;
            case 4:
                return SCENES.length;
            case 5:
                return SPEEDS.length;
            case 6:
                return SIZES.length;
            case 7:
                return LIGHTS.length;
            case 17:
                return CLOCKS.length;
            case 18:
                return LOOKS.length;
            case 19:
                return TIMES.length;
            default:
                if (control < 8 || control > 16)
                    throw new IllegalArgumentException("Unknown control");
                return 2;
        }
    }

    int choice(int control) {
        switch (control) {
            case 0:
                return mode;
            case 1:
                return population;
            case 2:
                return count;
            case 3:
                return species;
            case 4:
                return scene;
            case 5:
                return speed;
            case 6:
                return size;
            case 7:
                return light;
            case 8:
                return bubbles ? 1 : 0;
            case 9:
                return rays ? 1 : 0;
            case 17:
                return clock;
            case 18:
                return look;
            case 19:
                return time;
            default:
                if (control < 10 || control > 16)
                    throw new IllegalArgumentException("Unknown control");
                return hasCreature(1 << (control - 10)) ? 1 : 0;
        }
    }

    AquariumOptions adjust(int control, int delta) {
        int length = choiceCount(control);
        int next = cycle(isRandom(control) ? length : choice(control), delta, length + 1);
        int mask = next == length ? randomMask | (1 << control) : randomMask & ~(1 << control);
        AquariumOptions values = next == length ? this : withChoice(control, next, mask);
        if (control == 2) {
            mask &= ~(1 << 1);
            values = values.withChoice(1, 0, mask);
        } else if (control == 1 && (next == length || next != 0)) {
            mask &= ~(1 << 2);
            if (next != length) values = values.withChoice(2, POPULATION_COUNTS[next], mask);
        }
        return values.withMask(mask);
    }

    AquariumOptions toggleVersion(int version) {
        int allowed = versions ^ (1 << version);
        if (allowed == 0) return this;
        return new AquariumOptions(
                mode,
                count,
                species,
                scene,
                speed,
                size,
                light,
                clock,
                bubbles,
                rays,
                population,
                creatures,
                look,
                time,
                randomMask,
                allowed);
    }

    AquariumOptions withMask(int mask) {
        return new AquariumOptions(
                mode,
                count,
                species,
                scene,
                speed,
                size,
                light,
                clock,
                bubbles,
                rays,
                population,
                creatures,
                look,
                time,
                mask,
                versions);
    }

    AquariumOptions withChoice(int control, int value, int mask) {
        int visitors = creatures;
        if (control >= 10 && control <= 16) {
            int bit = 1 << (control - 10);
            visitors = value == 1 ? visitors | bit : visitors & ~bit;
        }
        return new AquariumOptions(
                control == 0 ? value : mode,
                control == 2 ? value : count,
                control == 3 ? value : species,
                control == 4 ? value : scene,
                control == 5 ? value : speed,
                control == 6 ? value : size,
                control == 7 ? value : light,
                control == 17 ? value : clock,
                control == 8 ? value == 1 : bubbles,
                control == 9 ? value == 1 : rays,
                control == 1 ? value : population,
                visitors,
                control == 18 ? value : look,
                control == 19 ? value : time,
                mask,
                versions);
    }

    static AquariumOptions defaults() {
        return new AquariumOptions(0, 16, 0, 0, 1, 1, 0, 0, true, true, 2, 0);
    }

    static int bounded(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    static int cycle(int value, int delta, int count) {
        return ((value + delta) % count + count) % count;
    }

    boolean hasCreature(int type) {
        return (creatures & type) != 0;
    }

    boolean schools() {
        return population == 5;
    }

    float swimmingSpeed() {
        return speed == 0 ? 0.45f : speed == 1 ? 0.8f : 1.35f;
    }

    float fishScale() {
        return size == 0 ? 0.6f : size == 1 ? 0.9f : 1.25f;
    }

    int nightShade() {
        return time == 1 ? 0x99000000 : 0x00000000;
    }
}
