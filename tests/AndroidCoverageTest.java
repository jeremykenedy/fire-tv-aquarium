package com.jeremykenedy.firetv.aquarium;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class AndroidCoverageTest {
    @Test
    public void invalidControlsAreRejectedAndAllMotionSettingsHaveDistinctValues() {
        AquariumOptions options = AquariumOptions.defaults();
        for (int control : new int[] {-1, 20}) {
            assertThrows(IllegalArgumentException.class, () -> options.choiceCount(control));
            assertThrows(IllegalArgumentException.class, () -> options.choice(control));
        }
        assertEquals(0.45f, options.withChoice(5, 0, 0).swimmingSpeed(), 0f);
        assertEquals(0.8f, options.withChoice(5, 1, 0).swimmingSpeed(), 0f);
        assertEquals(1.35f, options.withChoice(5, 2, 0).swimmingSpeed(), 0f);
        assertEquals(0.6f, options.withChoice(6, 0, 0).fishScale(), 0f);
        assertEquals(0.9f, options.withChoice(6, 1, 0).fishScale(), 0f);
        assertEquals(1.25f, options.withChoice(6, 2, 0).fishScale(), 0f);
        AquariumOptions emptyPool =
                new AquariumOptions(0, 16, 0, 0, 1, 1, 0, 0, true, true, 0, 0, 0, 0, 0, 0);
        assertEquals(127, emptyPool.versions);
    }

    @Test
    public void swimmingAndRandomSettingsKeepTheirContracts() {
        int previous = ReflectionHelpers.getStaticField(AquariumTest.class, "checks");
        AquariumTest.main(new String[0]);
        int completed = ReflectionHelpers.getStaticField(AquariumTest.class, "checks");
        assertTrue("The standalone suite must execute its assertions", completed > previous);
    }

    @Test
    public void preferencesRoundTripAllSettings() {
        Context context = RuntimeEnvironment.getApplication();
        AquariumPreferences preferences = new AquariumPreferences(context);
        AquariumOptions selected = AquariumOptions.defaults().withMask((1 << 20) - 1);
        preferences.save(selected);
        AquariumOptions loaded = preferences.read();
        assertEquals(selected.randomMask, loaded.randomMask);
        for (int index = 0; index < 20; index++) {
            assertEquals(selected.choice(index), loaded.choice(index));
        }
    }
}
