# android-games-home — desktop

**Preview file:** `designs/previews/android-games-home-desktop.png`
**Spec SSOT:** `designs/specs/android-games-home.md`

## Layout

Straight-on 16:10 landscape Android tablet screen without a device frame. A compact top app bar contains only the title and import action. The content area has one centered label and a row of three precisely equal square selection buttons. Whitespace stays generous; there are no additional modules.

## Modules

- Top app bar: exact title “游戏中心”; outlined action “导入游戏包”.
- Centered section title: exact text “选择游戏”.
- Three identical 264dp-feeling square tactile buttons:
  - Recessed circular Logo well, five-stone Logo, “五子棋”.
  - Recessed circular Logo well, circular “象” piece Logo, “象棋”.
  - Recessed circular Logo well, flipping-disc Logo, “黑白棋”.

## Visual details

Use a cool mineral canvas `#DDE5E3` with extremely subtle fine grain. Buttons are matte cool-porcelain controls with a restrained two-tone surface from `#FCFDFB` to `#F0F4F1`, visible `#AEBDB9` outline, thin top-left white rim, one tight contact shadow, and one soft ambient shadow. Each button contains the same 112dp shallow recessed Logo well `#E5ECE9`. Depth must remain subtle and high contrast—not low-contrast neumorphism. The three buttons and Logo wells have exactly equal dimensions and elevation. No board imagery, wood, felt, glass, blur, glow, or extra content.

## Image prompt

Use case: ui-mockup. Asset type: high-fidelity, shippable Android tablet home screen, not concept art. Create a straight-on 16:10 landscape app screen with no device frame. Cool pale mineral-grey canvas #DDE5E3 with extremely subtle fine matte mineral grain, almost invisible. Compact cool off-white top app bar with exact Chinese title “游戏中心” on the left and one outlined deep-teal #185864 action with exact text “导入游戏包” on the right. Main content contains only centered exact heading “选择游戏” and one centered horizontal row of exactly three large square game-selection buttons. Critical invariant: all three buttons have precisely identical width, height, 20dp-feeling radius, border, shadow, padding, elevation, and baseline; approximately 264dp square with equal 28dp gaps. Give the buttons restrained tactile quality: matte cool-porcelain surface with a very subtle light-to-light tonal shift from #FCFDFB at top to #F0F4F1 at bottom, visible thin #AEBDB9 outline, a fine white top-left rim highlight, a tight contact shadow plus soft low-opacity ambient shadow. Do not use low-contrast neumorphism. Each button contains the same 112dp shallow recessed circular Logo well #E5ECE9 with a clear edge and very subtle inset depth. Button 1: exactly five simple black and porcelain-white stones on a short diagonal, no grid, exact label “五子棋”. Button 2: flat circular Chinese chess-piece mark with restrained cinnabar double rings #A33B33 and exact central character “象”, exact label “象棋”. Button 3: three overlapping discs—black, white with dark outline, and split half-black half-white—exact label “黑白棋”. All Logos have equal visual weight. Clean Chinese Android system sans; only the “象” character may use restrained Chinese serif. Minimal, accessible, practical, Compose-implementable. No board images, board grids, wood, felt, game screenshots, glassmorphism, blur, glowing gradients, neon, metadata, badges, arrows, bottom navigation, extra text, or watermark.
