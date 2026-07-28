# Doushouqi Single-Player Capture Summary Design

**Status:** Approved interaction design; implementation pending written-spec review.

## Goal

Simplify every Doushouqi single-player faction reference to `红方` / `绿方`, remove player-side ownership copy from the rail, and use that space to report the piece captured by the most recent move.

## Scope

- Change only Doushouqi single-player session projection, result copy, and active-game rail.
- Keep Doushouqi rules, AI, board interaction, score behavior, menu rail, and two-player presentation unchanged.
- Keep the existing three-zone active rail and its two horizontal dividers.

## Copy Contract

Single-player UI must not render `松绿`, `朱砂`, or `玩家执…`.

- Current turn: `当前回合：绿方` or `当前回合：红方`.
- Result faction: `绿方` or `红方`, followed by the existing result reason.
- Capture heading: `最近一步吃子`.
- Captured piece: the captured side plus animal name, such as `红方鼠` or `绿方象`.
- No capture on the most recent move: `无`.

The score heading remains `玩家 : 智能` because it describes competitors rather than board factions.

## State Model

`DoushouqiSession` records one nullable `lastCapturedPiece` value in its public projection.

Before applying any legal human or robot move, the session reads the destination occupant from the current position. After applying the move:

- if the destination contained an opponent piece, `lastCapturedPiece` becomes that piece;
- if the destination was empty, `lastCapturedPiece` becomes `null`.

This is deliberately move-based, not player-plus-robot-round-based. A robot reply replaces the record produced by the preceding human move, including replacing a capture with `null` when the robot does not capture.

## Lifecycle

- Initial position: no captured piece; rail shows `无`.
- Legal human move: update immediately from that move.
- Accepted robot move: update immediately from that move.
- Rejected or stale move: do not change the record.
- Undo: restore the record from the snapshot taken before the human action. In single-player this also removes the robot reply, matching existing paired undo behavior.
- Restart: clear the record.
- Invalidate-only generation change: preserve the record.

## Rail Presentation

The middle rail zone contains:

1. current turn or terminal result;
2. `最近一步吃子`;
3. one compact faction-colored label or `无`.

Only one captured piece may be shown. The row must fit the existing 300dp rail without scrolling or changing button geometry. Color reinforces the `红方` / `绿方` text and is never the only carrier of faction information.

## Testing

Unit tests must prove:

- human capture is projected immediately;
- a subsequent non-capturing robot reply clears the human capture;
- a robot capture replaces the human move record;
- a non-capturing human move produces `null`;
- undo restores the preceding record;
- restart clears the record;
- stale robot responses cannot change it;
- single-player helpers never emit `松绿`, `朱砂`, or `玩家执`;
- two-player result copy remains unchanged.

Runtime acceptance must confirm the single-player rail displays only red/green faction copy and the latest-move capture row.
