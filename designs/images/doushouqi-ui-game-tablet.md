# doushouqi-ui - game tablet

**Preview file:** `designs/previews/doushouqi-ui-game-tablet.png`
**Spec SSOT:** `designs/specs/doushouqi-ui.md`

## Layout

Android tablet landscape viewport, approximately 16:10. Use 28dp outer padding. The left pane contains the complete 7-by-9 active board with pieces and legal-move feedback. The 300dp right rail contains mode/level, a large score line, current turn, concise status, and bottom actions.

## Modules

- Active board: standard initial Dou Shou Qi deployment with eight green and eight vermilion pieces labeled `象`, `狮`, `虎`, `豹`, `狼`, `狗`, `猫`, `鼠`.
- Terrain: two mineral-blue rivers, six traps, two dens, clear grid and crossing cues.
- Move feedback: selected piece inner border, one legal dot, one capture ring, one bright-blue four-corner latest-move marker.
- Score/status rail: exact Chinese labels `单人模式`, `智能等级 1`, `玩家 0 : 0 智能`, `松绿方回合`, `玩家执松绿`.
- Actions: exact labels `悔棋`, `返回菜单`.

## Visual details

Use the SSOT semantic colors. Pieces are slightly rounded rectangular tiles filling about 78% of a cell; labels are large off-white Black-weight Chinese characters with no outline or shadow. The river is the only bold color field. The right rail is flat and separated by two thin rules, not nested cards. Preserve an unobstructed view of terrain around each piece.

## Image prompt

Use case: ui-mockup
Asset type: Android Pad landscape in-game design preview
Primary request: Create a polished high-fidelity landscape tablet UI mockup for an active Chinese Dou Shou Qi match.
Scene/backdrop: full-screen cool mineral gray-green canvas, no device frame.
Subject: left side shows a complete 7 by 9 Dou Shou Qi board in a plausible standard opening position, with exactly eight pine-green and eight vermilion rounded rectangular animal tiles labeled with large single Chinese characters from "象 狮 虎 豹 狼 狗 猫 鼠"; two deep mineral-blue 2 by 3 rivers, six traps and two dens remain readable. Right side is a calm flat score ledger with exact text "单人模式", "智能等级 1", "玩家 0 : 0 智能", "松绿方回合", "玩家执松绿", and buttons "悔棋", "返回菜单".
Style/medium: production-ready Jetpack Compose tablet game UI, flat tactile bamboo board, ink-stamped animal labels, precise accessible geometry.
Composition/framing: 16:10 landscape, 28dp outer padding, board centered in left pane, 34dp gap, narrow 300dp right rail, complete board visible.
Color palette: canvas #E7ECE8, rail #F5F6F1, board #D5B875, grid #594A2F, river #315F78, green pieces #25664E, red pieces #A64332, piece text #FFF9E8, latest move #4FCBFF.
Text (verbatim): "象", "狮", "虎", "豹", "狼", "狗", "猫", "鼠", "单人模式", "智能等级 1", "玩家 0 : 0 智能", "松绿方回合", "玩家执松绿", "悔棋", "返回菜单".
Constraints: board exactly 7 columns by 9 rows; both rivers exactly 2 by 3; large readable animal characters; terrain remains visible; show one bright-blue four-corner last-move marker with a clear center; no extra panels.
Avoid: dark esports HUD, neon, photorealistic animals, cute cartoons, jungle wallpaper, glossy 3D pieces, glass cards, gradients, clutter, emojis, English text, watermark, device frame.
