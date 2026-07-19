# Design Brief - Android Pad Xiangqi UI

**Slug:** `xiangqi-ui`
**User brief (verbatim summary):** "设计 象棋 游戏界面偏向明亮、简洁、中式，每个棋子和完整棋盘需要单独一个 png 图片保存用于做贴图"
**Stack:** Kotlin, Jetpack Compose, Material 3, offline Android Pad game package
**Iteration:** 2026-07-19T19:30:00+08:00

## Base System (Step 1 - ui-ux-pro-max)

| Dimension | Content |
| --- | --- |
| Product / industry | Offline Xiangqi for a shared Android Pad, supporting single-player robot play and local two-player play. |
| Page structure | Landscape-only package with two views: a complete-board main menu and an active board with a fixed right score/action rail. |
| Color tokens | Bright porcelain white, pale celadon, ink black, cinnabar red, quiet brass, and shared latest-move blue. |
| Typography | Android system serif for the title, river, and piece labels; Android system sans / Noto Sans SC fallback for score, status, and controls. |
| Interaction | Every intersection keeps a full touch target; selection, latest move, check, terminal result, and disabled actions remain visually distinct. |
| Anti-patterns (avoid) | No dark gaming lobby, brown wood-dominant scene, scroll painting, ornate palace frame, glass blur, card nesting, embossed control chrome, tiny buttons, or code-drawn pieces in the final board. |

### Base layout

```text
+---------------------------------------------------+-------------------+
|                                                   | mode / title      |
|                                                   | score             |
|             complete 9 x 10 board                 | AI level          |
|                                                   | turn / check      |
|                                                   |                   |
|                                                   | undo              |
|                                                   | restart / exit    |
+---------------------------------------------------+-------------------+
```

## Explored Directions

| Direction | Character | Trade-off |
| --- | --- | --- |
| **A. 晴光瓷局 (selected)** | Cool-white porcelain board, pale celadon rim, ink grid, matte ivory pieces with cinnabar or blue-black labels. | Distinctive and bright while still recognizably Chinese; requires disciplined texture generation to avoid looking clinical. |
| B. 浅竹棋桌 | Pale bamboo board, dark burned grid, traditional wooden discs. | Immediately familiar, but too close to common Xiangqi apps and easily becomes warm brown-dominant. |
| C. 宣纸墨局 | Flat rice-paper board, brush grid, seal-red pieces. | Graphic and elegant, but brush irregularity reduces coordinate precision and piece readability at tablet distance. |

## Revised Direction (Step 2 - frontend-design)

### Subject grounding

- **Concrete subject:** a daylight Xiangqi table in a contemporary Chinese reading room, reduced to porcelain, ink, celadon, and cinnabar.
- **Audience:** family members and friends sharing an Android Pad, including players viewing the board from arm's length.
- **Single job:** read the position and make a move without losing track of turn, check, score, or round controls.
- **Subject language:** circular Chinese chess pieces, palace diagonals, river division, seal-red labels, celadon edge glaze, and orderly score-sheet typography.

### Memory point (signature)

**The celadon river band.** A very pale blue-green river crossing carries the paired `楚河` and `漢界` labels inside the complete board texture. It is the only expressive surface; all surrounding UI remains quiet.

### Aesthetic risk

The design replaces the expected yellow-brown wood with a thin white-porcelain playing slab and celadon edge. This keeps the board bright and distinctly Chinese without relying on ornamental borders or antique simulation.

### Detemplating changes

- Rejected the generated dark felt-green and gold board-game palette because it contradicts the requested bright surface and repeats a generic gaming look.
- Replaced the generic single-column CTA layout with the actual board-first two-screen workflow.
- Kept tactile material only in the board and piece PNGs; the backdrop and rail remain flat and quiet.
- Restricted serif typography to the title, river, and piece glyphs so gameplay data remains quick to scan.
- Concentrated visual identity in one celadon river band instead of spreading cloud motifs, seals, or decorative lines across the UI.

### Rejected defaults

- No warm cream and terracotta editorial composition.
- No near-black lobby with acid-green or neon red accents.
- No broadsheet columns, ornamental numbering, scroll painting, palace roof, dragon, cloud, or lattice decoration.
- No nested cards, floating board card, glass blur, large gradients, or decorative shadows.
- No fake calligraphy for buttons or status text.

### De-templating critique

| Question | Resolution |
| --- | --- |
| Is the subject and audience concrete? | Yes. A contemporary daylight Xiangqi table on a shared Android Pad. |
| Does the screen have one primary job? | Yes. Read the position and make a legal move. |
| Do palette, type, and layout come from the subject? | Yes. Porcelain, celadon, ink, cinnabar, circular pieces, river, and score-sheet order all come from Xiangqi and Chinese material culture. |
| Is the opening a thesis rather than a template? | Yes. The main menu opens on the complete playable board rather than a hero or feature cards. |
| Is typography specific rather than generic? | Yes. Song-style serif is confined to Chinese chess identity; system sans carries operational information. |
| Do structural elements encode information? | Yes. The river divides sides; the rail order follows score, level, turn/check, and actions. |
| What is the one memory point? | The pale celadon river band crossing the porcelain board. |
| Where is the aesthetic risk? | Replacing traditional wood with porcelain and glaze while preserving exact Xiangqi geometry. |
| Is boldness concentrated? | Yes. The board and pieces carry the identity; backdrop, rail, and controls remain restrained. |
| Does copy use player language? | Yes. No engine, package, search, or plugin terminology appears on screen. |

## Final Token Table

| Token | Value | Role |
| --- | --- | --- |
| `canvas` | `#EAF1EF` | Bright mineral-celadon background |
| `canvasLine` | `#D3E0DC` | Sparse backdrop rule |
| `board.surface` | `#F5F1E8` | Cool ivory porcelain board |
| `board.rim` | `#A9C7BE` | Pale celadon glazed rim |
| `board.rimEdge` | `#6E9489` | Thin outer edge and board registration |
| `board.grid` | `#263C38` | Ink-green grid and palace lines |
| `board.river` | `#D9E9E4` | Signature river band |
| `rail.surface` | `#FBFCFA` | Matte score-sheet rail |
| `rail.outline` | `#B8CCC6` | Rail and control borders |
| `ink.primary` | `#213431` | Primary interface text |
| `ink.muted` | `#61736F` | Supporting text |
| `piece.body` | `#F3E7CE` | Matte ivory piece body |
| `piece.edge` | `#B89B6D` | Quiet brass-tan piece edge |
| `side.red` | `#B83A32` | Red labels, active red side, primary action |
| `side.black` | `#263C42` | Blue-black labels and active black side |
| `accent.latest` | `#4FCBFF` at 72% | Shared latest-move four-corner marker |
| `feedback.check` | `#B82F27` | Check state |
| `feedback.focus` | `#08758A` | Accessibility focus |

### Typography

| Role | Family | Size / weight | Use |
| --- | --- | --- | --- |
| Game title | Android system serif | 42sp / 700 | `象棋` on the menu |
| River and piece label | Android system serif / Song-style glyph embedded in PNG | Asset-relative | `楚河`, `漢界`, and piece identities |
| Score | Android system sans, tabular figures | 46sp / 400 | Round score |
| Rail heading | Android system sans | 18sp / 700 | Mode and identity |
| Status | Android system sans | 18sp / 600 | Turn, side, check, result |
| Button | Android system sans | 16sp / 600 | Visible commands |
| Utility | Android system sans | 12sp / 500 | Version and secondary labels |

## Layout Concept

The board is the dominant unframed play surface; a single full-height score-sheet rail presents information and commands without cards inside cards.

### Main menu

```text
+---------------------------------------------------+-------------------+
|                                                   | 象棋              |
|                                                   | 楚河 · 漢界       |
|              complete preview board               | 版本 0.0.x        |
|              with PNG pieces                      |                   |
|                                                   | [单人模式]        |
|                                                   | [双人对战]        |
|                                                   | [退出游戏]        |
+---------------------------------------------------+-------------------+
```

### Game view

```text
+---------------------------------------------------+-------------------+
|                                                   | 玩家 : 智能       |
|                                                   |     0 : 0         |
|                    live board                     | 智能等级 1        |
|                                                   |-------------------|
|                                                   | 当前回合：红方    |
|                                                   | 将军              |
|                                                   | [悔棋] [退出游戏] |
+---------------------------------------------------+-------------------+
```

### Responsive rules

- Target landscape tablets from 800 x 600 dp upward.
- At widths >= 900 dp, board and rail stay side by side; the board keeps its 0.90 width/height ratio.
- The rail is 288-312 dp wide and uses full available height rather than floating as a card.
- At compact widths, board appears first and the rail becomes a full-width band below it.
- Every intersection uses the full grid step as its touch target; no visual asset changes hit geometry.
- Text wraps before reducing below the specified minimum size.

## Board Asset System

- Save one complete board PNG at `games/xiangqi/package/assets/board/xiangqi-board.png`.
- Final size: 1440 x 1600 RGBA PNG with transparent corners outside the thin board silhouette.
- The PNG owns the complete grid, palace diagonals, position marks, river band, `楚河` / `漢界`, rim, and surface material.
- Preserve interaction registration:
  - grid left: `192 / 1440`
  - grid top: `190 / 1600`
  - grid right: `1248 / 1440`
  - grid bottom: `1378 / 1600`
- Camera is perfectly orthographic and front-facing. No perspective, board rotation, pieces, hands, room, table, text outside the river labels, watermark, or cast shadow.
- Surface texture is subtle enough that every grid line remains crisp at tablet resolution.

## Piece Asset System

- Save 14 separate PNG files under `games/xiangqi/package/assets/pieces/`.
- Red files: `red-general.png`, `red-rook.png`, `red-horse.png`, `red-cannon.png`, `red-elephant.png`, `red-advisor.png`, `red-soldier.png`.
- Black files: `black-general.png`, `black-rook.png`, `black-horse.png`, `black-cannon.png`, `black-elephant.png`, `black-advisor.png`, `black-soldier.png`.
- Each final file is 1024 x 1024 RGBA PNG with transparent corners and 10-12% transparent padding.
- Every file contains exactly one centered circular piece in the same orthographic top view, diameter, rim geometry, light direction, and crop.
- Piece body is matte ivory with a restrained brass-tan edge. Red glyphs use cinnabar; Black glyphs use blue-black ink.
- Embedded glyph mapping:
  - Red: `帥`, `俥`, `傌`, `炮`, `相`, `仕`, `兵`
  - Black: `將`, `車`, `馬`, `砲`, `象`, `士`, `卒`
- Glyphs must be upright, centered, legible, and use a consistent Song-style engraved/inked treatment.
- No board, background, floor plane, external shadow, extra symbol, English, watermark, bevel-heavy 3D, or decorative motif.
- Compose renders with `ContentScale.Fit`; selection and latest-move overlays remain code-drawn above the texture.

## Board State Language

- **Selected piece:** 3 dp celadon-blue circular outline with a visible gap around the piece.
- **Latest move:** shared translucent bright-blue four-corner frame with a clear center.
- **Check:** cinnabar outline around the checked general plus `将军` in the rail.
- **Current turn:** `当前回合：` remains neutral; `红方` or `黑方` uses the corresponding side color plus a light outline.
- **Terminal result:** score rail switches from turn information to exact result and exposes `重新开始`.
- State treatments never resize a cell, piece, board, or rail.

## Interaction And Motion

- Tap feedback appears within 100 ms through a restrained state layer.
- Board input is disabled only while rules or robot turn require it.
- Robot thinking changes status text and may show one small progress indicator in the rail; no board-wide animation.
- Optional move transition is 140-180 ms; reduced-motion mode uses direct replacement.
- Undo, restart, and exit controls remain at least 48 x 48 dp with at least 8 dp separation.
- Turn, check, result, and interactive board coordinates remain available to Compose semantics.

## Copy Tone

- **Register:** calm, exact, contemporary Chinese.
- **Visible vocabulary:** 象棋, 楚河, 漢界, 单人模式, 双人对战, 退出游戏, 玩家, 智能, 智能等级, 当前回合, 红方, 黑方, 将军, 悔棋, 重新开始.
- **Robot thinking:** `智能思考中 · 等级 N`.
- **Terminal result:** use the exact result rather than a generic success or failure phrase.

## Preview Index

| Preview | Prompt doc | Description |
| --- | --- | --- |
| `designs/previews/xiangqi-ui-menu.png` | `designs/images/xiangqi-ui-menu.md` | Landscape main menu with the complete porcelain board and mode rail. |
| `designs/previews/xiangqi-ui-game.png` | `designs/images/xiangqi-ui-game.md` | Active single-player game with board state, score, turn, and actions. |

## Implementation Notes

- Primary interaction: tap a board intersection to select and move.
- Components: `XiangqiBackdrop` -> `XiangqiBoard` -> `XiangqiPieceImage` -> state overlays -> `XiangqiInfoRail` -> action controls.
- Replace all 14 existing piece PNGs and the complete board PNG as one visual set; do not mix old and new materials.
- Keep rules, AI, score, undo, restart, board rotation, coordinate mapping, and latest-move geometry unchanged.
- Do not change `game-api`, the shell, package loader, external distribution behavior, or other game packages.
