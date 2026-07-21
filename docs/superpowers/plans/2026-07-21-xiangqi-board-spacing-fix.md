# Xiangqi Board Spacing Fix

**Scope:** `games/xiangqi` UI geometry and package-owned board asset only.

## Acceptance

- Xiangqi advances to `versionCode = 13` and `versionName = 0.0.13`; the shell version stays unchanged.
- Ceramic pieces render at 80% of one grid step without changing source texture geometry.
- The 1600 x 1500 board registers its grid at left/top/right/bottom `128/110/1472/1360`.
- Every generated grid line is derived from the complete floating-point bounds so the PNG and runtime intersections agree at both end rows.
- Bottom-row pieces leave at least 80 source pixels of visible clearance above the board edge.
- The board keeps the approved porcelain, celadon, ink, and ceramic appearance while using a lighter grid, smaller river labels, and one fewer nested frame line.
- Rules, AI, undo, orientation, and latest-move marker geometry remain unchanged.

## Verification

1. Run `./gradlew :games:xiangqi:testDebugUnitTest`.
2. Run `npm run verify`.
3. Start the emulator, open the Xiangqi menu and a match, and compare both runtime captures with `designs/previews/xiangqi-ui-menu.png`.
