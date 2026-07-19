# Game Plugins

Load when editing `games/*`, `game-api/`, game manifests, or root `package*Game` Gradle tasks.

## Contract (SSOT: `game-api`)

Shell API version: `CURRENT_SHELL_API` (currently `1`).

Each game implements `GamePlugin`:

- `getManifest(): GameManifest`
- `@Composable MainScreen(context: GameContext)` — Single Player / Two Players / Exit
- `@Composable GameScreen(context: GameContext, mode: GameMode)` — board, status, restart, return

`GameContext` callbacks: `startGame`, `exitGame`, `returnToGameMain`, `log`.

Manifest validation lives in `GameManifest.isValidForShell()`. Required fields: `gameId` (lowercase slug), `displayName`, `versionCode`, `versionName`, `entryClass`, `minShellApi <= CURRENT_SHELL_API`, `orientation = "landscape"`. The shell displays `displayName` directly and does not map known game IDs to names.

`icon` is an optional package-relative path. The home screen supports PNG, WebP, JPEG, and UTF-8 text files; text content is rendered as a compact mark of up to six characters inside the shared logo well. Bitmap decoding is sampled to a bounded display size. Missing, empty, unsupported, or unreadable icons fall back to the first character of `displayName`. Keep all icon paths inside the game package and never use absolute paths or `..`.

## Game versions

Each game has an independent version beginning at `versionCode = 1` and `versionName = "0.0.1"`. `MainScreen` must display the plugin manifest's `versionName`.

For every update to a game's rules, robot, UI, or package assets:

1. Increment that game's integer `versionCode`.
2. Increment its semantic `versionName`.
3. Keep the values in the `*Plugin.manifest` and `games/<name>/package/manifest.json` identical.
4. Update the game's version regression test and build the game package.

Shell-only changes do not increment game versions. Never reuse an older `versionCode`; the package repository rejects downgrades.

## Package layout

Each game module has a source tree plus a static package directory:

```text
games/<name>/
  src/main/java/.../<Name>Plugin.kt   # implements GamePlugin, public no-arg ctor
  src/test/java/.../*RulesTest.kt     # rules unit tests
  package/
    manifest.json                     # copied into zip root
    assets/                           # optional static assets (icon, etc.)
```

Built zip contents (produced by Gradle, not hand-edited):

```text
<gameId>.zip
  manifest.json
  assets/...
  plugin.apk                          # dex output from game module classes.jar
```

Installed on device under: `<filesDir>/Games/<gameId>/` (see `GamePackageRepository`).

The shell discovers every valid installed directory without a per-game registry. Home order is based on the persisted successful-launch count, descending, with `displayName` and `gameId` as deterministic tie breakers. A newly imported game therefore appears automatically and starts with count zero.

## Build pipeline

Root `build.gradle.kts` registers per-game tasks:

1. `:games:<name>:bundleLibRuntimeToJarDebug` → `classes.jar`
2. `dex<Name>Game` — `d8 --min-api 26` → `build/game-plugin-dex/<name>/`
3. `assemble<Name>PluginApk` — zips dex as `plugin.apk`
4. `package<Name>Game` — merges `games/<name>/package/` + `plugin.apk` → `build/game-packages/<name>.zip`

`app` copies `build/game-packages/*.zip` into generated assets (`builtin-games/`) before `mergeDebugAssets`.

## Commands (single game)

From repo root:

- `./gradlew packageGomokuGame` — build gomoku zip only
- `./gradlew :games:gomoku:testDebugUnitTest` — gomoku rules tests only
- `./gradlew :games:gomoku:assembleDebug` — compile game library (no zip)
- `./gradlew packageChessGame` — build international chess zip only
- `./gradlew :games:chess:testDebugUnitTest` — international chess rules, session, and AI tests

Swap `gomoku` for `othello`, `xiangqi`, or `chess`.

## Adding a new built-in game module (checklist)

This checklist is only for adding game source and bundling its zip into this repository's APK. A compatible zip built elsewhere can be imported and discovered without changing or rebuilding the shell.

1. Create `games/<gameId>/` Android library module (Compose + `game-api` dependency).
2. Add `*Plugin` class implementing `GamePlugin` with public no-arg constructor.
3. Add `package/manifest.json` with matching `entryClass`.
4. Register module in `settings.gradle.kts`.
5. Call `registerGamePackageTask("package<Name>Game", "<gameId>")` in root `build.gradle.kts`.
6. Add `copyBuiltinGamePackages` dependency and `npm run build:game:<gameId>` script in `package.json`.
7. Add rules tests under `src/test/`; run `npm run verify`.

## Runtime loading

`DexGamePluginLoader` loads `plugin.apk` via `DexClassLoader`, instantiates `manifest.entryClass`, casts to `GamePlugin`. Failures mean wrong class name, missing no-arg ctor, or dex/API mismatch.

## Do not

- Put shell code (`app/`) inside game modules.
- Share game-specific rules across modules (no shared rules engine in MVP).
- Use absolute paths or `..` in manifest `icon` fields.
- Downgrade `versionCode` on reinstall (repository rejects downgrades).
