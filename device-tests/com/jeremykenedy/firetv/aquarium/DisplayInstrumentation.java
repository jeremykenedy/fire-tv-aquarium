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
        int status = Activity.RESULT_OK;
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
            onMain(
                    () -> {
                        Button preview = button("Show aquarium");
                        preview.requestFocus();
                        for (int i = 0; i < 40 && !button("Mode:").hasFocus(); i++)
                            key(KeyEvent.KEYCODE_DPAD_UP);
                        report.append("Navigation start: ")
                                .append(
                                        activity.getCurrentFocus() instanceof Button
                                                ? ((Button) activity.getCurrentFocus()).getText()
                                                : "no button")
                                .append('\n');
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
                onMain(
                        () -> {
                            while (preferences.read().look != look
                                    || preferences.read().isRandom(18))
                                button("Look:").performClick();
                            check(
                                    preferences.read().time == 1,
                                    "Night retained for " + AquariumOptions.LOOKS[look]);
                        });
            }
            setMode(1);
            onMain(
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
            onMain(() -> activity.finish());
            Thread.sleep(800);
            activity =
                    startActivitySync(
                            new Intent(getTargetContext(), AquariumActivity.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            onMain(
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
            randomControls(preferences);
            int[] output = AquariumOutput.size(getTargetContext());
            check(
                    output[0] > 0 && output[1] > 0,
                    "Physical display resolution: " + output[0] + "x" + output[1]);
            report.append("UHD decode selected: ")
                    .append(AquariumOutput.supportsUhdVideo(getTargetContext()))
                    .append('\n');
            result.putString("stream", report + "All native display controls passed\n");
            status = Activity.RESULT_OK;
        } catch (Throwable failure) {
            result.putString(
                    "stream", report + "ERROR: " + failure + " cause=" + failure.getCause() + "\n");
            status = Activity.RESULT_CANCELED;
        } finally {
            if (activity != null) onMain(() -> activity.finish());
            preferences.save(original);
            getTargetContext().getSharedPreferences("aquarium", 0).edit().commit();
        }
        finish(status, result);
    }

    private void onMain(Runnable operation) {
        java.util.concurrent.atomic.AtomicReference<Throwable> failure =
                new java.util.concurrent.atomic.AtomicReference<>();
        runOnMainSync(
                () -> {
                    try {
                        operation.run();
                    } catch (Throwable exception) {
                        failure.set(exception);
                    }
                });
        if (failure.get() != null)
            throw new IllegalStateException("UI check failed", failure.get());
    }

    private void randomControls(AquariumPreferences preferences) throws Exception {
        setMode(0);
        onMain(
                () -> {
                    button("Reset aquarium").performClick();
                    // Left from the first scene reaches Random without changing the look.
                    button("Background:").requestFocus();
                    key(KeyEvent.KEYCODE_DPAD_LEFT);
                    check(preferences.read().isRandom(4), "Remote Left selects Random background");
                    check(
                            !preferences.read().isRandom(18) && preferences.read().look == 0,
                            "Realistic look remains fixed");
                    button("Show aquarium").performClick();
                    key(KeyEvent.KEYCODE_BACK);
                    check(preferences.read().isRandom(4), "Preview preserves saved Random choice");
                    // Random mode comes after original footage.
                    button("Mode:").performClick();
                    button("Mode:").performClick();
                    check(preferences.read().isRandom(0), "Random version can be selected");
                    check(!button("Look:").isEnabled(), "Random version owns appearance");
                    check(
                            button("Shuffle Realistic:").isEnabled(),
                            "Shuffle inclusion controls available");
                    button("Shuffle Cartoon:").performClick();
                    check(
                            (preferences.read().versions & 8) == 0,
                            "Individual version excluded from shuffle");
                });
        onMain(() -> button("Mode:").requestFocus());
        waitForIdleSync();
        Thread.sleep(1000);
        capture("random-settings.png");
        onMain(() -> button("Shuffle Realistic:").requestFocus());
        waitForIdleSync();
        Thread.sleep(300);
        capture("shuffle-pool.png");
        onMain(() -> activity.finish());
        Thread.sleep(300);
        activity =
                startActivitySync(
                        new Intent(getTargetContext(), AquariumActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        onMain(
                () -> {
                    check(
                            preferences.read().isRandom(0) && preferences.read().isRandom(4),
                            "Random selections survive reopening");
                    check(
                            (preferences.read().versions & 8) == 0,
                            "Shuffle inclusion survives reopening");
                    button("Show aquarium").performClick();
                    key(KeyEvent.KEYCODE_MENU);
                    check(button("Show aquarium").hasFocus(), "Menu returns from random preview");
                });
    }

    private void key(int code) {
        activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
        activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
    }

    private void setMode(int mode) {
        onMain(
                () -> {
                    while (new AquariumPreferences(getTargetContext()).read().mode != mode
                            || new AquariumPreferences(getTargetContext()).read().isRandom(0))
                        button("Mode:").performClick();
                });
    }

    private void setTime(int time) {
        onMain(
                () -> {
                    while (new AquariumPreferences(getTargetContext()).read().time != time
                            || new AquariumPreferences(getTargetContext()).read().isRandom(19))
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
