package com.jeremykenedy.firetv.aquarium;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.media.MediaCodecInfo;
import android.media.MediaPlayer;
import android.os.Looper;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowMediaCodecList;
import org.robolectric.shadows.ShadowMediaPlayer;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(
        sdk = 28,
        shadows = {
            OutputPolicyTest.DisplayModes.class,
            OutputPolicyTest.CodecList.class,
            OutputPolicyTest.CodecDetails.class
        })
public final class PlaybackCoverageTest {
    @Before
    public void localClipsCanPrepare() {
        OutputPolicyTest.DisplayModes.current = OutputPolicyTest.mode(1, 1920, 1080);
        OutputPolicyTest.DisplayModes.supported = new android.view.Display.Mode[0];
        OutputPolicyTest.CodecList.broken = false;
        ShadowMediaCodecList.reset();
        ShadowMediaPlayer.setMediaInfoProvider(source -> new ShadowMediaPlayer.MediaInfo(28000, 0));
    }

    private SurfaceHolder holder(AquariumPlayback playback) {
        SurfaceView video = ReflectionHelpers.getField(playback, "video");
        return video.getHolder();
    }

    @Test
    public void localPlaybackIsMutedLoopingAndIgnoresStaleCallbacks() {
        AquariumPlayback playback = new AquariumPlayback(RuntimeEnvironment.getApplication());
        SurfaceHolder surface = holder(playback);
        playback.surfaceCreated(surface);
        assertNull(ReflectionHelpers.getField(playback, "player"));
        playback.start();
        MediaPlayer player = ReflectionHelpers.getField(playback, "player");
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertTrue(player.isPlaying());
        assertTrue(player.isLooping());
        assertEquals(0f, Shadows.shadowOf(player).getLeftVolume(), 0f);
        assertEquals(0f, Shadows.shadowOf(player).getRightVolume(), 0f);
        playback.start();
        MediaPlayer unrelated = new MediaPlayer();
        playback.onPrepared(unrelated);
        assertTrue(playback.onError(unrelated, 1, 2));
        ReflectionHelpers.setField(playback, "active", false);
        playback.onPrepared(player);
        ReflectionHelpers.setField(playback, "active", true);
        playback.surfaceChanged(surface, 0, 1920, 1080);
        playback.surfaceDestroyed(surface);
        assertNull(ReflectionHelpers.getField(playback, "player"));
        playback.onPrepared(player);
        playback.stop();
        playback.start();
        assertNull(ReflectionHelpers.getField(playback, "player"));
        playback.surfaceCreated(surface);
        playback.stop();
        playback.stop();
        unrelated.release();
    }

    @Test
    public void uhdFailureRetriesHdOnceThenDisplaysTheError() {
        OutputPolicyTest.DisplayModes.supported =
                new android.view.Display.Mode[] {OutputPolicyTest.mode(7, 3840, 2160)};
        ShadowMediaCodecList.addCodec(
                OutputPolicyTest.codec(
                        "vendor.uhd",
                        false,
                        "video/avc",
                        MediaCodecInfo.CodecProfileLevel.AVCLevel52));
        AquariumPlayback playback = new AquariumPlayback(RuntimeEnvironment.getApplication());
        playback.start();
        playback.surfaceCreated(holder(playback));
        MediaPlayer first = ReflectionHelpers.getField(playback, "player");
        assertEquals(false, ReflectionHelpers.getField(playback, "hd"));
        playback.onError(first, 1, 2);
        MediaPlayer second = ReflectionHelpers.getField(playback, "player");
        assertNotSame(first, second);
        assertEquals(true, ReflectionHelpers.getField(playback, "hd"));
        playback.onError(first, 1, 2);
        playback.onError(second, 3, 4);
        TextView error = ReflectionHelpers.getField(playback, "error");
        assertEquals(View.VISIBLE, error.getVisibility());
        assertEquals("Playback failed (3, 4)", error.getText().toString());
        assertNull(ReflectionHelpers.getField(playback, "player"));
        playback.stop();
    }

    @Test
    public void unreadableClipsShowAnExitMessage() {
        Context context = RuntimeEnvironment.getApplication();
        ShadowMediaPlayer.setMediaInfoProvider(
                source -> {
                    throw new IllegalStateException("Cannot read clip");
                });
        AquariumPlayback playback = new AquariumPlayback(context);
        playback.start();
        playback.surfaceCreated(holder(playback));
        TextView error = ReflectionHelpers.getField(playback, "error");
        assertEquals(View.VISIBLE, error.getVisibility());
        assertTrue(error.getText().toString().contains("Press Back to exit"));
        assertNull(ReflectionHelpers.getField(playback, "player"));
        playback.stop();
    }
}
