# Android Games — Agent Instructions

For humans: start with [README.md](README.md), then use the README inside each `games/<gameId>/` module for game-specific rules, AI, assets, and commands. The approved product and architecture baseline is the [design spec](docs/superpowers/specs/2026-07-07-android-pad-game-center-design.md).

## Project

Offline Android Pad game center (Kotlin + Jetpack Compose). The `app` module is a stable loading shell; each game ships as a zip package with dex plugin code loaded via `DexClassLoader`. Built-in games: Gomoku, Othello, Xiangqi, International Chess, Junqi, and Dou Shou Qi.

Gradle multi-module layout: `app` (shell), `game-api` (shell↔game contract), `games/*` (per-game modules + package assets).

The approved International Chess design is `docs/superpowers/specs/2026-07-18-international-chess-game-design.md`. Its package uses complete standard legal moves, legal-only public state application with internal unchecked search transitions, and deterministic player-score-driven 1-10 iterative-deepening Negamax with alpha-beta pruning, a primitive incremental make/unmake position, allocation-free automatic draw checks, bounded transposition storage, mature move ordering, and draw-aware quiescence. It follows Xiangqi-family session behavior and has no external runtime dependency.

The approved implementation baseline for the two-player hidden-information Junqi package is `docs/superpowers/specs/2026-07-21-junqi-game-design.md`. Keep its 12x5 graph, deployment constraints, observer-limited information model, no-cheating AI boundary, pass-and-play privacy flow, and package-only ownership intact.

The approved and implemented Dou Shou Qi package design is `docs/superpowers/specs/2026-07-26-doushouqi-game-design.md`. Preserve the standard 7x9 terrain, complete animal/rank/river/jump/trap/den rules, deterministic score-driven 1-10 offline search, Xiangqi-family session behavior, package-only ownership, and `designs/specs/doushouqi-ui.md` visual SSOT.

The Dou Shou Qi module owns its immutable 7x9 initial board, orthogonal movement, Rat-only river access, Rat-blocked Lion/Tiger jumps, rank captures, Rat/Elephant exception, terrain-bound Rat captures, trap weakening, immutable move application, and the exact den/final-capture/no-move/repetition/100-quiet terminal precedence. Keep these rules and their board/movement/capture/state/terminal tests together in `games/doushouqi/`.

Dou Shou Qi's score-driven AI owns an exact centralized 1-10 depth/node/deadline ladder, deterministic legal timeout/cancellation fallback, primitive array-backed make/unmake search transitions, incremental Zobrist placement keys, principal-variation/killer/history ordering, bounded tactical extension, bounded transposition storage, and deterministic weakening only at levels 1-5. Keep cancellation checks before node expansion, repetition counts and the quiet half-move counter mixed into transposition keys, keep primitive/public move generation equivalent across continuations, and preserve deterministic tactical coverage for den entry, final capture, Rat-versus-Elephant, and immediate den-threat defense.

Dou Shou Qi's session keeps single-player score by player/robot identity across side swaps and two-player score by Pine Green/Vermilion. Player wins swap side next round, player losses restore Pine Green, draws preserve side and score, paired single-player undo snapshots precede the human move, and an automatic robot opening is not undoable. Keep robot requests generation- and source-position-bound, rotate both coordinate directions only for a Vermilion single-player human, and regenerate tap candidates from the current position.

Dou Shou Qi version `0.0.7` owns a 1400x1400 transparent-corner bamboo board at `assets/board/doushouqi-board.png`, sixteen 512x512 transparent embossed animal pieces under `assets/pieces/`, and the user-selected 1024x1024 RGBA elephant-and-rat S-river porcelain Logo at `assets/icon.png`. The board and pieces remain reproducible through `games/doushouqi/tools/generate_doushouqi_assets.py`; do not regenerate or reinterpret the approved Logo. Its six traps use identical double-line octagonal hunting-net emblems with eight radial strands, four inward hooks, a center knot, and a subtle ochre wash; asset tests must protect all six fixed coordinates and reject a regression to ambiguous crosses. Its package-owned Compose UI keeps the 7x9 semantic interaction layer over the near-square texture, uses 86% short-cell piece canvases, the approved 28dp/34dp outer geometry and 300dp/320dp family rails, switches below 900dp to a board-over-rail layout, and disables board input while its dedicated `doushouqi-ai` executor is thinking. The menu rail groups Logo/title/version above three actions; the active rail uses score, turn/result, and action zones separated by two dividers. Single-player rail faction and result copy uses only `绿方` / `红方` and renders no player-side ownership line. Single-player capture summaries publish the complete player-plus-robot round as at most `绿方吃：<兽名>` and `红方吃：<兽名>`, or `无吃子`. Two-player summaries instead replace immediately after every accepted move with that move's one capture or `无吃子`; they never wait for or merge the opponent response. Undo restores the snapshot summary, restart clears it, illegal moves preserve it, and stale robot responses cannot mutate it. Keep exact PNG bounds/type/alpha checks before full decode, transparent Logo corners, per-texture Compose fallbacks, selected/legal/capture states, package-version-keyed loading, and shared marker constants covered by tests.

Current game behavior:

- The packaged launcher label is `游戏中心`, sourced from `@string/app_name`; keep the APK label and the visible home title aligned.
- The game-center shell currently uses `versionCode = 6` and `versionName = 0.0.6`; the home top bar displays `BuildConfig.VERSION_NAME`.
- Zip installation resolves canonical path segments to reject entries outside the temporary extraction tree and deletes that tree after every install attempt.
- `DexGamePluginLoader` caches one loader per game only while the manifest entry class and `plugin.apk` SHA-256 remain unchanged; changed package bytes use a fingerprint-specific dex optimization directory.
- Home game names and logos come only from each installed package's `displayName` and `icon` manifest fields. The shell supports bounded package-local PNG, WebP, JPEG, and compact text icon files and must not branch on known game IDs for presentation.
- All six built-in packages provide a `1024 x 1024` circular-safe PNG at `assets/icon.png`; Gomoku, Othello, Xiangqi, and International Chess follow `designs/specs/android-game-package-logos.md`, Junqi follows `docs/superpowers/specs/2026-07-21-junqi-game-design.md`, and Dou Shou Qi follows `designs/specs/doushouqi-ui.md`. Package verification requires that entry.
- The home game order is the descending persisted count of successful plugin loads. Equal counts use package display name and then game ID for deterministic, package-agnostic ordering.
- The landscape home game selector uses a fixed four-column grid. Incomplete final rows occupy the leading columns and retain empty trailing slots instead of centering their buttons; compact and portrait layouts remain a single column.
- All game menus use `单人模式`, `双人对战`, and `退出游戏`.
- Every game owns an independent version starting at `0.0.1`, and its main menu displays `GameManifest.versionName`.
- All games own their rules, robot, UI, score state, and restart flow inside their game module.
- All six games use Xiangqi's wide landscape geometry as the layout reference: 28dp outer padding, 34dp board/rail gap, a 300dp game rail at 94% content height, and a 320dp menu rail at 88% content height. The board stays centered in the remaining pane and preserves each game's native aspect ratio; below 900dp available width, the rail becomes a full-width band below the board. Game-specific board rendering and controls remain package-owned.
- All game boards mark the latest placed or moved-to cell with the same enlarged, translucent bright-blue four-corner highlight. It stays inside one cell, leaves a visible gap around the piece, and keeps the center clear. In single-player mode the robot response replaces the player's marker; a second-player round marks the robot opening. Undo restores the previous marker, while a fresh first-player round has none.
- All game side rails expose `悔棋` while no winner exists, and hide it after either side wins; an Othello draw keeps the undo action. Single-player undo restores the snapshot before the player's last move and the following robot response; two-player undo restores one move. Undo also restores score and winner state, while restart clears the history.
- In single-player mode, a player win swaps sides for the next round, a player loss restores the player to the first-moving side, and the robot opens immediately when the player becomes second. A draw changes neither score, current player side, nor score-derived intelligence level; two-player restart behavior stays fixed-first.
- Gomoku version `0.0.6` uses a 15×15 intersection board. Its robot priority is: win immediately, block an immediate five (including closed four), block moves that create at least two immediate winning points (continuous or broken open three), then use the positional fallback.
- Othello version `0.0.6` hides robot hint points in two-player mode.
- Xiangqi version `0.0.13` uses intersection placement, filters moves that expose the moving side's general, recognizes capture and checkmate wins, colors the active side in the turn display, and shows `将军` in the side panel when applicable. Its package-local session result distinguishes red win, black win, and draw; a draw is terminal but leaves score, current player side, and intelligence level unchanged. Its bright porcelain-and-celadon UI follows `designs/specs/xiangqi-ui.md`: the package owns one complete 1600x1500 RGBA board at `assets/board/xiangqi-board.png` plus fourteen independent 1024x1024 transparent piece PNGs under `assets/pieces/`. All pieces derive from `games/xiangqi/tools/source/ceramic-piece-master.png` and must preserve its double gold rim, glaze highlight, bevel shade, and soft lower-right shadow after runtime scaling. The runtime piece diameter is 80% of one grid step; the grid is registered at 128/110/1472/1360, leaving at least 80 source pixels of visible clearance below bottom-row pieces. Keep these bounds, the exact traditional glyph mapping, matched piece geometry, fallback rendering, package-local loading, and shared latest-move marker covered by `XiangqiPieceAssetsTest`. Its intelligence gradient is defined in `docs/superpowers/specs/2026-07-18-xiangqi-intelligence-gradient-design.md`: single-player AI level is the human player's accumulated win score plus one, capped at level 10. A pure-Kotlin iterative-deepening Negamax search uses a primitive make/unmake position, cached move ordering, effective-depth statistics, a bounded transposition table, per-level node/depth/deadline budgets, deterministic weakening for levels 1-5, and bounded quiescence for levels 8-10; levels 4 and 6 are the first four-ply and five-ply transition tiers, and search runs away from the Compose UI thread. Single-player scoring follows player-versus-robot identities across side swaps, while two-player scoring stays red-versus-black. In single-player mode, a black-side player sees a 180-degree coordinate-mapped board with black at the bottom while piece text stays upright.
- International Chess version `0.0.10` uses standard square placement, complete special moves and draw rules, legal-move-equivalent threefold-repetition keys, player-score-driven 1-10 offline search, explicit draw session state, Xiangqi-family score/undo/restart behavior, and a 180-degree Black-player view with upright pieces. Every draw type leaves score, current player side, and intelligence level unchanged. Player tap resolution always regenerates candidates from the current position after robot replies so subsequent moves and captures cannot use a stale previous-turn move list. Its package owns 12 transparent 1024×1024 Staunton piece PNGs under `assets/pieces/`; keep their names, dimensions, alpha corners, package ownership, exact piece mapping, transparent-margin trim, and 82% board-square render scale covered by `ChessPieceAssetsTest`. The menu board, active board, and promotion picker load and cache these package-owned textures once per installed package version, while any missing or invalid texture falls back to the corresponding Unicode glyph. Runtime decoding trims only fully transparent outer pixels in memory so the physical pieces match the approved scale without modifying package assets. Its cool mineral rim, tournament-green/ivory squares, rim coordinates, flat score-sheet rail, and state overlays follow `designs/specs/international-chess-ui.md`; the square board preserves its own coordinates and promotion flow inside the shared Xiangqi-reference outer geometry.
- International Chess promotion must expose queen, rook, bishop, and knight choices to human players; its search must preserve checkmate precedence over automatic draws and include session repetition history.
- Junqi Task 3 owns deterministic legal default/random/swap deployment, the complete rank/bomb/mine/flag battle matrix, permanent commander-death flag reveal, immutable move adjudication, flag-capture wins, no-move loss, both-immobile draw, and the completing-mover loss on quiet half-move 31. Tests must keep same-seed deployment repeatability plus multi-seed legality without requiring distinct layouts, reject mine/flag move attempts through `applyMove`, prove `SL[31]` still defeats a simultaneous both-immobile draw, and protect `JunqiState` defensive-copy/unmodifiable/non-mutating move behavior. Keep these rules together with Task 2's immutable 12x5 movement foundation inside `games/junqi/`.
- Junqi version `0.0.13` keeps Task 4's rank-free opponent observations, opaque position-stable deployment IDs, immutable inventory-consistent knowledge, all-retained-identity determinizations filtered to active pieces, exactly one live flag in nonterminal samples, commander-death survivor exclusion, sampled-safe flag defense, globally capacity-consistent bomb-exchange estimates, deterministic legal fallback before timed sampling, one shared per-request determinization pool reused by fallback, tactics, estimates, and search, end-to-end bounded sampled alpha-beta, the exact monotonic 1-10 sample/depth/node/time table, and deterministic weakening only at levels 1-5. Task 5 uses the `DEPLOYMENT`/`HANDOFF`/`PLAYING`/`FINISHED` session state machine with legal swap/random/reset/ready setup, opaque handoffs, non-blocking observer-safe battle summaries, identity-based score/restart policy, snapshot undo, last move, and generation-bound robot requests. Tests must reject stale robot responses after restart, handoff acceptance, and automatic battle transitions, plus invalid-phase transitions and human moves on robot turns. `HANDOFF` remains fully opaque. A collision immediately continues to the robot turn or next-player handoff; the observer-safe summary contains only the winning side, its `进攻`/`防守` role, and the current observer's formatted own-piece label. Single-player state independently retains the latest player-initiated and robot-initiated collisions; two-player state exposes only the latest collision after handoff. The rail uses orange/green winner swatches and labels such as `橙方胜 - 防守`, or `同归于尽`, followed by enlarged `我方棋子：<军衔>` text; it has no confirmation action and no opponent rank. Its package-owned board is the implemented traditional straw-gold design with labeled stations, oval camps, red headquarters, dark/ivory sleeper railways, and three center bridge routes. Compose renders internal `RED` as orange `#C65012` and internal `BLUE` as green `#23704B`, with white Black-weight rank labels dynamically sized to 64% of piece height. Keep the literal `JunqiVisuals` texture paths, all 60 unique in-bounds bitmap centers, every undirected road and rail edge, node labels and styles, transparent corners, shared last-move constants, and generator/tests aligned. The package retains cached package-local texture loading with Compose fallbacks, complete deployment/play/result controls, observer-only upright piece rendering with bottom-side 180-degree coordinate mapping, and a dedicated disposable background executor that applies only matching generation-bound AI requests. `packageJunqiGame` remains part of package and Debug APK verification. `JunqiAi.chooseMove` accepts only `JunqiObservation`, `JunqiKnowledge`, and `JunqiAiLevel`; default, random, and legal-swap deployments with identical public observations must remain independent of hidden enemy truth. `JunqiRules.battleOutcome` rejects immobile mine and flag attackers before bomb or other special handling.
- International Chess search and session repetition keys must remain identical, including normalized move-counter hash components and both usable and unusable en-passant rights.
- Mix the full International Chess repetition-count context into transposition-table keys; repetition scores are path-dependent.
- Keep International Chess piece textures inside the game package at `games/chess/package/assets/pieces/`; the shell must not own, name, or render them.
- Keep International Chess perft fixtures aligned with the published Position 2, Position 3, and Position 5 FEN strings and pair level-budget assertions with deterministic tactical depth checks.
- Human-facing documentation must list all six built-in packages and keep the independent shell/game versions aligned with their Gradle and manifest sources.

Current design direction:

- `designs/specs/junqi-ui.md` is the approved and implemented Junqi visual SSOT. It uses `designs/references/junqi-classic-board-reference.jpg` for the traditional low-saturation straw-gold board form: labeled rectangular stations, oval camps, red headquarters, dark/ivory sleeper railways, three separate center bridges, solid green/orange square military pieces, maximized white Black-weight rank text, and a flat modern order-ledger rail. Preserve all existing rules, privacy projections, exact 12x5 graph, Xiangqi-reference outer geometry, package ownership, fallback behavior, and shared last-move marker.
- `designs/specs/android-games-home.md` defines the light mineral-grey, matte-porcelain home screen with equal-size package-driven game buttons and no game-specific shell styling; landscape layouts use four stable columns with leading-aligned incomplete rows, and the wide style starts only when four 240dp buttons fit without a breakpoint shrink.
- `designs/specs/android-game-package-logos.md` defines the four package-owned circular PNG logos and their shared cool-porcelain medallion style.
- `designs/specs/android-games-family-versus-logo.md` records the approved game-center brand Logo: two face-to-face players around a shared game table. Root `logo.svg` and all launcher resources must preserve the user-selected 1254×1254 artwork without cropping or reinterpretation.
- The approved app-icon artwork is a 1254×1254 source embedded byte-for-byte in root `logo.svg`; `AppIconResourcesTest` guards its SHA-256 plus legacy/adaptive launcher resource wiring.
- `designs/specs/xiangqi-ui.md` defines the approved and implemented bright porcelain-and-celadon Xiangqi interface, complete-board PNG geometry, and 14-piece transparent PNG family.
- `designs/specs/doushouqi-ui.md` defines the approved Dou Shou Qi reference-restoration direction. `designs/references/doushouqi-board-reference.png` remains the source for the near-square bamboo board, deep-blue rivers, embossed Pine Green/Vermilion square animal pieces, and brush-style cream glyphs. The user-approved trap revision replaces ambiguous crosses with six identical double-line octagonal hunting-net emblems containing eight radial strands, four inward hooks, a center knot, and a subtle ochre wash. Preserve the shared family rail rather than copying the reference-specific sidebar.

## Environment

- **JDK** — required by Android Gradle Plugin 9.2.1
- **Android SDK** — API 36; build-tools **36.0.0** (used by root `d8` dex step)
- **SDK path** — `ANDROID_HOME`, or `local.properties` with `sdk.dir=...` (gitignored; create locally)
- **Emulator** — `scripts/start-android-debug.sh` defaults to AVD `android_games_mvp_pad` (override: `ANDROID_GAMES_AVD`)
- **Node** — optional; `package.json` wraps Gradle for convenience (no npm dependencies)

## Commands

Run from repository root:

- `npm run test` — all unit tests (`./gradlew test`)
- `npm run verify` — full MVP gate: tests + six validated game packages + debug APK asset validation
- `npm run build` — build debug APK and all game package zips
- `npm run build:apk` — `./gradlew :app:assembleDebug` (also copies built-in game zips into assets)
- `npm run build:game` — build all six game package zips
- `npm run build:game:gomoku` — `./gradlew packageGomokuGame`
- `npm run build:game:othello` — `./gradlew packageOthelloGame`
- `npm run build:game:xiangqi` — `./gradlew packageXiangqiGame`
- `npm run build:game:chess` — `./gradlew packageChessGame`
- `npm run build:game:junqi` — `./gradlew packageJunqiGame`
- `npm run build:game:doushouqi` — `./gradlew packageDoushouqiGame`
- `pnpm connect list` — list every USB-connected ADB device and its current state
- `pnpm connect <serial-id>` — select and verify one USB-connected device by exact ADB serial
- `bash scripts/test-connect-android-device.sh` — run deterministic host-side connect tests with a fake ADB executable
- `npm start` — boot emulator (if needed), build APK, install, launch `com.buddygames.center/.MainActivity`
- `./gradlew :game-api:testDebugUnitTest` — game-api manifest/contract tests only
- `./gradlew :app:testDebugUnitTest` — shell runtime tests only
- `./gradlew :games:gomoku:testDebugUnitTest` — single game rules tests (swap module name as needed)
- `./gradlew :games:chess:testDebugUnitTest` — International Chess rules, session, and AI tests
- `./gradlew :games:junqi:testDebugUnitTest` — Junqi rules, hidden-information AI, session, UI, assets, and manifest-contract tests
- `./gradlew :games:doushouqi:testDebugUnitTest` — Dou Shou Qi manifest, rules, AI, session, UI, and assets tests
- `./gradlew :games:xiangqi:testDebugUnitTest --tests com.buddygames.xiangqi.XiangqiAiCalibrationTest -PxiangqiCalibration=true -PxiangqiCalibrationPair=1` — opt-in long Xiangqi color-swapped calibration for levels 1 vs 2; use pair values 1-9

`pnpm run <script>` works the same; lockfile has no runtime deps.

## Structure

| Path | Role |
| ---- | ---- |
| `app/` | Game center shell UI, package install/discovery, dex plugin loader |
| `game-api/` | `GamePlugin`, `GameContext`, `GameManifest`, `CURRENT_SHELL_API` |
| `games/gomoku/` | Gomoku plugin module + package layout + game README |
| `games/othello/` | Othello plugin module + package layout + game README |
| `games/xiangqi/` | Xiangqi plugin module + package layout + game README |
| `games/chess/` | International Chess plugin module + package layout + game README |
| `games/junqi/` | Built-in Junqi package: deterministic deployment, immutable rules, hidden-information observations, knowledge, fair offline AI, and package-owned UI/assets |
| `games/doushouqi/` | Sixth built-in Dou Shou Qi package: standard 7x9 rules, deterministic 1-10 AI, session, and package-owned UI/assets |
| `build.gradle.kts` | Registers `package*Game` zip tasks (jar → d8 → plugin.apk → zip) |
| `scripts/connect-android-device.sh` | Lists USB ADB transports and verifies one exact device serial |
| `scripts/test-connect-android-device.sh` | Fake-ADB regression tests for host-side device connection states |
| `scripts/start-android-debug.sh` | Local emulator + install + launch |
| `docs/superpowers/specs/` | Approved product/architecture spec (SSOT) |
| `docs/superpowers/plans/` | MVP implementation plan |
| `docs/agents/game-plugins.md` | Plugin contract, packaging, adding games |

## Boundaries

### Always do

- Run `npm run verify` before claiming work complete (unless change is docs-only).
- Keep `verifyGamePackages` checking every built-in zip's required entries and its inclusion in the debug APK.
- Increment `app/build.gradle.kts` `versionCode` and semantic `versionName` for every game-center shell feature, UI, resource, package-management, or loader update. Game-only changes do not increment the shell version.
- Scope game logic/UI to the relevant `games/<name>/` module.
- Increment only the touched game's `versionCode` and semantic `versionName` for every rules, robot, UI, or package-asset update. Keep the plugin manifest and `games/<name>/package/manifest.json` exactly aligned.
- Keep each built-in package's `assets/icon.png` readable, square, `1024 x 1024`, circular-safe, and aligned with the manifest `icon` path.
- Keep robot strategy and its regression tests in the same game module; threat-priority changes must include deterministic board-state tests.
- Keep each `games/<gameId>/README.md` aligned with that game's manifest version, implemented rules, robot behavior, session flow, package assets, and supported commands. Documentation-only changes do not increment game versions.
- Xiangqi AI changes must preserve safe-move filtering and cover immediate general capture, checkmate preference, and poisoned-capture avoidance.
- Keep Xiangqi intelligence levels centralized in immutable configuration, monotonic in depth and node budget, deterministic for a given position and level, and derived from the human player's accumulated single-player win score rather than a fixed board side.
- Xiangqi black-side perspective changes must map model/display coordinates in both directions and keep two-player plus red-side layouts unchanged.
- Keep Junqi deployment at exactly 25 unique pieces on the side's non-camp stations, with the flag in own headquarters, mines in the back two rows, and bombs off the first line; seeded random layouts and legal swaps must remain deterministic, public stable IDs must not encode rank, and legal swaps must keep each public ID fixed to its deployment position while exchanging only private type state.
- Keep Junqi terminal precedence as flag capture, then `SL[31]`, then next-side mobility; every collision resets the quiet half-move count and commander removal permanently reveals that commander's flag.
- Keep Junqi AI public APIs structurally unable to accept `JunqiState` or raw enemy pieces. Opponent observations may expose only opaque stable ID, position, moved state, and public constraints; candidate sampling must assign across every retained active and eliminated identity under exact initial inventory capacities and current public facts before filtering to active identities, preserve exactly one live flag in every nonterminal sample, exclude commanders from survivors after the opponent flag is revealed, and never read hidden ranks.
- Keep Junqi AI levels centralized at the approved exact 1-10 sample/depth/node/time budgets, start the wall-clock deadline before preprocessing, compute a deterministic legal fallback from observation-visible own types and public occupancy before timed sampling, and never return `null` when a legal move exists. Each request may invoke the sampler at most `JunqiAiLevel.sampleCount` times and, when the deadline permits, exactly that many times; cache those complete assignments once and reuse them for root construction, tactical safety, bomb estimates, and search. Keep `samplesCompleted` equal to the completed shared samples, guard expensive sampling and move application on both sides under the same end-to-end deadline/node budgets, use only observation-derived deterministic seeds, preserve only sampled-safe tactical priorities, and apply deterministic candidate weakening only at levels 1-5.
- Keep Junqi's authoritative hidden `JunqiState` private to `JunqiSession`. Public session projections may expose only the active observer's `JunqiObservation`. `HANDOFF` must expose no board, deployment, rank, battle summary, or last-move coordinates. A normal `PLAYING` or `FINISHED` projection may additionally expose the observer-known last move and observer-safe `JunqiBattleSummary` values containing only `winnerSide`, formatted `winnerRoleLabel`, and the current observer's formatted `ownPieceLabel`. Single-player projections may retain one summary per attacker identity; never expose an opponent's true rank or raw `JunqiPieceType`.
- Keep Junqi single-player snapshots immediately before the human move so undo also removes the robot reply and restores both knowledge states, score, quiet counter, result, and last move. Two-player snapshots contain one move and undo returns to an opaque handoff for the restored mover. Winner states hide undo, draws retain it, and restart clears history.
- Keep Junqi AI search outside the session and Compose UI thread: `JunqiSession` produces observation-only generation-bound robot requests, `JunqiPlugin` owns and disposes a dedicated background executor, and `applyRobotMove` must reject stale requests after any move, undo, restart, handoff, or automatic battle transition changes the session generation.
- Keep Junqi UI projections observation-only: green-side (internal `BLUE`) bottom display mapping rotates coordinates 180 degrees without rotating board or piece labels, opponents remain backs except a publicly revealed flag, and `HANDOFF` mounts no board or piece semantics. Battle summaries stay in the normal side rail, never require confirmation, color the winning faction, always name the current observer's own battle piece, and expose no opponent rank.
- Keep Junqi package texture loading version-keyed by package root and manifest version. Before full decode, `BitmapFactory` bounds must report `image/png` and the asset-specific exact dimensions: icon `1024x1024`, board `1400x1680`, shelf `1400x360`; after full decode, dimensions must match again, otherwise use the existing Compose fallback.
- Keep Xiangqi board and piece textures reproducible through `games/xiangqi/tools/generate_xiangqi_assets.py`; font selection must cover every required traditional glyph before writing assets, and the generated pieces must use `games/xiangqi/tools/source/ceramic-piece-master.png` as their only material base.
- Keep single-player side-selection and opening-turn rules in each game's session model; restart behavior changes must cover player win, player loss, and robot opening as second-player tests.
- Record undo snapshots immediately before legal player actions, include score and terminal state in each snapshot, and keep the initial robot opening outside undo history.
- Keep the last-move marker in each game's session and undo snapshot. Othello marks only the newly placed disc, Xiangqi marks the destination coordinate after perspective mapping, and robot moves replace the preceding player marker.
- Keep last-move marker geometry and color constants aligned across all six game packages; marker scale must remain below one cell so adjacent pieces are unaffected.
- Keep undo-button visibility separate from undo availability: hide it only when a winner exists, and keep Othello draws undoable.
- Keep `game-api` backward-compatible or update every `games/*` plugin in the same change.
- Keep home presentation package-agnostic: read names and icons from the manifest and rank by successful-launch count without adding game ID branches.
- Run targeted unit tests for touched modules (see Commands).
- Match existing Kotlin + Compose style in neighboring files.
- After completing and verifying any repository change, automatically create a scoped local commit unless the user explicitly asks not to. Never push unless the user asks in the same turn.

### Ask first

- Change `game-api` public types or bump `CURRENT_SHELL_API`.
- Modify `DexGamePluginLoader`, `GamePackageRepository`, or root packaging tasks in `build.gradle.kts`.
- Add/remove Gradle modules or Android dependencies.
- Delete files, add CI, or overwrite this file.

### Never do

- Commit `local.properties`, `build/`, `.gradle/`, `*.apk`, `.idea/`, or credentials.
- Run `git config` changes or `git push --force` to main/master.
- Introduce Play Dynamic Delivery, online update repos, or package signing (MVP non-goals).
- Invent scripts, env vars, or Gradle tasks not present in the repo.

## Verification

After code changes:

1. `bash scripts/test-connect-android-device.sh` for host connect-script changes
2. `./gradlew :<module>:testDebugUnitTest` for each touched Android module
3. `npm run verify` for integration-level confidence
4. Optional: `npm start` for on-device/emulator smoke test

Emulator logs: `build/logs/emulator-<AVD_NAME>.log`

## Known fixes

| Symptom | Fix |
| ------- | --- |
| SDK location not found | Create `local.properties`: `sdk.dir=/path/to/Android/sdk` or export `ANDROID_HOME` |
| `d8` / build-tools missing | Install SDK build-tools 36.0.0 via `sdkmanager "build-tools;36.0.0"` |
| `:app:assembleDebug` missing built-in games | Run `npm run build:game` first, or use `npm run build:apk` (depends on package tasks) |
| Emulator AVD missing | Run `npm start` (auto-creates if system image installed) or `sdkmanager "system-images;android-36;google_apis;x86_64"` |
| Plugin load error at runtime | Confirm `manifest.json` `entryClass` matches `*Plugin` class implementing `GamePlugin` |

## Document map

| Doc | Purpose |
| --- | ------- |
| [README.md](README.md) | Human setup, build, runtime, package format, and current game capabilities |
| [games/gomoku/README.md](games/gomoku/README.md) | Gomoku rules, robot priority, session behavior, assets, and commands |
| [games/othello/README.md](games/othello/README.md) | Othello rules, robot priority, pass flow, assets, and commands |
| [games/xiangqi/README.md](games/xiangqi/README.md) | Xiangqi rules, ten-level AI, ceramic assets, calibration, and commands |
| [games/chess/README.md](games/chess/README.md) | International Chess rules, ten-level AI, draw handling, textures, and commands |
| [games/doushouqi/README.md](games/doushouqi/README.md) | Dou Shou Qi rules, AI, session, reference-restored UI, assets, and commands |
| [docs/superpowers/specs/2026-07-07-android-pad-game-center-design.md](docs/superpowers/specs/2026-07-07-android-pad-game-center-design.md) | Product scope, architecture, non-goals |
| [docs/superpowers/plans/2026-07-08-android-pad-game-center-mvp.md](docs/superpowers/plans/2026-07-08-android-pad-game-center-mvp.md) | MVP task breakdown and file map |
| [docs/superpowers/plans/2026-07-18-xiangqi-intelligence-gradient.md](docs/superpowers/plans/2026-07-18-xiangqi-intelligence-gradient.md) | TDD implementation steps for the Xiangqi ten-level search engine and score-driven single-player flow |
| [docs/superpowers/plans/2026-07-18-xiangqi-ai-search-calibration.md](docs/superpowers/plans/2026-07-18-xiangqi-ai-search-calibration.md) | TDD implementation plan for the optimized Xiangqi search position, observable effective depth, and calibrated level ladder |
| [docs/superpowers/plans/2026-07-18-international-chess-game.md](docs/superpowers/plans/2026-07-18-international-chess-game.md) | TDD implementation steps for International Chess rules, ten-level search, tablet UI, and package integration |
| [docs/superpowers/plans/2026-07-21-junqi-game.md](docs/superpowers/plans/2026-07-21-junqi-game.md) | TDD implementation steps for Junqi rules, hidden-information AI, pass-and-play UI, assets, and package integration |
| [docs/superpowers/reports/2026-07-22-junqi-runtime-acceptance.md](docs/superpowers/reports/2026-07-22-junqi-runtime-acceptance.md) | Emulator acceptance evidence for Junqi discovery, menu, single-player, pass-and-play privacy, 800x600dp layout, and texture fallback |
| [docs/superpowers/specs/2026-07-18-xiangqi-intelligence-gradient-design.md](docs/superpowers/specs/2026-07-18-xiangqi-intelligence-gradient-design.md) | Xiangqi ten-level offline intelligence gradient, score mapping, search boundary, and calibration |
| [docs/superpowers/specs/2026-07-18-xiangqi-ai-search-calibration-design.md](docs/superpowers/specs/2026-07-18-xiangqi-ai-search-calibration-design.md) | Xiangqi search-position optimization, effective-depth observability, and statistical level-calibration gates |
| [docs/superpowers/specs/2026-07-18-international-chess-game-design.md](docs/superpowers/specs/2026-07-18-international-chess-game-design.md) | International Chess rules, session behavior, UI, packaging, and offline 1-10 AI |
| [docs/superpowers/specs/2026-07-21-junqi-game-design.md](docs/superpowers/specs/2026-07-21-junqi-game-design.md) | Approved two-player hidden-information Junqi rules, AI boundary, UI, packaging, and tests |
| [docs/superpowers/specs/2026-07-26-doushouqi-game-design.md](docs/superpowers/specs/2026-07-26-doushouqi-game-design.md) | Approved Dou Shou Qi rules, session behavior, 1-10 offline AI, UI, packaging, and tests |
| [docs/superpowers/specs/2026-07-31-doushouqi-own-den-entry-design.md](docs/superpowers/specs/2026-07-31-doushouqi-own-den-entry-design.md) | Approved local rule variant allowing either side to enter, occupy, and leave its own den without winning |
| [docs/superpowers/plans/2026-07-31-doushouqi-own-den-entry.md](docs/superpowers/plans/2026-07-31-doushouqi-own-den-entry.md) | TDD plan for public/search own-den move parity, Doushouqi 0.0.8 release alignment, and verification |
| [docs/superpowers/plans/2026-07-26-doushouqi-game.md](docs/superpowers/plans/2026-07-26-doushouqi-game.md) | TDD implementation steps for Dou Shou Qi rules, search, session, UI, assets, and package integration |
| [docs/superpowers/plans/2026-07-27-doushouqi-trap-emblem.md](docs/superpowers/plans/2026-07-27-doushouqi-trap-emblem.md) | TDD implementation steps for the approved octagonal hunting-net trap emblems |
| [docs/superpowers/reports/2026-07-27-doushouqi-reference-runtime-acceptance.md](docs/superpowers/reports/2026-07-27-doushouqi-reference-runtime-acceptance.md) | Emulator evidence for the reference-restored Dou Shou Qi menu, board, pieces, family rail, AI round, and latest-move marker |
| [docs/superpowers/reports/2026-07-27-doushouqi-logo-sidebar-runtime-acceptance.md](docs/superpowers/reports/2026-07-27-doushouqi-logo-sidebar-runtime-acceptance.md) | Emulator evidence for the transparent Doushouqi Logo, family menu/game rails, and simplified red/green turn copy |
| [docs/superpowers/specs/2026-07-21-usb-adb-connect-script-design.md](docs/superpowers/specs/2026-07-21-usb-adb-connect-script-design.md) | USB ADB device listing and exact-serial connection command contract |
| [docs/superpowers/plans/2026-07-21-usb-adb-connect-script.md](docs/superpowers/plans/2026-07-21-usb-adb-connect-script.md) | TDD implementation steps for USB ADB device listing and exact-serial verification |
| [docs/agents/game-plugins.md](docs/agents/game-plugins.md) | GamePlugin contract, zip layout, adding a game |
| [designs/specs/android-games-home.md](designs/specs/android-games-home.md) | Current home-screen visual SSOT |
| [designs/specs/android-games-family-versus-logo.md](designs/specs/android-games-family-versus-logo.md) | Approved family-versus Logo and launcher-icon SSOT |
| [designs/specs/xiangqi-ui.md](designs/specs/xiangqi-ui.md) | Approved bright, simple Chinese Xiangqi UI and board/piece texture SSOT |
| [designs/specs/junqi-ui.md](designs/specs/junqi-ui.md) | Approved and implemented traditional railway Junqi UI SSOT and preview index |
| [docs/superpowers/specs/2026-07-27-doushouqi-logo-sidebar-design.md](docs/superpowers/specs/2026-07-27-doushouqi-logo-sidebar-design.md) | Approved transparent Doushouqi Logo and Xiangqi-family menu/game rail design |
| [docs/superpowers/plans/2026-07-27-doushouqi-logo-sidebar.md](docs/superpowers/plans/2026-07-27-doushouqi-logo-sidebar.md) | TDD plan for the transparent Doushouqi Logo, family rails, and simplified turn copy |
| [docs/superpowers/specs/2026-07-28-doushouqi-round-capture-summary-design.md](docs/superpowers/specs/2026-07-28-doushouqi-round-capture-summary-design.md) | Historical `0.0.6` completed-round capture behavior; its single-player aggregation remains current while its two-player merge policy is superseded |
| [docs/superpowers/plans/2026-07-28-doushouqi-round-capture-summary.md](docs/superpowers/plans/2026-07-28-doushouqi-round-capture-summary.md) | TDD plan for Doushouqi completed-round capture state, shared rail copy, release alignment, and runtime acceptance |
| [docs/superpowers/reports/2026-07-28-doushouqi-round-capture-summary-runtime-acceptance.md](docs/superpowers/reports/2026-07-28-doushouqi-round-capture-summary-runtime-acceptance.md) | Android 36 evidence for single-player one-capture and two-player two-capture completed-round rails |
| [docs/superpowers/specs/2026-07-28-doushouqi-two-player-immediate-capture-summary-design.md](docs/superpowers/specs/2026-07-28-doushouqi-two-player-immediate-capture-summary-design.md) | Approved and implemented two-player latest-move capture publishing while single-player keeps completed-round aggregation |
| [docs/superpowers/plans/2026-07-28-doushouqi-two-player-immediate-capture-summary.md](docs/superpowers/plans/2026-07-28-doushouqi-two-player-immediate-capture-summary.md) | TDD plan for immediate two-player capture state, 0.0.7 release alignment, and Android acceptance |
| [docs/superpowers/reports/2026-07-28-doushouqi-two-player-immediate-capture-summary-runtime-acceptance.md](docs/superpowers/reports/2026-07-28-doushouqi-two-player-immediate-capture-summary-runtime-acceptance.md) | Exact `android_games_mvp_pad` Android 36 evidence that a two-player capture publishes immediately and the following quiet move clears it |
| [docs/superpowers/specs/2026-07-28-doushouqi-single-player-capture-summary-design.md](docs/superpowers/specs/2026-07-28-doushouqi-single-player-capture-summary-design.md) | Approved single-player red/green-only copy and latest-move captured-piece summary |
| [docs/superpowers/plans/2026-07-28-doushouqi-single-player-capture-summary.md](docs/superpowers/plans/2026-07-28-doushouqi-single-player-capture-summary.md) | TDD plan for single-player red/green-only copy and latest-move capture state |
| [docs/superpowers/reports/2026-07-28-doushouqi-single-player-capture-summary-runtime-acceptance.md](docs/superpowers/reports/2026-07-28-doushouqi-single-player-capture-summary-runtime-acceptance.md) | Android 36 evidence for red/green-only single-player rail copy and latest-move capture replacement |
| [designs/specs/doushouqi-ui.md](designs/specs/doushouqi-ui.md) | Approved Dou Shou Qi double-river UI SSOT and preview index |

## Done checklist

- [ ] Targeted module unit tests pass
- [ ] `npm run verify` passes
- [ ] No secrets or build artifacts staged
- [ ] If `app/` changed, the game-center `versionCode` and `versionName` were incremented together
- [ ] Each touched game has matching, incremented versions in plugin code and `package/manifest.json`
- [ ] Each touched game's README still matches its implementation, manifest, assets, and commands
- [ ] If `game-api` changed, all six games still build and load
