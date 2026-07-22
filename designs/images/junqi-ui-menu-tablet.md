# junqi-ui - menu-tablet

**Preview file:** `designs/previews/junqi-ui-menu-tablet.png`
**Spec SSOT:** `designs/specs/junqi-ui.md`

## Layout

Wide Android tablet landscape mockup using the current verified runtime aspect ratio. A full-height vertical 12x5 Junqi command board occupies roughly 64% of the width on the left. A quiet, unframed order rail occupies the right 30% with generous spacing and no nested cards. The implementation still has to preserve this relationship at the 800x600dp baseline.

## Modules

- Classic board preview: low-saturation straw-gold surface, dark ink roads, dark/ivory sleeper railways, labeled rectangular stations, oval camps, red headquarters and three separate striped center bridges.
- Clean preview state: no pieces on the menu board, so the exact road, railway, camp, headquarters and bridge topology remains the focal point.
- Brand block: existing circular package Logo, exact title `军棋`, exact version `版本 0.0.8`.
- Menu: exact labels `单人模式`, `双人对战`, `退出游戏`; primary action solid grey-green, secondary outlined, exit red outlined.

## Visual details

Use exact palette from SSOT: canvas `#E8EDE9`, surface `#F6F7F3`, board `#D8C98F`, board light `#E9DCAA`, frame `#68745D`, road `#494233`, rail dark `#2D2A23`, rail light `#F1E5B9`, ink `#292B23`, green piece `#23704B`, orange piece `#C65012`, piece text `#FFFFFF`, headquarters `#B4312B`, camp `#BBC2A6`. The board is a refined digital reconstruction of the provided classic Junqi reference, not a generic parchment texture. Right rail is flat and quiet. Chinese typography uses restrained serif for title and clean sans serif for controls.

## Image prompt

Use case: style-transfer
Asset type: high-fidelity Android tablet game menu design preview
Input images: `build/runtime-acceptance/menu.png` is the geometry and content reference; `designs/references/junqi-classic-board-reference.jpg` is the board form and material reference. Preserve the runtime screenshot's exact graph, layout, Logo placement and text; borrow only the traditional visual language from the classic board.
Primary request: Restyle the existing main menu of a two-player hidden-information Chinese Junqi board game for an offline Android Pad without changing gameplay geometry or text.
Scene/backdrop: wide landscape Android app screenshot matching the runtime source aspect ratio, bright cold mineral-grey outer canvas, no device frame.
Subject: The runtime source's complete empty 12-by-5 Junqi board fills the left 64 percent. Preserve every road and rail connection. Restyle it after the classic reference: low-saturation straw-gold fiberboard, thin dark-ink road lines, highly legible alternating dark-and-ivory railway sleepers, rectangular stations labeled "兵站", oval camps labeled "行营", rectangular headquarters labeled in red "大本营", and three separate striped center bridges. Do not add pieces. On the right is a flat unframed order rail with the existing round red-versus-blue Junqi Logo, large exact Chinese title "军棋", exact smaller text "版本 0.0.8", and three large buttons with exact labels "单人模式", "双人对战", "退出游戏".
Style/medium: polished production UI mockup, respectful modern reconstruction of a traditional Chinese Junqi railway board, subtle fiberboard grain, crisp print-like labels, precise Android Compose proportions.
Composition/framing: orthographic front view, board and right rail fully visible, no scrolling, no overlapping text, 800x600dp-equivalent spacing, system status and navigation bars may be omitted.
Lighting/mood: soft diffuse studio light, subtle material depth, no dramatic shadows.
Color palette: exact #E8EDE9, #F6F7F3, #D8C98F, #E9DCAA, #68745D, #494233, #2D2A23, #F1E5B9, #292B23, #23704B, #C65012, #FFFFFF, #B4312B, #BBC2A6.
Text (verbatim): "军棋", "版本 0.0.8", "单人模式", "双人对战", "退出游戏".
Constraints: preserve the exact board graph and all source text; no pieces; title in restrained Chinese serif, controls in clean Chinese sans serif, zero letter spacing, buttons no more than 8px corner radius, one solid primary button, no cards inside cards.
Avoid: copying the reference watermark, low-resolution blur, orange vintage cast, camouflage, guns, bullets, explosions, battle scenes, neon HUD, black full-screen theme, blue-orange SaaS UI, glassmorphism, gradients, floating cards, pill chips, decorative numbers, English labels, illegible Chinese, watermarks.
