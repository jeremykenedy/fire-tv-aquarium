package com.jeremykenedy.firetv.aquarium;

import android.content.Context;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.view.Display;
import android.view.WindowManager;

/** Uses the physical TV mode, which can differ from the resolution of the launcher UI. */
final class AquariumOutput {
    private AquariumOutput() {}

    static int[] size(Context context) {
        Display display =
                ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
                        .getDefaultDisplay();
        for (Display.Mode mode : display.getSupportedModes()) {
            if (mode.getPhysicalWidth() == 3840 && mode.getPhysicalHeight() == 2160)
                return new int[] {3840, 2160};
        }
        Display.Mode current = display.getMode();
        int width = current.getPhysicalWidth();
        int height = current.getPhysicalHeight();
        float scale = Math.min(1f, Math.min(3840f / width, 2160f / height));
        return new int[] {Math.max(1, (int) (width * scale)), Math.max(1, (int) (height * scale))};
    }

    static boolean supportsUhdVideo(Context context) {
        int[] output = size(context);
        if (output[0] < 3840 || output[1] < 2160) return false;
        try {
            for (MediaCodecInfo codec :
                    new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos()) {
                if (codec.isEncoder()) continue;
                String name = codec.getName().toLowerCase(java.util.Locale.ROOT);
                if (name.startsWith("omx.google.") || name.startsWith("c2.android.")) continue;
                for (String type : codec.getSupportedTypes()) {
                    if (!type.equalsIgnoreCase("video/avc")) continue;
                    MediaCodecInfo.VideoCapabilities video =
                            codec.getCapabilitiesForType(type).getVideoCapabilities();
                    if (video != null && video.areSizeAndRateSupported(3840, 2160, 24)) return true;
                }
            }
        } catch (RuntimeException exception) {
            // Incomplete vendor codec reporting should select the smaller bundled loop.
        }
        return false;
    }
}
