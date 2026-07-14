# Intersection-Based Piece Placement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (- [ ]) syntax for tracking.

**Goal:** Place Gomoku stones and Xiangqi pieces on board-line intersections while retaining the existing game state and move behavior.

**Architecture:** Each game package receives a package-local fraction helper and a board composable that draws lines separately from transparent intersection hit targets. Logical coordinates stay unchanged: a Gomoku coordinate selects one of 15 by 15 intersections and a Xiangqi coordinate selects one of 9 by 10 intersections.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit 4, Android Gradle packaging, UIAutomator emulator checks.

## Global Constraints

- Keep GomokuState, XiangqiState, every rules engine, GameContext callback, and robot strategy unchanged.
- Do not modify Othello, game-api, package manifests, dex loading, root Gradle tasks, or dependencies.
- Gomoku has 15 horizontal and 15 vertical intersections, with 14 intervals per axis.
- Xiangqi has 9 vertical and 10 horizontal intersections, with 8 horizontal and 9 vertical intervals.
- A visible stone/piece center and its clickable hit area use the same intersection fraction.
- Keep the current wood-frame, information-rail, menu, and landscape layout language.

---

### Task 1: Add and prove Gomoku intersection geometry

**Files:**
- Modify: games/gomoku/src/main/java/com/buddygames/gomoku/GomokuPlugin.kt
- Modify: games/gomoku/src/test/java/com/buddygames/gomoku/GomokuRulesTest.kt

**Interfaces:**
- Produces: internal fun gomokuIntersectionFraction(index: Int): Float.
- Consumes: GomokuState.SIZE, whose value is 15.

- [ ] **Step 1: Write the failing unit test**

```kotlin
@Test
fun intersectionFractionAnchorsFirstCenterAndLastGomokuLines() {
    assertEquals(0f, gomokuIntersectionFraction(0), 0f)
    assertEquals(0.5f, gomokuIntersectionFraction(7), 0f)
    assertEquals(1f, gomokuIntersectionFraction(14), 0f)
}
```

- [ ] **Step 2: Verify the test is red**

Run: ./gradlew :games:gomoku:testDebugUnitTest --tests com.buddygames.gomoku.GomokuRulesTest.intersectionFractionAnchorsFirstCenterAndLastGomokuLines

Expected: compilation fails because gomokuIntersectionFraction is missing.

- [ ] **Step 3: Add the minimal geometry helper**

```kotlin
internal fun gomokuIntersectionFraction(index: Int): Float {
    require(index in 0 until GomokuState.SIZE)
    return index.toFloat() / (GomokuState.SIZE - 1)
}
```

- [ ] **Step 4: Replace the square-cell board with lines and intersection targets**

Inside GomokuBoard, derive step from a 14-interval square span. Draw 15 horizontal and 15 vertical one-dp lines at step / 2 plus index times step. Overlay 15 by 15 step-sized clickable boxes with their centers on those lines; draw each stone centered inside its own hit box. Preserve onPlay(row, col).

- [ ] **Step 5: Verify Gomoku**

Run: ./gradlew :games:gomoku:testDebugUnitTest packageGomokuGame

Expected: all rules tests including the new fraction test pass and build/game-packages/gomoku.zip is produced.

- [ ] **Step 6: Commit**

Run: git add games/gomoku/src/main/java/com/buddygames/gomoku/GomokuPlugin.kt games/gomoku/src/test/java/com/buddygames/gomoku/GomokuRulesTest.kt && git commit -m "fix: align gomoku stones to intersections"

### Task 2: Add and prove Xiangqi intersection geometry

**Files:**
- Modify: games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiPlugin.kt
- Modify: games/xiangqi/src/test/java/com/buddygames/xiangqi/XiangqiRulesTest.kt

**Interfaces:**
- Produces: internal fun xiangqiFileFraction(column: Int): Float and internal fun xiangqiRankFraction(row: Int): Float.
- Consumes: XiangqiState.COLS, whose value is 9, and XiangqiState.ROWS, whose value is 10.

- [ ] **Step 1: Write the failing unit test**

```kotlin
@Test
fun intersectionFractionsAnchorXiangqiFilesAndRanks() {
    assertEquals(0f, xiangqiFileFraction(0), 0f)
    assertEquals(0.5f, xiangqiFileFraction(4), 0f)
    assertEquals(1f, xiangqiFileFraction(8), 0f)
    assertEquals(0f, xiangqiRankFraction(0), 0f)
    assertEquals(1f, xiangqiRankFraction(9), 0f)
}
```

- [ ] **Step 2: Verify the test is red**

Run: ./gradlew :games:xiangqi:testDebugUnitTest --tests com.buddygames.xiangqi.XiangqiRulesTest.intersectionFractionsAnchorXiangqiFilesAndRanks

Expected: compilation fails because xiangqiFileFraction and xiangqiRankFraction are missing.

- [ ] **Step 3: Add the minimal geometry helpers**

```kotlin
internal fun xiangqiFileFraction(column: Int): Float {
    require(column in 0 until XiangqiState.COLS)
    return column.toFloat() / (XiangqiState.COLS - 1)
}

internal fun xiangqiRankFraction(row: Int): Float {
    require(row in 0 until XiangqiState.ROWS)
    return row.toFloat() / (XiangqiState.ROWS - 1)
}
```

- [ ] **Step 4: Replace the square-cell court with intersecting files and ranks**

Inside XiangqiBoard, compute an 8-interval by 9-interval court. Draw nine vertical and ten horizontal lines, with the river and palace marks as non-interactive decoration. Overlay 9 by 10 tap targets centered on line crossings; draw each XiangqiPiece and selected-state indicator at the exact same target center. Preserve onTap(row, col).

- [ ] **Step 5: Verify Xiangqi**

Run: ./gradlew :games:xiangqi:testDebugUnitTest packageXiangqiGame

Expected: all rules tests including the new fraction test pass and build/game-packages/xiangqi.zip is produced.

- [ ] **Step 6: Commit**

Run: git add games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiPlugin.kt games/xiangqi/src/test/java/com/buddygames/xiangqi/XiangqiRulesTest.kt && git commit -m "fix: align xiangqi pieces to intersections"

### Task 3: Complete cross-package validation

**Files:**
- Modify: no source files unless verification reports a specific layout defect.

**Interfaces:**
- Consumes: rebuilt Gomoku and Xiangqi game packages plus the shell debug APK.
- Produces: evidence that the two changed games render and accept moves on line intersections.

- [ ] **Step 1: Run the full verification gate**

Run: npm run verify

Expected: all unit tests, all three game package tasks, and app debug assembly pass.

- [ ] **Step 2: Start the emulator and launch the debug APK**

Run: HEADLESS=1 npm run start

Expected: the tablet AVD starts if needed and com.buddygames.center/.MainActivity launches.

- [ ] **Step 3: Verify geometry and moves in the emulator**

Use UIAutomator and screenshots to verify:

```text
Gomoku: the 15-line board has stones centered on crossings after one single-player move.
Xiangqi: initial pieces center on the 9 by 10 crossings, and a red soldier moves to the next crossing in single-player mode.
Both games: the board retains the information rail and the exit action returns to home.
```

- [ ] **Step 4: Inspect generated package contents**

Run: unzip -l build/game-packages/gomoku.zip && unzip -l build/game-packages/xiangqi.zip

Expected: both archives contain manifest.json, plugin.apk, and assets.
