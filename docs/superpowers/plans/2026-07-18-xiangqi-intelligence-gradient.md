# Xiangqi Intelligence Gradient Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the Xiangqi one-step heuristic robot with a compact ten-level offline alpha-beta engine whose active level is the human player's accumulated wins plus one, capped at 10.

**Architecture:** Keep `XiangqiRules` authoritative for move legality, add a focused `XiangqiAi` search boundary, and keep score/level derivation in `XiangqiSession`. `XiangqiPlugin` launches robot searches off the Compose UI thread and applies only results that still match the active position generation.

**Tech Stack:** Kotlin, Jetpack Compose runtime effects, JUnit 4, existing Android library packaging; no new dependencies or native libraries.

## Global Constraints

- Xiangqi remains entirely inside `games/xiangqi/`.
- Use one pure-Kotlin engine for levels 1 through 10.
- Level is `min(playerScore + 1, 10)`.
- Single-player score is player versus robot across side swaps; two-player score remains red versus black.
- Levels are deterministic for a given position and level.
- Search depth and node budgets are monotonic.
- Preserve legal-move safety, immediate general capture, checkmate preference, poisoned-capture avoidance, undo, restart, last-move, and black-side rotation.
- Increment Xiangqi from `0.0.5` / `5` to `0.0.6` / `6` in plugin code and package manifest.
- Run `./gradlew :games:xiangqi:testDebugUnitTest`, then `npm run verify`.
- Update `AGENTS.md` for every repository file change.

---

### Task 1: Mode-Aware Score And Intelligence Configuration

**Files:**
- Modify: `games/xiangqi/src/test/java/com/buddygames/xiangqi/XiangqiRulesTest.kt`
- Modify: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiSession.kt`
- Create: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiAi.kt`
- Modify: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiPlugin.kt`

**Interfaces:**
- Produces: `XiangqiScore.record(winner, mode, playerSide)`, `XiangqiScore.displayText(mode)`, `XiangqiScore.intelligenceLevel`, `XiangqiAiGradient.config(level)`.
- Consumes: existing `Side`, `GameMode`, and `XiangqiMove`.

- [ ] **Step 1: Write failing score and gradient tests**

Add tests proving player/robot scoring survives side swaps, two-player scoring
remains color-based, levels clamp from 1 through 10, and all budgets are
monotonic:

```kotlin
@Test
fun singlePlayerScoreFollowsPlayerIdentityAcrossSideSwaps() {
    val score = XiangqiScore()
        .record(Side.RED, GameMode.SINGLE_PLAYER, Side.RED)
        .record(Side.RED, GameMode.SINGLE_PLAYER, Side.BLACK)
        .record(Side.BLACK, GameMode.SINGLE_PLAYER, Side.BLACK)

    assertEquals(2, score.player)
    assertEquals(1, score.robot)
    assertEquals("2 : 1", score.displayText(GameMode.SINGLE_PLAYER))
    assertEquals(3, score.intelligenceLevel)
}

@Test
fun intelligenceLevelClampsAtTenAndBudgetsAreMonotonic() {
    assertEquals(1, XiangqiScore().intelligenceLevel)
    assertEquals(10, XiangqiScore(player = 20).intelligenceLevel)
    val configs = (1..10).map(XiangqiAiGradient::config)
    assertEquals((1..10).toList(), configs.map { it.level })
    assertTrue(configs.zipWithNext().all { (a, b) ->
        a.maxDepth <= b.maxDepth && a.nodeBudget <= b.nodeBudget
    })
}
```

- [ ] **Step 2: Run the focused tests and verify RED**

Run:

```bash
./gradlew :games:xiangqi:testDebugUnitTest
```

Expected: compilation fails because mode-aware scoring, identity fields,
`intelligenceLevel`, and `XiangqiAiGradient` do not exist.

- [ ] **Step 3: Implement the minimal score and configuration APIs**

Use one score object with explicit identity and color counters:

```kotlin
internal data class XiangqiScore(
    val red: Int = 0,
    val black: Int = 0,
    val player: Int = 0,
    val robot: Int = 0
) {
    val intelligenceLevel: Int get() = (player + 1).coerceAtMost(10)

    fun displayText(mode: GameMode): String =
        if (mode == GameMode.SINGLE_PLAYER) "$player : $robot" else "$red : $black"

    fun record(winner: Side?, mode: GameMode, playerSide: Side): XiangqiScore =
        when {
            winner == null -> this
            mode == GameMode.SINGLE_PLAYER && winner == playerSide -> copy(player = player + 1)
            mode == GameMode.SINGLE_PLAYER -> copy(robot = robot + 1)
            winner == Side.RED -> copy(red = red + 1)
            else -> copy(black = black + 1)
        }
}
```

Create immutable AI level configuration with the ten exact depth, node,
candidate-pool, error-rate, and evaluation-profile rows from the design spec.
Add safety deadlines of 80, 100, 150, 250, 400, 650, 1,000, 1,500, 2,200,
and 3,000 milliseconds respectively. Reject levels outside `1..10`.

Update existing plugin call sites to use
`score.record(winner, mode, playerSide)` and `score.displayText(mode)` so the
module stays compilable after the API change.

- [ ] **Step 4: Run tests and verify GREEN**

Run `./gradlew :games:xiangqi:testDebugUnitTest`.

Expected: score and gradient tests pass and the full Xiangqi test module
compiles.

---

### Task 2: Search-Friendly Legal Move Generation And Alpha-Beta Engine

**Files:**
- Modify: `games/xiangqi/src/test/java/com/buddygames/xiangqi/XiangqiRulesTest.kt`
- Modify: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiRules.kt`
- Modify: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiAi.kt`

**Interfaces:**
- Consumes: `XiangqiRules.legalMoves`, `XiangqiState.apply`, `XiangqiAiGradient.config`.
- Produces: `XiangqiAi.chooseMove(state, side, level, shouldStop): XiangqiMove?`.

- [ ] **Step 1: Write failing AI contract tests**

Replace old `XiangqiRules.robotMove` assertions with `XiangqiAi.chooseMove` and
add determinism, legality, configuration, and cancellation coverage:

```kotlin
@Test
fun everyLevelCapturesAnExposedGeneral() {
    val state = XiangqiState.empty()
        .put(5, 4, XiangqiPiece(Side.RED, PieceType.ROOK))
        .put(0, 4, XiangqiPiece(Side.BLACK, PieceType.GENERAL))
    (1..10).forEach { level ->
        assertEquals(
            XiangqiMove(5, 4, 0, 4),
            XiangqiAi.chooseMove(state, Side.RED, level)
        )
    }
}

@Test
fun weakenedSelectionIsDeterministicAndLegal() {
    val state = XiangqiState.initial()
    val first = XiangqiAi.chooseMove(state, Side.RED, 1)
    val second = XiangqiAi.chooseMove(state, Side.RED, 1)
    assertEquals(first, second)
    assertTrue(first != null && XiangqiRules.isLegalMove(state, first, Side.RED))
}
```

Keep the existing checkmate-over-capture and poisoned-capture positions, but
call levels 7 and 10 respectively through `XiangqiAi`.

- [ ] **Step 2: Run tests and verify RED**

Run `./gradlew :games:xiangqi:testDebugUnitTest`.

Expected: compilation fails because `XiangqiAi.chooseMove` is not implemented.

- [ ] **Step 3: Replace brute-force destination enumeration**

Generate candidate destinations by piece:

- rook/cannon: four orthogonal rays, respecting the first blocker and cannon
  screen;
- horse: eight offsets;
- elephant: four diagonal two-step offsets;
- advisor: four diagonal one-step offsets;
- general: four palace neighbors plus opposing-general file;
- soldier: forward plus sideways after crossing the river.

Pass generated candidates through the existing `isLegalMove` filter, keeping
self-check behavior unchanged. Remove the nested loop over all 90 destination
squares.

- [ ] **Step 4: Implement minimal iterative Negamax**

Implement:

```kotlin
internal object XiangqiAi {
    fun chooseMove(
        state: XiangqiState,
        side: Side,
        level: Int,
        shouldStop: () -> Boolean = { false }
    ): XiangqiMove?
}
```

Search depth 1 upward to the configured cap. Count every entered Negamax node,
stop at the configured node budget or `shouldStop()`, and publish root results
only after a full iteration. Order immediate wins, captures, checks, and quiet
moves. At a terminal node return mate score adjusted by ply; otherwise evaluate
material and the enabled positional profile. Levels 8–10 extend forcing
captures/checks with bounded quiescence.

At root, levels 1–6 use a stable board hash plus level to make the configured
candidate-pool/error-rate choice reproducible. Always override weakening for an
immediate general capture and reject candidates allowing immediate general
capture when a safe alternative exists.

Delete `XiangqiRules.robotMove`, `robotMoveScore`,
`immediateReplyThreat`, and their robot-only constants.

- [ ] **Step 5: Run tests and verify GREEN**

Run `./gradlew :games:xiangqi:testDebugUnitTest`.

Expected: all rules and AI regression tests pass.

---

### Task 3: Asynchronous Single-Player Flow And Intelligence UI

**Files:**
- Modify: `games/xiangqi/src/test/java/com/buddygames/xiangqi/XiangqiRulesTest.kt`
- Modify: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiSession.kt`
- Modify: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiPlugin.kt`
- Modify: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiVisuals.kt`

**Interfaces:**
- Consumes: `XiangqiScore.intelligenceLevel`, `XiangqiAi.chooseMove`.
- Produces: `needsXiangqiRobotTurn`, `xiangqiIntelligenceLabel`, cancellable
  background robot turns.

- [ ] **Step 1: Write failing session and UI helper tests**

```kotlin
@Test
fun blackPlayerRoundBeginsWithPendingRedRobotTurn() {
    val round = newXiangqiRound(Side.BLACK)
    assertEquals(XiangqiState.initial(), round.state)
    assertEquals(Side.RED, round.turn)
    assertTrue(needsXiangqiRobotTurn(GameMode.SINGLE_PLAYER, round.turn, round.playerSide, null))
}

@Test
fun intelligenceLabelOnlyFormatsValidLevels() {
    assertEquals("智能等级 1", xiangqiIntelligenceLabel(1))
    assertEquals("智能等级 10", xiangqiIntelligenceLabel(10))
}
```

Add score-record and undo assertions proving snapshots restore player and robot
scores.

- [ ] **Step 2: Run tests and verify RED**

Run `./gradlew :games:xiangqi:testDebugUnitTest`.

Expected: assertions fail because black rounds still calculate a synchronous
opening and the label/helper do not exist.

- [ ] **Step 3: Make round setup and scoring mode-aware**

`newXiangqiRound` returns the untouched initial state and sets `turn` to red.
Add:

```kotlin
internal fun needsXiangqiRobotTurn(
    mode: GameMode,
    turn: Side,
    playerSide: Side,
    winner: Side?
): Boolean = mode == GameMode.SINGLE_PLAYER && winner == null && turn != playerSide
```

Record results with `score.record(winner, mode, playerSide)`. Render
`score.displayText(mode)`.

- [ ] **Step 4: Move robot calculation off the UI thread**

Represent each requested turn with state, robot side, level, and an increasing
generation. Use `LaunchedEffect(request)` and `withContext(Dispatchers.Default)`
to call `XiangqiAi.chooseMove`. Pass a cancellation callback tied to coroutine
activity. Apply the result only when its generation and state still match.

Human input is rejected while `needsXiangqiRobotTurn` is true. Undo, restart,
and disposal increment the generation or clear the request so stale results
cannot mutate the board. After a human move, set `turn` to the robot side and
let the effect perform the reply. A black-player restart schedules the opening
through the same path, outside undo history.

- [ ] **Step 5: Display the active intelligence level**

Extend `XiangqiGameLayout` and `XiangqiInfoRail` with
`intelligenceLevel: Int?`. Single-player passes `score.intelligenceLevel`;
two-player passes `null`. Render `智能等级 N` under the score and preserve the
existing `将军`, undo, restart, and exit controls.

- [ ] **Step 6: Run tests and verify GREEN**

Run `./gradlew :games:xiangqi:testDebugUnitTest`.

Expected: all session, UI helper, rules, and AI tests pass.

---

### Task 4: Version, Documentation, And Full Verification

**Files:**
- Modify: `games/xiangqi/src/test/java/com/buddygames/xiangqi/XiangqiRulesTest.kt`
- Modify: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiPlugin.kt`
- Modify: `games/xiangqi/package/manifest.json`
- Modify: `AGENTS.md`
- Modify: `docs/superpowers/plans/2026-07-18-xiangqi-intelligence-gradient.md`

**Interfaces:**
- Consumes: completed behavior from Tasks 1–3.
- Produces: aligned package metadata and verified repository handoff.

- [ ] **Step 1: Write the failing version test**

Change the expected version to:

```kotlin
assertEquals(6, XiangqiPlugin.manifest.versionCode)
assertEquals("0.0.6", XiangqiPlugin.manifest.versionName)
assertEquals("版本 0.0.6", xiangqiVersionLabel(XiangqiPlugin.manifest.versionName))
```

- [ ] **Step 2: Run the version test and verify RED**

Run `./gradlew :games:xiangqi:testDebugUnitTest`.

Expected: version assertions report `5` / `0.0.5`.

- [ ] **Step 3: Align both version sources**

Set `XiangqiPlugin.manifest` and `games/xiangqi/package/manifest.json` to
`versionCode = 6` and `versionName = 0.0.6`.

Update `AGENTS.md` only if implementation details differ from the approved
description. Mark completed plan checkboxes without changing requirements.

- [ ] **Step 4: Run targeted verification**

Run:

```bash
./gradlew :games:xiangqi:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Run the repository gate**

Run:

```bash
npm run verify
```

Expected: all unit tests, three game packages, and the debug APK build
successfully.

- [ ] **Step 6: Inspect and commit**

Inspect `git status`, unstaged/staged diffs, and recent log. Exclude
`desgins/`, build outputs, local properties, and credentials. Commit the
implementation with a scoped structured message and report the remaining
working tree.
