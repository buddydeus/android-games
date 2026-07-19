# international-chess-ui - game

**Preview file:** `designs/previews/international-chess-ui-game.png`
**Spec SSOT:** `designs/specs/international-chess-ui.md`

## Layout

Landscape Android tablet UI at 2560 x 1600. The square board dominates the left. The fixed right rail shows identity, score, AI level, current turn, check state, undo, and return controls. The screen is an actual mid-game position, not a marketing mockup.

## Modules

- Mineral app background.
- Active chess board with PNG-style Staunton pieces.
- Selected White knight with brass square outline.
- Legal empty move dots, one legal capture ring, and a bright-blue latest-move corner frame.
- Full-height right rail with score and status sections separated by restrained rules.
- Large touch controls for undo and return.

## Visual details

- Use SSOT colors and typography.
- Show state through geometry plus color: brass selection/capture, blue latest move, red check.
- Keep all overlays inside one square and preserve a visible gap around pieces.
- The rail is a single matte surface, not a stack of cards.
- Use tabular score figures and color only the side name in the turn line.

## Image prompt

Use case: ui-mockup
Asset type: high-fidelity active-game Android tablet preview
Primary request: Design an actual single-player international chess game screen for a shared Android tablet.
Scene/backdrop: cool mineral-grey app canvas, no external environment.
Composition/framing: exact 16:10 landscape screen; large square live chessboard on the left two-thirds; one full-height matte score-sheet information rail on the right; no nested cards.
Board: graphite-green mineral stone rim; alternating neutral grey-ivory #D8D4C6 and muted tournament green #557161 squares; coordinates visible but quiet; believable legal mid-game position with White at the bottom.
Pieces: modern Staunton pieces with consistent physical PNG asset appearance; White matte ivory porcelain with graphite edges; Black soft-black obsidian with cool rim light; slightly elevated front/top three-quarter view; upright and centered.
Gameplay states: selected White knight with a 4px brass square outline, several legal empty destination dots, one brass circular capture ring around an enemy piece, latest robot move marked by a translucent bright-blue four-corner frame with a clear center, checked Black king outlined in restrained red.
Rail text (verbatim Chinese): "玩家 : 智能", "0 : 0", "智能等级 1", "当前回合：黑方", "将军", "智能思考中 - 等级 1", "悔棋", "返回菜单".
Typography: Android system sans, tabular large score, zero letter spacing; only "黑方" is colorized within the current-turn line.
Controls: two large 8px-radius rectangular touch buttons near the bottom with clear outlines and at least 8px separation.
Lighting/mood: daylight tournament calm, precise, tactile but restrained.
Constraints: all text readable, no overlap, board remains square, state overlays do not resize cells, pieces have clear gaps from overlays, controls fit within the rail.
Avoid: brown wood, ornate fantasy chess, neon, glowing gradients, glass blur, nested cards, oversized title, marketing copy, watermark.
