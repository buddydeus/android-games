#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONNECT_SCRIPT="$ROOT_DIR/scripts/connect-android-device.sh"
MODE="direct"

if [ "${1:-}" = "--pnpm" ]; then
  MODE="pnpm"
  shift
fi

if [ "$#" -ne 0 ]; then
  echo "Usage: bash scripts/test-connect-android-device.sh [--pnpm]" >&2
  exit 2
fi

TEMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TEMP_DIR"' EXIT

FAKE_ADB="$TEMP_DIR/adb"
FAKE_ADB_LOG="$TEMP_DIR/adb.log"
export FAKE_ADB_LOG

cat >"$FAKE_ADB" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

printf '%s\n' "$*" >>"$FAKE_ADB_LOG"

case "$*" in
  "devices -l")
    printf 'List of devices attached\n'
    printf 'USB123\tdevice product:tablet model:Pixel_Tablet transport_id:1\n'
    printf 'LOCKED456\tunauthorized usb:2-1 transport_id:2\n'
    printf 'OFFLINE789\toffline usb:2-2 transport_id:3\n'
    printf 'RECOVERY999\trecovery usb:2-3 transport_id:4\n'
    printf 'FLAKY321\tdevice product:tablet model:Flaky_Tablet transport_id:5\n'
    ;;
  "devices")
    printf 'List of devices attached\n'
    printf 'USB123\tdevice\n'
    printf 'LOCKED456\tunauthorized\n'
    printf 'OFFLINE789\toffline\n'
    printf 'RECOVERY999\trecovery\n'
    printf 'FLAKY321\tdevice\n'
    ;;
  "-s USB123 wait-for-device")
    ;;
  "-s USB123 get-state")
    printf 'device\n'
    ;;
  "-s USB123 shell getprop ro.product.model")
    printf 'Pixel Tablet\r\n'
    ;;
  "-s USB123 shell getprop ro.build.version.release")
    printf '16\r\n'
    ;;
  "-s FLAKY321 wait-for-device")
    ;;
  "-s FLAKY321 get-state")
    printf 'offline\n'
    ;;
  *)
    echo "Unexpected fake adb arguments: $*" >&2
    exit 64
    ;;
esac
EOF
chmod +x "$FAKE_ADB"

run_connect() {
  if [ "$MODE" = "pnpm" ]; then
    ANDROID_GAMES_ADB="$FAKE_ADB" pnpm --dir "$ROOT_DIR" connect "$@"
  else
    ANDROID_GAMES_ADB="$FAKE_ADB" bash "$CONNECT_SCRIPT" "$@"
  fi
}

fail() {
  echo "FAIL: $*" >&2
  exit 1
}

assert_contains() {
  local actual="$1"
  local expected="$2"
  if [[ "$actual" != *"$expected"* ]]; then
    fail "expected output to contain '$expected', got: $actual"
  fi
}

assert_failure_contains() {
  local expected="$1"
  shift

  local output
  local status
  set +e
  output="$(run_connect "$@" 2>&1)"
  status=$?
  set -e

  if [ "$status" -eq 0 ]; then
    fail "expected command to fail: $*"
  fi
  assert_contains "$output" "$expected"
}

list_output="$(run_connect list)"
assert_contains "$list_output" $'USB123\tdevice product:tablet model:Pixel_Tablet'
assert_contains "$list_output" $'LOCKED456\tunauthorized'
assert_contains "$list_output" $'OFFLINE789\toffline'

connected_output="$(run_connect USB123)"
assert_contains "$connected_output" "Connected ADB device:"
assert_contains "$connected_output" "Serial: USB123"
assert_contains "$connected_output" "Model: Pixel Tablet"
assert_contains "$connected_output" "Android: 16"

assert_failure_contains "Unknown ADB serial: MISSING" MISSING
assert_failure_contains "unauthorized" LOCKED456
assert_failure_contains "offline" OFFLINE789
assert_failure_contains "not ready (state: recovery)" RECOVERY999
assert_failure_contains "did not become ready (state: offline)" FLAKY321
assert_failure_contains "Usage:"
assert_failure_contains "Usage:" USB123 extra

set +e
missing_adb_output="$(ANDROID_GAMES_ADB="$TEMP_DIR/missing-adb" bash "$CONNECT_SCRIPT" list 2>&1)"
missing_adb_status=$?
set -e
if [ "$missing_adb_status" -eq 0 ]; then
  fail "expected a missing ADB executable to fail"
fi
assert_contains "$missing_adb_output" "Missing required Android SDK tool:"

if grep -Eq '^connect( |$)' "$FAKE_ADB_LOG"; then
  fail "USB device selection must not invoke adb connect"
fi

echo "connect-android-device tests passed ($MODE)"
