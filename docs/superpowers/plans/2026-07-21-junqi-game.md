# Junqi Game Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a complete offline two-player hidden-information Junqi package named `军棋`, with single-player AI, local pass-and-play, package-owned visuals, and independent `0.0.1` versioning.

**Architecture:** Keep all rules, true hidden state, observer knowledge, AI, session flow, UI, and assets in `games/junqi`. Expose only `JunqiPlugin` through the existing `GamePlugin` contract, then register the fifth zip in the generic root packaging and APK asset verification tasks without changing shell runtime code or shell version.

**Tech Stack:** Kotlin 2.4, Jetpack Compose, Android library module, JUnit 4, pure-Kotlin deterministic sampled Negamax/Alpha-Beta, Pillow asset generator, Gradle zip/d8 packaging.

## Global Constraints

- Use game ID `junqi`, display name `军棋`, `versionCode = 1`, and `versionName = 0.0.1`.
- Do not modify `game-api`, `CURRENT_SHELL_API`, `DexGamePluginLoader`, or `GamePackageRepository`.
- AI public entry points accept `JunqiObservation`, never `JunqiState` or hidden enemy types.
- Preserve the approved 12x5 road/rail graph, deployment limits, battle matrix, camp protection, headquarters lock, flag reveal, and `SL[31]` terminal rule.
- Single-player AI runs away from the Compose UI thread and uses immutable monotonic levels 1-10.
- Keep all Junqi images under `games/junqi/package/assets/`; invalid images fall back to Compose drawing.
- Keep the shared latest-move marker constants `0.92`, `0.04`, `0.18`, `0xB84FCBFF`, and `0x70115C93`.
- Every repository change updates `AGENTS.md`; final implementation updates root and game documentation.
- Run `./gradlew :games:junqi:testDebugUnitTest` after each implementation group and `npm run verify` before completion.
- Automatically create scoped local commits; never push.

---

### Task 1: Module Contract And Package Metadata

**Files:**
- Create: `games/junqi/build.gradle.kts`
- Create: `games/junqi/package/manifest.json`
- Create: `games/junqi/src/test/java/com/buddygames/junqi/JunqiManifestTest.kt`
- Modify: `settings.gradle.kts`
- Modify: `AGENTS.md`

**Interfaces:**
- Consumes: `com.buddygames.api.GameManifest` and the neighboring game module Gradle conventions.
- Produces: Gradle module `:games:junqi` and manifest constants `JUNQI_VERSION_CODE`, `JUNQI_VERSION_NAME`.

- [ ] **Step 1: Add the module configuration and a failing manifest test**

```kotlin
@Test fun manifestStartsAtIndependentVersion() {
    assertEquals(1, JUNQI_VERSION_CODE)
    assertEquals("0.0.1", JUNQI_VERSION_NAME)
}
```

- [ ] **Step 2: Verify RED**

Run: `./gradlew :games:junqi:testDebugUnitTest`
Expected: Kotlin compilation fails because Junqi manifest constants do not exist.

- [ ] **Step 3: Add package constants and aligned JSON metadata**

```kotlin
internal const val JUNQI_VERSION_CODE = 1
internal const val JUNQI_VERSION_NAME = "0.0.1"
```

The JSON must use `gameId: junqi`, `displayName: 军棋`, `entryClass: com.buddygames.junqi.JunqiPlugin`, and `icon: assets/icon.png`.

- [ ] **Step 4: Verify GREEN and commit**

Run: `./gradlew :games:junqi:testDebugUnitTest`
Expected: PASS.

Commit title: `feat: scaffold junqi game package`

### Task 2: Board Graph And Legal Movement

**Files:**
- Create: `games/junqi/src/main/java/com/buddygames/junqi/JunqiModel.kt`
- Create: `games/junqi/src/main/java/com/buddygames/junqi/JunqiBoard.kt`
- Create: `games/junqi/src/main/java/com/buddygames/junqi/JunqiRules.kt`
- Create: `games/junqi/src/test/java/com/buddygames/junqi/JunqiBoardTest.kt`
- Create: `games/junqi/src/test/java/com/buddygames/junqi/JunqiMovementTest.kt`
- Modify: `AGENTS.md`

**Interfaces:**
- Produces: `JunqiSide`, `JunqiPieceType`, `JunqiPosition`, `JunqiPiece`, `JunqiMove`, `JunqiState`, `JunqiBoard.roadNeighbors`, `JunqiBoard.railNeighbors`, and `JunqiRules.legalMoves(state, side)`.

- [ ] **Step 1: Write graph tests for all headquarters, camps, road edges, rail edges, and 60 nodes**

```kotlin
@Test fun engineerTurnsOnRailButCommanderCannot() {
    assertTrue(engineerMoves.contains(JunqiPosition(3, 4)))
    assertFalse(commanderMoves.contains(JunqiPosition(3, 4)))
}
```

- [ ] **Step 2: Verify RED**

Run: `./gradlew :games:junqi:testDebugUnitTest --tests '*JunqiBoardTest' --tests '*JunqiMovementTest'`
Expected: compile failure for missing model and graph APIs.

- [ ] **Step 3: Implement immutable graph construction and legal move generation**

Use adjacency sets for roads and rails, BFS only for engineer railway moves, straight unobstructed railway scans for other movable pieces, one-edge road movement, camp attack protection, and headquarters immobility.

- [ ] **Step 4: Verify GREEN and commit**

Run: `./gradlew :games:junqi:testDebugUnitTest`
Expected: PASS.

Commit title: `feat: implement junqi board movement`

### Task 3: Deployment, Battles, And Terminal Rules

**Files:**
- Create: `games/junqi/src/main/java/com/buddygames/junqi/JunqiDeployment.kt`
- Modify: `games/junqi/src/main/java/com/buddygames/junqi/JunqiRules.kt`
- Create: `games/junqi/src/test/java/com/buddygames/junqi/JunqiDeploymentTest.kt`
- Create: `games/junqi/src/test/java/com/buddygames/junqi/JunqiBattleTest.kt`
- Create: `games/junqi/src/test/java/com/buddygames/junqi/JunqiTerminalTest.kt`
- Modify: `AGENTS.md`

**Interfaces:**
- Produces: `JunqiDeployment.default(side)`, `JunqiDeployment.random(side, seed)`, `JunqiDeployment.swapIfLegal`, `JunqiRules.applyMove`, `JunqiBattleOutcome`, and `JunqiResult`.

- [ ] **Step 1: Add failing tests for 25-piece counts, flag/mine/bomb limits, legal swap and deterministic random layout**

```kotlin
@Test fun randomDeploymentIsLegalAndRepeatable() {
    assertEquals(JunqiDeployment.random(RED, 42), JunqiDeployment.random(RED, 42))
    assertTrue(JunqiDeployment.isLegal(JunqiDeployment.random(RED, 42), RED))
}
```

- [ ] **Step 2: Add failing tests for every special battle and terminal path**

Cover rank comparison, equal ranks, bomb, engineer versus mine, non-engineer versus mine, flag capture, commander death reveal, no legal move, both immobile draw, and the 31st quiet half-move loss.

- [ ] **Step 3: Verify RED**

Run: `./gradlew :games:junqi:testDebugUnitTest --tests '*JunqiDeploymentTest' --tests '*JunqiBattleTest' --tests '*JunqiTerminalTest'`
Expected: compile failure for deployment and adjudication APIs.

- [ ] **Step 4: Implement the minimum complete adjudicator and verify GREEN**

Run: `./gradlew :games:junqi:testDebugUnitTest`
Expected: PASS.

Commit title: `feat: adjudicate junqi deployment and battles`

### Task 4: Hidden Information And Fair AI

**Files:**
- Create: `games/junqi/src/main/java/com/buddygames/junqi/JunqiObservation.kt`
- Create: `games/junqi/src/main/java/com/buddygames/junqi/JunqiAi.kt`
- Create: `games/junqi/src/main/java/com/buddygames/junqi/JunqiSearchEngine.kt`
- Create: `games/junqi/src/test/java/com/buddygames/junqi/JunqiObservationTest.kt`
- Create: `games/junqi/src/test/java/com/buddygames/junqi/JunqiAiTest.kt`
- Modify: `AGENTS.md`

**Interfaces:**
- Produces: `JunqiObservation.from(state, viewer)`, `JunqiKnowledge.update(event)`, `JunqiAiLevel.forPlayerScore(score)`, and `JunqiAi.chooseMove(observation, knowledge, level): JunqiMove?`.

- [ ] **Step 1: Add failing information-boundary tests**

```kotlin
@Test fun identicalObservationsChooseIdenticallyAcrossDifferentHiddenArmies() {
    val first = JunqiAi.chooseMove(observationA, knowledge, level)
    val second = JunqiAi.chooseMove(observationB, knowledge, level)
    assertEquals(first, second)
}
```

Also assert enemy ranks are absent, moved pieces exclude flag/mine candidates, and revealed flags become singleton candidates.

- [ ] **Step 2: Add failing tactical and level monotonicity tests**

Cover immediate flag capture, exposed-flag defense, confirmed-mine engineer attack, bomb exchange preference, and nondecreasing sample/depth/node/time budgets from level 1 through 10.

- [ ] **Step 3: Verify RED**

Run: `./gradlew :games:junqi:testDebugUnitTest --tests '*JunqiObservationTest' --tests '*JunqiAiTest'`
Expected: compile failure for observation and AI APIs.

- [ ] **Step 4: Implement deterministic observation-seeded sampling and bounded search**

Root candidates come only from observation-visible own pieces. Sample enemy types from knowledge candidates and remaining counts; score immediate tactics first, then sampled alpha-beta evaluations. Stop at node or time budget and apply deterministic weakening only at levels 1-5.

- [ ] **Step 5: Verify GREEN and commit**

Run: `./gradlew :games:junqi:testDebugUnitTest`
Expected: PASS with both hidden-state variants selecting the same move.

Commit title: `feat: add fair offline junqi AI`

### Task 5: Session, Privacy, Undo, And Restart

**Files:**
- Create: `games/junqi/src/main/java/com/buddygames/junqi/JunqiSession.kt`
- Create: `games/junqi/src/test/java/com/buddygames/junqi/JunqiSessionTest.kt`
- Modify: `AGENTS.md`

**Interfaces:**
- Produces: `JunqiPhase`, `JunqiMode`, `JunqiSessionState`, and `JunqiSession` methods `selectDeploymentPiece`, `randomizeDeployment`, `ready`, `acceptHandoff`, `play`, `undo`, `restart`, and `applyRobotMove`.

- [ ] **Step 1: Write failing phase, privacy, score, undo, restart, side-swap, draw, and last-move tests**

```kotlin
@Test fun singlePlayerWinSwapsSideButLossRestoresRedAndDrawKeepsSide() {
    assertEquals(BLUE, won.restart().playerSide)
    assertEquals(RED, lost.restart().playerSide)
    assertEquals(draw.playerSide, draw.restart().playerSide)
}
```

- [ ] **Step 2: Verify RED**

Run: `./gradlew :games:junqi:testDebugUnitTest --tests '*JunqiSessionTest'`
Expected: compile failure for session APIs.

- [ ] **Step 3: Implement snapshot-based session transitions**

Single-player snapshots begin before the human move and include the robot reply; two-player snapshots contain one move. `HANDOFF` exposes no board observation. Winner states hide undo while draws retain it.

- [ ] **Step 4: Verify GREEN and commit**

Run: `./gradlew :games:junqi:testDebugUnitTest`
Expected: PASS.

Commit title: `feat: add junqi session flow`

### Task 6: Package-Owned Visual Assets

**Files:**
- Create: `games/junqi/tools/generate_junqi_assets.py`
- Create: `games/junqi/package/assets/icon.png`
- Create: `games/junqi/package/assets/board/junqi-board.png`
- Create: `games/junqi/package/assets/textures/junqi-shelf.png`
- Create: `games/junqi/src/main/java/com/buddygames/junqi/JunqiVisuals.kt`
- Create: `games/junqi/src/test/java/com/buddygames/junqi/JunqiAssetsTest.kt`
- Modify: `AGENTS.md`

**Interfaces:**
- Produces: `JunqiVisuals.BOARD_GRID`, package bitmap paths, and shared marker constants.

- [ ] **Step 1: Add failing asset dimension, alpha-corner, coordinate-registration, and marker tests**

```kotlin
@Test fun packagedBoardMatchesRegisteredGeometry() {
    assertEquals(1400, image.width)
    assertEquals(1680, image.height)
    assertEquals(0xB84FCBFF, JunqiVisuals.LAST_MOVE_COLOR)
}
```

- [ ] **Step 2: Verify RED**

Run: `./gradlew :games:junqi:testDebugUnitTest --tests '*JunqiAssetsTest'`
Expected: failure because the package images and geometry constants are absent.

- [ ] **Step 3: Implement a deterministic Pillow generator and generate all three images**

The board image includes the complete approved graph but no pieces. The icon uses opposing red/blue tiles around a rail junction inside a circular-safe porcelain medallion.

- [ ] **Step 4: Verify GREEN and commit**

Run: `./gradlew :games:junqi:testDebugUnitTest`
Expected: PASS.

Commit title: `feat: add junqi package visuals`

### Task 7: Compose Menu And Game Screen

**Files:**
- Create: `games/junqi/src/main/java/com/buddygames/junqi/JunqiPlugin.kt`
- Create: `games/junqi/src/main/java/com/buddygames/junqi/JunqiScreen.kt`
- Create: `games/junqi/src/main/java/com/buddygames/junqi/JunqiBoardView.kt`
- Create: `games/junqi/src/main/java/com/buddygames/junqi/JunqiTextures.kt`
- Create: `games/junqi/src/test/java/com/buddygames/junqi/JunqiPluginTest.kt`
- Modify: `AGENTS.md`

**Interfaces:**
- Consumes: `GameContext`, `JunqiSession`, `JunqiObservation`, `JunqiAi`.
- Produces: `com.buddygames.junqi.JunqiPlugin` and the complete `MainScreen`/`GameScreen` flow.

- [ ] **Step 1: Add failing plugin metadata and UI-label tests**

```kotlin
@Test fun menuUsesSharedLabelsAndManifestVersion() {
    assertEquals(listOf("单人模式", "双人对战", "退出游戏"), JunqiUiText.MENU_LABELS)
    assertEquals("版本 0.0.1", JunqiUiText.versionLabel)
}
```

- [ ] **Step 2: Verify RED**

Run: `./gradlew :games:junqi:testDebugUnitTest --tests '*JunqiPluginTest'`
Expected: compile failure for plugin/UI text APIs.

- [ ] **Step 3: Implement the package menu and responsive game screen**

Use package-local bitmap loading with Compose fallbacks, bottom-side coordinate mapping, upright piece labels, deployment controls, opaque handoff screen, generic battle result, score/turn/AI rail, undo/restart rules, and asynchronous robot generation checks.

- [ ] **Step 4: Verify GREEN and commit**

Run: `./gradlew :games:junqi:testDebugUnitTest`
Expected: PASS.

Commit title: `feat: build junqi tablet interface`

### Task 8: Fifth Package Integration And Documentation

**Files:**
- Modify: `build.gradle.kts`
- Modify: `app/build.gradle.kts`
- Modify: `package.json`
- Create: `games/junqi/README.md`
- Modify: `README.md`
- Modify: `docs/agents/game-plugins.md`
- Modify: `AGENTS.md`

**Interfaces:**
- Produces: `packageJunqiGame`, `npm run build:game:junqi`, `build/game-packages/junqi.zip`, and `assets/builtin-games/junqi.zip` in the debug APK.

- [ ] **Step 1: Add Junqi to package registration, aggregate scripts, APK dependency, and five-package verification**

```kotlin
registerGamePackageTask("packageJunqiGame", "junqi")
```

- [ ] **Step 2: Write the game README and update all human/agent package lists and commands**

Document standard rules, hidden-information fairness, 1-10 AI, local privacy handoff, package assets, build/test commands, and independent `0.0.1` version.

- [ ] **Step 3: Build and inspect the package**

Run: `npm run build:game:junqi && unzip -l build/game-packages/junqi.zip`
Expected: `manifest.json`, `plugin.apk`, `assets/icon.png`, `assets/board/junqi-board.png`, and `assets/textures/junqi-shelf.png` are present.

- [ ] **Step 4: Run integration verification and commit**

Run: `npm run verify`
Expected: all module tests, five package checks, and debug APK asset checks pass.

Commit title: `feat: integrate junqi game package`

### Task 9: Emulator Acceptance And Final Audit

**Files:**
- Modify only if runtime acceptance exposes a defect; every fix starts with a failing regression test and increments Junqi above `0.0.1` only when it changes shipped game behavior after the initial implementation commit.
- Modify: `AGENTS.md` for any resulting file change.

**Interfaces:**
- Consumes: built debug APK and package zip.
- Produces: verified launch, screenshots, UI hierarchy evidence, and clean Git state.

- [ ] **Step 1: Start, install, and launch on the existing emulator/device path**

Run: `HEADLESS=1 DETACH_EMULATOR=1 npm start`
Expected: `com.buddygames.center/.MainActivity` launches successfully.

- [ ] **Step 2: Verify package-driven discovery and core flows**

Use ADB UI hierarchy and screenshots to verify the home displays `军棋` from package metadata, then exercise menu, legal default deployment, swap, random, ready, handoff, one single-player move/robot response, one double-player move/handoff, undo, restart, and latest-move marker without overlap.

- [ ] **Step 3: Run final clean verification**

Run: `./gradlew :games:junqi:testDebugUnitTest && npm run verify && git diff --check && git status --short`
Expected: tests and packaging pass; only intentional source/doc changes remain before the final scoped commit, then the worktree is clean.
