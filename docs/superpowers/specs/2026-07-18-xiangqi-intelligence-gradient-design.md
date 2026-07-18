# Xiangqi Intelligence Gradient Design

Date: 2026-07-18
Status: Approved for implementation

## Goal

Replace the current one-step heuristic Xiangqi robot with a compact, offline
search engine and expose a stable ten-level intelligence gradient:

- Level 1 is suitable for a child learning the rules.
- Level 10 is the strongest level available to an experienced player.
- Adjacent levels are statistically distinguishable across repeated games.
- The Xiangqi package remains independently buildable and updateable.

The active single-player level is derived from the player's accumulated score:

```text
level = min(playerScore + 1, 10)
```

In single-player mode, `playerScore` is the human player's accumulated win
count, independent of whether the human currently controls red or black. A
player with zero wins faces level 1, one win faces level 2, and nine or more
wins faces level 10. A loss or draw does not reduce the level. Changing sides
between rounds must not move accumulated wins to the robot.

Single-player scoring is therefore displayed as player versus robot. Two-player
mode retains the existing red-versus-black score. Both forms remain part of undo
snapshots.

## Chosen Approach

Use one pure-Kotlin Xiangqi search engine for all ten levels.

The engine uses iterative-deepening Negamax with alpha-beta pruning. Low levels
are weakened through restricted evaluation features, small search budgets, a
larger candidate pool, and deterministic weighted selection of suboptimal
moves. High levels search more nodes, enable the full compact evaluation, and
always select the best completed result.

This design deliberately does not embed Pikafish, ElephantEye, NNUE weights,
native libraries, or an opening-book dependency. Those options provide higher
absolute strength but conflict with the current goal of a small, dex-only,
independently packaged game. Level 10 is therefore the strongest compact local
engine level and must be described as targeting experienced players, not as a
professional-engine strength guarantee.

## Engine Boundary

Keep rule authority and search policy separate:

- `XiangqiRules` owns legal movement, check detection, winner detection, and
  application of moves.
- `XiangqiAi` owns search, evaluation, move ordering, level configuration, and
  deterministic candidate selection.
- `XiangqiSession` owns mode-aware scores and the score-to-level mapping.
- `XiangqiPlugin` requests an AI move without containing search policy.

The old `robotMoveScore`, `immediateReplyThreat`, and one-step robot selection
are removed. Immediate general capture, checkmate preference, safe-move
filtering, and poisoned-capture avoidance become search regression tests rather
than special-purpose production branches.

## Ten-Level Configuration

Each level is represented by immutable configuration rather than scattered
conditionals.

| Level | Audience | Depth cap | Node budget | Time | Pool | Suboptimal | Evaluation | Q-depth |
| ---: | --- | ---: | ---: | ---: | ---: | ---: | --- | ---: |
| 1 | Child learner | 1 | 64 | 80ms | 8 | 65% | material | 0 |
| 2 | Child beginner | 2 | 512 | 100ms | 6 | 50% | material + safety | 0 |
| 3 | New player | 3 | 4,096 | 150ms | 5 | 32% | material + safety | 0 |
| 4 | Elementary player | 4 | 13,000 | 325ms | 4 | 20% | basic positional | 0 |
| 5 | Casual player | 4 | 20,000 | 400ms | 2 | 8% | basic positional | 0 |
| 6 | Skilled casual player | 5 | 96,000 | 1,000ms | 1 | 0% | full positional | 0 |
| 7 | Amateur player | 5 | 160,000 | 1,000ms | 1 | 0% | full positional | 0 |
| 8 | Strong amateur player | 6 | 300,000 | 1,500ms | 1 | 0% | full + quiescence | 1 |
| 9 | Advanced player | 7 | 700,000 | 2,200ms | 1 | 0% | full + quiescence | 1 |
| 10 | Experienced player | 8 | 2,000,000 | 3,000ms | 1 | 0% | full + quiescence | 2 |

The node budget is the strength control and produces more consistent behavior
across devices than a time-only limit. The caller also supplies a safety
deadline so a slow device cannot block indefinitely. The engine returns the
best move from the last fully completed iteration.

These values are the calibrated `0.0.7` product parameters. Further statistical
match calibration changes configuration data without changing the public level
meanings or score mapping.

## Deterministic Weakening

Levels 1 through 5 may choose a suboptimal move, but weakening must remain
credible:

- Never choose an illegal move.
- Never ignore an available immediate general capture.
- Never choose a move that permits an immediate general capture when a safe
  legal alternative exists.
- Rank the root candidates using the completed search.
- Select within the configured candidate pool using a stable seed derived from
  the position and level.
- Keep deterministic tests reproducible for the same position and level.

This avoids childish-looking random movement while still producing a visible
difference between beginner levels.

## Evaluation Profiles

Evaluation features are added progressively:

1. Material: piece values and terminal scores.
2. Safety: attacked valuable pieces and immediate check pressure.
3. Basic positional: piece-square values, crossed-river soldiers, and central
   activity.
4. Full positional: mobility, general safety, defended attackers, and phase
   aware piece-square values.
5. Quiescence: continue forcing captures and checks at the nominal leaf so the
   engine does not stop in the middle of an obvious exchange.

All scores are evaluated from the side-to-move perspective and use mate-distance
adjustments so a faster win and a slower loss are preferred.

## Search And Performance

Search requirements:

- Iterative deepening starts at depth 1.
- Negamax implements alpha-beta bounds.
- Move ordering prioritizes the previous iteration's principal move, immediate
  wins, captures ordered by victim/attacker value, checks, then quiet moves.
- A bounded Zobrist transposition table may be used by levels 7 through 10.
- Search checks cancellation, node budget, and deadline regularly.
- A budget stop returns the last completed iteration, never a partially scored
  root result.

The initial implementation may use the existing immutable `XiangqiState`, but
legal move generation must stop testing all 90 destination squares for every
piece. Candidate destinations are generated per piece before the existing
legality and self-check filter is applied.

AI search must run away from the Compose UI thread. While the robot is thinking,
board input is disabled. Restart, undo, or exit cancels and invalidates any
pending result so a stale move cannot be applied to a newer position.

## Single-Player Flow

- Compute the displayed and active AI level from the human player's accumulated
  single-player win score
  immediately before each robot turn.
- The initial robot opening uses the level derived from the current score.
- A human win increments the player score; the next robot turn therefore
  uses the next level, capped at 10.
- A robot win increments the robot score but does not reduce or otherwise
  change the human-derived intelligence level.
- A draw leaves scores and level unchanged.
- Undo restores scores from the existing snapshot and therefore also restores
  the derived level.
- Restart preserves the accumulated score and derives the level again after the
  existing side-selection rule has run.
- Two-player mode does not show or use an intelligence level.

The side rail shows `智能等级 N` in single-player mode so the player can observe
the current gradient.

## Verification

Deterministic unit tests must cover:

- Score 0 maps to level 1, score 9 maps to level 10, and higher scores remain at
  level 10.
- The human's accumulated score remains attached to the player when the player
  changes between red and black.
- Two-player scoring remains red versus black and never activates the AI
  gradient.
- Undo-derived score restores the corresponding level.
- Every level returns a legal move.
- All levels capture an exposed opposing general.
- Search preserves immediate checkmate preference and poisoned-capture
  avoidance.
- The same state and level produce the same weakened move.
- Higher levels receive strictly non-decreasing depth and node budgets.
- Cancellation or budget exhaustion returns a legal completed result.
- Two-player behavior remains unchanged.

Strength calibration is statistical rather than a unit-test assertion:

- Run at least 100 color-swapped games for each adjacent pair.
- Target a 65% to 75% score for the higher adjacent level.
- Target at least 80% for levels separated by two steps.
- Adjust configuration data, not search correctness, when calibration misses.

The `0.0.7` compact calibration on the local JVM reference run records:

- Representative-midgame completed depths:
  `[1, 2, 3, 4, 4, 5, 5, 5, 6, 6]`.
- Representative-midgame visited nodes:
  approximately `[39, 150, 1,347, 10,286, 10,286, 81,076, 81,076,
  168,000, 250,000, 380,000]`; deadline-bound high-level counts vary with the
  reference machine while completed-depth floors remain asserted.
- Level 10 completes depth 6 within the three-second guard, compared with depth
  2 and roughly 3,800 nodes before the search-position optimization.
- Across 12 deterministic opening and middlegame positions, adjacent identical
  move counts for levels 1 through 5 are `[1, 2, 4, 9]`, or at most 75%;
  the 4→5 strength distinction is additionally established by its 69.5%
  higher-level score in the 100-game match.
- The completed 100-game color-swapped release pairs score:
  `1→2 73.5%`, `2→3 71.5%`, `3→4 68.0%`, `4→5 69.5%`, and
  `5→6 73.0%` for the higher level. These cover the QA-reported weak 1–5 range
  and its transition to level 6. Pairs `6→7` through `9→10` remain opt-in
  release runs and are not claimed as completed in this calibration record.

The normal test suite checks this compact corpus and the color-swap harness.
The full 100-game adjacent-pair ladder is explicitly enabled with:

```bash
./gradlew :games:xiangqi:testDebugUnitTest \
  --tests com.buddygames.xiangqi.XiangqiAiCalibrationTest \
  -PxiangqiCalibration=true \
  -PxiangqiCalibrationPair=1
```

Use pair values `1` through `9` to run and retain each adjacent 100-game report
independently. Omitting `xiangqiCalibrationPair` runs the complete ladder.

Repository completion still requires the Xiangqi unit tests followed by
`npm run verify`.
