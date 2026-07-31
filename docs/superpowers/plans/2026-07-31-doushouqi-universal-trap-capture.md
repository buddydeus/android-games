# Doushouqi Universal Trap Capture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every defender in any Doushouqi trap capturable by every enemy animal, independent of trap ownership, while keeping public rules, primitive search, and AI evaluation aligned.

**Architecture:** Replace ownership-qualified destination-trap checks in both capture engines with an ownership-independent trap predicate. Update AI ordering and evaluation so entering an own trap is no longer rewarded and every trapped piece is treated as vulnerable; sessions and Compose inherit authoritative legal moves without new branches.

**Tech Stack:** Kotlin, immutable Doushouqi rules, primitive array-backed AI search, iterative-deepening alpha-beta, JUnit 4, Android Gradle tasks.

## Global Constraints

- A defender occupying any Vermilion or Pine Green trap has effective defensive rank zero.
- Any enemy animal whose move otherwise legally reaches the occupied trap may capture that defender, including an Elephant capturing a Rat.
- Friendly occupancy remains non-capturable and Rat land/river boundary restrictions remain unchanged.
- A trapped attacker leaving the trap attacks with its normal rank.
- Public rules and primitive search must generate identical moves.
- AI evaluation must penalize every occupied trap and move ordering must not reward entering an own trap.
- Do not add a rule flag, public API, UI branch, dependency, asset, shell change, or change to another game.
- Increment only Doushouqi from version code `8`, version name `0.0.8`, to version code `9`, version name `0.0.9`.
- Keep code and JSON manifests, tests, README files, designs, and `AGENTS.md` aligned.
- Run Doushouqi unit tests and `npm run verify` before completion.

---

### Task 1: Make Trap Capture Independent of Ownership

**Files:**
- Modify: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiCaptureTest.kt`
- Modify: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiSearchPositionTest.kt`
- Modify: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiSearchEngineTest.kt`
- Modify: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiRules.kt`
- Modify: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiSearchPosition.kt`

**Interfaces:**
- Consumes: `DoushouqiRules.legalMoves`, `DoushouqiSearchPosition.legalMoves`, `DoushouqiAi.chooseMove`, `trapOwner`, and existing fixtures.
- Produces: ownership-independent trap captures in both move generators with unchanged source-trap and friendly-occupancy behavior.

- [ ] **Step 1: Replace the ineffective trap test with failing ownership coverage**

Replace `enemyPieceInOwnedTrapHasEffectiveRankZero` in `DoushouqiCaptureTest` with:

```kotlin
@Test
fun defenderInEitherSidesTrapHasEffectiveRankZero() {
    val cases = listOf(
        Triple(pos(6, 3), pos(7, 3), DoushouqiSide.PINE_GREEN),
        Triple(pos(2, 3), pos(1, 3), DoushouqiSide.PINE_GREEN),
        Triple(pos(2, 3), pos(1, 3), DoushouqiSide.VERMILION),
        Triple(pos(6, 3), pos(7, 3), DoushouqiSide.VERMILION),
    )
    cases.forEach { (from, trap, attackerSide) ->
        val state = stateOf(
            attackerSide,
            from to DoushouqiPiece(attackerSide, DoushouqiAnimal.CAT),
            trap to DoushouqiPiece(attackerSide.other(), DoushouqiAnimal.ELEPHANT),
        )
        assertTrue(move(from, trap) in legal(state))
    }
}
```

Cat-versus-Elephant makes the assertion depend on trap weakening. The old Rat-versus-Elephant fixture was not diagnostic because a land Rat already captures an Elephant.

- [ ] **Step 2: Add failing Elephant-versus-trapped-Rat coverage**

Add:

```kotlin
@Test
fun elephantMayCaptureEnemyRatInEitherSidesTrap() {
    listOf(pos(6, 3) to pos(7, 3), pos(2, 3) to pos(1, 3)).forEach { (from, trap) ->
        val state = stateOf(
            from to green(DoushouqiAnimal.ELEPHANT),
            trap to red(DoushouqiAnimal.RAT),
        )
        assertTrue(move(from, trap) in legal(state))
    }
}
```

Keep `landRatCapturesElephantButElephantCannotCaptureRat` unchanged so ordinary-land Elephant-versus-Rat remains illegal.

- [ ] **Step 3: Add failing public/primitive parity and AI tactical coverage**

Add to `DoushouqiSearchPositionTest`:

```kotlin
@Test
fun primitiveAndPublicRulesAllowCaptureInDefendersOwnTrap() {
    val state = stateOf(
        pos(2, 3) to green(DoushouqiAnimal.CAT),
        pos(1, 3) to red(DoushouqiAnimal.ELEPHANT),
    )
    val capture = move(pos(2, 3), pos(1, 3))
    val publicMoves = DoushouqiRules.legalMoves(state)
    val primitiveMoves = DoushouqiSearchPosition(state).legalMoves()
    assertTrue(capture in publicMoves)
    assertTrue(capture in primitiveMoves)
    assertEquals(publicMoves, primitiveMoves)
}
```

Add to `DoushouqiSearchEngineTest`:

```kotlin
@Test
fun aiTakesWinningCaptureInDefendersOwnTrap() {
    val state = stateOf(
        pos(2, 3) to green(DoushouqiAnimal.CAT),
        pos(1, 3) to red(DoushouqiAnimal.ELEPHANT),
    )
    assertEquals(
        move(pos(2, 3), pos(1, 3)),
        DoushouqiAi.chooseMove(state, DoushouqiAiLevel.forLevel(3)),
    )
}
```

- [ ] **Step 4: Run focused tests and verify RED**

Run:

```bash
./gradlew :games:doushouqi:testDebugUnitTest \
  --tests com.buddygames.doushouqi.DoushouqiCaptureTest \
  --tests com.buddygames.doushouqi.DoushouqiSearchPositionTest \
  --tests com.buddygames.doushouqi.DoushouqiSearchEngineTest
```

Expected: defender-owned-trap cases fail in public rules, primitive search, and AI selection; attacker-owned-trap and trapped-attacker-leaving cases remain green.

- [ ] **Step 5: Replace the two ownership-qualified trap checks**

In `DoushouqiRules.canCapture`, replace `if (trapOwner(to) == attacker.side) return true` with:

```kotlin
if (trapOwner(to) != null) return true
```

In `DoushouqiSearchPosition.canCapture`, replace `if (trapOwner(to) == attackerSide) return true` with the same ownership-independent check. Keep both checks before Rat/Elephant special handling and do not change source-square rank handling.

- [ ] **Step 6: Run the Step 4 command and verify GREEN**

Expected: `BUILD SUCCESSFUL`, with public and primitive move lists equal and the AI selecting the winning capture.

---

### Task 2: Align AI Trap Ordering and Evaluation

**Files:**
- Modify: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiSearchEngineTest.kt`
- Modify: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiSearchEngine.kt`

**Interfaces:**
- Consumes: `DoushouqiSearchEngine.search`, custom `DoushouqiAiLevel` fixtures, fallback ordering, and depth-one evaluation.
- Produces: no own-trap order bonus and an ownership-independent occupied-trap penalty.

- [ ] **Step 1: Add failing fallback-order and evaluation tests**

Add to `DoushouqiSearchEngineTest`:

```kotlin
@Test
fun cancelledFallbackDoesNotPreferEnteringOwnTrap() {
    val result = DoushouqiSearchEngine.search(
        state = ownTrapOrderingState(),
        level = DoushouqiAiLevel(6, 1, 1_000, 1_000, 1, 0, 0),
        nanoTime = { 0L },
        shouldStop = { true },
    )
    assertEquals(move(pos(7, 2), pos(6, 2)), requireNotNull(result).move)
    assertEquals(0, result.completedDepth)
}

@Test
fun depthOneSearchTreatsOwnTrapAsVulnerable() {
    val result = DoushouqiSearchEngine.search(
        state = ownTrapChoiceState(),
        level = DoushouqiAiLevel(6, 1, 1_000, 1_000, 1, 0, 0),
        nanoTime = { 0L },
    )
    assertEquals(move(pos(8, 1), pos(8, 0)), requireNotNull(result).move)
    assertEquals(1, result.completedDepth)
}

private fun ownTrapChoiceState(): DoushouqiState = stateOf(
    pos(8, 1) to green(DoushouqiAnimal.CAT),
    pos(7, 1) to red(DoushouqiAnimal.ELEPHANT),
    pos(4, 6) to red(DoushouqiAnimal.DOG),
)

private fun ownTrapOrderingState(): DoushouqiState = stateOf(
    pos(7, 2) to green(DoushouqiAnimal.CAT),
    pos(4, 6) to red(DoushouqiAnimal.DOG),
)
```

The ordering fixture gives safe `(6,2)` and trapped `(7,3)` destinations equal progress toward the enemy den, so the old `+15_000` bonus alone makes the cancelled fallback choose `(7,3)`. In the evaluation fixture, the Elephant blocks the safe forward square; without an all-trap penalty, `(8,2)` remains positionally better than `(8,0)` at depth one.

- [ ] **Step 2: Run the two tests and verify RED**

```bash
./gradlew :games:doushouqi:testDebugUnitTest \
  --tests com.buddygames.doushouqi.DoushouqiSearchEngineTest.cancelledFallbackDoesNotPreferEnteringOwnTrap \
  --tests com.buddygames.doushouqi.DoushouqiSearchEngineTest.depthOneSearchTreatsOwnTrapAsVulnerable
```

Expected: the fallback test chooses `(7,3)` under the old order bonus, while the depth-one test chooses `(8,2)` under the old evaluation.

- [ ] **Step 3: Remove the bonus and broaden the penalty**

Delete from `moveOrderScore`:

```kotlin
if (trapOwner(move.to) == moving.side) score += 15_000
```

Replace in `evaluate`:

```kotlin
if (trapOwner(boardPosition) == side.other()) value -= 120
```

with:

```kotlin
if (trapOwner(boardPosition) != null) value -= 120
```

- [ ] **Step 4: Run the full search-engine test and module suite**

```bash
./gradlew :games:doushouqi:testDebugUnitTest \
  --tests com.buddygames.doushouqi.DoushouqiSearchEngineTest
./gradlew :games:doushouqi:testDebugUnitTest
```

Expected: both commands finish with `BUILD SUCCESSFUL`.

---

### Task 3: Release Doushouqi 0.0.9 and Align Documentation

**Files:**
- Modify: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiManifestTest.kt`
- Modify: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiManifest.kt`
- Modify: `games/doushouqi/package/manifest.json`
- Modify: `docs/superpowers/specs/2026-07-26-doushouqi-game-design.md`
- Modify: `docs/superpowers/specs/2026-07-31-doushouqi-universal-trap-capture-design.md`
- Modify: `games/doushouqi/README.md`
- Modify: `README.md`
- Modify: `AGENTS.md`

**Interfaces:**
- Produces: Doushouqi package version code `9`, version name `0.0.9`, plus authoritative ownership-independent trap wording.

- [ ] **Step 1: Make the manifest test require 0.0.9 and verify RED**

Change the expected identity to:

```kotlin
assertEquals(9, manifest.versionCode)
assertEquals("0.0.9", manifest.versionName)
```

Run `DoushouqiManifestTest`; expect failure because production still reports `8 / 0.0.8`.

- [ ] **Step 2: Align both manifest sources and verify GREEN**

Set `DOUSHOUQI_VERSION_CODE = 9`, `DOUSHOUQI_VERSION_NAME = "0.0.9"`, and matching JSON values. Re-run `DoushouqiManifestTest`; expect `BUILD SUCCESSFUL` including JSON/code equality.

- [ ] **Step 3: Update current rule and version documentation**

- Baseline design: state that any defender in any trap has effective rank zero, any enemy can capture it, and an attacker leaving a trap uses normal rank.
- Increment design: state that the variant is incorporated into the baseline.
- Module README: set `0.0.9` and replace “进入对手陷阱” with ownership-independent trap wording.
- Root README: update current Doushouqi references to `0.0.9` and describe universal trap capture.
- `AGENTS.md`: set current Doushouqi to `0.0.9` and require universal trap capture, public/primitive parity, and matching AI evaluation. Preserve historical version entries.

- [ ] **Step 4: Verify text and version alignment**

```bash
rg -n "trapOwner\((to|boardPosition|move\.to)\) ==|进入对手陷阱|opponent's trap has effective rank zero" \
  games/doushouqi README.md AGENTS.md \
  docs/superpowers/specs/2026-07-26-doushouqi-game-design.md
rg -n "0\.0\.8" games/doushouqi README.md AGENTS.md
git diff --check
```

Expected: no ownership-qualified current rule or AI condition; only historical `0.0.8` entries remain in `AGENTS.md`; diff check exits zero.

---

### Task 4: Run Release Verification and Commit

**Files:**
- Verify all files changed by Tasks 1–3.

**Interfaces:**
- Produces: tested `doushouqi.zip`, verified Debug APK inclusion, and a scoped local commit.

- [ ] **Step 1: Run the complete gate**

Run `npm run verify`. Expected: `BUILD SUCCESSFUL`, all unit tests pass, six game packages validate, and the Debug APK contains required built-in assets.

- [ ] **Step 2: Review scope**

Run `git diff --check`, `git status --short`, `git diff --stat`, and `git diff`. Confirm no secrets, build outputs, unrelated files, shell version changes, assets, or other games changed.

- [ ] **Step 3: Commit locally**

Stage only the scoped Doushouqi files and synchronized documentation, then commit:

```bash
git commit -m "$(cat <<'EOF'
feat: make Doushouqi traps universally vulnerable

- Let every enemy animal capture a defender in any trap
- Align primitive search ordering and evaluation with the trap rule
- Release Doushouqi 0.0.9 with synchronized tests and documentation
EOF
)"
```

Do not push. Report `git status --short --branch`, the commit, and fresh verification evidence.
