# Board Game Center Texture UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the approved cabinet-shelf visual system across the Android Pad shell and the three independently packaged games, using bitmap textures only as material layers beneath accurate Compose board geometry.

**Architecture:** The app shell loads its mineral backdrop from `app` assets. Each game package carries its own texture in `package/assets/` and reads it through the existing `GameContext.gamePackage.assetsDir`; no public contract or packaging task changes are needed. Compose continues to draw all grid lines, intersections, discs, pieces, and touch targets.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Android bitmap decode, Gradle zip game packaging.

## Global Constraints

- The SSOT is `designs/specs/board-game-center.md`.
- Preserve package isolation: app visuals stay in `app`, per-game visuals stay under `games/<name>/`.
- Keep Gomoku and Xiangqi pieces centered on line intersections and Othello discs centered in cells.
- Keep game info rails limited to score, turn, and exit.
- Minimum primary touch target: 48dp; use semantic descriptions for non-text controls and game cards.
- Do not change `game-api`, `DexGamePluginLoader`, repository logic, Gradle module topology, or root packaging tasks.

---

### Task 1: Material assets and deterministic shell mapping

**Files:**
- Create: `app/src/main/assets/textures/mineral-slate.png`
- Create: `games/gomoku/package/assets/maple-board.png`
- Create: `games/othello/package/assets/felt-board.png`
- Create: `games/xiangqi/package/assets/xiangqi-maple.png`
- Modify: `app/src/main/java/com/buddygames/center/ui/HomeGamePresentation.kt`
- Modify: `app/src/test/java/com/buddygames/center/ui/HomeGamePresentationTest.kt`

- [ ] Add a failing presentation test that asserts each built-in game maps to a different board-material asset path.
- [ ] Generate low-contrast bitmap textures with no embedded lettering or board lines.
- [ ] Extend `HomeGamePresentation` with a stable material asset key and make the test pass.
- [ ] Run `./gradlew :app:testDebugUnitTest`.

### Task 2: Shell home cabinet shelf

**Files:**
- Modify: `app/src/main/java/com/buddygames/center/ui/GameCenterApp.kt`

- [ ] Add cached asset-to-`ImageBitmap` loading for the mineral background and shelf mini-board textures.
- [ ] Replace the generic tile grid treatment with three material-specific shelf slots, retaining responsive grid behavior, package metadata, import messages, and full-card semantics.
- [ ] Update title, status, focus, and primary action styling to the SSOT tokens without adding new app navigation.
- [ ] Run `./gradlew :app:assembleDebug`.

### Task 3: Per-game material menus and boards

**Files:**
- Modify: `games/gomoku/src/main/java/com/buddygames/gomoku/GomokuPlugin.kt`
- Modify: `games/othello/src/main/java/com/buddygames/othello/OthelloPlugin.kt`
- Modify: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiPlugin.kt`

- [ ] Add small package-asset texture loaders scoped to each plugin.
- [ ] Apply material backgrounds to each menu and board object while retaining existing state, rule calls, and board hit target math.
- [ ] Refine pieces, board frames, mode actions, and information rail styles to match the SSOT, with 48dp exit actions and selection indication.
- [ ] Run all three targeted game test suites and `package*Game` tasks.

### Task 4: End-to-end visual and package verification

**Files:**
- Modify: `docs/superpowers/specs/board-game-center.md` only if the delivered texture asset names differ from the SSOT.

- [ ] Run `npm run verify`.
- [ ] Launch `HEADLESS=1 npm run start` and visually inspect home plus one menu and one board for each game.
- [ ] Check generated packages contain their texture assets and stop the debug session.
- [ ] Run `git diff --check` and keep user-provided `desgins/` untouched.
