package com.jeremykenedy.firetv.aquarium;

import android.content.Context;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class AquariumDisplay extends FrameLayout implements Runnable {
    private AquariumOptions options;
    private AquariumSceneView scene;
    private AquariumPlayback footage;
    private final TextView clock;
    private boolean active;

    AquariumDisplay(Context context, AquariumOptions initial) {
        super(context);
        setBackgroundColor(0xFF072431);
        clock = new TextView(context);
        clock.setTextColor(0xDDE7F2EE);
        clock.setTextSize(20);
        clock.setShadowLayer(4, 0, 2, 0xFF000000);
        LayoutParams placement = new LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.RIGHT);
        int margin = (int) (28 * getResources().getDisplayMetrics().density);
        placement.setMargins(margin, margin, margin, margin);
        addView(clock, placement);
        configure(initial);
    }

    void configure(AquariumOptions next) {
        boolean replace = options == null || options.mode != next.mode;
        options = next;
        if (replace) {
            if (scene != null) { scene.stop(); removeView(scene); scene = null; }
            if (footage != null) { footage.stop(); removeView(footage); footage = null; }
            if (options.mode == 0) {
                scene = new AquariumSceneView(getContext(), options);
                addView(scene, 0, new LayoutParams(-1, -1));
                if (active) scene.start();
            } else {
                footage = new AquariumPlayback(getContext());
                addView(footage, 0, new LayoutParams(-1, -1));
                if (active) footage.start();
            }
        } else if (scene != null) scene.configure(options);
        updateClock();
    }

    void start() {
        if (active) return;
        active = true;
        if (scene != null) scene.start();
        if (footage != null) footage.start();
        post(this);
    }
    void stop() {
        active = false;
        removeCallbacks(this);
        if (scene != null) scene.stop();
        if (footage != null) footage.stop();
    }
    private void updateClock() {
        clock.setVisibility(options.clock == 0 ? GONE : VISIBLE);
        if (options.clock != 0) clock.setText(new SimpleDateFormat(
            options.clock == 1 ? "h:mm a" : "HH:mm", Locale.getDefault()).format(new Date()));
    }
    @Override public void run() {
        if (!active) return;
        updateClock();
        postDelayed(this, 1000);
    }
}
