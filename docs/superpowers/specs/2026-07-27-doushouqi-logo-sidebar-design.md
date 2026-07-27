# Doushouqi Logo And Sidebar Design

**Status:** Approved, implemented, and verified on Android 36.

## Goal

Replace the Doushouqi package Logo with the user-selected elephant-versus-rat medallion on a transparent background, and align both the menu and active-game side rails with the established Xiangqi-family layouts without changing Doushouqi rules or board interaction.

## Approved Logo

The approved source is the user-provided square artwork containing:

- one cool mineral-grey circular porcelain rim;
- an ivory board surface with restrained gold grid incisions;
- one glossy S-shaped mineral-blue river;
- one pine-green embossed `象` tile at upper left;
- one cinnabar embossed `鼠` tile at lower right.

The `象` and `鼠` glyphs, S-river geometry, piece placement, material lighting, proportions, and rim must not be reinterpreted. Only the exterior white background is removed.

The production asset is a `1024 × 1024` RGBA PNG at `games/doushouqi/package/assets/icon.png`. All four corners must have alpha `0`; the medallion remains centered, circular-safe, and fully visible. The transparent edge may retain a narrow antialiased transition but must contain no white or chroma-key halo.

## Menu Sidebar

Use the same wide-screen structure as the existing game family:

- `320dp` fixed width;
- `88%` content height;
- `28dp` outer screen padding and `34dp` board-to-rail gap;
- light mineral surface, `8dp` corner radius, subtle border and restrained shadow;
- centered package Logo at `112dp`;
- centered title `斗兽棋`;
- centered package version label;
- three full-width `54dp` actions with `10dp` vertical spacing.

Action order and labels remain:

1. `单人模式`
2. `双人对战`
3. `退出游戏`

The first two buttons use the family-standard neutral outlined treatment. `退出游戏` uses a restrained cinnabar outline and text. No button uses the Logo’s blue river as an action color.

## Active-Game Sidebar

Use the same information hierarchy as Xiangqi and International Chess:

- `300dp` fixed width;
- `94%` content height;
- the same surface, border, radius, shadow, and horizontal padding as the menu rail;
- vertical `Arrangement.SpaceBetween`;
- top score block;
- first horizontal divider;
- middle turn/result block;
- second horizontal divider;
- bottom full-width action block.

### Score block

- Single player: `玩家 : 智能`
- Two player: `松绿方 : 朱砂方`
- Score uses the family-standard large tabular figures.
- Single player displays `智能等级 N` directly below the score.

### Turn and result block

- Nonterminal state displays `当前回合：` followed by a compact tinted side label.
- The label contains the simplified round-side copy `绿方` or `红方`; the border and pale fill supplement the text instead of replacing it.
- Single player displays `玩家执松绿` or `玩家执朱砂` as secondary text.
- Robot turn displays `智能思考中` below the current-turn line.
- Terminal state replaces the turn block with `对局结果` plus the existing explicit result reason.

### Actions

- Terminal state shows `重新开始` as the primary filled action.
- Nonterminal and draw states expose `悔棋` according to the existing visibility and availability rules.
- The final outlined action is `返回菜单`.
- Each action is `54dp` high with at least `8dp` separation.

## Responsive Behavior

Below `900dp` available width, the rail becomes a full-width band below the complete board. Content keeps the same three semantic groups and divider order; controls may form a wrapping row only when necessary. No board cell, piece, river, trap, or den is cropped to preserve the rail.

## Accessibility

- Every action has at least a `48dp` touch target.
- Normal text contrast is at least `4.5:1`; large score text is at least `3:1`.
- Side identity always combines text and color.
- The transparent Logo retains the semantic description `斗兽棋图标`.
- Focus indication remains visible for keyboard and D-pad navigation.

## Preserve

- Standard 7×9 rules and coordinates.
- Package-owned board and piece textures.
- Six octagonal hunting-net trap emblems.
- Score, undo, restart, side-swap, AI, latest-move, and rotation behavior.
- Shell package-agnostic Logo loading.

## Verification

- Asset tests assert exact PNG type, `1024 × 1024` dimensions, RGBA alpha, transparent corners, and meaningful opaque center coverage.
- UI contract tests assert both rail dimensions, two divider groups, exact labels, and action order.
- Emulator acceptance captures the menu rail and one active single-player position at the standard tablet resolution.
