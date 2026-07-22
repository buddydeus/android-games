# junqi-ui - game-tablet

**Preview file:** `designs/previews/junqi-ui-game-tablet.png`
**Spec SSOT:** `designs/specs/junqi-ui.md`

## Layout

Wide Android tablet landscape mockup using the current verified runtime aspect ratio. The complete 12x5 board remains fully visible on the left. The right order rail aligns its top score, middle turn order and bottom actions to the board's three structural zones. The implementation still has to preserve this relationship at the 800x600dp baseline.

## Modules

- Live board: classic straw-gold labeled board; all upper green opponents are solid green square backs with a restrained white star/shield mark and no rank; all lower orange own pieces use solid orange faces with maximized white heavy sans-serif two-character ranks.
- Move state: one green opponent move is surrounded by the shared bright-blue four-corner last-move frame, with a clear gap around the tile; one orange own piece has a grey-green selection inset and legal destinations use dark ink dots.
- Score line: exact labels `玩家` and `智能`, exact score `0 : 0`, exact `智能等级 1`.
- Turn order: exact `橙方回合` and `玩家执橙`, expressed by text plus a narrow orange rule.
- Actions: enabled outlined `悔棋` and quiet `返回菜单` at the bottom; no unavailable or irrelevant controls.

## Visual details

The board is the only visually heavy element. Dark/ivory sleeper railways cross thin ink roads with correct hierarchy, and all three central bridge routes remain distinct. Square military pieces have thin bevels and soft contact shadows but stay inside their nodes. Rank text is pure white Android-style heavy sans serif at weight 900, approximately 62%-68% of piece height with at least 6% inner clearance. The rail is not a card and uses two thin grey-green dividers. Score numerals are monospaced. No text overlaps at 800x600dp.

## Image prompt

Use case: style-transfer
Asset type: high-fidelity Android tablet live-game design preview
Input images: `build/runtime-acceptance/single-playing.png` is the geometry and content reference; `designs/references/junqi-classic-board-reference.jpg` is the board form and material reference. Preserve the runtime screenshot's exact 12x5 graph, piece inventory, placement, score and controls.
Primary request: Restyle the existing live single-player screen of a two-player hidden-information Chinese Junqi board game for an offline Android Pad without changing gameplay content or geometry.
Scene/backdrop: wide landscape Android app screenshot matching the runtime source aspect ratio, bright cold mineral-grey outer canvas, no device frame.
Subject: Preserve the runtime source's complete 12-by-5 Junqi graph, every road and railway edge, camps, headquarters, all 25 upper hidden backs, all 25 lower own pieces and the exact source rank inventory and placement. Restyle the board after the classic reference: low-saturation straw-gold fiberboard, thin dark-ink roads, alternating dark-and-ivory railway sleepers, labeled rectangular stations, labeled oval camps, red-labeled headquarters and three separate center bridges. Upper hidden pieces become solid square #23704B green lacquer backs with a restrained white star/shield mark and absolutely no rank text. Lower own pieces become solid square #C65012 orange faces. Render every visible Chinese rank in pure white, Android-style sans-serif Black weight 900, as large as possible: approximately 62%-68% of the piece height, centered, two characters on one line, with at least 6% inner clearance and no stroke or shadow. Add the shared translucent bright-cyan four-corner frame outside the second-row, second-column green piece with a visible gap and clear center. Change the right-rail turn copy to exact `橙方回合` and `玩家执橙`; keep the score and controls unchanged.
Style/medium: polished production UI mockup, respectful modern reconstruction of a traditional Chinese Junqi railway board, tactile square military pieces, subtle fiberboard grain, crisp labels, Android Compose-ready proportions.
Composition/framing: orthographic front view, complete board and right rail side by side, no crop, no scroll, no overlap, 800x600dp-equivalent spacing.
Lighting/mood: soft diffuse light, restrained tactile shadows, high legibility.
Color palette: exact #E8EDE9, #F6F7F3, #D8C98F, #E9DCAA, #68745D, #494233, #2D2A23, #F1E5B9, #292B23, green pieces #23704B, orange pieces #C65012, piece text #FFFFFF, headquarters #B4312B, camp #BBC2A6, last move #4FCBFF at translucent opacity.
Text (verbatim): "玩家", "智能", "0 : 0", "智能等级 1", "橙方回合", "玩家执橙", "悔棋", "返回菜单" plus the exact readable Chinese ranks on orange pieces only.
Constraints: preserve source board geometry, piece count, placement and rank inventory; green and orange identity also written in text, zero letter spacing, buttons no more than 8px radius, board roads and rails remain unobstructed, enemy ranks never visible, visible ranks use heavy white sans-serif and occupy the maximum safe area inside each piece.
Avoid: serif or calligraphic rank text, small rank text, outlined rank text, white or ivory piece faces, red or blue piece faces, copying the reference watermark, low-resolution blur, orange vintage cast, camouflage, guns, soldiers, explosions, neon HUD, black full-screen theme, gradients, floating cards, pill labels, glossy casino chips, circular chess pieces, oversized tiles, English UI, watermarks.
