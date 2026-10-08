package com.jeremykenedy.firetv.aquarium;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.service.dreams.DreamService;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class LifecycleCoverageTest {
    private boolean key(Button button, int action, int code) {
        Object listeners = ReflectionHelpers.callInstanceMethod(button, "getListenerInfo");
        View.OnKeyListener listener = ReflectionHelpers.getField(listeners, "mOnKeyListener");
        return listener.onKey(button, code, new KeyEvent(action, code));
    }

    @Test
    public void preferencesRecoverWrongTypesAndLegacyCounts() {
        Context context = RuntimeEnvironment.getApplication();
        SharedPreferences store = context.getSharedPreferences("aquarium", Context.MODE_PRIVATE);
        AquariumPreferences preferences = new AquariumPreferences(context);
        assertEquals(AquariumOptions.defaults().population, preferences.read().population);
        store.edit().putInt("count", 23).apply();
        assertEquals(0, preferences.read().population);
        store.edit().putString("count", "corrupt").putString("bubbles", "corrupt").apply();
        assertEquals(16, preferences.read().count);
        assertTrue(preferences.read().bubbles);
    }

    @Test
    public void remoteCallbacksAdjustSaveAndNavigateAllControls() {
        AquariumActivity activity =
                Robolectric.buildActivity(AquariumActivity.class).create().get();
        Button[] controls = ReflectionHelpers.getField(activity, "controls");
        for (int index = 1; index < 20; index++) {
            Button control = controls[index];
            assertTrue(control.isEnabled());
            for (int code :
                    new int[] {
                        KeyEvent.KEYCODE_DPAD_UP,
                        KeyEvent.KEYCODE_DPAD_DOWN,
                        KeyEvent.KEYCODE_DPAD_LEFT,
                        KeyEvent.KEYCODE_DPAD_RIGHT
                    }) {
                assertTrue(key(control, KeyEvent.ACTION_DOWN, code));
                assertTrue(key(control, KeyEvent.ACTION_UP, code));
            }
            assertFalse(key(control, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SPACE));
            control.performClick();
        }
        assertTrue(key(controls[0], KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP));
        controls[0].performClick();
        assertEquals(1, new AquariumPreferences(activity).read().mode);
        assertFalse(controls[18].isEnabled());
        assertTrue(key(controls[0], KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN));
        assertTrue(controls[19].hasFocus());
        assertTrue(key(controls[17], KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN));
        controls[0].performClick();
        assertTrue(new AquariumPreferences(activity).read().isRandom(0));
        for (int index = 20; index < 27; index++) {
            assertTrue(controls[index].isEnabled());
            key(controls[index], KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP);
            key(controls[index], KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN);
            key(controls[index], KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT);
            key(controls[index], KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT);
            controls[index].performClick();
        }
        verifyPreviewAndReset(activity);
    }

    private void verifyPreviewAndReset(AquariumActivity activity) {
        Button preview = ReflectionHelpers.getField(activity, "preview");
        LinearLayout panel = ReflectionHelpers.getField(activity, "panel");
        Button reset = (Button) panel.getChildAt(panel.getChildCount() - 2);
        for (int code : new int[] {KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN}) {
            assertTrue(key(preview, KeyEvent.ACTION_DOWN, code));
            assertTrue(key(preview, KeyEvent.ACTION_UP, code));
        }
        assertFalse(key(preview, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SPACE));
        assertTrue(key(reset, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP));
        assertTrue(key(reset, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP));
        assertFalse(key(reset, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN));
        preview.performClick();
        assertEquals(View.GONE, panel.getVisibility());
        assertTrue(
                activity.onKeyDown(
                        KeyEvent.KEYCODE_BACK,
                        new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK)));
        assertEquals(View.VISIBLE, panel.getVisibility());
        assertTrue(
                activity.onKeyDown(
                        KeyEvent.KEYCODE_MENU,
                        new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MENU)));
        assertFalse(
                activity.onKeyDown(
                        KeyEvent.KEYCODE_SPACE,
                        new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SPACE)));
        activity.onKeyDown(
                KeyEvent.KEYCODE_BACK, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
        reset.performClick();
        assertEquals(16, new AquariumPreferences(activity).read().count);
        activity.onResume();
        activity.onPause();
    }

    @Test
    public void randomLabelsAndDisplayClockKeepSavedChoices() {
        Context context = RuntimeEnvironment.getApplication();
        AquariumOptions selected = AquariumOptions.defaults().withMask((1 << 20) - 1);
        new AquariumPreferences(context).save(selected);
        AquariumActivity activity =
                Robolectric.buildActivity(AquariumActivity.class).create().get();
        Button[] controls = ReflectionHelpers.getField(activity, "controls");
        assertEquals("Mode: Random version", controls[0].getText().toString());
        assertEquals("Look: Random (by mode)", controls[18].getText().toString());
        assertTrue(controls[1].getText().toString().endsWith("Random"));
        AquariumDisplay display = new AquariumDisplay(context, AquariumOptions.defaults());
        display.run();
        display.start();
        display.start();
        display.run();
        for (int clock = 0; clock < 3; clock++) {
            display.configure(AquariumOptions.defaults().withChoice(17, clock, 0));
            TextView text = ReflectionHelpers.getField(display, "clock");
            assertEquals(clock == 0 ? View.GONE : View.VISIBLE, text.getVisibility());
            if (clock != 0) assertTrue(text.getText().toString().contains(":"));
        }
        AquariumOptions footage = AquariumOptions.defaults().withChoice(0, 1, 0);
        display.configure(footage);
        display.configure(footage.withChoice(19, 1, 0));
        display.stop();
        display.start();
        display.configure(AquariumOptions.defaults());
        display.stop();
        display.configure(footage);
        display.stop();
        display.run();
        AquariumSceneView scene = new AquariumSceneView(context, AquariumOptions.defaults());
        scene.stop();
        scene.run();
        scene.start();
        scene.start();
        scene.run();
        scene.configure(AquariumOptions.defaults().withChoice(4, 2, 0));
        scene.stop();
        scene.stop();
        scene.run();
    }

    @Test
    public void dreamLifecycleStartsAndReleasesItsDisplay() {
        AquariumDreamService service =
                Robolectric.buildService(AquariumDreamService.class).create().get();
        service.onDreamingStarted();
        service.onDreamingStopped();
        service.onDetachedFromWindow();
        Activity owner = Robolectric.buildActivity(Activity.class).setup().get();
        ReflectionHelpers.setField(DreamService.class, service, "mWindow", owner.getWindow());
        service.onAttachedToWindow();
        assertFalse(service.isInteractive());
        assertTrue(service.isFullscreen());
        service.onDreamingStarted();
        service.onDreamingStopped();
        service.onDetachedFromWindow();
        service.onDetachedFromWindow();
    }
}
