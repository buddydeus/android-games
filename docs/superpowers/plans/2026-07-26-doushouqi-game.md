# Dou Shou Qi Game Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver Dou Shou Qi as the sixth independently versioned built-in Android game package with complete 7×9 rules, score-driven 1–10 offline AI, Xiangqi-family sessions, and the approved double-river Compose UI.

**Architecture:** Add a self-contained `games/doushouqi` Android library. Immutable rules state is the shared authority for legal moves, repetition, quiet-move adjudication, session undo, and search; the plugin owns asynchronous robot execution and Compose presentation. The stable shell remains package-agnostic and changes only where its built-in zip catalog and shell version must include the new package.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit 4, Android Gradle Plugin 9.2.1, Gradle zip/d8 packaging, pure-Kotlin iterative-deepening Negamax with alpha-beta pruning.

## Global Constraints

- Follow `docs/superpowers/specs/2026-07-26-doushouqi-game-design.md` and `designs/specs/doushouqi-ui.md`; on visual conflict, the visual SSOT wins.
- Initial package version is exactly `versionCode = 1`, `versionName = 0.0.1`.
- Shell version becomes exactly `versionCode = 5`, `versionName = 0.0.5`.
- Do not change `game-api`, `CURRENT_SHELL_API`, or add dependencies.
- Pine Green moves first; internal orientation keeps Vermilion at the top and Pine Green at the bottom.
- Terminal precedence is den entry, final capture, next-side no-move loss, third repetition, then 100 quiet half-moves.
- Robot level is player wins plus one, clamped to `1..10`; only levels `1..5` use deterministic weakening.
- Search runs off the Compose UI thread and stale generation-bound results never apply.
- Every production behavior follows a witnessed RED test before implementation.
- Every repository file change keeps root `README.md`, root `AGENTS.md`, and `games/doushouqi/README.md` aligned.
- Run `./gradlew :games:doushouqi:testDebugUnitTest` after each game task and `npm run verify` before completion.
- Do not push.

---

## File Map

| File | Responsibility |
| --- | --- |
| `games/doushouqi/build.gradle.kts` | Android library and existing Compose/JUnit dependencies |
| `games/doushouqi/package/manifest.json` | Independent zip manifest |
| `games/doushouqi/package/assets/icon.png` | Circular-safe 1024×1024 package icon |
| `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiModel.kt` | Coordinates, terrain, pieces, moves, results, immutable state |
| `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiRules.kt` | Move generation, capture, application, terminal precedence |
| `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiAi.kt` | Exact level table, evaluation, deterministic fallback/weakening |
| `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiSearchEngine.kt` | Primitive make/unmake iterative-deepening search |
| `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiSession.kt` | Score, side policy, history, robot requests, generation checks |
| `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiVisuals.kt` | SSOT colors, geometry, coordinate mapping, shared marker constants |
| `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiScreen.kt` | Menu, board, rail, controls, accessibility semantics |
| `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiPlugin.kt` | `GamePlugin`, manifest, background executor, Compose/session wiring |
| `games/doushouqi/src/test/java/com/buddygames/doushouqi/*Test.kt` | Manifest, rules, state, AI, session, visuals, and asset contracts |
| `settings.gradle.kts` | Includes `:games:doushouqi` |
| `build.gradle.kts` | Registers/validates `packageDoushouqiGame` |
| `app/build.gradle.kts` | Shell 0.0.5 and built-in zip dependency |
| `package.json` | `build:game:doushouqi` and aggregate game build |
| `README.md`, `AGENTS.md`, `games/doushouqi/README.md` | Human and agent documentation |

---

### Task 1: Module And Manifest Contract

**Files:**
- Create: `games/doushouqi/build.gradle.kts`
- Create: `games/doushouqi/package/manifest.json`
- Create: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiManifest.kt`
- Create: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiManifestTest.kt`
- Create: `games/doushouqi/src/test/java/com/buddygames/doushouqi/StrictJsonParser.kt`
- Modify: `settings.gradle.kts`
- Modify: `README.md`
- Modify: `AGENTS.md`

**Interfaces:**
- Produces: `DOUSHOUQI_VERSION_CODE: Int`, `DOUSHOUQI_VERSION_NAME: String`, and `DoushouqiManifest.gameManifest: GameManifest`.
- Consumes later: `DoushouqiPlugin.manifest` delegates to `DoushouqiManifest.gameManifest`.

- [ ] **Step 1: Create the module directory/build file and include it**

Use the same dependency set as `games/chess/build.gradle.kts`:

```kotlin
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.buddygames.doushouqi"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":game-api"))
    implementation(platform("androidx.compose:compose-bom:2026.06.01"))
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    testImplementation("junit:junit:4.13.2")
}
```

Append `include(":games:doushouqi")` to `settings.gradle.kts`.

- [ ] **Step 2: Write the failing manifest test**

```kotlin
class DoushouqiManifestTest {
    @Test
    fun packageIdentityStartsAtIndependentVersionOne() {
        val manifest = DoushouqiManifest.gameManifest
        assertEquals("doushouqi", manifest.gameId)
        assertEquals("斗兽棋", manifest.displayName)
        assertEquals(1, manifest.versionCode)
        assertEquals("0.0.1", manifest.versionName)
        assertEquals("com.buddygames.doushouqi.DoushouqiPlugin", manifest.entryClass)
        assertEquals("assets/icon.png", manifest.icon)
    }

    @Test
    fun jsonManifestMatchesCodeManifest() {
        val json = StrictJsonParser.parseObject(
            repositoryRoot().resolve("games/doushouqi/package/manifest.json").readText()
        )
        val code = DoushouqiManifest.gameManifest
        assertEquals(code.gameId, json.string("gameId"))
        assertEquals(code.versionCode, json.int("versionCode"))
        assertEquals(code.versionName, json.string("versionName"))
        assertEquals(code.entryClass, json.string("entryClass"))
        assertEquals(code.icon, json.string("icon"))
    }
}
```

Copy `StrictJsonParser.kt` mechanically from the Junqi test utility with only its package changed.

- [ ] **Step 3: Run RED**

Run:

```bash
./gradlew :games:doushouqi:testDebugUnitTest --tests com.buddygames.doushouqi.DoushouqiManifestTest
```

Expected: compilation failure because `DoushouqiManifest` does not exist.

- [ ] **Step 4: Implement the manifest constants and JSON**

```kotlin
internal const val DOUSHOUQI_VERSION_CODE = 1
internal const val DOUSHOUQI_VERSION_NAME = "0.0.1"

internal object DoushouqiManifest {
    val gameManifest = GameManifest(
        gameId = "doushouqi",
        displayName = "斗兽棋",
        versionCode = DOUSHOUQI_VERSION_CODE,
        versionName = DOUSHOUQI_VERSION_NAME,
        entryClass = "com.buddygames.doushouqi.DoushouqiPlugin",
        minShellApi = 1,
        orientation = "landscape",
        icon = "assets/icon.png",
    )
}
```

`manifest.json` contains the same schema, identity, versions, entry class, shell API, landscape orientation, and icon path.

- [ ] **Step 5: Run GREEN and sync docs**

Run the same targeted test and expect both tests to pass. Add the planned `games/doushouqi/` module and targeted test command to root `README.md`/`AGENTS.md`, explicitly marking implementation in progress until Task 7.

- [ ] **Step 6: Commit**

```text
feat: scaffold Doushouqi package

- Add the independent Android game module and manifest contract
- Register the module and document its implementation boundary
```

---

### Task 2: Immutable Board, Movement, And Capture Rules

**Files:**
- Create: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiModel.kt`
- Create: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiRules.kt`
- Create: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiBoardTest.kt`
- Create: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiMovementTest.kt`
- Create: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiCaptureTest.kt`
- Create: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiTestFixtures.kt`
- Modify: `README.md`
- Modify: `AGENTS.md`

**Interfaces:**
- Produces:

```kotlin
data class DoushouqiPosition(val row: Int, val column: Int)
enum class DoushouqiSide { PINE_GREEN, VERMILION; fun other(): DoushouqiSide }
enum class DoushouqiAnimal(val label: String, val rank: Int)
data class DoushouqiPiece(val side: DoushouqiSide, val animal: DoushouqiAnimal)
data class DoushouqiMove(val from: DoushouqiPosition, val to: DoushouqiPosition)
enum class DoushouqiTerrain { LAND, RIVER, TRAP, DEN }
class DoushouqiState
object DoushouqiRules {
    fun legalMoves(state: DoushouqiState): List<DoushouqiMove>
    fun apply(state: DoushouqiState, move: DoushouqiMove): DoushouqiState?
}
```

`DoushouqiState` also exposes this package-internal deterministic fixture constructor, used by rules, search fixtures, and no Android code:

```kotlin
internal fun DoushouqiState.Companion.fromPieces(
    sideToMove: DoushouqiSide,
    pieces: Map<DoushouqiPosition, DoushouqiPiece>,
    quietHalfMoves: Int = 0,
    repetitionCounts: Map<Long, Int>? = null,
): DoushouqiState
```

`DoushouqiTestFixtures.kt` defines the exact helpers used below:

```kotlin
internal fun pos(row: Int, column: Int) = DoushouqiPosition(row, column)
internal fun green(animal: DoushouqiAnimal) =
    DoushouqiPiece(DoushouqiSide.PINE_GREEN, animal)
internal fun red(animal: DoushouqiAnimal) =
    DoushouqiPiece(DoushouqiSide.VERMILION, animal)
internal fun move(from: DoushouqiPosition, to: DoushouqiPosition) =
    DoushouqiMove(from, to)
internal fun stateOf(
    sideToMove: DoushouqiSide = DoushouqiSide.PINE_GREEN,
    vararg pieces: Pair<DoushouqiPosition, DoushouqiPiece>,
) = DoushouqiState.fromPieces(sideToMove, linkedMapOf(*pieces))
internal fun legal(state: DoushouqiState) = DoushouqiRules.legalMoves(state)
```

- Consumes later: search, session, and UI use only these public model/rules interfaces.

- [ ] **Step 1: Write board/initial-position tests**

```kotlin
@Test
fun standardTerrainAndInitialInventoryAreExact() {
    val state = DoushouqiState.initial()
    assertEquals(9, DoushouqiState.ROWS)
    assertEquals(7, DoushouqiState.COLUMNS)
    assertEquals(DoushouqiTerrain.DEN, terrainAt(pos(0, 3)))
    assertEquals(DoushouqiTerrain.DEN, terrainAt(pos(8, 3)))
    assertEquals(DoushouqiTerrain.RIVER, terrainAt(pos(3, 1)))
    assertEquals(DoushouqiTerrain.RIVER, terrainAt(pos(5, 5)))
    assertEquals(16, state.pieces().size)
    assertEquals(green(DoushouqiAnimal.ELEPHANT), state.pieceAt(pos(6, 0)))
    assertEquals(red(DoushouqiAnimal.LION), state.pieceAt(pos(0, 0)))
    assertEquals(DoushouqiSide.PINE_GREEN, state.sideToMove)
}

@Test
fun exposedCollectionsCannotMutateState() {
    val state = DoushouqiState.initial()
    val exported = state.boardSnapshot().toMutableList()
    exported.fill(null)
    assertEquals(16, state.pieces().size)
}
```

- [ ] **Step 2: Run board RED**

Run `DoushouqiBoardTest`; expect missing model symbols.

- [ ] **Step 3: Implement coordinates, terrain, pieces, and immutable state**

Use fixed `ROWS = 9`, `COLUMNS = 7`, row-major `index = row * COLUMNS + column`, defensive `List.toList()` and `Map.toMap()`, exact initial positions from the spec, and:

```kotlin
internal fun terrainAt(position: DoushouqiPosition): DoushouqiTerrain = when {
    position in DENS -> DoushouqiTerrain.DEN
    position in TRAPS -> DoushouqiTerrain.TRAP
    position.row in 3..5 && position.column in setOf(1, 2, 4, 5) ->
        DoushouqiTerrain.RIVER
    else -> DoushouqiTerrain.LAND
}
```

- [ ] **Step 4: Run board GREEN**

Run `DoushouqiBoardTest`; expect all initial board and immutability assertions to pass.

- [ ] **Step 5: Write movement RED tests**

Cover one-square orthogonal moves, diagonal rejection, own-den rejection, Rat river entry/exit, non-Rat water rejection, Lion/Tiger horizontal and vertical jumps, and either side's Rat blocking any crossed river square:

```kotlin
@Test
fun ratBlocksLionJumpRegardlessOfSide() {
    listOf(
        green(DoushouqiAnimal.RAT),
        red(DoushouqiAnimal.RAT),
    ).forEach { blocker ->
        val state = stateOf(
            sideToMove = DoushouqiSide.PINE_GREEN,
            pos(3, 0) to green(DoushouqiAnimal.LION),
            pos(3, 1) to blocker,
        )
        assertFalse(move(pos(3, 0), pos(3, 3)) in DoushouqiRules.legalMoves(state))
    }
}
```

- [ ] **Step 6: Run movement RED, implement move generation, then run GREEN**

Generate deterministic row-major legal moves. For Lion/Tiger, scan only when the adjacent square is river, reject any Rat in the traversed span, and land on the first non-river square.

- [ ] **Step 7: Write capture RED tests**

Cover equal/lower rank, stronger defender rejection, Rat captures Elephant on land, Elephant never captures Rat, Rat-versus-Rat only when both share land/water terrain, no cross-boundary capture, jump capture, enemy trapped defender rank zero, and trapped attacker restoring rank when leaving.

```kotlin
@Test
fun landRatCapturesElephantButElephantCannotCaptureRat() {
    val from = pos(4, 0)
    val to = pos(5, 0)
    assertTrue(
        move(from, to) in legal(
            stateOf(
                from to green(DoushouqiAnimal.RAT),
                to to red(DoushouqiAnimal.ELEPHANT),
            )
        )
    )
    assertFalse(
        move(from, to) in legal(
            stateOf(
                from to green(DoushouqiAnimal.ELEPHANT),
                to to red(DoushouqiAnimal.RAT),
            )
        )
    )
}
```

- [ ] **Step 8: Run capture RED, implement capture predicate, then run GREEN**

Keep capture policy in one function:

```kotlin
internal fun canCapture(
    attacker: DoushouqiPiece,
    from: DoushouqiPosition,
    defender: DoushouqiPiece,
    to: DoushouqiPosition,
): Boolean
```

Reject same-side destinations before this predicate.

- [ ] **Step 9: Run the module suite and sync docs**

Run `./gradlew :games:doushouqi:testDebugUnitTest`; document the exact standard movement/capture rules in all three README/agent files.

- [ ] **Step 10: Commit**

```text
feat: implement Doushouqi movement rules

- Add immutable standard board state and deterministic legal moves
- Cover rivers, jumps, traps, ranks, and Rat-Elephant captures
```

---

### Task 3: Move Application, Repetition, And Terminal Precedence

**Files:**
- Modify: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiModel.kt`
- Modify: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiRules.kt`
- Create: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiStateTest.kt`
- Create: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiTerminalTest.kt`
- Modify: `README.md`
- Modify: `AGENTS.md`

**Interfaces:**
- Extends `DoushouqiState` with:

```kotlin
val quietHalfMoves: Int
val repetitionCounts: Map<Long, Int>
val positionKey: Long
val lastMove: DoushouqiMove?
val result: DoushouqiResult?
```

- Produces:

```kotlin
enum class DoushouqiWinReason { DEN, FINAL_CAPTURE, NO_LEGAL_MOVE }
enum class DoushouqiDrawReason { REPETITION, QUIET_100 }
sealed interface DoushouqiResult {
    data class Win(val winner: DoushouqiSide, val reason: DoushouqiWinReason) : DoushouqiResult
    data class Draw(val reason: DoushouqiDrawReason) : DoushouqiResult
}
```

- [ ] **Step 1: Write move-application RED tests**

Assert illegal application returns `null`, source state stays unchanged, legal application moves/captures exactly once, side switches, last move updates, quiet count increments/resets, and the resulting position count increments.

- [ ] **Step 2: Run RED and implement base successor construction**

`DoushouqiRules.apply` first resolves against current `legalMoves`; `applyUnchecked` copies the board, moves the piece, toggles side, updates quiet count and repetition map, then adjudicates.

- [ ] **Step 3: Write terminal-precedence RED tests**

Use compact custom positions to prove:

```kotlin
@Test fun denEntryWinsBeforeThirdRepetition()
@Test fun finalCaptureWinsBeforeQuietHundred()
@Test fun completingMoveWinsWhenNextSideHasNoLegalMove()
@Test fun thirdOccurrenceDrawsWhenNoWinExists()
@Test fun hundredthQuietHalfMoveDrawsWhenNoWinExists()
```

Also assert draw result reasons and winning sides exactly.

- [ ] **Step 4: Run RED and implement adjudication in exact order**

Use:

```kotlin
private fun adjudicate(after: BaseSuccessor): DoushouqiResult? {
    if (after.enteredEnemyDen) return Win(after.mover, DEN)
    if (after.enemyPieceCount == 0) return Win(after.mover, FINAL_CAPTURE)
    if (legalMoves(after.stateWithoutResult).isEmpty()) return Win(after.mover, NO_LEGAL_MOVE)
    if (after.repetitionCount >= 3) return Draw(REPETITION)
    if (after.quietHalfMoves >= 100) return Draw(QUIET_100)
    return null
}
```

- [ ] **Step 5: Verify state/result integrity**

Run `DoushouqiStateTest`, `DoushouqiTerminalTest`, then the full module suite. Confirm `git diff --check`.

- [ ] **Step 6: Sync docs and commit**

```text
feat: adjudicate Doushouqi games

- Track immutable repetition and quiet-move context
- Enforce den, capture, mobility, and draw precedence
```

---

### Task 4: Deterministic 1–10 Search

**Files:**
- Create: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiAi.kt`
- Create: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiSearchEngine.kt`
- Create: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiAiTest.kt`
- Create: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiSearchEngineTest.kt`
- Modify: `README.md`
- Modify: `AGENTS.md`

**Interfaces:**

```kotlin
data class DoushouqiAiLevel(
    val level: Int,
    val maxDepth: Int,
    val nodeBudget: Int,
    val timeBudgetMillis: Long,
    val weakeningPool: Int,
    val weakeningPercent: Int,
    val tacticalExtension: Int,
) {
    companion object { fun forLevel(level: Int): DoushouqiAiLevel }
}

data class DoushouqiSearchResult(
    val move: DoushouqiMove,
    val completedDepth: Int,
    val nodes: Int,
    val timedOut: Boolean,
)

object DoushouqiAi {
    fun chooseMove(
        state: DoushouqiState,
        level: DoushouqiAiLevel,
        nanoTime: () -> Long = System::nanoTime,
    ): DoushouqiMove?
}
```

- [ ] **Step 1: Write exact level-table RED test**

Assert the ten rows from the design spec exactly, plus monotonic depth/nodes/time/extensions and weakening disabled for `6..10`.

- [ ] **Step 2: Run RED and implement immutable level configuration**

Store the ten entries in one private list; `forLevel` requires `1..10` and returns the exact entry.

- [ ] **Step 3: Write legal fallback/determinism RED tests**

Assert every nonterminal state with legal moves returns one of them even when the injected clock is already expired, and repeated calls with identical state/level return the same move.

- [ ] **Step 4: Implement deterministic fallback and evaluation**

Sort moves by immediate result, capture value, den-distance improvement, trap control, and row-major coordinates. Evaluation uses material, den routes, mobility, trap/den defense, river Rats, and open Lion/Tiger lanes. No random source is permitted.

- [ ] **Step 5: Write tactical search RED tests**

Create exact positions for immediate den win, forced den defense, final capture, Rat taking Elephant, blocked Lion jump, poisoned capture, and choosing repetition draw over forced loss.

- [ ] **Step 6: Implement iterative deepening search**

`DoushouqiSearchEngine` owns primitive arrays for board, side, quiet count, and repetition deltas; make/unmake restores every field. Use Negamax, alpha-beta, bounded transposition entries, PV/capture/killer/history ordering, terminal mate distance, and tactical extension only for capture/den threat/defense nodes.

- [ ] **Step 7: Enforce budgets and weakening**

Check node/deadline before expansion. Preserve the last fully completed iteration. For levels `1..5`, select deterministically within the configured root pool using a stable mix of `positionKey`, level, and completed depth; levels `6..10` select the best score.

- [ ] **Step 8: Run targeted and full tests**

Run both AI test classes, then `./gradlew :games:doushouqi:testDebugUnitTest`. Record observed completed-depth assertions only for deterministic tactical fixtures; do not claim statistical calibration.

- [ ] **Step 9: Sync docs and commit**

```text
feat: add Doushouqi search engine

- Implement deterministic budgeted iterative-deepening alpha-beta
- Define exact score-driven levels and tactical regression coverage
```

---

### Task 5: Session, Score, Undo, Rotation, And Stale Requests

**Files:**
- Create: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiSession.kt`
- Create: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiSessionTest.kt`
- Modify: `README.md`
- Modify: `AGENTS.md`

**Interfaces:**

```kotlin
enum class DoushouqiMode { SINGLE_PLAYER, TWO_PLAYERS }
data class DoushouqiScore(
    val player: Int = 0,
    val robot: Int = 0,
    val green: Int = 0,
    val vermilion: Int = 0,
)
data class DoushouqiRobotRequest(
    val generation: Long,
    val sourcePositionKey: Long,
    val state: DoushouqiState,
    val level: DoushouqiAiLevel,
)
data class DoushouqiSessionState(
    val position: DoushouqiState,
    val playerSide: DoushouqiSide,
    val score: DoushouqiScore,
    val historySize: Int,
    val generation: Long,
    val robotRequest: DoushouqiRobotRequest?,
)
class DoushouqiSession(mode: DoushouqiMode)
```

Methods: `play(move)`, `applyRobotMove(request, move)`, `undo()`, `restart()`, and `invalidate()`, each returning current `DoushouqiSessionState`.

- [ ] **Step 1: Write score/side-policy RED tests**

Assert player-versus-robot identities across colors, Green/Vermilion two-player score, intelligence `player + 1` capped at 10, win swaps player side, loss restores Pine Green, and both draw reasons preserve side/score/level.

- [ ] **Step 2: Run RED and implement score/round policy**

Keep result-to-score mapping centralized and compare `Win.winner` with `playerSide`.

- [ ] **Step 3: Write undo/restart RED tests**

Assert single-player history snapshot is immediately before the human move and robot response, two-player history is one move, initial robot opening is not undoable, and undo restores state/repetition/quiet/result/score/last move.

- [ ] **Step 4: Implement session transitions**

Session alone mutates its private current snapshot; callers receive immutable projections. Restart increments generation, clears history, applies next-side policy, and emits a robot request when the player is Vermilion.

- [ ] **Step 5: Write generation RED tests**

Assert an old request is rejected after a human move, undo, restart, another robot application, and `invalidate()`. Assert request and current source keys must both match.

- [ ] **Step 6: Implement generation-bound robot application**

`applyRobotMove` returns unchanged state for a stale request or illegal robot move; a valid move applies exactly once and increments generation.

- [ ] **Step 7: Write view/tap RED tests**

```kotlin
@Test fun vermilionHumanUsesRotatedCoordinatesInBothDirections()
@Test fun twoPlayerAndGreenHumanKeepNormalCoordinates()
@Test fun tapResolutionRegeneratesMovesAfterRobotReply()
@Test fun winsHideUndoWhileDrawsKeepUndoVisible()
@Test fun latestMoveRestoresThroughUndo()
```

Mapping is exact:

```kotlin
fun modelPosition(displayRow: Int, displayColumn: Int, rotated: Boolean) =
    if (rotated) DoushouqiPosition(8 - displayRow, 6 - displayColumn)
    else DoushouqiPosition(displayRow, displayColumn)
```

- [ ] **Step 8: Run session/full tests, sync docs, and commit**

```text
feat: add Doushouqi sessions

- Implement score-aware side swaps, paired undo, and draw preservation
- Reject stale asynchronous robot results with generation-bound requests
```

---

### Task 6: Compose UI, Plugin, Accessibility, And Package Icon

**Files:**
- Create: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiVisuals.kt`
- Create: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiScreen.kt`
- Create: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiPlugin.kt`
- Create: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiPluginTest.kt`
- Create: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiVisualsTest.kt`
- Create: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiAssetsTest.kt`
- Create: `games/doushouqi/package/assets/icon.png`
- Modify: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiManifestTest.kt`
- Modify: `README.md`
- Modify: `AGENTS.md`

**Interfaces:**
- Produces `DoushouqiPlugin : GamePlugin`, `DoushouqiMenu`, `DoushouqiGameLayout`, `DoushouqiBoard`, semantic token constants, and package asset.
- Consumes session state and the existing `GameContext` without changing `game-api`.

- [ ] **Step 1: Write visual-token and mapping RED tests**

Assert exact SSOT colors, family layout constants (`28`, `34`, `300`, `0.94`, `320`, `0.88`, breakpoint `900`), piece scale `0.78`, and shared marker constants `0.92`, `0.04`, `0.18`, `0xB84FCBFF`, `0x70115C93`.

- [ ] **Step 2: Run RED and implement `DoushouqiVisuals.kt`**

Use semantic names only; Compose components read these constants rather than scattering raw colors.

- [ ] **Step 3: Write plugin/menu contract RED tests**

Assert `DoushouqiPlugin.manifest == DoushouqiManifest.gameManifest`, version label `版本 0.0.1`, and exact menu labels.

- [ ] **Step 4: Implement menu and plugin skeleton**

`MainScreen` loads `assets/icon.png` with a null-safe fallback, then renders empty board + rail. `GameScreen` creates one session and one single-thread daemon executor named `doushouqi-ai`; `DisposableEffect` closes dispatcher/executor.

- [ ] **Step 5: Implement the board and rail from the visual SSOT**

Board requirements:

- exact 7×9 clickable cell grid and terrain;
- Compose-drawn river, traps, dens, jump cues, tiles, animal labels;
- 78% tile scale and upright labels;
- selected inner border, legal dot, capture broken ring, shared latest marker;
- normal/rotated coordinate mapping;
- board semantics include side, animal label, row, column, selected/legal state.

Rail requirements:

- mode, score, current turn, player side, level, result reason;
- `智能思考中` disables board input;
- `悔棋`, `重新开始`, `返回菜单` visibility follows session helpers;
- every control is at least `48dp`, with `8dp` gaps and visible press/disabled feedback.

- [ ] **Step 6: Write the icon RED test**

```kotlin
@Test
fun packageIconIsReadableCircularSafePng() {
    val image = ImageIO.read(repositoryRoot()
        .resolve("games/doushouqi/package/assets/icon.png"))
    assertNotNull(image)
    assertEquals(1024, image.width)
    assertEquals(1024, image.height)
    assertTrue(cornersAreTransparentOrCanvasSafe(image))
    assertTrue(subjectCoverage(image) in 0.20f..0.72f)
}

private fun cornersAreTransparentOrCanvasSafe(image: BufferedImage): Boolean {
    val corners = listOf(
        image.getRGB(0, 0),
        image.getRGB(image.width - 1, 0),
        image.getRGB(0, image.height - 1),
        image.getRGB(image.width - 1, image.height - 1),
    )
    if (corners.all { color -> color ushr 24 == 0 }) return true
    val reference = corners.first()
    return corners.all { color ->
        listOf(16, 8, 0).all { shift ->
            kotlin.math.abs(
                ((reference shr shift) and 0xFF) - ((color shr shift) and 0xFF)
            ) <= 12
        }
    }
}

private fun subjectCoverage(image: BufferedImage): Float {
    val background = image.getRGB(0, 0)
    var subject = 0
    var samples = 0
    for (y in 0 until image.height step 8) {
        for (x in 0 until image.width step 8) {
            samples++
            val color = image.getRGB(x, y)
            val differs = listOf(16, 8, 0).any { shift ->
                kotlin.math.abs(
                    ((background shr shift) and 0xFF) - ((color shr shift) and 0xFF)
                ) > 24
            }
            if (differs) subject++
        }
    }
    return subject.toFloat() / samples
}
```

- [ ] **Step 7: Run icon RED, generate the icon, and rerun GREEN**

Generate one production icon derived from the SSOT: circular porcelain/territory medallion, opposing Pine Green and Vermilion animal silhouettes separated by a mineral-blue river, no text, no watermark. Save only the accepted 1024×1024 PNG to the package path and visually inspect it.

- [ ] **Step 8: Run plugin/visual/assets and full module tests**

Run the three targeted classes and the full module suite. Compile errors in Compose count as RED and are fixed without weakening tests.

- [ ] **Step 9: Perform the designer delivery pass**

Check contrast, touch sizes, narrow layout, semantics, reduced motion, and remove one nonfunctional decoration. Update `designs/specs/doushouqi-ui.md`, paired prompt docs, `README.md`, and `AGENTS.md` only if implementation changes the approved detail.

- [ ] **Step 10: Commit**

```text
feat: build Doushouqi interface

- Implement the double-river Compose board and accessible score rail
- Wire generation-safe background play and package-owned icon
```

---

### Task 7: Built-In Packaging, Shell Version, Documentation, And Verification

**Files:**
- Modify: `build.gradle.kts`
- Modify: `app/build.gradle.kts`
- Modify: `package.json`
- Modify: `README.md`
- Modify: `AGENTS.md`
- Create: `games/doushouqi/README.md`
- Modify: `docs/superpowers/plans/2026-07-26-doushouqi-game.md` (check completed boxes only)

**Interfaces:**
- Produces `packageDoushouqiGame`, `build:game:doushouqi`, `doushouqi.zip`, and `assets/builtin-games/doushouqi.zip`.
- Preserves manifest-driven shell discovery and `CURRENT_SHELL_API = 1`.

- [ ] **Step 1: Write/extend packaging RED checks**

Before root integration, run:

```bash
./gradlew packageDoushouqiGame
```

Expected: failure because the root task is not registered.

- [ ] **Step 2: Register the root package and verification entry**

Add:

```kotlin
registerGamePackageTask("packageDoushouqiGame", "doushouqi")
```

Change the package verification list to:

```kotlin
listOf("gomoku", "othello", "xiangqi", "chess", "junqi", "doushouqi")
```

- [ ] **Step 3: Add the shell built-in dependency and version**

In `app/build.gradle.kts`, add `packageDoushouqiGame` to `copyBuiltinGamePackages`, set `versionCode = 5`, and set `versionName = "0.0.5"`. Do not add a game-ID branch to shell Kotlin code.

- [ ] **Step 4: Add npm scripts**

Append `&& npm run build:game:doushouqi` to `build:game` and add:

```json
"build:game:doushouqi": "./gradlew packageDoushouqiGame"
```

- [ ] **Step 5: Run package GREEN**

Run `./gradlew packageDoushouqiGame`. Inspect `build/game-packages/doushouqi.zip` and require exactly the needed top-level manifest/plugin plus `assets/icon.png`.

- [ ] **Step 6: Finish documentation**

`games/doushouqi/README.md` documents:

- version `0.0.1`;
- exact initial layout and rules;
- repetition/quiet draw precedence;
- single/two-player score, side, undo, restart, rotation;
- exact AI table and honest no-calibration statement;
- Compose-owned board/icon asset;
- targeted package/full verification commands.

Update root `README.md` and `AGENTS.md` from five to six built-ins, list the new module/version/scripts, add Doushouqi behavior and boundaries, and remove all “approved but not implemented” wording.

- [ ] **Step 7: Run targeted verification**

```bash
./gradlew :games:doushouqi:testDebugUnitTest
./gradlew packageDoushouqiGame
```

Both commands must exit zero with no failed tests.

- [ ] **Step 8: Run full completion gate**

```bash
npm run verify
```

Require exit zero for all tests, six package validations, and debug APK inclusion.

- [ ] **Step 9: Inspect final state**

Run:

```bash
git diff --check
git status --short
git diff --stat
```

Re-read the formal spec and map every requirement to code or a passing test. Do not claim emulator acceptance unless `npm start` is also run.

- [ ] **Step 10: Commit**

```text
feat: integrate Doushouqi game package

- Bundle the sixth independent game and bump the shell to 0.0.5
- Document and verify the complete package across the repository
```
