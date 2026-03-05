#!/bin/bash
set -euo pipefail

cd "$(dirname "$0")/android"

CONFIG="${1:-debug}"

echo "Building Airship Piano for Android ($CONFIG)..."

# Find adb
if command -v adb &>/dev/null; then
    ADB="adb"
elif [ -x "$HOME/Library/Android/sdk/platform-tools/adb" ]; then
    ADB="$HOME/Library/Android/sdk/platform-tools/adb"
else
    echo "Error: adb not found. Install Android SDK platform-tools."
    exit 1
fi

# Check for connected device
DEVICE_ID=$("$ADB" devices | grep -w "device" | head -1 | awk '{print $1}' || true)

if [ -z "$DEVICE_ID" ]; then
    echo "Error: No Android device found. Make sure your device is plugged in with USB debugging enabled."
    echo ""
    "$ADB" devices
    exit 1
fi

DEVICE_NAME=$("$ADB" -s "$DEVICE_ID" shell getprop ro.product.model 2>/dev/null || echo "$DEVICE_ID")
echo "Found device: $DEVICE_NAME ($DEVICE_ID)"

# Build
if [ "$CONFIG" = "release" ]; then
    ./gradlew :app:assembleRelease 2>&1 | tail -5
    APK_PATH="app/build/outputs/apk/release/app-release.apk"
else
    ./gradlew :app:assembleDebug 2>&1 | tail -5
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
fi

if [ ! -f "$APK_PATH" ]; then
    echo "Error: APK not found at $APK_PATH"
    exit 1
fi

echo ""
echo "Installing to $DEVICE_NAME..."
"$ADB" -s "$DEVICE_ID" install -r "$APK_PATH"

echo ""
echo "Launching Airship Piano..."
"$ADB" -s "$DEVICE_ID" shell am start -n com.windupairships.airshippiano/.MainActivity

echo ""
echo "Done! Airship Piano is running on $DEVICE_NAME."
