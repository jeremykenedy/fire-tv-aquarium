package com.jeremykenedy.firetv.aquarium;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.view.Display;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.MediaCodecInfoBuilder;
import org.robolectric.shadows.ShadowDisplay;
import org.robolectric.shadows.ShadowMediaCodecList;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

@RunWith(RobolectricTestRunner.class)
@Config(
        sdk = 28,
        shadows = {
            OutputPolicyTest.DisplayModes.class,
            OutputPolicyTest.CodecList.class,
            OutputPolicyTest.CodecDetails.class
        })
public final class OutputPolicyTest {
    @Implements(Display.class)
    public static class DisplayModes extends ShadowDisplay {
        static Display.Mode current;
        static Display.Mode[] supported;

        @Implementation
        protected Display.Mode getMode() {
            return current;
        }

        @Implementation
        protected Display.Mode[] getSupportedModes() {
            return supported;
        }
    }

    @Implements(MediaCodecList.class)
    public static class CodecList extends ShadowMediaCodecList {
        static boolean broken;
        @RealObject MediaCodecList list;

        @Implementation
        protected MediaCodecInfo[] getCodecInfos() {
            if (broken) throw new IllegalStateException("Incomplete vendor reporting");
            return Shadow.directlyOn(list, MediaCodecList.class, "getCodecInfos");
        }
    }

    @Implements(MediaCodecInfo.class)
    public static class CodecDetails {
        @RealObject MediaCodecInfo codec;

        @Implementation
        protected MediaCodecInfo.CodecCapabilities getCapabilitiesForType(String type) {
            MediaCodecInfo.CodecCapabilities caps =
                    Shadow.directlyOn(
                            codec,
                            MediaCodecInfo.class,
                            "getCapabilitiesForType",
                            ClassParameter.from(String.class, type));
            if (codec.getName().equals("vendor.incomplete")) {
                ReflectionHelpers.setField(caps, "mVideoCaps", null);
            }
            return caps;
        }
    }

    static Display.Mode mode(int id, int width, int height) {
        return ReflectionHelpers.callConstructor(
                Display.Mode.class,
                ClassParameter.from(int.class, id),
                ClassParameter.from(int.class, width),
                ClassParameter.from(int.class, height),
                ClassParameter.from(float.class, 60f));
    }

    static MediaCodecInfo codec(String name, boolean encoder, String mime, int level) {
        MediaFormat format = MediaFormat.createVideoFormat(mime, 3840, 2160);
        MediaCodecInfo.CodecProfileLevel profile = new MediaCodecInfo.CodecProfileLevel();
        profile.profile = MediaCodecInfo.CodecProfileLevel.AVCProfileHigh;
        profile.level = level;
        MediaCodecInfo.CodecCapabilities capabilities =
                MediaCodecInfoBuilder.CodecCapabilitiesBuilder.newBuilder()
                        .setMediaFormat(format)
                        .setIsEncoder(encoder)
                        .setProfileLevels(new MediaCodecInfo.CodecProfileLevel[] {profile})
                        .setColorFormats(
                                new int[] {
                                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
                                })
                        .build();
        return MediaCodecInfoBuilder.newBuilder()
                .setName(name)
                .setIsEncoder(encoder)
                .setCapabilities(capabilities)
                .build();
    }

    @Before
    public void resetOutput() {
        DisplayModes.current = mode(1, 1920, 1080);
        DisplayModes.supported = new Display.Mode[0];
        CodecList.broken = false;
        ShadowMediaCodecList.reset();
    }

    @Test
    public void physicalModesSelectNativeUhdAndBoundOtherPanels() {
        assertArrayEquals(
                new int[] {1920, 1080}, AquariumOutput.size(RuntimeEnvironment.getApplication()));
        assertFalse(AquariumOutput.supportsUhdVideo(RuntimeEnvironment.getApplication()));
        DisplayModes.current = mode(1, 3840, 1080);
        assertFalse(AquariumOutput.supportsUhdVideo(RuntimeEnvironment.getApplication()));
        DisplayModes.current = mode(1, 7680, 4320);
        assertArrayEquals(
                new int[] {3840, 2160}, AquariumOutput.size(RuntimeEnvironment.getApplication()));
        DisplayModes.supported =
                new Display.Mode[] {mode(2, 1920, 1080), mode(3, 3840, 1080), mode(7, 3840, 2160)};
        assertArrayEquals(
                new int[] {3840, 2160}, AquariumOutput.size(RuntimeEnvironment.getApplication()));
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        AquariumPlayback.configureWindow(activity.getWindow());
        assertEquals(7, activity.getWindow().getAttributes().preferredDisplayModeId);
        DisplayModes.supported = new Display.Mode[] {mode(2, 1920, 1080), mode(3, 3840, 1080)};
        AquariumPlayback.configureWindow(activity.getWindow());
        assertTrue(activity.getWindow().getDecorView().getSystemUiVisibility() != 0);
    }

    @Test
    public void onlyCapableHardwareAvcDecodersSelectUhd() {
        DisplayModes.supported = new Display.Mode[] {mode(7, 3840, 2160)};
        ShadowMediaCodecList.addCodec(
                codec(
                        "vendor.encoder",
                        true,
                        "video/avc",
                        MediaCodecInfo.CodecProfileLevel.AVCLevel52));
        ShadowMediaCodecList.addCodec(
                codec(
                        "OMX.GOOGLE.decoder",
                        false,
                        "video/avc",
                        MediaCodecInfo.CodecProfileLevel.AVCLevel52));
        ShadowMediaCodecList.addCodec(
                codec(
                        "c2.android.decoder",
                        false,
                        "video/avc",
                        MediaCodecInfo.CodecProfileLevel.AVCLevel52));
        ShadowMediaCodecList.addCodec(
                codec(
                        "vendor.other",
                        false,
                        "video/hevc",
                        MediaCodecInfo.CodecProfileLevel.AVCLevel52));
        MediaCodecInfo incomplete =
                codec(
                        "vendor.incomplete",
                        false,
                        "video/avc",
                        MediaCodecInfo.CodecProfileLevel.AVCLevel52);
        ReflectionHelpers.setField(
                incomplete.getCapabilitiesForType("video/avc"), "mVideoCaps", null);
        ShadowMediaCodecList.addCodec(incomplete);
        assertFalse(
                "incomplete reporting",
                AquariumOutput.supportsUhdVideo(RuntimeEnvironment.getApplication()));
    }

    @Test
    public void decoderResolutionAndBrokenVendorReportingSelectFallback() {
        DisplayModes.supported = new Display.Mode[] {mode(7, 3840, 2160)};
        ShadowMediaCodecList.reset();
        ShadowMediaCodecList.addCodec(
                codec(
                        "vendor.hd",
                        false,
                        "video/avc",
                        MediaCodecInfo.CodecProfileLevel.AVCLevel31));
        assertFalse(AquariumOutput.supportsUhdVideo(RuntimeEnvironment.getApplication()));
        ShadowMediaCodecList.reset();
        ShadowMediaCodecList.addCodec(
                codec(
                        "vendor.uhd",
                        false,
                        "video/avc",
                        MediaCodecInfo.CodecProfileLevel.AVCLevel52));
        assertTrue(AquariumOutput.supportsUhdVideo(RuntimeEnvironment.getApplication()));
        CodecList.broken = true;
        assertFalse(AquariumOutput.supportsUhdVideo(RuntimeEnvironment.getApplication()));
    }
}
