package com.jeremykenedy.firetv.aquarium;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.io.File;
import java.io.FileOutputStream;

/** Exercises the real settings buttons without sending keys to another app. */
public final class DisplayInstrumentation extends Instrumentation {
    private Activity activity;
    private final StringBuilder report = new StringBuilder();

    @Override
    public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        start();
    }

    @Override
    public void onStart() {
        Bundle result = new Bundle();
        AquariumPreferences preferences = new AquariumPreferences(getTargetContext());
        AquariumOptions original = preferences.read();
        try {
            activity =
                    startActivitySync(
                            new Intent(getTargetContext(), AquariumActivity.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            setMode(0);
            setTime(0);
            waitForIdleSync();
            Thread.sleep(800);
            capture("display-day.png");
            runOnMainSync(
                    () -> {
                        Button preview = button("Show aquarium");
                        preview.requestFocus();
                        for (int i = 0; i < 20; i++) key(KeyEvent.KEYCODE_DPAD_UP);
                        key(KeyEvent.KEYCODE_DPAD_DOWN);
                        key(KeyEvent.KEYCODE_DPAD_DOWN);
                        check(
                                button("Day / Night:").hasFocus(),
                                "Remote navigation reaches Day / Night");
                        key(KeyEvent.KEYCODE_DPAD_RIGHT);
                        check(
                                button("Day / Night:").getText().toString().endsWith("Night"),
                                "Remote Right selects Night");
                    });
            Thread.sleep(800);
            capture("display-night.png");
            for (int i = 0; i < 6; i++) {
                final int look = i;
                runOnMainSync(
                        () -> {
                            while (preferences.read().look != look) button("Look:").performClick();
                            check(
                                    preferences.read().time == 1,
                                    "Night retained for " + AquariumOptions.LOOKS[look]);
                        });
            }
            setMode(1);
            runOnMainSync(
                    () -> {
                        check(
                                button("Day / Night:").isEnabled(),
                                "Night available for original footage");
                        check(
                                !button("Look:").isEnabled(),
                                "Animated look disabled for recorded footage");
                    });
            Thread.sleep(1000);
            capture("original-night.png");
            setTime(0);
            Thread.sleep(800);
            capture("original-day.png");
            setTime(1);
            runOnMainSync(() -> activity.finish());
            Thread.sleep(800);
            activity =
                    startActivitySync(
                            new Intent(getTargetContext(), AquariumActivity.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            runOnMainSync(
                    () -> {
                        check(
                                button("Day / Night:").getText().toString().endsWith("Night"),
                                "Night survives closing and reopening settings");
                        check(
                                button("Mode:")
                                        .getText()
                                        .toString()
                                        .endsWith("Original 4K footage"),
                                "Original footage selection persists");
                    });
            result.putString("stream", report + "All native display controls passed\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable failure) {
            result.putString("stream", report + "ERROR: " + failure + "\n");
            finish(Activity.RESULT_CANCELED, result);
        } finally {
            if (activity != null) runOnMainSync(() -> activity.finish());
            preferences.save(original);
        }
    }

    private void key(int code) {
        activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
        activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
    }

    private void setMode(int mode) {
        runOnMainSync(
                () -> {
                    if (new AquariumPreferences(getTargetContext()).read().mode != mode)
                        button("Mode:").performClick();
                });
    }

    private void setTime(int time) {
        runOnMainSync(
                () -> {
                    if (new AquariumPreferences(getTargetContext()).read().time != time)
                        button("Day / Night:").performClick();
                });
    }

    private Button button(String prefix) {
        Button found = find(activity.findViewById(android.R.id.content), prefix);
        if (found == null) throw new IllegalStateException("Missing control: " + prefix);
        return found;
    }

    private Button find(View view, String prefix) {
        if (view instanceof Button && ((Button) view).getText().toString().startsWith(prefix))
            return (Button) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                Button found = find(group.getChildAt(i), prefix);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void capture(String name) throws Exception {
        if (!activity.hasWindowFocus()) {
            report.append("SKIP capture: another app has focus: ").append(name).append('\n');
            return;
        }
        Bitmap image = getUiAutomation().takeScreenshot();
        if (image == null) throw new IllegalStateException("Device capture");
        if (activity.hasWindowFocus()) {
            File directory =
                    new File(getTargetContext().getExternalFilesDir(null), "appearance-checks");
            if (!directory.isDirectory() && !directory.mkdirs())
                throw new IllegalStateException("Capture directory");
            try (FileOutputStream output = new FileOutputStream(new File(directory, name))) {
                image.compress(Bitmap.CompressFormat.PNG, 100, output);
            }
            report.append("CAPTURE: ").append(name).append('\n');
        }
        image.recycle();
    }

    private void check(boolean success, String name) {
        if (!success) throw new IllegalStateException(name);
        report.append("PASS: ").append(name).append('\n');
    }
}
