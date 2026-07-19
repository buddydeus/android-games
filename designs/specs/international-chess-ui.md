# Design Brief - Android Pad International Chess UI

**Slug:** `international-chess-ui`
**User brief (verbatim summary):** "设计 国际象棋 游戏界面，每个棋子需要单独一个 png 图片保存用于做棋子贴图"
**Stack:** Kotlin, Jetpack Compose, Material 3, offline Android Pad game package
**Iteration:** 2026-07-19T17:00:00+08:00

## Base System (Step 1 - ui-ux-pro-max)

| Dimension | Content |
| --- | --- |
| Product / industry | Offline board game for a shared Android Pad; international chess with single-player robot and local two-player play. |
| Page structure | Landscape-only game package with two views: board-led main menu and active game board with a fixed right status/action rail. |
| Color tokens | Tournament green, mineral grey, porcelain white, obsidian black, restrained brass, semantic check red, and the shared latest-move blue. |
| Typography | Android system serif for the game title only; Android system sans / Noto Sans SC fallback for status, score, coordinates, and controls. |
| Interaction | Every square remains a full touch target; selection, legal move, capture, check, and latest move are visually distinct; press feedback appears within 100 ms. |
| Anti-patterns (avoid) | No neon game lobby, no ornate medieval fantasy chrome, no warm wood-dominant board, no glass blur, no card nesting, no tiny icon-only controls, and no Unicode chess glyphs in the final board. |

### Base layout

```text
LANDSCAPE ANDROID PAD
+---------------------------------------------------+-------------------+
|                                                   | mode / identity   |
|                                                   | score             |
|                  8 x 8 BOARD                      | AI level          |
|                                                   | turn / check      |
|                                                   |                   |
|                                                   | undo              |
|                                                   | restart / return  |
+---------------------------------------------------+-------------------+
```

## Revised Direction (Step 2 - frontend-design)

### Subject grounding

- **Concrete subject:** a quiet, clockless tournament chess table rather than a generic game dashboard.
- **Audience:** family members and friends sharing an Android Pad, including players who need to identify pieces and board states from arm's length.
- **Single job:** make a legal move, understand the current position, and reach round controls without leaving the board.
- **Subject language:** Staunton silhouettes, score-sheet order, tournament green, stone table edges, porcelain and obsidian pieces.

### Memory point (signature)

**The mineral tournament table.** The board sits in a cool graphite-green stone rim, while individually rendered matte Staunton pieces read as physical objects without turning the screen into a heavy 3D scene.

### Aesthetic risk

The design rejects the expected brown wooden chessboard. A cool mineral frame and muted green board make the package visually related to the game-center shell while brass selection details and physical piece materials preserve chess character.

### Detemplating changes

- Replaced the database recommendation's dark navy, bright green, and orange gaming palette with a daylight mineral palette that matches a shared family tablet.
- Kept tactile depth only inside the board rim and piece rendering; removed parallax, complex shadows, and decorative 3D interface chrome.
- Replaced a generic feature-rich page structure with the actual two-screen chess workflow.
- Concentrated character in the board and pieces; the information rail remains flat, quiet, and scan-friendly.
- Kept interface copy functional: mode, score, intelligence level, turn, check, undo, restart, and return.

### Rejected defaults

- No warm cream and terracotta editorial treatment.
- No near-black lobby with acid-green highlights.
- No broadsheet columns, ornamental numbering, heraldry, crowns, or fantasy motifs.
- No full skeuomorphism, leather panels, carved wood, glass blur, or glowing controls.
- No decorative cards around status groups.

### De-templating critique

| Question | Resolution |
| --- | --- |
| Is the subject and audience concrete? | Yes. A clockless international chess table used on a shared Android Pad. |
| Does the screen have one primary job? | Yes. Read the position and make a move; all controls are secondary. |
| Do palette and materials come from chess? | Yes. Tournament green, score-sheet white, Staunton pieces, and a restrained brass selection cue. |
| Is the opening a thesis rather than a template? | Yes. The menu opens on a complete board, not a marketing hero or feature cards. |
| Is typography specific to the use case? | Yes. Serif is limited to the title; highly legible system sans handles gameplay data. |
| Do structural elements encode information? | Yes. The rail follows mode -> score -> level -> turn/check -> actions. |
| What is the one memory point? | The cool mineral tournament table with porcelain and obsidian Staunton pieces. |
| Where is the aesthetic risk? | Replacing the expected brown wood with a cool stone-and-green material system. |
| Is boldness concentrated? | Yes. Pieces and board carry the identity; the rail and background remain restrained. |
| Does copy use player language? | Yes. No package, engine, search, or plugin terminology appears in the interface. |

## Final token table

| Token | Value | Role |
| --- | --- | --- |
| `canvas.top` | `#E6ECEA` | Upper cool mineral background |
| `canvas.bottom` | `#CDD9D6` | Lower cool mineral background |
| `board.light` | `#D8D4C6` | Light square, neutral grey ivory |
| `board.dark` | `#557161` | Muted tournament green square |
| `board.rim` | `#344842` | Graphite-green stone frame |
| `board.rimEdge` | `#1F2E2A` | Frame edge and coordinate ink |
| `rail.surface` | `#F7F9F7` | Matte score-sheet rail |
| `rail.outline` | `#AAB8B4` | Rail and control borders |
| `ink.primary` | `#1D2927` | Main text |
| `ink.muted` | `#5B6966` | Supporting text |
| `piece.white` | `#F2EEE2` | Matte ivory porcelain pieces |
| `piece.black` | `#202826` | Soft-black obsidian pieces |
| `accent.brass` | `#B38A45` | Selection and capture ring |
| `accent.latest` | `#4FCBFF` at 72% | Shared latest-move four-corner marker |
| `feedback.check` | `#B0443D` | Check state and losing-side warning |
| `feedback.focus` | `#08758A` | Keyboard/accessibility focus ring |

### Typography

| Role | Family | Size / weight | Use |
| --- | --- | --- | --- |
| Game title | Android system serif | 38sp / 700 | "国际象棋" on the menu |
| Score | Android system sans, tabular figures | 44sp / 400 | Round score |
| Rail heading | Android system sans | 18sp / 700 | Mode and identity |
| Status | Android system sans | 17sp / 600 | Turn, side, check, result |
| Button | Android system sans | 16sp / 600 | Visible commands |
| Board coordinate | Android system sans | 10sp / 600 | Files and ranks |

## Layout concept

The board is the primary unframed work surface; a single full-height score-sheet rail holds all secondary information and commands without nested cards.

### Main menu

```text
+---------------------------------------------------+-------------------+
|                                                   | 国际象棋          |
|                                                   | CLASSICAL BOARD   |
|             complete preview board                | 版本 0.0.x        |
|             with PNG pieces                       |                   |
|                                                   | [单人模式]        |
|                                                   | [双人对战]        |
|                                                   | [退出游戏]        |
+---------------------------------------------------+-------------------+
```

### Game view

```text
+---------------------------------------------------+-------------------+
|                                                   | 玩家 : 智能       |
|                                                   |      0 : 0        |
|                    live board                     | 智能等级 1        |
|                                                   |-------------------|
|                                                   | 当前回合: 白方    |
|                                                   | 你的回合 - 执白   |
|                                                   |                   |
|                                                   | [悔棋] [返回菜单] |
+---------------------------------------------------+-------------------+
```

### Responsive rules

- Target landscape tablets from 800 x 600 dp upward.
- At widths >= 700 dp, board and rail stay side by side; the board is a stable square and never stretches.
- The rail is 288-320 dp wide and uses full height rather than a floating card.
- At compact or portrait sizes, board appears first and the rail becomes a full-width band below it.
- Board squares remain equal and provide the full square as the touch target.
- Text must wrap before reducing below the specified minimum sizes.

## Board and state language

- **Selected piece:** 4 dp brass square outline.
- **Legal empty destination:** centered graphite dot at 42% opacity.
- **Legal capture:** brass circular ring around the target piece with a visible gap.
- **Latest move:** shared translucent bright-blue four-corner frame, center clear.
- **Check:** 4 dp red square outline around the checked king plus `将军` in the rail.
- **Focus:** 3 dp blue-green focus ring outside the selected square treatment.
- State treatments never resize a square or piece.

## Piece asset system

- Save 12 separate PNG files under `games/chess/package/assets/pieces/`.
- File names:
  - `white-king.png`, `white-queen.png`, `white-rook.png`, `white-bishop.png`, `white-knight.png`, `white-pawn.png`
  - `black-king.png`, `black-queen.png`, `black-rook.png`, `black-bishop.png`, `black-knight.png`, `black-pawn.png`
- Each final file is 1024 x 1024 RGBA PNG with transparent corners.
- Use the same centered, slightly elevated front/top three-quarter camera angle, base ellipse, key light, and crop.
- White pieces use matte ivory porcelain with a graphite edge; Black pieces use soft-black obsidian with a restrained cool rim light.
- No board, floor plane, cast shadow, text, letters, numbers, crown ornament beyond the conventional Staunton king cross, or watermark.
- Keep 10% transparent padding and reserve enough internal space so selection/capture rings remain visible.
- Compose must render assets with `ContentScale.Fit`; never crop piece tops or bases.

## Interaction and motion

- Tap feedback appears within 100 ms using a restrained state layer; no bounce or elastic deformation.
- Moving pieces may crossfade or translate within 140-180 ms, but board state updates cannot wait for animation.
- Robot thinking changes only status text and a small progress indicator in the rail; the board remains tappable only when rules allow.
- Reduced-motion mode removes piece translation and uses a direct crossfade.
- Undo, restart, and return controls remain at least 48 x 48 dp with at least 8 dp separation.
- Dynamic turn/check changes must be announced through Compose semantics.

## Copy tone

- **Register:** quiet, exact, tournament-like.
- **Visible vocabulary:** 国际象棋, 单人模式, 双人对战, 退出游戏, 玩家, 智能, 当前回合, 白方, 黑方, 将军, 悔棋, 重新开始, 返回菜单.
- **Robot thinking:** "智能思考中 - 等级 N".
- **No-move result:** use the exact rule result rather than a generic failure message.

## Preview index

| Preview | Prompt doc | Description |
| --- | --- | --- |
| `designs/previews/international-chess-ui-menu.png` | `designs/images/international-chess-ui-menu.md` | Landscape game main menu with the complete board and mode rail. |
| `designs/previews/international-chess-ui-game.png` | `designs/images/international-chess-ui-game.md` | Active single-player game with board states and action rail. |

## Implementation notes (Step 4 handoff)

- Primary interaction: tap a board square to select and move.
- Components: `ChessBackdrop` -> `ChessBoard` -> `ChessPieceImage` -> state overlays -> `ChessInfoRail` -> action controls.
- Replace Unicode glyph rendering only after all 12 package assets pass alpha, dimensions, and package verification.
- Keep game logic, AI, score, undo, restart, and board coordinate mapping unchanged.
- Do not change `game-api`, the shell, package loader, or external distribution behavior.
