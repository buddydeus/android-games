# Xiangqi AI Search Optimization and Calibration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the allocation-heavy Xiangqi search hot path with a measurable pure-Kotlin engine whose effective depth and calibrated results form a distinguishable ten-level ladder.

**Architecture:** Keep `XiangqiRules` as session authority and add a search-only primitive board with make/unmake, direct attack detection, and equivalence tests. Move iterative deepening into a focused search engine that reports completed depth and stop reason, uses cached move ordering and a bounded transposition table, then calibrate the existing immutable level configurations with deterministic fixtures and opt-in color-swapped matches.

**Tech Stack:** Kotlin, JUnit 4, Android Gradle Plugin unit tests, existing Compose game plugin boundary

## Global Constraints

- Keep all production changes inside `games/xiangqi/`; do not change `app`, `game-api`, other games, dependencies, or root packaging tasks.
- Keep the score mapping exactly `min(playerScore + 1, 10)`.
- Keep runtime think time at or below 3,000 milliseconds.
- Preserve immediate general capture, checkmate preference, safe-move filtering, poisoned-capture avoidance, cancellation, and stale-result protection.
- Keep the engine pure Kotlin, offline, dex-compatible, and deterministic for the same state and level.
- Increment Xiangqi to `versionCode = 7` and `versionName = 0.0.7` in plugin code and `games/xiangqi/package/manifest.json`.
- Update `AGENTS.md` for the repository change.

---

### Task 1: Primitive Search Position

**Files:**
- Create: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiSearchPosition.kt`
- Create: `games/xiangqi/src/test/java/com/buddygames/xiangqi/XiangqiSearchPositionTest.kt`

**Interfaces:**
- Consumes: `XiangqiState`, `XiangqiMove`, `XiangqiPiece`, `PieceType`, and `Side`
- Produces: `XiangqiSearchPosition.from(state)`, `legalMoves(side)`, `generateLegalMoves(side, buffer)`, `makeMove(encodedMove)`, `unmakeMove(encodedMove, capturedPiece)`, `isInCheck(side)`, `hash`, and encoded-move conversion helpers

- [x] **Step 1: Write failing conversion and make/unmake tests**

```kotlin
@Test
fun makeAndUnmakeRestoreBoardGeneralsAndHash() {
    val original = XiangqiState.initial()
    val position = XiangqiSearchPosition.from(original)
    val originalHash = position.hash
    val move = encodeSearchMove(XiangqiMove(6, 0, 5, 0))

    val captured = position.makeMove(move)
    position.unmakeMove(move, captured)

    assertEquals(original, position.toState())
    assertEquals(originalHash, position.hash)
}
```

- [x] **Step 2: Run the focused test and verify RED**

Run:

```bash
./gradlew :games:xiangqi:testDebugUnitTest --tests com.buddygames.xiangqi.XiangqiSearchPositionTest
```

Expected: compilation fails because `XiangqiSearchPosition` and encoded-move helpers do not exist.

- [x] **Step 3: Implement primitive encoding, conversion, and reversible moves**

Use `IntArray(90)`, zero for empty, positive codes for red, negative codes for black, and a 14-bit move encoding:

```kotlin
internal fun encodeSearchMove(move: XiangqiMove): Int =
    ((move.fromRow * 9 + move.fromCol) shl 7) or
        (move.toRow * 9 + move.toCol)

internal fun decodeSearchMove(move: Int): XiangqiMove {
    val from = move ushr 7
    val to = move and 0x7f
    return XiangqiMove(from / 9, from % 9, to / 9, to % 9)
}
```

`makeMove` returns the captured piece code and updates both general locations
and an incremental deterministic Zobrist hash. `unmakeMove` restores those
values without allocating a board copy.

- [x] **Step 4: Write randomized legal-move equivalence tests**

Generate at least 40 deterministic legal positions from the initial state.
For both sides compare:

```kotlin
assertEquals(
    XiangqiRules.legalMoves(state, side).toSet(),
    position.legalMoves(side).map(::decodeSearchMove).toSet()
)
```

Also compare `isInCheck`, then make and unmake every sampled move and assert
the state and hash are restored.

- [x] **Step 5: Implement direct legal generation and check detection**

Generate rook and cannon rays only to their blocker boundaries, enforce horse
legs and elephant eyes, enforce palace/river rules, include flying-general
captures, and filter self-check with make/unmake. Provide
`generateLegalMoves(side, buffer, capturesOnly = false): Int`.

- [x] **Step 6: Run search-position and existing rule tests**

Run:

```bash
./gradlew :games:xiangqi:testDebugUnitTest --tests com.buddygames.xiangqi.XiangqiSearchPositionTest --tests com.buddygames.xiangqi.XiangqiRulesTest
```

Expected: PASS with exact legal-move equivalence.

### Task 2: Observable Iterative Search

**Files:**
- Create: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiSearchEngine.kt`
- Modify: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiAi.kt`
- Create: `games/xiangqi/src/test/java/com/buddygames/xiangqi/XiangqiSearchEngineTest.kt`

**Interfaces:**
- Consumes: `XiangqiSearchPosition`, `XiangqiAiLevelConfig`
- Produces: `XiangqiSearchStopReason`, `XiangqiSearchStats`, `XiangqiSearchResult`, `XiangqiAi.search(...)`; preserves `XiangqiAi.chooseMove(...)`

- [x] **Step 1: Write failing search-statistics tests**

```kotlin
@Test
fun searchReportsLastFullyCompletedDepth() {
    val result = XiangqiAi.search(
        XiangqiState.initial(),
        Side.RED,
        level = 3,
        limits = XiangqiSearchLimits(nodeBudget = 20_000, deadlineNanos = Long.MAX_VALUE)
    )

    assertNotNull(result.move)
    assertTrue(result.stats.completedDepth >= 2)
    assertTrue(result.stats.visitedNodes > 0)
    assertEquals(XiangqiSearchStopReason.COMPLETED, result.stats.stopReason)
}
```

Add separate tests for `NODE_BUDGET`, `DEADLINE`, and `CANCELLED`.

- [x] **Step 2: Run the focused test and verify RED**

Expected: compilation fails because the result and limit types do not exist.

- [x] **Step 3: Implement search results, limits, and compatibility wrapper**

```kotlin
internal enum class XiangqiSearchStopReason {
    COMPLETED, NODE_BUDGET, DEADLINE, CANCELLED
}

internal data class XiangqiSearchStats(
    val completedDepth: Int,
    val visitedNodes: Int,
    val elapsedMillis: Long,
    val stopReason: XiangqiSearchStopReason
)

internal data class XiangqiSearchResult(
    val move: XiangqiMove?,
    val stats: XiangqiSearchStats
)
```

Move iterative deepening and Negamax into `XiangqiSearchEngine`. Keep
`chooseMove` as `search(...).move`, and retain the last fully completed root
iteration on every stop condition.

- [x] **Step 4: Write a failing leaf-work regression**

Instrument only the internal engine context and assert a depth-one material
search evaluates leaves without sorting a second legal-move list. The expected
counter is one root generation plus terminal/check probes, not one sorted list
per leaf.

- [x] **Step 5: Implement reusable ply buffers and cached ordering scores**

Allocate move and score buffers once per search for every supported ply.
Compute an ordering score once per generated move. At depth zero evaluate
directly unless the side is in check or quiescence is enabled.

- [x] **Step 6: Run focused engine tests**

Run:

```bash
./gradlew :games:xiangqi:testDebugUnitTest --tests com.buddygames.xiangqi.XiangqiSearchEngineTest
```

Expected: PASS and deterministic stats for node-limited fixtures.

### Task 3: Direct Evaluation, Quiescence, and Transposition Table

**Files:**
- Modify: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiSearchPosition.kt`
- Modify: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiSearchEngine.kt`
- Modify: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiAi.kt`
- Modify: `games/xiangqi/src/test/java/com/buddygames/xiangqi/XiangqiSearchEngineTest.kt`
- Modify: `games/xiangqi/src/test/java/com/buddygames/xiangqi/XiangqiRulesTest.kt`

**Interfaces:**
- Produces: direct `evaluate(side, profile)`, bounded `XiangqiTranspositionTable`, and quiescence using the mutable position

- [x] **Step 1: Write failing evaluation-equivalence and tactical tests**

Use the existing protected-attacker, immediate-general-capture, checkmate, quiet
check-evasion, and poisoned-capture fixtures. Require the new engine to retain
their expected move choices. Add evaluation sign-symmetry:

```kotlin
assertEquals(
    position.evaluate(Side.RED, profile),
    -position.evaluate(Side.BLACK, profile)
)
```

- [x] **Step 2: Run focused tests and verify RED**

Expected: tests fail because direct evaluation and the new tactical search path
are incomplete.

- [x] **Step 3: Implement direct evaluation and bounded quiescence**

Calculate material and piece-square values in one board scan. Calculate
attacks, support, king shield, and approximate mobility without invoking
`XiangqiRules.legalMoves`. Quiescence searches captures and checks, plus all
legal evasions while in check, with make/unmake and its own depth bound.

- [x] **Step 4: Write failing transposition-table tests**

Test exact hash matching, depth replacement, lower/upper/exact bounds, best-move
retrieval, and that unmaking a move restores a reusable entry key.

- [x] **Step 5: Implement a fixed-size primitive transposition table**

Use power-of-two indexing and parallel primitive arrays for hash, depth, score,
bound, and best move. Enable it only for levels 6 through 10 and recreate it per
robot turn.

- [x] **Step 6: Run the complete Xiangqi unit suite**

Run:

```bash
./gradlew :games:xiangqi:testDebugUnitTest
```

Expected: all legacy and new tactical tests pass.

### Task 4: Effective-Depth Ladder and Calibration Harness

**Files:**
- Modify: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiAi.kt`
- Create: `games/xiangqi/src/test/java/com/buddygames/xiangqi/XiangqiAiCalibrationTest.kt`
- Modify: `docs/superpowers/specs/2026-07-18-xiangqi-intelligence-gradient-design.md`

**Interfaces:**
- Produces: recalibrated `XiangqiAiGradient` and deterministic calibration report

- [x] **Step 1: Write a failing compact depth-ladder test**

Use checked-in quiet opening, middlegame, tactical, and endgame fixtures with a
disabled deadline. Assert:

```kotlin
assertTrue(level5Depth > level4Depth)
assertTrue(level7Depth > level6Depth)
assertTrue(level10Depth >= 5)
assertTrue((1..10).zipWithNext().all { (a, b) -> medianDepth[a] <= medianDepth[b] })
```

- [x] **Step 2: Run the compact test and verify RED**

Expected: current configurations or search limits fail at least one effective
depth assertion.

- [x] **Step 3: Recalibrate immutable level configurations**

Keep the maximum think times at or below 3,000ms. Set depths, node budgets,
candidate pools, weakening percentages, evaluation profiles, and TT activation
from measured effective behavior. Restrict suboptimal selection to levels 1
through 5.

- [x] **Step 4: Implement the opt-in color-swapped match harness**

Add deterministic opening sequences, repeated-position detection, a fixed
maximum ply count, paired color swaps, and a text report containing wins,
draws, losses, score percentage, identical-move percentage, median depth,
median nodes, elapsed time, and stop-reason counts.

The normal test skips the long ladder unless:

```kotlin
System.getProperty("xiangqi.calibration") == "true"
```

- [x] **Step 5: Run compact calibration and affected-pair release calibration**

Run:

```bash
./gradlew :games:xiangqi:testDebugUnitTest --tests com.buddygames.xiangqi.XiangqiAiCalibrationTest
./gradlew :games:xiangqi:testDebugUnitTest --tests com.buddygames.xiangqi.XiangqiAiCalibrationTest -PxiangqiCalibration=true -PxiangqiCalibrationPair=1
```

Completed for pairs `1→2` through `5→6`, each with 100 color-swapped games and
65% to 75% higher-level score. Pairs `6→7` through `9→10` remain explicit
opt-in release audits because high-level pairs are hour-scale on the local JVM.

- [x] **Step 6: Record final parameters and calibration evidence**

Update the original gradient design table with the final immutable values and
add the calibration command, corpus size, result summary, and reference
environment. Do not claim professional-engine strength.

### Task 5: Package Integration, Version, and Verification

**Files:**
- Modify: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiPlugin.kt`
- Modify: `games/xiangqi/package/manifest.json`
- Modify: `games/xiangqi/src/test/java/com/buddygames/xiangqi/XiangqiRulesTest.kt`
- Modify: `AGENTS.md`
- Modify: `docs/superpowers/plans/2026-07-18-xiangqi-ai-search-calibration.md`

**Interfaces:**
- Preserves the existing Compose asynchronous call and `chooseMove` contract
- Produces Xiangqi package version `0.0.7`

- [x] **Step 1: Write the failing version-alignment test**

Change expected manifest values to:

```kotlin
assertEquals(7, XiangqiPlugin.manifest.versionCode)
assertEquals("0.0.7", XiangqiPlugin.manifest.versionName)
```

- [x] **Step 2: Run the version test and verify RED**

Expected: actual version remains `6` / `0.0.6`.

- [x] **Step 3: Update package versions and repository instructions**

Set code and JSON manifests to `7` / `0.0.7`. Update `AGENTS.md` to describe
the optimized mutable search position, observable effective-depth ladder, and
calibration gate. Mark completed plan checkboxes.

- [x] **Step 4: Run fresh verification**

Run:

```bash
./gradlew :games:xiangqi:testDebugUnitTest
npm run verify
git diff --check
```

Expected: both builds exit zero and no whitespace errors are reported.

- [x] **Step 5: Review and create a scoped local commit**

Inspect status, unstaged diff, staged diff, and recent log. Stage only the
Xiangqi implementation, tests, version files, `AGENTS.md`, and the two Xiangqi
AI documents. Exclude the unrelated `desgins/` directory. Commit without
pushing.
