# USB ADB Connect Script Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `pnpm connect list` and `pnpm connect <serial-id>` for listing USB ADB transports and verifying one exact device serial.

**Architecture:** A focused Bash command resolves the repository's ADB executable, parses `adb devices` for an exact serial and state, and queries device properties only after the transport is online. A self-contained shell regression test injects a fake ADB executable so every state is deterministic and no physical device is required.

**Tech Stack:** Bash, ADB CLI, pnpm package scripts, Gradle repository verification

## Global Constraints

- USB selection must use `adb -s <serial-id>` and must not call the TCP/IP-only `adb connect` command.
- `list` must preserve every state returned by `adb devices -l`, including `unauthorized` and `offline`.
- Serial matching must be exact.
- The command must add no npm, Gradle, Android, or runtime dependency.
- Every repository file change must be reflected in `AGENTS.md`.
- Run `npm run verify` before claiming the repository change complete.
- Create a scoped local commit after verification; do not push.

---

### Task 1: Host-side USB ADB command and regression tests

**Files:**
- Create: `scripts/connect-android-device.sh`
- Create: `scripts/test-connect-android-device.sh`

**Interfaces:**
- Consumes: optional `ANDROID_GAMES_ADB` executable override for deterministic tests; otherwise `ANDROID_HOME`, repository `local.properties`, or `$HOME/Library/Android/sdk`
- Produces: `bash scripts/connect-android-device.sh list` and `bash scripts/connect-android-device.sh <serial-id>` with zero status only for a successful list or online-device verification

- [ ] **Step 1: Write the failing shell regression test**

Create `scripts/test-connect-android-device.sh` with a temporary fake ADB executable. Accept an optional `--pnpm` argument that makes `run_connect` invoke `pnpm connect`; without it, invoke `bash scripts/connect-android-device.sh`. Its cases must assert:

```bash
list_output="$(run_connect list)"
assert_contains "$list_output" $'USB123\tdevice product:tablet model:Pixel_Tablet'
assert_contains "$list_output" $'LOCKED456\tunauthorized'

connected_output="$(run_connect USB123)"
assert_contains "$connected_output" "Connected ADB device:"
assert_contains "$connected_output" "Serial: USB123"
assert_contains "$connected_output" "Model: Pixel Tablet"
assert_contains "$connected_output" "Android: 16"

assert_failure_contains "Unknown ADB serial: MISSING" MISSING
FAKE_ADB_SCENARIO=unauthorized assert_failure_contains \
  "unauthorized" LOCKED456
assert_failure_contains "Usage:"
```

The fake executable must support `devices`, `devices -l`, `-s USB123 wait-for-device`, `-s USB123 get-state`, and both `getprop` queries. It must append received arguments to a log, and the test must reject any invocation whose first argument is `connect`.

- [ ] **Step 2: Run the regression test and verify RED**

Run: `bash scripts/test-connect-android-device.sh`

Expected: non-zero exit because `scripts/connect-android-device.sh` does not exist.

- [ ] **Step 3: Implement minimal argument, SDK, and ADB resolution**

Create `scripts/connect-android-device.sh` beginning with:

```bash
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
```

- [ ] **Step 4: Implement list and exact-serial state handling**

Add behavior equivalent to:

```bash
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
  device) ;;
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
```

- [ ] **Step 5: Run the shell regression test and verify GREEN**

Run: `bash scripts/test-connect-android-device.sh`

Expected: exit 0 with `connect-android-device tests passed` and no fake `adb connect` invocation.

---

### Task 2: Package command and user documentation

**Files:**
- Modify: `package.json`
- Modify: `README.md`
- Modify: `AGENTS.md`

**Interfaces:**
- Consumes: `scripts/connect-android-device.sh` from Task 1
- Produces: `pnpm connect list` and `pnpm connect <serial-id>` from the repository root

- [ ] **Step 1: Add a failing package-script assertion**

Run before modifying `package.json`:

```bash
node -e 'const scripts=require("./package.json").scripts; if (scripts.connect !== "bash scripts/connect-android-device.sh") process.exit(1)'
```

Expected: exit 1 because `scripts.connect` is absent.

- [ ] **Step 2: Register the package script**

Add to `package.json`:

```json
"connect": "bash scripts/connect-android-device.sh",
```

- [ ] **Step 3: Verify pnpm forwards arguments**

Run the same deterministic suite through the registered package script:

```bash
bash scripts/test-connect-android-device.sh --pnpm
```

Expected: exit 0 after the suite observes `USB123` in list output and the connected serial, model, and Android release in selection output.

- [ ] **Step 4: Document usage and repository commands**

Add a `连接 USB 设备` subsection to `README.md` containing:

```bash
pnpm connect list
pnpm connect <serial-id>
```

Explain that USB ADB connects automatically, the serial command selects and verifies one device, and `unauthorized` requires accepting the device RSA prompt. Update `AGENTS.md` so its Commands, Structure, and Verification sections include the connect script and `bash scripts/test-connect-android-device.sh`.

- [ ] **Step 5: Run focused verification**

Run:

```bash
bash scripts/test-connect-android-device.sh
bash scripts/test-connect-android-device.sh --pnpm
node -e 'const scripts=require("./package.json").scripts; if (scripts.connect !== "bash scripts/connect-android-device.sh") process.exit(1)'
git diff --check
```

Expected: all commands exit 0.

---

### Task 3: Repository verification and scoped commit

**Files:**
- Verify all files changed in Tasks 1 and 2

**Interfaces:**
- Consumes: completed connect script, regression test, package command, and documentation
- Produces: a verified local commit without pushing

- [ ] **Step 1: Run the full repository gate**

Run: `npm run verify`

Expected: Gradle exits 0 after unit tests, four validated game packages, and debug APK asset validation.

- [ ] **Step 2: Inspect final scope**

Run:

```bash
git status --short
git diff --check
git diff -- package.json scripts/connect-android-device.sh scripts/test-connect-android-device.sh README.md AGENTS.md
```

Expected: only the planned host tooling and documentation changes remain; no APK, build directory, local properties, credentials, or unrelated files are included.

- [ ] **Step 3: Commit the verified implementation**

Run:

```bash
git add package.json scripts/connect-android-device.sh scripts/test-connect-android-device.sh README.md AGENTS.md docs/superpowers/plans/2026-07-21-usb-adb-connect-script.md
git commit -m "$(cat <<'EOF'
feat: add USB ADB device connector

- List USB transports and verify exact device serials
- Cover host connection states and document pnpm usage
EOF
)"
```

Expected: commit succeeds without bypassing hooks; `git status` is clean afterward.
