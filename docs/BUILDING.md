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
