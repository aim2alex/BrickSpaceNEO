#!/bin/zsh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
APP_DIR="$ROOT/app/src/main"
BUILD_DIR="$ROOT/build"
RES_DIR="$BUILD_DIR/res"
GEN_DIR="$BUILD_DIR/generated"
CLS_DIR="$BUILD_DIR/classes"
DEX_DIR="$BUILD_DIR/dex"
APK_DIR="$BUILD_DIR/apk"
OUTPUT_APK="$ROOT/BrickSpaceNeo.apk"
ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
PLATFORM="$ANDROID_HOME/platforms/android-36/android.jar"
TOOLS="$ANDROID_HOME/build-tools/36.1.0"
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

mkdir -p "$RES_DIR" "$GEN_DIR" "$CLS_DIR" "$DEX_DIR" "$APK_DIR"

"$TOOLS/aapt2" compile --dir "$APP_DIR/res" -o "$RES_DIR/compiled.zip"
"$TOOLS/aapt2" link \
  -o "$APK_DIR/unsigned.apk" \
  -I "$PLATFORM" \
  --manifest "$APP_DIR/AndroidManifest.xml" \
  --java "$GEN_DIR" \
  --min-sdk-version 24 \
  --target-sdk-version 36 \
  "$RES_DIR/compiled.zip"

find "$APP_DIR/java" "$GEN_DIR" -name '*.java' > "$BUILD_DIR/sources.list"

"$JAVA_HOME/bin/javac" \
  -source 17 \
  -target 17 \
  -classpath "$PLATFORM" \
  -d "$CLS_DIR" \
  @"$BUILD_DIR/sources.list"

"$TOOLS/d8" \
  --lib "$PLATFORM" \
  --output "$DEX_DIR" \
  $(find "$CLS_DIR" -name '*.class')

cd "$DEX_DIR"
zip -q -r "$APK_DIR/unsigned.apk" classes.dex
cd "$ROOT"

"$TOOLS/zipalign" -f 4 "$APK_DIR/unsigned.apk" "$APK_DIR/aligned.apk"
"$TOOLS/apksigner" sign \
  --ks "$HOME/.android/debug.keystore" \
  --ks-pass pass:android \
  --key-pass pass:android \
  --out "$OUTPUT_APK" \
  "$APK_DIR/aligned.apk"

"$TOOLS/apksigner" verify "$OUTPUT_APK"
echo "APK ready: $OUTPUT_APK"
