# Building

## Requirements

- JDK 21, with `java`, `javac`, and `keytool` on PATH.
- Android SDK platform 36 and build-tools 36.0.0.
- Python 3.10+, FFmpeg (`ffprobe`), OpenSSL, zip, and a SHA-256 utility.

On macOS, `ANDROID_HOME` defaults to `$HOME/Library/Android/sdk`. On Linux,
set it to your installed SDK directory. Install the SDK packages with its
command-line tools:

```bash
sdkmanager 'platforms;android-36' 'build-tools;36.0.0'
```

## Build and test

```bash
bash test.sh
bash build.sh
python3 check_apk.py
```

`build.sh` compiles resources with AAPT2, Java with `javac`, and DEX with D8,
aligns the APK, signs it, verifies the signature, and creates the checksum.
No Gradle or external Android libraries are required. The app targets API 30
and supports API 23 or newer. The SDK platform used to compile is API 36.

Outputs:

| File | Purpose |
|---|---|
| `build/aquarium-4k.apk` | Signed installation package |
| `build/aquarium-4k.apk.sha256` | SHA-256 checksum |
| `build/classes/` | Compiled application classes |
| `build/tests/` | Compiled platform-independent tests |

The bundled video must be H.264 at exactly 3840x2160. Build validation rejects
lower-resolution or incompatible footage. The video is stored uncompressed
in the APK for native resource-file playback.

## Native appearance checks

The separate device-check APK renders every look and background on the TV's
GPU into a 3840x2160 offscreen surface. It also checks each fish species,
optional sea-life type, and saved appearance preferences. It does not open an
activity. Replace `DEVICE_IP` with your connected TV's address:

```bash
bash scripts/build-device-checks.sh
adb -s DEVICE_IP:5555 install -r build/device-tests/device-checks.apk
adb -s DEVICE_IP:5555 shell am instrument -w \
  com.jeremykenedy.firetv.aquarium.devicechecks/com.jeremykenedy.firetv.aquarium.AppearanceInstrumentation
adb -s DEVICE_IP:5555 shell am instrument -w \
  com.jeremykenedy.firetv.aquarium.devicechecks/com.jeremykenedy.firetv.aquarium.DisplayInstrumentation
adb -s DEVICE_IP:5555 shell am instrument -w -e export true \
  com.jeremykenedy.firetv.aquarium.devicechecks/com.jeremykenedy.firetv.aquarium.AppearanceInstrumentation
adb -s DEVICE_IP:5555 pull /sdcard/Download/AquariumChecks build/device-captures
adb -s DEVICE_IP:5555 uninstall com.jeremykenedy.firetv.aquarium.devicechecks
```

A successful run prints `All native appearance checks passed`. Captures are
reduced to 1920x1080 for documentation. The checks preserve and restore the app's
prior preferences. Capture export uses scoped media storage on API 29 or newer;
no storage permission is added to either APK. CI compiles this APK; running
these GPU checks requires an actual device. The device-check APK
is separate from the application and is not included in releases.

Display checks open the actual settings activity, exercise remote navigation and
Day / Night in both modes, and verify saved selections after reopening. A passing
run prints `All native display controls passed`. Screen captures are skipped if
another app owns the foreground. Both check runners restore prior preferences.

## Signing key

The first build creates a private key at
`$HOME/.android/firetv-aquarium.jks` and its random password at
`$HOME/.android/firetv-aquarium.pass`. Both have private filesystem permissions.
Back up both securely and retain the key for every upgrade.

If the key is missing while the password remains, the build stops rather than
silently signing an incompatible replacement. Never commit either file, paste
its password into logs, or publish it with an APK.

CI creates a temporary key only to validate a build. Downloadable release APKs
are signed locally using the stable application key. A CI-generated APK cannot
upgrade an existing release installation signed with a different key.
