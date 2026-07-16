# Board Game Center - Implementation Handoff

**SSOT:** `designs/specs/board-game-center.md`

## Compose Surface Map

1. `GameCenterTheme` exposes the final semantic tokens; game board colors are per-game theme values, not global navigation colors.
2. `HomeScreen` uses a responsive `LazyVerticalGrid` with a shelf-slot card composable. At tablet width the three cards retain equal visual weight.
3. `GameMenuScreen` takes a `GameTheme` with a game mark, title treatment, and material background, but shares button order and semantics.
4. `GameInfoRail` owns only score, turn, and exit. It does not grow into a control center.
5. Board hit targets are invisible 48dp-minimum semantic regions; visual pieces may remain smaller than their touch regions.

## State and Accessibility

- Focus: `focus.ring` is a 2dp outline with enough contrast against both raised and inset surfaces.
- State: ready status includes text and a check/launch icon, never color alone.
- Import: retain the four existing states, with progress announced through accessibility semantics.
- Motion: use `animateFloatAsState` / `AnimatedVisibility` only for the three motion contracts in the SSOT. Honor reduced motion by snapping to final states.

## Visual Restraint

- The cabinet shelf is the signature. Do not add decorative pattern layers, texture images, charts, rank badges, or secondary action strips.
- Use a single consistent low-elevation shadow for shelf objects; boards may use a stronger object shadow because they are treated as physical pieces.
