# Dou Shou Qi Game Design

## Goal

Add `doushouqi` as the sixth independently versioned zip game package in the offline Android Pad game center. The package implements complete standard 7×9 Dou Shou Qi rules, local single-player and two-player sessions, a deterministic score-driven 1–10 offline robot, and a package-owned Jetpack Compose interface following `designs/specs/doushouqi-ui.md`.

## Product Scope

- Package identity: `gameId = doushouqi`, `displayName = 斗兽棋`.
- Initial game release: `versionCode = 1`, `versionName = 0.0.1`.
- The main menu uses `单人模式`, `双人对战`, and `退出游戏`, and displays `版本 0.0.1`.
- Both modes provide score tracking, latest-move marking, undo, restart, terminal-result presentation, and return to menu.
- The game is bundled into the debug APK and can also be built and imported independently as `doushouqi.zip`.
- The game has no network, native binary, or external runtime dependency.

Online play, clocks, sound, animated water, selectable themes, rule variants, game-record import/export, and opening books are outside the first release.

## Board And Initial Position

### Coordinates And Terrain

`DoushouqiPosition` uses zero-based `(row, column)` coordinates over nine rows and seven columns. Internal orientation places Vermilion at the top and Pine Green at the bottom.

- Vermilion den: `(0, 3)`.
- Vermilion traps: `(0, 2)`, `(0, 4)`, `(1, 3)`.
- Pine Green den: `(8, 3)`.
- Pine Green traps: `(8, 2)`, `(8, 4)`, `(7, 3)`.
- Left river: rows `3..5`, columns `1..2`.
- Right river: rows `3..5`, columns `4..5`.

Every other square is ordinary land. Terrain is immutable and derived from coordinates rather than copied into every state.

### Pieces And Ranks

Each side owns exactly one of each animal:

| Animal | Chinese label | Rank |
| --- | --- | ---: |
| Elephant | `象` | 8 |
| Lion | `狮` | 7 |
| Tiger | `虎` | 6 |
| Leopard | `豹` | 5 |
| Wolf | `狼` | 4 |
| Dog | `狗` | 3 |
| Cat | `猫` | 2 |
| Rat | `鼠` | 1 |

Pine Green begins with:

- Elephant `(6, 0)`, Wolf `(6, 2)`, Leopard `(6, 4)`, Rat `(6, 6)`.
- Cat `(7, 1)`, Dog `(7, 5)`.
- Tiger `(8, 0)`, Lion `(8, 6)`.

Vermilion uses the 180-degree mirrored arrangement:

- Lion `(0, 0)`, Tiger `(0, 6)`.
- Dog `(1, 1)`, Cat `(1, 5)`.
- Rat `(2, 0)`, Leopard `(2, 2)`, Wolf `(2, 4)`, Elephant `(2, 6)`.

Pine Green moves first.

## Rules

### Ordinary Movement

- A piece moves one square orthogonally. Diagonal movement is never legal.
- A piece may enter, occupy, and leave its own den; doing so has no terminal effect.
- Only Rats may enter river squares.
- A Rat may move between adjacent land and river squares when the destination is empty.
- Non-Rat pieces may not enter river squares.
- A move onto an enemy-occupied destination must pass the capture rules below.

### Lion And Tiger River Jump

- A Lion or Tiger on land may move orthogonally across one complete contiguous river span to the first land square on the opposite bank.
- Every crossed square must be river.
- Any Rat of either side on any crossed river square blocks the jump.
- An empty landing square is legal.
- An enemy on the landing square may be captured only when the normal land capture rules permit it.
- A Lion or Tiger never stops inside the river and never jumps across land.

### Capture

- On land, an attacker normally captures an enemy of equal or lower effective rank.
- A Rat on land may capture an Elephant on land.
- An Elephant may never capture a Rat.
- Rat-versus-Rat capture is legal when both Rats are on land or both are already in the river.
- A capture crossing the land/river boundary is illegal. A Rat may cross that boundary only into an empty square.
- Lion and Tiger jump captures are land-to-land captures and use normal capture rules.

An animal occupying the opponent's trap has effective rank zero while it remains on that trap. Any enemy animal may capture that trapped defender. A trapped animal regains its normal rank when moving out of the trap, so an attack from a trap is evaluated with its normal rank at the destination.

### Terminal Results

Terminal checks use this precedence after every legal move:

1. A piece entering the opponent's den wins immediately.
2. Capturing the opponent's final piece wins immediately.
3. If the next side has no legal move, the completing side wins.
4. If the resulting legal-equivalent position has occurred for the third time, the game is a draw.
5. If 100 consecutive half-moves have completed without a capture, the game is a draw.

The repetition key contains the complete piece placement and side to move. Terrain is constant and therefore not stored in the key. `DoushouqiState` retains an immutable repetition-count map initialized with the initial key at count one. A legal move increments the resulting key before terminal adjudication. The quiet half-move counter resets to zero after every capture and otherwise increments by one.

The winning checks precede automatic draws: a den entry, final capture, or no-move win is never replaced by a simultaneous repetition or quiet-move draw.

### State Integrity

`DoushouqiState` is immutable from public callers:

- board storage is defensively copied;
- repetition counts are defensively copied and exposed read-only;
- legal move generation does not mutate the source state;
- applying an illegal move returns no successor and leaves the state unchanged;
- applying a legal move returns a new state with updated side, quiet counter, repetition counts, position key, last move, and result;
- generated moves contain source and destination coordinates plus the captured piece when present.

## Session Behavior

`DoushouqiSession` owns mode, score, current state, human side, history, last move, and robot-request generation. Repetition counts, quiet counter, and result are part of the immutable current state so rules, undo, and search consume one authoritative adjudication context.

### Single Player

- The first round starts with the human as Pine Green, the first-moving side.
- Single-player score identities are player versus robot regardless of board color.
- Robot level is `playerWins + 1`, clamped to `1..10`.
- A player win swaps the human side for the next round.
- A player loss restores the human to Pine Green.
- A draw keeps the current human side, score, and robot level.
- When the human is Vermilion, Pine Green's robot opening runs immediately.
- The human's side is displayed at the bottom. A Vermilion human gets a 180-degree coordinate-mapped board while piece labels remain upright.
- The initial robot opening is outside undo history.
- A normal undo restores the snapshot immediately before the human move and removes the following robot response.

### Two Player

- Pine Green always moves first.
- Score identities are Pine Green versus Vermilion.
- The standard Pine-Green-at-bottom view is fixed.
- Undo restores one legal move.

### Shared Session Rules

- A snapshot contains the complete immutable state, score, last move, human side, and robot generation.
- Undo restores every snapshot field.
- Restart clears history and last move, applies the score/side policy, and starts a robot opening when required.
- A win hides `悔棋`; a draw keeps it available.
- `重新开始` appears after any terminal result.
- Taps are resolved against legal moves regenerated from the current position, never against a stale candidate list retained across a robot response.
- Every move, undo, restart, or menu exit increments a generation token. A robot result applies only when its request generation and source-position key still match.

## Robot

### Boundary And Execution

`DoushouqiAi.chooseMove` receives only an immutable complete-information `DoushouqiState`, whose read-only repetition context is authoritative, and `DoushouqiAiLevel`. Search does not depend on Android UI types.

`DoushouqiPlugin` owns a dedicated disposable background executor. Search never runs on the Compose UI thread. Cancellation, deadline, node budget, and generation checks are observed before expensive expansion and before applying a result.

A deterministic legal fallback is selected before timed search, so a legal position never returns `null` because its budget expired.

### Search

The robot uses pure-Kotlin iterative-deepening Negamax with alpha-beta pruning:

- primitive make/unmake search position;
- bounded Zobrist-keyed transposition table;
- principal-variation, winning, capture, den-threat, killer, and history move ordering;
- repetition-aware path scoring;
- mate-distance scoring;
- bounded tactical extension for captures, forced den entries, and immediate den defenses;
- deterministic root weakening only at levels `1..5`.

Transposition keys include piece placement, side to move, quiet counter, and a stable hash of the current repetition-count context because draw scores are path-dependent.

Move ordering always places an immediate legal win first. Evaluation never overrides a found forced terminal result.

### Evaluation

Evaluation grows with level but remains deterministic:

1. material and terminal distance;
2. distance and safe routes to the opponent den;
3. mobility, trap control, and own-den defense;
4. river Rat control, Lion/Tiger jump lanes, blockers, defended captures, and tactical exposure.

Piece values follow rank but are position-sensitive. Rat value rises when it controls a river or attacks an Elephant; Lion and Tiger value rises with an open jump lane; any piece adjacent to an undefended enemy den receives a large threat bonus.

### Strength Table

| Level | Max depth | Nodes | Time | Root weakening | Tactical extension |
| --- | ---: | ---: | ---: | ---: | ---: |
| 1 | 1 | 1,000 | 60 ms | pool 6, 60% | 0 |
| 2 | 2 | 4,000 | 90 ms | pool 5, 45% | 0 |
| 3 | 3 | 12,000 | 140 ms | pool 4, 30% | 0 |
| 4 | 4 | 35,000 | 220 ms | pool 3, 20% | 1 |
| 5 | 5 | 80,000 | 350 ms | pool 2, 10% | 1 |
| 6 | 6 | 180,000 | 550 ms | best | 1 |
| 7 | 7 | 400,000 | 850 ms | best | 2 |
| 8 | 8 | 800,000 | 1,200 ms | best | 2 |
| 9 | 9 | 1,500,000 | 1,800 ms | best | 3 |
| 10 | 10 | 2,500,000 | 2,600 ms | best | 3 |

Depth, node budget, time budget, and tactical extension are monotonic. Search returns the best move from the last fully completed iteration. Level tests assert the exact table and deterministic tactical positions; they do not claim statistical calibration that has not been run.

## Interface

The visual SSOT is `designs/specs/doushouqi-ui.md`. Its paired menu and game previews live under `designs/previews/`, with prompt documents under `designs/images/`.

### Menu

- The left pane shows an empty 7×9 territory board with two mineral-blue rivers, traps, dens, and jump cues.
- The right rail shows the package Logo, `斗兽棋`, version, and the three standard menu actions.
- The first release uses package-owned `assets/icon.png`; the empty board and pieces are drawn by Compose.

### Game

- The board uses a bamboo-gold field, dark mineral-blue rivers, marked traps, and Vermilion/Pine Green dens.
- Pieces are slightly rounded rectangular tiles with large upright single-character animal labels.
- Selection uses a same-side inner border.
- Empty legal destinations use dark dots; capture destinations use ivory broken rings.
- The latest destination uses the shared translucent bright-blue four-corner marker with a clear center and visible piece clearance.
- The right rail shows mode, score, current turn, human side, robot level, result reason, and available actions.
- Robot thinking disables board input and displays `智能思考中` without a blocking full-screen overlay.

The wide layout uses the family baseline: `28dp` outer padding, `34dp` board/rail gap, a `300dp` game rail at `94%` content height, and a `320dp` menu rail at `88%` content height. Below `900dp` available width, the rail becomes a full-width band below the complete board.

Every actionable control is at least `48dp`, adjacent controls have at least `8dp` spacing, normal text contrast is at least `4.5:1`, and state is never communicated by color alone.

## Packaging And Versions

- Add `:games:doushouqi` to `settings.gradle.kts`.
- Register `packageDoushouqiGame` with the existing root package helper.
- Add `build:game:doushouqi`; include it in aggregate game build, package verification, and debug APK built-in assets.
- Increment the shell from `versionCode = 4`, `versionName = 0.0.4` to `versionCode = 5`, `versionName = 0.0.5` because the built-in catalog changes.
- Keep `DoushouqiPlugin.manifest` and `games/doushouqi/package/manifest.json` aligned at `1 / 0.0.1`.
- Provide a readable, circular-safe `1024×1024` PNG at `games/doushouqi/package/assets/icon.png`.
- Keep the shell package-agnostic: it reads the new display name and icon only from the manifest and does not add a `doushouqi` presentation branch.
- No `game-api` change or external Android dependency is required.

## Verification

### Rules Tests

- Exact 7×9 terrain, traps, dens, rivers, initial placement, side to move, and piece inventory.
- Orthogonal movement, own-den rejection, Rat river entry/exit, and non-Rat river rejection.
- Legal and blocked Lion/Tiger jumps in both axes, including either side's Rat as blocker.
- Rank captures, equal captures, Rat-versus-Elephant exception, Elephant-versus-Rat rejection, and trap rank zero.
- Rat capture behavior on land, in water, and across the land/river boundary.
- Den entry, final capture, no-legal-move win, terminal precedence, threefold repetition, and 100 quiet half-move draw.
- Defensive copying, source-state immutability, illegal move rejection, and legal-move determinism.

### Session Tests

- Menu/version contract, fixed first side, player-versus-robot and side-versus-side scoring.
- Player win side swap, player loss reset, draw preservation, and immediate robot opening as second side.
- Single-player paired undo, two-player one-move undo, initial-opening exclusion, score/result/repetition restoration, and history clearing on restart.
- Last-move replacement and restoration, current-position tap resolution, rotated coordinate mapping, upright labels, and terminal action visibility.
- Generation rejection after move, undo, restart, and menu exit.

### AI Tests

- Exact monotonic level table, legal fallback, deterministic output, deadline/node bounds, and completed-depth reporting.
- Immediate den win, immediate den defense, final capture, Rat-Elephant tactic, blocked jump, poisoned capture avoidance, and repetition-aware draw preference.
- Search cancellation and stale-result rejection.

### Assets And Integration

- Manifest alignment and `1024×1024` readable circular-safe icon.
- Shared latest-move marker constants and board coordinate mapping.
- `./gradlew :games:doushouqi:testDebugUnitTest`.
- `./gradlew packageDoushouqiGame`.
- `npm run verify`, covering all six validated game zips and their inclusion in the debug APK.
