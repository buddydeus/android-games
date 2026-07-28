# Doushouqi Two-Player Immediate Capture Summary Design

**Status:** Approved and implemented.

## Goal

Change Doushouqi two-player capture reporting from a merged Green-plus-Red round
to the result of the most recent accepted move. Single-player keeps its existing
player-plus-robot completed-round behavior.

## Scope

- Change only Doushouqi two-player session publishing, shared rail projection,
  tests, package version, and aligned documentation.
- Preserve rules, AI, scores, side switching, move legality, board interaction,
  menu layout, rail geometry, package assets, and single-player capture timing.
- Do not change `game-api` or the game-center shell.

## Mode-Specific Semantics

### Single-player

No behavior changes:

- the player's accepted move starts a private pending summary;
- the previous completed summary remains visible while the robot is thinking;
- the accepted robot reply publishes the complete player-plus-robot round;
- a terminal player move publishes immediately;
- an automatic robot opening does not publish a round.

### Two-player

Every accepted move immediately replaces the visible capture summary:

- a Green capture displays `绿方吃：<兽名>`;
- a Red capture displays `红方吃：<兽名>`;
- a non-capturing move displays `无吃子`;
- only the latest accepted move is represented, so the rail shows at most one
  capture line;
- the next move always replaces the preceding move's summary, even when this
  clears a capture to `无吃子`.

There is no pending or merged round in two-player mode. A terminal move follows
the same immediate publishing path and needs no special capture-summary branch.

## State Model

Keep the existing immutable `DoushouqiRoundCaptures` projection so Compose can
continue using one renderer in both modes:

```kotlin
data class DoushouqiRoundCaptures(
    val capturedByGreen: DoushouqiPiece? = null,
    val capturedByRed: DoushouqiPiece? = null,
)
```

In two-player mode an accepted move constructs a fresh summary and records only
the current attacker and destination occupant. It assigns that value directly
to `lastCompletedRoundCaptures` and leaves `pendingRoundCaptures` as `null`.
Therefore a two-player projection can never contain both non-null properties.

Single-player continues using nullable `pendingRoundCaptures` exactly as version
`0.0.6` does.

## Lifecycle

- Initial state: `无吃子`.
- Accepted two-player capture: publish one attacker line immediately.
- Accepted two-player quiet move: replace the summary with empty and show
  `无吃子`.
- Illegal or rejected move: preserve the current summary.
- Two-player undo: restore the board and summary from the snapshot before the
  undone move.
- Restart: clear the summary to `无吃子`.
- Single-player undo, restart, robot opening, stale request, and pending behavior
  remain unchanged.

## Rail Presentation

The existing rail renderer remains shared:

- empty summary renders `无吃子`;
- one Green slot renders `绿方吃：<兽名>`;
- one Red slot renders `红方吃：<兽名>`.

Single-player may still render two lines when both sides captured during its
completed player-plus-robot round. Two-player renders zero or one line because
its session projection contains only the latest move.

## Compatibility

The `DoushouqiRoundCaptures` and `DoushouqiSessionState` shapes do not change.
Only the two-player state transition policy changes. The prior completed-round
design and runtime report remain historical evidence for version `0.0.6`; this
design supersedes only their two-player merge behavior.

Release Doushouqi as version code `7`, version name `0.0.7`. The shell version
does not change.

## Testing

Session tests must prove:

- a Green capture publishes immediately without waiting for Red;
- a following quiet Red move replaces the Green capture with an empty summary;
- a Red capture publishes immediately and contains only the Red slot;
- two-player projections never merge captures from consecutive moves;
- undo restores the summary visible before the undone move;
- restart clears the summary;
- illegal moves preserve the summary;
- all existing single-player completed-round tests remain unchanged and pass.

UI helper tests continue proving attacker labels, animal names, empty handling,
and Green-before-Red ordering for the single-player two-line case.

Verification must include Doushouqi unit tests, `npm run verify`,
`git diff --check`, packaged version inspection, and Android runtime acceptance
showing a two-player capture immediately followed by a quiet move.
