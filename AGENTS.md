# Android Games — Agent Instructions

For humans: start with [README.md](README.md), then use the README inside each `games/<gameId>/` module for game-specific rules, AI, assets, and commands. The approved product and architecture baseline is the [design spec](docs/superpowers/specs/2026-07-07-android-pad-game-center-design.md).

## Project

Offline Android Pad game center (Kotlin + Jetpack Compose). The `app` module is a stable loading shell; each game ships as a zip package with dex plugin code loaded via `DexClassLoader`. Built-in games: Gomoku, Othello, Xiangqi, International Chess, and Junqi.

Gradle multi-module layout: `app` (shell), `game-api` (shell↔game contract), `games/*` (per-game modules + package assets).

The approved International Chess design is `docs/superpowers/specs/2026-07-18-international-chess-game-design.md`. Its package uses complete standard legal moves, legal-only public state application with internal unchecked search transitions, and deterministic player-score-driven 1-10 iterative-deepening Negamax with alpha-beta pruning, a primitive incremental make/unmake position, allocation-free automatic draw checks, bounded transposition storage, mature move ordering, and draw-aware quiescence. It follows Xiangqi-family session behavior and has no external runtime dependency.

The approved implementation baseline for the two-player hidden-information Junqi package is `docs/superpowers/specs/2026-07-21-junqi-game-design.md`. Keep its 12x5 graph, deployment constraints, observer-limited information model, no-cheating AI boundary, pass-and-play privacy flow, and package-only ownership intact.

Current game behavior:

- The packaged launcher label is `游戏中心`, sourced from `@string/app_name`; keep the APK label and the visible home title aligned.
- The game-center shell currently uses `versionCode = 3` and `versionName = 0.0.3`; the home top bar displays `BuildConfig.VERSION_NAME`.
- Home game names and logos come only from each installed package's `displayName` and `icon` manifest fields. The shell supports bounded package-local PNG, WebP, JPEG, and compact text icon files and must not branch on known game IDs for presentation.
- All five built-in packages provide a `1024 x 1024` circular-safe PNG at `assets/icon.png`; Gomoku, Othello, Xiangqi, and International Chess follow `designs/specs/android-game-package-logos.md`, while Junqi follows `docs/superpowers/specs/2026-07-21-junqi-game-design.md`. Package verification requires that entry.
- The home game order is the descending persisted count of successful plugin loads. Equal counts use package display name and then game ID for deterministic, package-agnostic ordering.
- All game menus use `单人模式`, `双人对战`, and `退出游戏`.
- Every game owns an independent version starting at `0.0.1`, and its main menu displays `GameManifest.versionName`.
- All games own their rules, robot, UI, score state, and restart flow inside their game module.
- All game boards mark the latest placed or moved-to cell with the same enlarged, translucent bright-blue four-corner highlight. It stays inside one cell, leaves a visible gap around the piece, and keeps the center clear. In single-player mode the robot response replaces the player's marker; a second-player round marks the robot opening. Undo restores the previous marker, while a fresh first-player round has none.
- All game side rails expose `悔棋` while no winner exists, and hide it after either side wins; an Othello draw keeps the undo action. Single-player undo restores the snapshot before the player's last move and the following robot response; two-player undo restores one move. Undo also restores score and winner state, while restart clears the history.
- In single-player mode, a player win swaps sides for the next round, a player loss restores the player to the first-moving side, and the robot opens immediately when the player becomes second. A draw changes neither score, current player side, nor score-derived intelligence level; two-player restart behavior stays fixed-first.
- Gomoku uses a 15×15 intersection board. Its robot priority is: win immediately, block an immediate five (including closed four), block moves that create at least two immediate winning points (continuous or broken open three), then use the positional fallback.
- Othello hides robot hint points in two-player mode.
- Xiangqi version `0.0.13` uses intersection placement, filters moves that expose the moving side's general, recognizes capture and checkmate wins, colors the active side in the turn display, and shows `将军` in the side panel when applicable. Its package-local session result distinguishes red win, black win, and draw; a draw is terminal but leaves score, current player side, and intelligence level unchanged. Its bright porcelain-and-celadon UI follows `designs/specs/xiangqi-ui.md`: the package owns one complete 1600x1500 RGBA board at `assets/board/xiangqi-board.png` plus fourteen independent 1024x1024 transparent piece PNGs under `assets/pieces/`. All pieces derive from `games/xiangqi/tools/source/ceramic-piece-master.png` and must preserve its double gold rim, glaze highlight, bevel shade, and soft lower-right shadow after runtime scaling. The runtime piece diameter is 80% of one grid step; the grid is registered at 128/110/1472/1360, leaving at least 80 source pixels of visible clearance below bottom-row pieces. Keep these bounds, the exact traditional glyph mapping, matched piece geometry, fallback rendering, package-local loading, and shared latest-move marker covered by `XiangqiPieceAssetsTest`. Its intelligence gradient is defined in `docs/superpowers/specs/2026-07-18-xiangqi-intelligence-gradient-design.md`: single-player AI level is the human player's accumulated win score plus one, capped at level 10. A pure-Kotlin iterative-deepening Negamax search uses a primitive make/unmake position, cached move ordering, effective-depth statistics, a bounded transposition table, per-level node/depth/deadline budgets, deterministic weakening for levels 1-5, and bounded quiescence for levels 8-10; levels 4 and 6 are the first four-ply and five-ply transition tiers, and search runs away from the Compose UI thread. Single-player scoring follows player-versus-robot identities across side swaps, while two-player scoring stays red-versus-black. In single-player mode, a black-side player sees a 180-degree coordinate-mapped board with black at the bottom while piece text stays upright.
- International Chess version `0.0.9` uses standard square placement, complete special moves and draw rules, legal-move-equivalent threefold-repetition keys, player-score-driven 1-10 offline search, explicit draw session state, Xiangqi-family score/undo/restart behavior, and a 180-degree Black-player view with upright pieces. Every draw type leaves score, current player side, and intelligence level unchanged. Player tap resolution always regenerates candidates from the current position after robot replies so subsequent moves and captures cannot use a stale previous-turn move list. Its package owns 12 transparent 1024×1024 Staunton piece PNGs under `assets/pieces/`; keep their names, dimensions, alpha corners, package ownership, exact piece mapping, transparent-margin trim, and 82% board-square render scale covered by `ChessPieceAssetsTest`. The menu board, active board, and promotion picker load and cache these package-owned textures once per installed package version, while any missing or invalid texture falls back to the corresponding Unicode glyph. Runtime decoding trims only fully transparent outer pixels in memory so the physical pieces match the approved scale without modifying package assets. Its cool mineral rim, tournament-green/ivory squares, rim coordinates, flat score-sheet rail, and state overlays follow `designs/specs/international-chess-ui.md`; the square board and right rail remain side by side at 800×600 landscape, and the rail distinguishes the active side, check, terminal result, AI level, undo, and restart.
- International Chess promotion must expose queen, rook, bishop, and knight choices to human players; its search must preserve checkmate precedence over automatic draws and include session repetition history.
- Junqi Task 3 owns deterministic legal default/random/swap deployment, the complete rank/bomb/mine/flag battle matrix, permanent commander-death flag reveal, immutable move adjudication, flag-capture wins, no-move loss, both-immobile draw, and the completing-mover loss on quiet half-move 31. Tests must keep same-seed deployment repeatability plus multi-seed legality without requiring distinct layouts, reject mine/flag move attempts through `applyMove`, prove `SL[31]` still defeats a simultaneous both-immobile draw, and protect `JunqiState` defensive-copy/unmodifiable/non-mutating move behavior. Keep these rules together with Task 2's immutable 12x5 movement foundation inside `games/junqi/`.
- Junqi version `0.0.8` keeps Task 4's rank-free opponent observations, opaque position-stable deployment IDs, immutable inventory-consistent knowledge, all-retained-identity determinizations filtered to active pieces, exactly one live flag in nonterminal samples, commander-death survivor exclusion, sampled-safe flag defense, globally capacity-consistent bomb-exchange estimates, deterministic legal fallback before timed sampling, one shared per-request determinization pool reused by fallback, tactics, estimates, and search, end-to-end bounded sampled alpha-beta, the exact monotonic 1-10 sample/depth/node/time table, and deterministic weakening only at levels 1-5. Task 5 adds the complete `DEPLOYMENT`/`HANDOFF`/`PLAYING`/`BATTLE_RESULT`/`FINISHED` session state machine, legal swap/random/reset/ready setup, opaque handoffs, generic battle acknowledgement, identity-based score/restart policy, snapshot undo, last move, and generation-bound robot requests. Tests must reject stale robot responses after restart, handoff acceptance, and battle acknowledgement; reject invalid-phase transitions and human moves on robot turns; recursively confirm `BATTLE_RESULT` has no board, rank, deployment contents, last-move leakage, or nested piece-rank enums; and whitelist only approved public projection enums. Task 6 adds deterministic package-owned `assets/icon.png`, `assets/board/junqi-board.png`, and `assets/textures/junqi-shelf.png`; keep their literal `JunqiVisuals` paths, all 60 unique in-bounds bitmap centers, every undirected road and rail edge (including the central bridge), complete camp/headquarters styling, transparent corners, shared last-move constants, and generator/tests aligned. Task 7 adds the package `JunqiPlugin`, cached package-local texture loading with Compose fallbacks, fixed board-and-rail landscape geometry, complete deployment/play/result controls, observer-only upright piece rendering with bottom-side 180-degree mapping, fully opaque handoff and generic battle pages with no board or rank semantics, and a dedicated disposable background executor that applies only matching generation-bound AI requests. Task 8 registers `packageJunqiGame`, verifies its package and Debug APK entry, and keeps the wildcard built-in copy intact; it adds a parsed static-manifest alignment test without changing the shell or Junqi versions. Road/rail overlaps use the generator's rail-over-road paint order in visual assertions. `JunqiAi.chooseMove` accepts only `JunqiObservation`, `JunqiKnowledge`, and `JunqiAiLevel`; default, random, and legal-swap deployments with identical public observations must remain independent of hidden enemy truth. `JunqiRules.battleOutcome` rejects immobile mine and flag attackers before bomb or other special handling.
- International Chess search and session repetition keys must remain identical, including normalized move-counter hash components and both usable and unusable en-passant rights.
- Mix the full International Chess repetition-count context into transposition-table keys; repetition scores are path-dependent.
- Keep International Chess piece textures inside the game package at `games/chess/package/assets/pieces/`; the shell must not own, name, or render them.
- Keep International Chess perft fixtures aligned with the published Position 2, Position 3, and Position 5 FEN strings and pair level-budget assertions with deterministic tactical depth checks.
- Human-facing documentation must list all five built-in packages and keep the independent shell/game versions aligned with their Gradle and manifest sources.

Current design direction:

- `designs/specs/junqi-ui.md` is the current proposed Junqi visual SSOT pending the user direction gate. It uses `designs/references/junqi-classic-board-reference.jpg` for the traditional low-saturation straw-gold board form: labeled rectangular stations, oval camps, red headquarters, dark/ivory sleeper railways, three separate center bridges, square red/blue military pieces, and a flat modern order-ledger rail. Do not implement or replace the approved runtime visuals until the user selects Option 1; if selected, preserve all existing rules, privacy projections, exact 12x5 graph, 800x600dp geometry, package ownership, fallback behavior, and shared last-move marker.
- `designs/specs/android-games-home.md` defines the light mineral-grey, matte-porcelain home screen with equal-size package-driven game buttons and no game-specific shell styling; its wide style starts only when four 240dp buttons fit without a breakpoint shrink.
- `designs/specs/android-game-package-logos.md` defines the four package-owned circular PNG logos and their shared cool-porcelain medallion style.
- `designs/specs/android-games-family-versus-logo.md` records the approved game-center brand Logo: two face-to-face players around a shared game table. Root `logo.svg` and all launcher resources must preserve the user-selected 1254×1254 artwork without cropping or reinterpretation.
- The approved app-icon artwork is a 1254×1254 source embedded byte-for-byte in root `logo.svg`; `AppIconResourcesTest` guards its SHA-256 plus legacy/adaptive launcher resource wiring.
- `designs/specs/xiangqi-ui.md` defines the approved and implemented bright porcelain-and-celadon Xiangqi interface, complete-board PNG geometry, and 14-piece transparent PNG family.

## Environment

- **JDK** — required by Android Gradle Plugin 9.2.1
- **Android SDK** — API 36; build-tools **36.0.0** (used by root `d8` dex step)
- **SDK path** — `ANDROID_HOME`, or `local.properties` with `sdk.dir=...` (gitignored; create locally)
- **Emulator** — `scripts/start-android-debug.sh` defaults to AVD `android_games_mvp_pad` (override: `ANDROID_GAMES_AVD`)
- **Node** — optional; `package.json` wraps Gradle for convenience (no npm dependencies)

## Commands

Run from repository root:

- `npm run test` — all unit tests (`./gradlew test`)
- `npm run verify` — full MVP gate: tests + five validated game packages + debug APK asset validation
- `npm run build` — build debug APK and all game package zips
- `npm run build:apk` — `./gradlew :app:assembleDebug` (also copies built-in game zips into assets)
- `npm run build:game` — build all five game package zips
- `npm run build:game:gomoku` — `./gradlew packageGomokuGame`
- `npm run build:game:othello` — `./gradlew packageOthelloGame`
- `npm run build:game:xiangqi` — `./gradlew packageXiangqiGame`
- `npm run build:game:chess` — `./gradlew packageChessGame`
- `npm run build:game:junqi` — `./gradlew packageJunqiGame`
- `pnpm connect list` — list every USB-connected ADB device and its current state
- `pnpm connect <serial-id>` — select and verify one USB-connected device by exact ADB serial
- `bash scripts/test-connect-android-device.sh` — run deterministic host-side connect tests with a fake ADB executable
- `npm start` — boot emulator (if needed), build APK, install, launch `com.buddygames.center/.MainActivity`
- `./gradlew :game-api:testDebugUnitTest` — game-api manifest/contract tests only
- `./gradlew :app:testDebugUnitTest` — shell runtime tests only
- `./gradlew :games:gomoku:testDebugUnitTest` — single game rules tests (swap module name as needed)
- `./gradlew :games:chess:testDebugUnitTest` — International Chess rules, session, and AI tests
- `./gradlew :games:junqi:testDebugUnitTest` — Junqi rules, hidden-information AI, session, UI, assets, and manifest-contract tests
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
| `games/junqi/` | Fifth built-in Junqi package: deterministic deployment, immutable rules, hidden-information observations, knowledge, fair offline AI, and package-owned UI/assets |
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
- Keep Junqi's authoritative hidden `JunqiState` private to `JunqiSession`. Public session projections may expose only the active observer's `JunqiObservation`; `HANDOFF` and `BATTLE_RESULT` must expose no board, deployment, rank, or last-move coordinates, and battle acknowledgement may expose only the generic outcome.
- Keep Junqi single-player snapshots immediately before the human move so undo also removes the robot reply and restores both knowledge states, score, quiet counter, result, and last move. Two-player snapshots contain one move and undo returns to an opaque handoff for the restored mover. Winner states hide undo, draws retain it, and restart clears history.
- Keep Junqi AI search outside the session and Compose UI thread: `JunqiSession` produces observation-only generation-bound robot requests, `JunqiPlugin` owns and disposes a dedicated background executor, and `applyRobotMove` must reject stale requests after any move, undo, restart, handoff, or battle acknowledgement changes the session generation.
- Keep Junqi UI projections observation-only: blue-bottom display mapping rotates coordinates 180 degrees without rotating labels, opponents remain backs except a publicly revealed flag, and `HANDOFF`/`BATTLE_RESULT` must mount no board or piece semantics beneath their fully opaque pages.
- Keep Junqi package texture loading version-keyed by package root and manifest version. Before full decode, `BitmapFactory` bounds must report `image/png` and the asset-specific exact dimensions: icon `1024x1024`, board `1400x1680`, shelf `1400x360`; after full decode, dimensions must match again, otherwise use the existing Compose fallback.
- Keep Xiangqi board and piece textures reproducible through `games/xiangqi/tools/generate_xiangqi_assets.py`; font selection must cover every required traditional glyph before writing assets, and the generated pieces must use `games/xiangqi/tools/source/ceramic-piece-master.png` as their only material base.
- Keep single-player side-selection and opening-turn rules in each game's session model; restart behavior changes must cover player win, player loss, and robot opening as second-player tests.
- Record undo snapshots immediately before legal player actions, include score and terminal state in each snapshot, and keep the initial robot opening outside undo history.
- Keep the last-move marker in each game's session and undo snapshot. Othello marks only the newly placed disc, Xiangqi marks the destination coordinate after perspective mapping, and robot moves replace the preceding player marker.
- Keep last-move marker geometry and color constants aligned across all five game packages; marker scale must remain below one cell so adjacent pieces are unaffected.
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
| [docs/superpowers/specs/2026-07-21-usb-adb-connect-script-design.md](docs/superpowers/specs/2026-07-21-usb-adb-connect-script-design.md) | USB ADB device listing and exact-serial connection command contract |
| [docs/superpowers/plans/2026-07-21-usb-adb-connect-script.md](docs/superpowers/plans/2026-07-21-usb-adb-connect-script.md) | TDD implementation steps for USB ADB device listing and exact-serial verification |
| [docs/agents/game-plugins.md](docs/agents/game-plugins.md) | GamePlugin contract, zip layout, adding a game |
| [designs/specs/android-games-home.md](designs/specs/android-games-home.md) | Current home-screen visual SSOT |
| [designs/specs/android-games-family-versus-logo.md](designs/specs/android-games-family-versus-logo.md) | Approved family-versus Logo and launcher-icon SSOT |
| [designs/specs/xiangqi-ui.md](designs/specs/xiangqi-ui.md) | Approved bright, simple Chinese Xiangqi UI and board/piece texture SSOT |
| [designs/specs/junqi-ui.md](designs/specs/junqi-ui.md) | Proposed Junqi command-table UI SSOT and preview index, pending user direction selection |

## Done checklist

- [ ] Targeted module unit tests pass
- [ ] `npm run verify` passes
- [ ] No secrets or build artifacts staged
- [ ] If `app/` changed, the game-center `versionCode` and `versionName` were incremented together
- [ ] Each touched game has matching, incremented versions in plugin code and `package/manifest.json`
- [ ] Each touched game's README still matches its implementation, manifest, assets, and commands
- [ ] If `game-api` changed, all five games still build and load
