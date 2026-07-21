#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

usage() {
  echo "Usage: pnpm connect list | pnpm connect <serial-id>" >&2
}

resolve_sdk() {
  if [ -n "${ANDROID_HOME:-}" ]; then
    printf '%s\n' "$ANDROID_HOME"
    return
  fi

  if [ -f "$ROOT_DIR/local.properties" ]; then
    local configured
    configured="$(sed -n 's/^sdk\.dir=//p' "$ROOT_DIR/local.properties" | tail -n 1)"
    if [ -n "$configured" ]; then
      printf '%s\n' "$configured"
      return
    fi
  fi

  printf '%s\n' "$HOME/Library/Android/sdk"
}

if [ "$#" -ne 1 ]; then
  usage
  exit 2
fi

ADB="${ANDROID_GAMES_ADB:-$(resolve_sdk)/platform-tools/adb}"
if [ ! -x "$ADB" ]; then
  echo "Missing required Android SDK tool: $ADB" >&2
  exit 1
fi

argument="$1"
if [ "$argument" = "list" ]; then
  exec "$ADB" devices -l
fi

devices="$("$ADB" devices)"
state="$(printf '%s\n' "$devices" | awk -v serial="$argument" '$1 == serial { print $2; exit }')"

if [ -z "$state" ]; then
  echo "Unknown ADB serial: $argument" >&2
  exit 1
fi

case "$state" in
  device)
    ;;
  unauthorized)
    echo "ADB device '$argument' is unauthorized. Unlock it and accept the USB debugging RSA prompt." >&2
    exit 1
    ;;
  offline)
    echo "ADB device '$argument' is offline. Reconnect USB or restart the ADB server." >&2
    exit 1
    ;;
  *)
    echo "ADB device '$argument' is not ready (state: $state)." >&2
    exit 1
    ;;
esac

"$ADB" -s "$argument" wait-for-device
verified_state="$("$ADB" -s "$argument" get-state | tr -d '\r')"
if [ "$verified_state" != "device" ]; then
  echo "ADB device '$argument' did not become ready (state: $verified_state)." >&2
  exit 1
fi

model="$("$ADB" -s "$argument" shell getprop ro.product.model | tr -d '\r')"
android_release="$("$ADB" -s "$argument" shell getprop ro.build.version.release | tr -d '\r')"
printf 'Connected ADB device:\n  Serial: %s\n  Model: %s\n  Android: %s\n' \
  "$argument" "$model" "$android_release"
