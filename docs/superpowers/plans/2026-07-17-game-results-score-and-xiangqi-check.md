# Game Results, Score, and Xiangqi Check Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add session scoring and restart controls to all three games, plus colored turn ownership and check warnings to Xiangqi.

**Architecture:** Keep all state inside each independently packaged game module. Add small immutable score types and pure rule helpers that unit tests can exercise, then pass score/result/restart data into the existing Compose information rails.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit 4, Android Gradle Plugin.

## Global Constraints

- Do not change `game-api`, the shell loader, or package format.
- Restart preserves the score but resets the current board and turn.
- A decisive result changes the score exactly once; a draw does not.
- Xiangqi colors only the side name in `当前回合：红方|黑方`.
- Xiangqi shows `将军` only while the active side is in check.

---

### Task 1: Pure Score and Terminal Models

**Files:**
- Modify: `games/gomoku/src/main/java/com/buddygames/gomoku/GomokuPlugin.kt`
- Modify: `games/othello/src/main/java/com/buddygames/othello/OthelloPlugin.kt`
- Modify: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiPlugin.kt`
- Test: each module's existing `*RulesTest.kt`

**Interfaces:**
- Produces: `GomokuScore.record(Stone?)`, `OthelloScore.record(Disc?)`, and `XiangqiScore.record(Side?)`.
- Produces: terminal-state helpers used by Compose to decide restart visibility.

- [ ] Add tests asserting the correct side increments and `null` leaves a draw score unchanged.
- [ ] Run the three module tests and confirm compilation fails because score types are missing.
- [ ] Add immutable score data classes and terminal helpers.
- [ ] Run the three module tests and confirm they pass.

### Task 2: Session Score and Restart Flow

**Files:**
- Modify: the three `*Plugin.kt` files.
- Modify: the three `*Visuals.kt` files.

**Interfaces:**
- Consumes: score types from Task 1.
- Produces: `score`, `gameOver`, and `onRestart` parameters on each game layout and information rail.

- [ ] Add a restart-state test where a pure reset helper returns each game's initial board and first turn.
- [ ] Run targeted tests and confirm failure because reset helpers are missing.
- [ ] Record each terminal winner once and implement restart callbacks that preserve score.
- [ ] Render live score text and show a primary `重新开始` button only at terminal state.
- [ ] Run targeted tests and confirm they pass.

### Task 3: Xiangqi Turn Styling and Check Warning

**Files:**
- Modify: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiRules.kt`
- Modify: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiPlugin.kt`
- Modify: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiVisuals.kt`
- Test: `games/xiangqi/src/test/java/com/buddygames/xiangqi/XiangqiRulesTest.kt`

**Interfaces:**
- Produces: `XiangqiRules.isInCheck(state: XiangqiState, side: Side): Boolean`.
- Consumes: `turn`, `winner`, and `isInCheck` in `XiangqiInfoRail`.

- [ ] Add tests for rook check, blocked rook, and opposing generals facing on an open file.
- [ ] Run Xiangqi tests and confirm failure because `isInCheck` is missing.
- [ ] Implement attack detection including the flying-general capture.
- [ ] Render `当前回合：` in ink and only the active side in red or black.
- [ ] Render a separate `将军` warning when the active side is in check and the game is not over.
- [ ] Run Xiangqi tests and confirm they pass.

### Task 4: Integration Verification

**Files:**
- Verify all modified game packages and generated APK.

- [ ] Run `git diff --check`.
- [ ] Run all three targeted module test tasks.
- [ ] Run `npm run verify`.
- [ ] Launch the emulator and inspect active, check, terminal, score, and restart states where practical.
