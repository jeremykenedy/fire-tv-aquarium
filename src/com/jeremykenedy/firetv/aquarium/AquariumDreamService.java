package com.jeremykenedy.firetv.aquarium;

import android.service.dreams.DreamService;

public final class AquariumDreamService extends DreamService {
    private AquariumDisplay playback;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(true);
        AquariumPlayback.configureWindow(getWindow());
        playback = new AquariumDisplay(this, new AquariumPreferences(this).read());
        setContentView(playback);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        if (playback != null) playback.start();
    }

    @Override
    public void onDreamingStopped() {
        if (playback != null) playback.stop();
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        if (playback != null) playback.stop();
        playback = null;
        super.onDetachedFromWindow();
    }
}
