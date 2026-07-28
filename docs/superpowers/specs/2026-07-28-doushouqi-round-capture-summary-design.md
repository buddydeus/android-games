# Doushouqi Round Capture Summary Design

**Status:** Approved and implemented.

## Goal

Replace Doushouqi's latest-move capture display with the capture result of the
most recently completed round in both single-player and two-player modes. A
normal round publishes at most one captured piece per side and never exposes a
half-finished round while the second move is pending.

## Scope

- Change only the Doushouqi session projection, undo snapshots, active-game
  rail, tests, package version, and aligned documentation.
- Preserve rules, AI move selection, scores, side-switch policy, board
  interaction, menu layout, rail geometry, and package ownership.
- Keep single-player faction copy limited to `绿方` / `红方`.
- Keep two-player score and result copy unchanged.

## Round Boundaries

### Single-player

A normal round begins with the player's accepted legal move and completes with
the accepted robot reply.

- The player's move starts an unpublished pending summary.
- While the robot is thinking, the rail keeps showing the preceding completed
  round.
- The accepted robot reply completes and atomically publishes the pending
  summary.
- A robot opening caused by a restart with the player moving second is outside
  a player-plus-robot round. It does not replace the completed summary.
- If the player's move ends the game and therefore no robot reply can occur,
  publish that one-move terminal round immediately.

### Two-player

A normal round begins with Green's accepted legal move and completes with Red's
accepted legal reply.

- Green's move starts an unpublished pending summary.
- Until Red moves, the rail keeps showing the preceding completed round.
- Red's accepted move completes and atomically publishes the pending summary.
- If Green's move ends the game and therefore Red cannot reply, publish that
  one-move terminal round immediately.

Rejected, illegal, or stale moves never start, complete, or publish a round.

## State Model

Introduce one immutable capture summary:

```kotlin
data class DoushouqiRoundCaptures(
    val capturedByGreen: DoushouqiPiece? = null,
    val capturedByRed: DoushouqiPiece? = null,
)
```

Each property stores the piece removed by that side during the round. Because a
legal Doushouqi move can capture at most one destination piece, a completed
round contains zero, one, or two pieces in total.

`DoushouqiSession` owns:

- `lastCompletedRoundCaptures`, exposed through `DoushouqiSessionState`;
- nullable `pendingRoundCaptures`, kept private until the round completes.

`pendingRoundCaptures == null` means no player/Green first move is awaiting its
paired reply. A non-null but empty summary means a legal first move occurred
without a capture. This distinction lets the session recognize a robot opening
without inspecting UI state or move history.

When an accepted move captures a piece, the session records it under the
attacking move's side, not under the captured piece's side. Publishing replaces
the entire preceding completed summary. A completed round with no captures is
therefore represented by an empty `DoushouqiRoundCaptures`.

## Lifecycle

- Initial position: the completed summary is empty and pending is `null`.
- Normal first move: update only the pending summary.
- Normal second move: update pending, publish it as the completed summary, then
  clear pending.
- First-move terminal result: publish pending immediately, then clear it.
- Single-player robot opening: leave both summaries unchanged.
- Undo: restore completed and pending summary state from the snapshot associated
  with the restored board. Single-player paired undo returns to the summary
  visible before the player's move; two-player one-move undo restores the exact
  previous round boundary.
- Restart: clear completed and pending summaries.
- Invalidate-only generation change: preserve both summaries.
- Rejected or stale robot response: preserve both summaries.

## Rail Copy and Presentation

Remove the heading `最近一步吃子`. Show the completed summary directly in the
middle rail zone below the current-turn or result line.

- Green captured a rat: `绿方吃：鼠`.
- Red captured an elephant: `红方吃：象`.
- Both sides captured: show both lines in Green-then-Red order.
- Neither side captured: show `无吃子`.

The named animal is the captured piece. The prefix names the side that performed
the capture. Each non-empty line uses the attacker's existing faction color,
while the text remains sufficient without color. The two possible lines must
fit the existing 300dp rail without scrolling or changing action-button
geometry.

## Compatibility

The old nullable `lastCapturedPiece` projection is replaced rather than kept as
a parallel compatibility path. It is package-internal state and has no shell or
`game-api` contract impact.

The earlier latest-move design, plan, and runtime report remain historical
evidence for version `0.0.5`; this design supersedes their capture-summary
behavior.

## Testing

Session tests must prove:

- single-player player capture remains unpublished while the robot is pending;
- a robot reply publishes both player and robot captures together;
- a completed no-capture round replaces an older non-empty summary;
- a terminal player move publishes its one-move summary immediately;
- a robot opening does not publish a round;
- two-player Green capture remains unpublished until Red moves;
- the Red move publishes both sides' captures;
- a terminal Green move publishes immediately;
- undo restores the correct completed and pending summaries in both modes;
- restart clears both summaries;
- illegal and stale moves preserve both summaries.

UI helper tests must prove:

- empty summary renders `无吃子`;
- Green and Red labels use `绿方吃：<兽名>` and `红方吃：<兽名>`;
- two non-empty lines are ordered Green then Red;
- no new single-player copy emits `松绿`, `朱砂`, or `玩家执`.

Verification must include the Doushouqi module tests, `npm run verify`,
`git diff --check`, package manifest version inspection, and Android runtime
acceptance of one completed round.
