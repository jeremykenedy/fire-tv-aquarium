package com.jeremykenedy.firetv.aquarium;

/** Validated, immutable settings shared by the renderer and the remote controls. */
final class AquariumOptions {
    static final String[] MODES = {"Animated aquarium", "Real 4K footage"};
    static final String[] SPECIES = {"Mixed", "Clownfish", "Yellow tang", "Blue tang", "Angelfish", "Neon tetra", "Betta"};
    static final String[] SCENES = {"Reef", "Fish tank", "Ocean", "Kelp forest", "Deep sea"};
    static final String[] POPULATIONS = {"Custom", "A few", "A handful", "A lot", "A ton", "Schools"};
    static final int[] POPULATION_COUNTS = {0, 6, 16, 32, 60, 48};
    static final String[] CREATURE_NAMES = {"Sharks", "Whales", "Octopuses", "Turtles", "Rays", "Dolphins", "Jellyfish"};
    static final int SHARK = 1, WHALE = 2, OCTOPUS = 4, TURTLE = 8, RAY = 16, DOLPHIN = 32, JELLYFISH = 64;
    static final String[] SPEEDS = {"Calm", "Gentle", "Lively"};
    static final String[] SIZES = {"Small", "Medium", "Large"};
    static final String[] LIGHTS = {"Daylight", "Warm", "Moonlight"};
    static final String[] CLOCKS = {"Hidden", "12-hour", "24-hour"};
    static final int MAX_FISH = 60;

    final int mode, count, species, scene, speed, size, light, clock, population, creatures;
    final boolean bubbles, rays;

    AquariumOptions(int mode, int count, int species, int scene, int speed, int size,
                    int light, int clock, boolean bubbles, boolean rays) {
        this(mode, count, species, scene, speed, size, light, clock, bubbles, rays, 0, 0);
    }

    AquariumOptions(int mode, int count, int species, int scene, int speed, int size,
                    int light, int clock, boolean bubbles, boolean rays, int population, int creatures) {
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

    boolean hasCreature(int type) { return (creatures & type) != 0; }
    boolean schools() { return population == 5; }

    float swimmingSpeed() { return speed == 0 ? 0.45f : speed == 1 ? 0.8f : 1.35f; }
    float fishScale() { return size == 0 ? 0.6f : size == 1 ? 0.9f : 1.25f; }
}
