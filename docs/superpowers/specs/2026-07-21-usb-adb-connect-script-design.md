# USB ADB Connect Script Design

## Goal

Provide a small repository command for discovering USB-connected Android devices and selecting one exact ADB serial for use. The supported commands are:

- `pnpm connect list` — list every device reported by ADB, including non-ready states.
- `pnpm connect <serial-id>` — verify that the named USB device is present, wait for it to become available, and confirm that it is online.

## Command boundary

Add `scripts/connect-android-device.sh` and expose it as the `connect` package script. The script resolves ADB with the same Android SDK rules as the existing debug launcher: `ANDROID_HOME`, then `local.properties`, then the standard macOS SDK location.

USB ADB transport is established by the ADB server when the device is discovered. Therefore the serial form selects the transport with `adb -s <serial-id>`; it must not call the network-only `adb connect` command.

## Behavior

`list` runs `adb devices -l` so the output includes the serial, connection state, model, product, and transport metadata that ADB provides. It preserves `unauthorized`, `offline`, and other states instead of hiding them.

For a serial argument, the script first reads the current device list and requires an exact serial match. It then handles the reported state:

- `device`: wait for the selected transport, verify `get-state`, and print its serial, model, and Android release.
- `unauthorized`: fail with guidance to unlock the device and accept the USB debugging RSA prompt.
- `offline`: fail with guidance to reconnect USB or restart ADB.
- any other state: fail and report the state without attempting an indefinite wait.

Missing arguments, extra arguments, an unknown serial, or a missing ADB executable return a non-zero status and print concise usage or recovery guidance.

## Testing and documentation

A shell regression test uses a temporary fake ADB executable to cover listing, successful selection, unknown serial, unauthorized state, and invalid invocation without requiring physical hardware. The fake executable is injected through a script-only test override so production SDK resolution remains unchanged.

README and `AGENTS.md` document both commands. The repository completion gate remains `npm run verify`; the shell regression test is run separately because it exercises host tooling rather than an Android module.
