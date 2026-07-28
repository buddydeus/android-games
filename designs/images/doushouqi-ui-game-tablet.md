# doushouqi-ui - game tablet

**Preview file:** `designs/previews/doushouqi-ui-game-tablet.png`
**Spec SSOT:** `designs/specs/doushouqi-ui.md`

## Layout

Android tablet landscape viewport, approximately 16:10. The user-provided 1568×1003 reference is the approved visual target. Use the project-family 28dp outer padding and 34dp board/rail gap. The left pane contains a near-square complete 7-by-9 active board; the 300dp right rail keeps the other games' status hierarchy and controls rather than copying reference-specific rail spacing.

## Modules

- Active board: near-square bamboo frame and exact standard initial Dou Shou Qi deployment with eight pine-green and eight vermilion pieces labeled `象`, `狮`, `虎`, `豹`, `狼`, `狗`, `猫`, `鼠`.
- Terrain: two mineral-blue rivers, six octagonal hunting-net traps, two dens, and a clear grid.
- Move feedback: selected piece inner border, one legal dot, one capture ring, one bright-blue four-corner latest-move marker.
- Score/status rail: family-standard three-zone rail with exact Chinese labels `玩家 : 智能`, `0 : 0`, `智能等级 1`, `当前回合：绿方`, `绿方吃：鼠`, `红方吃：象`; two horizontal dividers separate score, capture summary and actions. The illustrated two-line state is the single-player completed player-plus-robot round. In two-player mode, show only the latest accepted move's one capture (`绿方吃：<兽名>` or `红方吃：<兽名>`), or immediately replace it with `无吃子` after a quiet move; never merge both players. Never show `松绿`, `朱砂`, `玩家执`, or `最近一步吃子`.
- Actions: exact labels `悔棋`, `返回菜单`.

## Visual details

Use the SSOT semantic colors sampled from the reference. The board has tactile bamboo grain, rounded wood frame, dark brown grid intersections, octagonal hunting-net trap emblems with inward hooks and center knots, and deep textured blue rivers. Pieces are rounded square embossed tiles with inner rim, top highlight, lower shadow, and large off-white brush-style Chinese characters. The right rail stays flat and family-consistent. Preserve an unobstructed view of terrain around each piece.

## Image prompt

Use case: ui-mockup
Asset type: Android Pad landscape in-game design preview
Primary request: Reproduce the approved user reference as a polished high-fidelity landscape tablet UI for an active Chinese Dou Shou Qi match.
Scene/backdrop: full-screen cool mineral gray-green canvas, no device frame.
Subject: left side shows a complete 7 by 9 Dou Shou Qi board in a plausible standard opening position, with exactly eight pine-green and eight vermilion rounded rectangular animal tiles labeled with large single Chinese characters from "象 狮 虎 豹 狼 狗 猫 鼠"; two deep mineral-blue 2 by 3 rivers, six traps marked by double-line octagonal hunting nets with eight radial strands, four inward hooks and a center knot, and two dens remain readable. Right side is a calm Xiangqi-family score ledger divided into three vertical zones by two horizontal rules: top text "玩家 : 智能", large score "0 : 0", then "智能等级 1"; middle text "当前回合：绿方", then two compact completed-round tags "绿方吃：鼠" and "红方吃：象"; bottom buttons "悔棋", "返回菜单". Do not show player-side ownership.
Style/medium: production-ready Jetpack Compose tablet game UI using package-owned raster board and piece textures, tactile bamboo grain, embossed colored tiles, brush-style animal labels, precise accessible geometry.
Composition/framing: 16:10 landscape, 28dp outer padding, board centered in left pane, 34dp gap, narrow 300dp right rail, complete board visible.
Color palette: canvas #EDF3EF, rail #F7F8F3, board #E5B85D, grid #5A3A12, river #075D86, green pieces #0E5A3A, red pieces #C63A20, piece text #FFF3D2, latest move #4FCBFF.
Text (verbatim): "象", "狮", "虎", "豹", "狼", "狗", "猫", "鼠", "玩家 : 智能", "0 : 0", "智能等级 1", "当前回合：绿方", "绿方吃：鼠", "红方吃：象", "悔棋", "返回菜单".
Constraints: board exactly 7 columns by 9 rows; both rivers exactly 2 by 3; large readable animal characters; terrain remains visible; show one bright-blue four-corner last-move marker with a clear center; no extra panels.
Avoid: the words "松绿", "朱砂", "玩家执", or "最近一步吃子"; dark esports HUD, neon, photorealistic animals, cute cartoons, jungle wallpaper, glass cards, clutter, emojis, English text, watermark, device frame, screenshot-cropped static board.
