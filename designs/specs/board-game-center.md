# Design Brief - Board Game Center

**Slug:** `board-game-center`
**User brief (verbatim summary):** Reference `desgins/AI_CODING_HANDOFF.md` and redesign the Android Pad offline game-center interface.
**Stack:** Kotlin, Jetpack Compose, Material 3.
**Iteration:** 2026-07-14

## Base System

| Dimension | Direction |
| --- | --- |
| Product / industry | Offline Android tablet game center for classic Chinese board games: Gomoku, Othello, and Xiangqi. |
| Page structure | A calm game-library home; an immersive game-specific mode selection screen; a landscape board and narrow score rail in play. |
| Color tokens | Initial research suggested felt green, gold, and dark wood. The revised token table below supersedes this for the shipped direction. |
| Typography | Noto Sans SC is the dependable UI face; an Android-available Songti-family fallback is reserved for game titles and board labels. |
| Interaction | All primary targets are at least 48dp; cards and buttons gain a pressed state without reflow; board pieces have a short placement response; nonessential movement is reduced with Android motion settings. |
| Anti-patterns (avoid) | Do not use a marketplace layout, login, live-service chrome, bottom tabs, deep dashboards, decorative 3D scenes, hover-only actions, tiny precision targets, or a generic neon-on-black arcade look. |

## Revised Direction

### Subject grounding

The concrete subject is a small offline collection of familiar tabletop games, played on a shared Android Pad by family members or friends. The single job of the home screen is to make choosing a game feel as immediate as pulling a board from a game cabinet; the single job of a match screen is to keep the board legible and the turn state unmistakable.

### Memory point (signature)

**The cabinet shelf:** home presents each game as a distinct physical board resting in a shallow, slate-edged cabinet slot. The boards themselves, not generic app cards, form the game selection affordance. One row of slim inlaid markers under the header signals the three installed games without adding navigation.

### Aesthetic risk

The main surface is cool mineral blue-grey rather than the expected warm paper or dark felt. This makes the wood, ink, jade-white stones, and Xiangqi cinnabar feel like intentional game objects, not a themed productivity app. It is a restrained risk: texture is low-contrast and never competes with labels or board geometry.

### Detemplating changes

The research-proposed dark felt-and-gold default is replaced with a daylight cabinet palette: mineral grey background, charcoal ink text, lacquer-red only for destructive or red-side states, and individual board material colors. The product is no longer arranged as a flat three-card web grid; it becomes a library shelf with one strong action in the header. Type avoids a generic full-serif editorial look: only the nameplate-style game titles use Songti, while all controls retain the native-readable Noto Sans SC system stack.

### Rejected defaults

- Rejected the warm cream, terracotta, high-contrast serif cluster: wood appears only on the game objects.
- Rejected a near-black plus acid accent gaming skin: the center is an inviting offline tabletop, not an arcade lobby.
- Rejected broadsheet columns and decorative numbered sections: installation count and board size are useful metadata, but not visual ornament.

### De-templating critique

| Question | Resolution |
| --- | --- |
| Does the palette come from the subject? | Yes. Slate cabinet backing separates the device from wood boards, black ink, jade-white stones, green Othello felt, and cinnabar Xiangqi pieces. |
| Is the opening a thesis, not a template hero? | Yes. The first viewport is a playable-looking cabinet shelf, not marketing copy, statistics, or a gradient hero. |
| Is typography specific without harming legibility? | Yes. Songti is limited to game identities and board labels; Noto Sans SC handles controls, scores, and package metadata. |
| Does structure express real information? | Yes. The three shelf slots are the three installed local packages, and the slim inlay marks their physical count. |
| Is boldness concentrated? | Yes. The cabinet shelf is the sole signature; controls, rails, and menus stay quiet. |
| Is copy user-facing and active? | Yes. Actions say “导入游戏包”, “开始单人对局”, “开始双人对局”, and “退出游戏”; import failures state the accepted format and next action. |

## Final Tokens

| Token | Value | Use |
| --- | --- | --- |
| `surface.canvas` | `#DCE2E2` | Mineral-grey application background |
| `surface.raised` | `#F8FAF8` | Header, information rail, menus, and cards |
| `surface.inset` | `#B8C3C4` | Cabinet shelf recess, dividers, disabled surfaces |
| `ink.primary` | `#1D2B2F` | Main text and board linework |
| `ink.muted` | `#5D6E72` | Metadata and supporting labels |
| `action.primary` | `#164C5B` | Import and primary mode actions |
| `action.success` | `#2E715E` | Ready-to-play state |
| `action.danger` | `#A43B32` | Exit action and Xiangqi red-side accent |
| `board.wood` | `#C8985F` | Gomoku and Xiangqi playing surfaces |
| `board.wood-dark` | `#593F27` | Wood frame and inlay shadow |
| `board.felt` | `#245E52` | Othello board surface |
| `stone.jade` | `#F1F2E8` | Light Gomoku and Othello stone |
| `focus.ring` | `#0E7490` | Visible keyboard and accessibility focus ring |

### Type Roles

| Role | Family | Size / use |
| --- | --- | --- |
| Game identity | `Noto Serif SC`, `Songti SC`, `STSong`, serif | 40sp menu title, 30sp home game title, 22sp board labels |
| Interface | `Noto Sans SC`, `Roboto`, sans-serif | 16sp body, 14sp metadata, 18sp buttons |
| Score / turn | `Roboto Mono`, `Roboto`, sans-serif | 32sp score, 16sp turn status; tabular figures where possible |

## Layout Concept

The app moves from **cabinet shelf** to **game mat**: selection has a shallow three-slot library composition; play clears all nonessential chrome and lets one board occupy the dominant left field with a quiet score rail on the right.

```text
TABLET HOME
+------------------------------------------------------------------------+
| [棋局]  游戏中心              ─ ─ ─ installed inlay      [导入游戏包] |
|                                                                        |
|  本地棋局                                          3 个本地游戏      |
|  +----------------+  +----------------+  +----------------+          |
|  |  五子棋 board  |  |   象棋 board   |  |  黑白棋 board  |          |
|  |  status + meta |  |  status + meta |  |  status + meta |          |
|  +----------------+  +----------------+  +----------------+          |
+------------------------------------------------------------------------+

TABLET MATCH
+------------------------------------------------------+-----------------+
|                                                      | 历史比分        |
|           game board / pieces                        | 0 : 0           |
|                                                      |                 |
|                                                      | 当前回合        |
|                                                      | 你的回合 · 执黑 |
|                                                      |                 |
|                                                      | [退出游戏]      |
+------------------------------------------------------+-----------------+
```

### Responsive Rules

- `>= 900dp` available game width: match layout follows the Xiangqi reference geometry with 28dp outer padding, a 34dp board/rail gap, a centered aspect-preserving board, and a 300dp information rail at 94% content height. Menu rails use 320dp width at 88% content height.
- `600dp–1023dp`: two columns then one; the import action becomes a compact icon-plus-label button only when its label still fits.
- `< 600dp` or portrait: library uses one column; game info rail drops below the board as three equal semantic regions, with the exit button on a separate row.
- Never scale text by viewport width. Use the type roles above and allow wrapping where needed.

## Motion and Interaction

- Shelf item press: 0.98 scale and a 150ms inset-shadow change; no layout shift.
- Menu entry: one 220ms opacity/translation transition, then static.
- Piece placement: 160ms scale-and-settle only on the placed piece; no animation for board lines.
- Disable nonessential animations when Android’s reduced-motion setting is active.
- Use `Modifier.semantics` for game cards, package status, board coordinates, and exit action. Touch surfaces remain at least 48dp with 8dp separation.

## Copy Tone

- **Register:** quiet, familiar, direct.
- **Vocabulary:** game names, modes, turn, score, local package; no shell, plugin, dex, or other implementation terms in visible UI.
- **Empty/import state:** “未选择文件。请选择本地 .zip 游戏包。”
- **Error state:** “导入失败：当前只支持 zip 游戏包。请选择一个 .zip 文件后重试。”

## Screen Rules

### Home

- Header contains only the identity, installed inlay marker, and “导入游戏包”.
- Three shelf slots each display its game as a tangible mini-board, its launchable state, Chinese name, version, and board dimensions.
- No package-status sidebar, promo hero, bottom navigation, or dashboard widgets.

### Game Menu

- Full-screen centered composition: one material-specific game mark, the game name, “开始单人对局”, “开始双人对局”, and “退出游戏”.
- The background suggests the material around that board, but remains low contrast. No rules card or extra status panel.

### Game Screen

- Keep the landscape contract: board first, then a narrow rail containing only history score, current turn, and exit.
- Gomoku and Xiangqi pieces must remain centered on line intersections; Othello discs remain centered inside squares.
- Each board has an independent object language: maple board with ink lines and jade-white stones; deep green grid with bone/ebony discs; broad Xiangqi board with river gap, brass-outline round pieces, and cinnabar red side.

## Preview Index

| Preview | Spec doc | Description |
| --- | --- | --- |
| `designs/previews/board-game-center-desktop.png` | `designs/images/board-game-center-desktop.md` | Landscape tablet home showing the cabinet shelf signature. |
| `designs/previews/board-game-center-mobile.png` | `designs/images/board-game-center-mobile.md` | Compact portrait library adaptation with one board slot per row. |
| `designs/previews/board-game-center-game.png` | `designs/images/board-game-center-game.md` | Landscape Xiangqi match showing board-first hierarchy and narrow information rail. |

## Implementation Notes

- Primary CTA: `导入游戏包` on home; `开始单人对局` on a game menu.
- Build in order: shared color/type/spacing tokens; home cabinet shelf; shared mode menu scaffold; shared information rail; game-specific board skins and press states.
- Non-goals: authentication, network sync, marketplaces, rankings, settings, a bottom tab bar, dark mode, or decorative background animation.
