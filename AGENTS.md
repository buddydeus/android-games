# Android Games — Agent Instructions

For humans: start with [README.md](README.md). The approved product and architecture baseline is the [design spec](docs/superpowers/specs/2026-07-07-android-pad-game-center-design.md).

## Project

Offline Android Pad game center (Kotlin + Jetpack Compose). The `app` module is a stable loading shell; each game ships as a zip package with dex plugin code loaded via `DexClassLoader`. MVP games: Gomoku, Othello, Xiangqi.

Gradle multi-module layout: `app` (shell), `game-api` (shell↔game contract), `games/*` (per-game modules + package assets).

Current game behavior:

- All game menus use `单人模式`, `双人对战`, and `退出游戏`.
- All games own their rules, robot, UI, score state, and restart flow inside their game module.
- Gomoku uses a 15×15 intersection board. Its robot priority is: win immediately, block an immediate five (including closed four), block moves that create at least two immediate winning points (continuous or broken open three), then use the positional fallback.
- Othello hides robot hint points in two-player mode.
- Xiangqi uses intersection placement, colors the active side in the turn display, and shows `将军` in the side panel when applicable.

## Environment

- **JDK** — required by Android Gradle Plugin 9.2.1
- **Android SDK** — API 36; build-tools **36.0.0** (used by root `d8` dex step)
- **SDK path** — `ANDROID_HOME`, or `local.properties` with `sdk.dir=...` (gitignored; create locally)
- **Emulator** — `scripts/start-android-debug.sh` defaults to AVD `android_games_mvp_pad` (override: `ANDROID_GAMES_AVD`)
- **Node** — optional; `package.json` wraps Gradle for convenience (no npm dependencies)

## Commands

Run from repository root:

- `npm run test` — all unit tests (`./gradlew test`)
- `npm run verify` — full MVP gate: tests + three game packages + debug APK
- `npm run build` — build debug APK and all game package zips
- `npm run build:apk` — `./gradlew :app:assembleDebug` (also copies built-in game zips into assets)
- `npm run build:game` — build all three game package zips
- `npm run build:game:gomoku` — `./gradlew packageGomokuGame`
- `npm run build:game:othello` — `./gradlew packageOthelloGame`
- `npm run build:game:xiangqi` — `./gradlew packageXiangqiGame`
- `npm start` — boot emulator (if needed), build APK, install, launch `com.buddygames.center/.MainActivity`
- `./gradlew :game-api:testDebugUnitTest` — game-api manifest/contract tests only
- `./gradlew :app:testDebugUnitTest` — shell runtime tests only
- `./gradlew :games:gomoku:testDebugUnitTest` — single game rules tests (swap module name as needed)

`pnpm run <script>` works the same; lockfile has no runtime deps.

## Structure

| Path | Role |
| ---- | ---- |
| `app/` | Game center shell UI, package install/discovery, dex plugin loader |
| `game-api/` | `GamePlugin`, `GameContext`, `GameManifest`, `CURRENT_SHELL_API` |
| `games/gomoku/` | Gomoku plugin module + `package/` assets/manifest |
| `games/othello/` | Othello plugin module + package layout |
| `games/xiangqi/` | Xiangqi plugin module + package layout |
| `build.gradle.kts` | Registers `package*Game` zip tasks (jar → d8 → plugin.apk → zip) |
| `scripts/start-android-debug.sh` | Local emulator + install + launch |
| `docs/superpowers/specs/` | Approved product/architecture spec (SSOT) |
| `docs/superpowers/plans/` | MVP implementation plan |
| `docs/agents/game-plugins.md` | Plugin contract, packaging, adding games |

## Boundaries

### Always do

- Run `npm run verify` before claiming work complete (unless change is docs-only).
- Scope game logic/UI to the relevant `games/<name>/` module.
- Keep robot strategy and its regression tests in the same game module; threat-priority changes must include deterministic board-state tests.
- Keep `game-api` backward-compatible or update every `games/*` plugin in the same change.
- Run targeted unit tests for touched modules (see Commands).
- Match existing Kotlin + Compose style in neighboring files.

### Ask first

- Change `game-api` public types or bump `CURRENT_SHELL_API`.
- Modify `DexGamePluginLoader`, `GamePackageRepository`, or root packaging tasks in `build.gradle.kts`.
- Add/remove Gradle modules or Android dependencies.
- Delete files, add CI, or overwrite this file.

### Never do

- Commit `local.properties`, `build/`, `.gradle/`, `*.apk`, `.idea/`, or credentials.
- Run `git config` changes or `git push --force` to main/master.
- Introduce Play Dynamic Delivery, online update repos, or package signing (MVP non-goals).
- Invent scripts, env vars, or Gradle tasks not present in the repo.

## Verification

After code changes:

1. `./gradlew :<module>:testDebugUnitTest` for each touched module
2. `npm run verify` for integration-level confidence
3. Optional: `npm start` for on-device/emulator smoke test

Emulator logs: `build/logs/emulator-<AVD_NAME>.log`

## Known fixes

| Symptom | Fix |
| ------- | --- |
| SDK location not found | Create `local.properties`: `sdk.dir=/path/to/Android/sdk` or export `ANDROID_HOME` |
| `d8` / build-tools missing | Install SDK build-tools 36.0.0 via `sdkmanager "build-tools;36.0.0"` |
| `:app:assembleDebug` missing built-in games | Run `npm run build:game` first, or use `npm run build:apk` (depends on package tasks) |
| Emulator AVD missing | Run `npm start` (auto-creates if system image installed) or `sdkmanager "system-images;android-36;google_apis;x86_64"` |
| Plugin load error at runtime | Confirm `manifest.json` `entryClass` matches `*Plugin` class implementing `GamePlugin` |

## Document map

| Doc | Purpose |
| --- | ------- |
| [README.md](README.md) | Human setup, build, runtime, package format, and current game capabilities |
| [docs/superpowers/specs/2026-07-07-android-pad-game-center-design.md](docs/superpowers/specs/2026-07-07-android-pad-game-center-design.md) | Product scope, architecture, non-goals |
| [docs/superpowers/plans/2026-07-08-android-pad-game-center-mvp.md](docs/superpowers/plans/2026-07-08-android-pad-game-center-mvp.md) | MVP task breakdown and file map |
| [docs/agents/game-plugins.md](docs/agents/game-plugins.md) | GamePlugin contract, zip layout, adding a game |

## Done checklist

- [ ] Targeted module unit tests pass
- [ ] `npm run verify` passes
- [ ] No secrets or build artifacts staged
- [ ] If `game-api` changed, all three games still build and load
