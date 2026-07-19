# Xiangqi UI - Active Game

**Preview file:** `designs/previews/xiangqi-ui-game.png`
**Spec SSOT:** `designs/specs/xiangqi-ui.md`

## Layout

Landscape Android Pad UI at 1600 x 1000. The board remains the dominant left surface. The right rail presents score, intelligence level, current turn, check state, undo, and exit in a strict vertical reading order.

## Modules

- **Active board:** mid-game position with pieces on intersections, one selected red piece, and one latest-move blue four-corner marker.
- **Status rail:** `玩家 : 智能`, score `2 : 1`, `智能等级 3`, `当前回合：黑方`, and a compact `将军` warning.
- **Actions:** outlined `悔棋` and `退出游戏` buttons; no restart button while the round is active.
- **Orientation:** red side shown at the bottom in this preview; implementation also supports the existing rotated Black-player view.

## Visual Details

Use the exact thin-frame board and raised glazed ceramic piece family from the menu preview. Keep the selected-piece ring celadon-blue and visibly separate from the piece shadow. The latest move uses the shared translucent bright-blue four-corner frame with a clear center. `当前回合：` stays neutral while `黑方` uses blue-black ink in a light outlined field. `将军` uses cinnabar text and outline, not a large filled alarm block.

## Image Prompt

Create a polished high-fidelity active-game UI mockup for an offline Chinese chess Xiangqi game on a landscape Android tablet, 1600x1000 composition. Bright, simple, contemporary Chinese design matching a porcelain-and-celadon Xiangqi set. Left two-thirds: complete orthographic 9-by-10 board with warm ivory porcelain surface #FAF7EF, thin pale celadon rim #A9C8BE, crisp ink-green grid #27443F, palace diagonals, and a pale celadon river band #E2ECE7 labeled exactly "楚河" and "漢界". Show a believable mid-game position with raised warm-ivory glazed ceramic pieces placed precisely on intersections, each with a slim double brass-gold rim, upper-left highlight, lower-right shade and soft cast shadow; use cinnabar glyphs for Red and blue-black glyphs for Black. Add one subtle celadon-blue selection ring with a visible gap around a red piece. Mark the most recent destination with a translucent bright-blue four-corner frame, center completely clear. Right third: flat matte white score-sheet rail, labels "玩家 : 智能", large score "2 : 1", "智能等级 3", then "当前回合：" in neutral dark ink and "黑方" in blue-black with a light outline, a restrained cinnabar outlined "将军" notice, and large clean outlined buttons "悔棋" and "退出游戏". Background #EAF1EF, generous whitespace, readable Chinese, precise Android tablet touch UI. No card nesting, no dark wood, no antique scroll, no ornate patterns, no glass blur, no gradients, no hands, no device frame, no watermark.
