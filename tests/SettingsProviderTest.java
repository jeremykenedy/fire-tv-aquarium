package com.jeremykenedy.firetv.aquarium;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class SettingsProviderTest {
    private static final Uri SETTINGS =
            Uri.parse("content://" + SettingsProvider.AUTHORITY + "/settings");
    private static final Uri SCHEMA =
            Uri.parse("content://" + SettingsProvider.AUTHORITY + "/schema");
    private SettingsProvider provider;
    private SharedPreferences store;

    @Before
    public void createProvider() {
        store = RuntimeEnvironment.getApplication().getSharedPreferences("aquarium", 0);
        store.edit().clear().commit();
        provider = Robolectric.buildContentProvider(SettingsProvider.class).create().get();
    }

    private Map<String, Object> exported() {
        Map<String, Object> values = new LinkedHashMap<>();
        try (Cursor cursor = provider.query(SETTINGS, null, null, null, null)) {
            while (cursor.moveToNext()) {
                String text = cursor.getString(1);
                Object value =
                        "boolean".equals(cursor.getString(2))
                                ? (Object) Boolean.valueOf(text)
                                : Integer.valueOf(text);
                values.put(cursor.getString(0), value);
            }
        }
        return values;
    }

    @Test
    public void schemaCoversEverySavedFieldAndTypedDefaults() {
        Map<String, Object> defaults = exported();
        assertEquals(16, defaults.size());
        try (Cursor cursor = provider.query(SCHEMA, null, null, null, null)) {
            assertEquals(16, cursor.getCount());
            while (cursor.moveToNext()) {
                String key = cursor.getString(0);
                assertFalse(cursor.getString(1).isEmpty());
                assertEquals(defaults.get(key).toString(), cursor.getString(3));
                assertEquals("false", cursor.getString(5));
                assertTrue(cursor.getInt(7) >= cursor.getInt(6));
            }
        }
        assertTrue(provider.getType(SCHEMA).contains("schema.v1"));
        assertTrue(provider.getType(SETTINGS).endsWith("setting.v1"));
    }

    @Test
    public void completeImportPreservesRawRandomChoicesAndTheirStorageTypes() {
        ContentValues source = new ContentValues();
        source.put("mode", 1);
        source.put("count", 37);
        source.put("species", 5);
        source.put("scene", 3);
        source.put("speed", 2);
        source.put("size", 2);
        source.put("light", 1);
        source.put("clock", 2);
        source.put("population", 0);
        source.put("creatures", 35);
        source.put("look", 4);
        source.put("time", 1);
        source.put("randomMask", (1 << 6) | (1 << 7));
        source.put("versions", 42);
        source.put("bubbles", false);
        source.put("rays", true);
        assertEquals(16, provider.update(SETTINGS, source, null, null));
        Map<String, Object> first = exported();
        assertEquals(store.getAll(), first);
        assertEquals(2, first.get("size"));
        assertEquals(1, first.get("light"));
        AquariumOptions options =
                new AquariumPreferences(RuntimeEnvironment.getApplication()).read();
        assertTrue(options.isRandom(6));
        assertTrue(options.isRandom(7));
        assertEquals(37, options.count);
        assertEquals(0, options.population);
        store.edit().clear().commit();
        ContentValues restored = new ContentValues();
        for (Map.Entry<String, Object> entry : first.entrySet()) {
            if (entry.getValue() instanceof Boolean)
                restored.put(entry.getKey(), (Boolean) entry.getValue());
            else restored.put(entry.getKey(), (Integer) entry.getValue());
        }
        assertEquals(16, provider.update(SETTINGS, restored, null, null));
        assertEquals(first, exported());
        assertEquals(first, store.getAll());
    }

    @Test
    public void shellStringUpdatesRetainIntegerAndBooleanStorage() {
        ContentValues values = new ContentValues();
        values.put("key", "count");
        values.put("value", "23");
        assertEquals(1, provider.update(SETTINGS, values, null, null));
        assertEquals(23, store.getInt("count", -1));
        values.put("key", "rays");
        values.put("value", "false");
        assertEquals(1, provider.update(SETTINGS, values, null, null));
        assertFalse(store.getBoolean("rays", true));
    }

    @Test
    public void invalidBatchCannotPartiallyChangeAnySettings() {
        Map<String, Object> before = exported();
        for (Object invalid : new Object[] {-1, 61, "1.2", "2147483648", true}) {
            ContentValues values = new ContentValues();
            values.put("size", 2);
            values.put("count", invalid.toString());
            assertThrows(
                    IllegalArgumentException.class,
                    () -> provider.update(SETTINGS, values, null, null));
            assertEquals(before, exported());
        }
        for (String key : new String[] {"unknown", "versions", "randomMask", "mode", "rays"}) {
            ContentValues values = new ContentValues();
            values.put(key, key.equals("randomMask") ? "1048576" : key.equals("mode") ? "2" : "0");
            assertThrows(
                    IllegalArgumentException.class,
                    () -> provider.update(SETTINGS, values, null, null));
        }
        ContentValues absent = new ContentValues();
        absent.putNull("count");
        assertThrows(
                IllegalArgumentException.class,
                () -> provider.update(SETTINGS, absent, null, null));
        assertEquals(before, exported());
    }

    @Test
    public void routesAndMalformedUpdatesAreRejected() {
        for (Uri uri :
                new Uri[] {
                    Uri.parse("content://foreign/settings"),
                    Uri.parse("content://" + SettingsProvider.AUTHORITY + "/other")
                }) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> provider.query(uri, null, null, null, null));
            assertThrows(IllegalArgumentException.class, () -> provider.getType(uri));
        }
        assertThrows(
                IllegalArgumentException.class,
                () -> provider.update(SCHEMA, new ContentValues(), null, null));
        assertThrows(
                IllegalArgumentException.class, () -> provider.update(SETTINGS, null, null, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> provider.update(SETTINGS, new ContentValues(), null, null));
        ContentValues single = new ContentValues();
        single.put("key", "count");
        assertThrows(
                IllegalArgumentException.class,
                () -> provider.update(SETTINGS, single, null, null));
        single.put("extra", 2);
        assertThrows(
                IllegalArgumentException.class,
                () -> provider.update(SETTINGS, single, null, null));
        single.put("value", "1");
        assertThrows(
                IllegalArgumentException.class,
                () -> provider.update(SETTINGS, single, null, null));
        ContentValues excessive = new ContentValues();
        for (int i = 0; i < 17; i++) excessive.put("setting" + i, i);
        assertThrows(
                IllegalArgumentException.class,
                () -> provider.update(SETTINGS, excessive, null, null));
        assertThrows(UnsupportedOperationException.class, () -> provider.insert(SETTINGS, single));
        assertThrows(
                UnsupportedOperationException.class, () -> provider.delete(SETTINGS, null, null));
    }

    @Test
    public void failedPersistenceIsReportedWithoutClaimingAnImport() {
        Context context =
                new ContextWrapper(RuntimeEnvironment.getApplication()) {
                    @Override
                    public SharedPreferences getSharedPreferences(String name, int mode) {
                        return (SharedPreferences)
                                Proxy.newProxyInstance(
                                        getClassLoader(),
                                        new Class<?>[] {SharedPreferences.class},
                                        (proxy, method, arguments) -> {
                                            if (!method.getName().equals("edit"))
                                                return method.invoke(store, arguments);
                                            SharedPreferences.Editor editor = store.edit();
                                            return Proxy.newProxyInstance(
                                                    getClassLoader(),
                                                    new Class<?>[] {SharedPreferences.Editor.class},
                                                    (unused, edit, args) ->
                                                            edit.getName().equals("commit")
                                                                    ? false
                                                                    : edit.invoke(editor, args));
                                        });
                    }
                };
        SettingsProvider failed = new SettingsProvider();
        failed.attachInfo(context, new ProviderInfo());
        ContentValues values = new ContentValues();
        values.put("count", 7);
        assertEquals(0, failed.update(SETTINGS, values, null, null));
        assertTrue(store.getAll().isEmpty());
    }
}
