# Xiangqi AI Search Optimization and Calibration Design

Date: 2026-07-18
Status: Implemented in Xiangqi 0.0.7; long release calibration remains opt-in

## Problem

The ten Xiangqi levels currently have monotonic configuration values but do not
produce a reliable strength ladder. Fixed-position diagnostics found:

- Levels 1 and 2 complete only depth 1.
- Levels 3 and 4 usually complete depth 2.
- Level 5 completes depth 3 in only a small minority of sampled positions.
- Levels 6 and 7 often complete the same depth and select the same move.
- Levels 8 through 10 often complete only depth 2 because quiescence and full
  evaluation consume the deadline.
- A sampled level-10 search visited only thousands of nodes before its
  three-second deadline despite a configured two-million-node budget.

The defect is therefore not a lack of larger configuration numbers. Search
work per node is too expensive, so wall-clock deadlines stop iterative
deepening before the configured depth and node differences become effective.

## Goal

Keep the Xiangqi package compact, offline, pure Kotlin, and independently
updateable while making the ten levels measurably ordered:

- Levels 1 through 5 must no longer feel like one uniformly easy group.
- Adjacent levels must differ in playing strength, not merely choose different
  deterministic moves.
- Level 10 must complete materially deeper searches than the current
  two-ply result in representative midgames.
- The existing score mapping remains `min(playerScore + 1, 10)`.
- Immediate general capture, checkmate preference, safe-move filtering,
  poisoned-capture avoidance, cancellation, and stale-result protection remain
  intact.

This work does not introduce JNI, native engines, opening-book assets, network
access, new Android dependencies, or changes to `game-api`.

## Chosen Architecture

### Search-only mutable position

Add an internal `XiangqiSearchPosition` owned by the Xiangqi AI package. It
encodes the 90 intersections in one primitive array and supports:

- conversion from `XiangqiState`;
- allocation-free piece lookup;
- `makeMove` and `unmakeMove` with a compact undo record;
- direct general-location tracking;
- incremental deterministic Zobrist hashing;
- piece-specific pseudo-legal move generation;
- legal filtering and direct check detection without converting back to
  `XiangqiState`.

`XiangqiRules` remains the product rule authority used by the game session.
The search position is an optimized internal representation of the same rules.
Randomized equivalence tests must compare its legal moves with
`XiangqiRules.legalMoves` after every generated position and after make/unmake.
Search representation drift is a release blocker.

### Search result and observability

Introduce internal search result types:

```kotlin
internal data class XiangqiSearchStats(
    val completedDepth: Int,
    val visitedNodes: Int,
    val elapsedMillis: Long,
    val stopReason: XiangqiSearchStopReason
)

internal data class XiangqiSearchResult(
    val move: XiangqiMove?,
    val stats: XiangqiSearchStats
)
```

`XiangqiAi.search(...)` returns this result for tests and calibration.
`XiangqiAi.chooseMove(...)` remains the UI-facing compatibility wrapper and
returns only `result.move`. No diagnostics are shown in the player UI.

The last fully completed iterative-deepening result remains authoritative.
Cancellation, deadline, and node exhaustion are distinct stop reasons so a
test can prove which budget actually constrained a level.

### Efficient Negamax

Retain iterative-deepening Negamax with alpha-beta pruning, but change the hot
path:

- Generate each legal move once per node.
- Compute each move-ordering score once and sort cached `(move, score)` pairs.
- Prioritize previous principal variation, general capture, transposition-table
  move, captures by victim/attacker value, checks, then quiet moves.
- At a normal depth-zero leaf, evaluate directly without first sorting every
  legal move.
- Generate full evasions only when the side is in check.
- Use make/unmake instead of allocating copied boards for child nodes.
- Evaluate material, piece-square values, king safety, attacks, support, and
  mobility from direct board scans or generated attack data; evaluation must
  not call the full public legal-move generator multiple times.

Add a fixed-size primitive-array transposition table for levels 6 through 10.
Entries contain hash, depth, score, bound type, and best move. The table has a
hard memory cap and is recreated per robot turn, so the game package remains
self-contained and restart/cancellation behavior stays simple.

Quiescence remains enabled for levels 8 through 10. It searches captures,
checks, and all legal check evasions, but uses the same make/unmake position and
bounded depth. It must not make high levels shallower than levels 6 and 7 in
representative positions.

## Level Policy

The ten immutable configurations remain the only strength-control source, but
their exact node and time values are recalibrated after the optimized engine is
measured. A configuration is accepted only when its effective behavior meets
all of these rules:

- Depth and node budgets are non-decreasing.
- Representative completed depth is non-decreasing.
- Levels sharing an evaluation profile must still differ through a reliably
  completed deeper search or a deliberate, tested weakening rule.
- Deterministic weakening applies only to levels 1 through 5.
- Level 1 remains legal and tactically safe from immediate general loss but may
  choose visibly suboptimal quiet moves.
- Levels 4 and 5 may share a completed depth only when their candidate pool and
  weakening policy produce a calibrated adjacent-match advantage.
- Levels 6 and 7 may share a completed depth only when their node budgets
  produce a calibrated adjacent-match advantage.
- Levels 8 through 10 may not collapse to the same completed depth in the
  calibration corpus.
- Level 10 targets at least depth 5 on every representative midgame fixture and
  a median completed depth of at least 6 on the local JVM reference run within
  three seconds.

The time cap remains a UI responsiveness guard. Node budget and completed depth
are the primary strength controls. Calibration must reduce expensive evaluation
features or improve search efficiency instead of merely extending the maximum
think time beyond three seconds.

## Calibration Corpus

Add a deterministic, headless calibration harness inside the Xiangqi test
source set. It is opt-in and is not part of the normal fast `npm run verify`
path.

The corpus contains:

- opening positions generated from a checked-in list of legal opening move
  sequences;
- representative quiet middlegames;
- tactical positions covering captures, checks, exchanges, poisoned captures,
  and short mates;
- simplified endgames.

The harness records level, color, result, plies, completed depth, nodes, elapsed
time, and stop reason. Matches use the same opening twice with colors swapped.
Draw adjudication uses a fixed maximum ply count and repeated-position
detection, both local to the harness.

Release calibration runs at least 100 color-swapped games for every adjacent
pair. Acceptance targets remain:

- The higher adjacent level scores 65% to 75%.
- A level two steps higher scores at least 80%.
- No adjacent pair has more than 80% identical best moves across the
  fixed-position corpus.
- Strength ordering must hold for both red and black aggregates.

These percentages are calibration gates, not normal unit-test assertions.
The checked-in unit suite uses a smaller deterministic corpus to verify
ordering mechanics, effective-depth floors, legal equivalence, and stable
report generation.

## TDD and Regression Gates

Implementation proceeds through these red-green cycles:

1. Search-position make/unmake and legal-move equivalence.
2. Incremental hash restoration and transposition-table lookup.
3. Leaf search avoids full sorted move generation.
4. Search statistics report completed depth, nodes, elapsed time, and stop
   reason accurately.
5. Fixed midgames demonstrate increasing completed-depth floors.
6. Existing tactical robot tests pass through the new search representation.
7. Calibration harness produces deterministic color-swapped results.
8. Recalibrated level configuration passes the compact ordering corpus.

Performance assertions use node counts and completed depths under a disabled or
generous test deadline where possible. Wall-clock measurements are reported but
are not used as brittle unit-test equality assertions.

## Delivery and Versioning

All production changes remain under `games/xiangqi/`. The Xiangqi plugin and
package manifest versions are incremented together. `app`, `game-api`, the
other games, root packaging tasks, dependencies, and the score-to-level mapping
remain unchanged.

Completion requires:

1. `./gradlew :games:xiangqi:testDebugUnitTest`
2. Opt-in calibration runs for the affected adjacent pairs, with their reports
   reviewed against the acceptance targets; the complete nine-pair ladder is a
   longer release audit
3. `npm run verify`
4. A scoped local commit without pushing
