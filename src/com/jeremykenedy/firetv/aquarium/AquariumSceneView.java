package com.jeremykenedy.firetv.aquarium;

import android.content.Context;
import android.opengl.GLSurfaceView;

final class AquariumSceneView extends GLSurfaceView implements Runnable {
    private final AquariumAppearanceRenderer renderer;
    private boolean active;

    AquariumSceneView(Context context, AquariumOptions options) {
        super(context);
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        setPreserveEGLContextOnPause(true);
        renderer = new AquariumAppearanceRenderer(context.getResources(), options);
        setRenderer(renderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    void configure(AquariumOptions options) {
        renderer.configure(options);
    }

    void start() {
        if (active) return;
        active = true;
        onResume();
        post(this);
    }

    void stop() {
        if (!active) return;
        active = false;
        removeCallbacks(this);
        onPause();
    }

    @Override
    public void run() {
        if (!active) return;
        requestRender();
        postDelayed(this, 33);
    }
}
