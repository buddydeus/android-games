# Board Game Center - game

**Preview file:** `designs/previews/board-game-center-game.png`
**Spec SSOT:** `designs/specs/board-game-center.md`

## Layout

Landscape 16:10 Xiangqi match view. The left two thirds hold a large independent wood board. The right third is a narrow raised information rail with only score, current turn, and a quiet destructive exit button at the bottom. No header, restart control, rules panel, chat, or bottom bar.

## Modules

- Xiangqi board object: dark wood outer frame, pale maple playing surface, 9 vertical and 10 horizontal lines, river gap, and round pieces precisely centered on intersections.
- Information rail: “历史比分” / “0 : 0”, “当前回合” / “你的回合 · 执红”, and “退出游戏”.

## Visual Details

Canvas is `#DCE2E2`; board wood is `#C8985F` and frame `#593F27`; text uses `#1D2B2F`; red-side pieces and exit use `#A43B32`. Piece shadows are subtle enough to preserve grid geometry. The board is the physical focal object; the information rail is quiet and highly legible.

## Image Prompt

Create a high-fidelity Android tablet game UI mockup, 16:10 landscape, of a Chinese Xiangqi match. Use a cool mineral blue-grey #DCE2E2 background. On the left, a large independent physical Xiangqi board with a dark walnut outer frame #593F27 and a pale maple playing surface #C8985F. Draw exactly 9 vertical and 10 horizontal lines with the river gap, and place round Chinese chess pieces precisely centered on the line intersections, not inside square centers. Black pieces use ink #1D2B2F; red pieces use restrained cinnabar #A43B32; pieces have thin brass outlines and subtle shadows. On the right, a narrow raised white-grey information rail containing only Chinese labels 历史比分, 0 : 0, 当前回合, 你的回合 · 执红, and a bottom exit button 退出游戏. Noto Sans Chinese UI typography, Songti only for board labels. Board-first, quiet and tactile, accessible contrast, no top bar, no restart, no rules, no bottom nav, no gradients, no neon, no decorative clutter.
