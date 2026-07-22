# junqi-ui - game-tablet

**Preview file:** `designs/previews/junqi-ui-game-tablet.png`
**Spec SSOT:** `designs/specs/junqi-ui.md`

## Layout

Wide Android tablet landscape mockup using the current verified runtime aspect ratio. The complete 12x5 board remains fully visible on the left. The right order rail aligns its top score, middle turn order and bottom actions to the board's three structural zones. The implementation still has to preserve this relationship at the 800x600dp baseline.

## Modules

- Live board: all blue opponents shown only as blue enamel backs with a shallow shield emblem; red own pieces use ivory enamel faces, red edges, shoulder-rank grooves and readable two-character Chinese ranks.
- Move state: one blue opponent move is surrounded by the shared bright-blue four-corner last-move frame, with a clear gap around the tile; one red own piece has a brass selection inset and legal destinations use bone dots.
- Score line: exact labels `玩家` and `智能`, exact score `0 : 0`, exact `智能等级 1`.
- Turn order: exact `红方回合` and `玩家执红`, expressed by text plus a narrow red rule.
- Actions: enabled outlined `悔棋` and quiet `返回菜单` at the bottom; no unavailable or irrelevant controls.

## Visual details

The board is the only visually heavy element. Brass railways cross bone roads with correct hierarchy and the central bridge spine is clearly visible. Enamel pieces have thin bevels and soft contact shadows but stay inside their node cells. The rail is not a card and uses two thin brass dividers. Score numerals are monospaced. No text overlaps at 800x600dp.

## Image prompt

Use case: style-transfer
Asset type: high-fidelity Android tablet live-game design preview
Input image: `build/runtime-acceptance/single-playing.png` is the geometry and content reference; preserve its exact 12x5 graph, piece inventory, placement, score and controls.
Primary request: Restyle the existing live single-player screen of a two-player hidden-information Chinese Junqi board game for an offline Android Pad without changing gameplay content or geometry.
Scene/backdrop: wide landscape Android app screenshot matching the source aspect ratio, bright cold mineral-grey outer canvas, no device frame.
Subject: Preserve the source's complete tall 12-by-5 Junqi board, every road and railway edge, camps, headquarters, all blue hidden backs, all red own pieces and exact readable rank labels. Restyle the board as deep matte field-green with crisp bone-white roads, brushed-brass double-track railways, mint-grey camps, brass headquarters and a riveted central bridge spine. Blue backs use enamel #28577F with shallow shield emblems; red pieces use ivory enamel faces and #C23B32 edges. Add the shared translucent bright-cyan four-corner frame outside the second-row, second-column blue tile with a visible gap and clear center. Keep the source right-rail text and controls unchanged.
Style/medium: polished production UI mockup, tactile enamel military board pieces, brushed brass, precise command-table drafting aesthetic, Android Compose-ready proportions.
Composition/framing: orthographic front view, complete board and right rail side by side, no crop, no scroll, no overlap, 800x600dp-equivalent spacing.
Lighting/mood: soft diffuse light, restrained tactile shadows, high legibility.
Color palette: exact #E7ECE9, #F4F6F1, #18372F, #E6DEC2, #C3A15B, #182723, #C23B32, #28577F, last move #4FCBFF at translucent opacity.
Text (verbatim): "玩家", "智能", "0 : 0", "智能等级 1", "红方回合", "玩家执红", "悔棋", "返回菜单" plus readable Chinese ranks on red pieces only.
Constraints: preserve source board geometry, piece count, placement and text; red and blue identity also written in text, zero letter spacing, buttons no more than 8px radius, board roads and rails remain unobstructed, enemy ranks never visible.
Avoid: camouflage, guns, soldiers, explosions, neon HUD, black full-screen theme, gradients, floating cards, pill labels, glossy casino chips, circular chess pieces, oversized tiles, tiny unreadable text, English UI, watermarks.
