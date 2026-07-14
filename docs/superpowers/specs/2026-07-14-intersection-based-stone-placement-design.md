# Intersection-Based Stone Placement

## Goal

Render Gomoku stones and Xiangqi pieces on board-line intersections rather than inside square centers, without changing game rules, move coordinates, AI, navigation, package loading, or the shared plugin API.

## Rendering Contract

### Gomoku

- The existing 15 x 15 logical board represents 15 horizontal lines and 15 vertical lines.
- A logical move (row, col) is drawn at the intersection whose center is:
  - x = col * boardWidth / 14
  - y = row * boardHeight / 14
- The visible board therefore has 14 horizontal intervals and 14 vertical intervals.
- Tap hit areas are centered on the same intersections. Edge intersections remain tappable inside the board frame.

### Xiangqi

- The existing 9 x 10 logical board represents 9 vertical lines and 10 horizontal lines.
- A logical piece (row, col) is drawn at:
  - x = col * boardWidth / 8
  - y = row * boardHeight / 9
- The visible court has 8 horizontal intervals and 9 vertical intervals.
- The river label and palace diagonals remain background decoration; they do not cover or change interactive intersections.
- Tap hit areas are centered on the same intersections as pieces.

## Implementation Boundaries

- Keep the current GomokuState, XiangqiState, rule engines, and callbacks unchanged.
- Replace only the board rendering portion of GomokuPlugin.kt and XiangqiPlugin.kt.
- Extract package-local, non-Compose intersection geometry helpers so first, last, and midpoint coordinate behavior has unit-test coverage.
- Do not modify Othello, game-api, package manifests, dex loading, or root build tasks.

## Validation

- Unit tests prove the first and last positions are 0 and the available extent, and the middle position is centered.
- Targeted Gomoku and Xiangqi unit tests pass and their package zips build.
- npm run verify passes.
- Emulator screenshots confirm the first move in Gomoku lands at a line crossing and an Xiangqi piece remains centered on its file/rank intersection before and after a legal move.

