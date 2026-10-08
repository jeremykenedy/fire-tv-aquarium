#!/usr/bin/env bash
# Build an offline UHD screensaver with only the JDK and Android SDK.
# Run: bash build.sh
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
BUILD_TOOLS="$SDK/build-tools/36.0.0"
ANDROID_JAR="$SDK/platforms/android-36/android.jar"
OUT="$HERE/build"
KEYSTORE="$HOME/.android/firetv-aquarium.jks"
KEYPASS="$HOME/.android/firetv-aquarium.pass"

for tool in javac keytool openssl zip shasum ffprobe python3; do
  command -v "$tool" >/dev/null || { echo "Missing build tool: $tool" >&2; exit 1; }
done
[[ -f "$ANDROID_JAR" && -x "$BUILD_TOOLS/aapt2" ]] || {
  echo "Install Android SDK platform 36 and build-tools 36.0.0 first." >&2; exit 1;
}
[[ -s "$HERE/res/raw/aquarium.mp4" ]] || { echo "Missing bundled aquarium.mp4" >&2; exit 1; }
ffprobe -v error -select_streams v:0 -show_entries stream=width,height,codec_name \
  -of json "$HERE/res/raw/aquarium.mp4" | python3 -c '
import json, sys
streams = json.load(sys.stdin).get("streams", [])
if not streams or (streams[0].get("width"), streams[0].get("height")) != (3840, 2160):
    sys.exit("The bundled aquarium video must be native 3840x2160.")
if streams[0].get("codec_name") != "h264":
    sys.exit("The bundled aquarium video must use H.264.")
'

mkdir -p "$OUT" "$HOME/.android"
rm -rf "$OUT/classes" "$OUT/dex" "$OUT/generated"
mkdir -p "$OUT/classes" "$OUT/dex"
if [[ ! -f "$KEYSTORE" ]]; then
  [[ ! -e "$KEYPASS" ]] || { echo "Signing key missing but password exists. Restore the key." >&2; exit 1; }
  (umask 077 && openssl rand -hex 24 > "$KEYPASS")
  keytool -genkeypair -keystore "$KEYSTORE" -storepass:file "$KEYPASS" -alias aquarium \
    -keyalg RSA -keysize 2048 -validity 10950 -dname "CN=Jeremy Kenedy"
  chmod 600 "$KEYSTORE"
fi
[[ -s "$KEYPASS" ]] || { echo "Restore the signing-key password file." >&2; exit 1; }

"$BUILD_TOOLS/aapt2" compile --dir "$HERE/res" -o "$OUT/res.zip"
"$BUILD_TOOLS/aapt2" link -o "$OUT/unsigned.apk" -I "$ANDROID_JAR" \
  --manifest "$HERE/AndroidManifest.xml" --min-sdk-version 23 --target-sdk-version 30 \
  --version-code 2 --version-name 1.1.0 -0 mp4 --java "$OUT/generated" "$OUT/res.zip"
find "$HERE/src" "$OUT/generated" -name '*.java' > "$OUT/sources.txt"
javac -nowarn -Xlint:-options -source 8 -target 8 -bootclasspath "$ANDROID_JAR" \
  -d "$OUT/classes" @"$OUT/sources.txt"
find "$OUT/classes" -name '*.class' > "$OUT/classes.txt"
"$BUILD_TOOLS/d8" --release --lib "$ANDROID_JAR" --min-api 23 \
  --output "$OUT/dex" @"$OUT/classes.txt"
(cd "$OUT/dex" && zip -q -j "$OUT/unsigned.apk" classes.dex)
"$BUILD_TOOLS/zipalign" -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
"$BUILD_TOOLS/apksigner" sign --ks "$KEYSTORE" --ks-pass "file:$KEYPASS" --ks-key-alias aquarium \
  --out "$OUT/aquarium-4k.apk" "$OUT/aligned.apk"
"$BUILD_TOOLS/apksigner" verify "$OUT/aquarium-4k.apk"
(cd "$OUT" && shasum -a 256 aquarium-4k.apk > aquarium-4k.apk.sha256)
echo "Built $OUT/aquarium-4k.apk"
cat "$OUT/aquarium-4k.apk.sha256"
