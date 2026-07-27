# doushouqi-ui - game tablet

**Preview file:** `designs/previews/doushouqi-ui-game-tablet.png`
**Spec SSOT:** `designs/specs/doushouqi-ui.md`

## Layout

Android tablet landscape viewport, approximately 16:10. The user-provided 1568×1003 reference is the approved visual target. Use the project-family 28dp outer padding and 34dp board/rail gap. The left pane contains a near-square complete 7-by-9 active board; the 300dp right rail keeps the other games' status hierarchy and controls rather than copying reference-specific rail spacing.

## Modules

- Active board: near-square bamboo frame and exact standard initial Dou Shou Qi deployment with eight pine-green and eight vermilion pieces labeled `象`, `狮`, `虎`, `豹`, `狼`, `狗`, `猫`, `鼠`.
- Terrain: two mineral-blue rivers, six traps, two dens, clear grid and crossing cues.
- Move feedback: selected piece inner border, one legal dot, one capture ring, one bright-blue four-corner latest-move marker.
- Score/status rail: exact Chinese labels `单人模式`, `智能等级 1`, `玩家 0 : 0 智能`, `松绿方回合`, `玩家执松绿`.
- Actions: exact labels `悔棋`, `返回菜单`.

## Visual details

Use the SSOT semantic colors sampled from the reference. The board has tactile bamboo grain, rounded wood frame, dark brown grid intersections, crossed trap lines with center pins, and deep textured blue rivers. Pieces are rounded square embossed tiles with inner rim, top highlight, lower shadow, and large off-white brush-style Chinese characters. The right rail stays flat and family-consistent. Preserve an unobstructed view of terrain around each piece.

## Image prompt

Use case: ui-mockup
Asset type: Android Pad landscape in-game design preview
Primary request: Reproduce the approved user reference as a polished high-fidelity landscape tablet UI for an active Chinese Dou Shou Qi match.
Scene/backdrop: full-screen cool mineral gray-green canvas, no device frame.
Subject: left side shows a complete 7 by 9 Dou Shou Qi board in a plausible standard opening position, with exactly eight pine-green and eight vermilion rounded rectangular animal tiles labeled with large single Chinese characters from "象 狮 虎 豹 狼 狗 猫 鼠"; two deep mineral-blue 2 by 3 rivers, six traps and two dens remain readable. Right side is a calm flat score ledger with exact text "单人模式", "智能等级 1", "玩家 0 : 0 智能", "松绿方回合", "玩家执松绿", and buttons "悔棋", "返回菜单".
Style/medium: production-ready Jetpack Compose tablet game UI using package-owned raster board and piece textures, tactile bamboo grain, embossed colored tiles, brush-style animal labels, precise accessible geometry.
Composition/framing: 16:10 landscape, 28dp outer padding, board centered in left pane, 34dp gap, narrow 300dp right rail, complete board visible.
Color palette: canvas #EDF3EF, rail #F7F8F3, board #E5B85D, grid #5A3A12, river #075D86, green pieces #0E5A3A, red pieces #C63A20, piece text #FFF3D2, latest move #4FCBFF.
Text (verbatim): "象", "狮", "虎", "豹", "狼", "狗", "猫", "鼠", "单人模式", "智能等级 1", "玩家 0 : 0 智能", "松绿方回合", "玩家执松绿", "悔棋", "返回菜单".
Constraints: board exactly 7 columns by 9 rows; both rivers exactly 2 by 3; large readable animal characters; terrain remains visible; show one bright-blue four-corner last-move marker with a clear center; no extra panels.
Avoid: dark esports HUD, neon, photorealistic animals, cute cartoons, jungle wallpaper, glass cards, clutter, emojis, English text, watermark, device frame, screenshot-cropped static board.
