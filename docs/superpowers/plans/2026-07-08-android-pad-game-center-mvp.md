# Android Pad Game Center MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a runnable Kotlin + Jetpack Compose Android Pad MVP with a game center, zip-style game packages installed into independent game directories, and playable Gomoku, Othello, and Xiangqi games.

**Architecture:** The project is a Gradle multi-module Android application. `app` is the game center shell. `game-api` defines the shell-to-game contract. `games/*` contain each game's logic, UI, manifest, and package assets, and Gradle creates per-game package directories and zip files.

**Tech Stack:** Gradle 9.6.1, Android Gradle Plugin 9.2.1, Kotlin 2.4.0, Jetpack Compose BOM 2026.06.01, Activity Compose 1.13.0, Android SDK `android-36.1`.

## Global Constraints

- Use Kotlin and Jetpack Compose.
- The shell is a stable loading host.
- Game packages are zip-based and install under internal `Games/<gameId>/` directories.
- Each game package owns its logic, UI, resources, and robot behavior.
- MVP includes Gomoku, Othello, and Xiangqi.
- Each game has a main screen with `Single Player`, `Two Players`, and `Exit`.
- Each game has a gameplay screen.
- Use test-first development for rules and package-runtime behavior where practical.
- Do not ask the user for more choices during implementation.

---

## File Structure

- `settings.gradle.kts`: project module list and plugin repositories.
- `build.gradle.kts`: shared plugin versions.
- `gradle.properties`: AndroidX, Kotlin, and build defaults.
- `local.properties`: local Android SDK path.
- `app/`: Android shell application.
- `app/src/main/java/com/buddygames/center/MainActivity.kt`: Compose entry point.
- `app/src/main/java/com/buddygames/center/package/GamePackageRepository.kt`: install/discover package directories.
- `app/src/main/java/com/buddygames/center/registry/BuiltInGameRegistry.kt`: MVP registry connecting installed package manifests to local game factories.
- `game-api/`: shared API and manifest models.
- `games/gomoku/`: Gomoku package module.
- `games/othello/`: Othello package module.
- `games/xiangqi/`: Xiangqi package module.

### Task 1: Project Scaffold And Shared API

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `local.properties`
- Create: `game-api/build.gradle.kts`
- Create: `game-api/src/main/java/com/buddygames/api/GameApi.kt`
- Create: `game-api/src/test/java/com/buddygames/api/GameManifestTest.kt`

**Interfaces:**
- Produces: `GamePlugin`, `GameContext`, `GameMode`, `GameManifest`, `GamePackage`.

- [ ] Write failing tests for manifest validation.
- [ ] Implement the shared API and manifest validation.
- [ ] Run `./gradlew :game-api:testDebugUnitTest`.
- [ ] Commit scaffold and API.

### Task 2: Package Runtime

**Files:**
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/buddygames/center/package/GamePackageRepository.kt`
- Create: `app/src/test/java/com/buddygames/center/package/GamePackageRepositoryTest.kt`

**Interfaces:**
- Consumes: `GameManifest`, `GamePackage`.
- Produces: `GamePackageRepository.installFromDirectory`, `discoverInstalledGames`, and `ensureBuiltInPackagesInstalled`.

- [ ] Write failing repository tests for independent `Games/<gameId>` directories.
- [ ] Implement install/discover behavior with same-version overwrite and lower-version rejection.
- [ ] Run `./gradlew :app:testDebugUnitTest`.
- [ ] Commit package runtime.

### Task 3: Game Logic Modules

**Files:**
- Create: `games/gomoku/src/main/java/com/buddygames/gomoku/GomokuRules.kt`
- Create: `games/gomoku/src/test/java/com/buddygames/gomoku/GomokuRulesTest.kt`
- Create: `games/othello/src/main/java/com/buddygames/othello/OthelloRules.kt`
- Create: `games/othello/src/test/java/com/buddygames/othello/OthelloRulesTest.kt`
- Create: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiRules.kt`
- Create: `games/xiangqi/src/test/java/com/buddygames/xiangqi/XiangqiRulesTest.kt`

**Interfaces:**
- Produces: rule engines and robot move selectors for each game.

- [ ] Write failing rule tests for wins, legal moves, captures, and robot priorities.
- [ ] Implement rules and robot strategies.
- [ ] Run all game unit tests.
- [ ] Commit game rules.

### Task 4: Game Package UI And Package Artifacts

**Files:**
- Create: `games/*/build.gradle.kts`
- Create: `games/*/src/main/AndroidManifest.xml`
- Create: `games/*/src/main/java/.../*Plugin.kt`
- Create: `games/*/src/main/assets/manifest.json`

**Interfaces:**
- Consumes: `GamePlugin`, `GameContext`, `GameMode`.
- Produces: plugin screens and package zip tasks.

- [ ] Implement three plugin classes with game main screens and gameplay screens.
- [ ] Add per-game package copy and zip tasks.
- [ ] Run package zip tasks.
- [ ] Commit game packages.

### Task 5: Game Center Shell UI

**Files:**
- Create: `app/src/main/java/com/buddygames/center/MainActivity.kt`
- Create: `app/src/main/java/com/buddygames/center/ui/GameCenterApp.kt`
- Create: `app/src/main/java/com/buddygames/center/registry/BuiltInGameRegistry.kt`

**Interfaces:**
- Consumes: package repository and game plugins.
- Produces: playable home screen, game selection, game launch, game exit.

- [ ] Implement shell state and install built-in packages on first launch.
- [ ] Render game tiles.
- [ ] Launch selected plugin main screen.
- [ ] Support entering single-player and two-player gameplay.
- [ ] Commit shell UI.

### Task 6: Verification

**Commands:**
- `./gradlew test`
- `./gradlew packageGomokuGame packageOthelloGame packageXiangqiGame`
- `./gradlew :app:assembleDebug`

- [ ] Run JVM/unit tests.
- [ ] Build all game packages.
- [ ] Build the shell debug APK.
- [ ] Inspect generated artifacts under game package build directories.
- [ ] Commit verification fixes.
