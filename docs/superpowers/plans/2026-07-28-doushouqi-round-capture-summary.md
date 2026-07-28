# Doushouqi Round Capture Summary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the latest-move capture row with an atomically published, most-recent-completed-round capture summary in both Doushouqi game modes.

**Architecture:** `DoushouqiSession` owns a public immutable completed summary and a private nullable pending summary. Accepted first moves populate pending state without changing the rail; accepted second moves publish the whole round, while first-move terminal results publish early. Compose consumes a small ordered label projection and never infers captures from board differences.

**Tech Stack:** Kotlin, immutable Doushouqi state/session model, Jetpack Compose, JUnit 4, Android Gradle tasks.

## Global Constraints

- A single-player normal round is the player's accepted move followed by the accepted robot reply.
- A two-player normal round is Green's accepted move followed by Red's accepted reply.
- Keep the previous completed summary visible while a normal round is incomplete.
- Publish a first-move terminal round immediately because no reply can occur.
- A single-player robot opening is outside a player-plus-robot round and does not publish a summary.
- Show `绿方吃：<兽名>` and `红方吃：<兽名>` in Green-then-Red order; show `无吃子` when neither side captured.
- Keep single-player faction copy limited to `绿方` / `红方`; preserve two-player score and result copy.
- Do not change `game-api`, shell code, rules, AI selection, board assets, or rail geometry.
- Increment only Doushouqi from version `0.0.5` / code `5` to `0.0.6` / code `6`.
- Update root `README.md`, `AGENTS.md`, `games/doushouqi/README.md`, visual SSOT, and design status for the changed behavior.
- Run the Doushouqi unit tests and `npm run verify` before completion.

---

### Task 1: Model Completed and Pending Round Captures

**Files:**
- Modify: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiSessionTest.kt`
- Modify: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiSession.kt`

**Interfaces:**
- Produces: `DoushouqiRoundCaptures(capturedByGreen, capturedByRed)`.
- Produces: `DoushouqiSessionState.lastCompletedRoundCaptures`.
- Keeps private: `pendingRoundCaptures: DoushouqiRoundCaptures?`.
- Preserves: generation-bound robot requests and existing single-player/two-player undo depth.

- [ ] **Step 1: Replace latest-move tests with failing completed-round tests**

Remove the four tests that assert `lastCapturedPiece`. Add focused tests using
the existing `stateOf`, `green`, `red`, `pos`, and `move` fixtures:

```kotlin
@Test
fun singlePlayerPublishesPlayerAndRobotCapturesOnlyAfterRobotReply() {
    val session = DoushouqiSession(
        DoushouqiMode.SINGLE_PLAYER,
        stateOf(
            sideToMove = DoushouqiSide.PINE_GREEN,
            pos(4, 0) to green(DoushouqiAnimal.CAT),
            pos(3, 0) to red(DoushouqiAnimal.RAT),
            pos(2, 2) to green(DoushouqiAnimal.RAT),
            pos(2, 1) to red(DoushouqiAnimal.CAT),
        ),
    )

    val afterPlayer = session.play(move(pos(4, 0), pos(3, 0)))
    assertEquals(DoushouqiRoundCaptures(), afterPlayer.lastCompletedRoundCaptures)

    val afterRobot = session.applyRobotMove(
        requireNotNull(afterPlayer.robotRequest),
        move(pos(2, 1), pos(2, 2)),
    )

    assertEquals(
        DoushouqiRoundCaptures(
            capturedByGreen = red(DoushouqiAnimal.RAT),
            capturedByRed = green(DoushouqiAnimal.RAT),
        ),
        afterRobot.lastCompletedRoundCaptures,
    )
}

@Test
fun completedQuietSinglePlayerRoundReplacesOlderCaptureSummary() {
    val session = pairedCaptureSession()
    completePairedCaptureRound(session)
    val beforeQuietRound = session.state()

    val playerMove = DoushouqiRules.legalMoves(beforeQuietRound.position)
        .first { beforeQuietRound.position.pieceAt(it.to) == null }
    val afterPlayer = session.play(playerMove)
    assertEquals(
        beforeQuietRound.lastCompletedRoundCaptures,
        afterPlayer.lastCompletedRoundCaptures,
    )
    val request = requireNotNull(afterPlayer.robotRequest)
    val robotMove = DoushouqiRules.legalMoves(request.state)
        .first { request.state.pieceAt(it.to) == null }

    val completed = session.applyRobotMove(request, robotMove)

    assertEquals(DoushouqiRoundCaptures(), completed.lastCompletedRoundCaptures)
}

@Test
fun terminalPlayerMovePublishesOneMoveRoundImmediately() {
    val session = DoushouqiSession(
        DoushouqiMode.SINGLE_PLAYER,
        stateOf(
            sideToMove = DoushouqiSide.PINE_GREEN,
            pos(4, 0) to green(DoushouqiAnimal.CAT),
            pos(3, 0) to red(DoushouqiAnimal.RAT),
        ),
    )

    val finished = session.play(move(pos(4, 0), pos(3, 0)))

    assertEquals(
        DoushouqiRoundCaptures(
            capturedByGreen = red(DoushouqiAnimal.RAT),
        ),
        finished.lastCompletedRoundCaptures,
    )
    assertNull(finished.robotRequest)
}

@Test
fun robotOpeningDoesNotPublishRoundCaptureSummary() {
    val session = DoushouqiSession(
        DoushouqiMode.SINGLE_PLAYER,
        stateOf(
            sideToMove = DoushouqiSide.PINE_GREEN,
            pos(4, 0) to green(DoushouqiAnimal.CAT),
            pos(3, 0) to red(DoushouqiAnimal.RAT),
            pos(6, 6) to red(DoushouqiAnimal.CAT),
        ),
        playerSide = DoushouqiSide.VERMILION,
    )
    val request = requireNotNull(session.state().robotRequest)

    val opened = session.applyRobotMove(request, move(pos(4, 0), pos(3, 0)))

    assertEquals(DoushouqiRoundCaptures(), opened.lastCompletedRoundCaptures)
    assertEquals(0, opened.historySize)
}

@Test
fun twoPlayerPublishesOnlyAfterRedCompletesRound() {
    val session = DoushouqiSession(
        DoushouqiMode.TWO_PLAYERS,
        stateOf(
            sideToMove = DoushouqiSide.PINE_GREEN,
            pos(4, 0) to green(DoushouqiAnimal.CAT),
            pos(3, 0) to red(DoushouqiAnimal.RAT),
            pos(2, 2) to green(DoushouqiAnimal.RAT),
            pos(2, 1) to red(DoushouqiAnimal.CAT),
        ),
    )

    val afterGreen = session.play(move(pos(4, 0), pos(3, 0)))
    assertEquals(DoushouqiRoundCaptures(), afterGreen.lastCompletedRoundCaptures)

    val afterRed = session.play(move(pos(2, 1), pos(2, 2)))

    assertEquals(
        DoushouqiRoundCaptures(
            capturedByGreen = red(DoushouqiAnimal.RAT),
            capturedByRed = green(DoushouqiAnimal.RAT),
        ),
        afterRed.lastCompletedRoundCaptures,
    )
}
```

Use these exact helpers for the first two single-player tests:

```kotlin
private fun pairedCaptureSession(): DoushouqiSession = DoushouqiSession(
    DoushouqiMode.SINGLE_PLAYER,
    stateOf(
        sideToMove = DoushouqiSide.PINE_GREEN,
        pos(4, 0) to green(DoushouqiAnimal.CAT),
        pos(3, 0) to red(DoushouqiAnimal.RAT),
        pos(2, 2) to green(DoushouqiAnimal.RAT),
        pos(2, 1) to red(DoushouqiAnimal.CAT),
    ),
)

private fun completePairedCaptureRound(session: DoushouqiSession) {
    val afterPlayer = session.play(move(pos(4, 0), pos(3, 0)))
    session.applyRobotMove(
        requireNotNull(afterPlayer.robotRequest),
        move(pos(2, 1), pos(2, 2)),
    )
}
```

Add this terminal two-player test:

```kotlin
@Test
fun terminalGreenMovePublishesTwoPlayerRoundImmediately() {
    val session = DoushouqiSession(
        DoushouqiMode.TWO_PLAYERS,
        stateOf(
            sideToMove = DoushouqiSide.PINE_GREEN,
            pos(4, 0) to green(DoushouqiAnimal.CAT),
            pos(3, 0) to red(DoushouqiAnimal.RAT),
        ),
    )

    val finished = session.play(move(pos(4, 0), pos(3, 0)))

    assertEquals(
        DoushouqiRoundCaptures(
            capturedByGreen = red(DoushouqiAnimal.RAT),
        ),
        finished.lastCompletedRoundCaptures,
    )
}
```

In `singlePlayerUndoRestoresBeforeHumanAndRobotPair`, save
`initial.lastCompletedRoundCaptures` and assert the restored value equals it.
In `twoPlayerUndoRestoresExactlyOneMove`, make Green's move a capture and assert
that undo restores the empty completed summary. In the restart and stale-request
tests, compare the full `lastCompletedRoundCaptures` value before and after the
operation; do not assert on individual fields.

- [ ] **Step 2: Run the session tests and verify RED**

Run:

```bash
./gradlew :games:doushouqi:testDebugUnitTest \
  --tests com.buddygames.doushouqi.DoushouqiSessionTest
```

Expected: compilation fails because `DoushouqiRoundCaptures` and
`lastCompletedRoundCaptures` do not exist.

- [ ] **Step 3: Implement the minimal round state**

In `DoushouqiSession.kt`, replace `lastCapturedPiece` with:

```kotlin
data class DoushouqiRoundCaptures(
    val capturedByGreen: DoushouqiPiece? = null,
    val capturedByRed: DoushouqiPiece? = null,
) {
    internal fun recordCapture(
        attacker: DoushouqiSide,
        captured: DoushouqiPiece?,
    ): DoushouqiRoundCaptures = when {
        captured == null -> this
        attacker == DoushouqiSide.PINE_GREEN -> copy(capturedByGreen = captured)
        else -> copy(capturedByRed = captured)
    }
}
```

Expose `lastCompletedRoundCaptures: DoushouqiRoundCaptures` from
`DoushouqiSessionState`. Store both the completed summary and nullable pending
summary in `DoushouqiSnapshot`.

Initialize:

```kotlin
private var lastCompletedRoundCaptures = DoushouqiRoundCaptures()
private var pendingRoundCaptures: DoushouqiRoundCaptures? = null
```

For every accepted `play`, read `attacker = position.sideToMove` and the
destination occupant before applying the move, then:

```kotlin
val nextPending = when {
    mode == DoushouqiMode.SINGLE_PLAYER ->
        DoushouqiRoundCaptures().recordCapture(attacker, captured)
    attacker == DoushouqiSide.PINE_GREEN ->
        DoushouqiRoundCaptures().recordCapture(attacker, captured)
    else ->
        (pendingRoundCaptures ?: DoushouqiRoundCaptures())
            .recordCapture(attacker, captured)
}
pendingRoundCaptures = nextPending
val completesRound =
    next.result != null ||
        mode == DoushouqiMode.TWO_PLAYERS &&
        attacker == DoushouqiSide.VERMILION
if (completesRound) {
    lastCompletedRoundCaptures = nextPending
    pendingRoundCaptures = null
}
```

For an accepted robot move, update and publish only when
`pendingRoundCaptures != null`; otherwise treat it as the unpaired robot
opening and leave both summaries unchanged:

```kotlin
pendingRoundCaptures?.let { pending ->
    lastCompletedRoundCaptures =
        pending.recordCapture(attacker, captured)
    pendingRoundCaptures = null
}
```

Snapshots restore both fields. `restart` clears both. `invalidate` and all
rejected move paths preserve both.

- [ ] **Step 4: Run the session tests and verify GREEN**

Run:

```bash
./gradlew :games:doushouqi:testDebugUnitTest \
  --tests com.buddygames.doushouqi.DoushouqiSessionTest
```

Expected: all `DoushouqiSessionTest` tests pass.

- [ ] **Step 5: Commit the session behavior**

```bash
git add \
  games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiSession.kt \
  games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiSessionTest.kt
git commit -m "$(cat <<'EOF'
feat: track Doushouqi round captures

- Publish capture summaries only when a normal round completes
- Preserve round boundaries across undo, restart, and robot openings
EOF
)"
```

---

### Task 2: Render Ordered Capture Lines in Both Modes

**Files:**
- Modify: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiPluginTest.kt`
- Modify: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiScreen.kt`

**Interfaces:**
- Consumes: `DoushouqiSessionState.lastCompletedRoundCaptures`.
- Produces: `doushouqiRoundCaptureLabels(summary): List<Pair<DoushouqiSide, String>>`.
- Removes: `doushouqiCapturedPieceLabel(piece)`.

- [ ] **Step 1: Write failing copy-projection tests**

Replace `capturedPieceCopyUsesOnlyRedAndGreenFactionNames` with:

```kotlin
@Test
fun roundCaptureCopyIsOrderedByCapturingSide() {
    val labels = doushouqiRoundCaptureLabels(
        DoushouqiRoundCaptures(
            capturedByGreen = red(DoushouqiAnimal.RAT),
            capturedByRed = green(DoushouqiAnimal.ELEPHANT),
        ),
    )

    assertEquals(
        listOf(
            DoushouqiSide.PINE_GREEN to "绿方吃：鼠",
            DoushouqiSide.VERMILION to "红方吃：象",
        ),
        labels,
    )
}

@Test
fun emptyRoundCaptureCopyHasNoFactionLines() {
    assertEquals(
        emptyList<Pair<DoushouqiSide, String>>(),
        doushouqiRoundCaptureLabels(DoushouqiRoundCaptures()),
    )
}
```

- [ ] **Step 2: Run the UI helper tests and verify RED**

Run:

```bash
./gradlew :games:doushouqi:testDebugUnitTest \
  --tests com.buddygames.doushouqi.DoushouqiPluginTest
```

Expected: compilation fails because `doushouqiRoundCaptureLabels` does not
exist.

- [ ] **Step 3: Implement ordered labels and shared rail rendering**

Add:

```kotlin
internal fun doushouqiRoundCaptureLabels(
    captures: DoushouqiRoundCaptures,
): List<Pair<DoushouqiSide, String>> = buildList {
    captures.capturedByGreen?.let {
        add(DoushouqiSide.PINE_GREEN to "绿方吃：${it.animal.label}")
    }
    captures.capturedByRed?.let {
        add(DoushouqiSide.VERMILION to "红方吃：${it.animal.label}")
    }
}
```

Remove the `mode == SINGLE_PLAYER` guard and `最近一步吃子` heading. For both
modes, derive labels from `session.lastCompletedRoundCaptures`:

```kotlin
val captureLabels =
    doushouqiRoundCaptureLabels(session.lastCompletedRoundCaptures)
if (captureLabels.isEmpty()) {
    Text("无吃子", color = DoushouqiMuted, fontSize = 16.sp)
} else {
    captureLabels.forEach { (side, label) ->
        val sideColor =
            if (side == DoushouqiSide.PINE_GREEN) {
                DoushouqiGreenPiece
            } else {
                DoushouqiRedPiece
            }
        Text(
            label,
            modifier = Modifier
                .background(sideColor.copy(alpha = 0.10f), RoundedCornerShape(5.dp))
                .border(1.dp, sideColor.copy(alpha = 0.38f), RoundedCornerShape(5.dp))
                .padding(horizontal = 9.dp, vertical = 4.dp),
            color = sideColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
```

Keep the middle zone spacing, both dividers, turn/result copy, and action-button
geometry unchanged.

- [ ] **Step 4: Run UI helper and module tests**

Run:

```bash
./gradlew :games:doushouqi:testDebugUnitTest \
  --tests com.buddygames.doushouqi.DoushouqiPluginTest
./gradlew :games:doushouqi:testDebugUnitTest
```

Expected: both commands finish with `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit the rail change**

```bash
git add \
  games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiScreen.kt \
  games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiPluginTest.kt
git commit -m "$(cat <<'EOF'
feat: show Doushouqi round captures

- Render Green and Red capture lines from the last completed round
- Share the capture summary across single-player and two-player rails
EOF
)"
```

---

### Task 3: Release Doushouqi 0.0.6 and Align Documentation

**Files:**
- Modify: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiManifestTest.kt`
- Modify: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiManifest.kt`
- Modify: `games/doushouqi/package/manifest.json`
- Modify: `games/doushouqi/README.md`
- Modify: `designs/specs/doushouqi-ui.md`
- Modify: `designs/images/doushouqi-ui-game-tablet.md`
- Modify: `docs/superpowers/specs/2026-07-28-doushouqi-round-capture-summary-design.md`
- Modify: `README.md`
- Modify: `AGENTS.md`

**Interfaces:**
- Produces: package version code `6`, version name `0.0.6`.
- Documents: exact completed-round copy and lifecycle in both modes.

- [ ] **Step 1: Make the manifest test require version 0.0.6**

Change the two assertions in `DoushouqiManifestTest`:

```kotlin
assertEquals(6, manifest.versionCode)
assertEquals("0.0.6", manifest.versionName)
```

- [ ] **Step 2: Run the manifest test and verify RED**

Run:

```bash
./gradlew :games:doushouqi:testDebugUnitTest \
  --tests com.buddygames.doushouqi.DoushouqiManifestTest
```

Expected: `packageIdentityMatchesReferenceArtworkRelease` fails with expected
version code `6` but actual `5`.

- [ ] **Step 3: Update package version sources**

Set:

```kotlin
internal const val DOUSHOUQI_VERSION_CODE = 6
internal const val DOUSHOUQI_VERSION_NAME = "0.0.6"
```

Set matching JSON values:

```json
"versionCode": 6,
"versionName": "0.0.6"
```

- [ ] **Step 4: Align all human and agent documentation**

Update `games/doushouqi/README.md` to version `0.0.6` and replace the
latest-move paragraph with the exact completed-round behavior. Update
`designs/specs/doushouqi-ui.md` and
`designs/images/doushouqi-ui-game-tablet.md` so their required copy uses
`绿方吃：鼠`, `红方吃：象`, and `无吃子`, with no `最近一步吃子`.

Change the new design status to:

```markdown
**Status:** Approved and implemented.
```

Update the Doushouqi current-behavior paragraph in `AGENTS.md` to version
`0.0.6` and the new round semantics. Add the implementation plan to the
document maps in both `README.md` and `AGENTS.md`. Keep the prior `0.0.5`
design, plan, and report links as historical evidence.

- [ ] **Step 5: Run manifest and module tests**

Run:

```bash
./gradlew :games:doushouqi:testDebugUnitTest \
  --tests com.buddygames.doushouqi.DoushouqiManifestTest
./gradlew :games:doushouqi:testDebugUnitTest
```

Expected: both commands finish with `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit the release alignment**

```bash
git add \
  AGENTS.md \
  README.md \
  designs/images/doushouqi-ui-game-tablet.md \
  designs/specs/doushouqi-ui.md \
  docs/superpowers/plans/2026-07-28-doushouqi-round-capture-summary.md \
  docs/superpowers/specs/2026-07-28-doushouqi-round-capture-summary-design.md \
  games/doushouqi/README.md \
  games/doushouqi/package/manifest.json \
  games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiManifest.kt \
  games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiManifestTest.kt
git commit -m "$(cat <<'EOF'
chore: release Doushouqi 0.0.6

- Align package manifests with the round capture summary release
- Update game, design, and repository documentation
EOF
)"
```

---

### Task 4: Complete Integration and Android Runtime Acceptance

**Files:**
- Create: `docs/superpowers/reports/2026-07-28-doushouqi-round-capture-summary-runtime-acceptance.md`
- Modify: `README.md`
- Modify: `AGENTS.md`

**Interfaces:**
- Verifies: packaged Doushouqi version `0.0.6`.
- Records: initial, pending-half-round, and completed-round rail evidence.

- [ ] **Step 1: Run the full deterministic verification gate**

Run:

```bash
./gradlew :games:doushouqi:testDebugUnitTest
npm run verify
git diff --check
unzip -p build/game-packages/doushouqi.zip manifest.json |
  rg '"version(Code|Name)": (6|"0.0.6")'
```

Expected: both Gradle invocations finish with `BUILD SUCCESSFUL`, diff check
prints nothing, and the zip manifest prints version code `6` plus version name
`0.0.6`.

- [ ] **Step 2: Validate a complete single-player round on Android 36**

Start/install with the repository command:

```bash
npm start
```

Open Doushouqi single-player mode and verify:

- initial rail shows `无吃子`;
- after a player capture while `智能思考中` is visible, the previous completed
  summary remains unchanged;
- after the robot response, the rail atomically shows all captures from that
  player-plus-robot round;
- the labels use only `绿方吃：<兽名>` and `红方吃：<兽名>`.

Capture a screenshot under:

```text
build/runtime-acceptance/doushouqi-round-capture-summary.png
```

- [ ] **Step 3: Record acceptance and update document maps**

Write the report with the exact commands, Android/AVD dimensions, semantic-tree
copy, screenshot path, and pass/fail verdict. Link it from root `README.md` and
the `AGENTS.md` document map to satisfy repository documentation alignment.

- [ ] **Step 4: Re-run documentation checks and commit**

Run:

```bash
git diff --check
git status --short
```

Then commit only the report and map updates:

```bash
git add \
  AGENTS.md \
  README.md \
  docs/superpowers/reports/2026-07-28-doushouqi-round-capture-summary-runtime-acceptance.md
git commit -m "$(cat <<'EOF'
docs: record Doushouqi round capture acceptance

- Capture Android evidence for completed-round rail publishing
- Link the runtime report from repository documentation
EOF
)"
```
