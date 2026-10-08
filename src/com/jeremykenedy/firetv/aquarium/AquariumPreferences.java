package com.jeremykenedy.firetv.aquarium;

import android.content.Context;
import android.content.SharedPreferences;

final class AquariumPreferences {
    private final SharedPreferences store;

    AquariumPreferences(Context context) {
        store = context.getSharedPreferences("aquarium", Context.MODE_PRIVATE);
    }

    AquariumOptions read() {
        AquariumOptions d = AquariumOptions.defaults();
        return new AquariumOptions(integer("mode", d.mode), integer("count", d.count),
            integer("species", d.species), integer("scene", d.scene), integer("speed", d.speed),
            integer("size", d.size), integer("light", d.light), integer("clock", d.clock),
            bool("bubbles", d.bubbles), bool("rays", d.rays),
            integer("population", store.contains("count") ? 0 : d.population), integer("creatures", d.creatures));
    }

    private int integer(String name, int fallback) {
        try { return store.getInt(name, fallback); }
        catch (ClassCastException exception) { return fallback; }
    }

    private boolean bool(String name, boolean fallback) {
        try { return store.getBoolean(name, fallback); }
        catch (ClassCastException exception) { return fallback; }
    }

    void save(AquariumOptions options) {
        store.edit().putInt("mode", options.mode).putInt("count", options.count)
            .putInt("species", options.species).putInt("scene", options.scene)
            .putInt("speed", options.speed).putInt("size", options.size)
            .putInt("light", options.light).putInt("clock", options.clock)
            .putInt("population", options.population).putInt("creatures", options.creatures)
            .putBoolean("bubbles", options.bubbles).putBoolean("rays", options.rays).apply();
    }
}
