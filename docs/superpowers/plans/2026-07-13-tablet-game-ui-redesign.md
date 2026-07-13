# Tablet Game UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the Android Pad game-center home and all three packaged games to match the supplied prototype while preserving package behavior.

**Architecture:** The `app` module owns the home screen and import feedback. Each `games/*` module keeps its own full-screen menu and game board so packages remain independently updateable. Existing rule engines, package installation, dex loading, and `game-api` remain unchanged.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, JUnit 4, Android emulator UIAutomator.

## Global Constraints

- Preserve `CURRENT_SHELL_API = 1` and all `game-api` public types.
- Do not modify `GamePackageRepository`, `DexGamePluginLoader`, root Gradle packaging, dependencies, or package archive layout.
- Keep the app offline, landscape-first, and light-only. Do not add login, network, shop, rankings, settings, bottom navigation, or dark theme.
- Home uses 28dp page padding, 18dp card gaps, 14dp cards, 12dp buttons, and 48dp minimum touch targets.
- A board page contains its board plus only history score, current turn, and exit action in its information rail.
- Preserve existing game rules and single-player robot behavior.

---

### Task 1: Build and test home-game presentation metadata

**Files:**
- Create: `app/src/main/java/com/buddygames/center/ui/HomeGamePresentation.kt`
- Create: `app/src/test/java/com/buddygames/center/ui/HomeGamePresentationTest.kt`
- Modify: `app/src/main/java/com/buddygames/center/ui/GameCenterApp.kt`

**Interfaces:**
- Consumes: `GamePackage.manifest.gameId`, `displayName`, and `versionName`.
- Produces: `internal fun homeGamePresentation(gameId: String): HomeGamePresentation` for game-card metadata.

- [ ] **Step 1: Write the failing unit test**

```kotlin
@Test
fun presentationUsesKnownBoardMetadataAndFallsBackForImportedGames() {
    assertEquals("15 x 15", homeGamePresentation("gomoku").boardSize)
    assertEquals("8 x 8", homeGamePresentation("othello").boardSize)
    assertEquals("9 x 10", homeGamePresentation("xiangqi").boardSize)
    assertEquals("本地游戏", homeGamePresentation("sudoku").boardSize)
}
```

- [ ] **Step 2: Verify the test is red**

Run: `./gradlew :app:testDebugUnitTest --tests com.buddygames.center.ui.HomeGamePresentationTest`

Expected: failure because `homeGamePresentation` has not been defined.

- [ ] **Step 3: Implement the presentation model**

```kotlin
internal data class HomeGamePresentation(
    val boardSize: String,
    val accent: Color,
    val symbol: String,
    val packageLabel: String
)

internal fun homeGamePresentation(gameId: String): HomeGamePresentation = when (gameId) {
    "gomoku" -> HomeGamePresentation("15 x 15", Color(0xFF2D3438), "五", "gomoku")
    "othello" -> HomeGamePresentation("8 x 8", Color(0xFF2F6F56), "黑", "othello")
    "xiangqi" -> HomeGamePresentation("9 x 10", Color(0xFF9B2F2F), "象", "xiangqi")
    else -> HomeGamePresentation("本地游戏", Color(0xFF274C77), gameId.take(1), gameId)
}
```

- [ ] **Step 4: Rebuild `GameCenterHome` and `GameTile`**

Use a paper background, top bar with board mark and `游戏中心`, 48dp import button, installed-game eyebrow/title/count, semantic import message bar, and `LazyVerticalGrid(GridCells.Adaptive(260.dp))`. Make the complete card clickable and render symbol, `可启动`, name, `v<version> · <board size>`, and package label. Keep the document picker and repository calls unchanged.

- [ ] **Step 5: Verify green**

Run: `./gradlew :app:testDebugUnitTest`

Expected: `HomeGamePresentationTest` and existing package-repository tests pass.

- [ ] **Step 6: Commit the task**

Run: `git add app/src/main/java/com/buddygames/center/ui/HomeGamePresentation.kt app/src/main/java/com/buddygames/center/ui/GameCenterApp.kt app/src/test/java/com/buddygames/center/ui/HomeGamePresentationTest.kt && git commit -m "feat: redesign game center home"`

### Task 2: Redesign Gomoku screens without changing game behavior

**Files:**
- Modify: `games/gomoku/src/main/java/com/buddygames/gomoku/GomokuPlugin.kt`
- Test: `games/gomoku/src/test/java/com/buddygames/gomoku/GomokuRulesTest.kt`

**Interfaces:**
- Consumes: existing `GameContext`, `GomokuState`, `GomokuRules`, `Stone`, and `GameMode`.
- Produces: the package-local full-screen menu and responsive board with `GameContext.exitGame`.

- [ ] **Step 1: Establish the rules baseline**

Run: `./gradlew :games:gomoku:testDebugUnitTest`

Expected: all existing Gomoku rule tests pass.

- [ ] **Step 2: Recompose the menu**

Keep callbacks untouched. Use a pale wooden full-screen backdrop, a wood-grid mark with black/white stones, the `五子棋` title, and vertically centered 48dp-minimum buttons: `单人模式`, `双人对战`, and `退出游戏`.

- [ ] **Step 3: Recompose the board**

Keep `play` and `statusText` behavior. Remove restart and return-to-menu controls. Draw the 15 by 15 board inside a dark wood frame with circular stones. Use a wide `Row` for board plus paper/bronze information rail and a narrow `Column` fallback. The rail presents `0 : 0`, current status, and an exit button wired to `context.exitGame`.

- [ ] **Step 4: Verify and package**

Run: `./gradlew :games:gomoku:testDebugUnitTest packageGomokuGame`

Expected: rule tests pass and `build/game-packages/gomoku.zip` is produced.

- [ ] **Step 5: Commit the task**

Run: `git add games/gomoku/src/main/java/com/buddygames/gomoku/GomokuPlugin.kt && git commit -m "feat: redesign gomoku game screens"`

### Task 3: Redesign Othello screens without changing game behavior

**Files:**
- Modify: `games/othello/src/main/java/com/buddygames/othello/OthelloPlugin.kt`
- Test: `games/othello/src/test/java/com/buddygames/othello/OthelloRulesTest.kt`

**Interfaces:**
- Consumes: existing `GameContext`, `OthelloState`, `OthelloRules`, `Disc`, and `GameMode`.
- Produces: the package-local full-screen menu and responsive board with `GameContext.exitGame`.

- [ ] **Step 1: Establish the rules baseline**

Run: `./gradlew :games:othello:testDebugUnitTest`

Expected: legal move, flip, robot, and game-over tests pass.

- [ ] **Step 2: Recompose the menu**

Use a dark wood and green-board mark, full-screen green-themed backdrop, `黑白棋` title, and centered `单人模式`, `双人对战`, and `退出游戏` controls. Keep `startGame` and `exitGame` behavior intact.

- [ ] **Step 3: Recompose the board**

Keep `advanceAfterMove` and `play` logic. Remove restart and return controls. Draw an 8 by 8 green grid inside a dark wood frame with round discs. Add a responsive local rail with `0 : 0`, existing turn/win status, and exit action. Do not add a separate disc-count panel.

- [ ] **Step 4: Verify and package**

Run: `./gradlew :games:othello:testDebugUnitTest packageOthelloGame`

Expected: rules pass and `build/game-packages/othello.zip` is produced.

- [ ] **Step 5: Commit the task**

Run: `git add games/othello/src/main/java/com/buddygames/othello/OthelloPlugin.kt && git commit -m "feat: redesign othello game screens"`

### Task 4: Redesign Xiangqi screens without changing game behavior

**Files:**
- Modify: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiPlugin.kt`
- Test: `games/xiangqi/src/test/java/com/buddygames/xiangqi/XiangqiRulesTest.kt`

**Interfaces:**
- Consumes: existing `GameContext`, `XiangqiState`, `XiangqiRules`, `Side`, `XiangqiPiece`, and `GameMode`.
- Produces: the package-local full-screen menu and responsive board with `GameContext.exitGame`.

- [ ] **Step 1: Establish the rules baseline**

Run: `./gradlew :games:xiangqi:testDebugUnitTest`

Expected: existing horse, cannon, rook, and robot tests pass.

- [ ] **Step 2: Recompose the menu**

Use pale wood, vermilion accent, a Chinese-chess mark, `象棋` title, and centered `单人模式`, `双人对战`, and `退出游戏` controls without extra rules copy.

- [ ] **Step 3: Recompose the board**

Keep selection, legality, robot, and winner behavior. Remove restart and return controls. Draw a 9 by 10 court inside a wood frame, including river label, palace diagonals, circular ivory pieces, and red/black labels. Add the responsive local rail with `0 : 0`, current status, and exit action.

- [ ] **Step 4: Verify and package**

Run: `./gradlew :games:xiangqi:testDebugUnitTest packageXiangqiGame`

Expected: rules pass and `build/game-packages/xiangqi.zip` is produced.

- [ ] **Step 5: Commit the task**

Run: `git add games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiPlugin.kt && git commit -m "feat: redesign xiangqi game screens"`

### Task 5: Run end-to-end tablet acceptance

**Files:**
- Modify: no source files unless verification identifies a focused defect.

**Interfaces:**
- Consumes: rebuilt built-in package zips and debug APK.
- Produces: verified tablet-layout home, menus, game boards, navigation, and legal moves.

- [ ] **Step 1: Run the full verification gate**

Run: `npm run verify`

Expected: all tests, three package tasks, and `:app:assembleDebug` pass.

- [ ] **Step 2: Launch the debug build headlessly**

Run: `HEADLESS=1 npm run start`

Expected: the AVD boots if necessary, the APK installs, and `com.buddygames.center/.MainActivity` launches.

- [ ] **Step 3: Perform UIAutomator acceptance**

```text
Home: title, import action, installed count, and three cards
Menus: title, 单人模式, 双人对战, 退出游戏
Boards: board, score, current turn, 退出游戏 only
Navigation: home -> menu -> single-player board -> home
Gameplay: one legal move in Gomoku, Othello, and Xiangqi
```

- [ ] **Step 4: Inspect tablet screenshots**

Confirm a three-card home grid, readable text, no overlap, distinct material boards, and no restart/back controls on boards.

