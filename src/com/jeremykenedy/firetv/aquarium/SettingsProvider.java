package com.jeremykenedy.firetv.aquarium;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

/** Typed saved choices include the underlying values retained by Random settings. */
public final class SettingsProvider extends ContentProvider {
    static final String AUTHORITY = "com.jeremykenedy.firetv.aquarium.settings";
    private static final String[] KEYS = {
        "mode", "count", "species", "scene", "speed", "size", "light", "clock",
        "population", "creatures", "look", "time", "randomMask", "versions", "bubbles", "rays"
    };
    private static final String[] TITLES = {
        "Mode",
        "Fish count",
        "Species",
        "Background",
        "Swimming speed",
        "Fish size",
        "Lighting",
        "Clock",
        "Population",
        "Sea life mask",
        "Appearance",
        "Day or night",
        "Random choices mask",
        "Shuffle versions mask",
        "Bubbles",
        "Sunlight shimmer"
    };
    private static final int[] MAXIMUM = {
        1, 60, 6, 4, 2, 2, 2, 2, 5, 127, 5, 1, (1 << 20) - 1, 127
    };

    @Override
    public boolean onCreate() {
        return true;
    }

    private static String route(Uri uri) {
        String path = uri.getPath();
        if (!AUTHORITY.equals(uri.getAuthority())
                || (!"/schema".equals(path) && !"/settings".equals(path))) {
            throw new IllegalArgumentException("Unknown aquarium settings URI");
        }
        return path;
    }

    @Override
    public Cursor query(
            Uri uri, String[] projection, String selection, String[] arguments, String order) {
        boolean schema = "/schema".equals(route(uri));
        MatrixCursor cursor =
                new MatrixCursor(
                        schema
                                ? new String[] {
                                    "key",
                                    "title",
                                    "type",
                                    "default",
                                    "choices",
                                    "randomAllowed",
                                    "min",
                                    "max"
                                }
                                : new String[] {"key", "value", "type"});
        Object[] values =
                savedValues(
                        schema
                                ? AquariumOptions.defaults()
                                : new AquariumPreferences(getContext()).read());
        for (int i = 0; i < KEYS.length; i++) {
            String type = i < MAXIMUM.length ? "integer" : "boolean";
            if (schema) {
                cursor.addRow(
                        new Object[] {
                            KEYS[i],
                            TITLES[i],
                            type,
                            values[i],
                            i < MAXIMUM.length ? "" : "true|false",
                            false,
                            i == 13 ? 1 : 0,
                            i < MAXIMUM.length ? MAXIMUM[i] : 1
                        });
            } else {
                cursor.addRow(new Object[] {KEYS[i], values[i].toString(), type});
            }
        }
        return cursor;
    }

    private static Object[] savedValues(AquariumOptions options) {
        return new Object[] {
            options.mode,
            options.count,
            options.species,
            options.scene,
            options.speed,
            options.size,
            options.light,
            options.clock,
            options.population,
            options.creatures,
            options.look,
            options.time,
            options.randomMask,
            options.versions,
            options.bubbles,
            options.rays
        };
    }

    private static Object validated(String key, Object value) {
        int index = java.util.Arrays.asList(KEYS).indexOf(key);
        if (index < 0 || value == null)
            throw new IllegalArgumentException("Unknown or missing setting");
        String text = value.toString();
        if (index >= MAXIMUM.length) {
            if (!"true".equals(text) && !"false".equals(text))
                throw new IllegalArgumentException("A boolean setting needs true or false");
            return Boolean.valueOf(text);
        }
        int number;
        try {
            number = Integer.parseInt(text);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("An integer setting needs a whole number", failure);
        }
        if (number < (index == 13 ? 1 : 0) || number > MAXIMUM[index])
            throw new IllegalArgumentException("Aquarium setting is outside its supported range");
        return number;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] arguments) {
        if (!"/settings".equals(route(uri)) || values == null || values.size() == 0)
            throw new IllegalArgumentException("Settings are required");
        ContentValues typed = new ContentValues();
        if (values.containsKey("key")) {
            if (values.size() != 2 || !values.containsKey("value"))
                throw new IllegalArgumentException("A single update needs key and value");
            add(typed, values.getAsString("key"), values.get("value"));
        } else {
            if (values.size() > KEYS.length)
                throw new IllegalArgumentException("Too many settings");
            for (String key : values.keySet()) add(typed, key, values.get(key));
        }
        SharedPreferences.Editor editor = getContext().getSharedPreferences("aquarium", 0).edit();
        // Import raw choices together: UI adjustments deliberately couple count and population.
        for (String key : typed.keySet()) {
            Object value = typed.get(key);
            if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
            else editor.putInt(key, (Integer) value);
        }
        return editor.commit() ? typed.size() : 0;
    }

    private static void add(ContentValues target, String key, Object value) {
        Object parsed = validated(key, value);
        if (parsed instanceof Boolean) target.put(key, (Boolean) parsed);
        else target.put(key, (Integer) parsed);
    }

    @Override
    public String getType(Uri uri) {
        return "/schema".equals(route(uri))
                ? "vnd.android.cursor.dir/vnd.aquarium.setting-schema.v1"
                : "vnd.android.cursor.dir/vnd.aquarium.setting.v1";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Use a validated settings update");
    }

    @Override
    public int delete(Uri uri, String selection, String[] arguments) {
        throw new UnsupportedOperationException(
                "Settings cannot be deleted through this interface");
    }
}
