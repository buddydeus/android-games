# Doushouqi Two-Player Immediate Capture Summary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Doushouqi two-player capture copy reflect the latest accepted move immediately while preserving single-player completed-round aggregation.

**Architecture:** Keep `DoushouqiRoundCaptures` and the shared Compose renderer unchanged. Branch only inside `DoushouqiSession.play`: two-player moves publish a fresh zero-or-one-capture summary immediately, while single-player moves continue through nullable pending state until the robot reply.

**Tech Stack:** Kotlin, immutable Doushouqi session snapshots, Jetpack Compose, JUnit 4, Android Gradle tasks.

## Global Constraints

- Single-player player-plus-robot completed-round behavior must not change.
- Every accepted two-player move immediately replaces the visible capture summary.
- A two-player quiet move must immediately display `无吃子`.
- A two-player capture must display only `绿方吃：<兽名>` or `红方吃：<兽名>` for the latest attacker.
- Two-player state must never merge captures from consecutive moves.
- Illegal moves preserve the current summary; undo restores the summary before the undone move; restart clears it.
- Keep the shared rail renderer, rules, AI, score flow, board assets, layout geometry, shell, and `game-api` unchanged.
- Increment only Doushouqi from version code `6`, version name `0.0.6`, to version code `7`, version name `0.0.7`.
- Update root `README.md`, `AGENTS.md`, `games/doushouqi/README.md`, visual SSOT, design status, and runtime evidence.
- Run Doushouqi unit tests and `npm run verify` before completion.

---

### Task 1: Publish Every Two-Player Move Immediately

**Files:**
- Modify: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiSessionTest.kt`
- Modify: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiSession.kt`

**Interfaces:**
- Consumes: `DoushouqiRoundCaptures.recordCapture(attacker, captured)`.
- Produces: immediate two-player `lastCompletedRoundCaptures`.
- Preserves: nullable single-player `pendingRoundCaptures`.

- [ ] **Step 1: Replace merged two-player tests with failing immediate tests**

Replace `twoPlayerPublishesOnlyAfterRedCompletesRound` with:

```kotlin
@Test
fun twoPlayerGreenCapturePublishesImmediately() {
    val session = DoushouqiSession(
        DoushouqiMode.TWO_PLAYERS,
        pairedCaptureState(),
    )

    val afterGreen = session.play(move(pos(4, 0), pos(3, 0)))

    assertEquals(
        DoushouqiRoundCaptures(
            capturedByGreen = red(DoushouqiAnimal.RAT),
        ),
        afterGreen.lastCompletedRoundCaptures,
    )
}
```

Add:

```kotlin
@Test
fun twoPlayerQuietMoveImmediatelyClearsPreviousCapture() {
    val session = DoushouqiSession(
        DoushouqiMode.TWO_PLAYERS,
        pairedCaptureState(),
    )
    session.play(move(pos(4, 0), pos(3, 0)))

    val afterQuietRed = session.play(move(pos(2, 1), pos(1, 1)))

    assertEquals(
        DoushouqiRoundCaptures(),
        afterQuietRed.lastCompletedRoundCaptures,
    )
}

@Test
fun twoPlayerRedCaptureReplacesGreenCaptureWithoutMerging() {
    val session = DoushouqiSession(
        DoushouqiMode.TWO_PLAYERS,
        pairedCaptureState(),
    )
    session.play(move(pos(4, 0), pos(3, 0)))

    val afterRed = session.play(move(pos(2, 1), pos(2, 2)))

    assertEquals(
        DoushouqiRoundCaptures(
            capturedByRed = green(DoushouqiAnimal.RAT),
        ),
        afterRed.lastCompletedRoundCaptures,
    )
}
```

Replace `twoPlayerUndoOfRedMoveRestoresPendingGreenHalfRound` with:

```kotlin
@Test
fun twoPlayerUndoRestoresSummaryBeforeLatestMove() {
    val session = DoushouqiSession(
        DoushouqiMode.TWO_PLAYERS,
        pairedCaptureState(),
    )
    session.play(move(pos(4, 0), pos(3, 0)))
    session.play(move(pos(2, 1), pos(1, 1)))

    val afterUndo = session.undo()

    assertEquals(
        DoushouqiRoundCaptures(
            capturedByGreen = red(DoushouqiAnimal.RAT),
        ),
        afterUndo.lastCompletedRoundCaptures,
    )
}
```

Extend `twoPlayerRestartAlwaysReturnsToGreenFirst` with:

```kotlin
assertEquals(
    DoushouqiRoundCaptures(),
    restarted.lastCompletedRoundCaptures,
)
```

Add an illegal move assertion after a published Green capture:

```kotlin
@Test
fun illegalTwoPlayerMovePreservesLatestCapture() {
    val session = DoushouqiSession(
        DoushouqiMode.TWO_PLAYERS,
        pairedCaptureState(),
    )
    val afterGreen = session.play(move(pos(4, 0), pos(3, 0)))

    val unchanged = session.play(move(pos(3, 0), pos(2, 0)))

    assertEquals(afterGreen, unchanged)
}
```

The attempted second move uses the wrong side's Green piece while Red is to
move, so `DoushouqiRules.apply` rejects it.

- [ ] **Step 2: Run session tests and verify RED**

Run:

```bash
./gradlew :games:doushouqi:testDebugUnitTest \
  --tests com.buddygames.doushouqi.DoushouqiSessionTest
```

Expected failures:

- `twoPlayerGreenCapturePublishesImmediately` sees an empty summary;
- `twoPlayerRedCaptureReplacesGreenCaptureWithoutMerging` sees both slots;
- `twoPlayerUndoRestoresSummaryBeforeLatestMove` restores an empty completed
  summary under the old pending-round policy.

- [ ] **Step 3: Implement mode-specific publishing**

In `DoushouqiSession.play`, replace the current shared `nextPending` and
`completesRound` block with:

```kotlin
if (mode == DoushouqiMode.TWO_PLAYERS) {
    lastCompletedRoundCaptures =
        DoushouqiRoundCaptures().recordCapture(attacker, captured)
    pendingRoundCaptures = null
} else {
    val nextPending =
        DoushouqiRoundCaptures().recordCapture(attacker, captured)
    pendingRoundCaptures = nextPending
    if (next.result != null) {
        lastCompletedRoundCaptures = nextPending
        pendingRoundCaptures = null
    }
}
```

Do not change `applyRobotMove`: it remains the single-player second-move
publisher. Do not change snapshots, `undo`, `restart`, or Compose.

- [ ] **Step 4: Run session and full module tests**

Run:

```bash
./gradlew :games:doushouqi:testDebugUnitTest \
  --tests com.buddygames.doushouqi.DoushouqiSessionTest
./gradlew :games:doushouqi:testDebugUnitTest
```

Expected: both commands finish with `BUILD SUCCESSFUL`, including all unchanged
single-player completed-round tests.

- [ ] **Step 5: Commit the behavior**

```bash
git add \
  games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiSession.kt \
  games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiSessionTest.kt
git commit -m "$(cat <<'EOF'
feat: publish Doushouqi two-player captures immediately

- Replace the two-player summary after every accepted move
- Preserve single-player completed-round aggregation and undo behavior
EOF
)"
```

---

### Task 2: Release Doushouqi 0.0.7 and Align Documentation

**Files:**
- Modify: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiManifestTest.kt`
- Modify: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiManifest.kt`
- Modify: `games/doushouqi/package/manifest.json`
- Modify: `games/doushouqi/README.md`
- Modify: `designs/specs/doushouqi-ui.md`
- Modify: `designs/images/doushouqi-ui-game-tablet.md`
- Modify: `docs/superpowers/specs/2026-07-28-doushouqi-two-player-immediate-capture-summary-design.md`
- Modify: `README.md`
- Modify: `AGENTS.md`

**Interfaces:**
- Produces: Doushouqi package version code `7`, version name `0.0.7`.
- Documents: single-player completed rounds and two-player latest-move summaries.

- [ ] **Step 1: Make the manifest test require 0.0.7**

Change:

```kotlin
assertEquals(7, manifest.versionCode)
assertEquals("0.0.7", manifest.versionName)
```

- [ ] **Step 2: Run the manifest test and verify RED**

Run:

```bash
./gradlew :games:doushouqi:testDebugUnitTest \
  --tests com.buddygames.doushouqi.DoushouqiManifestTest
```

Expected: `packageIdentityMatchesReferenceArtworkRelease` fails because the
production manifest still reports code `6`.

- [ ] **Step 3: Update both version sources**

Set in `DoushouqiManifest.kt`:

```kotlin
internal const val DOUSHOUQI_VERSION_CODE = 7
internal const val DOUSHOUQI_VERSION_NAME = "0.0.7"
```

Set matching JSON:

```json
"versionCode": 7,
"versionName": "0.0.7"
```

- [ ] **Step 4: Align current-behavior documentation**

Update root and game README version references to `0.0.7`. Replace statements
that both modes show completed rounds with:

```text
单人模式在“玩家 + 智能”完整轮结束后发布最多两枚吃子；
双人模式每次合法走棋后立即替换为该步的一枚吃子，空走显示“无吃子”。
```

Update `AGENTS.md` with the same exact lifecycle and version. Update
`designs/specs/doushouqi-ui.md` so its rail contract distinguishes the two
modes. Keep the single-player two-line mockup valid, but add the two-player
latest-move rule to `designs/images/doushouqi-ui-game-tablet.md`.

Change the new design status to:

```markdown
**Status:** Approved and implemented.
```

Add this plan to root `README.md`, `AGENTS.md`, and `games/doushouqi/README.md`.
Keep version `0.0.6` documents as historical evidence.

- [ ] **Step 5: Run manifest and module tests**

Run:

```bash
./gradlew :games:doushouqi:testDebugUnitTest \
  --tests com.buddygames.doushouqi.DoushouqiManifestTest
./gradlew :games:doushouqi:testDebugUnitTest
git diff --check
```

Expected: both Gradle commands finish with `BUILD SUCCESSFUL`; diff check prints
nothing.

- [ ] **Step 6: Commit the release**

```bash
git add \
  AGENTS.md \
  README.md \
  designs/images/doushouqi-ui-game-tablet.md \
  designs/specs/doushouqi-ui.md \
  docs/superpowers/plans/2026-07-28-doushouqi-two-player-immediate-capture-summary.md \
  docs/superpowers/specs/2026-07-28-doushouqi-two-player-immediate-capture-summary-design.md \
  games/doushouqi/README.md \
  games/doushouqi/package/manifest.json \
  games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiManifest.kt \
  games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiManifestTest.kt
git commit -m "$(cat <<'EOF'
chore: release Doushouqi 0.0.7

- Align package manifests with immediate two-player capture summaries
- Update game, design, and repository documentation
EOF
)"
```

---

### Task 3: Verify and Record Android Two-Player Behavior

**Files:**
- Create: `docs/superpowers/reports/2026-07-28-doushouqi-two-player-immediate-capture-summary-runtime-acceptance.md`
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `games/doushouqi/README.md`

**Interfaces:**
- Verifies: packaged Doushouqi version `0.0.7`.
- Records: immediate capture display followed by immediate quiet-move clearing.

- [ ] **Step 1: Run the full repository gate**

Run:

```bash
./gradlew :games:doushouqi:testDebugUnitTest
npm run verify
git diff --check
unzip -p build/game-packages/doushouqi.zip manifest.json |
  rg '"version(Code|Name)": (7|"0.0.7")'
```

Expected: both Gradle invocations finish with `BUILD SUCCESSFUL`, full verify
reports `178 actionable tasks`, diff check prints nothing, and the zip manifest
prints code `7` plus name `0.0.7`.

- [ ] **Step 2: Validate immediate two-player publishing on Android 36**

Run:

```bash
npm start
```

Open Doushouqi two-player mode. Move Green to capture one Red piece and verify
the rail immediately shows only:

```text
绿方吃：<兽名>
```

Capture:

```text
build/runtime-acceptance/doushouqi-two-player-immediate-capture.png
```

Make a legal non-capturing Red move and verify the rail immediately shows:

```text
无吃子
```

Capture:

```text
build/runtime-acceptance/doushouqi-two-player-quiet-clears-capture.png
```

Confirm no second faction line remains after either move.

- [ ] **Step 3: Write and link runtime evidence**

Record the exact AVD, resolution, semantic-tree labels, move sequence,
screenshots, verification commands, and verdict in the report. Link it from
root `README.md`, `AGENTS.md`, and `games/doushouqi/README.md`.

- [ ] **Step 4: Validate and commit the report**

Run:

```bash
git diff --check
git status --short
```

Commit with a strict stopping chain:

```bash
git add \
  AGENTS.md \
  README.md \
  games/doushouqi/README.md \
  docs/superpowers/reports/2026-07-28-doushouqi-two-player-immediate-capture-summary-runtime-acceptance.md &&
git diff --staged --check &&
git commit -m "$(cat <<'EOF'
docs: record immediate Doushouqi capture acceptance

- Capture Android evidence for two-player capture and quiet-move replacement
- Link the runtime report from repository documentation
EOF
)"
```
