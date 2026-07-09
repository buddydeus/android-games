#!/usr/bin/env bash
set -euo pipefail

SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$SDK/platform-tools/adb"
EMULATOR="$SDK/emulator/emulator"
AVDMANAGER="$SDK/cmdline-tools/latest/bin/avdmanager"
AVD_NAME="${ANDROID_GAMES_AVD:-android_games_mvp_pad}"
APP_ID="com.buddygames.center"
MAIN_ACTIVITY="$APP_ID/.MainActivity"
APK="app/build/outputs/apk/debug/app-debug.apk"
LOG_DIR="build/logs"
EMULATOR_LOG="$LOG_DIR/emulator-$AVD_NAME.log"
EMULATOR_PID=""

mkdir -p "$LOG_DIR"

require_tool() {
  if [ ! -x "$1" ]; then
    echo "Missing required Android SDK tool: $1" >&2
    exit 1
  fi
}

device_online() {
  "$ADB" devices | awk 'NR > 1 && $2 == "device" { found = 1 } END { exit found ? 0 : 1 }'
}

ensure_avd_exists() {
  if "$AVDMANAGER" list avd | grep -q "Name: $AVD_NAME"; then
    return
  fi

  local image="$SDK/system-images/android-36/google_apis/x86_64"
  if [ ! -d "$image" ]; then
    echo "AVD '$AVD_NAME' does not exist and Android 36 x86_64 system image is not installed." >&2
    echo "Install it with: $SDK/cmdline-tools/latest/bin/sdkmanager 'system-images;android-36;google_apis;x86_64'" >&2
    exit 1
  fi

  echo "Creating AVD '$AVD_NAME'..."
  echo no | "$AVDMANAGER" create avd \
    -n "$AVD_NAME" \
    -k "system-images;android-36;google_apis;x86_64" \
    -d pixel_c
}

start_emulator_if_needed() {
  if device_online; then
    return
  fi

  ensure_avd_exists

  echo "Starting emulator '$AVD_NAME'..."
  local emulator_args=(-avd "$AVD_NAME" -netdelay none -netspeed full)
  if [ "${HEADLESS:-0}" = "1" ]; then
    emulator_args+=(-no-window -no-audio -gpu swiftshader_indirect)
  fi

  nohup "$EMULATOR" "${emulator_args[@]}" >"$EMULATOR_LOG" 2>&1 &
  EMULATOR_PID="$!"
}

wait_for_boot() {
  "$ADB" wait-for-device
  for _ in $(seq 1 120); do
    local boot_completed
    boot_completed="$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
    if [ "$boot_completed" = "1" ]; then
      return
    fi
    sleep 2
  done

  echo "Timed out waiting for emulator boot. Last emulator log lines:" >&2
  tail -80 "$EMULATOR_LOG" >&2 || true
  exit 1
}

require_tool "$ADB"
require_tool "$EMULATOR"
require_tool "$AVDMANAGER"

start_emulator_if_needed
wait_for_boot

./gradlew :app:assembleDebug
"$ADB" install -r "$APK"
"$ADB" shell am start -n "$MAIN_ACTIVITY"

echo "Started $MAIN_ACTIVITY on:"
"$ADB" devices -l

if [ -n "$EMULATOR_PID" ] && [ "${DETACH_EMULATOR:-0}" != "1" ]; then
  echo "Emulator '$AVD_NAME' is running for debugging. Press Ctrl-C to stop it."
  trap '"$ADB" emu kill >/dev/null 2>&1 || true' INT TERM EXIT
  wait "$EMULATOR_PID"
fi
