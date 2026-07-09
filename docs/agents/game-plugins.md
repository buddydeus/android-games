# Game Plugins

Load when editing `games/*`, `game-api/`, game manifests, or root `package*Game` Gradle tasks.

## Contract (SSOT: `game-api`)

Shell API version: `CURRENT_SHELL_API` (currently `1`).

Each game implements `GamePlugin`:

- `getManifest(): GameManifest`
- `@Composable MainScreen(context: GameContext)` — Single Player / Two Players / Exit
- `@Composable GameScreen(context: GameContext, mode: GameMode)` — board, status, restart, return

`GameContext` callbacks: `startGame`, `exitGame`, `returnToGameMain`, `log`.

Manifest validation lives in `GameManifest.isValidForShell()`. Required fields: `gameId` (lowercase slug), `displayName`, `versionCode`, `versionName`, `entryClass`, `minShellApi <= CURRENT_SHELL_API`, `orientation = "landscape"`.

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

Swap `gomoku` for `othello` or `xiangqi`.

## Adding a new game (checklist)

Ask first — touches Gradle modules and shell built-in asset copy.

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
