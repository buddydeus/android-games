# International Chess Game Design

## Goal

Add an independently versioned `chess` zip game package to the offline Android
Pad game center. The game follows the existing Xiangqi package structure and
interaction model while implementing standard international chess rules and a
compact, fully offline 1-10 strength robot.

## Product Scope

- The package identity is `chess`, the display name is `国际象棋`, and the first
  release is `versionCode = 1`, `versionName = 0.0.1`.
- The main menu uses `单人模式`, `双人对战`, and `退出游戏`, and displays
  `版本 0.0.1`.
- The game screen uses an 8x8 square board, a right-side status and action rail,
  and the same overall hierarchy as Xiangqi.
- Both modes support score tracking, latest-move marking, undo, restart, check,
  checkmate, and draw presentation.
- The game is bundled with the debug APK and can also be built and imported as
  `chess.zip`.

Online play, clocks, opening books, PGN import/export, selectable piece themes,
and configurable time controls are outside this release.

## Rules

### Position Model

`ChessState` owns the 64-square board, side to move, castling rights, en-passant
target, half-move clock, and full-move number. `ChessMove` stores source,
destination, optional promotion type, and move flags needed by make/unmake.

White always moves first. The initial board follows the standard orientation:
white pieces on ranks 1-2 and black pieces on ranks 7-8.

### Legal Moves

The rules engine generates pseudo-legal moves by piece and filters out every move
that leaves the moving side's king attacked. It supports:

- pawn single and double advances;
- pawn captures and en-passant captures;
- promotion to queen, rook, bishop, or knight;
- knight, bishop, rook, queen, and king movement;
- king-side and queen-side castling only when rights, empty squares, and attack
  constraints permit it.

For touch play, a pawn promotion destination selects queen promotion
automatically. The search engine still considers all four promotion choices.

### Terminal Results

No legal moves while in check is checkmate and awards the game to the other
side. No legal moves while not in check is stalemate. The session also declares
a draw for the fifty-move rule, threefold repetition, or standard insufficient
material cases: king versus king, king and one bishop or knight versus king, and
king plus bishop versus king plus bishop when both bishops occupy the same color.

## Session Behavior

`ChessRound` and `ChessSnapshot` follow the Xiangqi session pattern.

- Single player starts with the human as White.
- A human win swaps the human side for the next round.
- A human loss restores the human to White, the first-moving side.
- A draw keeps the current human side.
- If the human is Black, the White robot opens immediately and the board is
  rotated 180 degrees so the human side remains at the bottom. Piece glyphs stay
  upright.
- Two-player rounds always begin with White and use the standard White-at-bottom
  board.
- Single-player undo restores the snapshot before the human move and robot
  response. Two-player undo restores one move.
- The initial robot opening is outside undo history.
- Undo restores score, result, check state, repetition state, and the previous
  latest-move marker. Restart clears history.
- The undo button is hidden after a win and remains visible for a draw, matching
  the established Othello draw behavior.

Single-player scores are human versus robot across color swaps. Two-player scores
are White versus Black. The robot level is `humanWins + 1`, clamped to 1-10.

## Robot

### Algorithm

The robot is a pure-Kotlin deterministic chess search suitable for the package's
existing dex pipeline. It uses mature, conventional engine techniques without a
native binary or external runtime dependency:

- iterative-deepening Negamax with alpha-beta pruning;
- legal make/unmake position updates;
- Zobrist-keyed bounded transposition table;
- principal-variation, capture, promotion, killer-move, and history ordering;
- quiescence search for checks, captures, and promotions at stronger levels;
- checkmate-distance scoring;
- deterministic suboptimal root selection at levels 1-4.

Evaluation grows by level:

1. material;
2. material plus piece-square tables;
3. mobility and development;
4. pawn structure, passed pawns, king safety, bishop pair, and rook files.

The engine always chooses an immediate checkmate when found and never returns an
illegal move. Search runs on `Dispatchers.Default`, is cancelled when the screen
or round changes, and observes both a node budget and a wall-clock deadline.

### Strength Gradient

| Level | Max depth | Nodes | Time | Root weakening | Quiescence |
| --- | ---: | ---: | ---: | ---: | ---: |
| 1 | 1 | 2,000 | 80 ms | pool 6, 60% | 0 |
| 2 | 2 | 6,000 | 120 ms | pool 5, 45% | 0 |
| 3 | 3 | 18,000 | 220 ms | pool 4, 30% | 0 |
| 4 | 3 | 45,000 | 350 ms | pool 3, 15% | 1 |
| 5 | 4 | 100,000 | 550 ms | best | 1 |
| 6 | 5 | 220,000 | 850 ms | best | 1 |
| 7 | 5 | 450,000 | 1,200 ms | best | 2 |
| 8 | 6 | 800,000 | 1,700 ms | best | 2 |
| 9 | 7 | 1,500,000 | 2,400 ms | best | 3 |
| 10 | 8 | 2,500,000 | 3,200 ms | best | 4 |

Depth, node budget, time budget, evaluation detail, and quiescence depth are
monotonic. A deadline can stop a level before its nominal depth; the move from
the last fully completed iteration remains valid.

## Interface

### Main Menu

The menu mirrors the Xiangqi composition: textured light board-game background,
prominent international chess title, three unified mode buttons, and the package
version below the actions.

### Game Screen

- The board uses warm light and muted green square colors with coordinate labels.
- White and black pieces use upright Unicode chess glyphs with contrasting
  porcelain and ink treatments.
- Tap a movable piece to select it; legal destinations receive restrained dots
  or capture rings. Tap a destination to move.
- The latest destination uses the shared enlarged translucent bright-blue
  four-corner marker. It stays inside one square and leaves a visible gap around
  the piece.
- When the human plays Black, display coordinates map both ways through the
  180-degree view transform.
- The right rail shows mode, score identities, current turn with only `白方` or
  `黑方` colorized, robot level in single-player mode, and `将军` when the active
  side is checked.
- The right rail exposes `悔棋`, `重新开始` after a terminal result, and
  `返回菜单`, with the established winner-dependent visibility rules.

The game-center home adds a code-drawn knight mark and orders international chess
after Xiangqi. Landscape home buttons use up to four equal squares per row so all
four bundled games fit in the first viewport on common tablets.

## Packaging And Versions

- Add `:games:chess` to Gradle settings.
- Register `packageChessGame` with the existing package task helper.
- Add `build:game:chess`, include it in `build:game` and `verify`, and copy its zip
  into built-in app assets.
- Increment the shell to `versionCode = 2`, `versionName = 0.0.2` because the
  bundled catalog, home ordering, logo, and layout change.
- Keep `ChessPlugin.manifest` and `games/chess/package/manifest.json` aligned at
  `1 / 0.0.1`.

No `game-api` change and no external Android or chess-engine dependency is
required.

## Verification

Rules tests cover initial move counts, blocked movement, self-check filtering,
castling constraints, en passant, all promotions, checkmate, stalemate, fifty
moves, repetition, and insufficient material.

Session tests cover menu/version text, scoring identities, side swaps, Black
robot opening, rotated coordinate mapping, undo snapshots, terminal action
visibility, and latest-move geometry.

Search tests cover legal output, immediate mate preference, poisoned-capture
avoidance, monotonic level budgets, completed-depth reporting, cancellation, and
determinism. Integration verification builds all four game packages and the
debug APK through `npm run verify`.
