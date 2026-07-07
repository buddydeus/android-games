# Android Pad Game Center MVP Design

Date: 2026-07-07
Status: Approved for written spec review

## Goal

Build an offline Android Pad game center using Kotlin and Jetpack Compose. The game center is a stable loading shell that should rarely need updates. Individual games are delivered and updated as zip-based game packages, so fixing one game can be done by replacing that game's package.

The MVP includes the shell framework and three game packages:

- Gomoku
- Othello
- Xiangqi

Each game package owns its game logic, UI, resources, and robot opponent behavior.

## Product Scope

The shell provides:

- A Pad-friendly main screen listing installed games.
- A local zip import action.
- Game package installation and update into internal app storage.
- Dynamic loading of game package code.
- A host surface for game-provided Compose screens.
- Navigation back to the game center.

Each game provides:

- A game main screen with `Single Player`, `Two Players`, and `Exit`.
- A game play screen with the board, turn/status display, restart, and return.
- Local two-player mode.
- Single-player mode with a usable MVP robot.

## Non-Goals

The MVP does not include:

- Google Play compliant dynamic delivery.
- Online package repository or automatic network updates.
- Package signing and trust-chain validation.
- Multiplayer over network.
- Save-game synchronization.
- A shared cross-game rules engine.

These can be added later without changing the core shell-to-plugin contract.

## Chosen Approach

Use zip packages containing Android plugin code loaded with `DexClassLoader`.

This is appropriate because distribution is private or sideloaded for now. It lets the shell remain mostly stable while game packages contain executable game logic and can be updated independently.

Alternatives considered:

- WebView HTML/JS packages: easier hot updates, but weaker native integration and a split UI/runtime model.
- Rules DSL packages: safer and more controlled, but too expensive for the first version because Gomoku, Othello, and Xiangqi have meaningfully different rule systems.

## Architecture

### 1. Game Center Shell

Native Android application built with Kotlin and Jetpack Compose.

Responsibilities:

- Render the game center home screen.
- Let users import a game package zip from local storage.
- Install or update game packages.
- Read installed package manifests.
- Load plugin code.
- Host plugin-provided Compose screens.
- Provide stable shell services through `GameContext`.

### 2. Package Runtime

The package runtime manages installed games under internal app storage:

```text
files/
  Games/
    gomoku/
      manifest.json
      plugin.apk
      assets/
    othello/
      manifest.json
      plugin.apk
      assets/
    xiangqi/
      manifest.json
      plugin.apk
      assets/
```

Responsibilities:

- Copy imported zip files to a temporary install location.
- Extract packages into `files/.installing/<gameId>-<timestamp>/`.
- Validate package structure and manifest fields.
- Atomically replace `files/Games/<gameId>/` after validation.
- Keep the previous installed version if an update fails.
- Discover installed games by scanning `files/Games/*/manifest.json`.

### 3. Plugin API

The shell and games communicate through a stable API. The exact Kotlin package can be finalized during implementation, but the contract is:

```kotlin
interface GamePlugin {
    fun getManifest(): GameManifest

    @Composable
    fun MainScreen(context: GameContext)

    @Composable
    fun GameScreen(context: GameContext, mode: GameMode)
}

enum class GameMode {
    SINGLE_PLAYER,
    TWO_PLAYERS
}
```

`GameContext` provides shell-owned capabilities:

- `gameId`
- package root directory
- assets directory
- logging
- `startGame(mode: GameMode)`
- `exitGame()`
- `returnToGameMain()`

Plugins should not depend on shell implementation details outside this API.

### 4. Game Packages

Each game package is independent and owns:

- Manifest metadata.
- Compose UI.
- Rule engine.
- Robot opponent.
- Local resources.
- Any package-specific data files.

The shell does not know chess rules, board layout rules, or robot strategies.

## Package Format

Each game package is a zip file with this top-level structure:

```text
manifest.json
plugin.apk
assets/
```

`manifest.json` MVP shape:

```json
{
  "schemaVersion": 1,
  "gameId": "gomoku",
  "displayName": "Gomoku",
  "versionCode": 1,
  "versionName": "1.0.0",
  "entryClass": "com.buddygames.gomoku.GomokuPlugin",
  "minShellApi": 1,
  "orientation": "landscape",
  "icon": "assets/icon.png"
}
```

Required validation:

- `schemaVersion` is supported.
- `gameId` is non-empty and uses a safe directory-name format.
- `displayName` is non-empty.
- `versionCode` is positive.
- `entryClass` is non-empty.
- `minShellApi <= current shell API`.
- `plugin.apk` exists.
- `icon`, when provided, points inside the package directory.

## Import And Update Flow

1. User selects a zip package in the game center.
2. Shell copies it to a temporary file.
3. Shell extracts it into `files/.installing/<gameId>-<timestamp>/`.
4. Shell reads and validates `manifest.json`.
5. Shell validates required files.
6. Shell compares the new package with any installed package sharing the same `gameId`.
7. Shell installs by replacing `files/Games/<gameId>/`.
8. Shell refreshes the game list.

Version behavior:

- Higher `versionCode`: allow update.
- Same `versionCode`: allow overwrite for MVP development convenience.
- Lower `versionCode`: reject by default.

Failure behavior:

- If validation fails, delete the temporary install directory and report the error.
- If replacement fails, keep the existing installed game.
- If loading fails after installation, show the game as installed but unavailable with an actionable error.

## Runtime Loading Flow

1. Shell scans installed package manifests.
2. Shell renders installed games on the home screen.
3. User selects a game.
4. Shell creates a `DexClassLoader` for that package's `plugin.apk`.
5. Shell loads `entryClass` by reflection.
6. Shell verifies the instance implements `GamePlugin`.
7. Shell hosts the plugin's `MainScreen`.
8. Plugin uses `GameContext.startGame(mode)` to enter `GameScreen`.
9. Plugin uses `GameContext.exitGame()` to return to the game center.

The MVP should load one active game at a time.

## Game Center UI

The first screen is the usable game center, not a marketing page.

Pad layout:

- Landscape-first layout.
- Top bar with title and import action.
- Installed games shown as large selectable tiles.
- Empty state with an import action.
- Error state for invalid packages or failed plugin loading.

Game tiles should show:

- Icon when available.
- Display name.
- Version name.
- Availability status.

## Game Package UI Contract

Each game implements two screens.

### Game Main Screen

Required controls:

- `Single Player`
- `Two Players`
- `Exit`

`Single Player` starts local player versus robot.
`Two Players` starts local same-device play.
`Exit` returns to the game center.

### Game Play Screen

Required elements:

- Main game board.
- Current turn.
- Game result or status.
- Restart.
- Return.

The plugin owns all game-specific UI details.

## Gomoku MVP

- Board: 15 by 15.
- Players: black and white.
- Rule: horizontal, vertical, or diagonal five-in-a-row wins.
- Forbidden-hand rules are out of scope for MVP.
- Single player: human first, robot second.

Robot strategy:

1. Play a winning move if available.
2. Block the human's immediate winning move if available.
3. Prefer empty positions near existing stones.
4. Otherwise choose any legal empty position.

## Othello MVP

- Board: 8 by 8.
- Standard four-piece initial state.
- A legal move must flip at least one opponent piece.
- If a player has no legal move, the turn passes.
- If both players have no legal move, the game ends.
- Winner is the side with more pieces; equal count is a draw.
- Single player: human first, robot second.

Robot strategy:

1. Prefer corners.
2. Prefer the move that flips the most pieces.
3. Otherwise choose any legal move.

## Xiangqi MVP

- Board: 9 by 10.
- Implements standard movement and capture for:
  - Rook
  - Horse
  - Cannon
  - Elephant
  - Advisor
  - General
  - Soldier
- Victory: capturing the opposing general wins.
- Full check/checkmate enforcement is out of scope for MVP.
- Full flying-general illegal-state filtering is out of scope for MVP.
- Single player: human first, robot second.

Robot strategy:

1. Capture the opposing general if possible.
2. Capture the highest-value available piece.
3. Otherwise choose any legal move.

Future Xiangqi package updates can add stricter rule enforcement without shell changes.

## Testing Strategy

Use test-first development for production behavior.

Shell tests:

- Manifest parsing accepts valid package metadata.
- Manifest validation rejects missing required fields.
- Package import rejects unsafe `gameId` values.
- Package import rejects unsupported `minShellApi`.
- Package update rejects lower `versionCode`.
- Same `versionCode` overwrite is allowed.
- Failed install keeps the previous package.
- Plugin loader rejects classes that do not implement `GamePlugin`.

Game package tests:

- Gomoku detects wins in all four directions.
- Gomoku robot wins immediately when possible.
- Gomoku robot blocks immediate human wins.
- Othello computes legal moves from the initial position.
- Othello flips pieces correctly.
- Othello passes turns when a side has no legal move.
- Xiangqi validates each piece's basic movement.
- Xiangqi cannon capture requires exactly one screen piece.
- Xiangqi robot prioritizes capturing the general.

Manual verification:

- Import each MVP game package from zip.
- Launch each game from the game center.
- Use all three game main-screen buttons.
- Complete at least one two-player game path per game.
- Complete at least one single-player robot move per game.
- Update one game package without affecting the other two.

## Implementation Notes

The implementation should start with the shared plugin API and package manifest model, then package import tests, then plugin loading, then the shell UI, then the three game packages.

For the first project scaffold, use a multi-module Gradle structure so the shell and packages can share the API while still producing independent plugin artifacts:

```text
android-games/
  settings.gradle.kts
  build.gradle.kts
  app/
  game-api/
  games/
    gomoku/
    othello/
    xiangqi/
```

Expected artifacts:

- Shell APK from `app`.
- Plugin APKs from each game module.
- Zip packaging tasks for `gomoku`, `othello`, and `xiangqi`.

## Implementation Defaults

- Use `plugin.apk` as the plugin artifact inside each game zip.
- Add Gradle zip tasks named `packageGomokuGame`, `packageOthelloGame`, and `packageXiangqiGame`.
- Include no bundled packages initially; validate the import path as the core product behavior.
