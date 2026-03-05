#!/bin/bash
set -euo pipefail

cd "$(dirname "$0")"

PROPS_FILE="$HOME/home/airship-piano-android.properties"
if [ ! -f "$PROPS_FILE" ]; then
    echo "Error: $PROPS_FILE not found"
    echo "Create it with: storeFile, storePassword, keyAlias, keyPassword"
    exit 1
fi

echo "Building release AAB..."
./gradlew :app:bundleRelease

AAB_PATH="app/build/outputs/bundle/release/app-release.aab"
if [ ! -f "$AAB_PATH" ]; then
    echo "Error: AAB not found at $AAB_PATH"
    exit 1
fi

echo ""
echo "Build complete: $AAB_PATH"
echo "Upload to Google Play Console: https://play.google.com/console"
