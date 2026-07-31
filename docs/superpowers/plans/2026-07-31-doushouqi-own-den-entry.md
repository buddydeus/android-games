# Doushouqi Own-Den Entry Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow both Doushouqi sides to enter, occupy, and leave their own den without winning while preserving opponent-den wins and public/search move equivalence.

**Architecture:** Remove the own-den destination rejection from the immutable public rule generator and the primitive make/unmake search generator. Keep terminal adjudication unchanged because both engines already award a den win only when the destination belongs to the opponent; sessions and Compose continue consuming authoritative legal moves without new branches.

**Tech Stack:** Kotlin, immutable Doushouqi rules, primitive array-backed AI search position, JUnit 4, Android Gradle tasks.

## Global Constraints

- An empty own den is a legal land destination for either side; normal friendly occupancy and capture rules still apply.
- Entering, occupying, or leaving the mover's own den does not itself create a win or draw.
- Entering the opponent's den still wins immediately with `DoushouqiWinReason.DEN`.
- Public rules and primitive AI search must generate identical moves.
- Do not add a rule flag, public API, UI branch, dependency, asset, shell change, or change to another game.
- Increment only Doushouqi from version code `7`, version name `0.0.7`, to version code `8`, version name `0.0.8`.
- Keep `DoushouqiManifest`, package JSON, tests, root and module README files, the baseline design, and `AGENTS.md` aligned.
- Run Doushouqi unit tests and `npm run verify` before completion.

---

### Task 1: Allow Own-Den Moves in Public Rules and AI Search

**Files:**
- Modify: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiMovementTest.kt`
- Modify: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiSearchPositionTest.kt`
- Modify: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiRules.kt`
- Modify: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiSearchPosition.kt`

**Interfaces:**
- Consumes: `DoushouqiRules.legalMoves`, `DoushouqiRules.apply`, `DoushouqiSearchPosition.legalMoves`, and existing `pos`, `move`, `green`, `red`, and `stateOf` fixtures.
- Produces: own-den moves in both legal-move engines with unchanged `DoushouqiResult` semantics.

- [ ] **Step 1: Replace the public own-den rejection test with failing variant coverage**

In `DoushouqiMovementTest`, replace `piecesCannotEnterTheirOwnDenOrFriendlyOccupiedSquare` with:

```kotlin
@Test
fun bothSidesMayEnterTheirOwnDenWithoutWinning() {
    val greenFrom = pos(7, 3)
    val greenDen = pos(8, 3)
    val greenState = stateOf(
        greenFrom to green(DoushouqiAnimal.CAT),
        pos(4, 6) to red(DoushouqiAnimal.DOG),
    )

    assertTrue(move(greenFrom, greenDen) in legal(greenState))
    assertEquals(
        null,
        requireNotNull(DoushouqiRules.apply(greenState, move(greenFrom, greenDen))).result,
    )

    val redFrom = pos(1, 3)
    val redDen = pos(0, 3)
    val redState = stateOf(
        DoushouqiSide.VERMILION,
        redFrom to red(DoushouqiAnimal.CAT),
        pos(4, 0) to green(DoushouqiAnimal.DOG),
    )

    assertTrue(move(redFrom, redDen) in legal(redState))
    assertEquals(
        null,
        requireNotNull(DoushouqiRules.apply(redState, move(redFrom, redDen))).result,
    )
}

@Test
fun friendlyPieceStillBlocksOwnDen() {
    val from = pos(7, 3)
    val ownDen = pos(8, 3)
    val state = stateOf(
        from to green(DoushouqiAnimal.CAT),
        ownDen to green(DoushouqiAnimal.DOG),
    )

    assertFalse(move(from, ownDen) in legal(state))
}
```

Add `import org.junit.Assert.assertEquals`. The first test proves both orientations plus non-terminal application; the second preserves the general friendly-occupancy rule.

- [ ] **Step 2: Add failing primitive-search parity coverage**

In `DoushouqiSearchPositionTest`, add `assertTrue` and this test:

```kotlin
@Test
fun primitiveAndPublicRulesAllowBothSidesToEnterOwnDen() {
    val cases = listOf(
        stateOf(
            pos(7, 3) to green(DoushouqiAnimal.CAT),
            pos(4, 6) to red(DoushouqiAnimal.DOG),
        ) to move(pos(7, 3), pos(8, 3)),
        stateOf(
            DoushouqiSide.VERMILION,
            pos(1, 3) to red(DoushouqiAnimal.CAT),
            pos(4, 0) to green(DoushouqiAnimal.DOG),
        ) to move(pos(1, 3), pos(0, 3)),
    )

    cases.forEach { (state, ownDenMove) ->
        val publicMoves = DoushouqiRules.legalMoves(state)
        val primitiveMoves = DoushouqiSearchPosition(state).legalMoves()

        assertTrue(ownDenMove in publicMoves)
        assertTrue(ownDenMove in primitiveMoves)
        assertEquals(publicMoves, primitiveMoves)
    }
}
```

- [ ] **Step 3: Run the focused tests and verify RED**

Run:

```bash
./gradlew :games:doushouqi:testDebugUnitTest \
  --tests com.buddygames.doushouqi.DoushouqiMovementTest \
  --tests com.buddygames.doushouqi.DoushouqiSearchPositionTest
```

Expected: the new own-den assertions fail because both candidate generators currently return `null` when `denOwner(destination)` equals the moving side. The friendly-occupancy assertion remains green.

- [ ] **Step 4: Remove only the two own-den destination guards**

Delete this line from `DoushouqiRules.candidateMove`:

```kotlin
if (denOwner(destination) == piece.side) return null
```

Delete this line from `DoushouqiSearchPosition.candidateMove`:

```kotlin
if (denOwner(destination) == side) return null
```

Do not change either adjudicator: their existing `denOwner(destination) == mover.other()` checks must remain opponent-specific.

- [ ] **Step 5: Run focused and full module tests and verify GREEN**

Run:

```bash
./gradlew :games:doushouqi:testDebugUnitTest \
  --tests com.buddygames.doushouqi.DoushouqiMovementTest \
  --tests com.buddygames.doushouqi.DoushouqiSearchPositionTest \
  --tests com.buddygames.doushouqi.DoushouqiTerminalTest
./gradlew :games:doushouqi:testDebugUnitTest
```

Expected: both commands finish with `BUILD SUCCESSFUL`; the existing enemy-den terminal test still proves `DoushouqiWinReason.DEN` precedence.

---

### Task 2: Release Doushouqi 0.0.8 and Align Documentation

**Files:**
- Modify: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiManifestTest.kt`
- Modify: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiManifest.kt`
- Modify: `games/doushouqi/package/manifest.json`
- Modify: `docs/superpowers/specs/2026-07-26-doushouqi-game-design.md`
- Modify: `games/doushouqi/README.md`
- Modify: `README.md`
- Modify: `AGENTS.md`

**Interfaces:**
- Produces: Doushouqi package version code `8`, version name `0.0.8`.
- Documents: the local own-den-entry variant and unchanged opponent-den win rule.

- [ ] **Step 1: Make the manifest test require 0.0.8 and verify RED**

In `DoushouqiManifestTest.packageIdentityMatchesReferenceArtworkRelease`, change:

```kotlin
assertEquals(8, manifest.versionCode)
assertEquals("0.0.8", manifest.versionName)
```

Run:

```bash
./gradlew :games:doushouqi:testDebugUnitTest \
  --tests com.buddygames.doushouqi.DoushouqiManifestTest
```

Expected: `packageIdentityMatchesReferenceArtworkRelease` fails because the code manifest still reports version code `7` and name `0.0.7`.

- [ ] **Step 2: Align both manifest sources**

In `DoushouqiManifest.kt`, set:

```kotlin
internal const val DOUSHOUQI_VERSION_CODE = 8
internal const val DOUSHOUQI_VERSION_NAME = "0.0.8"
```

In `games/doushouqi/package/manifest.json`, set:

```json
"versionCode": 8,
"versionName": "0.0.8"
```

Re-run the manifest test and expect `BUILD SUCCESSFUL`, including `jsonManifestMatchesCodeManifest`.

- [ ] **Step 3: Update the authoritative and human-facing rule text**

Make these exact documentation changes:

- In `2026-07-26-doushouqi-game-design.md`, replace “A piece may not enter its own den” with “A piece may enter, occupy, and leave its own den; doing so has no terminal effect.” Keep the enemy-den terminal rule unchanged.
- In `games/doushouqi/README.md`, set the version to `0.0.8`, replace “不能进入己方兽穴” with copy stating that both sides may enter and leave their own den without winning, and retain enemy-den entry as the first win condition.
- In root `README.md`, change every current Doushouqi version reference from `0.0.7` to `0.0.8` and add the own-den variant to the Doushouqi capability summary.
- In `AGENTS.md`, change the current Doushouqi version from `0.0.7` to `0.0.8` and add an invariant requiring public rules and primitive search to allow both sides into their own den without treating it as a win. Do not rewrite historical `0.0.7` plan/report entries.

- [ ] **Step 4: Verify version and rule-text alignment**

Run:

```bash
rg -n "不能进入己方兽穴|may not enter its own den|denOwner\(destination\) == (piece\.side|side)" \
  games/doushouqi \
  README.md \
  AGENTS.md \
  docs/superpowers/specs/2026-07-26-doushouqi-game-design.md
rg -n "0\.0\.7" games/doushouqi README.md AGENTS.md
git diff --check
```

Expected: the first search returns no own-den prohibition or candidate-generator guard. The version search returns only historical `0.0.7` plan/report descriptions in `AGENTS.md`, not current package metadata or current-version prose. `git diff --check` exits zero.

---

### Task 3: Run Release Verification and Commit

**Files:**
- Verify all files changed by Tasks 1 and 2.

**Interfaces:**
- Produces: tested `doushouqi.zip`, verified Debug APK inclusion, and a scoped local commit.

- [ ] **Step 1: Run the complete repository gate**

Run:

```bash
npm run verify
```

Expected: `BUILD SUCCESSFUL`; all unit tests pass, all six game packages validate, and the Debug APK contains all required built-in package assets.

- [ ] **Step 2: Review the final diff and staged scope**

Run:

```bash
git diff --check
git status --short
git diff --stat
git diff
```

Confirm there are no secrets, generated build outputs, unrelated files, shell version changes, or other game changes.

- [ ] **Step 3: Create the scoped local commit**

Stage only the implementation, regression tests, release metadata, and synchronized documentation, then commit:

```bash
git add \
  games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiRules.kt \
  games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiSearchPosition.kt \
  games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiManifest.kt \
  games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiMovementTest.kt \
  games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiSearchPositionTest.kt \
  games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiManifestTest.kt \
  games/doushouqi/package/manifest.json \
  games/doushouqi/README.md \
  docs/superpowers/specs/2026-07-26-doushouqi-game-design.md \
  README.md \
  AGENTS.md
git commit -m "$(cat <<'EOF'
feat: allow Doushouqi pieces into their own den

- Keep own-den movement legal and non-terminal for both sides
- Align public rules and primitive AI search move generation
- Release Doushouqi 0.0.8 with synchronized tests and documentation
EOF
)"
```

Do not push. Run `git status --short --branch` and report the new commit plus verification evidence.
