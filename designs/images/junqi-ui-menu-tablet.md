# junqi-ui - menu-tablet

**Preview file:** `designs/previews/junqi-ui-menu-tablet.png`
**Spec SSOT:** `designs/specs/junqi-ui.md`

## Layout

Wide Android tablet landscape mockup using the current verified runtime aspect ratio. A full-height vertical 12x5 Junqi command board occupies roughly 64% of the width on the left. A quiet, unframed order rail occupies the right 30% with generous spacing and no nested cards. The implementation still has to preserve this relationship at the 800x600dp baseline.

## Modules

- Command board preview: deep field-green surface, ivory roads, brass double-track railways and an unmistakable riveted brass bridge spine across the center.
- Clean preview state: no pieces on the menu board, so the exact road, railway, camp, headquarters and bridge topology remains the focal point.
- Brand block: existing circular package Logo, exact title `军棋`, exact version `版本 0.0.8`.
- Menu: exact labels `单人模式`, `双人对战`, `退出游戏`; primary action solid deep green, secondary outlined, exit red outlined.

## Visual details

Use exact palette from SSOT: canvas `#E7ECE9`, surface `#F4F6F1`, board `#18372F`, bone `#E6DEC2`, brass `#C3A15B`, ink `#182723`, red `#C23B32`, blue `#28577F`. Pieces are tactile enamel and brushed metal, not generic rounded cards. Right rail is flat and editorially quiet. Chinese typography uses restrained serif for title and clean sans serif for controls.

## Image prompt

Use case: style-transfer
Asset type: high-fidelity Android tablet game menu design preview
Input image: `build/runtime-acceptance/menu.png` is the geometry and content reference; preserve its exact board graph, layout, Logo placement and text.
Primary request: Restyle the existing main menu of a two-player hidden-information Chinese Junqi board game for an offline Android Pad without changing gameplay geometry or text.
Scene/backdrop: wide landscape Android app screenshot matching the source aspect ratio, bright cold mineral-grey outer canvas, no device frame.
Subject: The source's complete empty 12-by-5 Junqi command board fills the left 64 percent. Preserve every road and rail connection. Restyle it as deep matte field-green with crisp bone-white roads, brushed-brass double railways, mint-grey camps, brass shield-shaped headquarters, and a memorable riveted brass railway bridge spine crossing the exact center. Do not add pieces. On the right is a flat unframed military order rail with the existing round red-versus-blue Junqi Logo, large exact Chinese title "军棋", exact smaller text "版本 0.0.8", and three large buttons with exact labels "单人模式", "双人对战", "退出游戏".
Style/medium: polished production UI mockup, tactile command-table materials, restrained Chinese military drafting aesthetic, precise Android Compose proportions, bright surrounding shell with one bold dark board.
Composition/framing: orthographic front view, board and right rail fully visible, no scrolling, no overlapping text, 800x600dp-equivalent spacing, system status and navigation bars may be omitted.
Lighting/mood: soft diffuse studio light, subtle material depth, no dramatic shadows.
Color palette: exact #E7ECE9, #F4F6F1, #18372F, #E6DEC2, #C3A15B, #182723, #C23B32, #28577F.
Text (verbatim): "军棋", "版本 0.0.8", "单人模式", "双人对战", "退出游戏".
Constraints: preserve the exact board graph and all source text; no pieces; title in restrained Chinese serif, controls in clean Chinese sans serif, zero letter spacing, buttons no more than 8px corner radius, one solid primary button, no cards inside cards.
Avoid: camouflage, guns, bullets, explosions, battle scenes, neon HUD, black full-screen theme, warm cream and terracotta palette, blue-orange SaaS UI, glassmorphism, gradients, floating cards, pill chips, decorative numbers, English labels, illegible Chinese, watermarks.
