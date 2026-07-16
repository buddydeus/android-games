# Gomoku and Othello High-Fidelity UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the Gomoku and Othello package UIs so their menus and matches faithfully reproduce the supplied tactile board-game references on Android tablets.

**Architecture:** Keep every visual and asset inside its existing game package. Compose owns responsive layout, hit targets, board geometry, state, and accessibility; package-local raster textures provide maple and green-felt material detail, while Canvas gradients and shadows create frames and pieces without introducing dependencies.

**Tech Stack:** Kotlin, Jetpack Compose, Compose Canvas, JUnit 4, package-local PNG assets

## Global Constraints

- Do not change `game-api`, plugin loading, package installation, or root packaging tasks.
- Gomoku stones remain centered on the 15 x 15 line intersections.
- Othello discs remain centered inside the 8 x 8 cells.
- All controls retain at least 48dp touch targets and clear Chinese labels.
- Both landscape tablet and narrower portrait layouts must remain usable without overlap.

---

### Task 1: Lock Board Geometry Contracts

**Files:**
- Modify: `games/gomoku/src/test/java/com/buddygames/gomoku/GomokuRulesTest.kt`
- Modify: `games/gomoku/src/main/java/com/buddygames/gomoku/GomokuPlugin.kt`
- Modify: `games/othello/src/test/java/com/buddygames/othello/OthelloRulesTest.kt`
- Modify: `games/othello/src/main/java/com/buddygames/othello/OthelloPlugin.kt`

**Interfaces:**
- Produces: pure sizing helpers used by the Compose boards to preserve square geometry across tablet layouts.

- [ ] Add failing tests for the minimum, typical, and maximum rendered board sizes.
- [ ] Run each touched module test and confirm the new assertions fail because the helpers do not exist.
- [ ] Add the minimal sizing helpers and route board measurement through them.
- [ ] Run both module test suites and confirm they pass.

### Task 2: Rebuild Gomoku Visuals

**Files:**
- Modify: `games/gomoku/src/main/java/com/buddygames/gomoku/GomokuPlugin.kt`
- Reuse: `games/gomoku/package/assets/textures/gomoku-shelf.png`

**Interfaces:**
- Consumes: existing `GomokuState`, `GomokuRules`, package texture loader, and board sizing helper.
- Produces: responsive menu, maple board, intersection hit grid, dimensional stones, star points, and match information rail.

- [ ] Replace the sparse menu with a board-led composition and tactile control panel.
- [ ] Draw the maple board as layered shadow, walnut frame, inset lip, texture, ink grid, and five star points.
- [ ] Draw black and jade-white stones with cast shadow, radial lighting, rim, and last-move marker while retaining full-cell hit targets.
- [ ] Rebuild the information rail with centered score, separators, current-round status, and neutral outlined exit control.
- [ ] Verify portrait fallback stacks the panel below the board without clipping.

### Task 3: Rebuild Othello Visuals

**Files:**
- Modify: `games/othello/src/main/java/com/buddygames/othello/OthelloPlugin.kt`
- Reuse: `games/othello/package/assets/textures/othello-shelf.png`

**Interfaces:**
- Consumes: existing `OthelloState`, `OthelloRules`, package texture loader, and board sizing helper.
- Produces: responsive menu, dark timber frame, green felt cells, legal-move hints, dimensional discs, live counts, and match information rail.

- [ ] Replace the sparse menu with a board-led composition and tactile control panel.
- [ ] Draw the board as layered shadow, dark walnut frame, inset green felt, and crisp 8 x 8 grid.
- [ ] Draw ebony and bone discs with cast shadow, radial lighting, rim, and legal-move hints without changing rules behavior.
- [ ] Present live black/white counts and current-round state in the reference-style rail.
- [ ] Verify portrait fallback stacks the panel below the board without clipping.

### Task 4: Package and Runtime Verification

**Files:**
- Inspect: `build/game-packages/gomoku.zip`
- Inspect: `build/game-packages/othello.zip`

**Interfaces:**
- Consumes: completed game modules and existing Gradle package tasks.
- Produces: verified game zips and emulator screenshots.

- [ ] Run `./gradlew :games:gomoku:testDebugUnitTest :games:othello:testDebugUnitTest`.
- [ ] Run `npm run verify` and confirm all tests, zips, and the debug APK build successfully.
- [ ] Inspect both zips to confirm each texture remains package-local.
- [ ] Run `HEADLESS=1 npm start`, capture Gomoku and Othello menu/match screenshots, and compare board proportions, materials, panel hierarchy, intersections/cells, and touch behavior against the references.
- [ ] Correct any visual mismatch found in the screenshot review and repeat verification.
