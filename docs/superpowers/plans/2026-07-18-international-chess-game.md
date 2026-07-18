# International Chess Game Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a complete, independently packaged International Chess game with standard rules, Xiangqi-family tablet UX, and deterministic offline AI levels 1-10.

**Architecture:** A new `games/chess` Android library owns its immutable public game state, mutable search position, session transitions, AI, and Compose UI. The shell only adds catalog presentation and build/package wiring; `game-api` remains unchanged. Rules and session behavior use immutable snapshots, while search uses allocation-conscious make/unmake and iterative-deepening Negamax away from the UI thread.

**Tech Stack:** Kotlin 2.4, Jetpack Compose, JUnit 4, Android Gradle Plugin 9.2.1, pure-Kotlin alpha-beta chess search, existing zip plus `DexClassLoader` plugin pipeline.

## Global Constraints

- Package identity is `chess`, display name is `国际象棋`, entry class is `com.buddygames.chess.ChessPlugin`.
- Initial game version is exactly `versionCode = 1`, `versionName = 0.0.1`; plugin and package manifest must match.
- Shell version becomes exactly `versionCode = 2`, `versionName = 0.0.2`.
- Main menu labels are exactly `单人模式`, `双人对战`, `退出游戏`.
- White moves first; the single-player human starts as White.
- A human win swaps sides, a human loss restores White, and a draw keeps the human side.
- A Black human sees a 180-degree mapped board and receives an immediate White robot opening.
- Latest move uses the shared `0.92f` scale, `0.04f` inset, `0.18f` corner length, and `0xB84FCBFF` color.
- AI is pure Kotlin, deterministic for a position and level, legal-move safe, score-driven from level 1 through 10, and runs outside the Compose UI thread.
- `game-api` public types and `CURRENT_SHELL_API` do not change.
- Every repository change updates `AGENTS.md`; do not stage the unrelated untracked `desgins/` directory.

---

### Task 1: Chess Module And Standard Rules

**Files:**
- Create: `games/chess/build.gradle.kts`
- Create: `games/chess/src/main/java/com/buddygames/chess/ChessRules.kt`
- Create: `games/chess/src/test/java/com/buddygames/chess/ChessRulesTest.kt`
- Modify: `settings.gradle.kts`
- Modify: `AGENTS.md`

**Interfaces:**
- Produces: `ChessSide`, `ChessPieceType`, `ChessPiece`, `ChessMove`, `ChessCastlingRights`, `ChessState`, `ChessResult`, and `ChessRules`.
- `ChessState.initial(): ChessState`, `ChessState.empty(sideToMove: ChessSide = WHITE): ChessState`, `ChessState.put(square: Int, piece: ChessPiece): ChessState`, and `ChessState.apply(move: ChessMove): ChessState`.
- `ChessRules.legalMoves(state: ChessState): List<ChessMove>`, `ChessRules.isInCheck(state: ChessState, side: ChessSide): Boolean`, `ChessRules.result(state: ChessState, repetitions: Int = 1): ChessResult?`.

- [ ] **Step 1: Scaffold the library and write failing model tests**

Add JUnit tests that assert:

```kotlin
assertEquals(20, ChessRules.legalMoves(ChessState.initial()).size)
assertEquals(ChessSide.WHITE, ChessState.initial().sideToMove)
assertEquals(ChessPiece(ChessSide.WHITE, ChessPieceType.KING), ChessState.initial().pieceAt("e1"))
assertEquals(ChessPiece(ChessSide.BLACK, ChessPieceType.QUEEN), ChessState.initial().pieceAt("d8"))
```

Include tests for square parsing (`a1 = 0`, `h8 = 63`) and immutable `put`.

- [ ] **Step 2: Run the rules test and verify RED**

Run:

```bash
./gradlew :games:chess:testDebugUnitTest
```

Expected: compilation fails because the chess model and rules do not exist.

- [ ] **Step 3: Implement model and pseudo-legal movement**

Implement compact square indexing (`rank * 8 + file`), immutable board copies,
initial placement, piece-specific pseudo-legal movement, state updates, castling
rights, en-passant target, half-move clock, and full-move number. Keep rule code
free of Compose and Android types.

- [ ] **Step 4: Add failing special-rule and king-safety tests**

Add focused tests for:

```kotlin
assertFalse(ChessMove.fromUci("e2e4") in ChessRules.legalMoves(pinnedPawnState))
assertTrue(ChessMove.fromUci("e1g1") in ChessRules.legalMoves(clearCastleState))
assertFalse(ChessMove.fromUci("e1g1") in ChessRules.legalMoves(attackedTransitState))
assertTrue(ChessMove.fromUci("e5d6") in ChessRules.legalMoves(enPassantState))
assertEquals(
    setOf(QUEEN, ROOK, BISHOP, KNIGHT),
    ChessRules.legalMoves(promotionState).mapNotNull { it.promotion }.toSet()
)
```

Also assert that applying en passant removes the captured pawn and applying
castling moves the rook.

- [ ] **Step 5: Run tests and verify the new cases fail**

Run the same module test command. Expected: special rules and self-check
filtering fail while basic movement passes.

- [ ] **Step 6: Implement legal filtering and special moves**

Filter pseudo-legal moves by applying each move and checking the moving king.
Require an unattacked origin, transit, and destination for castling. Generate all
four promotions and preserve the prior state values needed by later make/unmake.

- [ ] **Step 7: Add failing terminal-result tests**

Cover Fool's Mate as `BLACK_WIN`, a standard stalemate as `DRAW_STALEMATE`,
half-move clock 100 as `DRAW_FIFTY_MOVE`, repetition count 3 as
`DRAW_REPETITION`, and each insufficient-material case from the design.

- [ ] **Step 8: Implement terminal results and run GREEN**

Implement:

```kotlin
enum class ChessResult {
    WHITE_WIN,
    BLACK_WIN,
    DRAW_STALEMATE,
    DRAW_FIFTY_MOVE,
    DRAW_REPETITION,
    DRAW_INSUFFICIENT_MATERIAL
}
```

Run:

```bash
./gradlew :games:chess:testDebugUnitTest
```

Expected: all chess rule tests pass.

- [ ] **Step 9: Update agent structure guidance and commit**

Document `games/chess` and its full standard-rule boundary in `AGENTS.md`.

```bash
git add settings.gradle.kts games/chess/build.gradle.kts games/chess/src/main/java/com/buddygames/chess/ChessRules.kt games/chess/src/test/java/com/buddygames/chess/ChessRulesTest.kt AGENTS.md
git commit -m "$(cat <<'EOF'
feat: add international chess rules

- Add the independently testable chess game module and standard position model
- Implement legal moves, special moves, check, checkmate, and draw detection
EOF
)"
```

### Task 2: Offline Search And Ten-Level Intelligence

**Files:**
- Create: `games/chess/src/main/java/com/buddygames/chess/ChessAi.kt`
- Create: `games/chess/src/main/java/com/buddygames/chess/ChessSearchPosition.kt`
- Create: `games/chess/src/main/java/com/buddygames/chess/ChessSearchEngine.kt`
- Create: `games/chess/src/test/java/com/buddygames/chess/ChessSearchEngineTest.kt`
- Modify: `AGENTS.md`

**Interfaces:**
- Consumes: immutable model and legality from Task 1.
- Produces: `ChessAiGradient.config(level)`, `ChessAi.search(state, level, limits, shouldStop)`, and `ChessSearchResult`.
- `ChessAiLevelConfig` contains exact level, max depth, node budget, think time, candidate pool, suboptimal percent, evaluation profile, and quiescence depth.

- [ ] **Step 1: Write failing level-gradient tests**

Assert exact values from the design table:

```kotlin
assertEquals((1..10).toList(), configs.map { it.level })
assertEquals(listOf(1, 2, 3, 3, 4, 5, 5, 6, 7, 8), configs.map { it.maxDepth })
assertEquals(
    listOf(2_000, 6_000, 18_000, 45_000, 100_000, 220_000, 450_000, 800_000, 1_500_000, 2_500_000),
    configs.map { it.nodeBudget }
)
assertTrue(configs.zipWithNext().all { (a, b) ->
    a.maxDepth <= b.maxDepth &&
        a.nodeBudget <= b.nodeBudget &&
        a.maxThinkTimeMillis <= b.maxThinkTimeMillis &&
        a.quiescenceDepth <= b.quiescenceDepth
})
```

- [ ] **Step 2: Run search tests and verify RED**

Run:

```bash
./gradlew :games:chess:testDebugUnitTest --tests com.buddygames.chess.ChessSearchEngineTest
```

Expected: compilation fails because search types do not exist.

- [ ] **Step 3: Implement level configuration and mutable search position**

Use primitive arrays for board and undo state. Implement legal make/unmake,
attack detection, Zobrist hash updates, material counts, and conversion to and
from `ChessState`. Add a parity test comparing mutable-position legal moves with
`ChessRules.legalMoves` across the initial position and at least three special
positions.

- [ ] **Step 4: Write failing tactical and control tests**

Add tests that require:

- every returned move is legal;
- mate in one is selected at levels 1, 5, and 10;
- a defended pawn capture that loses a queen is rejected at level 8;
- identical position plus level returns the same move;
- node, deadline, and cancellation stops return a legal fallback;
- completed depth reports the last fully completed iteration.

- [ ] **Step 5: Implement iterative-deepening Negamax**

Implement alpha-beta Negamax with:

```kotlin
private const val MATE_SCORE = 1_000_000
private const val INFINITY = 1_100_000
```

Use bounded power-of-two transposition storage, mate-distance scores, preferred
move ordering, MVV-LVA captures, promotion priority, two killer slots per ply,
and history scores. Preserve the root move from the last completed iteration.

- [ ] **Step 6: Implement evaluation and quiescence**

Implement material, piece-square, mobility/development, pawn structure, passed
pawns, king safety, bishop pair, and rook-file terms behind monotonic
`ChessEvaluationProfile` values. Quiescence searches captures and promotions,
plus legal evasions while in check, to the configured bound.

- [ ] **Step 7: Implement deterministic weakening and run GREEN**

For levels 1-4, select deterministically from the configured top root pool using
the position hash and bounded score loss. Levels 5-10 always select the best
completed root move.

Run:

```bash
./gradlew :games:chess:testDebugUnitTest
```

Expected: all chess rules and search tests pass.

- [ ] **Step 8: Update agent AI guidance and commit**

Record the centralized, monotonic, deterministic AI constraints in `AGENTS.md`.

```bash
git add games/chess/src/main/java/com/buddygames/chess/ChessAi.kt games/chess/src/main/java/com/buddygames/chess/ChessSearchPosition.kt games/chess/src/main/java/com/buddygames/chess/ChessSearchEngine.kt games/chess/src/test/java/com/buddygames/chess/ChessSearchEngineTest.kt AGENTS.md
git commit -m "$(cat <<'EOF'
feat: add offline chess intelligence

- Add allocation-conscious iterative-deepening Negamax with mature move ordering
- Calibrate deterministic score-driven robot strength across ten levels
EOF
)"
```

### Task 3: Session Flow And Tablet Game Interface

**Files:**
- Create: `games/chess/src/main/java/com/buddygames/chess/ChessSession.kt`
- Create: `games/chess/src/main/java/com/buddygames/chess/ChessVisuals.kt`
- Create: `games/chess/src/main/java/com/buddygames/chess/ChessPlugin.kt`
- Create: `games/chess/src/test/java/com/buddygames/chess/ChessSessionTest.kt`
- Create: `games/chess/package/manifest.json`
- Create: `games/chess/package/assets/icon.txt`
- Modify: `AGENTS.md`

**Interfaces:**
- Consumes: rules and AI from Tasks 1-2 plus unchanged `GamePlugin`.
- Produces: public no-arg `ChessPlugin`, `ChessRound`, `ChessSnapshot`, `ChessScore`, and Compose main/game screens.
- Plugin manifest is exactly `1 / 0.0.1` and matches package JSON.

- [ ] **Step 1: Write failing session and version tests**

Test exact menu/version values, `ChessScore.intelligenceLevel`, human-versus-robot
scoring across color swaps, White-versus-Black two-player scoring, next-round
side rules, Black robot opening detection, and coordinate mapping:

```kotlin
assertEquals(63, chessBoardSquare(0, rotated = true))
assertEquals(0, chessBoardSquare(63, rotated = true))
assertEquals(1, ChessScore().intelligenceLevel)
assertEquals(10, ChessScore(player = 20).intelligenceLevel)
assertEquals("版本 0.0.1", chessVersionLabel("0.0.1"))
```

- [ ] **Step 2: Run session tests and verify RED**

Run the chess module tests. Expected: compilation fails because session and
plugin types do not exist.

- [ ] **Step 3: Implement session transitions and snapshots**

Track state, selected square, result, score, history, position-key counts, and
last move. Record snapshots immediately before legal player moves. Implement
single-player pair undo, two-player one-ply undo, result-aware score restoration,
robot opening outside history, and automatic queen selection for touch
promotion.

- [ ] **Step 4: Add failing visibility and marker tests**

Assert win hides undo, every draw preserves undo, restart appears for all terminal
results, and marker constants match the global constraints exactly.

- [ ] **Step 5: Implement plugin main and game screens**

Follow Xiangqi's screen structure:

- menu title, unified mode buttons, exit, and version;
- 8x8 board with warm light and muted green squares;
- upright white/black Unicode pieces;
- selection, legal destination, capture, check, and latest-move treatments;
- bidirectional rotated coordinate mapping for a Black human;
- right rail with mode, identity-aware score, colorized active side, AI level,
  `将军`, undo, terminal restart, and return menu;
- AI launched on `Dispatchers.Default` and cancelled by round/screen lifecycle.

Keep board sizing stable with an `aspectRatio(1f)` square and ensure side-rail
text fits common 800x600 and 1280x800 tablet layouts.

- [ ] **Step 6: Run tests and compile the module**

Run:

```bash
./gradlew :games:chess:testDebugUnitTest :games:chess:assembleDebug
```

Expected: tests pass and Compose compilation succeeds.

- [ ] **Step 7: Update agent UI/session guidance and commit**

```bash
git add games/chess/src games/chess/package AGENTS.md
git commit -m "$(cat <<'EOF'
feat: add international chess experience

- Add Xiangqi-family round flow, score-aware undo, side swaps, and robot openings
- Build the tablet chess menu, board, status rail, and terminal interactions
EOF
)"
```

### Task 4: Game-Center Catalog And Package Pipeline

**Files:**
- Modify: `build.gradle.kts`
- Modify: `package.json`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/buddygames/center/ui/HomeGamePresentation.kt`
- Modify: `app/src/main/java/com/buddygames/center/ui/HomeGameLogo.kt`
- Modify: `app/src/main/java/com/buddygames/center/ui/GameCenterApp.kt`
- Modify: `app/src/test/java/com/buddygames/center/ui/HomeGamePresentationTest.kt`
- Modify: `AGENTS.md`

**Interfaces:**
- Consumes: `:games:chess` and `packageChessGame`.
- Produces: built-in `chess.zip`, home ordering/logo, four-square landscape rows, shell `0.0.2`.

- [ ] **Step 1: Write failing shell presentation tests**

Require `HomeGameLogo.Chess`, chess order `2`, Othello order `3`, four-column
landscape layout, and shell label `版本 0.0.2`. At 800x600 require four equal
173 dp squares; at 1200x800 require four equal 240 dp squares.

- [ ] **Step 2: Run app tests and verify RED**

Run:

```bash
./gradlew :app:testDebugUnitTest
```

Expected: tests fail because chess presentation and four-column geometry do not
exist.

- [ ] **Step 3: Implement shell presentation**

Add a code-drawn chess knight logo, catalog ordering, and `maxColumns = 4`.
Compute landscape button size as:

```kotlin
((widthDp - horizontalPadding * 2 - gap * 3) / 4f)
    .toInt()
    .coerceAtMost(240)
```

Chunk rows by `layout.maxColumns`. Keep compact portrait behavior as one full
width button per row.

- [ ] **Step 4: Wire module and package tasks**

Register:

```kotlin
registerGamePackageTask("packageChessGame", "chess")
```

Add `build:game:chess`, include it in aggregate build/verify scripts, and add
`packageChessGame` to built-in asset dependencies. Increment app version to
`2 / 0.0.2`.

- [ ] **Step 5: Run targeted integration**

Run:

```bash
./gradlew :app:testDebugUnitTest packageChessGame :app:assembleDebug
```

Expected: app tests pass, `build/game-packages/chess.zip` exists, and the debug
APK contains the generated built-in zip.

- [ ] **Step 6: Update agent command and version guidance and commit**

```bash
git add settings.gradle.kts build.gradle.kts package.json app AGENTS.md
git commit -m "$(cat <<'EOF'
feat: integrate international chess package

- Bundle and expose the chess zip through the existing game-center pipeline
- Add the chess home mark and fit four equal game buttons on landscape tablets
EOF
)"
```

### Task 5: Documentation, Review, And Full Verification

**Files:**
- Modify: `README.md`
- Modify: `docs/agents/game-plugins.md`
- Modify: `AGENTS.md`

**Interfaces:**
- Consumes: completed chess module and shell integration.
- Produces: accurate human and agent commands, package list, behavior summary, and final verification evidence.

- [ ] **Step 1: Update human documentation**

Change three-game wording to four games, add `games/chess`, document standard
rules and ten-level AI, list `build:game:chess`, update aggregate verification
language, output paths, and current Xiangqi version to the actual manifest value.

- [ ] **Step 2: Update plugin and agent documentation**

Add chess package commands and module structure to `docs/agents/game-plugins.md`.
Update `AGENTS.md` current behavior, commands, structure, verification, document
map, and done checklist for the fourth package.

- [ ] **Step 3: Run targeted tests**

Run:

```bash
./gradlew :games:chess:testDebugUnitTest :app:testDebugUnitTest
```

Expected: all targeted tests pass.

- [ ] **Step 4: Run the full project gate**

Run:

```bash
npm run verify
```

Expected: all unit tests, all four game zips, and the debug APK build
successfully.

- [ ] **Step 5: Inspect package contents and repository hygiene**

Run:

```bash
unzip -l build/game-packages/chess.zip
git status --short
git diff --check
```

Expected zip entries include `manifest.json`, `plugin.apk`, and
`assets/icon.txt`. No generated APK, zip, build directory, local properties, or
unrelated `desgins/` files are staged.

- [ ] **Step 6: Request whole-change code review and fix findings**

Review against
`docs/superpowers/specs/2026-07-18-international-chess-game-design.md`, prioritizing
rule correctness, make/unmake parity, cancellation, score/undo identity, rotated
mapping, package loading, and version alignment. Re-run affected tests after each
fix and repeat `npm run verify` after material changes.

- [ ] **Step 7: Commit documentation and verified fixes**

```bash
git add README.md docs/agents/game-plugins.md AGENTS.md
git commit -m "$(cat <<'EOF'
docs: document international chess package

- Add chess setup, build, rules, AI, and package guidance
- Align agent verification and version instructions with all four built-in games
EOF
)"
```

If review fixes changed code after the prior feature commits, stage only those
reviewed paths and create a separate `fix:` commit with an exact bullet summary.
