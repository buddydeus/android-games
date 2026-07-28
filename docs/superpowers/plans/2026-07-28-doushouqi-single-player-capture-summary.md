# Doushouqi Single-Player Capture Summary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove pine-green/vermilion ownership copy from Doushouqi single-player UI and show the one piece captured by the most recent legal move.

**Architecture:** `DoushouqiSession` reads the destination occupant before every accepted human or robot move and projects it as nullable `lastCapturedPiece`. Snapshots retain that value so paired single-player undo restores the previous summary; restart clears it. Compose renders the projection directly instead of reconstructing captures from board differences.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, JUnit 4, Gradle Android unit tests.

## Global Constraints

- Change only the Doushouqi module plus synchronized design and project documentation.
- Increment Doushouqi from `versionCode = 4`, `versionName = 0.0.4` to `versionCode = 5`, `versionName = 0.0.5`; keep Kotlin and JSON manifests aligned.
- Keep shell version unchanged.
- Single-player side copy must use only `绿方` and `红方`; it must not render `松绿`, `朱砂`, or `玩家执`.
- `lastCapturedPiece` is either the one actual `DoushouqiPiece` removed by the most recent accepted move or `null`.
- Every accepted human or robot move replaces the prior capture summary; a non-capturing move sets it to `null`.
- Undo restores the capture summary from before the human move; restart clears it.
- Rejected and stale robot moves do not change the capture summary.
- Two-player score, side copy, session flow, and rail layout remain unchanged.
- Update `README.md`, `AGENTS.md`, and `games/doushouqi/README.md`.

---

### Task 1: Latest-Move Capture State

**Files:**
- Modify: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiSessionTest.kt`
- Modify: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiSession.kt`

**Interfaces:**
- Consumes: the destination occupant from `position.pieceAt(move.to)` before a legal move is applied.
- Produces: `DoushouqiSessionState.lastCapturedPiece: DoushouqiPiece?`.

- [ ] **Step 1: Write failing session tests**

Add these focused cases to `DoushouqiSessionTest`:

```kotlin
@Test
fun latestAcceptedMoveReplacesCapturedPieceSummary() {
    val humanCaptureState = stateOf(
        sideToMove = DoushouqiSide.PINE_GREEN,
        pos(4, 0) to green(DoushouqiAnimal.CAT),
        pos(3, 0) to red(DoushouqiAnimal.RAT),
        pos(1, 5) to red(DoushouqiAnimal.CAT),
    )
    val session = DoushouqiSession(
        DoushouqiMode.SINGLE_PLAYER,
        humanCaptureState,
    )

    val afterHuman = session.play(move(pos(4, 0), pos(3, 0)))
    assertEquals(red(DoushouqiAnimal.RAT), afterHuman.lastCapturedPiece)

    val request = requireNotNull(afterHuman.robotRequest)
    val quietRobotMove = DoushouqiRules.legalMoves(request.state)
        .first { request.state.pieceAt(it.to) == null }
    val afterRobot = session.applyRobotMove(request, quietRobotMove)

    assertNull(afterRobot.lastCapturedPiece)
}

@Test
fun robotCaptureBecomesLatestCapturedPiece() {
    val state = stateOf(
        sideToMove = DoushouqiSide.PINE_GREEN,
        pos(5, 0) to green(DoushouqiAnimal.CAT),
        pos(2, 2) to green(DoushouqiAnimal.RAT),
        pos(2, 1) to red(DoushouqiAnimal.CAT),
    )
    val session = DoushouqiSession(DoushouqiMode.SINGLE_PLAYER, state)
    val afterHuman = session.play(move(pos(5, 0), pos(4, 0)))
    val request = requireNotNull(afterHuman.robotRequest)

    val afterRobot = session.applyRobotMove(
        request,
        move(pos(2, 1), pos(2, 2)),
    )

    assertEquals(green(DoushouqiAnimal.RAT), afterRobot.lastCapturedPiece)
}

@Test
fun undoRestoresLatestCaptureAndRestartClearsIt() {
    val session = DoushouqiSession(
        DoushouqiMode.SINGLE_PLAYER,
        stateOf(
            sideToMove = DoushouqiSide.PINE_GREEN,
            pos(4, 0) to green(DoushouqiAnimal.CAT),
            pos(3, 0) to red(DoushouqiAnimal.RAT),
            pos(1, 5) to red(DoushouqiAnimal.CAT),
        ),
    )
    val captured = session.play(move(pos(4, 0), pos(3, 0)))
    assertNotNull(captured.lastCapturedPiece)

    assertNull(session.undo().lastCapturedPiece)
    session.play(move(pos(4, 0), pos(3, 0)))
    assertNull(session.restart().lastCapturedPiece)
}
```

Extend `staleRequestsAreRejectedAfterMoveUndoRestartAndInvalidate` to save `lastCapturedPiece` before each rejected request and assert it remains unchanged.

- [ ] **Step 2: Run the focused tests and verify RED**

Run:

```bash
./gradlew :games:doushouqi:testDebugUnitTest --tests com.buddygames.doushouqi.DoushouqiSessionTest
```

Expected: compilation fails because `lastCapturedPiece` does not exist.

- [ ] **Step 3: Implement minimal session state**

Make these minimal production changes:

```kotlin
data class DoushouqiSessionState(
    val position: DoushouqiState,
    val playerSide: DoushouqiSide,
    val score: DoushouqiScore,
    val historySize: Int,
    val generation: Long,
    val robotRequest: DoushouqiRobotRequest?,
    val lastCapturedPiece: DoushouqiPiece?,
) {
    val intelligenceLevel: DoushouqiAiLevel
        get() = score.intelligenceLevel
}

private data class DoushouqiSnapshot(
    val position: DoushouqiState,
    val score: DoushouqiScore,
    val lastCapturedPiece: DoushouqiPiece?,
)
```

Add `private var lastCapturedPiece: DoushouqiPiece? = null`. In both `play` and `applyRobotMove`, obtain `val captured = position.pieceAt(move.to)` only after the move is accepted as legal, then assign `lastCapturedPiece = captured` after applying it. Include the value in snapshots/projection, restore it in `undo`, and set it to `null` in `restart`. Do not mutate it on early-return paths or in `invalidate`.

- [ ] **Step 4: Run the focused tests and verify GREEN**

Run the Task 1 command again. Expected: all `DoushouqiSessionTest` tests pass.

- [ ] **Step 5: Commit session state**

```bash
git add games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiSession.kt \
  games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiSessionTest.kt
git commit -m "$(cat <<'EOF'
feat: track Doushouqi latest capture

- Project the piece removed by the most recent accepted move
- Restore capture state through undo and restart
EOF
)"
```

### Task 2: Single-Player Copy And Capture Row

**Files:**
- Modify: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiPluginTest.kt`
- Modify: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiScreen.kt`

**Interfaces:**
- Consumes: `DoushouqiSessionState.lastCapturedPiece`.
- Produces: `doushouqiSinglePlayerSideLabel(side)`, `doushouqiCapturedPieceLabel(piece)`, mode-aware result copy, and a single-player latest-capture row.

- [ ] **Step 1: Write failing copy tests**

Replace the player-identity assertions with:

```kotlin
@Test
fun singlePlayerCopyUsesOnlyRedAndGreenFactionNames() {
    assertEquals("绿方", doushouqiSinglePlayerSideLabel(DoushouqiSide.PINE_GREEN))
    assertEquals("红方", doushouqiSinglePlayerSideLabel(DoushouqiSide.VERMILION))
    assertEquals("红方鼠", doushouqiCapturedPieceLabel(red(DoushouqiAnimal.RAT)))
    assertEquals("绿方象", doushouqiCapturedPieceLabel(green(DoushouqiAnimal.ELEPHANT)))
}

@Test
fun resultCopyIsModeAware() {
    val result = DoushouqiResult.Win(
        DoushouqiSide.VERMILION,
        DoushouqiWinReason.DEN,
    )
    assertEquals("红方进入兽穴", doushouqiResultText(result, DoushouqiMode.SINGLE_PLAYER))
    assertEquals("朱砂方进入兽穴", doushouqiResultText(result, DoushouqiMode.TWO_PLAYERS))
}
```

- [ ] **Step 2: Run the focused tests and verify RED**

Run:

```bash
./gradlew :games:doushouqi:testDebugUnitTest --tests com.buddygames.doushouqi.DoushouqiPluginTest
```

Expected: compilation fails because the new helpers do not exist.

- [ ] **Step 3: Implement the minimal rail**

Remove `doushouqiPlayerSideLine` and its single-player rail call. Render `最近一步吃子` plus `无` or one compact faction-colored text tag from `session.lastCapturedPiece`. Add:

```kotlin
internal fun doushouqiSinglePlayerSideLabel(side: DoushouqiSide): String =
    if (side == DoushouqiSide.PINE_GREEN) "绿方" else "红方"

internal fun doushouqiCapturedPieceLabel(piece: DoushouqiPiece): String =
    "${doushouqiSinglePlayerSideLabel(piece.side)}${piece.animal.label}"

internal fun doushouqiResultText(
    result: DoushouqiResult?,
    mode: DoushouqiMode,
): String? = when (result) {
    is DoushouqiResult.Win -> "${
        if (mode == DoushouqiMode.SINGLE_PLAYER) {
            doushouqiSinglePlayerSideLabel(result.winner)
        } else {
            doushouqiFullSideLabel(result.winner)
        }
    }${
        when (result.reason) {
            DoushouqiWinReason.DEN -> "进入兽穴"
            DoushouqiWinReason.FINAL_CAPTURE -> "吃光对方"
            DoushouqiWinReason.NO_LEGAL_MOVE -> "胜 · 对方无棋可走"
        }
    }"
    is DoushouqiResult.Draw -> when (result.reason) {
        DoushouqiDrawReason.REPETITION -> "三次重复和棋"
        DoushouqiDrawReason.QUIET_100 -> "连续百步未吃子和棋"
    }
    null -> null
}
```

Keep `doushouqiFullSideLabel` only for two-player result copy. Keep both horizontal dividers and existing button geometry.

- [ ] **Step 4: Run focused and module tests**

Run:

```bash
./gradlew :games:doushouqi:testDebugUnitTest
```

Expected: all Doushouqi unit tests pass.

- [ ] **Step 5: Commit single-player rail copy**

```bash
git add games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiScreen.kt \
  games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiPluginTest.kt
git commit -m "$(cat <<'EOF'
feat: show Doushouqi latest capture

- Remove player-side ownership copy from single-player rails
- Render red and green latest-capture labels
EOF
)"
```

### Task 3: Release, Documentation, And Integration

**Files:**
- Modify: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiManifestTest.kt`
- Modify: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiManifest.kt`
- Modify: `games/doushouqi/package/manifest.json`
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `games/doushouqi/README.md`

**Interfaces:**
- Produces: aligned Doushouqi `0.0.5` package metadata and human-facing documentation.

- [ ] **Step 1: Write the failing manifest expectation**

Change the manifest assertions to:

```kotlin
assertEquals(5, manifest.versionCode)
assertEquals("0.0.5", manifest.versionName)
```

Run:

```bash
./gradlew :games:doushouqi:testDebugUnitTest \
  --tests com.buddygames.doushouqi.DoushouqiManifestTest
```

Expected: FAIL because the production manifest still reports `4` / `0.0.4`.

- [ ] **Step 2: Align both manifest sources**

Update the Kotlin manifest constants and package JSON to `5` / `0.0.5`.

- [ ] **Step 3: Synchronize documentation**

Document red/green-only single-player copy, removal of player-side ownership text, latest-move captured-piece replacement, undo/restart behavior, and the new version in all three required docs. Add the implementation plan and runtime acceptance report to the root document maps.

- [ ] **Step 4: Run final verification**

Run:

```bash
./gradlew :games:doushouqi:testDebugUnitTest
npm run verify
git diff --check
```

Expected: both Gradle commands report `BUILD SUCCESSFUL` and `git diff --check` produces no output.

- [ ] **Step 5: Run Android 36 landscape acceptance**

Launch with `npm start`, open Doushouqi single-player mode, and verify:

- initial rail shows `最近一步吃子` and `无`;
- no `松绿`, `朱砂`, or `玩家执` text exists in the single-player semantics tree;
- after a capturing move the row shows exactly one label such as `红方鼠`;
- after the next non-capturing move the row returns to `无`;
- the board and both horizontal rail dividers remain visible.

Save screenshots under `build/runtime-acceptance/` and record evidence in `docs/superpowers/reports/2026-07-28-doushouqi-single-player-capture-summary-runtime-acceptance.md`.

- [ ] **Step 6: Commit release metadata and evidence**

```bash
git add AGENTS.md README.md designs/images/doushouqi-ui-game-tablet.md \
  designs/specs/doushouqi-ui.md \
  docs/superpowers/plans/2026-07-28-doushouqi-single-player-capture-summary.md \
  docs/superpowers/reports/2026-07-28-doushouqi-single-player-capture-summary-runtime-acceptance.md \
  games/doushouqi/README.md games/doushouqi/package/manifest.json \
  games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiManifest.kt \
  games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiManifestTest.kt
git commit -m "$(cat <<'EOF'
docs: verify Doushouqi capture summary

- Release Doushouqi 0.0.5 with synchronized documentation
- Record automated and Android runtime acceptance
EOF
)"
```
