#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
TOOLS="$SDK/build-tools/36.0.0"
ANDROID_JAR="$SDK/platforms/android-36/android.jar"
OUT="$HERE/build/device-tests"
mkdir -p "$OUT/classes" "$OUT/dex"
find "$HERE/device-tests" -name '*.java' > "$OUT/sources.txt"
javac -nowarn --release 8 -cp "$ANDROID_JAR:$HERE/build/classes" \
  -d "$OUT/classes" @"$OUT/sources.txt"
find "$OUT/classes" -name '*.class' > "$OUT/classes.txt"
"$TOOLS/d8" --release --lib "$ANDROID_JAR" --classpath "$HERE/build/classes" \
  --min-api 23 --output "$OUT/dex" @"$OUT/classes.txt"
"$TOOLS/aapt2" link -o "$OUT/unsigned.apk" -I "$ANDROID_JAR" \
  --manifest "$HERE/device-tests/AndroidManifest.xml" --min-sdk-version 23 --target-sdk-version 36
(cd "$OUT/dex" && zip -q -j "$OUT/unsigned.apk" classes.dex)
"$TOOLS/zipalign" -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
"$TOOLS/apksigner" sign --ks "$HOME/.android/firetv-aquarium.jks" \
  --ks-pass "file:$HOME/.android/firetv-aquarium.pass" --ks-key-alias aquarium \
  --out "$OUT/device-checks.apk" "$OUT/aligned.apk"
"$TOOLS/apksigner" verify "$OUT/device-checks.apk"
echo "Built separate device-check APK. This is not part of the application release."
