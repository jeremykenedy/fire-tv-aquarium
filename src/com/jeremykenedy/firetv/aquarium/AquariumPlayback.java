package com.jeremykenedy.firetv.aquarium;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

/** Local, muted, hardware-decoded UHD playback, with no third-party libraries. */
final class AquariumPlayback extends FrameLayout implements SurfaceHolder.Callback,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {
    private static final String TAG = "Aquarium4K";
    private final SurfaceView video;
    private final TextView error;
    private MediaPlayer player;
    private boolean active;
    private boolean surfaceReady;

    AquariumPlayback(Context context) {
        super(context);
        setBackgroundColor(Color.BLACK);
        video = new SurfaceView(context);
        addView(video, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        video.getHolder().addCallback(this);
        // Fire TV can keep its UI at 1080p while the video surface is native UHD.
        video.getHolder().setFixedSize(3840, 2160);
        error = new TextView(context);
        error.setTextColor(Color.WHITE);
        error.setTextSize(24);
        error.setGravity(Gravity.CENTER);
        error.setVisibility(View.GONE);
        addView(error, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    static void configureWindow(Window window) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        Display display = window.getWindowManager().getDefaultDisplay();
        WindowManager.LayoutParams attributes = window.getAttributes();
        for (Display.Mode mode : display.getSupportedModes()) {
            if (mode.getPhysicalWidth() == 3840 && mode.getPhysicalHeight() == 2160) {
                attributes.preferredDisplayModeId = mode.getModeId();
                break;
            }
        }
        window.setAttributes(attributes);
    }

    void start() {
        active = true;
        prepareIfReady();
    }

    void stop() {
        active = false;
        releasePlayer();
    }

    private void prepareIfReady() {
        if (!active || !surfaceReady || player != null) return;
        error.setVisibility(View.GONE);
        MediaPlayer next = new MediaPlayer();
        player = next;
        try (AssetFileDescriptor clip = getResources().openRawResourceFd(R.raw.aquarium)) {
            next.setDataSource(clip.getFileDescriptor(), clip.getStartOffset(), clip.getLength());
            next.setDisplay(video.getHolder());
            next.setVolume(0, 0);
            next.setLooping(true);
            next.setOnPreparedListener(this);
            next.setOnErrorListener(this);
            next.prepareAsync();
        } catch (Exception exception) {
            Log.e(TAG, "Cannot open bundled aquarium footage", exception);
            showError("Aquarium video could not be opened. Press Back to exit.");
        }
    }

    @Override
    public void onPrepared(MediaPlayer prepared) {
        if (player != prepared || !active) return;
        Log.i(TAG, "Playing " + prepared.getVideoWidth() + "x" + prepared.getVideoHeight()
            + ", duration=" + prepared.getDuration() + "ms, muted, looping");
        prepared.start();
    }

    @Override
    public boolean onError(MediaPlayer failed, int what, int extra) {
        if (player == failed) showError("Playback failed (" + what + ", " + extra + ")");
        return true;
    }

    private void showError(String message) {
        releasePlayer();
        error.setText(message);
        error.setVisibility(View.VISIBLE);
    }

    private void releasePlayer() {
        MediaPlayer previous = player;
        player = null;
        if (previous != null) {
            previous.setOnPreparedListener(null);
            previous.setOnErrorListener(null);
            previous.release();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceReady = true;
        prepareIfReady();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG, "Video surface " + width + "x" + height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceReady = false;
        releasePlayer();
    }
}
