package com.jeremykenedy.firetv.aquarium;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Remote-operated settings with a live preview of the saved aquarium. */
public final class AquariumActivity extends Activity {
    private AquariumPreferences preferences;
    private AquariumOptions options;
    private AquariumDisplay aquarium;
    private LinearLayout panel;
    private Button preview;
    private final Button[] controls = new Button[18];

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        AquariumPlayback.configureWindow(getWindow());
        preferences = new AquariumPreferences(this);
        options = preferences.read();
        FrameLayout root = new FrameLayout(this);
        aquarium = new AquariumDisplay(this, options);
        root.addView(aquarium, new FrameLayout.LayoutParams(-1, -1));
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(20), dp(20), dp(12));
        panel.setBackground(background(0xEE072431, 0xFF204653));
        FrameLayout.LayoutParams placement =
                new FrameLayout.LayoutParams(dp(342), -1, Gravity.LEFT);
        placement.setMargins(dp(20), dp(20), 0, dp(20));
        root.addView(panel, placement);
        TextView title = new TextView(this);
        title.setText("Aquarium 4K");
        title.setTextSize(30);
        title.setTextColor(0xFFE7F2EE);
        title.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        panel.addView(title);
        TextView hint = new TextView(this);
        hint.setText("Left / Right to adjust. Menu opens settings.");
        hint.setTextSize(12);
        hint.setTextColor(0xFF91B6BD);
        hint.setPadding(0, dp(6), 0, dp(12));
        panel.addView(hint);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setSmoothScrollingEnabled(false);
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(rows);
        for (int i = 0; i < controls.length; i++) {
            final int index = i;
            Button control = button("");
            controls[i] = control;
            LinearLayout.LayoutParams row = new LinearLayout.LayoutParams(-1, dp(43));
            row.bottomMargin = dp(3);
            rows.addView(control, row);
            control.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            adjust(index, 1);
                        }
                    });
            control.setOnKeyListener(
                    new View.OnKeyListener() {
                        @Override
                        public boolean onKey(View v, int key, KeyEvent event) {
                            if (key == KeyEvent.KEYCODE_DPAD_UP
                                    || key == KeyEvent.KEYCODE_DPAD_DOWN) {
                                if (event.getAction() == KeyEvent.ACTION_DOWN)
                                    focusControl(index, key == KeyEvent.KEYCODE_DPAD_UP ? -1 : 1);
                                return true;
                            }
                            if (key != KeyEvent.KEYCODE_DPAD_LEFT
                                    && key != KeyEvent.KEYCODE_DPAD_RIGHT) return false;
                            if (event.getAction() == KeyEvent.ACTION_DOWN)
                                adjust(index, key == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 1);
                            return true;
                        }
                    });
        }
        preview = button("Show aquarium");
        preview.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        panel.setVisibility(View.GONE);
                    }
                });
        panel.addView(preview, new LinearLayout.LayoutParams(-1, dp(46)));
        final Button reset = button("Reset aquarium");
        reset.setTextSize(13);
        reset.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        options = AquariumOptions.defaults();
                        changed();
                    }
                });
        panel.addView(reset, new LinearLayout.LayoutParams(-1, dp(36)));
        preview.setOnKeyListener(
                new View.OnKeyListener() {
                    @Override
                    public boolean onKey(View v, int key, KeyEvent event) {
                        if (key != KeyEvent.KEYCODE_DPAD_UP && key != KeyEvent.KEYCODE_DPAD_DOWN)
                            return false;
                        if (event.getAction() == KeyEvent.ACTION_DOWN) {
                            if (key == KeyEvent.KEYCODE_DPAD_UP) controls[17].requestFocus();
                            else reset.requestFocus();
                        }
                        return true;
                    }
                });
        reset.setOnKeyListener(
                new View.OnKeyListener() {
                    @Override
                    public boolean onKey(View v, int key, KeyEvent event) {
                        if (key != KeyEvent.KEYCODE_DPAD_UP) return false;
                        if (event.getAction() == KeyEvent.ACTION_DOWN) preview.requestFocus();
                        return true;
                    }
                });
        TextView privacy = new TextView(this);
        privacy.setText("Offline. No ads or analytics.");
        privacy.setTextSize(11);
        privacy.setTextColor(0xFF91B6BD);
        privacy.setGravity(Gravity.CENTER);
        panel.addView(privacy);
        setContentView(root);
        updateLabels();
        preview.requestFocus();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable background(int fill, int stroke) {
        GradientDrawable result = new GradientDrawable();
        result.setColor(fill);
        result.setCornerRadius(dp(6));
        result.setStroke(dp(1), stroke);
        return result;
    }

    private Button button(String text) {
        Button result = new Button(this);
        result.setText(text);
        result.setTextSize(15);
        result.setTextColor(0xFFE7F2EE);
        result.setAllCaps(false);
        result.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        result.setPadding(dp(13), 0, dp(13), 0);
        StateListDrawable states = new StateListDrawable();
        states.addState(
                new int[] {android.R.attr.state_focused}, background(0xFF245D69, 0xFF88D8D2));
        states.addState(new int[] {}, background(0xFF103745, 0xFF204653));
        result.setBackground(states);
        return result;
    }

    private void adjust(int index, int delta) {
        int mode = options.mode,
                count = options.count,
                species = options.species,
                scene = options.scene,
                speed = options.speed,
                size = options.size,
                light = options.light,
                clock = options.clock,
                population = options.population,
                creatures = options.creatures;
        boolean bubbles = options.bubbles, shimmer = options.rays;
        switch (index) {
            case 0:
                mode = AquariumOptions.cycle(mode, delta, AquariumOptions.MODES.length);
                break;
            case 1:
                population =
                        AquariumOptions.cycle(
                                population, delta, AquariumOptions.POPULATIONS.length);
                if (population != 0) count = AquariumOptions.POPULATION_COUNTS[population];
                break;
            case 2:
                count = AquariumOptions.bounded(count + delta, 0, AquariumOptions.MAX_FISH);
                population = 0;
                break;
            case 3:
                species = AquariumOptions.cycle(species, delta, AquariumOptions.SPECIES.length);
                break;
            case 4:
                scene = AquariumOptions.cycle(scene, delta, AquariumOptions.SCENES.length);
                break;
            case 5:
                speed = AquariumOptions.cycle(speed, delta, AquariumOptions.SPEEDS.length);
                break;
            case 6:
                size = AquariumOptions.cycle(size, delta, AquariumOptions.SIZES.length);
                break;
            case 7:
                light = AquariumOptions.cycle(light, delta, AquariumOptions.LIGHTS.length);
                break;
            case 8:
                bubbles = !bubbles;
                break;
            case 9:
                shimmer = !shimmer;
                break;
            case 17:
                clock = AquariumOptions.cycle(clock, delta, AquariumOptions.CLOCKS.length);
                break;
            default:
                if (index < 10 || index > 16) throw new IllegalArgumentException("Unknown control");
                creatures ^= 1 << (index - 10);
        }
        options =
                new AquariumOptions(
                        mode,
                        count,
                        species,
                        scene,
                        speed,
                        size,
                        light,
                        clock,
                        bubbles,
                        shimmer,
                        population,
                        creatures);
        changed();
    }

    private void changed() {
        preferences.save(options);
        aquarium.configure(options);
        updateLabels();
    }

    private void focusControl(int index, int direction) {
        int target = index + direction;
        while (target >= 0 && target < controls.length && !controls[target].isEnabled())
            target += direction;
        if (target >= controls.length) preview.requestFocus();
        else controls[Math.max(0, target)].requestFocus();
    }

    private void updateLabels() {
        String[] labels = {
            "Mode: " + AquariumOptions.MODES[options.mode],
            "Population: " + AquariumOptions.POPULATIONS[options.population],
            "Fish: " + options.count,
            "Species: " + AquariumOptions.SPECIES[options.species],
            "Background: " + AquariumOptions.SCENES[options.scene],
            "Swimming: " + AquariumOptions.SPEEDS[options.speed],
            "Fish size: " + AquariumOptions.SIZES[options.size],
            "Lighting: " + AquariumOptions.LIGHTS[options.light],
            "Bubbles: " + (options.bubbles ? "On" : "Off"),
            "Sunlight shimmer: " + (options.rays ? "On" : "Off"),
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "Clock: " + AquariumOptions.CLOCKS[options.clock]
        };
        for (int i = 0; i < AquariumOptions.CREATURE_NAMES.length; i++)
            labels[10 + i] =
                    AquariumOptions.CREATURE_NAMES[i]
                            + ": "
                            + (options.hasCreature(1 << i) ? "On" : "Off");
        for (int i = 0; i < controls.length; i++) {
            controls[i].setText(labels[i]);
            boolean enabled = options.mode == 0 || i == 0 || i == 17;
            controls[i].setEnabled(enabled);
            controls[i].setAlpha(enabled ? 1 : 0.45f);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        aquarium.start();
    }

    @Override
    public void onPause() {
        aquarium.stop();
        super.onPause();
    }

    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        if (key == KeyEvent.KEYCODE_MENU
                || (key == KeyEvent.KEYCODE_BACK && panel.getVisibility() == View.GONE)) {
            panel.setVisibility(View.VISIBLE);
            preview.requestFocus();
            return true;
        }
        return super.onKeyDown(key, event);
    }
}
